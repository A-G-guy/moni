use moni_core::core::error::CoreError;
use moni_core::models::intent::{CoreIntent, MockPreset};

#[test]
fn test_core_error_from_rusqlite_error() {
    // 查询不存在表时会触发 rusqlite::Error，From 实现应将其包装为 Database 变体
    let conn = rusqlite::Connection::open_in_memory().unwrap();
    let result = conn.execute("SELECT * FROM nope", []);
    assert!(result.is_err());

    let core_err: CoreError = result.unwrap_err().into();
    match &core_err {
        CoreError::Database(msg) => assert!(!msg.is_empty(), "错误信息不应为空"),
        other => panic!("期望 Database 变体, 得到 {other:?}"),
    }

    // Display 实现应附带"数据库错误"前缀
    assert!(core_err.to_string().contains("数据库错误"));
}

#[test]
fn test_core_error_display_branches() {
    assert_eq!(
        CoreError::Internal("oops".into()).to_string(),
        "oops".to_string()
    );
    assert!(
        CoreError::InvalidInput("bad".into())
            .to_string()
            .contains("参数错误")
    );
    assert!(CoreError::RecordNotFound(42).to_string().contains("id=42"));
    assert!(CoreError::CategoryNotFound(7).to_string().contains("id=7"));
    assert_eq!(
        CoreError::CategoryInUse.to_string(),
        "分类已被使用，无法删除"
    );
    assert_eq!(
        CoreError::CategoryAlreadyArchived.to_string(),
        "分类已被归档"
    );
    assert_eq!(CoreError::CategoryNotArchived.to_string(), "分类未归档");

    let too_new = CoreError::BackupTooNew {
        required: 9,
        supported: 1,
    };
    let s = too_new.to_string();
    assert!(s.contains("format_version=9"), "实际输出: {s}");
    assert!(s.contains("当前支持=1"), "实际输出: {s}");

    let restore_failed = CoreError::BackupRestoreFailed {
        stage: "validation".into(),
        reason: "row mismatch".into(),
    };
    let s = restore_failed.to_string();
    assert!(s.contains("validation"));
    assert!(s.contains("row mismatch"));

    assert!(
        CoreError::BackupZipError("z".into())
            .to_string()
            .contains("ZIP")
    );
    assert!(
        CoreError::BackupManifestInvalid("m".into())
            .to_string()
            .contains("清单")
    );
    assert!(
        CoreError::BackupCorrupted("c".into())
            .to_string()
            .contains("损坏")
    );
    assert!(CoreError::BackupIo("io".into()).to_string().contains("IO"));
}

#[test]
fn test_mock_preset_display() {
    assert_eq!(MockPreset::Normal.to_string(), "normal");
    assert_eq!(MockPreset::Stress.to_string(), "stress");
    assert_eq!(format!("{}", MockPreset::Normal), "normal");
}

#[test]
fn test_mock_preset_serde_lowercase() {
    let normal_json = serde_json::to_string(&MockPreset::Normal).unwrap();
    assert_eq!(normal_json, r#""normal""#);
    let stress_json = serde_json::to_string(&MockPreset::Stress).unwrap();
    assert_eq!(stress_json, r#""stress""#);

    let parsed: MockPreset = serde_json::from_str(r#""normal""#).unwrap();
    assert!(matches!(parsed, MockPreset::Normal));
}

#[test]
fn test_core_intent_serde_roundtrip() {
    let intent = CoreIntent::DevGenerateMockData {
        count: 5,
        preset: MockPreset::Stress,
    };
    let json = serde_json::to_string(&intent).unwrap();
    assert!(json.contains("dev_generate_mock_data"));
    assert!(json.contains("stress"));

    let parsed: CoreIntent = serde_json::from_str(&json).unwrap();
    match parsed {
        CoreIntent::DevGenerateMockData { count, preset } => {
            assert_eq!(count, 5);
            assert!(matches!(preset, MockPreset::Stress));
        }
        _ => panic!("反序列化应得到 DevGenerateMockData"),
    }
}

#[test]
fn test_core_intent_unit_variant_serde() {
    let intent = CoreIntent::DevClearAllData;
    let json = serde_json::to_string(&intent).unwrap();
    assert!(json.contains("dev_clear_all_data"));

    let intent = CoreIntent::CategoryList;
    let json = serde_json::to_string(&intent).unwrap();
    assert!(json.contains("category_list"));

    let intent = CoreIntent::DismissError;
    let json = serde_json::to_string(&intent).unwrap();
    assert!(json.contains("dismiss_error"));
}
