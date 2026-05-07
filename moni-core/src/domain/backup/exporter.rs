use std::fs::File;
use std::io::Write;
use rusqlite::Connection;
use zip::write::FileOptions;

use crate::domain::backup::manifest::{
    BackupManifest, BackupStats, DeviceInfo, FileFingerprint,
    compute_manifest_sha256,
};
use crate::domain::backup::readme::generate_readme;
use crate::db::schema::CURRENT_SCHEMA_VERSION;
use crate::models::backup::BackupExportReport;

const FORMAT_VERSION: u32 = 1;
const PRODUCT_NAME: &str = "Moni";
const PACKAGE_NAME: &str = "com.agguy.moni";

/// 收集数据库统计信息。
#[doc(hidden)]
pub fn collect_stats(
    conn: &Connection,
    settings_json: &str,
) -> Result<BackupStats, crate::core::error::CoreError> {
    let record_count: u64 = conn
        .query_row("SELECT COUNT(*) FROM records", [], |row| row.get(0))
        .map_err(|e| crate::core::error::CoreError::Database(e.to_string()))?;
    let category_count: u64 = conn
        .query_row("SELECT COUNT(*) FROM categories", [], |row| row.get(0))
        .map_err(|e| crate::core::error::CoreError::Database(e.to_string()))?;
    let budget_count: u64 = conn
        .query_row("SELECT COUNT(*) FROM budgets", [], |row| row.get(0))
        .map_err(|e| crate::core::error::CoreError::Database(e.to_string()))?;
    let settings_count = serde_json::from_str::<serde_json::Value>(settings_json)
        .map(|v| v.as_object().map(|o| o.len() as u64).unwrap_or(0))
        .unwrap_or(0);

    Ok(BackupStats {
        record_count,
        category_count,
        budget_count: Some(budget_count),
        settings_count,
    })
}

/// 通过 VACUUM INTO 创建数据库干净副本到临时路径。
fn dump_db_to_temp(conn: &Connection, tmp_path: &str) -> Result<(), crate::core::error::CoreError> {
    conn.execute("VACUUM INTO ?1", [tmp_path])
        .map_err(|e| crate::core::error::CoreError::Database(format!("VACUUM INTO 失败: {e}")))?;
    Ok(())
}

/// 向 ZIP 写入单个文件条目，返回指纹信息。
fn write_zip_entry(
    zip: &mut zip::ZipWriter<File>,
    entry_name: &str,
    content: &[u8],
) -> Result<FileFingerprint, crate::core::error::CoreError> {
    let options: zip::write::FileOptions<'_, ()> = FileOptions::default()
        .compression_method(zip::CompressionMethod::Deflated)
        .compression_level(Some(9));
    zip.start_file(entry_name, options)
        .map_err(|e| crate::core::error::CoreError::BackupZipError(e.to_string()))?;
    zip.write_all(content)
        .map_err(|e| crate::core::error::CoreError::BackupZipError(e.to_string()))?;

    let sha256 = {
        use sha2::{Digest, Sha256};
        let mut hasher = Sha256::new();
        hasher.update(content);
        crate::domain::backup::manifest::bytes_to_hex(&hasher.finalize())
    };

    Ok(FileFingerprint {
        path: entry_name.to_string(),
        size: content.len() as u64,
        sha256,
    })
}

/// 构建并返回 manifest.json 内容字节。
fn build_manifest(
    files: &[FileFingerprint],
    stats: &BackupStats,
    app_version_name: &str,
    app_version_code: i64,
    device_manufacturer: &str,
    device_model: &str,
    android_sdk: i32,
) -> Result<Vec<u8>, crate::core::error::CoreError> {
    let created_at = chrono::Local::now().to_rfc3339();

    let mut manifest = BackupManifest {
        format_version: FORMAT_VERSION,
        schema_version: CURRENT_SCHEMA_VERSION,
        app_version_name: app_version_name.to_string(),
        app_version_code,
        product_name: PRODUCT_NAME.to_string(),
        package_name: PACKAGE_NAME.to_string(),
        created_at,
        device: DeviceInfo {
            manufacturer: device_manufacturer.to_string(),
            model: device_model.to_string(),
            android_sdk,
        },
        stats: stats.clone(),
        files: files.to_vec(),
        manifest_sha256: String::new(),
    };

    manifest.manifest_sha256 = compute_manifest_sha256(&manifest)?;

    let json = serde_json::to_string_pretty(&manifest)
        .map_err(|e| crate::core::error::CoreError::Internal(format!("清单序列化失败: {e}")))?;
    Ok(json.into_bytes())
}

