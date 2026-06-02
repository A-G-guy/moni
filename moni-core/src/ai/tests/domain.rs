use super::{AiBookkeepingParseRequest, mask_api_key};

#[test]
fn masks_short_and_long_keys() {
    assert_eq!(mask_api_key(""), "");
    assert_eq!(mask_api_key("abc"), "••••");
    assert_eq!(mask_api_key("sk-1234567890"), "sk-1••••7890");
}

#[test]
fn normalized_text_includes_sent_at_context() {
    let request = AiBookkeepingParseRequest {
        text: "今天午餐 12 元".to_string(),
        images: Vec::new(),
        sent_at: Some(1_780_329_600),
    };

    let text = request.normalized_text();

    assert!(text.contains("本次请求发送时间"));
    assert!(text.contains("Unix 秒=1780329600"));
    assert!(text.contains("用户输入：今天午餐 12 元"));
}
