//! Auto-generated typed JSON-RPC namespace — do not edit manually.
//!
//! Generated from `api.schema.json` by `scripts/codegen/rust.ts`. The
//! [`ClientRpc`] and [`SessionRpc`] view structs let callers reach every
//! protocol method through a typed namespace tree, so wire method names
//! and request/response shapes live in exactly one place — this file.

#![allow(missing_docs)]
#![allow(clippy::too_many_arguments)]
#![allow(dead_code)]

use super::api_types::{rpc_methods, *};
use super::session_events::SessionMode;
use crate::session::Session;
use crate::{Client, Error};

/// Typed view over the [`Client`]'s server-level RPC namespace.
#[derive(Clone, Copy)]
pub struct ClientRpc<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpc<'a> {
    /// `account.*` sub-namespace.
    pub fn account(&self) -> ClientRpcAccount<'a> {
        ClientRpcAccount {
            client: self.client,
        }
    }

    /// `agentRegistry.*` sub-namespace.
    pub fn agent_registry(&self) -> ClientRpcAgentRegistry<'a> {
        ClientRpcAgentRegistry {
            client: self.client,
        }
    }

    /// `agents.*` sub-namespace.
    pub fn agents(&self) -> ClientRpcAgents<'a> {
        ClientRpcAgents {
            client: self.client,
        }
    }

    /// `instructions.*` sub-namespace.
    pub fn instructions(&self) -> ClientRpcInstructions<'a> {
        ClientRpcInstructions {
            client: self.client,
        }
    }

    /// `mcp.*` sub-namespace.
    pub fn mcp(&self) -> ClientRpcMcp<'a> {
        ClientRpcMcp {
            client: self.client,
        }
    }

    /// `models.*` sub-namespace.
    pub fn models(&self) -> ClientRpcModels<'a> {
        ClientRpcModels {
            client: self.client,
        }
    }

    /// `plugins.*` sub-namespace.
    pub fn plugins(&self) -> ClientRpcPlugins<'a> {
        ClientRpcPlugins {
            client: self.client,
        }
    }

    /// `runtime.*` sub-namespace.
    pub fn runtime(&self) -> ClientRpcRuntime<'a> {
        ClientRpcRuntime {
            client: self.client,
        }
    }

    /// `secrets.*` sub-namespace.
    pub fn secrets(&self) -> ClientRpcSecrets<'a> {
        ClientRpcSecrets {
            client: self.client,
        }
    }

    /// `sessionFs.*` sub-namespace.
    pub fn session_fs(&self) -> ClientRpcSessionFs<'a> {
        ClientRpcSessionFs {
            client: self.client,
        }
    }

    /// `sessions.*` sub-namespace.
    pub fn sessions(&self) -> ClientRpcSessions<'a> {
        ClientRpcSessions {
            client: self.client,
        }
    }

    /// `skills.*` sub-namespace.
    pub fn skills(&self) -> ClientRpcSkills<'a> {
        ClientRpcSkills {
            client: self.client,
        }
    }

    /// `tools.*` sub-namespace.
    pub fn tools(&self) -> ClientRpcTools<'a> {
        ClientRpcTools {
            client: self.client,
        }
    }

    /// `user.*` sub-namespace.
    pub fn user(&self) -> ClientRpcUser<'a> {
        ClientRpcUser {
            client: self.client,
        }
    }

    /// Checks server responsiveness and returns protocol information.
    ///
    /// Wire method: `ping`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional message to echo back to the caller.
    ///
    /// # Returns
    ///
    /// Server liveness response, including the echoed message, current server timestamp, and protocol version.
    pub async fn ping(&self, params: PingRequest) -> Result<PingResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PING, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Performs the SDK server connection handshake and validates the optional connection token. Marked internal because this is JSON-RPC transport plumbing invoked automatically by an SDK client's own `connect()` wrapper, not a user-facing method. Stays internal as long as the SDK client owns the handshake; would only become public if the SDK ever exposed the raw schema surface to consumers without a connection wrapper.
    ///
    /// Wire method: `connect`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional connection token presented by the SDK client during the handshake.
    ///
    /// # Returns
    ///
    /// Handshake result reporting the server's protocol version and package version on success.
    pub(crate) async fn connect(&self, params: ConnectRequest) -> Result<ConnectResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::CONNECT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `account.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcAccount<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcAccount<'a> {
    /// Gets Copilot quota usage for the authenticated user or supplied GitHub token.
    ///
    /// Wire method: `account.getQuota`.
    ///
    /// # Returns
    ///
    /// Quota usage snapshots for the resolved user, keyed by quota type.
    pub async fn get_quota(&self) -> Result<AccountGetQuotaResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::ACCOUNT_GETQUOTA, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Gets Copilot quota usage for the authenticated user or supplied GitHub token.
    ///
    /// Wire method: `account.getQuota`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional GitHub token used to look up quota for a specific user instead of the global auth context.
    ///
    /// # Returns
    ///
    /// Quota usage snapshots for the resolved user, keyed by quota type.
    pub async fn get_quota_with_params(
        &self,
        params: AccountGetQuotaRequest,
    ) -> Result<AccountGetQuotaResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::ACCOUNT_GETQUOTA, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `agentRegistry.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcAgentRegistry<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcAgentRegistry<'a> {
    /// Spawns a managed-server child with the supplied configuration and returns a discriminated-union result. The caller (typically the CLI controller) is responsible for attaching to the spawned child and sending any follow-up prompt. When the controller-local spawn gate is closed the server returns JSON-RPC MethodNotFound.
    ///
    /// Wire method: `agentRegistry.spawn`.
    ///
    /// # Parameters
    ///
    /// * `params` - Inputs to spawn a managed-server child via the controller's spawn delegate.
    ///
    /// # Returns
    ///
    /// Outcome of an agentRegistry.spawn call.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn spawn(
        &self,
        params: AgentRegistrySpawnRequest,
    ) -> Result<AgentRegistrySpawnResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::AGENTREGISTRY_SPAWN, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn spawn(
        &self,
        params: AgentRegistrySpawnRequest,
    ) -> Result<AgentRegistrySpawnResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::AGENTREGISTRY_SPAWN, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `agents.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcAgents<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcAgents<'a> {
    /// Discovers custom agents across user, project, plugin, and remote sources.
    ///
    /// Wire method: `agents.discover`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional project paths to include in agent discovery.
    ///
    /// # Returns
    ///
    /// Agents discovered across user, project, plugin, and remote sources.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn discover(&self, params: AgentsDiscoverRequest) -> Result<ServerAgentList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::AGENTS_DISCOVER, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn discover(
        &self,
        params: AgentsDiscoverRequest,
    ) -> Result<ServerAgentList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::AGENTS_DISCOVER, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns the canonical directories where a client may create custom agents that the runtime will recognize, including ones that do not exist yet. Project directories become active once created.
    ///
    /// Wire method: `agents.getDiscoveryPaths`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional project paths to include when enumerating agent discovery directories.
    ///
    /// # Returns
    ///
    /// Canonical locations where custom agents can be created so the runtime will recognize them.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_discovery_paths(
        &self,
        params: AgentsGetDiscoveryPathsRequest,
    ) -> Result<AgentDiscoveryPathList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::AGENTS_GETDISCOVERYPATHS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_discovery_paths(
        &self,
        params: AgentsGetDiscoveryPathsRequest,
    ) -> Result<AgentDiscoveryPathList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::AGENTS_GETDISCOVERYPATHS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `instructions.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcInstructions<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcInstructions<'a> {
    /// Discovers instruction sources across user, repository, and plugin sources.
    ///
    /// Wire method: `instructions.discover`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional project paths to include in instruction discovery.
    ///
    /// # Returns
    ///
    /// Instruction sources discovered across user, repository, and plugin sources.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn discover(
        &self,
        params: InstructionsDiscoverRequest,
    ) -> Result<ServerInstructionSourceList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::INSTRUCTIONS_DISCOVER, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn discover(
        &self,
        params: InstructionsDiscoverRequest,
    ) -> Result<ServerInstructionSourceList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::INSTRUCTIONS_DISCOVER, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns the canonical files and directories where a client may create custom instructions that the runtime will recognize, including ones that do not exist yet. Repository targets become active once created.
    ///
    /// Wire method: `instructions.getDiscoveryPaths`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional project paths to include when enumerating instruction discovery targets.
    ///
    /// # Returns
    ///
    /// Canonical files and directories where custom instructions can be created so the runtime will recognize them.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_discovery_paths(
        &self,
        params: InstructionsGetDiscoveryPathsRequest,
    ) -> Result<InstructionDiscoveryPathList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::INSTRUCTIONS_GETDISCOVERYPATHS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_discovery_paths(
        &self,
        params: InstructionsGetDiscoveryPathsRequest,
    ) -> Result<InstructionDiscoveryPathList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::INSTRUCTIONS_GETDISCOVERYPATHS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `mcp.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcMcp<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcMcp<'a> {
    /// `mcp.config.*` sub-namespace.
    pub fn config(&self) -> ClientRpcMcpConfig<'a> {
        ClientRpcMcpConfig {
            client: self.client,
        }
    }

    /// Discovers MCP servers from user, workspace, plugin, and builtin sources.
    ///
    /// Wire method: `mcp.discover`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional working directory used as context for MCP server discovery.
    ///
    /// # Returns
    ///
    /// MCP servers discovered from user, workspace, plugin, and built-in sources.
    pub async fn discover(&self, params: McpDiscoverRequest) -> Result<McpDiscoverResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::MCP_DISCOVER, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `mcp.config.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcMcpConfig<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcMcpConfig<'a> {
    /// Lists MCP servers from user configuration.
    ///
    /// Wire method: `mcp.config.list`.
    ///
    /// # Returns
    ///
    /// User-configured MCP servers, keyed by server name.
    pub async fn list(&self) -> Result<McpConfigList, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::MCP_CONFIG_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Adds an MCP server to user configuration.
    ///
    /// Wire method: `mcp.config.add`.
    ///
    /// # Parameters
    ///
    /// * `params` - MCP server name and configuration to add to user configuration.
    pub async fn add(&self, params: McpConfigAddRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::MCP_CONFIG_ADD, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Updates an MCP server in user configuration.
    ///
    /// Wire method: `mcp.config.update`.
    ///
    /// # Parameters
    ///
    /// * `params` - MCP server name and replacement configuration to write to user configuration.
    pub async fn update(&self, params: McpConfigUpdateRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::MCP_CONFIG_UPDATE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Removes an MCP server from user configuration.
    ///
    /// Wire method: `mcp.config.remove`.
    ///
    /// # Parameters
    ///
    /// * `params` - MCP server name to remove from user configuration.
    pub async fn remove(&self, params: McpConfigRemoveRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::MCP_CONFIG_REMOVE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Enables MCP servers in user configuration for new sessions.
    ///
    /// Wire method: `mcp.config.enable`.
    ///
    /// # Parameters
    ///
    /// * `params` - MCP server names to enable for new sessions.
    pub async fn enable(&self, params: McpConfigEnableRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::MCP_CONFIG_ENABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Disables MCP servers in user configuration for new sessions.
    ///
    /// Wire method: `mcp.config.disable`.
    ///
    /// # Parameters
    ///
    /// * `params` - MCP server names to disable for new sessions.
    pub async fn disable(&self, params: McpConfigDisableRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::MCP_CONFIG_DISABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Drops this runtime process's in-memory MCP server-definition cache so the next MCP config read observes disk.
    ///
    /// Wire method: `mcp.config.reload`.
    pub async fn reload(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::MCP_CONFIG_RELOAD, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// `models.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcModels<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcModels<'a> {
    /// Lists Copilot models available to the authenticated user.
    ///
    /// Wire method: `models.list`.
    ///
    /// # Returns
    ///
    /// List of Copilot models available to the resolved user, including capabilities and billing metadata.
    pub async fn list(&self) -> Result<ModelList, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::MODELS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Lists Copilot models available to the authenticated user.
    ///
    /// Wire method: `models.list`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional GitHub token used to list models for a specific user instead of the global auth context.
    ///
    /// # Returns
    ///
    /// List of Copilot models available to the resolved user, including capabilities and billing metadata.
    pub async fn list_with_params(&self, params: ModelsListRequest) -> Result<ModelList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::MODELS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `plugins.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcPlugins<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcPlugins<'a> {
    /// `plugins.marketplaces.*` sub-namespace.
    pub fn marketplaces(&self) -> ClientRpcPluginsMarketplaces<'a> {
        ClientRpcPluginsMarketplaces {
            client: self.client,
        }
    }

    /// Lists plugins installed in user/global state.
    ///
    /// Wire method: `plugins.list`.
    ///
    /// # Returns
    ///
    /// Plugins installed in user/global state.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<PluginListResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<PluginListResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Installs a plugin from a marketplace, GitHub repo, URL, or local path.
    ///
    /// Wire method: `plugins.install`.
    ///
    /// # Parameters
    ///
    /// * `params` - Plugin source and optional working directory for relative-path resolution.
    ///
    /// # Returns
    ///
    /// Result of installing a plugin.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn install(
        &self,
        params: PluginsInstallRequest,
    ) -> Result<PluginInstallResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_INSTALL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn install(
        &self,
        params: PluginsInstallRequest,
    ) -> Result<PluginInstallResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_INSTALL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Uninstalls an installed plugin.
    ///
    /// Wire method: `plugins.uninstall`.
    ///
    /// # Parameters
    ///
    /// * `params` - Name (or spec) of the plugin to uninstall.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn uninstall(&self, params: PluginsUninstallRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_UNINSTALL, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn uninstall(&self, params: PluginsUninstallRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_UNINSTALL, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Updates an installed plugin to its latest published version.
    ///
    /// Wire method: `plugins.update`.
    ///
    /// # Parameters
    ///
    /// * `params` - Name (or spec) of the plugin to update.
    ///
    /// # Returns
    ///
    /// Result of updating a single plugin.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn update(&self, params: PluginsUpdateRequest) -> Result<PluginUpdateResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_UPDATE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn update(
        &self,
        params: PluginsUpdateRequest,
    ) -> Result<PluginUpdateResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_UPDATE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Updates every installed plugin to its latest published version.
    ///
    /// Wire method: `plugins.updateAll`.
    ///
    /// # Returns
    ///
    /// Result of updating all installed plugins.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn update_all(&self) -> Result<PluginUpdateAllResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_UPDATEALL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn update_all(&self) -> Result<PluginUpdateAllResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_UPDATEALL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Enables installed plugins for new sessions.
    ///
    /// Wire method: `plugins.enable`.
    ///
    /// # Parameters
    ///
    /// * `params` - Plugin names (or specs) to enable.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn enable(&self, params: PluginsEnableRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_ENABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn enable(&self, params: PluginsEnableRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_ENABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Disables installed plugins for new sessions.
    ///
    /// Wire method: `plugins.disable`.
    ///
    /// # Parameters
    ///
    /// * `params` - Plugin names (or specs) to disable.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn disable(&self, params: PluginsDisableRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_DISABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn disable(&self, params: PluginsDisableRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_DISABLE, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// `plugins.marketplaces.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcPluginsMarketplaces<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcPluginsMarketplaces<'a> {
    /// Lists all registered marketplaces (defaults + user-added).
    ///
    /// Wire method: `plugins.marketplaces.list`.
    ///
    /// # Returns
    ///
    /// All registered marketplaces, including built-in defaults.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<MarketplaceListResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_MARKETPLACES_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<MarketplaceListResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_MARKETPLACES_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Registers a new marketplace from a source (owner/repo, URL, or local path).
    ///
    /// Wire method: `plugins.marketplaces.add`.
    ///
    /// # Parameters
    ///
    /// * `params` - Marketplace source to register.
    ///
    /// # Returns
    ///
    /// Result of registering a new marketplace.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn add(
        &self,
        params: PluginsMarketplacesAddRequest,
    ) -> Result<MarketplaceAddResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_MARKETPLACES_ADD, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn add(
        &self,
        params: PluginsMarketplacesAddRequest,
    ) -> Result<MarketplaceAddResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_MARKETPLACES_ADD, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Removes a previously-registered marketplace. When the marketplace has dependent plugins and `force` is not set, the marketplace is left intact and the result lists the dependents so the caller can decide whether to retry with `force=true`.
    ///
    /// Wire method: `plugins.marketplaces.remove`.
    ///
    /// # Parameters
    ///
    /// * `params` - Name of the marketplace to remove and an optional force flag.
    ///
    /// # Returns
    ///
    /// Outcome of the remove attempt, including dependent-plugin info when applicable.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn remove(
        &self,
        params: PluginsMarketplacesRemoveRequest,
    ) -> Result<MarketplaceRemoveResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_MARKETPLACES_REMOVE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn remove(
        &self,
        params: PluginsMarketplacesRemoveRequest,
    ) -> Result<MarketplaceRemoveResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_MARKETPLACES_REMOVE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Lists plugins advertised by a registered marketplace.
    ///
    /// Wire method: `plugins.marketplaces.browse`.
    ///
    /// # Parameters
    ///
    /// * `params` - Name of the marketplace whose plugin catalog to fetch.
    ///
    /// # Returns
    ///
    /// Plugins advertised by the marketplace.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn browse(
        &self,
        params: PluginsMarketplacesBrowseRequest,
    ) -> Result<MarketplaceBrowseResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_MARKETPLACES_BROWSE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn browse(
        &self,
        params: PluginsMarketplacesBrowseRequest,
    ) -> Result<MarketplaceBrowseResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_MARKETPLACES_BROWSE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Re-fetches one or all registered marketplace catalogs.
    ///
    /// Wire method: `plugins.marketplaces.refresh`.
    ///
    /// # Returns
    ///
    /// Result of refreshing one or more marketplace catalogs.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn refresh(&self) -> Result<MarketplaceRefreshResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_MARKETPLACES_REFRESH, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn refresh(&self) -> Result<MarketplaceRefreshResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_MARKETPLACES_REFRESH, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Re-fetches one or all registered marketplace catalogs.
    ///
    /// Wire method: `plugins.marketplaces.refresh`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional marketplace name; omit to refresh all.
    ///
    /// # Returns
    ///
    /// Result of refreshing one or more marketplace catalogs.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn refresh_with_params(
        &self,
        params: PluginsMarketplacesRefreshRequest,
    ) -> Result<MarketplaceRefreshResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_MARKETPLACES_REFRESH, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn refresh_with_params(
        &self,
        params: PluginsMarketplacesRefreshRequest,
    ) -> Result<MarketplaceRefreshResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PLUGINS_MARKETPLACES_REFRESH, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `runtime.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcRuntime<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcRuntime<'a> {
    /// Gracefully shuts down an SDK-owned runtime. The response is sent only after cleanup completes; callers may then terminate the owned runtime process.
    ///
    /// Wire method: `runtime.shutdown`.
    pub async fn shutdown(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::RUNTIME_SHUTDOWN, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// `secrets.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcSecrets<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcSecrets<'a> {
    /// Registers secret values for redaction in session logs and exports. The SDK calls this to inject dynamically generated secret values (e.g., OIDC tokens).
    ///
    /// Wire method: `secrets.addFilterValues`.
    ///
    /// # Parameters
    ///
    /// * `params` - Secret values to add to the redaction filter.
    ///
    /// # Returns
    ///
    /// Confirmation that the secret values were registered.
    pub async fn add_filter_values(
        &self,
        params: SecretsAddFilterValuesRequest,
    ) -> Result<SecretsAddFilterValuesResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SECRETS_ADDFILTERVALUES, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `sessionFs.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcSessionFs<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcSessionFs<'a> {
    /// Registers an SDK client as the session filesystem provider.
    ///
    /// Wire method: `sessionFs.setProvider`.
    ///
    /// # Parameters
    ///
    /// * `params` - Initial working directory, session-state path layout, and path conventions used to register the calling SDK client as the session filesystem provider.
    ///
    /// # Returns
    ///
    /// Indicates whether the calling client was registered as the session filesystem provider.
    pub async fn set_provider(
        &self,
        params: SessionFsSetProviderRequest,
    ) -> Result<SessionFsSetProviderResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONFS_SETPROVIDER, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `sessions.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcSessions<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcSessions<'a> {
    /// Creates or resumes a local session and returns the opened session ID.
    ///
    /// Wire method: `sessions.open`.
    ///
    /// # Returns
    ///
    /// Result of opening a session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn open(&self) -> Result<SessionOpenResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_OPEN, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn open(&self) -> Result<SessionOpenResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_OPEN, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Creates a new session by forking persisted history from an existing session.
    ///
    /// Wire method: `sessions.fork`.
    ///
    /// # Parameters
    ///
    /// * `params` - Source session identifier to fork from, optional event-ID boundary, and optional friendly name for the new session.
    ///
    /// # Returns
    ///
    /// Identifier and optional friendly name assigned to the newly forked session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn fork(&self, params: SessionsForkRequest) -> Result<SessionsForkResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_FORK, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn fork(
        &self,
        params: SessionsForkRequest,
    ) -> Result<SessionsForkResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_FORK, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Connects to an existing remote session and exposes it as an SDK session.
    ///
    /// Wire method: `sessions.connect`.
    ///
    /// # Parameters
    ///
    /// * `params` - Remote session connection parameters.
    ///
    /// # Returns
    ///
    /// Remote session connection result.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn connect(
        &self,
        params: ConnectRemoteSessionParams,
    ) -> Result<RemoteSessionConnectionResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_CONNECT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn connect(
        &self,
        params: ConnectRemoteSessionParams,
    ) -> Result<RemoteSessionConnectionResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_CONNECT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Lists sessions, optionally filtered by source and working-directory context. Returned entries are discriminated by `isRemote`: local entries carry only the lightweight `LocalSessionMetadataValue` shape; remote entries carry the full `RemoteSessionMetadataValue` shape (repository, PR number, taskType, etc.).
    ///
    /// Wire method: `sessions.list`.
    ///
    /// # Returns
    ///
    /// Sessions matching the filter, ordered most-recently-modified first.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<SessionList, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<SessionList, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Lists sessions, optionally filtered by source and working-directory context. Returned entries are discriminated by `isRemote`: local entries carry only the lightweight `LocalSessionMetadataValue` shape; remote entries carry the full `RemoteSessionMetadataValue` shape (repository, PR number, taskType, etc.).
    ///
    /// Wire method: `sessions.list`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional source filter, metadata-load limit, and context filter applied to the returned sessions.
    ///
    /// # Returns
    ///
    /// Sessions matching the filter, ordered most-recently-modified first.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list_with_params(
        &self,
        params: SessionsListRequest,
    ) -> Result<SessionList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list_with_params(
        &self,
        params: SessionsListRequest,
    ) -> Result<SessionList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Finds the local session bound to a GitHub task ID, if any.
    ///
    /// Wire method: `sessions.findByTaskId`.
    ///
    /// # Parameters
    ///
    /// * `params` - GitHub task ID to look up.
    ///
    /// # Returns
    ///
    /// ID of the local session bound to the given GitHub task, or omitted when none.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn find_by_task_id(
        &self,
        params: SessionsFindByTaskIDRequest,
    ) -> Result<SessionsFindByTaskIDResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_FINDBYTASKID, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn find_by_task_id(
        &self,
        params: SessionsFindByTaskIDRequest,
    ) -> Result<SessionsFindByTaskIDResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_FINDBYTASKID, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Resolves a UUID prefix to a unique session ID, if exactly one session matches.
    ///
    /// Wire method: `sessions.findByPrefix`.
    ///
    /// # Parameters
    ///
    /// * `params` - UUID prefix to resolve to a unique session ID.
    ///
    /// # Returns
    ///
    /// Session ID matching the prefix, omitted when no unique match exists.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn find_by_prefix(
        &self,
        params: SessionsFindByPrefixRequest,
    ) -> Result<SessionsFindByPrefixResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_FINDBYPREFIX, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn find_by_prefix(
        &self,
        params: SessionsFindByPrefixRequest,
    ) -> Result<SessionsFindByPrefixResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_FINDBYPREFIX, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns the most-relevant prior session for a given working-directory context.
    ///
    /// Wire method: `sessions.getLastForContext`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional working-directory context used to score session relevance.
    ///
    /// # Returns
    ///
    /// Most-relevant session ID for the supplied context, or omitted when no sessions exist.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_last_for_context(
        &self,
        params: SessionsGetLastForContextRequest,
    ) -> Result<SessionsGetLastForContextResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_GETLASTFORCONTEXT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_last_for_context(
        &self,
        params: SessionsGetLastForContextRequest,
    ) -> Result<SessionsGetLastForContextResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_GETLASTFORCONTEXT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Computes the absolute path to a session's persisted events.jsonl file. Internal: filesystem paths are only meaningful in-process (CLI and runtime share a filesystem). Currently used by the CLI's contribution-graph feature to read historical events directly. Remote SDK consumers must not depend on this; a proper event-query API would replace it if the contribution graph ever needed to work over the wire.
    ///
    /// Wire method: `sessions.getEventFilePath`.
    ///
    /// # Parameters
    ///
    /// * `params` - Session ID whose event-log file path to compute.
    ///
    /// # Returns
    ///
    /// Absolute path to the session's events.jsonl file on disk.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn get_event_file_path(
        &self,
        params: SessionsGetEventFilePathRequest,
    ) -> Result<SessionsGetEventFilePathResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_GETEVENTFILEPATH, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns the on-disk byte size of each session's workspace directory.
    ///
    /// Wire method: `sessions.getSizes`.
    ///
    /// # Returns
    ///
    /// Map of sessionId -> on-disk size in bytes for each session's workspace directory.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_sizes(&self) -> Result<SessionSizes, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_GETSIZES, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_sizes(&self) -> Result<SessionSizes, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_GETSIZES, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns the subset of the supplied session IDs that are currently held by another running process.
    ///
    /// Wire method: `sessions.checkInUse`.
    ///
    /// # Parameters
    ///
    /// * `params` - Session IDs to test for live in-use locks.
    ///
    /// # Returns
    ///
    /// Session IDs from the input set that are currently in use by another process.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn check_in_use(
        &self,
        params: SessionsCheckInUseRequest,
    ) -> Result<SessionsCheckInUseResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_CHECKINUSE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn check_in_use(
        &self,
        params: SessionsCheckInUseRequest,
    ) -> Result<SessionsCheckInUseResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_CHECKINUSE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns a session's persisted remote-steerable flag, if any has been recorded. Internal: this is CLI-specific book-keeping used by `--continue` / `--resume` to inherit the prior session's remote-steerable preference. SDK consumers that want similar behavior should manage their own persistence around start/stop calls rather than relying on this runtime-side flag.
    ///
    /// Wire method: `sessions.getPersistedRemoteSteerable`.
    ///
    /// # Parameters
    ///
    /// * `params` - Session ID to look up the persisted remote-steerable flag for.
    ///
    /// # Returns
    ///
    /// The session's persisted remote-steerable flag, or omitted when no value has been persisted.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn get_persisted_remote_steerable(
        &self,
        params: SessionsGetPersistedRemoteSteerableRequest,
    ) -> Result<SessionsGetPersistedRemoteSteerableResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::SESSIONS_GETPERSISTEDREMOTESTEERABLE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Closes a session: emits shutdown, flushes pending events, releases the in-use lock, and disposes the active session.
    ///
    /// Wire method: `sessions.close`.
    ///
    /// # Parameters
    ///
    /// * `params` - Session ID to close.
    ///
    /// # Returns
    ///
    /// Closes a session: emits shutdown, flushes pending events to disk, releases the in-use lock, disposes the active session. Idempotent: succeeds even if the session is not currently active.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn close(&self, params: SessionsCloseRequest) -> Result<SessionsCloseResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_CLOSE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn close(
        &self,
        params: SessionsCloseRequest,
    ) -> Result<SessionsCloseResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_CLOSE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Closes, deactivates, and deletes a set of sessions, returning the bytes freed per session.
    ///
    /// Wire method: `sessions.bulkDelete`.
    ///
    /// # Parameters
    ///
    /// * `params` - Session IDs to close, deactivate, and delete from disk.
    ///
    /// # Returns
    ///
    /// Map of sessionId -> bytes freed by removing the session's workspace directory.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn bulk_delete(
        &self,
        params: SessionsBulkDeleteRequest,
    ) -> Result<SessionBulkDeleteResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_BULKDELETE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn bulk_delete(
        &self,
        params: SessionsBulkDeleteRequest,
    ) -> Result<SessionBulkDeleteResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_BULKDELETE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Deletes sessions older than the given threshold, with optional dry-run and exclusion list.
    ///
    /// Wire method: `sessions.pruneOld`.
    ///
    /// # Parameters
    ///
    /// * `params` - Age threshold and optional flags controlling which old sessions are pruned (or simulated when dryRun is true).
    ///
    /// # Returns
    ///
    /// Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn prune_old(
        &self,
        params: SessionsPruneOldRequest,
    ) -> Result<SessionPruneResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_PRUNEOLD, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn prune_old(
        &self,
        params: SessionsPruneOldRequest,
    ) -> Result<SessionPruneResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_PRUNEOLD, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Flushes a session's pending events to disk.
    ///
    /// Wire method: `sessions.save`.
    ///
    /// # Parameters
    ///
    /// * `params` - Session ID whose pending events should be flushed to disk.
    ///
    /// # Returns
    ///
    /// Flush a session's pending events to disk. No-op when no writer exists for the session (e.g., already closed).
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn save(&self, params: SessionsSaveRequest) -> Result<SessionsSaveResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_SAVE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn save(
        &self,
        params: SessionsSaveRequest,
    ) -> Result<SessionsSaveResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_SAVE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Releases the in-use lock held by this process for a session.
    ///
    /// Wire method: `sessions.releaseLock`.
    ///
    /// # Parameters
    ///
    /// * `params` - Session ID whose in-use lock should be released.
    ///
    /// # Returns
    ///
    /// Release the in-use lock held by this process for the given session. No-op when this process does not currently hold a lock for the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn release_lock(
        &self,
        params: SessionsReleaseLockRequest,
    ) -> Result<SessionsReleaseLockResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_RELEASELOCK, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn release_lock(
        &self,
        params: SessionsReleaseLockRequest,
    ) -> Result<SessionsReleaseLockResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_RELEASELOCK, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Backfills missing summary and context fields on the supplied session metadata records.
    ///
    /// Wire method: `sessions.enrichMetadata`.
    ///
    /// # Parameters
    ///
    /// * `params` - Session metadata records to enrich with summary and context information.
    ///
    /// # Returns
    ///
    /// The enriched metadata records, with summary and context fields backfilled where available. Sessions confirmed empty and unnamed are omitted.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn enrich_metadata(
        &self,
        params: SessionsEnrichMetadataRequest,
    ) -> Result<SessionEnrichMetadataResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_ENRICHMETADATA, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn enrich_metadata(
        &self,
        params: SessionsEnrichMetadataRequest,
    ) -> Result<SessionEnrichMetadataResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_ENRICHMETADATA, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Reloads user, plugin, and (optionally) repo hooks on the active session.
    ///
    /// Wire method: `sessions.reloadPluginHooks`.
    ///
    /// # Parameters
    ///
    /// * `params` - Active session ID and an optional flag for deferring repo-level hooks until folder trust.
    ///
    /// # Returns
    ///
    /// Reload all hooks (user, plugin, optionally repo) and apply them to the active session. Call after installing or removing plugins so their hooks take effect immediately. No-op when no active session matches the given sessionId.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn reload_plugin_hooks(
        &self,
        params: SessionsReloadPluginHooksRequest,
    ) -> Result<SessionsReloadPluginHooksResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_RELOADPLUGINHOOKS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn reload_plugin_hooks(
        &self,
        params: SessionsReloadPluginHooksRequest,
    ) -> Result<SessionsReloadPluginHooksResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_RELOADPLUGINHOOKS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Loads previously-deferred repo-level hooks on the active session, returning queued startup prompts.
    ///
    /// Wire method: `sessions.loadDeferredRepoHooks`.
    ///
    /// # Parameters
    ///
    /// * `params` - Active session ID whose deferred repo-level hooks should be loaded.
    ///
    /// # Returns
    ///
    /// Queued repo-level startup prompts and the total hook command count after loading.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn load_deferred_repo_hooks(
        &self,
        params: SessionsLoadDeferredRepoHooksRequest,
    ) -> Result<SessionLoadDeferredRepoHooksResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::SESSIONS_LOADDEFERREDREPOHOOKS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn load_deferred_repo_hooks(
        &self,
        params: SessionsLoadDeferredRepoHooksRequest,
    ) -> Result<SessionLoadDeferredRepoHooksResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::SESSIONS_LOADDEFERREDREPOHOOKS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Replaces the manager-wide additional plugins registered with the session manager.
    ///
    /// Wire method: `sessions.setAdditionalPlugins`.
    ///
    /// # Parameters
    ///
    /// * `params` - Manager-wide additional plugins to register; replaces any previously-configured set.
    ///
    /// # Returns
    ///
    /// Replace the manager-wide additional plugins. New session creations and subsequent hook reloads see the new set; already-running sessions keep their existing hook installation until the next reload.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set_additional_plugins(
        &self,
        params: SessionsSetAdditionalPluginsRequest,
    ) -> Result<SessionsSetAdditionalPluginsResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::SESSIONS_SETADDITIONALPLUGINS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set_additional_plugins(
        &self,
        params: SessionsSetAdditionalPluginsRequest,
    ) -> Result<SessionsSetAdditionalPluginsResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::SESSIONS_SETADDITIONALPLUGINS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Gets the dynamic-context board entry count associated with a session, when available. Internal: this exists solely so CLI telemetry events (`rem_spawn_gate`, `rem_consolidation_complete`) can pair START / END board counts around the detached rem-agent spawn. "Dynamic context board" is a runtime-internal concept that is not part of the public SDK contract; the long-term plan is to relocate the telemetry emission into the runtime so this method can be deleted entirely.
    ///
    /// Wire method: `sessions.getBoardEntryCount`.
    ///
    /// # Parameters
    ///
    /// * `params` - Session ID whose board entry count should be returned.
    ///
    /// # Returns
    ///
    /// Dynamic-context board entry count, when available.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn get_board_entry_count(
        &self,
        params: SessionsGetBoardEntryCountRequest,
    ) -> Result<SessionsGetBoardEntryCountResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_GETBOARDENTRYCOUNT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Attaches the runtime-managed remote-control singleton to a session, awaiting initial setup. If remote control is already attached to a different session, the singleton is transferred (preserving the underlying Mission Control connection). Returns the final status.
    ///
    /// Wire method: `sessions.startRemoteControl`.
    ///
    /// # Parameters
    ///
    /// * `params` - Parameters for attaching the remote-control singleton to a session.
    ///
    /// # Returns
    ///
    /// Wrapper for the singleton's current status.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn start_remote_control(
        &self,
        params: SessionsStartRemoteControlRequest,
    ) -> Result<RemoteControlStatusResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_STARTREMOTECONTROL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn start_remote_control(
        &self,
        params: SessionsStartRemoteControlRequest,
    ) -> Result<RemoteControlStatusResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_STARTREMOTECONTROL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Atomically rebinds the remote-control singleton to a different session, preserving the underlying Mission Control connection. When `expectedFromSessionId` is provided and does not match the singleton's current `attachedSessionId`, the transfer is rejected with `transferred: false` and the current status is returned unchanged.
    ///
    /// Wire method: `sessions.transferRemoteControl`.
    ///
    /// # Parameters
    ///
    /// * `params` - Parameters for atomically rebinding the remote-control singleton.
    ///
    /// # Returns
    ///
    /// Outcome of a transferRemoteControl call.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn transfer_remote_control(
        &self,
        params: SessionsTransferRemoteControlRequest,
    ) -> Result<RemoteControlTransferResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::SESSIONS_TRANSFERREMOTECONTROL,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn transfer_remote_control(
        &self,
        params: SessionsTransferRemoteControlRequest,
    ) -> Result<RemoteControlTransferResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::SESSIONS_TRANSFERREMOTECONTROL,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Patches the steering state of the active remote-control singleton. When remote control is off, this is a no-op and the off status is returned. Today only `enabled: true` is actionable on the underlying exporter; passing `false` is reserved for future use.
    ///
    /// Wire method: `sessions.setRemoteControlSteering`.
    ///
    /// # Parameters
    ///
    /// * `params` - Patch for the singleton's steering state.
    ///
    /// # Returns
    ///
    /// Wrapper for the singleton's current status.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set_remote_control_steering(
        &self,
        params: SessionsSetRemoteControlSteeringRequest,
    ) -> Result<RemoteControlStatusResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::SESSIONS_SETREMOTECONTROLSTEERING,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set_remote_control_steering(
        &self,
        params: SessionsSetRemoteControlSteeringRequest,
    ) -> Result<RemoteControlStatusResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::SESSIONS_SETREMOTECONTROLSTEERING,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Stops the remote-control singleton. When `expectedSessionId` is provided and does not match the singleton's current `attachedSessionId`, the stop is rejected with `stopped: false` and the current status is returned unchanged (unless `force` is set, in which case the singleton is unconditionally torn down).
    ///
    /// Wire method: `sessions.stopRemoteControl`.
    ///
    /// # Returns
    ///
    /// Outcome of a stopRemoteControl call.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn stop_remote_control(&self) -> Result<RemoteControlStopResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_STOPREMOTECONTROL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn stop_remote_control(&self) -> Result<RemoteControlStopResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_STOPREMOTECONTROL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Stops the remote-control singleton. When `expectedSessionId` is provided and does not match the singleton's current `attachedSessionId`, the stop is rejected with `stopped: false` and the current status is returned unchanged (unless `force` is set, in which case the singleton is unconditionally torn down).
    ///
    /// Wire method: `sessions.stopRemoteControl`.
    ///
    /// # Parameters
    ///
    /// * `params` - Parameters for stopping the remote-control singleton.
    ///
    /// # Returns
    ///
    /// Outcome of a stopRemoteControl call.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn stop_remote_control_with_params(
        &self,
        params: SessionsStopRemoteControlRequest,
    ) -> Result<RemoteControlStopResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_STOPREMOTECONTROL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn stop_remote_control_with_params(
        &self,
        params: SessionsStopRemoteControlRequest,
    ) -> Result<RemoteControlStopResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_STOPREMOTECONTROL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns the current state of the remote-control singleton, including the attached session id and frontend URL when active.
    ///
    /// Wire method: `sessions.getRemoteControlStatus`.
    ///
    /// # Returns
    ///
    /// Wrapper for the singleton's current status.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_remote_control_status(&self) -> Result<RemoteControlStatusResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(
                rpc_methods::SESSIONS_GETREMOTECONTROLSTATUS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_remote_control_status(
        &self,
    ) -> Result<RemoteControlStatusResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(
                rpc_methods::SESSIONS_GETREMOTECONTROLSTATUS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Cursor-based long-poll for sessions spawned by the runtime (e.g. in response to a Mission Control `start_session` command). The cursor is an opaque token; pass it back to receive only spawn events that occurred AFTER the cursor was issued. Omit the cursor on the first call to receive any events buffered since the runtime started. Internal: this is a CLI background-daemon plumbing primitive. SDK consumers that need to react to runtime-spawned sessions should subscribe to a higher-level event stream rather than driving a long-poll loop.
    ///
    /// Wire method: `sessions.pollSpawnedSessions`.
    ///
    /// # Returns
    ///
    /// Batch of spawn events plus a cursor for follow-up polls.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn poll_spawned_sessions(&self) -> Result<PollSpawnedSessionsResult, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_POLLSPAWNEDSESSIONS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Cursor-based long-poll for sessions spawned by the runtime (e.g. in response to a Mission Control `start_session` command). The cursor is an opaque token; pass it back to receive only spawn events that occurred AFTER the cursor was issued. Omit the cursor on the first call to receive any events buffered since the runtime started. Internal: this is a CLI background-daemon plumbing primitive. SDK consumers that need to react to runtime-spawned sessions should subscribe to a higher-level event stream rather than driving a long-poll loop.
    ///
    /// Wire method: `sessions.pollSpawnedSessions`.
    ///
    /// # Parameters
    ///
    /// * `params` - Cursor and optional long-poll wait for polling runtime-spawned sessions.
    ///
    /// # Returns
    ///
    /// Batch of spawn events plus a cursor for follow-up polls.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn poll_spawned_sessions_with_params(
        &self,
        params: SessionsPollSpawnedSessionsRequest,
    ) -> Result<PollSpawnedSessionsResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_POLLSPAWNEDSESSIONS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Registers extension-provided tools on the given session, gated by an optional `enabled` callback. Returns an opaque unsubscribe function the caller must invoke to deregister the tools when the extension is torn down. Marked internal because `loader`, `enabled`, and the returned `unsubscribe` are in-process handles that cannot cross the JSON-RPC boundary. Disappears once extension discovery / launch / tool registration are owned by the runtime: SDK consumers will pass pure config (search paths, disabled ids) via `SessionOptions` and the runtime will resolve, launch, register, and tear down extensions itself.
    ///
    /// Wire method: `sessions.registerExtensionToolsOnSession`.
    ///
    /// # Parameters
    ///
    /// * `params` - Params to attach an extension loader's tools to a session.
    ///
    /// # Returns
    ///
    /// Handle for releasing the extension tool registration.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn register_extension_tools_on_session(
        &self,
        params: RegisterExtensionToolsParams,
    ) -> Result<RegisterExtensionToolsResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::SESSIONS_REGISTEREXTENSIONTOOLSONSESSION,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Attaches (or detaches) an in-process ExtensionController delegate for the given session, used by shared-API surfaces that need to query or modify the session's extension state. Pass `controller: undefined` to detach. Marked internal because the controller is an in-process object that cannot cross the JSON-RPC boundary. Disappears alongside `registerExtensionToolsOnSession`: once the runtime owns extension management, the public surface exposes list/enable/disable/reload as dedicated RPCs served by the runtime.
    ///
    /// Wire method: `sessions.configureSessionExtensions`.
    ///
    /// # Parameters
    ///
    /// * `params` - Params to attach or detach an in-process ExtensionController delegate.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn configure_session_extensions(
        &self,
        params: ConfigureSessionExtensionsParams,
    ) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::SESSIONS_CONFIGURESESSIONEXTENSIONS,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }
}

