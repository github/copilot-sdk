//! System message transform callbacks for customizing agent prompts.
//!
//! Implement [`SystemMessageTransform`](crate::transforms::SystemMessageTransform) to intercept and modify system prompt
//! sections during session creation. The CLI sends the current content for
//! each section the transform registered, and the SDK returns the modified
//! content.

use std::collections::HashMap;

use async_trait::async_trait;
use serde::{Deserialize, Serialize};

use crate::types::SessionId;

/// Context provided to every transform invocation.
#[derive(Debug, Clone)]
pub struct TransformContext {
    /// The session being created or resumed.
    pub session_id: SessionId,
}

/// Handles `systemMessage.transform` RPC requests from the CLI.
///
/// The CLI sends these during session creation/resumption when the session's
/// `SystemMessageConfig` contains sections with `action: "transform"`. For each
/// such section, the CLI provides the current content and expects the SDK to
/// return the (possibly modified) content.
///
/// Implement this trait and pass it to [`Client::create_session`](crate::Client::create_session) /
/// [`Client::resume_session`](crate::Client::resume_session) to participate in system message customization.
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
    session_id: &SessionId,
    sections: HashMap<String, TransformSection>,
) -> TransformResponse {
    let ctx = TransformContext {
        session_id: session_id.clone(),
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
mod tests;
