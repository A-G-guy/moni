use moni_core::domain::stats::calculator;

#[test]
fn test_monthly_summary() {
    let input = vec![
        ("2026-04".to_string(), 10000, 5000),
        ("2026-05".to_string(), 12000, 8000),
    ];
    let result = calculator::calculate_monthly_summary(input);
    assert_eq!(result.len(), 2);
    assert_eq!(result[0].balance_cents, 5000);
    assert_eq!(result[1].balance_cents, 4000);
}

#[test]
fn test_category_breakdown() {
    let input = vec![
        (1, "餐饮".to_string(), 6000),
        (2, "交通".to_string(), 4000),
    ];
    let result = calculator::calculate_category_breakdown(input);
    assert_eq!(result.len(), 2);
    assert_eq!(result[0].percentage, 60.0);
    assert_eq!(result[1].percentage, 40.0);
}

#[test]
fn test_category_breakdown_empty() {
    let result = calculator::calculate_category_breakdown(Vec::new());
    assert!(result.is_empty());
}

#[test]
fn test_category_breakdown_zero_total() {
    let input = vec![
        (1, "餐饮".to_string(), 0),
    ];
    let result = calculator::calculate_category_breakdown(input);
    assert!(result.is_empty());
}
