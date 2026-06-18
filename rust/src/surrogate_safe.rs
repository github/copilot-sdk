//! Recovery for JSON payloads that carry malformed `\u` escapes.
//!
//! `serde_json` is strict per RFC 8259: a lone UTF-16 surrogate (or any
//! `\u` escape that is not followed by four hex digits) is a hard parse
//! error. When such a payload arrives on the JSON-RPC stream — for
//! example a backend response that was serialized by a lenient producer,
//! or a session-resume frame truncated mid-emoji — the strict parse
//! aborts the whole read loop and tears down the transport, which then
//! retries forever on the same bad bytes (see github/app#1055).
//!
//! [`sanitize_json_escapes`] rewrites the offending escapes to the
//! Unicode replacement character (`\ufffd`) so the message can be parsed
//! and delivered instead of killing the connection. It is invoked only on
//! the parse-error path, so well-formed traffic pays nothing.

use std::borrow::Cow;

/// The JSON escape for U+FFFD REPLACEMENT CHARACTER, substituted for any
/// malformed or unpaired `\u` escape.
const REPLACEMENT_ESCAPE: &[u8] = b"\\ufffd";

#[inline]
fn hex_value(byte: u8) -> u16 {
    match byte {
        b'0'..=b'9' => u16::from(byte - b'0'),
        b'a'..=b'f' => u16::from(byte - b'a' + 10),
        b'A'..=b'F' => u16::from(byte - b'A' + 10),
        _ => unreachable!("caller guarantees an ASCII hex digit"),
    }
}

/// Decode a `\uXXXX` escape that begins at `input[i]` (which the caller
/// guarantees is `\\` immediately followed by `u`). Returns the 16-bit
/// code unit when four hex digits follow, otherwise `None`.
fn decode_unicode_escape(input: &[u8], i: usize) -> Option<u16> {
    let digits = input.get(i + 2..i + 6)?;
    if digits.iter().all(u8::is_ascii_hexdigit) {
        Some(
            (hex_value(digits[0]) << 12)
                | (hex_value(digits[1]) << 8)
                | (hex_value(digits[2]) << 4)
                | hex_value(digits[3]),
        )
    } else {
        None
    }
}

#[inline]
fn is_high_surrogate(unit: u16) -> bool {
    (0xD800..=0xDBFF).contains(&unit)
}

#[inline]
fn is_low_surrogate(unit: u16) -> bool {
    (0xDC00..=0xDFFF).contains(&unit)
}