/// `skills.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcSkills<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcSkills<'a> {
    /// `skills.config.*` sub-namespace.
    pub fn config(&self) -> ClientRpcSkillsConfig<'a> {
        ClientRpcSkillsConfig {
            client: self.client,
        }
    }

    /// Discovers skills across global and project sources.
    ///
    /// Wire method: `skills.discover`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional project paths and additional skill directories to include in discovery.
    ///
    /// # Returns
    ///
    /// Skills discovered across global and project sources.
    pub async fn discover(&self, params: SkillsDiscoverRequest) -> Result<ServerSkillList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SKILLS_DISCOVER, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns the canonical directories where a client may create skills that the runtime will recognize, including ones that do not exist yet. Project directories become active once created.
    ///
    /// Wire method: `skills.getDiscoveryPaths`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional project paths to enumerate.
    ///
    /// # Returns
    ///
    /// Canonical locations where skills can be created so the runtime will recognize them.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_discovery_paths(
        &self,
        params: SkillsGetDiscoveryPathsRequest,
    ) -> Result<SkillDiscoveryPathList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SKILLS_GETDISCOVERYPATHS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_discovery_paths(
        &self,
        params: SkillsGetDiscoveryPathsRequest,
    ) -> Result<SkillDiscoveryPathList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SKILLS_GETDISCOVERYPATHS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `skills.config.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcSkillsConfig<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcSkillsConfig<'a> {
    /// Replaces the global list of disabled skills.
    ///
    /// Wire method: `skills.config.setDisabledSkills`.
    ///
    /// # Parameters
    ///
    /// * `params` - Skill names to mark as disabled in global configuration, replacing any previous list.
    pub async fn set_disabled_skills(
        &self,
        params: SkillsConfigSetDisabledSkillsRequest,
    ) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(
                rpc_methods::SKILLS_CONFIG_SETDISABLEDSKILLS,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }
}

/// `tools.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcTools<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcTools<'a> {
    /// Lists built-in tools available for a model.
    ///
    /// Wire method: `tools.list`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional model identifier whose tool overrides should be applied to the listing.
    ///
    /// # Returns
    ///
    /// Built-in tools available for the requested model, with their parameters and instructions.
    pub async fn list(&self, params: ToolsListRequest) -> Result<ToolList, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::TOOLS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `user.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcUser<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcUser<'a> {
    /// `user.settings.*` sub-namespace.
    pub fn settings(&self) -> ClientRpcUserSettings<'a> {
        ClientRpcUserSettings {
            client: self.client,
        }
    }
}

/// `user.settings.*` RPCs.
#[derive(Clone, Copy)]
pub struct ClientRpcUserSettings<'a> {
    pub(crate) client: &'a Client,
}

impl<'a> ClientRpcUserSettings<'a> {
    /// Drops this runtime process's in-memory user settings cache so the next settings read observes disk.
    ///
    /// Wire method: `user.settings.reload`.
    pub async fn reload(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::USER_SETTINGS_RELOAD, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// Typed view over a [`Session`]'s RPC namespace.
#[derive(Clone, Copy)]
pub struct SessionRpc<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpc<'a> {
    /// `session.agent.*` sub-namespace.
    pub fn agent(&self) -> SessionRpcAgent<'a> {
        SessionRpcAgent {
            session: self.session,
        }
    }

    /// `session.auth.*` sub-namespace.
    pub fn auth(&self) -> SessionRpcAuth<'a> {
        SessionRpcAuth {
            session: self.session,
        }
    }

    /// `session.canvas.*` sub-namespace.
    pub fn canvas(&self) -> SessionRpcCanvas<'a> {
        SessionRpcCanvas {
            session: self.session,
        }
    }

    /// `session.commands.*` sub-namespace.
    pub fn commands(&self) -> SessionRpcCommands<'a> {
        SessionRpcCommands {
            session: self.session,
        }
    }

    /// `session.eventLog.*` sub-namespace.
    pub fn event_log(&self) -> SessionRpcEventLog<'a> {
        SessionRpcEventLog {
            session: self.session,
        }
    }

    /// `session.extensions.*` sub-namespace.
    pub fn extensions(&self) -> SessionRpcExtensions<'a> {
        SessionRpcExtensions {
            session: self.session,
        }
    }

    /// `session.fleet.*` sub-namespace.
    pub fn fleet(&self) -> SessionRpcFleet<'a> {
        SessionRpcFleet {
            session: self.session,
        }
    }

    /// `session.history.*` sub-namespace.
    pub fn history(&self) -> SessionRpcHistory<'a> {
        SessionRpcHistory {
            session: self.session,
        }
    }

    /// `session.instructions.*` sub-namespace.
    pub fn instructions(&self) -> SessionRpcInstructions<'a> {
        SessionRpcInstructions {
            session: self.session,
        }
    }

    /// `session.lsp.*` sub-namespace.
    pub fn lsp(&self) -> SessionRpcLsp<'a> {
        SessionRpcLsp {
            session: self.session,
        }
    }

    /// `session.mcp.*` sub-namespace.
    pub fn mcp(&self) -> SessionRpcMcp<'a> {
        SessionRpcMcp {
            session: self.session,
        }
    }

    /// `session.metadata.*` sub-namespace.
    pub fn metadata(&self) -> SessionRpcMetadata<'a> {
        SessionRpcMetadata {
            session: self.session,
        }
    }

    /// `session.mode.*` sub-namespace.
    pub fn mode(&self) -> SessionRpcMode<'a> {
        SessionRpcMode {
            session: self.session,
        }
    }

    /// `session.model.*` sub-namespace.
    pub fn model(&self) -> SessionRpcModel<'a> {
        SessionRpcModel {
            session: self.session,
        }
    }

    /// `session.name.*` sub-namespace.
    pub fn name(&self) -> SessionRpcName<'a> {
        SessionRpcName {
            session: self.session,
        }
    }

    /// `session.options.*` sub-namespace.
    pub fn options(&self) -> SessionRpcOptions<'a> {
        SessionRpcOptions {
            session: self.session,
        }
    }

    /// `session.permissions.*` sub-namespace.
    pub fn permissions(&self) -> SessionRpcPermissions<'a> {
        SessionRpcPermissions {
            session: self.session,
        }
    }

    /// `session.plan.*` sub-namespace.
    pub fn plan(&self) -> SessionRpcPlan<'a> {
        SessionRpcPlan {
            session: self.session,
        }
    }

    /// `session.plugins.*` sub-namespace.
    pub fn plugins(&self) -> SessionRpcPlugins<'a> {
        SessionRpcPlugins {
            session: self.session,
        }
    }

    /// `session.provider.*` sub-namespace.
    pub fn provider(&self) -> SessionRpcProvider<'a> {
        SessionRpcProvider {
            session: self.session,
        }
    }

    /// `session.queue.*` sub-namespace.
    pub fn queue(&self) -> SessionRpcQueue<'a> {
        SessionRpcQueue {
            session: self.session,
        }
    }

    /// `session.remote.*` sub-namespace.
    pub fn remote(&self) -> SessionRpcRemote<'a> {
        SessionRpcRemote {
            session: self.session,
        }
    }

    /// `session.schedule.*` sub-namespace.
    pub fn schedule(&self) -> SessionRpcSchedule<'a> {
        SessionRpcSchedule {
            session: self.session,
        }
    }

    /// `session.shell.*` sub-namespace.
    pub fn shell(&self) -> SessionRpcShell<'a> {
        SessionRpcShell {
            session: self.session,
        }
    }

    /// `session.skills.*` sub-namespace.
    pub fn skills(&self) -> SessionRpcSkills<'a> {
        SessionRpcSkills {
            session: self.session,
        }
    }

    /// `session.tasks.*` sub-namespace.
    pub fn tasks(&self) -> SessionRpcTasks<'a> {
        SessionRpcTasks {
            session: self.session,
        }
    }

    /// `session.telemetry.*` sub-namespace.
    pub fn telemetry(&self) -> SessionRpcTelemetry<'a> {
        SessionRpcTelemetry {
            session: self.session,
        }
    }

    /// `session.tools.*` sub-namespace.
    pub fn tools(&self) -> SessionRpcTools<'a> {
        SessionRpcTools {
            session: self.session,
        }
    }

    /// `session.ui.*` sub-namespace.
    pub fn ui(&self) -> SessionRpcUi<'a> {
        SessionRpcUi {
            session: self.session,
        }
    }

    /// `session.usage.*` sub-namespace.
    pub fn usage(&self) -> SessionRpcUsage<'a> {
        SessionRpcUsage {
            session: self.session,
        }
    }

    /// `session.workspaces.*` sub-namespace.
    pub fn workspaces(&self) -> SessionRpcWorkspaces<'a> {
        SessionRpcWorkspaces {
            session: self.session,
        }
    }

    /// Suspends the session while preserving persisted state for later resume.
    ///
    /// Wire method: `session.suspend`.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn suspend(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SUSPEND, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn suspend(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SUSPEND, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Sends a user message to the session and returns its message ID.
    ///
    /// Wire method: `session.send`.
    ///
    /// # Parameters
    ///
    /// * `params` - Parameters for sending a user message to the session
    ///
    /// # Returns
    ///
    /// Result of sending a user message
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn send(&self, params: SendRequest) -> Result<SendResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SEND, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn send(&self, params: SendRequest) -> Result<SendResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SEND, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Aborts the current agent turn.
    ///
    /// Wire method: `session.abort`.
    ///
    /// # Parameters
    ///
    /// * `params` - Parameters for aborting the current turn
    ///
    /// # Returns
    ///
    /// Result of aborting the current turn
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn abort(&self, params: AbortRequest) -> Result<AbortResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_ABORT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn abort(&self, params: AbortRequest) -> Result<AbortResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_ABORT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Shuts down the session and persists its final state. Awaits any deferred sessionEnd hooks before resolving so user-supplied hook scripts complete before the runtime tears down.
    ///
    /// Wire method: `session.shutdown`.
    ///
    /// # Parameters
    ///
    /// * `params` - Parameters for shutting down the session
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn shutdown(&self, params: ShutdownRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SHUTDOWN, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn shutdown(&self, params: ShutdownRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SHUTDOWN, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Emits a user-visible session log event.
    ///
    /// Wire method: `session.log`.
    ///
    /// # Parameters
    ///
    /// * `params` - Message text, optional severity level, persistence flag, optional follow-up URL, and optional tip.
    ///
    /// # Returns
    ///
    /// Identifier of the session event that was emitted for the log message.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn log(&self, params: LogRequest) -> Result<LogResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_LOG, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn log(&self, params: LogRequest) -> Result<LogResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_LOG, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.agent.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcAgent<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcAgent<'a> {
    /// Lists custom agents available to the session.
    ///
    /// Wire method: `session.agent.list`.
    ///
    /// # Returns
    ///
    /// Custom agents available to the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<AgentList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AGENT_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<AgentList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AGENT_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Gets the currently selected custom agent for the session.
    ///
    /// Wire method: `session.agent.getCurrent`.
    ///
    /// # Returns
    ///
    /// The currently selected custom agent, or null when using the default agent.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_current(&self) -> Result<AgentGetCurrentResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AGENT_GETCURRENT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_current(&self) -> Result<AgentGetCurrentResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AGENT_GETCURRENT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Selects a custom agent for subsequent turns in the session.
    ///
    /// Wire method: `session.agent.select`.
    ///
    /// # Parameters
    ///
    /// * `params` - Name of the custom agent to select for subsequent turns.
    ///
    /// # Returns
    ///
    /// The newly selected custom agent.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn select(&self, params: AgentSelectRequest) -> Result<AgentSelectResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AGENT_SELECT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn select(
        &self,
        params: AgentSelectRequest,
    ) -> Result<AgentSelectResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AGENT_SELECT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Clears the selected custom agent and returns the session to the default agent.
    ///
    /// Wire method: `session.agent.deselect`.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn deselect(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AGENT_DESELECT, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn deselect(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AGENT_DESELECT, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Reloads custom agent definitions and returns the refreshed list.
    ///
    /// Wire method: `session.agent.reload`.
    ///
    /// # Returns
    ///
    /// Custom agents available to the session after reloading definitions from disk.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn reload(&self) -> Result<AgentReloadResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AGENT_RELOAD, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn reload(&self) -> Result<AgentReloadResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AGENT_RELOAD, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.auth.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcAuth<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcAuth<'a> {
    /// Gets authentication status and account metadata for the session.
    ///
    /// Wire method: `session.auth.getStatus`.
    ///
    /// # Returns
    ///
    /// Authentication status and account metadata for the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_status(&self) -> Result<SessionAuthStatus, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AUTH_GETSTATUS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_status(&self) -> Result<SessionAuthStatus, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AUTH_GETSTATUS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Updates the session's auth credentials used for outbound model and API requests.
    ///
    /// Wire method: `session.auth.setCredentials`.
    ///
    /// # Parameters
    ///
    /// * `params` - New auth credentials to install on the session. Omit to leave credentials unchanged.
    ///
    /// # Returns
    ///
    /// Indicates whether the credential update succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set_credentials(
        &self,
        params: SessionSetCredentialsParams,
    ) -> Result<SessionSetCredentialsResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AUTH_SETCREDENTIALS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set_credentials(
        &self,
        params: SessionSetCredentialsParams,
    ) -> Result<SessionSetCredentialsResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AUTH_SETCREDENTIALS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.canvas.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcCanvas<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcCanvas<'a> {
    /// `session.canvas.action.*` sub-namespace.
    pub fn action(&self) -> SessionRpcCanvasAction<'a> {
        SessionRpcCanvasAction {
            session: self.session,
        }
    }

    /// Lists canvases declared for the session.
    ///
    /// Wire method: `session.canvas.list`.
    ///
    /// # Returns
    ///
    /// Declared canvases available in this session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<CanvasList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_CANVAS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<CanvasList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_CANVAS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Lists currently open canvas instances for the live session.
    ///
    /// Wire method: `session.canvas.listOpen`.
    ///
    /// # Returns
    ///
    /// Live open-canvas snapshot.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list_open(&self) -> Result<CanvasListOpenResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_CANVAS_LISTOPEN, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list_open(&self) -> Result<CanvasListOpenResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_CANVAS_LISTOPEN, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Opens or focuses a canvas instance.
    ///
    /// Wire method: `session.canvas.open`.
    ///
    /// # Parameters
    ///
    /// * `params` - Canvas open parameters.
    ///
    /// # Returns
    ///
    /// Open canvas instance snapshot.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn open(&self, params: CanvasOpenRequest) -> Result<OpenCanvasInstance, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_CANVAS_OPEN, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn open(
        &self,
        params: CanvasOpenRequest,
    ) -> Result<OpenCanvasInstance, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_CANVAS_OPEN, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Closes an open canvas instance.
    ///
    /// Wire method: `session.canvas.close`.
    ///
    /// # Parameters
    ///
    /// * `params` - Canvas close parameters.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn close(&self, params: CanvasCloseRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_CANVAS_CLOSE, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn close(&self, params: CanvasCloseRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_CANVAS_CLOSE, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// `session.canvas.action.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcCanvasAction<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcCanvasAction<'a> {
    /// Invokes an action on an open canvas instance.
    ///
    /// Wire method: `session.canvas.action.invoke`.
    ///
    /// # Parameters
    ///
    /// * `params` - Canvas action invocation parameters.
    ///
    /// # Returns
    ///
    /// Canvas action invocation result.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn invoke(
        &self,
        params: CanvasActionInvokeRequest,
    ) -> Result<CanvasActionInvokeResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_CANVAS_ACTION_INVOKE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn invoke(
        &self,
        params: CanvasActionInvokeRequest,
    ) -> Result<CanvasActionInvokeResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_CANVAS_ACTION_INVOKE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.commands.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcCommands<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcCommands<'a> {
    /// Lists slash commands available in the session.
    ///
    /// Wire method: `session.commands.list`.
    ///
    /// # Returns
    ///
    /// Slash commands available in the session, after applying any include/exclude filters.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<CommandList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_COMMANDS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<CommandList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_COMMANDS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Lists slash commands available in the session.
    ///
    /// Wire method: `session.commands.list`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional filters controlling which command sources to include in the listing.
    ///
    /// # Returns
    ///
    /// Slash commands available in the session, after applying any include/exclude filters.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list_with_params(
        &self,
        params: CommandsListRequest,
    ) -> Result<CommandList, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_COMMANDS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list_with_params(
        &self,
        params: CommandsListRequest,
    ) -> Result<CommandList, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_COMMANDS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Invokes a slash command in the session.
    ///
    /// Wire method: `session.commands.invoke`.
    ///
    /// # Parameters
    ///
    /// * `params` - Slash command name and optional raw input string to invoke.
    ///
    /// # Returns
    ///
    /// Result of invoking the slash command (text output, prompt to send to the agent, or completion).
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn invoke(
        &self,
        params: CommandsInvokeRequest,
    ) -> Result<SlashCommandInvocationResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_COMMANDS_INVOKE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn invoke(
        &self,
        params: CommandsInvokeRequest,
    ) -> Result<SlashCommandInvocationResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_COMMANDS_INVOKE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Reports completion of a pending client-handled slash command.
    ///
    /// Wire method: `session.commands.handlePendingCommand`.
    ///
    /// # Parameters
    ///
    /// * `params` - Pending command request ID and an optional error if the client handler failed.
    ///
    /// # Returns
    ///
    /// Indicates whether the pending client-handled command was completed successfully.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn handle_pending_command(
        &self,
        params: CommandsHandlePendingCommandRequest,
    ) -> Result<CommandsHandlePendingCommandResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_COMMANDS_HANDLEPENDINGCOMMAND,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn handle_pending_command(
        &self,
        params: CommandsHandlePendingCommandRequest,
    ) -> Result<CommandsHandlePendingCommandResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_COMMANDS_HANDLEPENDINGCOMMAND,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Executes a slash command synchronously and returns any error.
    ///
    /// Wire method: `session.commands.execute`.
    ///
    /// # Parameters
    ///
    /// * `params` - Slash command name and argument string to execute synchronously.
    ///
    /// # Returns
    ///
    /// Error message produced while executing the command, if any.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn execute(
        &self,
        params: ExecuteCommandParams,
    ) -> Result<ExecuteCommandResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_COMMANDS_EXECUTE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn execute(
        &self,
        params: ExecuteCommandParams,
    ) -> Result<ExecuteCommandResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_COMMANDS_EXECUTE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Enqueues a slash command for FIFO processing on the local session.
    ///
    /// Wire method: `session.commands.enqueue`.
    ///
    /// # Parameters
    ///
    /// * `params` - Slash-prefixed command string to enqueue for FIFO processing.
    ///
    /// # Returns
    ///
    /// Indicates whether the command was accepted into the local execution queue.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn enqueue(
        &self,
        params: EnqueueCommandParams,
    ) -> Result<EnqueueCommandResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_COMMANDS_ENQUEUE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn enqueue(
        &self,
        params: EnqueueCommandParams,
    ) -> Result<EnqueueCommandResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_COMMANDS_ENQUEUE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Reports whether the host actually executed a queued command and whether to continue processing.
    ///
    /// Wire method: `session.commands.respondToQueuedCommand`.
    ///
    /// # Parameters
    ///
    /// * `params` - Queued-command request ID and the result indicating whether the host executed it (and whether to stop processing further queued commands).
    ///
    /// # Returns
    ///
    /// Indicates whether the queued-command response was matched to a pending request.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn respond_to_queued_command(
        &self,
        params: CommandsRespondToQueuedCommandRequest,
    ) -> Result<CommandsRespondToQueuedCommandResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_COMMANDS_RESPONDTOQUEUEDCOMMAND,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn respond_to_queued_command(
        &self,
        params: CommandsRespondToQueuedCommandRequest,
    ) -> Result<CommandsRespondToQueuedCommandResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_COMMANDS_RESPONDTOQUEUEDCOMMAND,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.eventLog.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcEventLog<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcEventLog<'a> {
    /// Reads a batch of session events from a cursor, optionally waiting for new events.
    ///
    /// Wire method: `session.eventLog.read`.
    ///
    /// # Parameters
    ///
    /// * `params` - Cursor, batch size, and optional long-poll/filter parameters for reading session events.
    ///
    /// # Returns
    ///
    /// Batch of session events returned by a read, with cursor and continuation metadata.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn read(&self, params: EventLogReadRequest) -> Result<EventsReadResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EVENTLOG_READ, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn read(
        &self,
        params: EventLogReadRequest,
    ) -> Result<EventsReadResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EVENTLOG_READ, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns a snapshot of the current tail cursor without consuming events.
    ///
    /// Wire method: `session.eventLog.tail`.
    ///
    /// # Returns
    ///
    /// Snapshot of the current tail cursor without returning any events. Use this when a consumer wants to subscribe to live events going forward without first paginating through the entire persisted history (which would happen if `read` were called without a cursor on a long-lived session).
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn tail(&self) -> Result<EventLogTailResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EVENTLOG_TAIL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn tail(&self) -> Result<EventLogTailResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EVENTLOG_TAIL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Registers consumer interest in an event type for runtime gating purposes.
    ///
    /// Wire method: `session.eventLog.registerInterest`.
    ///
    /// # Parameters
    ///
    /// * `params` - Event type to register consumer interest for, used by runtime gating logic.
    ///
    /// # Returns
    ///
    /// Opaque handle representing an event-type interest registration.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn register_interest(
        &self,
        params: RegisterEventInterestParams,
    ) -> Result<RegisterEventInterestResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_EVENTLOG_REGISTERINTEREST,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn register_interest(
        &self,
        params: RegisterEventInterestParams,
    ) -> Result<RegisterEventInterestResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_EVENTLOG_REGISTERINTEREST,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Releases a consumer's previously-registered interest in an event type.
    ///
    /// Wire method: `session.eventLog.releaseInterest`.
    ///
    /// # Parameters
    ///
    /// * `params` - Opaque handle previously returned by `registerInterest` to release.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn release_interest(
        &self,
        params: ReleaseEventInterestParams,
    ) -> Result<EventLogReleaseInterestResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_EVENTLOG_RELEASEINTEREST,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn release_interest(
        &self,
        params: ReleaseEventInterestParams,
    ) -> Result<EventLogReleaseInterestResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_EVENTLOG_RELEASEINTEREST,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.extensions.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcExtensions<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcExtensions<'a> {
    /// Lists extensions discovered for the session and their current status.
    ///
    /// Wire method: `session.extensions.list`.
    ///
    /// # Returns
    ///
    /// Extensions discovered for the session, with their current status.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<ExtensionList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EXTENSIONS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<ExtensionList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EXTENSIONS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Enables an extension for the session.
    ///
    /// Wire method: `session.extensions.enable`.
    ///
    /// # Parameters
    ///
    /// * `params` - Source-qualified extension identifier to enable for the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn enable(&self, params: ExtensionsEnableRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EXTENSIONS_ENABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn enable(&self, params: ExtensionsEnableRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EXTENSIONS_ENABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Disables an extension for the session.
    ///
    /// Wire method: `session.extensions.disable`.
    ///
    /// # Parameters
    ///
    /// * `params` - Source-qualified extension identifier to disable for the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn disable(&self, params: ExtensionsDisableRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EXTENSIONS_DISABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn disable(&self, params: ExtensionsDisableRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EXTENSIONS_DISABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Reloads extension definitions and processes for the session.
    ///
    /// Wire method: `session.extensions.reload`.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn reload(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EXTENSIONS_RELOAD, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn reload(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EXTENSIONS_RELOAD, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Push attachments into the next user-message turn from an extension. The host should surface them as composer pills and forward them via the next session.send call. Callable only by extension-owned connections.
    ///
    /// Wire method: `session.extensions.sendAttachmentsToMessage`.
    ///
    /// # Parameters
    ///
    /// * `params` - Parameters for session.extensions.sendAttachmentsToMessage.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn send_attachments_to_message(
        &self,
        params: SendAttachmentsToMessageParams,
    ) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_EXTENSIONS_SENDATTACHMENTSTOMESSAGE,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn send_attachments_to_message(
        &self,
        params: SendAttachmentsToMessageParams,
    ) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_EXTENSIONS_SENDATTACHMENTSTOMESSAGE,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }
}

