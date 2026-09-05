/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

//! Data-driven builder for `Windows.UI.Shell.Tasks.AppTaskContent` — the one
//! piece of the Forerunner engine that the Copilot desktop app and the CLI
//! implement *identically*.
//!
//! Both products translate their own domain state into the same fixed sequence
//! of WinRT calls: create a base content shape (a step sequence, a preview
//! thumbnail, a text summary, or a generated-assets grid), then optionally
//! attach a question, action buttons, and a text input. Only the *source* of
//! that data differs — the CLI feeds a flat napi options struct dispatched by a
//! template string; the app feeds domain `AttentionRequest` / activity-timeline
//! state and builds its own deep links. This module captures the common tail as
//! a neutral [`ContentSpec`] plus a `build_content` function so neither
//! product hand-rolls the WinRT call sequence.
//!
//! The spec types are pure data (cross-platform, unit-testable). The functions
//! that actually call WinRT are `#[cfg(windows)]` and operate on the vendored
//! projection in `super::bindings`; each product keeps its own worker thread,
//! object registry, deep-link grammar, and telemetry as a thin adapter that
//! maps its state into a [`ContentSpec`] and calls `build_content`.
//!
//! # Faithfulness
//!
//! `build_content` applies the question, then every button in order, then the
//! text input — the exact order both products use. Because each product only
//! populates the fields it uses (the app never sets both buttons and a text
//! input; the CLI passes through whatever its caller supplied), the union
//! builder reproduces each product's current WinRT calls byte-for-byte. Product
//! specific preparation that is *not* shared — the CLI's template-string
//! dispatch, the app's 12-asset cap, `file://` URI resolution, and per-asset
//! icon choice — stays in each adapter, which hands this module an already
//! resolved [`ContentSpec`].

/// A single generated asset shown in the [`ContentBody::Assets`] result grid.
/// Maps to a `Windows.UI.Shell.Tasks.AppTaskResultAsset`. All URIs are already
/// resolved to forms the shell can open (`ms-appx:///…`, `file:///…`, or an
/// absolute path); resolution and filtering are the caller's responsibility.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ResultAsset {
    /// Display name of the generated asset.
    pub name: String,
    /// Contextual description shown alongside the asset.
    pub context: String,
    /// Icon URI representing the asset (an image asset typically uses itself).
    pub icon_uri: String,
    /// URI launched when the asset is activated.
    pub asset_uri: String,
}

/// The base content shape of a task's hover card, before any interactive
/// elements are attached. Each variant maps 1:1 to an `AppTaskContent::Create*`
/// factory.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ContentBody {
    /// A sequence of completed step labels plus the currently executing step
    /// (`AppTaskContent::CreateSequenceOfSteps`). The common running-progress
    /// shape.
    Steps {
        /// Completed step labels, in order.
        completed_steps: Vec<String>,
        /// The step currently executing (empty string for none).
        current_step: String,
    },
    /// A preview image captioned with the current step
    /// (`AppTaskContent::CreatePreviewThumbnail`).
    Preview {
        /// Preview image URI.
        image_uri: String,
        /// Caption shown under the preview (typically the current step).
        caption: String,
    },
    /// A plain result summary (`AppTaskContent::CreateTextSummaryResult`).
    Summary {
        /// Result summary text.
        text: String,
    },
    /// A grid of generated assets (`AppTaskContent::CreateGeneratedAssetsResult`).
    Assets {
        /// The assets to show, already resolved and capped by the caller.
        assets: Vec<ResultAsset>,
    },
}

/// A clickable button attached to a task's hover card. The shell caps the count
/// (currently 2); this builder applies every button supplied, matching both
/// products, and leaves the cap to the caller/shell.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Button {
    /// Button label.
    pub text: String,
    /// URI launched when the button is clicked.
    pub action_uri: String,
}

/// A single-line text input attached to a task's hover card. Submitted text is
/// substituted into `action_uri_template` by the shell and the result launched.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TextInput {
    /// Placeholder text shown in the empty input.
    pub placeholder: String,
    /// URI template the submitted text is substituted into.
    pub action_uri_template: String,
}

/// Everything needed to build one `AppTaskContent`: a base [`ContentBody`] plus
/// the optional interactive elements attached on top. Interactive fields apply
/// to any body and are what the `NeedsAttention` state uses (it requires a
/// `question`).
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ContentSpec {
    /// The base content shape.
    pub body: ContentBody,
    /// A question shown to the user (required for the `NeedsAttention` state).
    pub question: Option<String>,
    /// Action buttons, applied in order.
    pub buttons: Vec<Button>,
    /// An optional text input.
    pub text_input: Option<TextInput>,
}

impl ContentSpec {
    /// Creates a spec with only a base body and no interactive elements — the
    /// common non-attention case (a running-progress or result card).
    #[must_use]
    pub fn new(body: ContentBody) -> Self {
        Self {
            body,
            question: None,
            buttons: Vec::new(),
            text_input: None,
        }
    }
}

#[cfg(windows)]
mod imp {
    use windows::Foundation::Uri;
    use windows_core::HSTRING;

    use super::super::bindings::Windows::UI::Shell::Tasks::{AppTaskContent, AppTaskResultAsset};
    use super::{ContentBody, ContentSpec, ResultAsset};

