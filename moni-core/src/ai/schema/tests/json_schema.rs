use super::{gemini_response_schema, openai_response_format};

#[test]
fn builds_openai_schema_with_strict_wrapper() {
    let value = openai_response_format();
    assert_eq!(value["type"], "json_schema");
    assert_eq!(value["json_schema"]["strict"], true);
}

#[test]
fn builds_gemini_schema_object() {
    let value = gemini_response_schema();
    assert_eq!(value["type"], "object");
    assert_eq!(
        value["properties"]["clarification_question"]["type"],
        "string"
    );
}
