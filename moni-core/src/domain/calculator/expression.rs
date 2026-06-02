use crate::shared::types::AmountCents;

/// 运算符及其优先级（数值越大优先级越高）。
fn precedence(op: char) -> u8 {
    match op {
        '+' | '-' => 1,
        '×' | '÷' => 2,
        _ => 0,
    }
}

fn is_operator(c: char) -> bool {
    matches!(c, '+' | '-' | '×' | '÷')
}

/// 判断 token 是否为单字符运算符，如果是则返回该字符。
fn as_single_char_operator(token: &str) -> Option<char> {
    let mut chars = token.chars();
    let first = chars.next()?;
    if chars.next().is_some() {
        return None;
    }
    if is_operator(first) {
        Some(first)
    } else {
        None
    }
}

/// 将表达式字符串拆分为操作数和运算符 token 列表。
fn tokenize(expression: &str) -> Vec<String> {
    let mut tokens = Vec::new();
    let mut current = String::new();

    for ch in expression.chars() {
        if is_operator(ch) {
            if !current.is_empty() {
                tokens.push(current.clone());
                current.clear();
            }
            tokens.push(ch.to_string());
        } else {
            current.push(ch);
        }
    }

    if !current.is_empty() {
        tokens.push(current);
    }

    tokens
}

/// 中缀表达式转后缀表达式（Shunting Yard 算法）。
fn to_postfix(tokens: &[String]) -> Vec<String> {
    let mut output = Vec::new();
    let mut op_stack: Vec<char> = Vec::new();

    for token in tokens {
        if let Some(op) = as_single_char_operator(token) {
            while let Some(&top) = op_stack.last() {
                if is_operator(top) && precedence(top) >= precedence(op) {
                    output.push(op_stack.pop().unwrap().to_string());
                } else {
                    break;
                }
            }
            op_stack.push(op);
        } else {
            output.push(token.clone());
        }
    }

    while let Some(op) = op_stack.pop() {
        output.push(op.to_string());
    }

    output
}

/// 解析单个操作数为分。
///
/// 有效格式：整数（"15"）、一位小数（"15.5"）、两位小数（"15.50"）。
/// 不允许：空字符串、多个小数点、超过两位小数、负数（表达式中负号由运算符处理）、
/// 小数点后无数字（"15."）。
fn parse_operand_to_cents(operand: &str) -> Option<AmountCents> {
    let trimmed = operand.trim();
    if trimmed.is_empty() || trimmed.starts_with('-') {
        return None;
    }

    let parts: Vec<&str> = trimmed.split('.').collect();
    if parts.len() > 2 {
        return None;
    }

    let integer_part = parts[0];
    if integer_part.is_empty() || !integer_part.chars().all(|c| c.is_ascii_digit()) {
        return None;
    }

    let integer = integer_part.parse::<i64>().ok()?;

    let decimal_part = if parts.len() > 1 { parts[1] } else { "" };
    // 有小数点但小数部分为空（如 "15."）视为不完整
    if parts.len() > 1 && decimal_part.is_empty() {
        return None;
    }
    if !decimal_part.is_empty() && !decimal_part.chars().all(|c| c.is_ascii_digit()) {
        return None;
    }
    if decimal_part.len() > 2 {
        return None;
    }

    let decimal = match decimal_part.len() {
        0 => 0i64,
        1 => decimal_part.parse::<i64>().ok()? * 10,
        2 => decimal_part.parse::<i64>().ok()?,
        _ => return None,
    };

    Some(integer * 100 + decimal)
}

/// 后缀表达式求值（中间结果以元为单位，使用 f64 保持精度）。
fn evaluate_postfix(postfix: &[String]) -> Option<f64> {
    let mut stack: Vec<f64> = Vec::new();

    for token in postfix {
        if let Some(op) = as_single_char_operator(token) {
            if stack.len() < 2 {
                return None;
            }
            let b = stack.pop().unwrap();
            let a = stack.pop().unwrap();
            let result = match op {
                '+' => a + b,
                '-' => a - b,
                '×' => a * b,
                '÷' => {
                    if b == 0.0 {
                        return None;
                    }
                    a / b
                }
                _ => return None,
            };
            stack.push(result);
        } else {
            let cents = parse_operand_to_cents(token)?;
            stack.push(cents as f64 / 100.0);
        }
    }

    if stack.len() == 1 { stack.pop() } else { None }
}

/// 解析表达式并返回计算结果（分）。
///
/// 支持加减乘除四则运算，遵循标准运算符优先级（先乘除后加减）。
/// 每个操作数最多两位小数，最终结果四舍五入后转为分。
pub fn evaluate_expression(expression: &str) -> Option<AmountCents> {
    let trimmed = expression.trim();
    if trimmed.is_empty() {
        return None;
    }
    if trimmed.ends_with(|c| is_operator(c)) {
        return None;
    }

    let tokens = tokenize(trimmed);
    if tokens.is_empty() {
        return None;
    }
    if tokens.len() == 1 {
        return parse_operand_to_cents(&tokens[0]);
    }

    let postfix = to_postfix(&tokens);
    let result = evaluate_postfix(&postfix)?;

    if result >= 0.0 {
        Some((result * 100.0).round() as i64)
    } else {
        None
    }
}

/// 判断表达式是否包含未计算的运算符。
///
/// 注意：以运算符结尾的表达式（如 "15+"）也视为包含未计算运算符，
/// 但 evaluate_expression 会返回 None。
pub fn has_pending_operation(expression: &str) -> bool {
    let trimmed = expression.trim();
    if trimmed.is_empty() {
        return false;
    }
    if trimmed.ends_with(|c| is_operator(c)) {
        return true;
    }
    trimmed.chars().any(is_operator)
}

/// 格式化表达式用于显示，在运算符两侧添加空格。
pub fn format_for_display(expression: &str) -> String {
    expression
        .replace('+', " + ")
        .replace('-', " - ")
        .replace('×', " × ")
        .replace('÷', " ÷ ")
}
