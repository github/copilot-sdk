//! Auto-generated typed JSON-RPC namespace — do not edit manually.
//!
//! Generated from `api.schema.json` by `scripts/codegen/rust.ts`. The
//! [`ClientRpc`] and [`SessionRpc`] view structs let callers reach every
//! protocol method through a typed namespace tree, so wire method names
//! and request/response shapes live in exactly one place — this file.

#![allow(missing_docs)]
#![allow(clippy::too_many_arguments)]

use super::api_types::{rpc_methods, *};
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
    /// Server liveness response, including the echoed message, current timestamp, and protocol version.
    pub async fn ping(&self, params: PingRequest) -> Result<PingResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PING, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Performs the SDK server connection handshake and validates the optional connection token.
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
    pub async fn connect(&self, params: ConnectRequest) -> Result<ConnectResult, Error> {
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
    pub async fn fork(&self, params: SessionsForkRequest) -> Result<SessionsForkResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::SESSIONS_FORK, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
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

    /// `session.commands.*` sub-namespace.
    pub fn commands(&self) -> SessionRpcCommands<'a> {
        SessionRpcCommands {
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

    /// `session.mcp.*` sub-namespace.
    pub fn mcp(&self) -> SessionRpcMcp<'a> {
        SessionRpcMcp {
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

    /// `session.remote.*` sub-namespace.
    pub fn remote(&self) -> SessionRpcRemote<'a> {
        SessionRpcRemote {
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
    pub async fn suspend(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SUSPEND, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Emits a user-visible session log event.
    ///
    /// Wire method: `session.log`.
    ///
    /// # Parameters
    ///
    /// * `params` - Message text, optional severity level, persistence flag, and optional follow-up URL.
    ///
    /// # Returns
    ///
    /// Identifier of the session event that was emitted for the log message.
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
    pub async fn list(&self) -> Result<AgentList, Error> {
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
    pub async fn get_current(&self) -> Result<AgentGetCurrentResult, Error> {
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
    pub async fn deselect(&self) -> Result<(), Error> {
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
    pub async fn reload(&self) -> Result<AgentReloadResult, Error> {
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
    pub async fn get_status(&self) -> Result<SessionAuthStatus, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AUTH_GETSTATUS, Some(wire_params))
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
    pub async fn list(&self) -> Result<CommandList, Error> {
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

    /// Responds to a queued command request from the session.
    ///
    /// Wire method: `session.commands.respondToQueuedCommand`.
    ///
    /// # Parameters
    ///
    /// * `params` - Queued command request ID and the result indicating whether the client handled it.
    ///
    /// # Returns
    ///
    /// Indicates whether the queued-command response was accepted by the session.
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
    pub async fn list(&self) -> Result<ExtensionList, Error> {
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
    pub async fn reload(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EXTENSIONS_RELOAD, Some(wire_params))
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
    /// Compaction outcome with the number of tokens and messages removed and the resulting context window breakdown.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn compact(&self) -> Result<HistoryCompactResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
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
}

/// `session.mcp.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcMcp<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcMcp<'a> {
    /// `session.mcp.oauth.*` sub-namespace.
    pub fn oauth(&self) -> SessionRpcMcpOauth<'a> {
        SessionRpcMcpOauth {
            session: self.session,
        }
    }

    /// Lists MCP servers configured for the session and their connection status.
    ///
    /// Wire method: `session.mcp.list`.
    ///
    /// # Returns
    ///
    /// MCP servers configured for the session, with their connection status.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This API is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases. Pin both the
    /// SDK and CLI versions if your code depends on it.
    ///
    /// </div>
    pub async fn list(&self) -> Result<McpServerList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_LIST, Some(wire_params))
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
    pub async fn reload(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_RELOAD, Some(wire_params))
            .await?;
        Ok(())
    }
}

/// `session.mcp.oauth.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcMcpOauth<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcMcpOauth<'a> {
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
    /// The agent mode. Valid values: "interactive", "plan", "autopilot".
    pub async fn get(&self) -> Result<SessionMode, Error> {
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
    /// The currently selected model for the session.
    pub async fn get_current(&self) -> Result<CurrentModel, Error> {
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
    /// * `params` - Target model identifier and optional reasoning effort, summary, and capability overrides.
    ///
    /// # Returns
    ///
    /// The model identifier active on the session after the switch.
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
    pub async fn get(&self) -> Result<NameGetResult, Error> {
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
}

/// `session.permissions.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcPermissions<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcPermissions<'a> {
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

    /// Enables or disables automatic approval of tool permission requests for the session.
    ///
    /// Wire method: `session.permissions.setApproveAll`.
    ///
    /// # Parameters
    ///
    /// * `params` - Whether to auto-approve all tool permission requests for the rest of the session.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
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

    /// Clears session-scoped tool permission approvals.
    ///
    /// Wire method: `session.permissions.resetSessionApprovals`.
    ///
    /// # Returns
    ///
    /// Indicates whether the operation succeeded.
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
    pub async fn read(&self) -> Result<PlanReadResult, Error> {
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

    /// Deletes the session plan file from the workspace.
    ///
    /// Wire method: `session.plan.delete`.
    pub async fn delete(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLAN_DELETE, Some(wire_params))
            .await?;
        Ok(())
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
    pub async fn list(&self) -> Result<PluginList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLUGINS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
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
    pub async fn disable(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_REMOTE_DISABLE, Some(wire_params))
            .await?;
        Ok(())
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
    pub async fn list(&self) -> Result<SkillList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_LIST, Some(wire_params))
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
    pub async fn reload(&self) -> Result<SkillsLoadDiagnostics, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_RELOAD, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
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
    pub async fn list(&self) -> Result<TaskList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_LIST, Some(wire_params))
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
}

/// `session.ui.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcUi<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcUi<'a> {
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
    pub async fn get_metrics(&self) -> Result<UsageGetMetricsResult, Error> {
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
    /// Current workspace metadata for the session, or null when not available.
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

    /// Lists files stored in the session workspace files directory.
    ///
    /// Wire method: `session.workspaces.listFiles`.
    ///
    /// # Returns
    ///
    /// Relative paths of files stored in the session workspace files directory.
    pub async fn list_files(&self) -> Result<WorkspacesListFilesResult, Error> {
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

    /// Creates or overwrites a file in the session workspace files directory.
    ///
    /// Wire method: `session.workspaces.createFile`.
    ///
    /// # Parameters
    ///
    /// * `params` - Relative path and UTF-8 content for the workspace file to create or overwrite.
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
}
