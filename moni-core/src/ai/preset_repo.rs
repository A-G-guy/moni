use rusqlite::Connection;

use crate::ai::domain::{ProviderPreset, ProviderPresetSaveRequest};
use crate::ai::errors::AiError;
use crate::db::settings_repo;

const PRESET_KEY_PREFIX: &str = "ai.preset.";
const NEXT_ID_KEY: &str = "ai.next_preset_id";
const DEFAULT_ID_KEY: &str = "ai.default_preset_id";

/// 返回全部 Provider 预设。
pub fn list(conn: &Connection) -> Result<Vec<ProviderPreset>, AiError> {
    let settings = settings_repo::load_all(conn).map_err(map_db_error)?;
    let default_id = get_default_id(conn)?;
    let mut presets = settings
        .into_iter()
        .filter_map(|(key, value)| {
            key.strip_prefix(PRESET_KEY_PREFIX)
                .and_then(|_| serde_json::from_str::<ProviderPreset>(&value).ok())
        })
        .map(|mut preset| {
            preset.is_default = Some(preset.id) == default_id;
            preset
        })
        .collect::<Vec<_>>();
    presets.sort_by_key(|preset| preset.id);
    Ok(presets)
}

/// 保存 Provider 预设。更新时如果 api_key 为空则保留旧 key。
pub fn save(conn: &Connection, request: ProviderPresetSaveRequest) -> Result<i64, AiError> {
    validate_request(&request)?;
    let now = chrono::Utc::now().timestamp();
    let old = request.id.map(|id| get(conn, id)).transpose()?.flatten();
    let id = old
        .as_ref()
        .map_or_else(|| next_id(conn), |preset| Ok(preset.id))?;
    let api_key = resolve_api_key(&request, old.as_ref())?;
    let preset = ProviderPreset {
        id,
        name: request.name.trim().to_string(),
        api_format: request.api_format,
        base_url: request.base_url.trim().trim_end_matches('/').to_string(),
        api_key,
        model: request.model.trim().to_string(),
        thinking_level: request.thinking_level,
        supports_vision: request.supports_vision,
        is_default: request.is_default,
        created_at: old.as_ref().map_or(now, |preset| preset.created_at),
        updated_at: now,
    };
    write_preset(conn, &preset)?;
    if preset.is_default || get_default_id(conn)?.is_none() {
        set_default(conn, id)?;
    }
    Ok(id)
}

/// 根据 ID 删除 Provider 预设。
pub fn delete(conn: &Connection, id: i64) -> Result<(), AiError> {
    let key = preset_key(id);
    let affected = settings_repo::delete(conn, &key).map_err(map_db_error)?;
    if affected == 0 {
        return Err(AiError::PresetNotFound(id));
    }
    if get_default_id(conn)? == Some(id) {
        settings_repo::delete(conn, DEFAULT_ID_KEY).map_err(map_db_error)?;
    }
    Ok(())
}

/// 设置默认 Provider 预设。
pub fn set_default(conn: &Connection, id: i64) -> Result<(), AiError> {
    if get(conn, id)?.is_none() {
        return Err(AiError::PresetNotFound(id));
    }
    settings_repo::set(conn, DEFAULT_ID_KEY, &id.to_string()).map_err(map_db_error)?;
    Ok(())
}

/// 读取默认 Provider 预设。
pub fn get_default(conn: &Connection) -> Result<Option<ProviderPreset>, AiError> {
    let Some(id) = get_default_id(conn)? else {
        return Ok(None);
    };
    let mut preset = get(conn, id)?.ok_or(AiError::PresetNotFound(id))?;
    preset.is_default = true;
    Ok(Some(preset))
}

/// 根据 ID 读取完整 Provider 预设。
pub fn get(conn: &Connection, id: i64) -> Result<Option<ProviderPreset>, AiError> {
    let value = settings_repo::get(conn, &preset_key(id)).map_err(map_db_error)?;
    value
        .map(|json| serde_json::from_str(&json).map_err(|error| AiError::Parse(error.to_string())))
        .transpose()
}

fn write_preset(conn: &Connection, preset: &ProviderPreset) -> Result<(), AiError> {
    let json = serde_json::to_string(preset).map_err(|error| AiError::Parse(error.to_string()))?;
    settings_repo::set(conn, &preset_key(preset.id), &json).map_err(map_db_error)?;
    Ok(())
}

fn next_id(conn: &Connection) -> Result<i64, AiError> {
    let current = settings_repo::get(conn, NEXT_ID_KEY)
        .map_err(map_db_error)?
        .and_then(|value| value.parse::<i64>().ok())
        .unwrap_or(1);
    settings_repo::set(conn, NEXT_ID_KEY, &(current + 1).to_string()).map_err(map_db_error)?;
    Ok(current)
}

fn get_default_id(conn: &Connection) -> Result<Option<i64>, AiError> {
    settings_repo::get(conn, DEFAULT_ID_KEY)
        .map_err(map_db_error)
        .map(|value| value.and_then(|raw| raw.parse::<i64>().ok()))
}

fn validate_request(request: &ProviderPresetSaveRequest) -> Result<(), AiError> {
    if request.name.trim().is_empty() {
        return Err(AiError::InvalidPreset("名称不能为空".to_string()));
    }
    if request.base_url.trim().is_empty() {
        return Err(AiError::InvalidPreset("Base URL 不能为空".to_string()));
    }
    if request.model.trim().is_empty() {
        return Err(AiError::InvalidPreset("模型不能为空".to_string()));
    }
    Ok(())
}

fn resolve_api_key(
    request: &ProviderPresetSaveRequest,
    old: Option<&ProviderPreset>,
) -> Result<String, AiError> {
    let new_key = request.api_key.as_deref().unwrap_or_default().trim();
    if !new_key.is_empty() {
        return Ok(new_key.to_string());
    }
    old.map(|preset| preset.api_key.clone())
        .filter(|api_key| !api_key.trim().is_empty())
        .ok_or(AiError::MissingApiKey)
}

fn preset_key(id: i64) -> String {
    format!("{PRESET_KEY_PREFIX}{id}")
}

fn map_db_error(error: rusqlite::Error) -> AiError {
    AiError::InvalidPreset(format!("数据库访问失败: {error}"))
}

#[cfg(test)]
#[path = "tests/preset_repo.rs"]
mod tests;