/// 执行全量备份导出，写入 ZIP 文件。
pub fn backup_export(
    conn: &Connection,
    out_zip_path: &str,
    settings_json: &str,
    app_version_name: &str,
    app_version_code: i64,
    device_manufacturer: &str,
    device_model: &str,
    android_sdk: i32,
    on_progress: Option<&dyn Fn(&str, i32)>,
) -> Result<BackupExportReport, crate::core::error::CoreError> {
    if let Some(cb) = on_progress {
        cb("统计数据中...", 5);
    }
    let stats = collect_stats(conn, settings_json)?;

    // 1. 准备临时数据库副本
    if let Some(cb) = on_progress {
        cb("创建数据库副本...", 20);
    }
    let tmp_dir = std::env::temp_dir().join("moni_backup_tmp");
    std::fs::create_dir_all(&tmp_dir)
        .map_err(|e| crate::core::error::CoreError::BackupIo(format!("创建临时目录失败: {e}")))?;
    let tmp_db = tmp_dir.join(format!("moni.{}.db", uuid::Uuid::new_v4()));
    dump_db_to_temp(conn, tmp_db.to_str().unwrap_or("moni_tmp.db"))?;

    // 2. 创建 ZIP 并写入各条目
    if let Some(cb) = on_progress {
        cb("打包数据中...", 40);
    }
    let file = File::create(out_zip_path)
        .map_err(|e| crate::core::error::CoreError::BackupIo(format!("创建 ZIP 失败: {e}")))?;
    let mut zip = zip::ZipWriter::new(file);

    let mut files: Vec<FileFingerprint> = Vec::new();

    // db/moni.db
    let db_bytes = std::fs::read(&tmp_db)
        .map_err(|e| crate::core::error::CoreError::BackupIo(format!("读取临时数据库失败: {e}")))?;
    files.push(write_zip_entry(&mut zip, "db/moni.db", &db_bytes)?);

    // settings/preferences.json
    let settings_bytes = settings_json.as_bytes();
    files.push(write_zip_entry(&mut zip, "settings/preferences.json", settings_bytes)?);

    // README.md
    let readme = generate_readme(
        app_version_name,
        &chrono::Local::now().format("%Y-%m-%d %H:%M:%S").to_string(),
        stats.record_count,
        stats.category_count,
        stats.settings_count,
    );
    let readme_bytes = readme.into_bytes();
    files.push(write_zip_entry(&mut zip, "README.md", &readme_bytes)?);

    // 3. 生成并写入 manifest.json
    if let Some(cb) = on_progress {
        cb("生成清单...", 70);
    }
    let manifest_bytes = build_manifest(
        &files,
        &stats,
        app_version_name,
        app_version_code,
        device_manufacturer,
        device_model,
        android_sdk,
    )?;
    files.push(write_zip_entry(&mut zip, "manifest.json", &manifest_bytes)?);

    // 4. 收尾 ZIP
    if let Some(cb) = on_progress {
        cb("完成...", 90);
    }
    let total_bytes = zip.finish()
        .map_err(|e| crate::core::error::CoreError::BackupZipError(e.to_string()))?
        .metadata()
        .map(|m| m.len())
        .unwrap_or(0);

    // 5. 清理临时文件与目录（使用 remove_dir_all 确保彻底清理）
    let _ = std::fs::remove_file(&tmp_db);
    let _ = std::fs::remove_dir_all(&tmp_dir);
    if let Some(cb) = on_progress {
        cb("完成", 100);
    }

    Ok(BackupExportReport {
        out_zip_path: out_zip_path.to_string(),
        record_count: stats.record_count,
        category_count: stats.category_count,
        settings_count: stats.settings_count,
        total_bytes,
    })
}