    /// Builds a `Windows.Foundation.Uri` from a string. URI validity is the
    /// caller's responsibility (callers supply well-formed `ms-appx:///` /
    /// `file:///` / absolute forms). Identical to the app's `uri` and the CLI's
    /// `make_uri` helpers this replaces.
    pub fn make_uri(value: &str) -> windows_core::Result<Uri> {
        Uri::CreateUri(&HSTRING::from(value))
    }

    /// Builds one `AppTaskResultAsset` from a resolved [`ResultAsset`] spec via
    /// `AppTaskResultAsset::CreateInstance(name, context, icon, asset)`.
    pub fn make_result_asset(asset: &ResultAsset) -> windows_core::Result<AppTaskResultAsset> {
        let icon = make_uri(&asset.icon_uri)?;
        let target = make_uri(&asset.asset_uri)?;
        AppTaskResultAsset::CreateInstance(
            &HSTRING::from(&asset.name),
            &HSTRING::from(&asset.context),
            &icon,
            &target,
        )
    }

    /// Builds the base `AppTaskContent` for the spec's [`ContentBody`], then
    /// attaches the question, buttons (in order), and text input. Reproduces the
    /// exact WinRT call sequence both products currently hand-roll.
    pub fn build_content(spec: &ContentSpec) -> windows_core::Result<AppTaskContent> {
        let content = match &spec.body {
            ContentBody::Steps {
                completed_steps,
                current_step,
            } => {
                let steps: Vec<HSTRING> = completed_steps.iter().map(HSTRING::from).collect();
                AppTaskContent::CreateSequenceOfSteps(&steps, &HSTRING::from(current_step))?
            }
            ContentBody::Preview { image_uri, caption } => {
                let image = make_uri(image_uri)?;
                AppTaskContent::CreatePreviewThumbnail(&image, &HSTRING::from(caption))?
            }
            ContentBody::Summary { text } => {
                AppTaskContent::CreateTextSummaryResult(&HSTRING::from(text))?
            }
            ContentBody::Assets { assets } => {
                let assets: Vec<Option<AppTaskResultAsset>> = assets
                    .iter()
                    .map(|a| make_result_asset(a).map(Some))
                    .collect::<windows_core::Result<Vec<_>>>()?;
                AppTaskContent::CreateGeneratedAssetsResult(&assets)?
            }
        };

        if let Some(question) = &spec.question {
            content.SetQuestion(&HSTRING::from(question))?;
        }
        for button in &spec.buttons {
            let action = make_uri(&button.action_uri)?;
            content.AddButton(&HSTRING::from(&button.text), &action)?;
        }
        if let Some(input) = &spec.text_input {
            content.SetTextInput(
                &HSTRING::from(&input.placeholder),
                &HSTRING::from(&input.action_uri_template),
            )?;
        }

        Ok(content)
    }
}

#[cfg(windows)]
pub use imp::{build_content, make_result_asset, make_uri};

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn new_spec_has_no_interactive_elements() {
        let spec = ContentSpec::new(ContentBody::Summary {
            text: "done".to_owned(),
        });
        assert_eq!(
            spec.body,
            ContentBody::Summary {
                text: "done".to_owned()
            }
        );
        assert!(spec.question.is_none());
        assert!(spec.buttons.is_empty());
        assert!(spec.text_input.is_none());
    }

    #[test]
    fn steps_body_preserves_order_and_current() {
        let body = ContentBody::Steps {
            completed_steps: vec!["a".to_owned(), "b".to_owned()],
            current_step: "c".to_owned(),
        };
        let ContentBody::Steps {
            completed_steps,
            current_step,
        } = body
        else {
            unreachable!()
        };
        assert_eq!(completed_steps, ["a", "b"]);
        assert_eq!(current_step, "c");
    }

    #[test]
    fn attention_spec_carries_question_buttons_and_input() {
        let spec = ContentSpec {
            body: ContentBody::Steps {
                completed_steps: Vec::new(),
                current_step: "Working".to_owned(),
            },
            question: Some("Proceed?".to_owned()),
            buttons: vec![
                Button {
                    text: "Yes".to_owned(),
                    action_uri: "local+copilot://task-response/s?a=yes".to_owned(),
                },
                Button {
                    text: "No".to_owned(),
                    action_uri: "local+copilot://task-response/s?a=no".to_owned(),
                },
            ],
            text_input: Some(TextInput {
                placeholder: "Type a response".to_owned(),
                action_uri_template: "local+copilot://task-response/s?t={input}".to_owned(),
            }),
        };
        assert_eq!(spec.question.as_deref(), Some("Proceed?"));
        assert_eq!(spec.buttons.len(), 2);
        assert_eq!(spec.buttons[0].text, "Yes");
        assert_eq!(
            spec.text_input.as_ref().map(|t| t.placeholder.as_str()),
            Some("Type a response")
        );
    }

    #[test]
    fn assets_body_holds_resolved_specs() {
        let body = ContentBody::Assets {
            assets: vec![ResultAsset {
                name: "diagram.png".to_owned(),
                context: "Generated image".to_owned(),
                icon_uri: "file:///c:/tmp/diagram.png".to_owned(),
                asset_uri: "file:///c:/tmp/diagram.png".to_owned(),
            }],
        };
        let ContentBody::Assets { assets } = body else {
            unreachable!()
        };
        assert_eq!(assets.len(), 1);
        assert_eq!(assets[0].icon_uri, assets[0].asset_uri);
    }
}