/// `session.fleet.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcFleet<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcFleet<'a> {
    /// Starts fleet mode by submitting the fleet orchestration prompt to the session.
    ///
    /// Wire method: `session.fleet.start`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional user prompt to combine with the fleet orchestration instructions.
    ///
    /// # Returns
    ///
    /// Indicates whether fleet mode was successfully activated.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn start(&self, params: FleetStartRequest) -> Result<FleetStartResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_FLEET_START, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn start(&self, params: FleetStartRequest) -> Result<FleetStartResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_FLEET_START, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.history.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcHistory<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcHistory<'a> {
    /// Compacts the session history to reduce context usage.
    ///
    /// Wire method: `session.history.compact`.
    ///
    /// # Returns
    ///
    /// Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn compact(&self) -> Result<HistoryCompactResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_HISTORY_COMPACT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn compact(&self) -> Result<HistoryCompactResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_HISTORY_COMPACT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Compacts the session history to reduce context usage.
    ///
    /// Wire method: `session.history.compact`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional compaction parameters.
    ///
    /// # Returns
    ///
    /// Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn compact_with_params(
        &self,
        params: HistoryCompactRequest,
    ) -> Result<HistoryCompactResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_HISTORY_COMPACT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn compact_with_params(
        &self,
        params: HistoryCompactRequest,
    ) -> Result<HistoryCompactResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_HISTORY_COMPACT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Truncates persisted session history to a specific event.
    ///
    /// Wire method: `session.history.truncate`.
    ///
    /// # Parameters
    ///
    /// * `params` - Identifier of the event to truncate to; this event and all later events are removed.
    ///
    /// # Returns
    ///
    /// Number of events that were removed by the truncation.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn truncate(
        &self,
        params: HistoryTruncateRequest,
    ) -> Result<HistoryTruncateResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_HISTORY_TRUNCATE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn truncate(
        &self,
        params: HistoryTruncateRequest,
    ) -> Result<HistoryTruncateResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_HISTORY_TRUNCATE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Cancels any in-progress background compaction on a local session.
    ///
    /// Wire method: `session.history.cancelBackgroundCompaction`.
    ///
    /// # Returns
    ///
    /// Indicates whether an in-progress background compaction was cancelled.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn cancel_background_compaction(
        &self,
    ) -> Result<HistoryCancelBackgroundCompactionResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_HISTORY_CANCELBACKGROUNDCOMPACTION,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn cancel_background_compaction(
        &self,
    ) -> Result<HistoryCancelBackgroundCompactionResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_HISTORY_CANCELBACKGROUNDCOMPACTION,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Aborts any in-progress manual compaction on a local session.
    ///
    /// Wire method: `session.history.abortManualCompaction`.
    ///
    /// # Returns
    ///
    /// Indicates whether an in-progress manual compaction was aborted.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn abort_manual_compaction(
        &self,
    ) -> Result<HistoryAbortManualCompactionResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_HISTORY_ABORTMANUALCOMPACTION,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn abort_manual_compaction(
        &self,
    ) -> Result<HistoryAbortManualCompactionResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_HISTORY_ABORTMANUALCOMPACTION,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Produces a markdown summary of the session's conversation context for hand-off scenarios.
    ///
    /// Wire method: `session.history.summarizeForHandoff`.
    ///
    /// # Returns
    ///
    /// Markdown summary of the conversation context (empty when not available).
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn summarize_for_handoff(&self) -> Result<HistorySummarizeForHandoffResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_HISTORY_SUMMARIZEFORHANDOFF,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn summarize_for_handoff(
        &self,
    ) -> Result<HistorySummarizeForHandoffResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_HISTORY_SUMMARIZEFORHANDOFF,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.instructions.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcInstructions<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcInstructions<'a> {
    /// Gets instruction sources loaded for the session.
    ///
    /// Wire method: `session.instructions.getSources`.
    ///
    /// # Returns
    ///
    /// Instruction sources loaded for the session, in merge order.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_sources(&self) -> Result<InstructionsGetSourcesResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_INSTRUCTIONS_GETSOURCES,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_sources(&self) -> Result<InstructionsGetSourcesResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_INSTRUCTIONS_GETSOURCES,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.lsp.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcLsp<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcLsp<'a> {
    /// Loads the merged LSP configuration set for the session's working directory.
    ///
    /// Wire method: `session.lsp.initialize`.
    ///
    /// # Parameters
    ///
    /// * `params` - Parameters for (re)loading the merged LSP configuration set.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn initialize(&self, params: LspInitializeRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_LSP_INITIALIZE, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn initialize(&self, params: LspInitializeRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_LSP_INITIALIZE, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// `session.mcp.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcMcp<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcMcp<'a> {
    /// `session.mcp.apps.*` sub-namespace.
    pub fn apps(&self) -> SessionRpcMcpApps<'a> {
        SessionRpcMcpApps {
            session: self.session,
        }
    }

    /// `session.mcp.oauth.*` sub-namespace.
    pub fn oauth(&self) -> SessionRpcMcpOauth<'a> {
        SessionRpcMcpOauth {
            session: self.session,
        }
    }

    /// Lists MCP servers configured for the session, their connection status, and host-level state. The host-level state (disabled/filtered servers, failed/needs-auth/pending connections, mcp3p policy, full config) is empty/zero when no MCP host has been initialized for the session.
    ///
    /// Wire method: `session.mcp.list`.
    ///
    /// # Returns
    ///
    /// MCP servers configured for the session, with their connection status and host-level state.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<McpServerList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<McpServerList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Lists the tools exposed by a connected MCP server on this session's host.
    ///
    /// Wire method: `session.mcp.listTools`.
    ///
    /// # Parameters
    ///
    /// * `params` - Server name whose tool list should be returned.
    ///
    /// # Returns
    ///
    /// Tools exposed by the connected MCP server. Throws when the server is not connected.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list_tools(
        &self,
        params: McpListToolsRequest,
    ) -> Result<McpListToolsResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_LISTTOOLS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list_tools(
        &self,
        params: McpListToolsRequest,
    ) -> Result<McpListToolsResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_LISTTOOLS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Enables an MCP server for the session.
    ///
    /// Wire method: `session.mcp.enable`.
    ///
    /// # Parameters
    ///
    /// * `params` - Name of the MCP server to enable for the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn enable(&self, params: McpEnableRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_ENABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn enable(&self, params: McpEnableRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_ENABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Disables an MCP server for the session.
    ///
    /// Wire method: `session.mcp.disable`.
    ///
    /// # Parameters
    ///
    /// * `params` - Name of the MCP server to disable for the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn disable(&self, params: McpDisableRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_DISABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn disable(&self, params: McpDisableRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_DISABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Reloads MCP server connections for the session.
    ///
    /// Wire method: `session.mcp.reload`.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn reload(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_RELOAD, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn reload(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_RELOAD, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Reloads MCP server connections for the session with an explicit host-provided configuration.
    ///
    /// Wire method: `session.mcp.reloadWithConfig`.
    ///
    /// # Parameters
    ///
    /// * `params` - Opaque MCP reload configuration.
    ///
    /// # Returns
    ///
    /// MCP server startup filtering result.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn reload_with_config(
        &self,
        params: McpReloadWithConfigRequest,
    ) -> Result<McpStartServersResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_RELOADWITHCONFIG, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Runs an MCP sampling inference on behalf of an MCP server.
    ///
    /// Wire method: `session.mcp.executeSampling`.
    ///
    /// # Parameters
    ///
    /// * `params` - Identifiers and raw MCP CreateMessageRequest params used to run a sampling inference.
    ///
    /// # Returns
    ///
    /// Outcome of an MCP sampling execution: success result, failure error, or cancellation.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn execute_sampling(
        &self,
        params: McpExecuteSamplingParams,
    ) -> Result<McpSamplingExecutionResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_EXECUTESAMPLING, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn execute_sampling(
        &self,
        params: McpExecuteSamplingParams,
    ) -> Result<McpSamplingExecutionResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_EXECUTESAMPLING, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Cancels an in-flight MCP sampling execution by request ID.
    ///
    /// Wire method: `session.mcp.cancelSamplingExecution`.
    ///
    /// # Parameters
    ///
    /// * `params` - The requestId previously passed to executeSampling that should be cancelled.
    ///
    /// # Returns
    ///
    /// Indicates whether an in-flight sampling execution with the given requestId was found and cancelled.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn cancel_sampling_execution(
        &self,
        params: McpCancelSamplingExecutionParams,
    ) -> Result<McpCancelSamplingExecutionResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_MCP_CANCELSAMPLINGEXECUTION,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn cancel_sampling_execution(
        &self,
        params: McpCancelSamplingExecutionParams,
    ) -> Result<McpCancelSamplingExecutionResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_MCP_CANCELSAMPLINGEXECUTION,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Sets how environment-variable values supplied to MCP servers are resolved (direct or indirect).
    ///
    /// Wire method: `session.mcp.setEnvValueMode`.
    ///
    /// # Parameters
    ///
    /// * `params` - Mode controlling how MCP server env values are resolved (`direct` or `indirect`).
    ///
    /// # Returns
    ///
    /// Env-value mode recorded on the session after the update.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set_env_value_mode(
        &self,
        params: McpSetEnvValueModeParams,
    ) -> Result<McpSetEnvValueModeResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_SETENVVALUEMODE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set_env_value_mode(
        &self,
        params: McpSetEnvValueModeParams,
    ) -> Result<McpSetEnvValueModeResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_SETENVVALUEMODE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Removes the auto-managed `github` MCP server when present.
    ///
    /// Wire method: `session.mcp.removeGitHub`.
    ///
    /// # Returns
    ///
    /// Indicates whether the auto-managed `github` MCP server was removed (false when nothing to remove).
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn remove_git_hub(&self) -> Result<McpRemoveGitHubResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_REMOVEGITHUB, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn remove_git_hub(&self) -> Result<McpRemoveGitHubResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_REMOVEGITHUB, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Configures the built-in GitHub MCP server for the session's current auth context.
    ///
    /// Wire method: `session.mcp.configureGitHub`.
    ///
    /// # Parameters
    ///
    /// * `params` - Opaque auth info used to configure GitHub MCP.
    ///
    /// # Returns
    ///
    /// Result of configuring GitHub MCP.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn configure_git_hub(
        &self,
        params: McpConfigureGitHubRequest,
    ) -> Result<McpConfigureGitHubResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_CONFIGUREGITHUB, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Starts an individual MCP server on the session's host.
    ///
    /// Wire method: `session.mcp.startServer`.
    ///
    /// # Parameters
    ///
    /// * `params` - Server name and opaque configuration for an individual MCP server start.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn start_server(&self, params: McpStartServerRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_STARTSERVER, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Restarts an individual MCP server on the session's host (stops then starts).
    ///
    /// Wire method: `session.mcp.restartServer`.
    ///
    /// # Parameters
    ///
    /// * `params` - Server name and opaque configuration for an individual MCP server restart.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn restart_server(
        &self,
        params: McpRestartServerRequest,
    ) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_RESTARTSERVER, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Stops an individual MCP server on the session's host.
    ///
    /// Wire method: `session.mcp.stopServer`.
    ///
    /// # Parameters
    ///
    /// * `params` - Server name for an individual MCP server stop.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn stop_server(&self, params: McpStopServerRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_STOPSERVER, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn stop_server(&self, params: McpStopServerRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_STOPSERVER, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Registers a pre-connected external MCP client (e.g. IDE) on the session's host. The caller retains lifecycle ownership of the client and transport. Marked internal because the `client` and `transport` arguments are in-process MCP SDK instances that cannot be serialized across the JSON-RPC boundary; once the CLI moves on top of the SDK, external clients will be expressed as transport configs the runtime can construct itself.
    ///
    /// Wire method: `session.mcp.registerExternalClient`.
    ///
    /// # Parameters
    ///
    /// * `params` - Registration parameters for an external MCP client.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn register_external_client(
        &self,
        params: McpRegisterExternalClientRequest,
    ) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_MCP_REGISTEREXTERNALCLIENT,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }

    /// Unregisters a previously registered external MCP client by server name. Marked internal as the paired companion of `registerExternalClient`: only in-process callers that registered a client this way can meaningfully unregister it. Disappears alongside `registerExternalClient`: once external clients are described to the runtime as config rather than handed in as instances, lifecycle (including deregistration) is owned entirely by the runtime.
    ///
    /// Wire method: `session.mcp.unregisterExternalClient`.
    ///
    /// # Parameters
    ///
    /// * `params` - Server name identifying the external client to remove.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn unregister_external_client(
        &self,
        params: McpUnregisterExternalClientRequest,
    ) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_MCP_UNREGISTEREXTERNALCLIENT,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }

    /// Checks whether a named MCP server is currently running on the session's host.
    ///
    /// Wire method: `session.mcp.isServerRunning`.
    ///
    /// # Parameters
    ///
    /// * `params` - Server name to check running status for.
    ///
    /// # Returns
    ///
    /// Whether the named MCP server is running.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn is_server_running(
        &self,
        params: McpIsServerRunningRequest,
    ) -> Result<McpIsServerRunningResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_ISSERVERRUNNING, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn is_server_running(
        &self,
        params: McpIsServerRunningRequest,
    ) -> Result<McpIsServerRunningResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_ISSERVERRUNNING, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.mcp.apps.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcMcpApps<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcMcpApps<'a> {
    /// Fetch an MCP resource (typically a `ui://` MCP App bundle, per SEP-1865) from a connected server. Requires the `mcp-apps` session capability.
    ///
    /// Wire method: `session.mcp.apps.readResource`.
    ///
    /// # Parameters
    ///
    /// * `params` - MCP server and resource URI to fetch.
    ///
    /// # Returns
    ///
    /// Resource contents returned by the MCP server.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn read_resource(
        &self,
        params: McpAppsReadResourceRequest,
    ) -> Result<McpAppsReadResourceResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_MCP_APPS_READRESOURCE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn read_resource(
        &self,
        params: McpAppsReadResourceRequest,
    ) -> Result<McpAppsReadResourceResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_MCP_APPS_READRESOURCE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// List tools that an MCP App view is allowed to call (SEP-1865 visibility filter). Returns tools whose `_meta.ui.visibility` is unset (default `["model","app"]`) or includes `"app"`.
    ///
    /// Wire method: `session.mcp.apps.listTools`.
    ///
    /// # Parameters
    ///
    /// * `params` - MCP server to list app-callable tools for.
    ///
    /// # Returns
    ///
    /// App-callable tools from the named MCP server.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list_tools(
        &self,
        params: McpAppsListToolsRequest,
    ) -> Result<McpAppsListToolsResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_APPS_LISTTOOLS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list_tools(
        &self,
        params: McpAppsListToolsRequest,
    ) -> Result<McpAppsListToolsResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_APPS_LISTTOOLS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Call an MCP tool from an MCP App view (SEP-1865). Enforces the visibility check that prevents an app iframe from invoking model-only tools. Returns the standard MCP `CallToolResult`.
    ///
    /// Wire method: `session.mcp.apps.callTool`.
    ///
    /// # Parameters
    ///
    /// * `params` - MCP server, tool name, and arguments to invoke from an MCP App view.
    ///
    /// # Returns
    ///
    /// Standard MCP CallToolResult
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn call_tool(
        &self,
        params: McpAppsCallToolRequest,
    ) -> Result<SessionMcpAppsCallToolResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_APPS_CALLTOOL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn call_tool(
        &self,
        params: McpAppsCallToolRequest,
    ) -> Result<SessionMcpAppsCallToolResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_APPS_CALLTOOL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Replace the host context returned to MCP App guests on `ui/initialize`. Hosts use this to advertise theme, locale, or other metadata to the guest UI.
    ///
    /// Wire method: `session.mcp.apps.setHostContext`.
    ///
    /// # Parameters
    ///
    /// * `params` - Host context to advertise to MCP App guests.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set_host_context(
        &self,
        params: McpAppsSetHostContextRequest,
    ) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_MCP_APPS_SETHOSTCONTEXT,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set_host_context(
        &self,
        params: McpAppsSetHostContextRequest,
    ) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_MCP_APPS_SETHOSTCONTEXT,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }

    /// Read the current host context advertised to MCP App guests.
    ///
    /// Wire method: `session.mcp.apps.getHostContext`.
    ///
    /// # Returns
    ///
    /// Current host context advertised to MCP App guests.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_host_context(&self) -> Result<McpAppsHostContext, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_MCP_APPS_GETHOSTCONTEXT,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_host_context(&self) -> Result<McpAppsHostContext, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_MCP_APPS_GETHOSTCONTEXT,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Diagnose MCP Apps wiring for a specific MCP server. Reports the session capability, feature-flag state, advertised extension, and how many tools have `_meta.ui` populated.
    ///
    /// Wire method: `session.mcp.apps.diagnose`.
    ///
    /// # Parameters
    ///
    /// * `params` - MCP server to diagnose MCP Apps wiring for.
    ///
    /// # Returns
    ///
    /// Diagnostic snapshot of MCP Apps wiring for the named server.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn diagnose(
        &self,
        params: McpAppsDiagnoseRequest,
    ) -> Result<McpAppsDiagnoseResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_APPS_DIAGNOSE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn diagnose(
        &self,
        params: McpAppsDiagnoseRequest,
    ) -> Result<McpAppsDiagnoseResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_APPS_DIAGNOSE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.mcp.oauth.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcMcpOauth<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcMcpOauth<'a> {
    /// Responds to a pending MCP OAuth provider request. Marked internal because the `provider` argument is an in-process OAuthClientProvider instance that cannot be carried over the wire; the public OAuth surface will route the response through a wire-clean handshake once the CLI moves on top of the SDK.
    ///
    /// Wire method: `session.mcp.oauth.respond`.
    ///
    /// # Parameters
    ///
    /// * `params` - MCP OAuth request id and optional provider response.
    ///
    /// # Returns
    ///
    /// Empty result after recording the MCP OAuth response.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub(crate) async fn respond(
        &self,
        params: McpOauthRespondRequest,
    ) -> Result<McpOauthRespondResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_OAUTH_RESPOND, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Starts OAuth authentication for a remote MCP server.
    ///
    /// Wire method: `session.mcp.oauth.login`.
    ///
    /// # Parameters
    ///
    /// * `params` - Remote MCP server name and optional overrides controlling reauthentication, OAuth client display name, and the callback success-page copy.
    ///
    /// # Returns
    ///
    /// OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn login(&self, params: McpOauthLoginRequest) -> Result<McpOauthLoginResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_OAUTH_LOGIN, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn login(
        &self,
        params: McpOauthLoginRequest,
    ) -> Result<McpOauthLoginResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_OAUTH_LOGIN, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.metadata.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcMetadata<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcMetadata<'a> {
    /// Returns a snapshot of the session's identifying metadata, mode, agent, and remote info.
    ///
    /// Wire method: `session.metadata.snapshot`.
    ///
    /// # Returns
    ///
    /// Point-in-time snapshot of slow-changing session identifier and state fields
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn snapshot(&self) -> Result<SessionMetadataSnapshot, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_METADATA_SNAPSHOT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn snapshot(&self) -> Result<SessionMetadataSnapshot, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_METADATA_SNAPSHOT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Reports whether the local session is currently processing user/agent messages.
    ///
    /// Wire method: `session.metadata.isProcessing`.
    ///
    /// # Returns
    ///
    /// Indicates whether the local session is currently processing a turn or background continuation.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn is_processing(&self) -> Result<MetadataIsProcessingResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_METADATA_ISPROCESSING,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn is_processing(&self) -> Result<MetadataIsProcessingResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_METADATA_ISPROCESSING,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns a snapshot of activity flags for the session.
    ///
    /// Wire method: `session.metadata.activity`.
    ///
    /// # Returns
    ///
    /// Current activity flags for the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn activity(&self) -> Result<SessionActivity, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_METADATA_ACTIVITY, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn activity(&self) -> Result<SessionActivity, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_METADATA_ACTIVITY, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns the token breakdown for the session's current context window for a given model.
    ///
    /// Wire method: `session.metadata.contextInfo`.
    ///
    /// # Parameters
    ///
    /// * `params` - Model identifier and token limits used to compute the context-info breakdown.
    ///
    /// # Returns
    ///
    /// Token breakdown for the session's current context window, or null if uninitialized.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn context_info(
        &self,
        params: MetadataContextInfoRequest,
    ) -> Result<MetadataContextInfoResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_METADATA_CONTEXTINFO, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn context_info(
        &self,
        params: MetadataContextInfoRequest,
    ) -> Result<MetadataContextInfoResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_METADATA_CONTEXTINFO, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Records a working-directory/git context change and emits a `session.context_changed` event.
    ///
    /// Wire method: `session.metadata.recordContextChange`.
    ///
    /// # Parameters
    ///
    /// * `params` - Updated working-directory/git context to record on the session.
    ///
    /// # Returns
    ///
    /// Notify the session that its working directory context has changed. Emits a `session.context_changed` event so consumers (telemetry, OTel tracker, ACP, the timeline UI) can react. Use this when the host has detected a cwd/branch/repo change outside the session's normal lifecycle (e.g., after a shell command in interactive mode).
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn record_context_change(
        &self,
        params: MetadataRecordContextChangeRequest,
    ) -> Result<MetadataRecordContextChangeResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_METADATA_RECORDCONTEXTCHANGE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn record_context_change(
        &self,
        params: MetadataRecordContextChangeRequest,
    ) -> Result<MetadataRecordContextChangeResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_METADATA_RECORDCONTEXTCHANGE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Updates the session's recorded working directory.
    ///
    /// Wire method: `session.metadata.setWorkingDirectory`.
    ///
    /// # Parameters
    ///
    /// * `params` - Absolute path to set as the session's new working directory.
    ///
    /// # Returns
    ///
    /// Update the session's working directory. Used by the host when the user explicitly changes cwd (e.g., the `/cd` slash command). The host is responsible for `process.chdir` and any related side-effects (file index, etc.); this method only updates the session's own recorded path.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set_working_directory(
        &self,
        params: MetadataSetWorkingDirectoryRequest,
    ) -> Result<MetadataSetWorkingDirectoryResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_METADATA_SETWORKINGDIRECTORY,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set_working_directory(
        &self,
        params: MetadataSetWorkingDirectoryRequest,
    ) -> Result<MetadataSetWorkingDirectoryResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_METADATA_SETWORKINGDIRECTORY,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Re-tokenizes the session's existing messages against a model and returns aggregate token totals.
    ///
    /// Wire method: `session.metadata.recomputeContextTokens`.
    ///
    /// # Parameters
    ///
    /// * `params` - Model identifier to use when re-tokenizing the session's existing messages.
    ///
    /// # Returns
    ///
    /// Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn recompute_context_tokens(
        &self,
        params: MetadataRecomputeContextTokensRequest,
    ) -> Result<MetadataRecomputeContextTokensResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_METADATA_RECOMPUTECONTEXTTOKENS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn recompute_context_tokens(
        &self,
        params: MetadataRecomputeContextTokensRequest,
    ) -> Result<MetadataRecomputeContextTokensResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_METADATA_RECOMPUTECONTEXTTOKENS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.mode.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcMode<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcMode<'a> {
    /// Gets the current agent interaction mode.
    ///
    /// Wire method: `session.mode.get`.
    ///
    /// # Returns
    ///
    /// The session mode the agent is operating in
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get(&self) -> Result<SessionMode, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODE_GET, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get(&self) -> Result<SessionMode, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODE_GET, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Sets the current agent interaction mode.
    ///
    /// Wire method: `session.mode.set`.
    ///
    /// # Parameters
    ///
    /// * `params` - Agent interaction mode to apply to the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set(&self, params: ModeSetRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODE_SET, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set(&self, params: ModeSetRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODE_SET, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// `session.model.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcModel<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcModel<'a> {
    /// Gets the currently selected model for the session.
    ///
    /// Wire method: `session.model.getCurrent`.
    ///
    /// # Returns
    ///
    /// The currently selected model, reasoning effort, and context tier for the session. The context tier reflects `Session.getContextTier()`, restored from the session journal on resume.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_current(&self) -> Result<CurrentModel, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODEL_GETCURRENT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_current(&self) -> Result<CurrentModel, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODEL_GETCURRENT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Switches the session to a model and optional reasoning configuration.
    ///
    /// Wire method: `session.model.switchTo`.
    ///
    /// # Parameters
    ///
    /// * `params` - Target model identifier and optional reasoning effort, summary, capability overrides, and context tier.
    ///
    /// # Returns
    ///
    /// The model identifier active on the session after the switch.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn switch_to(
        &self,
        params: ModelSwitchToRequest,
    ) -> Result<ModelSwitchToResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODEL_SWITCHTO, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn switch_to(
        &self,
        params: ModelSwitchToRequest,
    ) -> Result<ModelSwitchToResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODEL_SWITCHTO, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Updates the session's reasoning effort without changing the selected model.
    ///
    /// Wire method: `session.model.setReasoningEffort`.
    ///
    /// # Parameters
    ///
    /// * `params` - Reasoning effort level to apply to the currently selected model.
    ///
    /// # Returns
    ///
    /// Update the session's reasoning effort without changing the selected model. Use `switchTo` instead when you also need to change the model. The runtime stores the effort on the session and applies it to subsequent turns.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set_reasoning_effort(
        &self,
        params: ModelSetReasoningEffortRequest,
    ) -> Result<ModelSetReasoningEffortResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_MODEL_SETREASONINGEFFORT,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set_reasoning_effort(
        &self,
        params: ModelSetReasoningEffortRequest,
    ) -> Result<ModelSetReasoningEffortResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_MODEL_SETREASONINGEFFORT,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Lists models available to this session using its own auth and integration context. Connected hosts (CLI TUI, GitHub App) should call this through the session client so remote sessions return the remote CLI's available models rather than the caller's.
    ///
    /// Wire method: `session.model.list`.
    ///
    /// # Returns
    ///
    /// The list of models available to this session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<SessionModelList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODEL_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<SessionModelList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODEL_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Lists models available to this session using its own auth and integration context. Connected hosts (CLI TUI, GitHub App) should call this through the session client so remote sessions return the remote CLI's available models rather than the caller's.
    ///
    /// Wire method: `session.model.list`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional listing options.
    ///
    /// # Returns
    ///
    /// The list of models available to this session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list_with_params(
        &self,
        params: ModelListRequest,
    ) -> Result<SessionModelList, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODEL_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list_with_params(
        &self,
        params: ModelListRequest,
    ) -> Result<SessionModelList, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODEL_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.name.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcName<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcName<'a> {
    /// Gets the session's friendly name.
    ///
    /// Wire method: `session.name.get`.
    ///
    /// # Returns
    ///
    /// The session's friendly name, or null when not yet set.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get(&self) -> Result<NameGetResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_NAME_GET, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get(&self) -> Result<NameGetResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_NAME_GET, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Sets the session's friendly name.
    ///
    /// Wire method: `session.name.set`.
    ///
    /// # Parameters
    ///
    /// * `params` - New friendly name to apply to the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set(&self, params: NameSetRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_NAME_SET, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set(&self, params: NameSetRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_NAME_SET, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Persists an auto-generated session summary as the session's name when no user-set name exists.
    ///
    /// Wire method: `session.name.setAuto`.
    ///
    /// # Parameters
    ///
    /// * `params` - Auto-generated session summary to apply as the session's name when no user-set name exists.
    ///
    /// # Returns
    ///
    /// Indicates whether the auto-generated summary was applied as the session's name.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set_auto(&self, params: NameSetAutoRequest) -> Result<NameSetAutoResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_NAME_SETAUTO, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set_auto(
        &self,
        params: NameSetAutoRequest,
    ) -> Result<NameSetAutoResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_NAME_SETAUTO, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.options.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcOptions<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcOptions<'a> {
    /// Patches the genuinely-mutable subset of session options.
    ///
    /// Wire method: `session.options.update`.
    ///
    /// # Parameters
    ///
    /// * `params` - Patch of mutable session options to apply to the running session.
    ///
    /// # Returns
    ///
    /// Indicates whether the session options patch was applied successfully.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn update(
        &self,
        params: SessionUpdateOptionsParams,
    ) -> Result<SessionUpdateOptionsResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_OPTIONS_UPDATE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn update(
        &self,
        params: SessionUpdateOptionsParams,
    ) -> Result<SessionUpdateOptionsResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_OPTIONS_UPDATE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.permissions.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcPermissions<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcPermissions<'a> {
    /// `session.permissions.folderTrust.*` sub-namespace.
    pub fn folder_trust(&self) -> SessionRpcPermissionsFolderTrust<'a> {
        SessionRpcPermissionsFolderTrust {
            session: self.session,
        }
    }

    /// `session.permissions.locations.*` sub-namespace.
    pub fn locations(&self) -> SessionRpcPermissionsLocations<'a> {
        SessionRpcPermissionsLocations {
            session: self.session,
        }
    }

    /// `session.permissions.paths.*` sub-namespace.
    pub fn paths(&self) -> SessionRpcPermissionsPaths<'a> {
        SessionRpcPermissionsPaths {
            session: self.session,
        }
    }

    /// `session.permissions.urls.*` sub-namespace.
    pub fn urls(&self) -> SessionRpcPermissionsUrls<'a> {
        SessionRpcPermissionsUrls {
            session: self.session,
        }
    }

    /// Replaces selected permission policy fields (rules, paths, URLs, exclusions, allow-all flags) on the session.
    ///
    /// Wire method: `session.permissions.configure`.
    ///
    /// # Parameters
    ///
    /// * `params` - Patch of permission policy fields to apply (omit a field to leave it unchanged).
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn configure(
        &self,
        params: PermissionsConfigureParams,
    ) -> Result<PermissionsConfigureResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_CONFIGURE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn configure(
        &self,
        params: PermissionsConfigureParams,
    ) -> Result<PermissionsConfigureResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_CONFIGURE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Provides a decision for a pending tool permission request.
    ///
    /// Wire method: `session.permissions.handlePendingPermissionRequest`.
    ///
    /// # Parameters
    ///
    /// * `params` - Pending permission request ID and the decision to apply (approve/reject and scope).
    ///
    /// # Returns
    ///
    /// Indicates whether the permission decision was applied; false when the request was already resolved.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn handle_pending_permission_request(
        &self,
        params: PermissionDecisionRequest,
    ) -> Result<PermissionRequestResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_HANDLEPENDINGPERMISSIONREQUEST,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn handle_pending_permission_request(
        &self,
        params: PermissionDecisionRequest,
    ) -> Result<PermissionRequestResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_HANDLEPENDINGPERMISSIONREQUEST,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Reconstructs the set of pending tool permission requests from the session's event history.
    ///
    /// Wire method: `session.permissions.pendingRequests`.
    ///
    /// # Returns
    ///
    /// List of pending permission requests reconstructed from event history.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn pending_requests(&self) -> Result<PendingPermissionRequestList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_PENDINGREQUESTS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn pending_requests(&self) -> Result<PendingPermissionRequestList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_PENDINGREQUESTS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Enables or disables automatic approval of tool permission requests for the session.
    ///
    /// Wire method: `session.permissions.setApproveAll`.
    ///
    /// # Parameters
    ///
    /// * `params` - Allow-all toggle for tool permission requests, with an optional telemetry source.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set_approve_all(
        &self,
        params: PermissionsSetApproveAllRequest,
    ) -> Result<PermissionsSetApproveAllResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_SETAPPROVEALL,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set_approve_all(
        &self,
        params: PermissionsSetApproveAllRequest,
    ) -> Result<PermissionsSetApproveAllResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_SETAPPROVEALL,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Enables or disables full allow-all permissions (tools, paths, and URLs) for the session. Used by attach-mode clients (e.g. LocalRpcSession's `/allow-all` forwarder) to flip the target session's permission state. Unlike `setApproveAll`, this swaps in the unrestricted path and URL managers and emits `session.permissions_changed` on transition. The result returns the authoritative post-mutation state so callers can update their local mirrors without racing the `session.permissions_changed` notification on the same wire.
    ///
    /// Wire method: `session.permissions.setAllowAll`.
    ///
    /// # Parameters
    ///
    /// * `params` - Whether to enable full allow-all permissions for the session.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded and reports the post-mutation state.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set_allow_all(
        &self,
        params: PermissionsSetAllowAllRequest,
    ) -> Result<AllowAllPermissionSetResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_SETALLOWALL,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set_allow_all(
        &self,
        params: PermissionsSetAllowAllRequest,
    ) -> Result<AllowAllPermissionSetResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_SETALLOWALL,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns whether full allow-all permissions are currently active for the session.
    ///
    /// Wire method: `session.permissions.getAllowAll`.
    ///
    /// # Returns
    ///
    /// Current full allow-all permission state.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_allow_all(&self) -> Result<AllowAllPermissionState, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_GETALLOWALL,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_allow_all(&self) -> Result<AllowAllPermissionState, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_GETALLOWALL,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Adds or removes session-scoped or location-scoped permission rules.
    ///
    /// Wire method: `session.permissions.modifyRules`.
    ///
    /// # Parameters
    ///
    /// * `params` - Scope and add/remove instructions for modifying session- or location-scoped permission rules.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn modify_rules(
        &self,
        params: PermissionsModifyRulesParams,
    ) -> Result<PermissionsModifyRulesResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_MODIFYRULES,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn modify_rules(
        &self,
        params: PermissionsModifyRulesParams,
    ) -> Result<PermissionsModifyRulesResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_MODIFYRULES,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Sets whether the client wants permission prompts bridged into session events.
    ///
    /// Wire method: `session.permissions.setRequired`.
    ///
    /// # Parameters
    ///
    /// * `params` - Toggles whether permission prompts should be bridged into session events for this client.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set_required(
        &self,
        params: PermissionsSetRequiredRequest,
    ) -> Result<PermissionsSetRequiredResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_SETREQUIRED,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set_required(
        &self,
        params: PermissionsSetRequiredRequest,
    ) -> Result<PermissionsSetRequiredResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_SETREQUIRED,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Clears session-scoped tool permission approvals.
    ///
    /// Wire method: `session.permissions.resetSessionApprovals`.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn reset_session_approvals(
        &self,
    ) -> Result<PermissionsResetSessionApprovalsResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_RESETSESSIONAPPROVALS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn reset_session_approvals(
        &self,
    ) -> Result<PermissionsResetSessionApprovalsResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_RESETSESSIONAPPROVALS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Notifies the runtime that a permission prompt UI has been shown to the user.
    ///
    /// Wire method: `session.permissions.notifyPromptShown`.
    ///
    /// # Parameters
    ///
    /// * `params` - Notification payload describing the permission prompt that the client just rendered.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn notify_prompt_shown(
        &self,
        params: PermissionPromptShownNotification,
    ) -> Result<PermissionsNotifyPromptShownResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_NOTIFYPROMPTSHOWN,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn notify_prompt_shown(
        &self,
        params: PermissionPromptShownNotification,
    ) -> Result<PermissionsNotifyPromptShownResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_NOTIFYPROMPTSHOWN,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.permissions.folderTrust.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcPermissionsFolderTrust<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcPermissionsFolderTrust<'a> {
    /// Reports whether a folder is trusted according to the user's folder trust state.
    ///
    /// Wire method: `session.permissions.folderTrust.isTrusted`.
    ///
    /// # Parameters
    ///
    /// * `params` - Folder path to check for trust.
    ///
    /// # Returns
    ///
    /// Folder trust check result.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn is_trusted(
        &self,
        params: FolderTrustCheckParams,
    ) -> Result<FolderTrustCheckResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_FOLDERTRUST_ISTRUSTED,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn is_trusted(
        &self,
        params: FolderTrustCheckParams,
    ) -> Result<FolderTrustCheckResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_FOLDERTRUST_ISTRUSTED,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Adds a folder to the user's trusted folders list.
    ///
    /// Wire method: `session.permissions.folderTrust.addTrusted`.
    ///
    /// # Parameters
    ///
    /// * `params` - Folder path to add to trusted folders.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn add_trusted(
        &self,
        params: FolderTrustAddParams,
    ) -> Result<PermissionsFolderTrustAddTrustedResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_FOLDERTRUST_ADDTRUSTED,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn add_trusted(
        &self,
        params: FolderTrustAddParams,
    ) -> Result<PermissionsFolderTrustAddTrustedResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_FOLDERTRUST_ADDTRUSTED,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.permissions.locations.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcPermissionsLocations<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcPermissionsLocations<'a> {
    /// Resolves the permission location key and type for a working directory.
    ///
    /// Wire method: `session.permissions.locations.resolve`.
    ///
    /// # Parameters
    ///
    /// * `params` - Working directory to resolve into a location-permissions key.
    ///
    /// # Returns
    ///
    /// Resolved location-permissions key and type.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn resolve(
        &self,
        params: PermissionLocationResolveParams,
    ) -> Result<PermissionLocationResolveResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_LOCATIONS_RESOLVE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn resolve(
        &self,
        params: PermissionLocationResolveParams,
    ) -> Result<PermissionLocationResolveResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_LOCATIONS_RESOLVE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Applies persisted location-scoped tool approvals and allowed directories for a working directory to this session's permission service.
    ///
    /// Wire method: `session.permissions.locations.apply`.
    ///
    /// # Parameters
    ///
    /// * `params` - Working directory to load persisted location permissions for.
    ///
    /// # Returns
    ///
    /// Summary of persisted location permissions applied to the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn apply(
        &self,
        params: PermissionLocationApplyParams,
    ) -> Result<PermissionLocationApplyResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_LOCATIONS_APPLY,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn apply(
        &self,
        params: PermissionLocationApplyParams,
    ) -> Result<PermissionLocationApplyResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_LOCATIONS_APPLY,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Persists a tool approval for a permission location and applies its rules to this session's live permission service.
    ///
    /// Wire method: `session.permissions.locations.addToolApproval`.
    ///
    /// # Parameters
    ///
    /// * `params` - Location-scoped tool approval to persist.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn add_tool_approval(
        &self,
        params: PermissionLocationAddToolApprovalParams,
    ) -> Result<PermissionsLocationsAddToolApprovalResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_LOCATIONS_ADDTOOLAPPROVAL,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn add_tool_approval(
        &self,
        params: PermissionLocationAddToolApprovalParams,
    ) -> Result<PermissionsLocationsAddToolApprovalResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_LOCATIONS_ADDTOOLAPPROVAL,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.permissions.paths.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcPermissionsPaths<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcPermissionsPaths<'a> {
    /// Returns the session's allowed directories and primary working directory.
    ///
    /// Wire method: `session.permissions.paths.list`.
    ///
    /// # Returns
    ///
    /// Snapshot of the session's allow-listed directories and primary working directory.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<PermissionPathsList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_PATHS_LIST,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<PermissionPathsList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_PATHS_LIST,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Adds a directory to the session's allow-list.
    ///
    /// Wire method: `session.permissions.paths.add`.
    ///
    /// # Parameters
    ///
    /// * `params` - Directory path to add to the session's allowed directories.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn add(
        &self,
        params: PermissionPathsAddParams,
    ) -> Result<PermissionsPathsAddResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_PATHS_ADD,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn add(
        &self,
        params: PermissionPathsAddParams,
    ) -> Result<PermissionsPathsAddResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_PATHS_ADD,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Updates the session's primary working directory used by the permission policy.
    ///
    /// Wire method: `session.permissions.paths.updatePrimary`.
    ///
    /// # Parameters
    ///
    /// * `params` - Directory path to set as the session's new primary working directory.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn update_primary(
        &self,
        params: PermissionPathsUpdatePrimaryParams,
    ) -> Result<PermissionsPathsUpdatePrimaryResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_PATHS_UPDATEPRIMARY,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn update_primary(
        &self,
        params: PermissionPathsUpdatePrimaryParams,
    ) -> Result<PermissionsPathsUpdatePrimaryResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_PATHS_UPDATEPRIMARY,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Reports whether a path falls within any of the session's allowed directories.
    ///
    /// Wire method: `session.permissions.paths.isPathWithinAllowedDirectories`.
    ///
    /// # Parameters
    ///
    /// * `params` - Path to evaluate against the session's allowed directories.
    ///
    /// # Returns
    ///
    /// Indicates whether the supplied path is within the session's allowed directories.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn is_path_within_allowed_directories(
        &self,
        params: PermissionPathsAllowedCheckParams,
    ) -> Result<PermissionPathsAllowedCheckResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_PATHS_ISPATHWITHINALLOWEDDIRECTORIES,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn is_path_within_allowed_directories(
        &self,
        params: PermissionPathsAllowedCheckParams,
    ) -> Result<PermissionPathsAllowedCheckResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_PATHS_ISPATHWITHINALLOWEDDIRECTORIES,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Reports whether a path falls within the session's workspace (primary) directory.
    ///
    /// Wire method: `session.permissions.paths.isPathWithinWorkspace`.
    ///
    /// # Parameters
    ///
    /// * `params` - Path to evaluate against the session's workspace (primary) directory.
    ///
    /// # Returns
    ///
    /// Indicates whether the supplied path is within the session's workspace directory.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn is_path_within_workspace(
        &self,
        params: PermissionPathsWorkspaceCheckParams,
    ) -> Result<PermissionPathsWorkspaceCheckResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_PATHS_ISPATHWITHINWORKSPACE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn is_path_within_workspace(
        &self,
        params: PermissionPathsWorkspaceCheckParams,
    ) -> Result<PermissionPathsWorkspaceCheckResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_PATHS_ISPATHWITHINWORKSPACE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.permissions.urls.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcPermissionsUrls<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcPermissionsUrls<'a> {
    /// Toggles the runtime's URL-permission policy between unrestricted and restricted modes.
    ///
    /// Wire method: `session.permissions.urls.setUnrestrictedMode`.
    ///
    /// # Parameters
    ///
    /// * `params` - Whether the URL-permission policy should run in unrestricted mode.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set_unrestricted_mode(
        &self,
        params: PermissionUrlsSetUnrestrictedModeParams,
    ) -> Result<PermissionsUrlsSetUnrestrictedModeResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_URLS_SETUNRESTRICTEDMODE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set_unrestricted_mode(
        &self,
        params: PermissionUrlsSetUnrestrictedModeParams,
    ) -> Result<PermissionsUrlsSetUnrestrictedModeResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PERMISSIONS_URLS_SETUNRESTRICTEDMODE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.plan.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcPlan<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcPlan<'a> {
    /// Reads the session plan file from the workspace.
    ///
    /// Wire method: `session.plan.read`.
    ///
    /// # Returns
    ///
    /// Existence, contents, and resolved path of the session plan file.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn read(&self) -> Result<PlanReadResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLAN_READ, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn read(&self) -> Result<PlanReadResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLAN_READ, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Writes new content to the session plan file.
    ///
    /// Wire method: `session.plan.update`.
    ///
    /// # Parameters
    ///
    /// * `params` - Replacement contents to write to the session plan file.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn update(&self, params: PlanUpdateRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLAN_UPDATE, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn update(&self, params: PlanUpdateRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLAN_UPDATE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Deletes the session plan file from the workspace.
    ///
    /// Wire method: `session.plan.delete`.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn delete(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLAN_DELETE, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn delete(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLAN_DELETE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Reads todo rows from the session SQL database for plan rendering.
    ///
    /// Wire method: `session.plan.readSqlTodos`.
    ///
    /// # Returns
    ///
    /// Todo rows read from the session SQL database. Empty when no session database is available.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn read_sql_todos(&self) -> Result<PlanReadSqlTodosResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLAN_READSQLTODOS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn read_sql_todos(&self) -> Result<PlanReadSqlTodosResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLAN_READSQLTODOS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Reads todo rows AND dependency edges from the session SQL database for structured progress UI. Same defensive behavior as readSqlTodos — returns empty arrays when the database, tables, or columns aren't available. Clients should call this on session start and after every `session.todos_changed` event to refresh structured-UI rendering.
    ///
    /// Wire method: `session.plan.readSqlTodosWithDependencies`.
    ///
    /// # Returns
    ///
    /// Todo rows + dependency edges read from the session SQL database.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn read_sql_todos_with_dependencies(
        &self,
    ) -> Result<PlanReadSqlTodosWithDependenciesResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PLAN_READSQLTODOSWITHDEPENDENCIES,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn read_sql_todos_with_dependencies(
        &self,
    ) -> Result<PlanReadSqlTodosWithDependenciesResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_PLAN_READSQLTODOSWITHDEPENDENCIES,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.plugins.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcPlugins<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcPlugins<'a> {
    /// Lists plugins installed for the session.
    ///
    /// Wire method: `session.plugins.list`.
    ///
    /// # Returns
    ///
    /// Plugins installed for the session, with their enabled state and version metadata.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<PluginList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLUGINS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<PluginList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLUGINS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Reloads the session's plugin set, refreshing MCP servers, custom agents, hooks, and skills cache so SDK-driven changes via `server.plugins.*` take effect immediately.
    ///
    /// Wire method: `session.plugins.reload`.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn reload(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLUGINS_RELOAD, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn reload(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLUGINS_RELOAD, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Reloads the session's plugin set, refreshing MCP servers, custom agents, hooks, and skills cache so SDK-driven changes via `server.plugins.*` take effect immediately.
    ///
    /// Wire method: `session.plugins.reload`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional flags controlling which side effects the reload performs.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn reload_with_params(&self, params: PluginsReloadRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLUGINS_RELOAD, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn reload_with_params(
        &self,
        params: PluginsReloadRequest,
    ) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLUGINS_RELOAD, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// `session.provider.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcProvider<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcProvider<'a> {
    /// Returns the provider endpoint and credentials the session is currently configured to talk to, so the caller can make inference calls directly against the same backend the session uses.
    ///
    /// Wire method: `session.provider.getEndpoint`.
    ///
    /// # Returns
    ///
    /// A snapshot of the provider endpoint the session is currently configured to talk to.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_endpoint(&self) -> Result<ProviderEndpoint, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PROVIDER_GETENDPOINT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_endpoint(&self) -> Result<ProviderEndpoint, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PROVIDER_GETENDPOINT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns the provider endpoint and credentials the session is currently configured to talk to, so the caller can make inference calls directly against the same backend the session uses.
    ///
    /// Wire method: `session.provider.getEndpoint`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional model identifier to scope the endpoint snapshot to.
    ///
    /// # Returns
    ///
    /// A snapshot of the provider endpoint the session is currently configured to talk to.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_endpoint_with_params(
        &self,
        params: ProviderGetEndpointRequest,
    ) -> Result<ProviderEndpoint, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PROVIDER_GETENDPOINT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_endpoint_with_params(
        &self,
        params: ProviderGetEndpointRequest,
    ) -> Result<ProviderEndpoint, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PROVIDER_GETENDPOINT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.queue.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcQueue<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcQueue<'a> {
    /// Returns the local session's pending user-facing queued items and steering messages.
    ///
    /// Wire method: `session.queue.pendingItems`.
    ///
    /// # Returns
    ///
    /// Snapshot of the session's pending queued items and immediate-steering messages.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn pending_items(&self) -> Result<QueuePendingItemsResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_QUEUE_PENDINGITEMS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn pending_items(&self) -> Result<QueuePendingItemsResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_QUEUE_PENDINGITEMS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Removes the most recently queued user-facing item (LIFO).
    ///
    /// Wire method: `session.queue.removeMostRecent`.
    ///
    /// # Returns
    ///
    /// Indicates whether a user-facing pending item was removed.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn remove_most_recent(&self) -> Result<QueueRemoveMostRecentResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_QUEUE_REMOVEMOSTRECENT,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn remove_most_recent(&self) -> Result<QueueRemoveMostRecentResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_QUEUE_REMOVEMOSTRECENT,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Clears all pending queued items on the local session.
    ///
    /// Wire method: `session.queue.clear`.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn clear(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_QUEUE_CLEAR, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn clear(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_QUEUE_CLEAR, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// `session.remote.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcRemote<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcRemote<'a> {
    /// Enables remote session export or steering.
    ///
    /// Wire method: `session.remote.enable`.
    ///
    /// # Parameters
    ///
    /// * `params` - Optional remote session mode ("off", "export", or "on"); defaults to enabling both export and remote steering.
    ///
    /// # Returns
    ///
    /// GitHub URL for the session and a flag indicating whether remote steering is enabled.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn enable(&self, params: RemoteEnableRequest) -> Result<RemoteEnableResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_REMOTE_ENABLE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn enable(
        &self,
        params: RemoteEnableRequest,
    ) -> Result<RemoteEnableResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_REMOTE_ENABLE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Disables remote session export and steering.
    ///
    /// Wire method: `session.remote.disable`.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn disable(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_REMOTE_DISABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn disable(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_REMOTE_DISABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Persists a remote-steerability change emitted by the host as a session event.
    ///
    /// Wire method: `session.remote.notifySteerableChanged`.
    ///
    /// # Parameters
    ///
    /// * `params` - New remote-steerability state to persist as a `session.remote_steerable_changed` event.
    ///
    /// # Returns
    ///
    /// Persist a steerability change as a `session.remote_steerable_changed` event. Used by the host (CLI / SDK consumer) when it has just finished enabling or disabling steering on a remote exporter that the runtime does not directly own.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn notify_steerable_changed(
        &self,
        params: RemoteNotifySteerableChangedRequest,
    ) -> Result<RemoteNotifySteerableChangedResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_REMOTE_NOTIFYSTEERABLECHANGED,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn notify_steerable_changed(
        &self,
        params: RemoteNotifySteerableChangedRequest,
    ) -> Result<RemoteNotifySteerableChangedResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_REMOTE_NOTIFYSTEERABLECHANGED,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.schedule.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcSchedule<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcSchedule<'a> {
    /// Lists the session's currently active scheduled prompts.
    ///
    /// Wire method: `session.schedule.list`.
    ///
    /// # Returns
    ///
    /// Snapshot of the currently active recurring prompts for this session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<ScheduleList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SCHEDULE_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<ScheduleList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SCHEDULE_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Removes a scheduled prompt by id.
    ///
    /// Wire method: `session.schedule.stop`.
    ///
    /// # Parameters
    ///
    /// * `params` - Identifier of the scheduled prompt to remove.
    ///
    /// # Returns
    ///
    /// Remove a scheduled prompt by id. The result entry is omitted if the id was unknown.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn stop(&self, params: ScheduleStopRequest) -> Result<ScheduleStopResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SCHEDULE_STOP, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn stop(
        &self,
        params: ScheduleStopRequest,
    ) -> Result<ScheduleStopResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SCHEDULE_STOP, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.shell.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcShell<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcShell<'a> {
    /// Starts a shell command and streams output through session notifications.
    ///
    /// Wire method: `session.shell.exec`.
    ///
    /// # Parameters
    ///
    /// * `params` - Shell command to run, with optional working directory and timeout in milliseconds.
    ///
    /// # Returns
    ///
    /// Identifier of the spawned process, used to correlate streamed output and exit notifications.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn exec(&self, params: ShellExecRequest) -> Result<ShellExecResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SHELL_EXEC, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn exec(&self, params: ShellExecRequest) -> Result<ShellExecResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SHELL_EXEC, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Sends a signal to a shell process previously started via "shell.exec".
    ///
    /// Wire method: `session.shell.kill`.
    ///
    /// # Parameters
    ///
    /// * `params` - Identifier of a process previously returned by "shell.exec" and the signal to send.
    ///
    /// # Returns
    ///
    /// Indicates whether the signal was delivered; false if the process was unknown or already exited.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn kill(&self, params: ShellKillRequest) -> Result<ShellKillResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SHELL_KILL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn kill(&self, params: ShellKillRequest) -> Result<ShellKillResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SHELL_KILL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Executes a user-requested shell command through the session runtime.
    ///
    /// Wire method: `session.shell.executeUserRequested`.
    ///
    /// # Parameters
    ///
    /// * `params` - User-requested shell command and cancellation handle.
    ///
    /// # Returns
    ///
    /// Result of a user-requested shell command.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn execute_user_requested(
        &self,
        params: ShellExecuteUserRequestedRequest,
    ) -> Result<UserRequestedShellCommandResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_SHELL_EXECUTEUSERREQUESTED,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn execute_user_requested(
        &self,
        params: ShellExecuteUserRequestedRequest,
    ) -> Result<UserRequestedShellCommandResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_SHELL_EXECUTEUSERREQUESTED,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Cancels a user-requested shell command by request ID.
    ///
    /// Wire method: `session.shell.cancelUserRequested`.
    ///
    /// # Parameters
    ///
    /// * `params` - User-requested shell execution cancellation handle.
    ///
    /// # Returns
    ///
    /// Cancellation result for a user-requested shell command.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn cancel_user_requested(
        &self,
        params: ShellCancelUserRequestedRequest,
    ) -> Result<CancelUserRequestedShellCommandResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_SHELL_CANCELUSERREQUESTED,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn cancel_user_requested(
        &self,
        params: ShellCancelUserRequestedRequest,
    ) -> Result<CancelUserRequestedShellCommandResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_SHELL_CANCELUSERREQUESTED,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.skills.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcSkills<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcSkills<'a> {
    /// Lists skills available to the session.
    ///
    /// Wire method: `session.skills.list`.
    ///
    /// # Returns
    ///
    /// Skills available to the session, with their enabled state.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<SkillList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<SkillList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns the skills that have been invoked during this session.
    ///
    /// Wire method: `session.skills.getInvoked`.
    ///
    /// # Returns
    ///
    /// Skills invoked during this session, ordered by invocation time (most recent last).
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_invoked(&self) -> Result<SkillsGetInvokedResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_GETINVOKED, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_invoked(&self) -> Result<SkillsGetInvokedResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_GETINVOKED, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Enables a skill for the session.
    ///
    /// Wire method: `session.skills.enable`.
    ///
    /// # Parameters
    ///
    /// * `params` - Name of the skill to enable for the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn enable(&self, params: SkillsEnableRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_ENABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn enable(&self, params: SkillsEnableRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_ENABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Disables a skill for the session.
    ///
    /// Wire method: `session.skills.disable`.
    ///
    /// # Parameters
    ///
    /// * `params` - Name of the skill to disable for the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn disable(&self, params: SkillsDisableRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_DISABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn disable(&self, params: SkillsDisableRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_DISABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Reloads skill definitions for the session.
    ///
    /// Wire method: `session.skills.reload`.
    ///
    /// # Returns
    ///
    /// Diagnostics from reloading skill definitions, with warnings and errors as separate lists.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn reload(&self) -> Result<SkillsLoadDiagnostics, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_RELOAD, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn reload(&self) -> Result<SkillsLoadDiagnostics, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_RELOAD, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Ensures the session's skill definitions have been loaded from disk.
    ///
    /// Wire method: `session.skills.ensureLoaded`.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn ensure_loaded(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_ENSURELOADED, Some(wire_params))
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn ensure_loaded(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_ENSURELOADED, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// `session.tasks.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcTasks<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcTasks<'a> {
    /// Starts a background agent task in the session.
    ///
    /// Wire method: `session.tasks.startAgent`.
    ///
    /// # Parameters
    ///
    /// * `params` - Agent type, prompt, name, and optional description and model override for the new task.
    ///
    /// # Returns
    ///
    /// Identifier assigned to the newly started background agent task.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn start_agent(
        &self,
        params: TasksStartAgentRequest,
    ) -> Result<TasksStartAgentResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_STARTAGENT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn start_agent(
        &self,
        params: TasksStartAgentRequest,
    ) -> Result<TasksStartAgentResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_STARTAGENT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Lists background tasks tracked by the session.
    ///
    /// Wire method: `session.tasks.list`.
    ///
    /// # Returns
    ///
    /// Background tasks currently tracked by the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list(&self) -> Result<TaskList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list(&self) -> Result<TaskList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Refreshes metadata for any detached background shells the runtime knows about.
    ///
    /// Wire method: `session.tasks.refresh`.
    ///
    /// # Returns
    ///
    /// Refresh metadata for any detached background shells the runtime knows about. Use after a long pause to pick up exit/output state for shells running outside the agent loop.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn refresh(&self) -> Result<TasksRefreshResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_REFRESH, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn refresh(&self) -> Result<TasksRefreshResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_REFRESH, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Waits for all in-flight background tasks and any follow-up turns to settle.
    ///
    /// Wire method: `session.tasks.waitForPending`.
    ///
    /// # Returns
    ///
    /// Wait until all in-flight background tasks (agents + shells) and any follow-up turns scheduled by their completions have settled. Returns when the runtime is fully drained or after an internal timeout (default 10 minutes; configurable via COPILOT_TASK_WAIT_TIMEOUT_SECONDS).
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn wait_for_pending(&self) -> Result<TasksWaitForPendingResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_WAITFORPENDING, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn wait_for_pending(&self) -> Result<TasksWaitForPendingResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_WAITFORPENDING, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns progress information for a background task by ID.
    ///
    /// Wire method: `session.tasks.getProgress`.
    ///
    /// # Parameters
    ///
    /// * `params` - Identifier of the background task to fetch progress for.
    ///
    /// # Returns
    ///
    /// Progress information for the task, or null when no task with that ID is tracked.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_progress(
        &self,
        params: TasksGetProgressRequest,
    ) -> Result<TasksGetProgressResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_GETPROGRESS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_progress(
        &self,
        params: TasksGetProgressRequest,
    ) -> Result<TasksGetProgressResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_GETPROGRESS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns the first sync-waiting task that can currently be promoted to background mode.
    ///
    /// Wire method: `session.tasks.getCurrentPromotable`.
    ///
    /// # Returns
    ///
    /// The first sync-waiting task that can currently be promoted to background mode.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_current_promotable(&self) -> Result<TasksGetCurrentPromotableResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TASKS_GETCURRENTPROMOTABLE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_current_promotable(
        &self,
    ) -> Result<TasksGetCurrentPromotableResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TASKS_GETCURRENTPROMOTABLE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Promotes an eligible synchronously-waited task so it continues running in the background.
    ///
    /// Wire method: `session.tasks.promoteToBackground`.
    ///
    /// # Parameters
    ///
    /// * `params` - Identifier of the task to promote to background mode.
    ///
    /// # Returns
    ///
    /// Indicates whether the task was successfully promoted to background mode.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn promote_to_background(
        &self,
        params: TasksPromoteToBackgroundRequest,
    ) -> Result<TasksPromoteToBackgroundResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TASKS_PROMOTETOBACKGROUND,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn promote_to_background(
        &self,
        params: TasksPromoteToBackgroundRequest,
    ) -> Result<TasksPromoteToBackgroundResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TASKS_PROMOTETOBACKGROUND,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Atomically promotes the first promotable sync-waiting task to background mode and returns it.
    ///
    /// Wire method: `session.tasks.promoteCurrentToBackground`.
    ///
    /// # Returns
    ///
    /// The promoted task as it now exists in background mode, omitted if no promotable task was waiting.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn promote_current_to_background(
        &self,
    ) -> Result<TasksPromoteCurrentToBackgroundResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TASKS_PROMOTECURRENTTOBACKGROUND,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn promote_current_to_background(
        &self,
    ) -> Result<TasksPromoteCurrentToBackgroundResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TASKS_PROMOTECURRENTTOBACKGROUND,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Cancels a background task.
    ///
    /// Wire method: `session.tasks.cancel`.
    ///
    /// # Parameters
    ///
    /// * `params` - Identifier of the background task to cancel.
    ///
    /// # Returns
    ///
    /// Indicates whether the background task was successfully cancelled.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn cancel(&self, params: TasksCancelRequest) -> Result<TasksCancelResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_CANCEL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn cancel(
        &self,
        params: TasksCancelRequest,
    ) -> Result<TasksCancelResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_CANCEL, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Removes a completed or cancelled background task from tracking.
    ///
    /// Wire method: `session.tasks.remove`.
    ///
    /// # Parameters
    ///
    /// * `params` - Identifier of the completed or cancelled task to remove from tracking.
    ///
    /// # Returns
    ///
    /// Indicates whether the task was removed. False when the task does not exist or is still running/idle.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn remove(&self, params: TasksRemoveRequest) -> Result<TasksRemoveResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_REMOVE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn remove(
        &self,
        params: TasksRemoveRequest,
    ) -> Result<TasksRemoveResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_REMOVE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Sends a message to a background agent task.
    ///
    /// Wire method: `session.tasks.sendMessage`.
    ///
    /// # Parameters
    ///
    /// * `params` - Identifier of the target agent task, message content, and optional sender agent ID.
    ///
    /// # Returns
    ///
    /// Indicates whether the message was delivered, with an error message when delivery failed.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn send_message(
        &self,
        params: TasksSendMessageRequest,
    ) -> Result<TasksSendMessageResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_SENDMESSAGE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn send_message(
        &self,
        params: TasksSendMessageRequest,
    ) -> Result<TasksSendMessageResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_SENDMESSAGE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.telemetry.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcTelemetry<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcTelemetry<'a> {
    /// Gets the telemetry engagement ID currently associated with the session, when available.
    ///
    /// Wire method: `session.telemetry.getEngagementId`.
    ///
    /// # Returns
    ///
    /// Telemetry engagement ID for the session, when available.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_engagement_id(&self) -> Result<SessionTelemetryEngagement, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TELEMETRY_GETENGAGEMENTID,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_engagement_id(&self) -> Result<SessionTelemetryEngagement, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TELEMETRY_GETENGAGEMENTID,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Sets feature override key/value pairs to attach to subsequent telemetry events for the session.
    ///
    /// Wire method: `session.telemetry.setFeatureOverrides`.
    ///
    /// # Parameters
    ///
    /// * `params` - Feature override key/value pairs to attach to subsequent telemetry events from this session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn set_feature_overrides(
        &self,
        params: TelemetrySetFeatureOverridesRequest,
    ) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TELEMETRY_SETFEATUREOVERRIDES,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn set_feature_overrides(
        &self,
        params: TelemetrySetFeatureOverridesRequest,
    ) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TELEMETRY_SETFEATUREOVERRIDES,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }
}

