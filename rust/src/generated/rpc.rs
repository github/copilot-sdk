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

    /// Wire method: `ping`.
    pub async fn ping(&self, params: PingRequest) -> Result<PingResult, Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::PING, Some(wire_params))
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
    /// Wire method: `account.getQuota`.
    pub async fn get_quota(&self) -> Result<AccountGetQuotaResult, Error> {
        let wire_params = serde_json::json!({});
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

    /// Wire method: `mcp.discover`.
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
    /// Wire method: `mcp.config.list`.
    pub async fn list(&self) -> Result<McpConfigList, Error> {
        let wire_params = serde_json::json!({});
        let _value = self
            .client
            .call(rpc_methods::MCP_CONFIG_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Wire method: `mcp.config.add`.
    pub async fn add(&self, params: McpConfigAddRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::MCP_CONFIG_ADD, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Wire method: `mcp.config.update`.
    pub async fn update(&self, params: McpConfigUpdateRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::MCP_CONFIG_UPDATE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Wire method: `mcp.config.remove`.
    pub async fn remove(&self, params: McpConfigRemoveRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::MCP_CONFIG_REMOVE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Wire method: `mcp.config.enable`.
    pub async fn enable(&self, params: McpConfigEnableRequest) -> Result<(), Error> {
        let wire_params = serde_json::to_value(params)?;
        let _value = self
            .client
            .call(rpc_methods::MCP_CONFIG_ENABLE, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Wire method: `mcp.config.disable`.
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
    /// Wire method: `models.list`.
    pub async fn list(&self) -> Result<ModelList, Error> {
        let wire_params = serde_json::json!({});
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
    /// Wire method: `sessionFs.setProvider`.
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
    /// Wire method: `sessions.fork`.
    /// Stability: `experimental`.
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

    /// Wire method: `skills.discover`.
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
    /// Wire method: `skills.config.setDisabledSkills`.
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
    /// Wire method: `tools.list`.
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

    /// Wire method: `session.log`.
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
    /// Wire method: `session.agent.list`.
    /// Stability: `experimental`.
    pub async fn list(&self) -> Result<AgentList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AGENT_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Wire method: `session.agent.getCurrent`.
    /// Stability: `experimental`.
    pub async fn get_current(&self) -> Result<AgentGetCurrentResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AGENT_GETCURRENT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Wire method: `session.agent.select`.
    /// Stability: `experimental`.
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

    /// Wire method: `session.agent.deselect`.
    /// Stability: `experimental`.
    pub async fn deselect(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_AGENT_DESELECT, Some(wire_params))
            .await?;
        Ok(())
    }

    /// Wire method: `session.agent.reload`.
    /// Stability: `experimental`.
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
    /// Wire method: `session.auth.getStatus`.
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
    /// Wire method: `session.commands.handlePendingCommand`.
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
}

/// `session.extensions.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcExtensions<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcExtensions<'a> {
    /// Wire method: `session.extensions.list`.
    /// Stability: `experimental`.
    pub async fn list(&self) -> Result<ExtensionList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_EXTENSIONS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Wire method: `session.extensions.enable`.
    /// Stability: `experimental`.
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

    /// Wire method: `session.extensions.disable`.
    /// Stability: `experimental`.
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

    /// Wire method: `session.extensions.reload`.
    /// Stability: `experimental`.
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
    /// Wire method: `session.fleet.start`.
    /// Stability: `experimental`.
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
    /// Wire method: `session.history.compact`.
    /// Stability: `experimental`.
    pub async fn compact(&self) -> Result<HistoryCompactResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_HISTORY_COMPACT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Wire method: `session.history.truncate`.
    /// Stability: `experimental`.
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
    /// Wire method: `session.instructions.getSources`.
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

    /// Wire method: `session.mcp.list`.
    /// Stability: `experimental`.
    pub async fn list(&self) -> Result<McpServerList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MCP_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Wire method: `session.mcp.enable`.
    /// Stability: `experimental`.
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

    /// Wire method: `session.mcp.disable`.
    /// Stability: `experimental`.
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

    /// Wire method: `session.mcp.reload`.
    /// Stability: `experimental`.
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
    /// Wire method: `session.mcp.oauth.login`.
    /// Stability: `experimental`.
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
    /// Wire method: `session.mode.get`.
    pub async fn get(&self) -> Result<SessionMode, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODE_GET, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Wire method: `session.mode.set`.
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
    /// Wire method: `session.model.getCurrent`.
    pub async fn get_current(&self) -> Result<CurrentModel, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_MODEL_GETCURRENT, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Wire method: `session.model.switchTo`.
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
    /// Wire method: `session.name.get`.
    pub async fn get(&self) -> Result<NameGetResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_NAME_GET, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Wire method: `session.name.set`.
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
    /// Wire method: `session.permissions.handlePendingPermissionRequest`.
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

    /// Wire method: `session.permissions.setApproveAll`.
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

    /// Wire method: `session.permissions.resetSessionApprovals`.
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
    /// Wire method: `session.plan.read`.
    pub async fn read(&self) -> Result<PlanReadResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_PLAN_READ, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Wire method: `session.plan.update`.
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
    /// Wire method: `session.plugins.list`.
    /// Stability: `experimental`.
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

/// `session.shell.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcShell<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcShell<'a> {
    /// Wire method: `session.shell.exec`.
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

    /// Wire method: `session.shell.kill`.
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
    /// Wire method: `session.skills.list`.
    /// Stability: `experimental`.
    pub async fn list(&self) -> Result<SkillList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Wire method: `session.skills.enable`.
    /// Stability: `experimental`.
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

    /// Wire method: `session.skills.disable`.
    /// Stability: `experimental`.
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

    /// Wire method: `session.skills.reload`.
    /// Stability: `experimental`.
    pub async fn reload(&self) -> Result<(), Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_SKILLS_RELOAD, Some(wire_params))
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
    /// Wire method: `session.tasks.startAgent`.
    /// Stability: `experimental`.
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

    /// Wire method: `session.tasks.list`.
    /// Stability: `experimental`.
    pub async fn list(&self) -> Result<TaskList, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_TASKS_LIST, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Wire method: `session.tasks.promoteToBackground`.
    /// Stability: `experimental`.
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

    /// Wire method: `session.tasks.cancel`.
    /// Stability: `experimental`.
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

    /// Wire method: `session.tasks.remove`.
    /// Stability: `experimental`.
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
}

/// `session.tools.*` RPCs.
#[derive(Clone, Copy)]
pub struct SessionRpcTools<'a> {
    pub(crate) session: &'a Session,
}

impl<'a> SessionRpcTools<'a> {
    /// Wire method: `session.tools.handlePendingToolCall`.
    pub async fn handle_pending_tool_call(
        &self,
        params: ToolsHandlePendingToolCallRequest,
    ) -> Result<HandleToolCallResult, Error> {
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
    /// Wire method: `session.ui.elicitation`.
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

    /// Wire method: `session.ui.handlePendingElicitation`.
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
    /// Wire method: `session.usage.getMetrics`.
    /// Stability: `experimental`.
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
    /// Wire method: `session.workspaces.getWorkspace`.
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

    /// Wire method: `session.workspaces.listFiles`.
    pub async fn list_files(&self) -> Result<WorkspacesListFilesResult, Error> {
        let wire_params = serde_json::json!({ "sessionId": self.session.id() });
        let _value = self
            .session
            .client()
            .call(rpc_methods::SESSION_WORKSPACES_LISTFILES, Some(wire_params))
            .await?;
        Ok(serde_json::from_value(_value)?)
    }

    /// Wire method: `session.workspaces.readFile`.
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

    /// Wire method: `session.workspaces.createFile`.
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
