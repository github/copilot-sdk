//! Private app-extension principal and capability registration types.
//!
//! This module supports app-bundled integrations and is not a stable public SDK surface.

use std::collections::HashSet;
use std::sync::Arc;

use async_trait::async_trait;
use serde::Serialize;

pub use crate::generated::api_types::{
    AppCanvasActionDescriptor, AppCanvasActionDescriptorVariant, AppCanvasContext,
    AppCanvasOpenResult, AppCanvasProjectContext, AppMediatedFetchHttpRequest,
    AppMediatedFetchHttpRequestMethod, AppMediatedFetchResponse, AppSessionActionResult,
    AppSessionPresentationTarget,
};
use crate::generated::api_types::{
    AppCanvasHostActionRequest, AppCanvasHostCloseRequest, AppCanvasHostOpenRequest,
    AppExtensionContributionPoint as WireContributionPoint, AppExtensionRegisterRequest,
    AppExtensionRegisterResult as WireRegisterResult, AppForgeHostInvokeRequest,
    AppMediatedFetchHostRequest as WireMediatedFetchHostRequest, AppSessionActionHostRequest,
    AppSessionPullRequestActionInvocation, AppSessionPullRequestActionInvocationKind, rpc_methods,
};
use crate::session::Session;
use crate::types::SessionId;
use crate::{Client, Error, ErrorKind, JsonRpcRequest, JsonRpcResponse, error_codes};

const PROTOCOL_VERSION: u64 = 1;
const MAX_ID_LENGTH: usize = 256;
const MAX_OPERATION_LENGTH: usize = 256;
const MAX_CANVAS_INSTANCE_ID_LENGTH: usize = 256;
const MAX_FETCH_PATH_LENGTH: usize = 8192;
const MAX_FETCH_HEADERS: usize = 64;
const MAX_FETCH_HEADER_BYTES: usize = 32 * 1024;
const MAX_FETCH_BODY_BYTES: usize = 256 * 1024;
const MAX_FETCH_RESPONSE_BODY_BYTES: usize = 1024 * 1024;
const MAX_ACTION_PROMPT_BYTES: usize = 32 * 1024;

/// Constrained HTTP method accepted by mediated fetch.
pub type AppMediatedFetchMethod = AppMediatedFetchHttpRequestMethod;

/// Opaque identity for an allowlisted app-extension package.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AppExtensionPackageId(String);

/// Opaque identity for one app-extension launch generation.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AppExtensionActivationId(String);

/// Opaque identity for one principal-owned contribution.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AppExtensionContributionId(String);

/// Capability contribution point declared by a trusted app-extension manifest.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum AppExtensionContributionPoint {
    /// Session badge contribution.
    SessionBadges,
    /// Future app-canvas contribution.
    Canvases,
    /// Future forge-provider contribution.
    ForgeProvider,
}

/// Runtime-authenticated identity of one statically declared contribution.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AppExtensionDeclaredContribution {
    contribution_point: AppExtensionContributionPoint,
    contribution_id: AppExtensionContributionId,
}

/// Runtime-authenticated package and activation identity.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AppExtensionPrincipal {
    package_id: AppExtensionPackageId,
    activation_id: AppExtensionActivationId,
}

/// Capability grants bound to an authenticated principal.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct AppExtensionCapabilityGrants {
    session_badges: bool,
    canvases: bool,
    forge_provider: bool,
    mediated_fetch: bool,
}

/// Identity attached to a capability contribution.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AppExtensionContributionIdentity {
    principal: AppExtensionPrincipal,
    contribution_id: AppExtensionContributionId,
}

/// Authenticated principal registration returned by the runtime.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AppExtensionRegistration {
    principal: AppExtensionPrincipal,
    capabilities: AppExtensionCapabilityGrants,
    contributions: Vec<AppExtensionDeclaredContribution>,
}

/// Opaque target for one runtime-authenticated app-canvas contribution.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AppCanvasTarget {
    principal: AppExtensionPrincipal,
    contribution_id: AppExtensionContributionId,
}

/// Opaque target for one runtime-authenticated forge-provider contribution.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AppForgeProviderTarget {
    principal: AppExtensionPrincipal,
    contribution_id: AppExtensionContributionId,
}

/// Typed request for opening an app-scoped canvas.
#[derive(Debug, Clone)]
pub struct AppCanvasOpenRequest {
    /// Target contribution.
    pub target: AppCanvasTarget,
    /// App-owned canvas instance identity.
    pub instance_id: String,
    /// Optional serializable input supplied by the app.
    pub input: Option<serde_json::Value>,
    /// Optional trusted project/workspace context.
    pub context: Option<AppCanvasContext>,
}

/// Typed request for invoking an app-scoped canvas action.
#[derive(Debug, Clone)]
pub struct AppCanvasActionRequest {
    /// Target contribution.
    pub target: AppCanvasTarget,
    /// App-owned canvas instance identity.
    pub instance_id: String,
    /// Action name declared by the canvas contract.
    pub action_name: String,
    /// Optional serializable action input.
    pub input: Option<serde_json::Value>,
    /// Optional trusted project/workspace context.
    pub context: Option<AppCanvasContext>,
}

/// Typed request for closing an app-scoped canvas.
#[derive(Debug, Clone)]
pub struct AppCanvasCloseRequest {
    /// Target contribution.
    pub target: AppCanvasTarget,
    /// App-owned canvas instance identity.
    pub instance_id: String,
    /// Optional trusted project/workspace context.
    pub context: Option<AppCanvasContext>,
}

/// Typed request for invoking a forge-provider operation.
#[derive(Debug, Clone)]
pub struct AppForgeInvokeRequest {
    /// Target contribution.
    pub target: AppForgeProviderTarget,
    /// Provider-defined operation name.
    pub operation: String,
    /// Optional app-owned forge account identity.
    pub account_id: Option<String>,
    /// Optional serializable operation input.
    pub input: Option<serde_json::Value>,
}

