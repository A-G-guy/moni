use rusqlite::Connection;

use super::{delete, get_default, list, save, set_default};
use crate::ai::domain::{ApiFormat, ProviderPresetSaveRequest, ThinkingLevel};
use crate::db::schema;

#[test]
fn saves_lists_and_deletes_presets() {
    let conn = Connection::open_in_memory().expect("db");
    schema::init_schema(&conn).expect("schema");
    let id = save(&conn, request(None, "OpenAI", true)).expect("save");
    assert_eq!(id, 1);
    let presets = list(&conn).expect("list");
    assert_eq!(presets.len(), 1);
    assert!(presets[0].is_default);
    assert_eq!(presets[0].api_key, "secret");
    delete(&conn, id).expect("delete");
    assert!(get_default(&conn).expect("default").is_none());
}

#[test]
fn updates_preserving_existing_api_key() {
    let conn = Connection::open_in_memory().expect("db");
    schema::init_schema(&conn).expect("schema");
    let id = save(&conn, request(None, "OpenAI", true)).expect("save");
    let mut update = request(Some(id), "OpenAI 2", false);
    update.api_key = None;
    save(&conn, update).expect("update");
    set_default(&conn, id).expect("default");
    let preset = get_default(&conn).expect("default").expect("preset");
    assert_eq!(preset.name, "OpenAI 2");
    assert_eq!(preset.api_key, "secret");
}

fn request(id: Option<i64>, name: &str, is_default: bool) -> ProviderPresetSaveRequest {
    ProviderPresetSaveRequest {
        id,
        name: name.to_string(),
        api_format: ApiFormat::OpenAiChatCompletions,
        base_url: "https://api.openai.com/v1".to_string(),
        api_key: Some("secret".to_string()),
        model: "gpt-4o-mini".to_string(),
        thinking_level: ThinkingLevel::Off,
        supports_vision: false,
        is_default,
    }
}
