use std::borrow::Cow;
use std::collections::HashSet;
use std::fmt;
use std::ops::Range;

use serde::de::{DeserializeOwned, MapAccess, SeqAccess, Visitor};
use serde::{Deserialize, Deserializer};
use serde_json::Value;

use super::{JsonRpcError, JsonRpcResponse};
use crate::Error;

pub(crate) struct Response {
    pub(super) jsonrpc: String,
    pub(super) id: u64,
    pub(super) result: Option<ResponseResult>,
    pub(crate) error: Option<JsonRpcError>,
}

pub(super) enum ResponseResult {
    Parsed(Value),
    Raw { body: Vec<u8>, range: Range<usize> },
}

impl From<JsonRpcResponse> for Response {
    fn from(response: JsonRpcResponse) -> Self {
        Self {
            jsonrpc: response.jsonrpc,
            id: response.id,
            result: response.result.map(ResponseResult::Parsed),
            error: response.error,
        }
    }
}

impl Response {
    pub(super) fn into_response(self) -> Result<JsonRpcResponse, Error> {
        Ok(JsonRpcResponse {
            jsonrpc: self.jsonrpc,
            id: self.id,
            result: self.result.map(ResponseResult::into_value).transpose()?,
            error: self.error,
        })
    }

    pub(crate) fn deserialize<T: DeserializeOwned>(self) -> Result<T, Error> {
        match self.result {
            Some(ResponseResult::Raw { body, range }) => {
                let raw = &body[range];
                match serde_json::from_slice(raw) {
                    Ok(result) => Ok(result),
                    // Retain Value's DTO conversion and error behavior for
                    // payloads that the direct deserializer cannot consume.
                    Err(_) => Ok(serde_json::from_value(serde_json::from_slice(raw)?)?),
                }
            }
            result => {
                let value = result
                    .map(ResponseResult::into_value)
                    .transpose()?
                    .unwrap_or(Value::Null);
                Ok(serde_json::from_value(value)?)
            }
        }
    }
}

impl ResponseResult {
    fn into_value(self) -> Result<Value, Error> {
        match self {
            Self::Parsed(value) => Ok(value),
            Self::Raw { body, range } => Ok(serde_json::from_slice(&body[range])?),
        }
    }
}

// RawValue skips string, number, and nesting validation. Validate the whole
// frame at its original depth before bypassing Value. Duplicate keys also
// require Value's last-wins normalization before untagged/flattened DTOs run.
pub(super) fn validate_typed_frame(body: &[u8]) -> Result<(), serde_json::Error> {
    serde_json::from_slice::<ValidatedJson>(body).map(|_| ())
}

struct ValidatedJson;

impl<'de> Deserialize<'de> for ValidatedJson {
    fn deserialize<D: Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        deserializer.deserialize_any(ValidatedJsonVisitor)
    }
}

#[derive(Deserialize, PartialEq, Eq, Hash)]
#[serde(transparent)]
struct JsonKey<'a>(#[serde(borrow)] Cow<'a, str>);

struct ValidatedJsonVisitor;

impl<'de> Visitor<'de> for ValidatedJsonVisitor {
    type Value = ValidatedJson;

    fn expecting(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        formatter.write_str("a JSON value")
    }

    fn visit_bool<E: serde::de::Error>(self, _: bool) -> Result<Self::Value, E> {
        Ok(ValidatedJson)
    }

    fn visit_i64<E: serde::de::Error>(self, _: i64) -> Result<Self::Value, E> {
        Ok(ValidatedJson)
    }

    fn visit_u64<E: serde::de::Error>(self, _: u64) -> Result<Self::Value, E> {
        Ok(ValidatedJson)
    }

    fn visit_f64<E: serde::de::Error>(self, _: f64) -> Result<Self::Value, E> {
        Ok(ValidatedJson)
    }

    fn visit_str<E: serde::de::Error>(self, _: &str) -> Result<Self::Value, E> {
        Ok(ValidatedJson)
    }

    fn visit_unit<E: serde::de::Error>(self) -> Result<Self::Value, E> {
        Ok(ValidatedJson)
    }

    fn visit_seq<A: SeqAccess<'de>>(self, mut sequence: A) -> Result<Self::Value, A::Error> {
        while sequence.next_element::<ValidatedJson>()?.is_some() {}
        Ok(ValidatedJson)
    }

    fn visit_map<A: MapAccess<'de>>(self, mut map: A) -> Result<Self::Value, A::Error> {
        let mut keys = HashSet::new();
        while let Some(key) = map.next_key::<JsonKey<'de>>()? {
            if !keys.insert(key) {
                return Err(serde::de::Error::custom("duplicate JSON object key"));
            }
            map.next_value::<ValidatedJson>()?;
        }
        Ok(ValidatedJson)
    }
}