/// Typed request for invoking a Create Pull Request action.
#[derive(Debug, Clone)]
pub struct AppSessionBadgeActionRequest {
    /// Runtime-authenticated session-badge contribution identity.
    pub target: AppExtensionContributionIdentity,
    /// Exact app-visible target from the current eligible-session snapshot.
    pub session: AppSessionPresentationTarget,
    /// Whether the user selected draft pull request creation.
    pub draft: bool,
}

/// Validated mediated-fetch effect delivered to the trusted app host.
#[derive(Debug, Clone)]
pub struct AppMediatedFetchRequest {
    /// Runtime-authenticated extension principal.
    pub principal: AppExtensionPrincipal,
    /// Runtime-authenticated forge-provider contribution identity.
    pub contribution_id: AppExtensionContributionId,
    /// App-owned forge account identity authorized by the runtime.
    pub account_id: String,
    /// Provider operation authorizing this request.
    pub operation: String,
    /// Credential-free bounded HTTP request.
    pub request: AppMediatedFetchHttpRequest,
}

/// Session-scoped trusted host implementation for mediated network access.
#[async_trait]
pub trait AppMediatedFetchHandler: Send + Sync + 'static {
    /// Execute one validated mediated request and return a sanitized response.
    async fn fetch(
        &self,
        request: AppMediatedFetchRequest,
    ) -> Result<AppMediatedFetchResponse, Error>;
}

/// App-host controller for routing typed canvas and forge requests.
#[derive(Clone)]
pub struct AppExtensionsHost {
    client: Client,
    app_session_id: SessionId,
}

impl AppExtensionRegistration {
    /// Return the authenticated principal.
    pub fn principal(&self) -> &AppExtensionPrincipal {
        &self.principal
    }

    /// Return the runtime-granted capabilities.
    pub fn capabilities(&self) -> AppExtensionCapabilityGrants {
        self.capabilities
    }

    /// Return the identity of the activation's single badge contribution.
    pub fn session_badges_identity(&self) -> Result<AppExtensionContributionIdentity, Error> {
        let mut declarations = self.contributions.iter().filter(|contribution| {
            contribution.contribution_point == AppExtensionContributionPoint::SessionBadges
        });
        let Some(declaration) = declarations.next() else {
            return Err(invalid_registration(
                "expected exactly one sessionBadges contribution; received 0".to_string(),
            ));
        };
        if declarations.next().is_some() {
            let count = self
                .contributions
                .iter()
                .filter(|contribution| {
                    contribution.contribution_point == AppExtensionContributionPoint::SessionBadges
                })
                .count();
            return Err(invalid_registration(format!(
                "expected exactly one sessionBadges contribution; received {count}"
            )));
        }
        Ok(AppExtensionContributionIdentity {
            principal: self.principal.clone(),
            contribution_id: declaration.contribution_id.clone(),
        })
    }
}

impl AppExtensionPrincipal {
    /// Create an opaque principal target from trusted runtime/app metadata.
    pub fn new(package_id: impl Into<String>, activation_id: impl Into<String>) -> Self {
        Self {
            package_id: AppExtensionPackageId(package_id.into()),
            activation_id: AppExtensionActivationId(activation_id.into()),
        }
    }

    /// Return the opaque package identity.
    pub fn package_id(&self) -> &AppExtensionPackageId {
        &self.package_id
    }

    /// Return the opaque activation identity.
    pub fn activation_id(&self) -> &AppExtensionActivationId {
        &self.activation_id
    }
}

