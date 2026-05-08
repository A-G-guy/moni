use moni_core::domain::calculator::expression;

#[test]
fn test_evaluate_integer() {
    assert_eq!(expression::evaluate_expression("15"), Some(1500));
}

#[test]
fn test_evaluate_one_decimal() {
    assert_eq!(expression::evaluate_expression("15.5"), Some(1550));
}

#[test]
fn test_evaluate_two_decimals() {
    assert_eq!(expression::evaluate_expression("15.55"), Some(1555));
}

#[test]
fn test_evaluate_zero_decimal() {
    assert_eq!(expression::evaluate_expression("15.0"), Some(1500));
    assert_eq!(expression::evaluate_expression("15.00"), Some(1500));
}

#[test]
fn test_addition() {
    assert_eq!(expression::evaluate_expression("15.5+20"), Some(3550));
    assert_eq!(expression::evaluate_expression("100+50"), Some(15000));
}

#[test]
fn test_subtraction() {
    assert_eq!(expression::evaluate_expression("20-15.5"), Some(450));
    assert_eq!(expression::evaluate_expression("100-30"), Some(7000));
}

#[test]
fn test_multiplication() {
    assert_eq!(expression::evaluate_expression("10×5"), Some(5000));
    assert_eq!(expression::evaluate_expression("2.5×4"), Some(1000));
}

#[test]
fn test_division() {
    assert_eq!(expression::evaluate_expression("100÷3"), Some(3333));
    assert_eq!(expression::evaluate_expression("10÷2"), Some(500));
}

#[test]
fn test_mixed_priority() {
    assert_eq!(expression::evaluate_expression("100-25×2"), Some(5000));
    assert_eq!(expression::evaluate_expression("100+50×3-25"), Some(22500));
}

#[test]
fn test_complex_expression() {
    assert_eq!(
        expression::evaluate_expression("100+50×3-25÷2"),
        Some(23750)
    );
}

#[test]
fn test_multiple_operations_same_priority() {
    assert_eq!(expression::evaluate_expression("10+20-5"), Some(2500));
    assert_eq!(expression::evaluate_expression("100÷2÷2"), Some(2500));
}

#[test]
fn test_decimal_addition() {
    assert_eq!(expression::evaluate_expression("10.5+20.5"), Some(3100));
    assert_eq!(expression::evaluate_expression("0.5+0.5"), Some(100));
}

#[test]
fn test_empty_returns_none() {
    assert_eq!(expression::evaluate_expression(""), None);
}

#[test]
fn test_blank_returns_none() {
    assert_eq!(expression::evaluate_expression("   "), None);
}

#[test]
fn test_trailing_operator_returns_none() {
    assert_eq!(expression::evaluate_expression("15+"), None);
    assert_eq!(expression::evaluate_expression("100-"), None);
    assert_eq!(expression::evaluate_expression("20×"), None);
    assert_eq!(expression::evaluate_expression("10÷"), None);
}

#[test]
fn test_multiple_dots_returns_none() {
    assert_eq!(expression::evaluate_expression("15..5"), None);
    assert_eq!(expression::evaluate_expression("1.2.3"), None);
}

#[test]
fn test_over_two_decimals_returns_none() {
    assert_eq!(expression::evaluate_expression("15.555"), None);
}

#[test]
fn test_negative_returns_none() {
    assert_eq!(expression::evaluate_expression("-15"), None);
}

#[test]
fn test_divide_by_zero_returns_none() {
    assert_eq!(expression::evaluate_expression("100÷0"), None);
}

#[test]
fn test_decimal_without_digits_returns_none() {
    assert_eq!(expression::evaluate_expression("15."), None);
}

#[test]
fn test_invalid_characters_returns_none() {
    assert_eq!(expression::evaluate_expression("10a+5"), None);
    assert_eq!(expression::evaluate_expression("10+5b"), None);
}

#[test]
fn test_has_pending_operation_with_operator() {
    assert!(!expression::has_pending_operation("15"));
    assert!(expression::has_pending_operation("15+20"));
    assert!(expression::has_pending_operation("15+"));
}

#[test]
fn test_has_pending_operation_empty() {
    assert!(!expression::has_pending_operation(""));
    assert!(!expression::has_pending_operation("   "));
}

#[test]
fn test_format_for_display() {
    assert_eq!(expression::format_for_display("15+20"), "15 + 20");
    assert_eq!(expression::format_for_display("100-25×2"), "100 - 25 × 2");
    assert_eq!(expression::format_for_display("10÷2+3"), "10 ÷ 2 + 3");
}

#[test]
fn test_rounding_behavior() {
    // 验证四舍五入行为与 Kotlin round() 一致
    // 通过除法产生需要四舍五入的结果
    assert_eq!(expression::evaluate_expression("1÷3"), Some(33));
    assert_eq!(expression::evaluate_expression("2÷3"), Some(67));
    assert_eq!(expression::evaluate_expression("1÷6"), Some(17));
    assert_eq!(expression::evaluate_expression("5÷6"), Some(83));
}

#[test]
fn test_large_numbers() {
    assert_eq!(
        expression::evaluate_expression("999999.99"),
        Some(99_999_999)
    );
}