/// `session.tools.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcTools<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcTools<'a> {
    /// Provides the result for a pending external tool call.
    ///
    /// Wire method: `session.tools.handlePendingToolCall`.
    ///
    /// # Parameters
    ///
    /// * `params` - Pending external tool call request ID, with the tool result or an error describing why it failed.
    ///
    /// # Returns
    ///
    /// Indicates whether the external tool call result was handled successfully.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn handle_pending_tool_call(
        &self,
        params: HandlePendingToolCallRequest,
    ) -> Result<HandlePendingToolCallResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TOOLS_HANDLEPENDINGTOOLCALL,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn handle_pending_tool_call(
        &self,
        params: HandlePendingToolCallRequest,
    ) -> Result<HandlePendingToolCallResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TOOLS_HANDLEPENDINGTOOLCALL,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Resolves, builds, and validates the runtime tool list for the session.
    ///
    /// Wire method: `session.tools.initializeAndValidate`.
    ///
    /// # Returns
    ///
    /// Resolve, build, and validate the runtime tool list for this session. Subagent sessions and consumer flows that need an initialized tool set before `send` invoke this. Default base-class implementation is a no-op for sessions that don't support tool validation.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn initialize_and_validate(&self) -> Result<ToolsInitializeAndValidateResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TOOLS_INITIALIZEANDVALIDATE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn initialize_and_validate(
        &self,
    ) -> Result<ToolsInitializeAndValidateResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TOOLS_INITIALIZEANDVALIDATE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Returns lightweight metadata for the session's currently initialized tools.
    ///
    /// Wire method: `session.tools.getCurrentMetadata`.
    ///
    /// # Returns
    ///
    /// Current lightweight tool metadata snapshot for the session.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_current_metadata(&self) -> Result<ToolsGetCurrentMetadataResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TOOLS_GETCURRENTMETADATA,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_current_metadata(
        &self,
    ) -> Result<ToolsGetCurrentMetadataResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TOOLS_GETCURRENTMETADATA,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Updates the current session's live subagent settings after user settings change. The persisted user settings remain the source of truth for future sessions.
    ///
    /// Wire method: `session.tools.updateSubagentSettings`.
    ///
    /// # Parameters
    ///
    /// * `params` - Subagent settings to apply to the current session
    ///
    /// # Returns
    ///
    /// Empty result after applying subagent settings
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn update_subagent_settings(
        &self,
        params: UpdateSubagentSettingsRequest,
    ) -> Result<ToolsUpdateSubagentSettingsResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TOOLS_UPDATESUBAGENTSETTINGS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn update_subagent_settings(
        &self,
        params: UpdateSubagentSettingsRequest,
    ) -> Result<ToolsUpdateSubagentSettingsResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_TOOLS_UPDATESUBAGENTSETTINGS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.ui.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcUi<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcUi<'a> {
    /// Runs a transient no-tools model query against the current conversation context.
    ///
    /// Wire method: `session.ui.ephemeralQuery`.
    ///
    /// # Parameters
    ///
    /// * `params` - Transient question to answer without adding it to conversation history.
    ///
    /// # Returns
    ///
    /// Transient answer generated from current conversation context.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn ephemeral_query(
        &self,
        params: UIEphemeralQueryRequest,
    ) -> Result<UIEphemeralQueryResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_UI_EPHEMERALQUERY, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn ephemeral_query(
        &self,
        params: UIEphemeralQueryRequest,
    ) -> Result<UIEphemeralQueryResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_UI_EPHEMERALQUERY, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Requests structured input from a UI-capable client.
    ///
    /// Wire method: `session.ui.elicitation`.
    ///
    /// # Parameters
    ///
    /// * `params` - Prompt message and JSON schema describing the form fields to elicit from the user.
    ///
    /// # Returns
    ///
    /// The elicitation response (accept with form values, decline, or cancel)
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn elicitation(
        &self,
        params: UIElicitationRequest,
    ) -> Result<UIElicitationResponse, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_UI_ELICITATION, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn elicitation(
        &self,
        params: UIElicitationRequest,
    ) -> Result<UIElicitationResponse, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_UI_ELICITATION, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Provides the user response for a pending elicitation request.
    ///
    /// Wire method: `session.ui.handlePendingElicitation`.
    ///
    /// # Parameters
    ///
    /// * `params` - Pending elicitation request ID and the user's response (accept/decline/cancel + form values).
    ///
    /// # Returns
    ///
    /// Indicates whether the elicitation response was accepted; false if it was already resolved by another client.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn handle_pending_elicitation(
        &self,
        params: UIHandlePendingElicitationRequest,
    ) -> Result<UIElicitationResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_HANDLEPENDINGELICITATION,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn handle_pending_elicitation(
        &self,
        params: UIHandlePendingElicitationRequest,
    ) -> Result<UIElicitationResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_HANDLEPENDINGELICITATION,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Resolves a pending `user_input.requested` event with the user's response.
    ///
    /// Wire method: `session.ui.handlePendingUserInput`.
    ///
    /// # Parameters
    ///
    /// * `params` - Request ID of a pending `user_input.requested` event and the user's response.
    ///
    /// # Returns
    ///
    /// Indicates whether the pending UI request was resolved by this call.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn handle_pending_user_input(
        &self,
        params: UIHandlePendingUserInputRequest,
    ) -> Result<UIHandlePendingResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_HANDLEPENDINGUSERINPUT,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn handle_pending_user_input(
        &self,
        params: UIHandlePendingUserInputRequest,
    ) -> Result<UIHandlePendingResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_HANDLEPENDINGUSERINPUT,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Resolves a pending `sampling.requested` event with a sampling result, or rejects it.
    ///
    /// Wire method: `session.ui.handlePendingSampling`.
    ///
    /// # Parameters
    ///
    /// * `params` - Request ID of a pending `sampling.requested` event and an optional sampling result payload (omit to reject).
    ///
    /// # Returns
    ///
    /// Indicates whether the pending UI request was resolved by this call.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn handle_pending_sampling(
        &self,
        params: UIHandlePendingSamplingRequest,
    ) -> Result<UIHandlePendingResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_HANDLEPENDINGSAMPLING,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn handle_pending_sampling(
        &self,
        params: UIHandlePendingSamplingRequest,
    ) -> Result<UIHandlePendingResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_HANDLEPENDINGSAMPLING,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Resolves a pending `auto_mode_switch.requested` event with the user's accept/decline decision.
    ///
    /// Wire method: `session.ui.handlePendingAutoModeSwitch`.
    ///
    /// # Parameters
    ///
    /// * `params` - Request ID of a pending `auto_mode_switch.requested` event and the user's response.
    ///
    /// # Returns
    ///
    /// Indicates whether the pending UI request was resolved by this call.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn handle_pending_auto_mode_switch(
        &self,
        params: UIHandlePendingAutoModeSwitchRequest,
    ) -> Result<UIHandlePendingResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_HANDLEPENDINGAUTOMODESWITCH,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn handle_pending_auto_mode_switch(
        &self,
        params: UIHandlePendingAutoModeSwitchRequest,
    ) -> Result<UIHandlePendingResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_HANDLEPENDINGAUTOMODESWITCH,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Resolves a pending `exit_plan_mode.requested` event with the user's response.
    ///
    /// Wire method: `session.ui.handlePendingExitPlanMode`.
    ///
    /// # Parameters
    ///
    /// * `params` - Request ID of a pending `exit_plan_mode.requested` event and the user's response.
    ///
    /// # Returns
    ///
    /// Indicates whether the pending UI request was resolved by this call.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn handle_pending_exit_plan_mode(
        &self,
        params: UIHandlePendingExitPlanModeRequest,
    ) -> Result<UIHandlePendingResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_HANDLEPENDINGEXITPLANMODE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn handle_pending_exit_plan_mode(
        &self,
        params: UIHandlePendingExitPlanModeRequest,
    ) -> Result<UIHandlePendingResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_HANDLEPENDINGEXITPLANMODE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Registers an in-process handler for auto-mode-switch requests so the server bridge skips dispatch.
    ///
    /// Wire method: `session.ui.registerDirectAutoModeSwitchHandler`.
    ///
    /// # Returns
    ///
    /// Register an in-process handler for `auto_mode_switch.requested` events. The caller still attaches the actual listener via the standard event-subscription mechanism; this registration solely tells the server bridge to skip its own dispatch (so a remote client doesn't race the in-process handler for the same requestId).
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn register_direct_auto_mode_switch_handler(
        &self,
    ) -> Result<UIRegisterDirectAutoModeSwitchHandlerResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_REGISTERDIRECTAUTOMODESWITCHHANDLER,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn register_direct_auto_mode_switch_handler(
        &self,
    ) -> Result<UIRegisterDirectAutoModeSwitchHandlerResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_REGISTERDIRECTAUTOMODESWITCHHANDLER,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Unregisters a previously-registered in-process auto-mode-switch handler by its opaque handle.
    ///
    /// Wire method: `session.ui.unregisterDirectAutoModeSwitchHandler`.
    ///
    /// # Parameters
    ///
    /// * `params` - Opaque handle previously returned by `registerDirectAutoModeSwitchHandler` to release.
    ///
    /// # Returns
    ///
    /// Indicates whether the handle was active and the registration count was decremented.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn unregister_direct_auto_mode_switch_handler(
        &self,
        params: UIUnregisterDirectAutoModeSwitchHandlerRequest,
    ) -> Result<UIUnregisterDirectAutoModeSwitchHandlerResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_UNREGISTERDIRECTAUTOMODESWITCHHANDLER,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn unregister_direct_auto_mode_switch_handler(
        &self,
        params: UIUnregisterDirectAutoModeSwitchHandlerRequest,
    ) -> Result<UIUnregisterDirectAutoModeSwitchHandlerResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_UI_UNREGISTERDIRECTAUTOMODESWITCHHANDLER,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.usage.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcUsage<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcUsage<'a> {
    /// Gets accumulated usage metrics for the session.
    ///
    /// Wire method: `session.usage.getMetrics`.
    ///
    /// # Returns
    ///
    /// Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_metrics(&self) -> Result<UsageGetMetricsResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_USAGE_GETMETRICS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_metrics(&self) -> Result<UsageGetMetricsResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_USAGE_GETMETRICS, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}