impl AppExtensionPackageId {
    /// Return the identity for logging or equality comparison.
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl AppExtensionActivationId {
    /// Return the identity for logging or equality comparison.
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl AppExtensionContributionId {
    /// Create an opaque contribution identity from trusted manifest metadata.
    pub fn new(value: impl Into<String>) -> Self {
        Self(value.into())
    }

    /// Return the identity for logging or equality comparison.
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl AppCanvasTarget {
    /// Create a canvas target from trusted principal and manifest metadata.
    pub fn new(
        principal: AppExtensionPrincipal,
        contribution_id: AppExtensionContributionId,
    ) -> Self {
        Self {
            principal,
            contribution_id,
        }
    }

    /// Return the target principal.
    pub fn principal(&self) -> &AppExtensionPrincipal {
        &self.principal
    }

    /// Return the target contribution identity.
    pub fn contribution_id(&self) -> &AppExtensionContributionId {
        &self.contribution_id
    }
}

impl AppForgeProviderTarget {
    /// Create a forge-provider target from trusted principal and manifest metadata.
    pub fn new(
        principal: AppExtensionPrincipal,
        contribution_id: AppExtensionContributionId,
    ) -> Self {
        Self {
            principal,
            contribution_id,
        }
    }

    /// Return the target principal.
    pub fn principal(&self) -> &AppExtensionPrincipal {
        &self.principal
    }

    /// Return the target contribution identity.
    pub fn contribution_id(&self) -> &AppExtensionContributionId {
        &self.contribution_id
    }
}

impl AppExtensionsHost {
    /// Open one app-scoped canvas contribution.
    pub async fn open_canvas(
        &self,
        request: AppCanvasOpenRequest,
    ) -> Result<AppCanvasOpenResult, Error> {
        validate_target(&request.target.principal, &request.target.contribution_id)?;
        validate_bounded(
            &request.instance_id,
            "instanceId",
            MAX_CANVAS_INSTANCE_ID_LENGTH,
        )?;
        self.client
            .rpc()
            .extensions()
            .app_canvas()
            .open(AppCanvasHostOpenRequest {
                activation_id: request.target.principal.activation_id.0,
                app_session_id: self.app_session_id.to_string(),
                context: request.context,
                contribution_id: request.target.contribution_id.0,
                input: request.input,
                instance_id: request.instance_id,
                package_id: request.target.principal.package_id.0,
                protocol_version: serde_json::json!(PROTOCOL_VERSION),
            })
            .await
    }

    /// Invoke one action on an open app-scoped canvas.
    pub async fn invoke_canvas_action(
        &self,
        request: AppCanvasActionRequest,
    ) -> Result<serde_json::Value, Error> {
        validate_target(&request.target.principal, &request.target.contribution_id)?;
        validate_bounded(
            &request.instance_id,
            "instanceId",
            MAX_CANVAS_INSTANCE_ID_LENGTH,
        )?;
        validate_bounded(&request.action_name, "actionName", MAX_OPERATION_LENGTH)?;
        self.client
            .rpc()
            .extensions()
            .app_canvas()
            .action()
            .invoke(AppCanvasHostActionRequest {
                action_name: request.action_name,
                activation_id: request.target.principal.activation_id.0,
                app_session_id: self.app_session_id.to_string(),
                context: request.context,
                contribution_id: request.target.contribution_id.0,
                input: request.input,
                instance_id: request.instance_id,
                package_id: request.target.principal.package_id.0,
                protocol_version: serde_json::json!(PROTOCOL_VERSION),
            })
            .await
    }

    /// Close one app-scoped canvas instance.
    pub async fn close_canvas(&self, request: AppCanvasCloseRequest) -> Result<(), Error> {
        validate_target(&request.target.principal, &request.target.contribution_id)?;
        validate_bounded(
            &request.instance_id,
            "instanceId",
            MAX_CANVAS_INSTANCE_ID_LENGTH,
        )?;
        self.client
            .rpc()
            .extensions()
            .app_canvas()
            .close(AppCanvasHostCloseRequest {
                activation_id: request.target.principal.activation_id.0,
                app_session_id: self.app_session_id.to_string(),
                context: request.context,
                contribution_id: request.target.contribution_id.0,
                instance_id: request.instance_id,
                package_id: request.target.principal.package_id.0,
                protocol_version: serde_json::json!(PROTOCOL_VERSION),
            })
            .await
    }

    /// Invoke one operation on an app-scoped forge-provider contribution.
    pub async fn invoke_forge_provider(
        &self,
        request: AppForgeInvokeRequest,
    ) -> Result<serde_json::Value, Error> {
        validate_target(&request.target.principal, &request.target.contribution_id)?;
        validate_bounded(&request.operation, "operation", MAX_OPERATION_LENGTH)?;
        self.client
            .rpc()
            .extensions()
            .app_forge()
            .invoke(AppForgeHostInvokeRequest {
                account_id: request.account_id,
                activation_id: request.target.principal.activation_id.0,
                app_session_id: self.app_session_id.to_string(),
                contribution_id: request.target.contribution_id.0,
                input: request.input,
                operation: request.operation,
                package_id: request.target.principal.package_id.0,
                protocol_version: serde_json::json!(PROTOCOL_VERSION),
            })
            .await
    }

    /// Invoke one Create Pull Request action on an app-session badge contribution.
    pub async fn invoke_session_badge_action(
        &self,
        request: AppSessionBadgeActionRequest,
    ) -> Result<Option<AppSessionActionResult>, Error> {
        validate_target(&request.target.principal, &request.target.contribution_id)?;
        validate_session_presentation_target(&request.session)?;
        let value = self
            .client
            .rpc()
            .extensions()
            .app_session_badges()
            .action()
            .invoke(AppSessionActionHostRequest {
                action: AppSessionPullRequestActionInvocation {
                    draft: request.draft,
                    kind: AppSessionPullRequestActionInvocationKind::CreatePullRequest,
                },
                activation_id: request.target.principal.activation_id.0,
                app_session_id: self.app_session_id.to_string(),
                contribution_id: request.target.contribution_id.0,
                package_id: request.target.principal.package_id.0,
                protocol_version: serde_json::json!(PROTOCOL_VERSION),
                target: request.session,
            })
            .await?;
        if value.is_null() {
            return Ok(None);
        }
        let result: AppSessionActionResult = serde_json::from_value(value)?;
        validate_session_action_result(&result)?;
        Ok(Some(result))
    }
}

impl Session {
    /// Create the private app-extension host controller for this retained session.
    pub fn app_extensions(&self) -> AppExtensionsHost {
        AppExtensionsHost {
            client: self.client().clone(),
            app_session_id: self.id().clone(),
        }
    }
}

impl AppExtensionCapabilityGrants {
    /// Whether session badge registration is granted.
    pub fn session_badges(&self) -> bool {
        self.session_badges
    }

    /// Whether future app-canvas registration is granted.
    pub fn canvases(&self) -> bool {
        self.canvases
    }

    /// Whether future forge-provider registration is granted.
    pub fn forge_provider(&self) -> bool {
        self.forge_provider
    }

    /// Whether future mediated fetch is granted.
    pub fn mediated_fetch(&self) -> bool {
        self.mediated_fetch
    }
}

impl AppExtensionContributionIdentity {
    /// Return the contribution owner.
    pub fn principal(&self) -> &AppExtensionPrincipal {
        &self.principal
    }

    /// Return the opaque contribution identity.
    pub fn contribution_id(&self) -> &AppExtensionContributionId {
        &self.contribution_id
    }
}

impl AppExtensionDeclaredContribution {
    /// Return the declared contribution point.
    pub fn contribution_point(&self) -> AppExtensionContributionPoint {
        self.contribution_point
    }

    /// Return the opaque contribution identity.
    pub fn contribution_id(&self) -> &AppExtensionContributionId {
        &self.contribution_id
    }
}

#[cfg_attr(not(test), expect(dead_code))]
pub(crate) async fn register(client: &Client) -> Result<AppExtensionRegistration, Error> {
    let result = client
        .rpc()
        .extensions()
        .app_extension()
        .register(AppExtensionRegisterRequest {
            protocol_version: serde_json::json!(PROTOCOL_VERSION),
        })
        .await?;
    parse_registration(result)
}

fn parse_registration(result: WireRegisterResult) -> Result<AppExtensionRegistration, Error> {
    if result.protocol_version != serde_json::json!(PROTOCOL_VERSION) {
        return Err(invalid_registration(format!(
            "unsupported app extension protocol version: {}",
            result.protocol_version
        )));
    }
    if result.principal.package_id.is_empty() {
        return Err(invalid_registration(
            "principal.packageId must be a non-empty string".to_string(),
        ));
    }
    if result.principal.activation_id.is_empty() {
        return Err(invalid_registration(
            "principal.activationId must be a non-empty string".to_string(),
        ));
    }
    let mut seen = HashSet::new();
    let mut contributions = Vec::with_capacity(result.contributions.len());
    for contribution in result.contributions {
        if contribution.contribution_id.is_empty() {
            return Err(invalid_registration(
                "contributionId must be a non-empty string".to_string(),
            ));
        }
        let contribution_point = match contribution.contribution_point {
            WireContributionPoint::SessionBadges => AppExtensionContributionPoint::SessionBadges,
            WireContributionPoint::Canvases => AppExtensionContributionPoint::Canvases,
            WireContributionPoint::ForgeProvider => AppExtensionContributionPoint::ForgeProvider,
            WireContributionPoint::Unknown => {
                return Err(invalid_registration(
                    "unsupported app extension contribution point".to_string(),
                ));
            }
        };
        if !seen.insert((contribution_point, contribution.contribution_id.clone())) {
            return Err(invalid_registration(format!(
                "duplicate app extension contribution identity: {contribution_point:?}/{}",
                contribution.contribution_id
            )));
        }
        contributions.push(AppExtensionDeclaredContribution {
            contribution_point,
            contribution_id: AppExtensionContributionId(contribution.contribution_id),
        });
    }

    Ok(AppExtensionRegistration {
        principal: AppExtensionPrincipal {
            package_id: AppExtensionPackageId(result.principal.package_id),
            activation_id: AppExtensionActivationId(result.principal.activation_id),
        },
        capabilities: AppExtensionCapabilityGrants {
            session_badges: result.capabilities.session_badges == Some(true),
            canvases: result.capabilities.canvases == Some(true),
            forge_provider: result.capabilities.forge_provider == Some(true),
            mediated_fetch: result.capabilities.mediated_fetch == Some(true),
        },
        contributions,
    })
}

fn invalid_registration(message: String) -> Error {
    Error::with_message(ErrorKind::InvalidConfig, message)
}

fn validate_target(
    principal: &AppExtensionPrincipal,
    contribution_id: &AppExtensionContributionId,
) -> Result<(), Error> {
    validate_bounded(principal.package_id.as_str(), "packageId", MAX_ID_LENGTH)?;
    validate_bounded(
        principal.activation_id.as_str(),
        "activationId",
        MAX_ID_LENGTH,
    )?;
    validate_bounded(contribution_id.as_str(), "contributionId", MAX_ID_LENGTH)
}

fn validate_session_presentation_target(
    target: &AppSessionPresentationTarget,
) -> Result<(), Error> {
    validate_bounded(&target.workspace_id, "workspaceId", MAX_ID_LENGTH)?;
    validate_bounded(target.session_id.as_str(), "sessionId", MAX_ID_LENGTH)?;
    validate_bounded(
        &target.repository_path,
        "repositoryPath",
        MAX_ACTION_PROMPT_BYTES,
    )?;
    validate_bounded(
        &target.worktree_path,
        "worktreePath",
        MAX_ACTION_PROMPT_BYTES,
    )?;
    if target
        .branch
        .as_ref()
        .is_some_and(|branch| branch.len() > 4096)
    {
        return Err(invalid_registration(
            "branch must be at most 4096 bytes".to_string(),
        ));
    }
    Ok(())
}

fn validate_session_action_result(result: &AppSessionActionResult) -> Result<(), Error> {
    validate_bounded(&result.prompt, "prompt", MAX_ACTION_PROMPT_BYTES)?;
    if result.prompt.lines().next() != Some("# Pull Request Creation") {
        return Err(invalid_registration(
            "prompt must start with the exact \"# Pull Request Creation\" header".to_string(),
        ));
    }
    validate_bounded(&result.required_tool, "requiredTool", MAX_OPERATION_LENGTH)?;
    if !result
        .required_tool
        .bytes()
        .enumerate()
        .all(|(index, byte)| {
            byte.is_ascii_alphanumeric() || (index > 0 && matches!(byte, b'_' | b'.' | b':' | b'-'))
        })
    {
        return Err(invalid_registration(
            "requiredTool is not a valid tool identifier".to_string(),
        ));
    }
    Ok(())
}

fn validate_non_empty(value: &str, name: &str) -> Result<(), Error> {
    if value.is_empty() {
        return Err(invalid_registration(format!(
            "{name} must be a non-empty string"
        )));
    }
    Ok(())
}

fn validate_bounded(value: &str, name: &str, max_length: usize) -> Result<(), Error> {
    validate_non_empty(value, name)?;
    if value.len() > max_length {
        return Err(invalid_registration(format!(
            "{name} must be at most {max_length} bytes"
        )));
    }
    Ok(())
}

fn validate_mediated_fetch_request(request: &AppMediatedFetchRequest) -> Result<(), Error> {
    validate_target(&request.principal, &request.contribution_id)?;
    validate_bounded(&request.account_id, "accountId", MAX_ID_LENGTH)?;
    validate_bounded(&request.operation, "operation", MAX_OPERATION_LENGTH)?;
    validate_bounded(&request.request.path, "path", MAX_FETCH_PATH_LENGTH)?;
    if matches!(request.request.method, AppMediatedFetchMethod::Unknown) {
        return Err(invalid_registration(
            "unsupported mediated fetch method".to_string(),
        ));
    }
    validate_fetch_path(&request.request.path)?;
    if request.request.headers.as_ref().is_some_and(|headers| {
        headers.len() > MAX_FETCH_HEADERS
            || headers.iter().any(|(name, value)| {
                name.len() + value.len() > MAX_FETCH_HEADER_BYTES
                    || !is_http_header_name(name)
                    || is_sensitive_request_header(name)
                    || value.contains(['\r', '\n'])
            })
            || headers
                .iter()
                .map(|(name, value)| name.len() + value.len())
                .sum::<usize>()
                > MAX_FETCH_HEADER_BYTES
    }) {
        return Err(invalid_registration(
            "mediated fetch headers exceed transport bounds".to_string(),
        ));
    }
    if request
        .request
        .body
        .as_ref()
        .is_some_and(|body| body.len() > MAX_FETCH_BODY_BYTES)
    {
        return Err(invalid_registration(
            "mediated fetch body exceeds transport bounds".to_string(),
        ));
    }
    Ok(())
}

fn validate_mediated_fetch_response(response: &AppMediatedFetchResponse) -> Result<(), Error> {
    if !(100..=599).contains(&response.status) {
        return Err(invalid_registration(
            "mediated fetch response status is invalid".to_string(),
        ));
    }
    if response.headers.len() > MAX_FETCH_HEADERS
        || response.headers.iter().any(|(name, value)| {
            name.len() + value.len() > MAX_FETCH_HEADER_BYTES
                || !is_http_header_name(name)
                || value.contains(['\r', '\n'])
        })
        || response
            .headers
            .iter()
            .map(|(name, value)| name.len() + value.len())
            .sum::<usize>()
            > MAX_FETCH_HEADER_BYTES
    {
        return Err(invalid_registration(
            "mediated fetch response headers exceed transport bounds".to_string(),
        ));
    }
    if response
        .body
        .as_ref()
        .is_some_and(|body| body.len() > MAX_FETCH_RESPONSE_BODY_BYTES)
    {
        return Err(invalid_registration(
            "mediated fetch response body exceeds transport bounds".to_string(),
        ));
    }
    Ok(())
}

fn validate_fetch_path(path: &str) -> Result<(), Error> {
    if !path.starts_with('/') || path.starts_with("//") || path.contains(['\\', '\r', '\n']) {
        return Err(invalid_registration(
            "mediated fetch path must be a credential-free root-relative URL path".to_string(),
        ));
    }
    let path = path.split(['?', '#']).next().unwrap_or(path);
    let mut decoded = Vec::with_capacity(path.len());
    let bytes = path.as_bytes();
    let mut index = 0;
    while index < bytes.len() {
        if bytes[index] == b'%' {
            let Some(high) = bytes.get(index + 1).and_then(|byte| hex_value(*byte)) else {
                return Err(invalid_registration(
                    "mediated fetch path contains invalid percent encoding".to_string(),
                ));
            };
            let Some(low) = bytes.get(index + 2).and_then(|byte| hex_value(*byte)) else {
                return Err(invalid_registration(
                    "mediated fetch path contains invalid percent encoding".to_string(),
                ));
            };
            decoded.push((high << 4) | low);
            index += 3;
        } else {
            decoded.push(bytes[index]);
            index += 1;
        }
    }
    if decoded.contains(&b'\\')
        || decoded
            .split(|byte| *byte == b'/')
            .any(|part| part == b"..")
    {
        return Err(invalid_registration(
            "mediated fetch path must not contain parent traversal segments".to_string(),
        ));
    }
    Ok(())
}

fn hex_value(byte: u8) -> Option<u8> {
    match byte {
        b'0'..=b'9' => Some(byte - b'0'),
        b'a'..=b'f' => Some(byte - b'a' + 10),
        b'A'..=b'F' => Some(byte - b'A' + 10),
        _ => None,
    }
}

fn is_http_header_name(name: &str) -> bool {
    !name.is_empty()
        && name.bytes().all(|byte| {
            byte.is_ascii_alphanumeric()
                || matches!(
                    byte,
                    b'!' | b'#'
                        | b'$'
                        | b'%'
                        | b'&'
                        | b'\''
                        | b'*'
                        | b'+'
                        | b'-'
                        | b'.'
                        | b'^'
                        | b'_'
                        | b'`'
                        | b'|'
                        | b'~'
                )
        })
}

fn is_sensitive_request_header(name: &str) -> bool {
    let name = name.to_ascii_lowercase();
    matches!(
        name.as_str(),
        "authorization" | "cookie" | "host" | "proxy-authorization"
    ) || name.starts_with("x-forwarded-")
}

async fn send_response<T: Serialize>(client: &Client, request_id: u64, result: T) {
    match serde_json::to_value(result) {
        Ok(result) => {
            let _ = client
                .send_response(&JsonRpcResponse {
                    jsonrpc: "2.0".to_string(),
                    id: request_id,
                    result: Some(result),
                    error: None,
                })
                .await;
        }
        Err(error) => {
            send_error(
                client,
                request_id,
                error_codes::INTERNAL_ERROR,
                &format!("failed to serialize mediated fetch response: {error}"),
            )
            .await;
        }
    }
}

async fn send_error(client: &Client, request_id: u64, code: i32, message: &str) {
    let _ = client
        .send_response(&JsonRpcResponse {
            jsonrpc: "2.0".to_string(),
            id: request_id,
            result: None,
            error: Some(crate::JsonRpcError {
                code,
                message: message.to_string(),
                data: None,
            }),
        })
        .await;
}

pub(crate) async fn dispatch_mediated_fetch(
    client: &Client,
    handler: Option<&Arc<dyn AppMediatedFetchHandler>>,
    request: JsonRpcRequest,
) -> bool {
    if request.method != rpc_methods::APPFORGE_FETCH {
        return false;
    }
    let params = request
        .params
        .as_ref()
        .cloned()
        .unwrap_or_else(|| serde_json::Value::Object(serde_json::Map::new()));
    let wire = match serde_json::from_value::<WireMediatedFetchHostRequest>(params) {
        Ok(wire) => wire,
        Err(error) => {
            send_error(
                client,
                request.id,
                error_codes::INVALID_PARAMS,
                &format!("invalid appForge.fetch params: {error}"),
            )
            .await;
            return true;
        }
    };
    if wire.protocol_version != serde_json::json!(PROTOCOL_VERSION) {
        send_error(
            client,
            request.id,
            error_codes::INVALID_PARAMS,
            "unsupported appForge.fetch protocol version",
        )
        .await;
        return true;
    }
    if let Err(error) = validate_non_empty(&wire.session_id, "sessionId") {
        send_error(
            client,
            request.id,
            error_codes::INVALID_PARAMS,
            &error.to_string(),
        )
        .await;
        return true;
    }
    let Some(handler) = handler.cloned() else {
        send_error(
            client,
            request.id,
            error_codes::METHOD_NOT_FOUND,
            "No AppMediatedFetchHandler installed on this session",
        )
        .await;
        return true;
    };
    let effect = AppMediatedFetchRequest {
        principal: AppExtensionPrincipal::new(wire.package_id, wire.activation_id),
        contribution_id: AppExtensionContributionId::new(wire.contribution_id),
        account_id: wire.account_id,
        operation: wire.operation,
        request: wire.request,
    };
    if let Err(error) = validate_mediated_fetch_request(&effect) {
        send_error(
            client,
            request.id,
            error_codes::INVALID_PARAMS,
            &error.to_string(),
        )
        .await;
        return true;
    }
    match handler.fetch(effect).await {
        Ok(response) => {
            if let Err(error) = validate_mediated_fetch_response(&response) {
                send_error(
                    client,
                    request.id,
                    error_codes::INTERNAL_ERROR,
                    &error.to_string(),
                )
                .await;
            } else {
                send_response(client, request.id, response).await;
            }
        }
        Err(error) => {
            send_error(
                client,
                request.id,
                error_codes::INTERNAL_ERROR,
                &error.to_string(),
            )
            .await;
        }
    }
    true
}

#[cfg(test)]
mod tests {
    use std::collections::HashMap;
    use std::path::PathBuf;
    use std::sync::Arc;

    use serde_json::{Value, json};
    use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt, duplex};

    use super::*;

    async fn read_framed(reader: &mut (impl AsyncRead + Unpin)) -> Value {
        let mut header = String::new();
        loop {
            let mut byte = [0u8; 1];
            reader.read_exact(&mut byte).await.unwrap();
            header.push(byte[0] as char);
            if header.ends_with("\r\n\r\n") {
                break;
            }
        }
        let length = header
            .trim()
            .strip_prefix("Content-Length: ")
            .unwrap()
            .parse()
            .unwrap();
        let mut body = vec![0; length];
        reader.read_exact(&mut body).await.unwrap();
        serde_json::from_slice(&body).unwrap()
    }

    async fn write_framed(writer: &mut (impl AsyncWrite + Unpin), value: &Value) {
        let body = serde_json::to_vec(value).unwrap();
        writer
            .write_all(format!("Content-Length: {}\r\n\r\n", body.len()).as_bytes())
            .await
            .unwrap();
        writer.write_all(&body).await.unwrap();
        writer.flush().await.unwrap();
    }

    fn request(id: u64, method: &str, params: Value) -> JsonRpcRequest {
        JsonRpcRequest {
            jsonrpc: "2.0".to_string(),
            id,
            method: method.to_string(),
            params: Some(params),
        }
    }

    #[tokio::test]
    async fn registration_sends_no_spoofable_identity_and_types_the_principal() {
        let (client_write, mut server_read) = duplex(8192);
        let (mut server_write, client_read) = duplex(8192);
        let client =
            Client::from_streams(client_read, client_write, PathBuf::from(r"C:\src")).unwrap();
        let server = tokio::spawn(async move {
            let request = read_framed(&mut server_read).await;
            assert_eq!(request["method"], "extensions.appExtension.register");
            assert_eq!(request["params"], json!({ "protocolVersion": 1 }));
            assert!(request["params"].get("packageId").is_none());
            assert!(request["params"].get("activationId").is_none());
            write_framed(
                &mut server_write,
                &json!({
                    "jsonrpc": "2.0",
                    "id": request["id"],
                    "result": {
                        "protocolVersion": 1,
                        "principal": {
                            "packageId": "bundled:github-app:badges",
                            "activationId": "activation-7"
                        },
                        "capabilities": {
                            "sessionBadges": true
                        },
                        "contributions": [{
                            "contributionPoint": "sessionBadges",
                            "contributionId": "github-pr"
                        }]
                    }
                }),
            )
            .await;
        });

        let registration = register(&client).await.unwrap();
        assert_eq!(
            registration.principal.package_id,
            AppExtensionPackageId("bundled:github-app:badges".to_string())
        );
        assert_eq!(
            registration.principal.activation_id,
            AppExtensionActivationId("activation-7".to_string())
        );
        assert!(registration.capabilities.session_badges);
        assert!(!registration.capabilities.canvases);
        assert!(!registration.capabilities.forge_provider);
        assert!(!registration.capabilities.mediated_fetch);
        assert_eq!(
            registration.session_badges_identity().unwrap(),
            AppExtensionContributionIdentity {
                principal: registration.principal,
                contribution_id: AppExtensionContributionId("github-pr".to_string()),
            }
        );
        server.await.unwrap();
    }

    #[test]
    fn registration_rejects_empty_runtime_principal_identity() {
        let result: WireRegisterResult = serde_json::from_value(json!({
            "protocolVersion": 1,
            "principal": {
                "packageId": "",
                "activationId": "activation-7"
            },
            "capabilities": {
                "sessionBadges": true
            },
            "contributions": [{
                "contributionPoint": "sessionBadges",
                "contributionId": "github-pr"
            }]
        }))
        .unwrap();

        assert!(parse_registration(result).is_err());
    }

    #[test]
    fn registration_rejects_capabilities_as_contribution_points() {
        let result: WireRegisterResult = serde_json::from_value(json!({
            "protocolVersion": 1,
            "principal": {
                "packageId": "package",
                "activationId": "activation"
            },
            "capabilities": {
                "forgeProvider": true,
                "mediatedFetch": true
            },
            "contributions": [{
                "contributionPoint": "mediatedFetch",
                "contributionId": "fetch"
            }]
        }))
        .unwrap();

        assert!(parse_registration(result).is_err());
    }

    #[test]
    fn session_badges_identity_requires_one_trusted_declaration() {
        let registration = AppExtensionRegistration {
            principal: AppExtensionPrincipal {
                package_id: AppExtensionPackageId("package".to_string()),
                activation_id: AppExtensionActivationId("activation".to_string()),
            },
            capabilities: AppExtensionCapabilityGrants {
                session_badges: true,
                canvases: false,
                forge_provider: false,
                mediated_fetch: false,
            },
            contributions: vec![],
        };
        assert!(registration.session_badges_identity().is_err());

        let declaration = AppExtensionDeclaredContribution {
            contribution_point: AppExtensionContributionPoint::SessionBadges,
            contribution_id: AppExtensionContributionId("github-pr".to_string()),
        };
        let registration = AppExtensionRegistration {
            contributions: vec![declaration.clone(), declaration],
            ..registration
        };
        assert!(registration.session_badges_identity().is_err());
    }

    #[tokio::test]
    async fn app_host_routes_canvas_and_forge_requests_with_trusted_identity() {
        let (client_write, mut server_read) = duplex(16384);
        let (mut server_write, client_read) = duplex(16384);
        let client =
            Client::from_streams(client_read, client_write, PathBuf::from(r"C:\src")).unwrap();
        let host = AppExtensionsHost {
            client,
            app_session_id: SessionId::new("hidden-app-session"),
        };
        let principal = AppExtensionPrincipal::new("package", "activation");
        let canvas = AppCanvasTarget::new(
            principal.clone(),
            AppExtensionContributionId::new("repository-overview"),
        );
        let forge = AppForgeProviderTarget::new(
            principal.clone(),
            AppExtensionContributionId::new("github"),
        );
        let badges = AppExtensionContributionIdentity {
            principal,
            contribution_id: AppExtensionContributionId::new("github-pr"),
        };
        let server = tokio::spawn(async move {
            let open = read_framed(&mut server_read).await;
            assert_eq!(open["method"], "extensions.appCanvas.open");
            assert_eq!(
                open["params"],
                json!({
                    "appSessionId": "hidden-app-session",
                    "protocolVersion": 1,
                    "packageId": "package",
                    "activationId": "activation",
                    "contributionId": "repository-overview",
                    "instanceId": "canvas-1",
                    "input": {"tab": "pulls"}
                })
            );
            write_framed(
                &mut server_write,
                &json!({
                    "jsonrpc": "2.0",
                    "id": open["id"],
                    "result": {
                        "state": {"selected": 1},
                        "title": "Repository",
                        "status": "Ready"
                    }
                }),
            )
            .await;

            let action = read_framed(&mut server_read).await;
            assert_eq!(action["method"], "extensions.appCanvas.action.invoke");
            assert_eq!(action["params"]["appSessionId"], "hidden-app-session");
            assert_eq!(action["params"]["actionName"], "select");
            write_framed(
                &mut server_write,
                &json!({
                    "jsonrpc": "2.0",
                    "id": action["id"],
                    "result": {"selected": 2}
                }),
            )
            .await;

            let close = read_framed(&mut server_read).await;
            assert_eq!(close["method"], "extensions.appCanvas.close");
            assert_eq!(close["params"]["instanceId"], "canvas-1");
            write_framed(
                &mut server_write,
                &json!({"jsonrpc": "2.0", "id": close["id"], "result": null}),
            )
            .await;

            let invoke = read_framed(&mut server_read).await;
            assert_eq!(invoke["method"], "extensions.appForge.invoke");
            assert_eq!(
                invoke["params"],
                json!({
                    "appSessionId": "hidden-app-session",
                    "protocolVersion": 1,
                    "packageId": "package",
                    "activationId": "activation",
                    "contributionId": "github",
                    "operation": "getPullRequest",
                    "accountId": "account-1",
                    "input": {"number": 2574}
                })
            );
            write_framed(
                &mut server_write,
                &json!({
                    "jsonrpc": "2.0",
                    "id": invoke["id"],
                    "result": {"number": 2574}
                }),
            )
            .await;

            let create_pr = read_framed(&mut server_read).await;
            assert_eq!(
                create_pr["method"],
                "extensions.appSessionBadges.action.invoke"
            );
            assert_eq!(
                create_pr["params"],
                json!({
                    "appSessionId": "hidden-app-session",
                    "protocolVersion": 1,
                    "packageId": "package",
                    "activationId": "activation",
                    "contributionId": "github-pr",
                    "target": {
                        "workspaceId": "workspace-1",
                        "sessionId": "product-session-1",
                        "repositoryPath": r"C:\src\repo",
                        "worktreePath": r"C:\src\worktree",
                        "branch": "feature"
                    },
                    "action": {
                        "kind": "createPullRequest",
                        "draft": true
                    }
                })
            );
            write_framed(
                &mut server_write,
                &json!({
                    "jsonrpc": "2.0",
                    "id": create_pr["id"],
                    "result": {
                        "prompt": "# Pull Request Creation\nCreate the fake pull request.",
                        "requiredTool": "create_ado_pull_request"
                    }
                }),
            )
            .await;
        });

        let opened = host
            .open_canvas(AppCanvasOpenRequest {
                target: canvas.clone(),
                instance_id: "canvas-1".to_string(),
                input: Some(json!({"tab": "pulls"})),
                context: None,
            })
            .await
            .unwrap();
        assert_eq!(opened.state, Some(json!({"selected": 1})));
        assert_eq!(opened.title.as_deref(), Some("Repository"));
        assert_eq!(opened.status.as_deref(), Some("Ready"));
        assert_eq!(
            host.invoke_canvas_action(AppCanvasActionRequest {
                target: canvas.clone(),
                instance_id: "canvas-1".to_string(),
                action_name: "select".to_string(),
                input: Some(json!({"number": 2})),
                context: None,
            })
            .await
            .unwrap(),
            json!({"selected": 2})
        );
        host.close_canvas(AppCanvasCloseRequest {
            target: canvas,
            instance_id: "canvas-1".to_string(),
            context: None,
        })
        .await
        .unwrap();
        assert_eq!(
            host.invoke_forge_provider(AppForgeInvokeRequest {
                target: forge,
                operation: "getPullRequest".to_string(),
                account_id: Some("account-1".to_string()),
                input: Some(json!({"number": 2574})),
            })
            .await
            .unwrap(),
            json!({"number": 2574})
        );
        let action = host
            .invoke_session_badge_action(AppSessionBadgeActionRequest {
                target: badges,
                session: AppSessionPresentationTarget {
                    branch: Some("feature".to_string()),
                    repository_path: r"C:\src\repo".to_string(),
                    session_id: SessionId::new("product-session-1"),
                    workspace_id: "workspace-1".to_string(),
                    worktree_path: r"C:\src\worktree".to_string(),
                },
                draft: true,
            })
            .await
            .unwrap()
            .unwrap();
        assert_eq!(
            action.prompt,
            "# Pull Request Creation\nCreate the fake pull request."
        );
        assert_eq!(action.required_tool, "create_ado_pull_request");
        server.await.unwrap();
    }

    #[test]
    fn session_action_result_validation_rejects_malformed_values() {
        assert!(
            validate_session_action_result(&AppSessionActionResult {
                prompt: "Missing header".to_string(),
                required_tool: "create_ado_pull_request".to_string(),
            })
            .is_err()
        );
        assert!(
            validate_session_action_result(&AppSessionActionResult {
                prompt: "# Pull Request Creation\nCreate it.".to_string(),
                required_tool: "invalid tool".to_string(),
            })
            .is_err()
        );
        assert!(
            validate_session_action_result(&AppSessionActionResult {
                prompt: format!(
                    "# Pull Request Creation\n{}",
                    "x".repeat(MAX_ACTION_PROMPT_BYTES)
                ),
                required_tool: "create_ado_pull_request".to_string(),
            })
            .is_err()
        );
    }

    struct EchoMediatedFetch;

    #[async_trait]
    impl AppMediatedFetchHandler for EchoMediatedFetch {
        async fn fetch(
            &self,
            request: AppMediatedFetchRequest,
        ) -> Result<AppMediatedFetchResponse, Error> {
            assert_eq!(request.principal.package_id().as_str(), "package");
            assert_eq!(request.principal.activation_id().as_str(), "activation");
            assert_eq!(request.contribution_id.as_str(), "github");
            assert_eq!(request.account_id, "account-1");
            assert_eq!(request.operation, "getPullRequest");
            assert_eq!(request.request.path, "/repos/github/copilot-sdk/pulls/2574");
            Ok(AppMediatedFetchResponse {
                body: Some(r#"{"number":2574}"#.to_string()),
                headers: HashMap::from([(
                    "content-type".to_string(),
                    "application/json".to_string(),
                )]),
                status: 200,
                truncated: false,
            })
        }
    }

    #[tokio::test]
    async fn mediated_fetch_dispatch_rejects_bad_versions_and_recovers() {
        let (client_write, mut server_read) = duplex(16384);
        let (_server_write, client_read) = duplex(16384);
        let client =
            Client::from_streams(client_read, client_write, PathBuf::from(r"C:\src")).unwrap();
        let handler: Arc<dyn AppMediatedFetchHandler> = Arc::new(EchoMediatedFetch);
        let params = json!({
            "sessionId": "hidden-app-session",
            "protocolVersion": 2,
            "packageId": "package",
            "activationId": "activation",
            "contributionId": "github",
            "accountId": "account-1",
            "operation": "getPullRequest",
            "request": {
                "method": "GET",
                "path": "/repos/github/copilot-sdk/pulls/2574"
            }
        });

        assert!(
            dispatch_mediated_fetch(
                &client,
                Some(&handler),
                request(1, rpc_methods::APPFORGE_FETCH, params.clone()),
            )
            .await
        );
        let invalid = read_framed(&mut server_read).await;
        assert_eq!(invalid["error"]["code"], error_codes::INVALID_PARAMS);

        let mut valid_params = params;
        valid_params["protocolVersion"] = json!(1);
        valid_params["request"]["path"] = json!("https://api.github.com/user");
        assert!(
            dispatch_mediated_fetch(
                &client,
                Some(&handler),
                request(2, rpc_methods::APPFORGE_FETCH, valid_params.clone()),
            )
            .await
        );
        let invalid_path = read_framed(&mut server_read).await;
        assert_eq!(invalid_path["error"]["code"], error_codes::INVALID_PARAMS);

        valid_params["request"]["path"] = json!("/repos/github/copilot-sdk/pulls/2574");
        valid_params["request"]["headers"] = json!({"Authorization": "secret"});
        assert!(
            dispatch_mediated_fetch(
                &client,
                Some(&handler),
                request(3, rpc_methods::APPFORGE_FETCH, valid_params.clone()),
            )
            .await
        );
        let invalid_header = read_framed(&mut server_read).await;
        assert_eq!(invalid_header["error"]["code"], error_codes::INVALID_PARAMS);

        valid_params["request"]
            .as_object_mut()
            .unwrap()
            .remove("headers");
        assert!(
            dispatch_mediated_fetch(
                &client,
                Some(&handler),
                request(4, rpc_methods::APPFORGE_FETCH, valid_params),
            )
            .await
        );
        let valid = read_framed(&mut server_read).await;
        assert_eq!(
            valid["result"],
            json!({
                "status": 200,
                "headers": {"content-type": "application/json"},
                "body": "{\"number\":2574}",
                "truncated": false
            })
        );
    }
}
