/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

#![cfg(test)]

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

    let response = dispatch_transform(&transform, &SessionId::new("sess-1"), sections).await;
    assert_eq!(
        response.sections["instructions"].content,
        "[modified] help me"
    );
    assert_eq!(response.sections["context"].content, "some context");
    assert_eq!(response.sections["other"].content, "other stuff");
}
