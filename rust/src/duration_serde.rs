//! Serde helpers for duration fields encoded as millisecond numbers on the wire.
//!
//! The JSON-RPC schemas use `format: "duration"` on integer/number fields with
//! the convention "value in milliseconds". This module provides serde adapters
//! so generated code can use `std::time::Duration` directly while still
//! producing/consuming the millisecond-integer (or millisecond-float) wire
//! format the CLI expects.
//!
//! Each submodule is intended for use with `#[serde(with = "...")]` on a
//! `Duration` (or `Option<Duration>`) field in a generated struct.

use std::time::Duration;

use serde::{Deserialize, Deserializer, Serialize, Serializer};

/// Convert milliseconds (saturating at 0 for negative input) to a `Duration`.
fn ms_i64_to_duration(ms: i64) -> Duration {
    Duration::from_millis(ms.max(0) as u64)
}

/// Convert fractional milliseconds (saturating at 0 / non-finite to 0) to a `Duration`.
fn ms_f64_to_duration(ms: f64) -> Duration {
    if !ms.is_finite() || ms <= 0.0 {
        Duration::ZERO
    } else {
        Duration::from_secs_f64(ms / 1000.0)
    }
}

/// Required `Duration` <-> integer milliseconds.
pub mod millis {
    use super::*;

    pub fn serialize<S: Serializer>(value: &Duration, s: S) -> Result<S::Ok, S::Error> {
        // Saturating cast: u128 -> u64 is fine for any sane duration (~584M years).
        let ms = u64::try_from(value.as_millis()).unwrap_or(u64::MAX);
        ms.serialize(s)
    }

    pub fn deserialize<'de, D: Deserializer<'de>>(d: D) -> Result<Duration, D::Error> {
        let ms = i64::deserialize(d)?;
        Ok(ms_i64_to_duration(ms))
    }
}

/// Optional `Duration` <-> integer milliseconds.
pub mod millis_opt {
    use super::*;

    pub fn serialize<S: Serializer>(value: &Option<Duration>, s: S) -> Result<S::Ok, S::Error> {
        match value {
            Some(d) => {
                let ms = u64::try_from(d.as_millis()).unwrap_or(u64::MAX);
                s.serialize_some(&ms)
            }
            None => s.serialize_none(),
        }
    }

    pub fn deserialize<'de, D: Deserializer<'de>>(d: D) -> Result<Option<Duration>, D::Error> {
        Ok(Option::<i64>::deserialize(d)?.map(ms_i64_to_duration))
    }
}

/// Required `Duration` <-> floating-point milliseconds.
pub mod millis_f64 {
    use super::*;

    pub fn serialize<S: Serializer>(value: &Duration, s: S) -> Result<S::Ok, S::Error> {
        (value.as_secs_f64() * 1000.0).serialize(s)
    }

    pub fn deserialize<'de, D: Deserializer<'de>>(d: D) -> Result<Duration, D::Error> {
        let ms = f64::deserialize(d)?;
        Ok(ms_f64_to_duration(ms))
    }
}

/// Optional `Duration` <-> floating-point milliseconds.
pub mod millis_f64_opt {
    use super::*;

    pub fn serialize<S: Serializer>(value: &Option<Duration>, s: S) -> Result<S::Ok, S::Error> {
        match value {
            Some(d) => s.serialize_some(&(d.as_secs_f64() * 1000.0)),
            None => s.serialize_none(),
        }
    }

    pub fn deserialize<'de, D: Deserializer<'de>>(d: D) -> Result<Option<Duration>, D::Error> {
        Ok(Option::<f64>::deserialize(d)?.map(ms_f64_to_duration))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde::{Deserialize, Serialize};

    #[derive(Debug, Serialize, Deserialize, PartialEq)]
    struct ReqInt {
        #[serde(with = "millis")]
        d: Duration,
    }

    #[derive(Debug, Serialize, Deserialize, PartialEq)]
    struct OptInt {
        #[serde(with = "millis_opt", skip_serializing_if = "Option::is_none", default)]
        d: Option<Duration>,
    }

    #[derive(Debug, Serialize, Deserialize, PartialEq)]
    struct ReqFloat {
        #[serde(with = "millis_f64")]
        d: Duration,
    }

    #[derive(Debug, Serialize, Deserialize, PartialEq)]
    struct OptFloat {
        #[serde(
            with = "millis_f64_opt",
            skip_serializing_if = "Option::is_none",
            default
        )]
        d: Option<Duration>,
    }

    #[test]
    fn required_int_roundtrip() {
        let v = ReqInt {
            d: Duration::from_millis(1500),
        };
        let s = serde_json::to_string(&v).unwrap();
        assert_eq!(s, r#"{"d":1500}"#);
        let back: ReqInt = serde_json::from_str(&s).unwrap();
        assert_eq!(back, v);
    }

    #[test]
    fn optional_int_some_and_none() {
        let some = OptInt {
            d: Some(Duration::from_millis(250)),
        };
        let s = serde_json::to_string(&some).unwrap();
        assert_eq!(s, r#"{"d":250}"#);
        let back: OptInt = serde_json::from_str(&s).unwrap();
        assert_eq!(back, some);

        let none = OptInt { d: None };
        let s = serde_json::to_string(&none).unwrap();
        assert_eq!(s, r#"{}"#);
        let back: OptInt = serde_json::from_str(&s).unwrap();
        assert_eq!(back, none);
    }

    #[test]
    fn required_float_roundtrip() {
        let v = ReqFloat {
            d: Duration::from_micros(1_500_500),
        };
        let s = serde_json::to_string(&v).unwrap();
        let back: ReqFloat = serde_json::from_str(&s).unwrap();
        assert_eq!(back.d.as_micros(), v.d.as_micros());
    }

    #[test]
    fn optional_float_some_and_none() {
        let some = OptFloat {
            d: Some(Duration::from_millis(42)),
        };
        let s = serde_json::to_string(&some).unwrap();
        let back: OptFloat = serde_json::from_str(&s).unwrap();
        assert_eq!(back, some);

        let none = OptFloat { d: None };
        let s = serde_json::to_string(&none).unwrap();
        assert_eq!(s, r#"{}"#);
    }

    #[test]
    fn negative_int_clamps_to_zero() {
        let s = r#"{"d":-100}"#;
        let v: ReqInt = serde_json::from_str(s).unwrap();
        assert_eq!(v.d, Duration::ZERO);
    }

    #[test]
    fn non_finite_float_clamps_to_zero() {
        let s = r#"{"d":-1.5}"#;
        let v: ReqFloat = serde_json::from_str(s).unwrap();
        assert_eq!(v.d, Duration::ZERO);
    }
}
