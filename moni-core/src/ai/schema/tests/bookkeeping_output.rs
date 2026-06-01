use super::parse_bookkeeping_output;

#[test]
fn parses_valid_bookkeeping_json() {
    let result = parse_bookkeeping_output(
        r#"{"is_bookkeeping":true,"reply_text":"ok","amount_cents":3500,"record_type":"expense","category_id":1,"confidence":0.9}"#,
    )
    .expect("parse");
    assert!(result.is_bookkeeping);
    assert_eq!(result.card_data.expect("card").amount_cents, 3500);
}

#[test]
fn rejects_invalid_amount() {
    let error = parse_bookkeeping_output(
        r#"{"is_bookkeeping":true,"amount_cents":0,"record_type":"expense"}"#,
    )
    .expect_err("invalid");
    assert!(error.to_string().contains("金额"));
}

#[test]
fn preserves_unknown_category_id() {
    let result = parse_bookkeeping_output(
        r#"{"is_bookkeeping":true,"reply_text":"ok","amount_cents":3500,"record_type":"expense","category_id":-1,"confidence":0.9}"#,
    )
    .expect("parse");
    assert_eq!(result.card_data.expect("card").category_id, -1);
}

#[test]
fn parses_non_bookkeeping_json() {
    let result =
        parse_bookkeeping_output(r#"{"is_bookkeeping":false,"reply_text":"你好"}"#).expect("parse");
    assert!(!result.is_bookkeeping);
    assert!(result.card_data.is_none());
}