/// Rewrite lone UTF-16 surrogates and otherwise-malformed `\u` escapes in
/// a UTF-8 JSON document to `\ufffd`, leaving every other byte untouched.
///
/// Returns [`Cow::Borrowed`] (and a count of `0`) when the input is
/// already strict-JSON safe, so the common case allocates nothing. The
/// returned count is the number of escapes that were replaced.
///
/// The scan tracks JSON string context so an escaped backslash (`\\u…`,
/// where the `u` is literal text, not an escape) is never misinterpreted
/// as a unicode escape. Bytes outside strings — structure, numbers, raw
/// multi-byte UTF-8 — pass through verbatim.
pub(crate) fn sanitize_json_escapes(input: &[u8]) -> (Cow<'_, [u8]>, usize) {
    // Lone-surrogate and malformed escapes only exist behind a backslash;
    // without one there is nothing to repair.
    if !input.contains(&b'\\') {
        return (Cow::Borrowed(input), 0);
    }

    let mut out: Option<Vec<u8>> = None;
    let mut replacements = 0usize;
    let mut in_string = false;
    let mut i = 0;
    let len = input.len();

    // Allocate the owned buffer lazily, seeded with the verbatim prefix we
    // had been borrowing, the first time a byte range diverges from the
    // input.
    macro_rules! diverge {
        () => {
            out.get_or_insert_with(|| input[..i].to_vec())
        };
    }
    macro_rules! copy {
        ($range:expr) => {
            if let Some(buf) = out.as_mut() {
                buf.extend_from_slice($range);
            }
        };
    }

    while i < len {
        let byte = input[i];

        if !in_string {
            if byte == b'"' {
                in_string = true;
            }
            copy!(&input[i..=i]);
            i += 1;
            continue;
        }

        match byte {
            b'"' => {
                in_string = false;
                copy!(&input[i..=i]);
                i += 1;
            }
            b'\\' => match input.get(i + 1) {
                // A `\u…` escape: inspect it for surrogate validity.
                Some(b'u') => match decode_unicode_escape(input, i) {
                    Some(unit) if is_high_surrogate(unit) => {
                        let low_start = i + 6;
                        let paired = input.get(low_start) == Some(&b'\\')
                            && input.get(low_start + 1) == Some(&b'u')
                            && decode_unicode_escape(input, low_start)
                                .is_some_and(is_low_surrogate);
                        if paired {
                            copy!(&input[i..i + 12]);
                            i += 12;
                        } else {
                            diverge!().extend_from_slice(REPLACEMENT_ESCAPE);
                            replacements += 1;
                            i += 6;
                        }
                    }
                    Some(unit) if is_low_surrogate(unit) => {
                        diverge!().extend_from_slice(REPLACEMENT_ESCAPE);
                        replacements += 1;
                        i += 6;
                    }
                    Some(_) => {
                        copy!(&input[i..i + 6]);
                        i += 6;
                    }
                    None => {
                        // `\u` not followed by four hex digits. Emit one
                        // replacement and consume the backslash, the `u`,
                        // and any partial hex run so the leftover digits
                        // don't read as a second broken escape.
                        diverge!().extend_from_slice(REPLACEMENT_ESCAPE);
                        replacements += 1;
                        let mut j = i + 2;
                        while j < len && j < i + 6 && input[j].is_ascii_hexdigit() {
                            j += 1;
                        }
                        i = j;
                    }
                },
                // Any other escape (`\"`, `\\`, `\n`, …) is two bytes that
                // pass through untouched; consuming both also prevents an
                // escaped backslash from masking the following character.
                Some(_) => {
                    copy!(&input[i..i + 2]);
                    i += 2;
                }
                // Trailing lone backslash at end of buffer.
                None => {
                    copy!(&input[i..=i]);
                    i += 1;
                }
            },
            _ => {
                copy!(&input[i..=i]);
                i += 1;
            }
        }
    }

    match out {
        Some(buf) => (Cow::Owned(buf), replacements),
        None => (Cow::Borrowed(input), replacements),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn sanitized_str(input: &str) -> (String, usize) {
        let (bytes, count) = sanitize_json_escapes(input.as_bytes());
        (String::from_utf8(bytes.into_owned()).unwrap(), count)
    }

    #[test]
    fn clean_payload_is_borrowed_without_allocation() {
        let input = br#"{"a":"hello \u00e9 \ud83d\ude00 world","b":[1,2,3]}"#;
        let (out, count) = sanitize_json_escapes(input);
        assert!(matches!(out, Cow::Borrowed(_)));
        assert_eq!(count, 0);
        assert_eq!(out.as_ref(), input);
    }

    #[test]
    fn payload_without_backslash_is_borrowed() {
        let input = br#"{"a":"plain text","n":42}"#;
        let (out, count) = sanitize_json_escapes(input);
        assert!(matches!(out, Cow::Borrowed(_)));
        assert_eq!(count, 0);
    }

    #[test]
    fn lone_high_surrogate_becomes_replacement() {
        let (out, count) = sanitized_str(r#"{"a":"x\ud83d y"}"#);
        assert_eq!(out, r#"{"a":"x\ufffd y"}"#);
        assert_eq!(count, 1);
        // The repaired text must now parse strictly.
        let value: serde_json::Value = serde_json::from_str(&out).unwrap();
        assert_eq!(value["a"], "x\u{fffd} y");
    }

    #[test]
    fn lone_low_surrogate_becomes_replacement() {
        let (out, count) = sanitized_str(r#"{"a":"x\udd11 y"}"#);
        assert_eq!(out, r#"{"a":"x\ufffd y"}"#);
        assert_eq!(count, 1);
        serde_json::from_str::<serde_json::Value>(&out).unwrap();
    }

    #[test]
    fn valid_surrogate_pair_is_preserved() {
        let (out, count) = sanitized_str(r#"{"a":"key \ud83d\udd11 done"}"#);
        assert_eq!(out, r#"{"a":"key \ud83d\udd11 done"}"#);
        assert_eq!(count, 0);
        let value: serde_json::Value = serde_json::from_str(&out).unwrap();
        assert_eq!(value["a"], "key \u{1f511} done");
    }

    #[test]
    fn high_surrogate_followed_by_bmp_escape_is_repaired() {
        // `\ud83d` (high) followed by a non-low escape `\u0041` ('A').
        let (out, count) = sanitized_str(r#"{"a":"\ud83d\u0041"}"#);
        assert_eq!(out, r#"{"a":"\ufffd\u0041"}"#);
        assert_eq!(count, 1);
        let value: serde_json::Value = serde_json::from_str(&out).unwrap();
        assert_eq!(value["a"], "\u{fffd}A");
    }

    #[test]
    fn high_surrogate_at_end_of_string_is_repaired() {
        let (out, count) = sanitized_str(r#"{"a":"tail\ud83d"}"#);
        assert_eq!(out, r#"{"a":"tail\ufffd"}"#);
        assert_eq!(count, 1);
        serde_json::from_str::<serde_json::Value>(&out).unwrap();
    }

    #[test]
    fn malformed_hex_escape_is_repaired() {
        let (out, count) = sanitized_str(r#"{"a":"x\uZZZZ y"}"#);
        assert_eq!(out, r#"{"a":"x\ufffdZZZZ y"}"#);
        assert_eq!(count, 1);
        serde_json::from_str::<serde_json::Value>(&out).unwrap();
    }

    #[test]
    fn truncated_hex_escape_before_quote_is_repaired() {
        let (out, count) = sanitized_str(r#"{"a":"x\ud8"}"#);
        assert_eq!(out, r#"{"a":"x\ufffd"}"#);
        assert_eq!(count, 1);
        serde_json::from_str::<serde_json::Value>(&out).unwrap();
    }

    #[test]
    fn escaped_backslash_before_u_is_not_an_escape() {
        // `\\u d83d` is a literal backslash followed by the text "ud83d";
        // it must be left alone, not treated as a surrogate.
        let (out, count) = sanitized_str(r#"{"a":"\\ud83d"}"#);
        assert_eq!(out, r#"{"a":"\\ud83d"}"#);
        assert_eq!(count, 0);
    }

    #[test]
    fn surrogate_like_bytes_outside_strings_are_untouched() {
        // A key that itself contains a valid escape plus a lone surrogate
        // in the value, to confirm string boundaries are tracked.
        let (out, count) = sanitized_str(r#"{"k\ud83d":"v"}"#);
        assert_eq!(out, r#"{"k\ufffd":"v"}"#);
        assert_eq!(count, 1);
        serde_json::from_str::<serde_json::Value>(&out).unwrap();
    }

    #[test]
    fn multiple_lone_surrogates_are_all_replaced() {
        let (out, count) = sanitized_str(r#"{"a":"\ud83d\ud83d","b":"\udc00"}"#);
        assert_eq!(out, r#"{"a":"\ufffd\ufffd","b":"\ufffd"}"#);
        assert_eq!(count, 3);
        serde_json::from_str::<serde_json::Value>(&out).unwrap();
    }

    #[test]
    fn raw_multibyte_utf8_in_string_is_preserved() {
        let (out, count) = sanitized_str(r#"{"a":"café 🚀 \ud83d"}"#);
        assert_eq!(out, r#"{"a":"café 🚀 \ufffd"}"#);
        assert_eq!(count, 1);
        serde_json::from_str::<serde_json::Value>(&out).unwrap();
    }
}