/// `session.workspaces.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcWorkspaces<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcWorkspaces<'a> {
    /// Gets current workspace metadata for the session.
    ///
    /// Wire method: `session.workspaces.getWorkspace`.
    ///
    /// # Returns
    ///
    /// Current workspace metadata for the session, including its absolute filesystem path when available.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn get_workspace(&self) -> Result<WorkspacesGetWorkspaceResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_WORKSPACES_GETWORKSPACE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn get_workspace(&self) -> Result<WorkspacesGetWorkspaceResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_WORKSPACES_GETWORKSPACE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Lists files stored in the session workspace files directory.
    ///
    /// Wire method: `session.workspaces.listFiles`.
    ///
    /// # Returns
    ///
    /// Relative paths of files stored in the session workspace files directory.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list_files(&self) -> Result<WorkspacesListFilesResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_WORKSPACES_LISTFILES, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list_files(&self) -> Result<WorkspacesListFilesResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_WORKSPACES_LISTFILES, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Reads a file from the session workspace files directory.
    ///
    /// Wire method: `session.workspaces.readFile`.
    ///
    /// # Parameters
    ///
    /// * `params` - Relative path of the workspace file to read.
    ///
    /// # Returns
    ///
    /// Contents of the requested workspace file as a UTF-8 string.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn read_file(
        &self,
        params: WorkspacesReadFileRequest,
    ) -> Result<WorkspacesReadFileResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_WORKSPACES_READFILE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn read_file(
        &self,
        params: WorkspacesReadFileRequest,
    ) -> Result<WorkspacesReadFileResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_WORKSPACES_READFILE, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Creates or overwrites a file in the session workspace files directory.
    ///
    /// Wire method: `session.workspaces.createFile`.
    ///
    /// # Parameters
    ///
    /// * `params` - Relative path and UTF-8 content for the workspace file to create or overwrite.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn create_file(&self, params: WorkspacesCreateFileRequest) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_WORKSPACES_CREATEFILE,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn create_file(
        &self,
        params: WorkspacesCreateFileRequest,
    ) -> Result<(), Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_WORKSPACES_CREATEFILE,
                Some(wire_params),
            )
            .await?;
        Ok(())
    }

    /// Lists workspace checkpoints in chronological order.
    ///
    /// Wire method: `session.workspaces.listCheckpoints`.
    ///
    /// # Returns
    ///
    /// Workspace checkpoints in chronological order; empty when the workspace is not enabled.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn list_checkpoints(&self) -> Result<WorkspacesListCheckpointsResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_WORKSPACES_LISTCHECKPOINTS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn list_checkpoints(&self) -> Result<WorkspacesListCheckpointsResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_WORKSPACES_LISTCHECKPOINTS,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Reads the content of a workspace checkpoint by number.
    ///
    /// Wire method: `session.workspaces.readCheckpoint`.
    ///
    /// # Parameters
    ///
    /// * `params` - Checkpoint number to read.
    ///
    /// # Returns
    ///
    /// Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn read_checkpoint(
        &self,
        params: WorkspacesReadCheckpointRequest,
    ) -> Result<WorkspacesReadCheckpointResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_WORKSPACES_READCHECKPOINT,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn read_checkpoint(
        &self,
        params: WorkspacesReadCheckpointRequest,
    ) -> Result<WorkspacesReadCheckpointResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_WORKSPACES_READCHECKPOINT,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Saves pasted content as a UTF-8 file in the session workspace.
    ///
    /// Wire method: `session.workspaces.saveLargePaste`.
    ///
    /// # Parameters
    ///
    /// * `params` - Pasted content to save as a UTF-8 file in the session workspace.
    ///
    /// # Returns
    ///
    /// Descriptor for the saved paste file, or null when the workspace is unavailable.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn save_large_paste(
        &self,
        params: WorkspacesSaveLargePasteRequest,
    ) -> Result<WorkspacesSaveLargePasteResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_WORKSPACES_SAVELARGEPASTE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn save_large_paste(
        &self,
        params: WorkspacesSaveLargePasteRequest,
    ) -> Result<WorkspacesSaveLargePasteResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(
                rpc_methods::SESSION_WORKSPACES_SAVELARGEPASTE,
                Some(wire_params),
            )
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Computes a diff for the session workspace.
    ///
    /// Wire method: `session.workspaces.diff`.
    ///
    /// # Parameters
    ///
    /// * `params` - Parameters for computing a workspace diff.
    ///
    /// # Returns
    ///
    /// Workspace diff result for the requested mode.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    #[cfg(feature = "experimental")]
    pub async fn diff(&self, params: WorkspacesDiffRequest) -> Result<WorkspaceDiffResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_WORKSPACES_DIFF, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    #[cfg(not(feature = "experimental"))]
    #[doc(hidden)]
    pub(crate) async fn diff(
        &self,
        params: WorkspacesDiffRequest,
    ) -> Result<WorkspaceDiffResult, Error> {
        let mut wire_params = serde_json::to_value(params)?;
        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_WORKSPACES_DIFF, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }
}
