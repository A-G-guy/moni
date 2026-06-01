use super::mask_api_key;

#[test]
fn masks_short_and_long_keys() {
    assert_eq!(mask_api_key(""), "");
    assert_eq!(mask_api_key("abc"), "••••");
    assert_eq!(mask_api_key("sk-1234567890"), "sk-1••••7890");
}
