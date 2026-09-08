//! Private app-extension principal and capability registration types.
//!
//! This module supports app-bundled integrations and is not a stable public SDK surface.

use std::collections::HashSet;

use crate::generated::api_types::{
    AppExtensionContributionPoint as WireContributionPoint, AppExtensionRegisterRequest,
    AppExtensionRegisterResult as WireRegisterResult,
};
use crate::{Client, Error, ErrorKind};

const PROTOCOL_VERSION: u64 = 1;

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
    /// Future mediated-fetch contribution.
    MediatedFetch,
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
    /// Return the identity for logging or equality comparison.
    pub fn as_str(&self) -> &str {
        &self.0
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
            WireContributionPoint::MediatedFetch => AppExtensionContributionPoint::MediatedFetch,
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

#[cfg(test)]
mod tests {
    use std::path::PathBuf;

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
}
