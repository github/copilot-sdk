use std::collections::HashMap;

use async_trait::async_trait;
use serde::{Deserialize, Serialize};

use crate::types::SessionId;

/// Context provided to every transform invocation.
#[non_exhaustive]
#[derive(Debug, Clone)]
pub struct TransformContext {
    pub session_id: SessionId,
}

impl TransformContext {
    /// Create a new transform context.
    pub fn new(session_id: SessionId) -> Self {
        Self { session_id }
    }
}

/// Handles `systemMessage.transform` RPC requests from the CLI.
///
/// The CLI sends these during session creation/resumption when the session's
/// `SystemMessageConfig` contains sections with `action: "transform"`. For each
/// such section, the CLI provides the current content and expects the SDK to
/// return the (possibly modified) content.
///
/// Implement this trait and pass it to [`Client::create_session`] /
/// [`Client::resume_session`] to participate in system message customization.
///
/// # Example
///
/// ```ignore
/// struct MyTransform;
///
/// #[async_trait::async_trait]
/// impl SystemMessageTransform for MyTransform {
///     fn section_ids(&self) -> Vec<String> {
///         vec!["instructions".to_string()]
///     }
///
///     async fn transform_section(
///         &self,
///         _section_id: &str,
///         content: &str,
///         _ctx: TransformContext,
///     ) -> Option<String> {
///         Some(format!("{content}\n\nAlways be concise."))
///     }
/// }
/// ```
#[async_trait]
pub trait SystemMessageTransform: Send + Sync + 'static {
    /// Section IDs this transform handles.
    ///
    /// The SDK injects `action: "transform"` entries into the
    /// [`SystemMessageConfig`](crate::types::SystemMessageConfig) wire format
    /// for each returned ID.
    fn section_ids(&self) -> Vec<String>;

    /// Transform a section's content. Return `Some(new_content)` to modify the
    /// section, or `None` to pass through unchanged.
    async fn transform_section(
        &self,
        section_id: &str,
        content: &str,
        ctx: TransformContext,
    ) -> Option<String>;
}

/// Wire format for a single section in the transform request/response.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub(crate) struct TransformSection {
    pub(crate) content: String,
}

/// Wire format for the `systemMessage.transform` response.
#[derive(Debug, Clone, Serialize)]
pub(crate) struct TransformResponse {
    pub(crate) sections: HashMap<String, TransformSection>,
}

/// Apply transforms to the incoming sections map, returning the response.
///
/// For each section, calls the matching transform if the implementor returns
/// `Some`; otherwise passes through the original content.
pub(crate) async fn dispatch_transform(
    transform: &dyn SystemMessageTransform,
    session_id: &str,
    sections: HashMap<String, TransformSection>,
) -> TransformResponse {
    let ctx = TransformContext {
        session_id: session_id.into(),
    };

    let mut result = HashMap::with_capacity(sections.len());
    for (section_id, data) in sections {
        let content = match transform
            .transform_section(&section_id, &data.content, ctx.clone())
            .await
        {
            Some(transformed) => transformed,
            None => data.content,
        };
        result.insert(section_id, TransformSection { content });
    }

    TransformResponse { sections: result }
}

#[cfg(test)]
mod tests {
    use super::*;

    struct TestTransform;

    #[async_trait]
    impl SystemMessageTransform for TestTransform {
        fn section_ids(&self) -> Vec<String> {
            vec!["instructions".to_string(), "context".to_string()]
        }

        async fn transform_section(
            &self,
            section_id: &str,
            content: &str,
            _ctx: TransformContext,
        ) -> Option<String> {
            match section_id {
                "instructions" => Some(format!("[modified] {content}")),
                _ => None,
            }
        }
    }

    #[tokio::test]
    async fn dispatch_applies_matching_transform() {
        let transform = TestTransform;
        let mut sections = HashMap::new();
        sections.insert(
            "instructions".to_string(),
            TransformSection {
                content: "be helpful".to_string(),
            },
        );

        let response = dispatch_transform(&transform, "sess-1", sections).await;
        assert_eq!(
            response.sections["instructions"].content,
            "[modified] be helpful"
        );
    }

    #[tokio::test]
    async fn dispatch_passes_through_unhandled_section() {
        let transform = TestTransform;
        let mut sections = HashMap::new();
        sections.insert(
            "context".to_string(),
            TransformSection {
                content: "original context".to_string(),
            },
        );

        let response = dispatch_transform(&transform, "sess-1", sections).await;
        assert_eq!(response.sections["context"].content, "original context");
    }

    #[tokio::test]
    async fn dispatch_unknown_section_passes_through() {
        let transform = TestTransform;
        let mut sections = HashMap::new();
        sections.insert(
            "unknown".to_string(),
            TransformSection {
                content: "mystery".to_string(),
            },
        );

        let response = dispatch_transform(&transform, "sess-1", sections).await;
        assert_eq!(response.sections["unknown"].content, "mystery");
    }

    #[tokio::test]
    async fn dispatch_mixed_sections() {
        let transform = TestTransform;
        let mut sections = HashMap::new();
        sections.insert(
            "instructions".to_string(),
            TransformSection {
                content: "help me".to_string(),
            },
        );
        sections.insert(
            "context".to_string(),
            TransformSection {
                content: "some context".to_string(),
            },
        );
        sections.insert(
            "other".to_string(),
            TransformSection {
                content: "other stuff".to_string(),
            },
        );

        let response = dispatch_transform(&transform, "sess-1", sections).await;
        assert_eq!(
            response.sections["instructions"].content,
            "[modified] help me"
        );
        assert_eq!(response.sections["context"].content, "some context");
        assert_eq!(response.sections["other"].content, "other stuff");
    }

    #[tokio::test]
    async fn section_ids_returns_registered_sections() {
        let transform = TestTransform;
        let ids = transform.section_ids();
        assert_eq!(ids, vec!["instructions", "context"]);
    }
}
