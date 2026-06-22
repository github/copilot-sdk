//! Auto-generated from api.schema.json — do not edit manually.

#![allow(clippy::large_enum_variant)]
#![allow(dead_code)]
#![allow(rustdoc::invalid_html_tags)]

use std::collections::HashMap;

use serde::{Deserialize, Serialize};

use super::session_events::{
    AbortReason, ContextTier, McpServerSource, McpServerStatus, PermissionPromptRequest,
    PermissionRule, ReasoningSummary, SessionMode, ShutdownType, SkillSource,
    UserToolSessionApproval,
};
use crate::types::{RequestId, SessionEvent, SessionId};

/// JSON-RPC method name constants.
pub mod rpc_methods {
    /// `ping`
    pub const PING: &str = "ping";
    /// `connect`
    pub const CONNECT: &str = "connect";
    /// `models.list`
    pub const MODELS_LIST: &str = "models.list";
    /// `tools.list`
    pub const TOOLS_LIST: &str = "tools.list";
    /// `account.getQuota`
    pub const ACCOUNT_GETQUOTA: &str = "account.getQuota";
    /// `secrets.addFilterValues`
    pub const SECRETS_ADDFILTERVALUES: &str = "secrets.addFilterValues";
    /// `mcp.config.list`
    pub const MCP_CONFIG_LIST: &str = "mcp.config.list";
    /// `mcp.config.add`
    pub const MCP_CONFIG_ADD: &str = "mcp.config.add";
    /// `mcp.config.update`
    pub const MCP_CONFIG_UPDATE: &str = "mcp.config.update";
    /// `mcp.config.remove`
    pub const MCP_CONFIG_REMOVE: &str = "mcp.config.remove";
    /// `mcp.config.enable`
    pub const MCP_CONFIG_ENABLE: &str = "mcp.config.enable";
    /// `mcp.config.disable`
    pub const MCP_CONFIG_DISABLE: &str = "mcp.config.disable";
    /// `mcp.config.reload`
    pub const MCP_CONFIG_RELOAD: &str = "mcp.config.reload";
    /// `mcp.discover`
    pub const MCP_DISCOVER: &str = "mcp.discover";
    /// `plugins.list`
    pub const PLUGINS_LIST: &str = "plugins.list";
    /// `plugins.install`
    pub const PLUGINS_INSTALL: &str = "plugins.install";
    /// `plugins.uninstall`
    pub const PLUGINS_UNINSTALL: &str = "plugins.uninstall";
    /// `plugins.update`
    pub const PLUGINS_UPDATE: &str = "plugins.update";
    /// `plugins.updateAll`
    pub const PLUGINS_UPDATEALL: &str = "plugins.updateAll";
    /// `plugins.enable`
    pub const PLUGINS_ENABLE: &str = "plugins.enable";
    /// `plugins.disable`
    pub const PLUGINS_DISABLE: &str = "plugins.disable";
    /// `plugins.marketplaces.list`
    pub const PLUGINS_MARKETPLACES_LIST: &str = "plugins.marketplaces.list";
    /// `plugins.marketplaces.add`
    pub const PLUGINS_MARKETPLACES_ADD: &str = "plugins.marketplaces.add";
    /// `plugins.marketplaces.remove`
    pub const PLUGINS_MARKETPLACES_REMOVE: &str = "plugins.marketplaces.remove";
    /// `plugins.marketplaces.browse`
    pub const PLUGINS_MARKETPLACES_BROWSE: &str = "plugins.marketplaces.browse";
    /// `plugins.marketplaces.refresh`
    pub const PLUGINS_MARKETPLACES_REFRESH: &str = "plugins.marketplaces.refresh";
    /// `skills.config.setDisabledSkills`
    pub const SKILLS_CONFIG_SETDISABLEDSKILLS: &str = "skills.config.setDisabledSkills";
    /// `skills.discover`
    pub const SKILLS_DISCOVER: &str = "skills.discover";
    /// `skills.getDiscoveryPaths`
    pub const SKILLS_GETDISCOVERYPATHS: &str = "skills.getDiscoveryPaths";
    /// `agents.discover`
    pub const AGENTS_DISCOVER: &str = "agents.discover";
    /// `agents.getDiscoveryPaths`
    pub const AGENTS_GETDISCOVERYPATHS: &str = "agents.getDiscoveryPaths";
    /// `instructions.discover`
    pub const INSTRUCTIONS_DISCOVER: &str = "instructions.discover";
    /// `instructions.getDiscoveryPaths`
    pub const INSTRUCTIONS_GETDISCOVERYPATHS: &str = "instructions.getDiscoveryPaths";
    /// `user.settings.reload`
    pub const USER_SETTINGS_RELOAD: &str = "user.settings.reload";
    /// `runtime.shutdown`
    pub const RUNTIME_SHUTDOWN: &str = "runtime.shutdown";
    /// `sessionFs.setProvider`
    pub const SESSIONFS_SETPROVIDER: &str = "sessionFs.setProvider";
    /// `llmInference.setProvider`
    pub const LLMINFERENCE_SETPROVIDER: &str = "llmInference.setProvider";
    /// `llmInference.httpResponseStart`
    pub const LLMINFERENCE_HTTPRESPONSESTART: &str = "llmInference.httpResponseStart";
    /// `llmInference.httpResponseChunk`
    pub const LLMINFERENCE_HTTPRESPONSECHUNK: &str = "llmInference.httpResponseChunk";
    /// `sessions.open`
    pub const SESSIONS_OPEN: &str = "sessions.open";
    /// `sessions.fork`
    pub const SESSIONS_FORK: &str = "sessions.fork";
    /// `sessions.connect`
    pub const SESSIONS_CONNECT: &str = "sessions.connect";
    /// `sessions.list`
    pub const SESSIONS_LIST: &str = "sessions.list";
    /// `sessions.findByTaskId`
    pub const SESSIONS_FINDBYTASKID: &str = "sessions.findByTaskId";
    /// `sessions.findByPrefix`
    pub const SESSIONS_FINDBYPREFIX: &str = "sessions.findByPrefix";
    /// `sessions.getLastForContext`
    pub const SESSIONS_GETLASTFORCONTEXT: &str = "sessions.getLastForContext";
    /// `sessions.getEventFilePath`
    pub const SESSIONS_GETEVENTFILEPATH: &str = "sessions.getEventFilePath";
    /// `sessions.getSizes`
    pub const SESSIONS_GETSIZES: &str = "sessions.getSizes";
    /// `sessions.checkInUse`
    pub const SESSIONS_CHECKINUSE: &str = "sessions.checkInUse";
    /// `sessions.getPersistedRemoteSteerable`
    pub const SESSIONS_GETPERSISTEDREMOTESTEERABLE: &str = "sessions.getPersistedRemoteSteerable";
    /// `sessions.close`
    pub const SESSIONS_CLOSE: &str = "sessions.close";
    /// `sessions.bulkDelete`
    pub const SESSIONS_BULKDELETE: &str = "sessions.bulkDelete";
    /// `sessions.pruneOld`
    pub const SESSIONS_PRUNEOLD: &str = "sessions.pruneOld";
    /// `sessions.save`
    pub const SESSIONS_SAVE: &str = "sessions.save";
    /// `sessions.releaseLock`
    pub const SESSIONS_RELEASELOCK: &str = "sessions.releaseLock";
    /// `sessions.enrichMetadata`
    pub const SESSIONS_ENRICHMETADATA: &str = "sessions.enrichMetadata";
    /// `sessions.reloadPluginHooks`
    pub const SESSIONS_RELOADPLUGINHOOKS: &str = "sessions.reloadPluginHooks";
    /// `sessions.loadDeferredRepoHooks`
    pub const SESSIONS_LOADDEFERREDREPOHOOKS: &str = "sessions.loadDeferredRepoHooks";
    /// `sessions.setAdditionalPlugins`
    pub const SESSIONS_SETADDITIONALPLUGINS: &str = "sessions.setAdditionalPlugins";
    /// `sessions.getBoardEntryCount`
    pub const SESSIONS_GETBOARDENTRYCOUNT: &str = "sessions.getBoardEntryCount";
    /// `sessions.startRemoteControl`
    pub const SESSIONS_STARTREMOTECONTROL: &str = "sessions.startRemoteControl";
    /// `sessions.transferRemoteControl`
    pub const SESSIONS_TRANSFERREMOTECONTROL: &str = "sessions.transferRemoteControl";
    /// `sessions.setRemoteControlSteering`
    pub const SESSIONS_SETREMOTECONTROLSTEERING: &str = "sessions.setRemoteControlSteering";
    /// `sessions.stopRemoteControl`
    pub const SESSIONS_STOPREMOTECONTROL: &str = "sessions.stopRemoteControl";
    /// `sessions.getRemoteControlStatus`
    pub const SESSIONS_GETREMOTECONTROLSTATUS: &str = "sessions.getRemoteControlStatus";
    /// `sessions.pollSpawnedSessions`
    pub const SESSIONS_POLLSPAWNEDSESSIONS: &str = "sessions.pollSpawnedSessions";
    /// `sessions.registerExtensionToolsOnSession`
    pub const SESSIONS_REGISTEREXTENSIONTOOLSONSESSION: &str =
        "sessions.registerExtensionToolsOnSession";
    /// `sessions.configureSessionExtensions`
    pub const SESSIONS_CONFIGURESESSIONEXTENSIONS: &str = "sessions.configureSessionExtensions";
    /// `agentRegistry.spawn`
    pub const AGENTREGISTRY_SPAWN: &str = "agentRegistry.spawn";
    /// `session.suspend`
    pub const SESSION_SUSPEND: &str = "session.suspend";
    /// `session.send`
    pub const SESSION_SEND: &str = "session.send";
    /// `session.abort`
    pub const SESSION_ABORT: &str = "session.abort";
    /// `session.shutdown`
    pub const SESSION_SHUTDOWN: &str = "session.shutdown";
    /// `session.auth.getStatus`
    pub const SESSION_AUTH_GETSTATUS: &str = "session.auth.getStatus";
    /// `session.auth.setCredentials`
    pub const SESSION_AUTH_SETCREDENTIALS: &str = "session.auth.setCredentials";
    /// `session.canvas.list`
    pub const SESSION_CANVAS_LIST: &str = "session.canvas.list";
    /// `session.canvas.listOpen`
    pub const SESSION_CANVAS_LISTOPEN: &str = "session.canvas.listOpen";
    /// `session.canvas.open`
    pub const SESSION_CANVAS_OPEN: &str = "session.canvas.open";
    /// `session.canvas.close`
    pub const SESSION_CANVAS_CLOSE: &str = "session.canvas.close";
    /// `session.canvas.action.invoke`
    pub const SESSION_CANVAS_ACTION_INVOKE: &str = "session.canvas.action.invoke";
    /// `session.model.getCurrent`
    pub const SESSION_MODEL_GETCURRENT: &str = "session.model.getCurrent";
    /// `session.model.switchTo`
    pub const SESSION_MODEL_SWITCHTO: &str = "session.model.switchTo";
    /// `session.model.setReasoningEffort`
    pub const SESSION_MODEL_SETREASONINGEFFORT: &str = "session.model.setReasoningEffort";
    /// `session.model.list`
    pub const SESSION_MODEL_LIST: &str = "session.model.list";
    /// `session.mode.get`
    pub const SESSION_MODE_GET: &str = "session.mode.get";
    /// `session.mode.set`
    pub const SESSION_MODE_SET: &str = "session.mode.set";
    /// `session.name.get`
    pub const SESSION_NAME_GET: &str = "session.name.get";
    /// `session.name.set`
    pub const SESSION_NAME_SET: &str = "session.name.set";
    /// `session.name.setAuto`
    pub const SESSION_NAME_SETAUTO: &str = "session.name.setAuto";
    /// `session.plan.read`
    pub const SESSION_PLAN_READ: &str = "session.plan.read";
    /// `session.plan.update`
    pub const SESSION_PLAN_UPDATE: &str = "session.plan.update";
    /// `session.plan.delete`
    pub const SESSION_PLAN_DELETE: &str = "session.plan.delete";
    /// `session.plan.readSqlTodos`
    pub const SESSION_PLAN_READSQLTODOS: &str = "session.plan.readSqlTodos";
    /// `session.plan.readSqlTodosWithDependencies`
    pub const SESSION_PLAN_READSQLTODOSWITHDEPENDENCIES: &str =
        "session.plan.readSqlTodosWithDependencies";
    /// `session.workspaces.getWorkspace`
    pub const SESSION_WORKSPACES_GETWORKSPACE: &str = "session.workspaces.getWorkspace";
    /// `session.workspaces.listFiles`
    pub const SESSION_WORKSPACES_LISTFILES: &str = "session.workspaces.listFiles";
    /// `session.workspaces.readFile`
    pub const SESSION_WORKSPACES_READFILE: &str = "session.workspaces.readFile";
    /// `session.workspaces.createFile`
    pub const SESSION_WORKSPACES_CREATEFILE: &str = "session.workspaces.createFile";
    /// `session.workspaces.listCheckpoints`
    pub const SESSION_WORKSPACES_LISTCHECKPOINTS: &str = "session.workspaces.listCheckpoints";
    /// `session.workspaces.readCheckpoint`
    pub const SESSION_WORKSPACES_READCHECKPOINT: &str = "session.workspaces.readCheckpoint";
    /// `session.workspaces.saveLargePaste`
    pub const SESSION_WORKSPACES_SAVELARGEPASTE: &str = "session.workspaces.saveLargePaste";
    /// `session.workspaces.diff`
    pub const SESSION_WORKSPACES_DIFF: &str = "session.workspaces.diff";
    /// `session.instructions.getSources`
    pub const SESSION_INSTRUCTIONS_GETSOURCES: &str = "session.instructions.getSources";
    /// `session.fleet.start`
    pub const SESSION_FLEET_START: &str = "session.fleet.start";
    /// `session.agent.list`
    pub const SESSION_AGENT_LIST: &str = "session.agent.list";
    /// `session.agent.getCurrent`
    pub const SESSION_AGENT_GETCURRENT: &str = "session.agent.getCurrent";
    /// `session.agent.select`
    pub const SESSION_AGENT_SELECT: &str = "session.agent.select";
    /// `session.agent.deselect`
    pub const SESSION_AGENT_DESELECT: &str = "session.agent.deselect";
    /// `session.agent.reload`
    pub const SESSION_AGENT_RELOAD: &str = "session.agent.reload";
    /// `session.tasks.startAgent`
    pub const SESSION_TASKS_STARTAGENT: &str = "session.tasks.startAgent";
    /// `session.tasks.list`
    pub const SESSION_TASKS_LIST: &str = "session.tasks.list";
    /// `session.tasks.refresh`
    pub const SESSION_TASKS_REFRESH: &str = "session.tasks.refresh";
    /// `session.tasks.waitForPending`
    pub const SESSION_TASKS_WAITFORPENDING: &str = "session.tasks.waitForPending";
    /// `session.tasks.getProgress`
    pub const SESSION_TASKS_GETPROGRESS: &str = "session.tasks.getProgress";
    /// `session.tasks.getCurrentPromotable`
    pub const SESSION_TASKS_GETCURRENTPROMOTABLE: &str = "session.tasks.getCurrentPromotable";
    /// `session.tasks.promoteToBackground`
    pub const SESSION_TASKS_PROMOTETOBACKGROUND: &str = "session.tasks.promoteToBackground";
    /// `session.tasks.promoteCurrentToBackground`
    pub const SESSION_TASKS_PROMOTECURRENTTOBACKGROUND: &str =
        "session.tasks.promoteCurrentToBackground";
    /// `session.tasks.cancel`
    pub const SESSION_TASKS_CANCEL: &str = "session.tasks.cancel";
    /// `session.tasks.remove`
    pub const SESSION_TASKS_REMOVE: &str = "session.tasks.remove";
    /// `session.tasks.sendMessage`
    pub const SESSION_TASKS_SENDMESSAGE: &str = "session.tasks.sendMessage";
    /// `session.skills.list`
    pub const SESSION_SKILLS_LIST: &str = "session.skills.list";
    /// `session.skills.getInvoked`
    pub const SESSION_SKILLS_GETINVOKED: &str = "session.skills.getInvoked";
    /// `session.skills.enable`
    pub const SESSION_SKILLS_ENABLE: &str = "session.skills.enable";
    /// `session.skills.disable`
    pub const SESSION_SKILLS_DISABLE: &str = "session.skills.disable";
    /// `session.skills.reload`
    pub const SESSION_SKILLS_RELOAD: &str = "session.skills.reload";
    /// `session.skills.ensureLoaded`
    pub const SESSION_SKILLS_ENSURELOADED: &str = "session.skills.ensureLoaded";
    /// `session.mcp.list`
    pub const SESSION_MCP_LIST: &str = "session.mcp.list";
    /// `session.mcp.listTools`
    pub const SESSION_MCP_LISTTOOLS: &str = "session.mcp.listTools";
    /// `session.mcp.enable`
    pub const SESSION_MCP_ENABLE: &str = "session.mcp.enable";
    /// `session.mcp.disable`
    pub const SESSION_MCP_DISABLE: &str = "session.mcp.disable";
    /// `session.mcp.reload`
    pub const SESSION_MCP_RELOAD: &str = "session.mcp.reload";
    /// `session.mcp.reloadWithConfig`
    pub const SESSION_MCP_RELOADWITHCONFIG: &str = "session.mcp.reloadWithConfig";
    /// `session.mcp.executeSampling`
    pub const SESSION_MCP_EXECUTESAMPLING: &str = "session.mcp.executeSampling";
    /// `session.mcp.cancelSamplingExecution`
    pub const SESSION_MCP_CANCELSAMPLINGEXECUTION: &str = "session.mcp.cancelSamplingExecution";
    /// `session.mcp.setEnvValueMode`
    pub const SESSION_MCP_SETENVVALUEMODE: &str = "session.mcp.setEnvValueMode";
    /// `session.mcp.removeGitHub`
    pub const SESSION_MCP_REMOVEGITHUB: &str = "session.mcp.removeGitHub";
    /// `session.mcp.configureGitHub`
    pub const SESSION_MCP_CONFIGUREGITHUB: &str = "session.mcp.configureGitHub";
    /// `session.mcp.startServer`
    pub const SESSION_MCP_STARTSERVER: &str = "session.mcp.startServer";
    /// `session.mcp.restartServer`
    pub const SESSION_MCP_RESTARTSERVER: &str = "session.mcp.restartServer";
    /// `session.mcp.stopServer`
    pub const SESSION_MCP_STOPSERVER: &str = "session.mcp.stopServer";
    /// `session.mcp.registerExternalClient`
    pub const SESSION_MCP_REGISTEREXTERNALCLIENT: &str = "session.mcp.registerExternalClient";
    /// `session.mcp.unregisterExternalClient`
    pub const SESSION_MCP_UNREGISTEREXTERNALCLIENT: &str = "session.mcp.unregisterExternalClient";
    /// `session.mcp.isServerRunning`
    pub const SESSION_MCP_ISSERVERRUNNING: &str = "session.mcp.isServerRunning";
    /// `session.mcp.oauth.respond`
    pub const SESSION_MCP_OAUTH_RESPOND: &str = "session.mcp.oauth.respond";
    /// `session.mcp.oauth.handlePendingRequest`
    pub const SESSION_MCP_OAUTH_HANDLEPENDINGREQUEST: &str =
        "session.mcp.oauth.handlePendingRequest";
    /// `session.mcp.oauth.login`
    pub const SESSION_MCP_OAUTH_LOGIN: &str = "session.mcp.oauth.login";
    /// `session.mcp.apps.readResource`
    pub const SESSION_MCP_APPS_READRESOURCE: &str = "session.mcp.apps.readResource";
    /// `session.mcp.apps.listTools`
    pub const SESSION_MCP_APPS_LISTTOOLS: &str = "session.mcp.apps.listTools";
    /// `session.mcp.apps.callTool`
    pub const SESSION_MCP_APPS_CALLTOOL: &str = "session.mcp.apps.callTool";
    /// `session.mcp.apps.setHostContext`
    pub const SESSION_MCP_APPS_SETHOSTCONTEXT: &str = "session.mcp.apps.setHostContext";
    /// `session.mcp.apps.getHostContext`
    pub const SESSION_MCP_APPS_GETHOSTCONTEXT: &str = "session.mcp.apps.getHostContext";
    /// `session.mcp.apps.diagnose`
    pub const SESSION_MCP_APPS_DIAGNOSE: &str = "session.mcp.apps.diagnose";
    /// `session.plugins.list`
    pub const SESSION_PLUGINS_LIST: &str = "session.plugins.list";
    /// `session.plugins.reload`
    pub const SESSION_PLUGINS_RELOAD: &str = "session.plugins.reload";
    /// `session.provider.getEndpoint`
    pub const SESSION_PROVIDER_GETENDPOINT: &str = "session.provider.getEndpoint";
    /// `session.options.update`
    pub const SESSION_OPTIONS_UPDATE: &str = "session.options.update";
    /// `session.lsp.initialize`
    pub const SESSION_LSP_INITIALIZE: &str = "session.lsp.initialize";
    /// `session.extensions.list`
    pub const SESSION_EXTENSIONS_LIST: &str = "session.extensions.list";
    /// `session.extensions.enable`
    pub const SESSION_EXTENSIONS_ENABLE: &str = "session.extensions.enable";
    /// `session.extensions.disable`
    pub const SESSION_EXTENSIONS_DISABLE: &str = "session.extensions.disable";
    /// `session.extensions.reload`
    pub const SESSION_EXTENSIONS_RELOAD: &str = "session.extensions.reload";
    /// `session.extensions.sendAttachmentsToMessage`
    pub const SESSION_EXTENSIONS_SENDATTACHMENTSTOMESSAGE: &str =
        "session.extensions.sendAttachmentsToMessage";
    /// `session.tools.handlePendingToolCall`
    pub const SESSION_TOOLS_HANDLEPENDINGTOOLCALL: &str = "session.tools.handlePendingToolCall";
    /// `session.tools.initializeAndValidate`
    pub const SESSION_TOOLS_INITIALIZEANDVALIDATE: &str = "session.tools.initializeAndValidate";
    /// `session.tools.getCurrentMetadata`
    pub const SESSION_TOOLS_GETCURRENTMETADATA: &str = "session.tools.getCurrentMetadata";
    /// `session.tools.updateSubagentSettings`
    pub const SESSION_TOOLS_UPDATESUBAGENTSETTINGS: &str = "session.tools.updateSubagentSettings";
    /// `session.commands.list`
    pub const SESSION_COMMANDS_LIST: &str = "session.commands.list";
    /// `session.commands.invoke`
    pub const SESSION_COMMANDS_INVOKE: &str = "session.commands.invoke";
    /// `session.commands.handlePendingCommand`
    pub const SESSION_COMMANDS_HANDLEPENDINGCOMMAND: &str = "session.commands.handlePendingCommand";
    /// `session.commands.execute`
    pub const SESSION_COMMANDS_EXECUTE: &str = "session.commands.execute";
    /// `session.commands.enqueue`
    pub const SESSION_COMMANDS_ENQUEUE: &str = "session.commands.enqueue";
    /// `session.commands.respondToQueuedCommand`
    pub const SESSION_COMMANDS_RESPONDTOQUEUEDCOMMAND: &str =
        "session.commands.respondToQueuedCommand";
    /// `session.telemetry.getEngagementId`
    pub const SESSION_TELEMETRY_GETENGAGEMENTID: &str = "session.telemetry.getEngagementId";
    /// `session.telemetry.setFeatureOverrides`
    pub const SESSION_TELEMETRY_SETFEATUREOVERRIDES: &str = "session.telemetry.setFeatureOverrides";
    /// `session.ui.ephemeralQuery`
    pub const SESSION_UI_EPHEMERALQUERY: &str = "session.ui.ephemeralQuery";
    /// `session.ui.elicitation`
    pub const SESSION_UI_ELICITATION: &str = "session.ui.elicitation";
    /// `session.ui.handlePendingElicitation`
    pub const SESSION_UI_HANDLEPENDINGELICITATION: &str = "session.ui.handlePendingElicitation";
    /// `session.ui.handlePendingUserInput`
    pub const SESSION_UI_HANDLEPENDINGUSERINPUT: &str = "session.ui.handlePendingUserInput";
    /// `session.ui.handlePendingSampling`
    pub const SESSION_UI_HANDLEPENDINGSAMPLING: &str = "session.ui.handlePendingSampling";
    /// `session.ui.handlePendingAutoModeSwitch`
    pub const SESSION_UI_HANDLEPENDINGAUTOMODESWITCH: &str =
        "session.ui.handlePendingAutoModeSwitch";
    /// `session.ui.handlePendingExitPlanMode`
    pub const SESSION_UI_HANDLEPENDINGEXITPLANMODE: &str = "session.ui.handlePendingExitPlanMode";
    /// `session.ui.registerDirectAutoModeSwitchHandler`
    pub const SESSION_UI_REGISTERDIRECTAUTOMODESWITCHHANDLER: &str =
        "session.ui.registerDirectAutoModeSwitchHandler";
    /// `session.ui.unregisterDirectAutoModeSwitchHandler`
    pub const SESSION_UI_UNREGISTERDIRECTAUTOMODESWITCHHANDLER: &str =
        "session.ui.unregisterDirectAutoModeSwitchHandler";
    /// `session.permissions.configure`
    pub const SESSION_PERMISSIONS_CONFIGURE: &str = "session.permissions.configure";
    /// `session.permissions.handlePendingPermissionRequest`
    pub const SESSION_PERMISSIONS_HANDLEPENDINGPERMISSIONREQUEST: &str =
        "session.permissions.handlePendingPermissionRequest";
    /// `session.permissions.pendingRequests`
    pub const SESSION_PERMISSIONS_PENDINGREQUESTS: &str = "session.permissions.pendingRequests";
    /// `session.permissions.setApproveAll`
    pub const SESSION_PERMISSIONS_SETAPPROVEALL: &str = "session.permissions.setApproveAll";
    /// `session.permissions.setAllowAll`
    pub const SESSION_PERMISSIONS_SETALLOWALL: &str = "session.permissions.setAllowAll";
    /// `session.permissions.getAllowAll`
    pub const SESSION_PERMISSIONS_GETALLOWALL: &str = "session.permissions.getAllowAll";
    /// `session.permissions.modifyRules`
    pub const SESSION_PERMISSIONS_MODIFYRULES: &str = "session.permissions.modifyRules";
    /// `session.permissions.setRequired`
    pub const SESSION_PERMISSIONS_SETREQUIRED: &str = "session.permissions.setRequired";
    /// `session.permissions.resetSessionApprovals`
    pub const SESSION_PERMISSIONS_RESETSESSIONAPPROVALS: &str =
        "session.permissions.resetSessionApprovals";
    /// `session.permissions.notifyPromptShown`
    pub const SESSION_PERMISSIONS_NOTIFYPROMPTSHOWN: &str = "session.permissions.notifyPromptShown";
    /// `session.permissions.paths.list`
    pub const SESSION_PERMISSIONS_PATHS_LIST: &str = "session.permissions.paths.list";
    /// `session.permissions.paths.add`
    pub const SESSION_PERMISSIONS_PATHS_ADD: &str = "session.permissions.paths.add";
    /// `session.permissions.paths.updatePrimary`
    pub const SESSION_PERMISSIONS_PATHS_UPDATEPRIMARY: &str =
        "session.permissions.paths.updatePrimary";
    /// `session.permissions.paths.isPathWithinAllowedDirectories`
    pub const SESSION_PERMISSIONS_PATHS_ISPATHWITHINALLOWEDDIRECTORIES: &str =
        "session.permissions.paths.isPathWithinAllowedDirectories";
    /// `session.permissions.paths.isPathWithinWorkspace`
    pub const SESSION_PERMISSIONS_PATHS_ISPATHWITHINWORKSPACE: &str =
        "session.permissions.paths.isPathWithinWorkspace";
    /// `session.permissions.locations.resolve`
    pub const SESSION_PERMISSIONS_LOCATIONS_RESOLVE: &str = "session.permissions.locations.resolve";
    /// `session.permissions.locations.apply`
    pub const SESSION_PERMISSIONS_LOCATIONS_APPLY: &str = "session.permissions.locations.apply";
    /// `session.permissions.locations.addToolApproval`
    pub const SESSION_PERMISSIONS_LOCATIONS_ADDTOOLAPPROVAL: &str =
        "session.permissions.locations.addToolApproval";
    /// `session.permissions.folderTrust.isTrusted`
    pub const SESSION_PERMISSIONS_FOLDERTRUST_ISTRUSTED: &str =
        "session.permissions.folderTrust.isTrusted";
    /// `session.permissions.folderTrust.addTrusted`
    pub const SESSION_PERMISSIONS_FOLDERTRUST_ADDTRUSTED: &str =
        "session.permissions.folderTrust.addTrusted";
    /// `session.permissions.urls.setUnrestrictedMode`
    pub const SESSION_PERMISSIONS_URLS_SETUNRESTRICTEDMODE: &str =
        "session.permissions.urls.setUnrestrictedMode";
    /// `session.log`
    pub const SESSION_LOG: &str = "session.log";
    /// `session.metadata.snapshot`
    pub const SESSION_METADATA_SNAPSHOT: &str = "session.metadata.snapshot";
    /// `session.metadata.isProcessing`
    pub const SESSION_METADATA_ISPROCESSING: &str = "session.metadata.isProcessing";
    /// `session.metadata.activity`
    pub const SESSION_METADATA_ACTIVITY: &str = "session.metadata.activity";
    /// `session.metadata.contextInfo`
    pub const SESSION_METADATA_CONTEXTINFO: &str = "session.metadata.contextInfo";
    /// `session.metadata.recordContextChange`
    pub const SESSION_METADATA_RECORDCONTEXTCHANGE: &str = "session.metadata.recordContextChange";
    /// `session.metadata.setWorkingDirectory`
    pub const SESSION_METADATA_SETWORKINGDIRECTORY: &str = "session.metadata.setWorkingDirectory";
    /// `session.metadata.recomputeContextTokens`
    pub const SESSION_METADATA_RECOMPUTECONTEXTTOKENS: &str =
        "session.metadata.recomputeContextTokens";
    /// `session.shell.exec`
    pub const SESSION_SHELL_EXEC: &str = "session.shell.exec";
    /// `session.shell.kill`
    pub const SESSION_SHELL_KILL: &str = "session.shell.kill";
    /// `session.shell.executeUserRequested`
    pub const SESSION_SHELL_EXECUTEUSERREQUESTED: &str = "session.shell.executeUserRequested";
    /// `session.shell.cancelUserRequested`
    pub const SESSION_SHELL_CANCELUSERREQUESTED: &str = "session.shell.cancelUserRequested";
    /// `session.history.compact`
    pub const SESSION_HISTORY_COMPACT: &str = "session.history.compact";
    /// `session.history.truncate`
    pub const SESSION_HISTORY_TRUNCATE: &str = "session.history.truncate";
    /// `session.history.cancelBackgroundCompaction`
    pub const SESSION_HISTORY_CANCELBACKGROUNDCOMPACTION: &str =
        "session.history.cancelBackgroundCompaction";
    /// `session.history.abortManualCompaction`
    pub const SESSION_HISTORY_ABORTMANUALCOMPACTION: &str = "session.history.abortManualCompaction";
    /// `session.history.summarizeForHandoff`
    pub const SESSION_HISTORY_SUMMARIZEFORHANDOFF: &str = "session.history.summarizeForHandoff";
    /// `session.queue.pendingItems`
    pub const SESSION_QUEUE_PENDINGITEMS: &str = "session.queue.pendingItems";
    /// `session.queue.removeMostRecent`
    pub const SESSION_QUEUE_REMOVEMOSTRECENT: &str = "session.queue.removeMostRecent";
    /// `session.queue.clear`
    pub const SESSION_QUEUE_CLEAR: &str = "session.queue.clear";
    /// `session.eventLog.read`
    pub const SESSION_EVENTLOG_READ: &str = "session.eventLog.read";
    /// `session.eventLog.tail`
    pub const SESSION_EVENTLOG_TAIL: &str = "session.eventLog.tail";
    /// `session.eventLog.registerInterest`
    pub const SESSION_EVENTLOG_REGISTERINTEREST: &str = "session.eventLog.registerInterest";
    /// `session.eventLog.releaseInterest`
    pub const SESSION_EVENTLOG_RELEASEINTEREST: &str = "session.eventLog.releaseInterest";
    /// `session.usage.getMetrics`
    pub const SESSION_USAGE_GETMETRICS: &str = "session.usage.getMetrics";
    /// `session.remote.enable`
    pub const SESSION_REMOTE_ENABLE: &str = "session.remote.enable";
    /// `session.remote.disable`
    pub const SESSION_REMOTE_DISABLE: &str = "session.remote.disable";
    /// `session.remote.notifySteerableChanged`
    pub const SESSION_REMOTE_NOTIFYSTEERABLECHANGED: &str = "session.remote.notifySteerableChanged";
    /// `session.schedule.list`
    pub const SESSION_SCHEDULE_LIST: &str = "session.schedule.list";
    /// `session.schedule.stop`
    pub const SESSION_SCHEDULE_STOP: &str = "session.schedule.stop";
    /// `sessionFs.readFile`
    pub const SESSIONFS_READFILE: &str = "sessionFs.readFile";
    /// `sessionFs.writeFile`
    pub const SESSIONFS_WRITEFILE: &str = "sessionFs.writeFile";
    /// `sessionFs.appendFile`
    pub const SESSIONFS_APPENDFILE: &str = "sessionFs.appendFile";
    /// `sessionFs.exists`
    pub const SESSIONFS_EXISTS: &str = "sessionFs.exists";
    /// `sessionFs.stat`
    pub const SESSIONFS_STAT: &str = "sessionFs.stat";
    /// `sessionFs.mkdir`
    pub const SESSIONFS_MKDIR: &str = "sessionFs.mkdir";
    /// `sessionFs.readdir`
    pub const SESSIONFS_READDIR: &str = "sessionFs.readdir";
    /// `sessionFs.readdirWithTypes`
    pub const SESSIONFS_READDIRWITHTYPES: &str = "sessionFs.readdirWithTypes";
    /// `sessionFs.rm`
    pub const SESSIONFS_RM: &str = "sessionFs.rm";
    /// `sessionFs.rename`
    pub const SESSIONFS_RENAME: &str = "sessionFs.rename";
    /// `sessionFs.sqliteQuery`
    pub const SESSIONFS_SQLITEQUERY: &str = "sessionFs.sqliteQuery";
    /// `sessionFs.sqliteExists`
    pub const SESSIONFS_SQLITEEXISTS: &str = "sessionFs.sqliteExists";
    /// `canvas.open`
    pub const CANVAS_OPEN: &str = "canvas.open";
    /// `canvas.close`
    pub const CANVAS_CLOSE: &str = "canvas.close";
    /// `canvas.action.invoke`
    pub const CANVAS_ACTION_INVOKE: &str = "canvas.action.invoke";
}

/// Parameters for aborting the current turn
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AbortRequest {
    /// Finite reason code describing why the current turn was aborted
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<AbortReason>,
}

/// Result of aborting the current turn
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AbortResult {
    /// Error message if the abort failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether the abort completed successfully
    pub success: bool,
}

/// Optional GitHub token used to look up quota for a specific user instead of the global auth context.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountGetQuotaRequest {
    /// GitHub token for per-user quota lookup. When provided, resolves this token to determine the user's quota instead of using the global auth.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_hub_token: Option<String>,
}

/// Schema for the `AccountQuotaSnapshot` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountQuotaSnapshot {
    /// Number of requests included in the entitlement, or -1 for unlimited entitlements
    pub entitlement_requests: i64,
    /// Whether the user has an unlimited usage entitlement
    pub is_unlimited_entitlement: bool,
    /// Number of additional usage requests made this period
    pub overage: f64,
    /// Whether additional usage is allowed when quota is exhausted
    pub overage_allowed_with_exhausted_quota: bool,
    /// Percentage of entitlement remaining
    pub remaining_percentage: f64,
    /// Date when the quota resets (ISO 8601 string)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reset_date: Option<String>,
    /// Whether usage is still permitted after quota exhaustion
    pub usage_allowed_with_exhausted_quota: bool,
    /// Number of requests used so far this period
    pub used_requests: i64,
}

/// Quota usage snapshots for the resolved user, keyed by quota type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AccountGetQuotaResult {
    /// Quota snapshots keyed by type (e.g., chat, completions, premium_interactions)
    pub quota_snapshots: HashMap<String, AccountQuotaSnapshot>,
}

/// Schema for the `AgentDiscoveryPath` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentDiscoveryPath {
    /// Absolute path of the search/create directory (may not exist on disk yet)
    pub path: String,
    /// Whether this is the canonical directory to create a new agent in its tier. At most one entry per tier is preferred.
    pub preferred_for_creation: bool,
    /// The input project path this directory was derived from (only for project scope)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_path: Option<String>,
    /// Which tier this directory belongs to
    pub scope: AgentDiscoveryPathScope,
}

/// Canonical locations where custom agents can be created so the runtime will recognize them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentDiscoveryPathList {
    /// Canonical agent create/discovery directories, in priority order
    pub paths: Vec<AgentDiscoveryPath>,
}

/// Schema for the `AgentInfo` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentInfo {
    /// Description of the agent's purpose
    pub description: String,
    /// Human-readable display name
    pub display_name: String,
    /// Stable identifier for selection. For most agents this is the same as `name`; for plugin/builtin agents it may differ. Always populated; defaults to `name` when no distinct id was assigned.
    pub id: String,
    /// MCP server configurations attached to this agent, keyed by server name. Server config shape mirrors the MCP `mcpServers` schema.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_servers: Option<HashMap<String, serde_json::Value>>,
    /// Preferred model id for this agent. When omitted, inherits the outer agent's model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Unique identifier of the custom agent
    pub name: String,
    /// Absolute local file path of the agent definition. Only set for file-based agents loaded from disk; remote agents do not have a path.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Skill names preloaded into this agent's context. Omitted means none.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skills: Option<Vec<String>>,
    /// Where the agent definition was loaded from
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<AgentInfoSource>,
    /// Allowed tool names for this agent. Empty array means none; omitted means inherit defaults.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
    /// Whether the agent can be selected directly by the user. Agents marked `false` are subagent-only.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_invocable: Option<bool>,
}

/// The currently selected custom agent, or null when using the default agent.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentGetCurrentResult {
    /// Currently selected custom agent, or null if using the default agent
    pub agent: AgentInfo,
}

/// Custom agents available to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentList {
    /// Available custom agents
    pub agents: Vec<AgentInfo>,
}

/// Full registry entry for the spawned child. Lets the controller call `handleLiveTargetSelected(entry)` directly without re-reading the registry (avoids a TOCTOU window).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistryLiveTargetEntry {
    /// Kind of attention required when status === "attention". Meaningful only when status === "attention".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attention_kind: Option<AgentRegistryLiveTargetEntryAttentionKind>,
    /// Git branch of the session (when known)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Copilot CLI version that wrote the entry
    pub copilot_version: String,
    /// Working directory of the session (when known)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Bind host for the entry's JSON-RPC server
    pub host: String,
    /// Process kind tag for the registry entry
    pub kind: AgentRegistryLiveTargetEntryKind,
    /// Wall-clock milliseconds since the watcher last observed this entry (heartbeat freshness)
    pub last_seen_ms: i64,
    /// How the most recent turn ended (clean vs aborted). Lets the renderer distinguish done from done_cancelled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_terminal_event: Option<AgentRegistryLiveTargetEntryLastTerminalEvent>,
    /// Model identifier currently selected for the session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Operating-system pid of the process owning this entry
    pub pid: i64,
    /// TCP port the entry's JSON-RPC server is listening on
    pub port: i64,
    /// Registry entry schema version (1 = ui-server, 2 = managed-server)
    pub schema_version: i64,
    /// Session ID of the foreground session for this entry
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// Friendly session name (when set)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_name: Option<String>,
    /// ISO 8601 timestamp captured at registration
    pub started_at: String,
    /// Coarse lifecycle status of the foreground session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<AgentRegistryLiveTargetEntryStatus>,
    /// Monotonic per-publisher revision counter incremented on every status update. Lets watchers detect transient flips.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_revision: Option<i64>,
    /// Connection token (null when the target is unauthenticated)
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) token: Option<String>,
}

/// Per-spawn log-capture outcome; populated from spawnLiveTarget.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistryLogCapture {
    /// Whether per-spawn log capture is on (false when env-disabled or open failed)
    pub enabled: bool,
    /// Human-readable open failure message (only set when enabled === false AND the env-disable opt-out was NOT used)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub open_error: Option<String>,
    /// Categorized reason for log-open failure
    #[serde(skip_serializing_if = "Option::is_none")]
    pub open_error_reason: Option<AgentRegistryLogCaptureOpenErrorReason>,
    /// Absolute path to the per-spawn log file (only set when enabled)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
}

/// `child_process.spawn` itself failed before the child entered the registry.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistrySpawnError {
    /// Underlying errno code (e.g. ENOENT, EACCES) when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code: Option<String>,
    /// Discriminator: child_process.spawn itself failed
    pub kind: AgentRegistrySpawnErrorKind,
    /// Human-readable error message
    pub message: String,
}

/// Spawn succeeded but the child did not publish a matching managed-server entry within the timeout.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistrySpawnRegistryTimeout {
    /// Process ID of the orphaned child (so the caller can offer 'kill the pid' guidance)
    pub child_pid: i64,
    /// Discriminator: spawn succeeded but child never registered
    pub kind: AgentRegistrySpawnRegistryTimeoutKind,
    /// Per-spawn log-capture outcome; populated from spawnLiveTarget.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub log_capture: Option<AgentRegistryLogCapture>,
}

/// Inputs to spawn a managed-server child via the controller's spawn delegate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistrySpawnRequest {
    /// Custom or built-in agent name (e.g. 'explore'). When omitted, the child uses its own default.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_name: Option<String>,
    /// Working directory for the spawned child (must be an existing directory)
    pub cwd: String,
    /// Optional first user message. Forwarded to the caller (the CLI's spawn wrapper sends it post-attach via the standard LocalRpcSession.send path).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initial_prompt: Option<String>,
    /// Model identifier to apply to the new session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Friendly session name. Must satisfy validateSessionName: non-empty, no leading/trailing whitespace, <=100 chars, no control chars, no double quotes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Permission posture for the new session. 'yolo' requires the controller-local session to currently be in allow-all mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub permission_mode: Option<AgentRegistrySpawnPermissionMode>,
}

/// Managed-server child was spawned and registered successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistrySpawnSpawned {
    /// Full registry entry for the spawned child. Lets the controller call `handleLiveTargetSelected(entry)` directly without re-reading the registry (avoids a TOCTOU window).
    pub entry: AgentRegistryLiveTargetEntry,
    /// If the delegate attempted to send the initial prompt and failed, the categorized error message.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initial_prompt_error: Option<String>,
    /// Whether the delegate already sent the initial prompt. Always omitted in the current wiring: the controller sends the prompt post-attach via the standard LocalRpcSession.send path.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initial_prompt_sent: Option<bool>,
    /// Discriminator: managed-server child spawned successfully
    pub kind: AgentRegistrySpawnSpawnedKind,
    /// Per-spawn log-capture outcome; populated from spawnLiveTarget.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub log_capture: Option<AgentRegistryLogCapture>,
}

/// Synchronous pre-validation rejected the spawn request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentRegistrySpawnValidationError {
    /// Which parameter field was invalid. Omitted when the rejection is not field-specific.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub field: Option<AgentRegistrySpawnValidationErrorField>,
    /// Discriminator: synchronous pre-validation rejected the request
    pub kind: AgentRegistrySpawnValidationErrorKind,
    /// Human-readable explanation; safe to surface in the UI banner. Never logged to unrestricted telemetry.
    pub message: String,
    /// Categorized reason for the rejection. Low-cardinality enum so telemetry can aggregate by reason without leaking raw paths or agent/model names.
    pub reason: AgentRegistrySpawnValidationErrorReason,
}

/// Custom agents available to the session after reloading definitions from disk.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentReloadResult {
    /// Reloaded custom agents
    pub agents: Vec<AgentInfo>,
}

/// Optional project paths to include in agent discovery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentsDiscoverRequest {
    /// When true, omit the host's agents (the user-level agent directory and all plugin agents), leaving only project and remote agents. For multitenant deployments.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_host_agents: Option<bool>,
    /// Optional list of project directory paths to scan for project-scoped agents. When omitted or empty, only user/plugin/remote-independent agents are returned (no project scan).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
}

/// Name of the custom agent to select for subsequent turns.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentSelectRequest {
    /// Name of the custom agent to select
    pub name: String,
}

/// The newly selected custom agent.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentSelectResult {
    /// The newly selected custom agent
    pub agent: AgentInfo,
}

/// Optional project paths to include when enumerating agent discovery directories.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentsGetDiscoveryPathsRequest {
    /// When true, omit the host's user-level agent directory, leaving only project directories. For multitenant deployments (mirrors `discover`'s `excludeHostAgents`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_host_agents: Option<bool>,
    /// Optional list of project directory paths. When omitted or empty, only the user-level directory is returned.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
}

/// Indicates whether the operation succeeded and reports the post-mutation state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AllowAllPermissionSetResult {
    /// Authoritative allow-all state after the mutation
    pub enabled: bool,
    /// Whether the operation succeeded
    pub success: bool,
}

/// Current full allow-all permission state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AllowAllPermissionState {
    /// Whether full allow-all permissions are currently active
    pub enabled: bool,
}

/// Schema for the `CopilotUserResponseEndpoints` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotUserResponseEndpoints {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api: Option<String>,
    #[serde(rename = "origin-tracker", skip_serializing_if = "Option::is_none")]
    pub origin_tracker: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub proxy: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub telemetry: Option<String>,
}

/// Schema for the `CopilotUserResponseQuotaSnapshotsChat` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotUserResponseQuotaSnapshotsChat {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entitlement: Option<f64>,
    #[serde(rename = "has_quota", skip_serializing_if = "Option::is_none")]
    pub has_quota: Option<bool>,
    #[serde(rename = "overage_count", skip_serializing_if = "Option::is_none")]
    pub overage_count: Option<f64>,
    #[serde(rename = "overage_permitted", skip_serializing_if = "Option::is_none")]
    pub overage_permitted: Option<bool>,
    #[serde(rename = "percent_remaining", skip_serializing_if = "Option::is_none")]
    pub percent_remaining: Option<f64>,
    #[serde(rename = "quota_id", skip_serializing_if = "Option::is_none")]
    pub quota_id: Option<String>,
    #[serde(rename = "quota_remaining", skip_serializing_if = "Option::is_none")]
    pub quota_remaining: Option<f64>,
    #[serde(rename = "quota_reset_at", skip_serializing_if = "Option::is_none")]
    pub quota_reset_at: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remaining: Option<f64>,
    #[serde(rename = "timestamp_utc", skip_serializing_if = "Option::is_none")]
    pub timestamp_utc: Option<String>,
    #[serde(
        rename = "token_based_billing",
        skip_serializing_if = "Option::is_none"
    )]
    pub token_based_billing: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unlimited: Option<bool>,
}

/// Schema for the `CopilotUserResponseQuotaSnapshotsCompletions` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotUserResponseQuotaSnapshotsCompletions {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entitlement: Option<f64>,
    #[serde(rename = "has_quota", skip_serializing_if = "Option::is_none")]
    pub has_quota: Option<bool>,
    #[serde(rename = "overage_count", skip_serializing_if = "Option::is_none")]
    pub overage_count: Option<f64>,
    #[serde(rename = "overage_permitted", skip_serializing_if = "Option::is_none")]
    pub overage_permitted: Option<bool>,
    #[serde(rename = "percent_remaining", skip_serializing_if = "Option::is_none")]
    pub percent_remaining: Option<f64>,
    #[serde(rename = "quota_id", skip_serializing_if = "Option::is_none")]
    pub quota_id: Option<String>,
    #[serde(rename = "quota_remaining", skip_serializing_if = "Option::is_none")]
    pub quota_remaining: Option<f64>,
    #[serde(rename = "quota_reset_at", skip_serializing_if = "Option::is_none")]
    pub quota_reset_at: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remaining: Option<f64>,
    #[serde(rename = "timestamp_utc", skip_serializing_if = "Option::is_none")]
    pub timestamp_utc: Option<String>,
    #[serde(
        rename = "token_based_billing",
        skip_serializing_if = "Option::is_none"
    )]
    pub token_based_billing: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unlimited: Option<bool>,
}

/// Schema for the `CopilotUserResponseQuotaSnapshotsPremiumInteractions` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotUserResponseQuotaSnapshotsPremiumInteractions {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entitlement: Option<f64>,
    #[serde(rename = "has_quota", skip_serializing_if = "Option::is_none")]
    pub has_quota: Option<bool>,
    #[serde(rename = "overage_count", skip_serializing_if = "Option::is_none")]
    pub overage_count: Option<f64>,
    #[serde(rename = "overage_permitted", skip_serializing_if = "Option::is_none")]
    pub overage_permitted: Option<bool>,
    #[serde(rename = "percent_remaining", skip_serializing_if = "Option::is_none")]
    pub percent_remaining: Option<f64>,
    #[serde(rename = "quota_id", skip_serializing_if = "Option::is_none")]
    pub quota_id: Option<String>,
    #[serde(rename = "quota_remaining", skip_serializing_if = "Option::is_none")]
    pub quota_remaining: Option<f64>,
    #[serde(rename = "quota_reset_at", skip_serializing_if = "Option::is_none")]
    pub quota_reset_at: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remaining: Option<f64>,
    #[serde(rename = "timestamp_utc", skip_serializing_if = "Option::is_none")]
    pub timestamp_utc: Option<String>,
    #[serde(
        rename = "token_based_billing",
        skip_serializing_if = "Option::is_none"
    )]
    pub token_based_billing: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unlimited: Option<bool>,
}

/// Schema for the `CopilotUserResponseQuotaSnapshots` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotUserResponseQuotaSnapshots {
    /// Schema for the `CopilotUserResponseQuotaSnapshotsChat` type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub chat: Option<CopilotUserResponseQuotaSnapshotsChat>,
    /// Schema for the `CopilotUserResponseQuotaSnapshotsCompletions` type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completions: Option<CopilotUserResponseQuotaSnapshotsCompletions>,
    /// Schema for the `CopilotUserResponseQuotaSnapshotsPremiumInteractions` type.
    #[serde(
        rename = "premium_interactions",
        skip_serializing_if = "Option::is_none"
    )]
    pub premium_interactions: Option<CopilotUserResponseQuotaSnapshotsPremiumInteractions>,
}

/// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotUserResponse {
    #[serde(rename = "access_type_sku", skip_serializing_if = "Option::is_none")]
    pub access_type_sku: Option<String>,
    #[serde(
        rename = "analytics_tracking_id",
        skip_serializing_if = "Option::is_none"
    )]
    pub analytics_tracking_id: Option<String>,
    #[serde(rename = "assigned_date", skip_serializing_if = "Option::is_none")]
    pub assigned_date: Option<serde_json::Value>,
    #[serde(
        rename = "can_signup_for_limited",
        skip_serializing_if = "Option::is_none"
    )]
    pub can_signup_for_limited: Option<bool>,
    #[serde(rename = "can_upgrade_plan", skip_serializing_if = "Option::is_none")]
    pub can_upgrade_plan: Option<bool>,
    #[serde(rename = "chat_enabled", skip_serializing_if = "Option::is_none")]
    pub chat_enabled: Option<bool>,
    #[serde(
        rename = "cli_remote_control_enabled",
        skip_serializing_if = "Option::is_none"
    )]
    pub cli_remote_control_enabled: Option<bool>,
    #[serde(
        rename = "cloud_session_storage_enabled",
        skip_serializing_if = "Option::is_none"
    )]
    pub cloud_session_storage_enabled: Option<bool>,
    #[serde(
        rename = "codex_agent_enabled",
        skip_serializing_if = "Option::is_none"
    )]
    pub codex_agent_enabled: Option<bool>,
    #[serde(rename = "copilot_plan", skip_serializing_if = "Option::is_none")]
    pub copilot_plan: Option<String>,
    #[serde(
        rename = "copilotignore_enabled",
        skip_serializing_if = "Option::is_none"
    )]
    pub copilotignore_enabled: Option<bool>,
    /// Schema for the `CopilotUserResponseEndpoints` type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub endpoints: Option<CopilotUserResponseEndpoints>,
    #[serde(rename = "is_mcp_enabled", skip_serializing_if = "Option::is_none")]
    pub is_mcp_enabled: Option<serde_json::Value>,
    #[serde(rename = "is_staff", skip_serializing_if = "Option::is_none")]
    pub is_staff: Option<bool>,
    #[serde(
        rename = "limited_user_quotas",
        skip_serializing_if = "Option::is_none"
    )]
    pub limited_user_quotas: Option<HashMap<String, f64>>,
    #[serde(
        rename = "limited_user_reset_date",
        skip_serializing_if = "Option::is_none"
    )]
    pub limited_user_reset_date: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    #[serde(rename = "monthly_quotas", skip_serializing_if = "Option::is_none")]
    pub monthly_quotas: Option<HashMap<String, f64>>,
    #[serde(rename = "organization_list", skip_serializing_if = "Option::is_none")]
    pub organization_list: Option<serde_json::Value>,
    #[serde(
        rename = "organization_login_list",
        skip_serializing_if = "Option::is_none"
    )]
    pub organization_login_list: Option<Vec<String>>,
    #[serde(rename = "quota_reset_date", skip_serializing_if = "Option::is_none")]
    pub quota_reset_date: Option<String>,
    #[serde(
        rename = "quota_reset_date_utc",
        skip_serializing_if = "Option::is_none"
    )]
    pub quota_reset_date_utc: Option<String>,
    /// Schema for the `CopilotUserResponseQuotaSnapshots` type.
    #[serde(rename = "quota_snapshots", skip_serializing_if = "Option::is_none")]
    pub quota_snapshots: Option<CopilotUserResponseQuotaSnapshots>,
    #[serde(
        rename = "restricted_telemetry",
        skip_serializing_if = "Option::is_none"
    )]
    pub restricted_telemetry: Option<bool>,
    #[serde(
        rename = "token_based_billing",
        skip_serializing_if = "Option::is_none"
    )]
    pub token_based_billing: Option<bool>,
}

/// Schema for the `ApiKeyAuthInfo` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ApiKeyAuthInfo {
    /// The API key. Treat as a secret.
    pub api_key: String,
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Authentication host.
    pub host: String,
    /// API-key authentication for non-GitHub LLM providers (e.g. when running BYOM-style).
    pub r#type: ApiKeyAuthInfoType,
}

/// Blob attachment with inline base64-encoded data
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentBlob {
    /// Internal: content-addressed id of the session.binary_asset event holding this attachment's model-facing bytes (e.g. "sha256:..."). Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub asset_id: Option<String>,
    /// Internal: decoded byte length of the attachment's model-facing bytes. Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub byte_length: Option<i64>,
    /// Base64-encoded content. Present on input and for external consumers; replaced by an internal `assetId` reference in persisted events when interned to a content-addressed asset.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub data: Option<String>,
    /// User-facing display name for the attachment
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// MIME type of the inline data
    pub mime_type: String,
    /// Internal: why model-facing bytes are absent from persistence. Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub omitted_reason: Option<OmittedBinaryOmittedReason>,
    /// Attachment type discriminator
    pub r#type: AttachmentBlobType,
}

/// Directory attachment
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentDirectory {
    /// User-facing display name for the attachment
    pub display_name: String,
    /// Absolute directory path
    pub path: String,
    /// Attachment type discriminator
    pub r#type: AttachmentDirectoryType,
}

/// Structured context contributed by an extension. Composer pills displayed in the host are forwarded back through session.send.attachments, then rendered into the model prompt as an <extension_context> XML block.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentExtensionContext {
    /// Provider-local canvas identifier when the push was bound to a canvas instance
    #[serde(skip_serializing_if = "Option::is_none")]
    pub canvas_id: Option<String>,
    /// ISO 8601 timestamp captured by the runtime when the push was accepted
    pub captured_at: String,
    /// Owning extension identifier. Runtime-derived from the caller's connection when produced via session.extensions.sendAttachmentsToMessage; preserved verbatim on subsequent transports.
    pub extension_id: String,
    /// Open canvas instance identifier when the push was bound to a canvas instance
    #[serde(skip_serializing_if = "Option::is_none")]
    pub instance_id: Option<String>,
    /// Caller-supplied JSON payload
    #[serde(skip_serializing_if = "Option::is_none")]
    pub payload: Option<serde_json::Value>,
    /// Human-readable composer pill label
    pub title: String,
    /// Attachment type discriminator
    pub r#type: AttachmentExtensionContextType,
}

/// Optional line range to scope the attachment to a specific section of the file
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentFileLineRange {
    /// End line number (1-based, inclusive)
    pub end: i64,
    /// Start line number (1-based)
    pub start: i64,
}

/// File attachment
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentFile {
    /// Internal: content-addressed id of the session.binary_asset event holding this attachment's model-facing bytes (e.g. "sha256:..."). Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub asset_id: Option<String>,
    /// Internal: decoded byte length of the attachment's model-facing bytes. Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub byte_length: Option<i64>,
    /// User-facing display name for the attachment
    pub display_name: String,
    /// Optional line range to scope the attachment to a specific section of the file
    #[serde(skip_serializing_if = "Option::is_none")]
    pub line_range: Option<AttachmentFileLineRange>,
    /// Internal: MIME type of the file's model-facing bytes (post-resize for images). Set when the file's bytes are interned to an asset. Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Internal: why model-facing bytes are absent from persistence. Absent externally.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub omitted_reason: Option<OmittedBinaryOmittedReason>,
    /// Absolute file path
    pub path: String,
    /// Attachment type discriminator
    pub r#type: AttachmentFileType,
}

/// GitHub issue, pull request, or discussion reference
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentGitHubReference {
    /// Issue, pull request, or discussion number
    pub number: i64,
    /// Type of GitHub reference
    pub reference_type: AttachmentGitHubReferenceType,
    /// Current state of the referenced item (e.g., open, closed, merged)
    pub state: String,
    /// Title of the referenced item
    pub title: String,
    /// Attachment type discriminator
    pub r#type: AttachmentGitHubReferenceType,
    /// URL to the referenced item on GitHub
    pub url: String,
}

/// End position of the selection
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentSelectionDetailsEnd {
    /// End character offset within the line (0-based)
    pub character: i64,
    /// End line number (0-based)
    pub line: i64,
}

/// Start position of the selection
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentSelectionDetailsStart {
    /// Start character offset within the line (0-based)
    pub character: i64,
    /// Start line number (0-based)
    pub line: i64,
}

/// Position range of the selection within the file
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentSelectionDetails {
    /// End position of the selection
    pub end: AttachmentSelectionDetailsEnd,
    /// Start position of the selection
    pub start: AttachmentSelectionDetailsStart,
}

/// Code selection attachment from an editor
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttachmentSelection {
    /// User-facing display name for the selection
    pub display_name: String,
    /// Absolute path to the file containing the selection
    pub file_path: String,
    /// Position range of the selection within the file
    pub selection: AttachmentSelectionDetails,
    /// The selected text content
    pub text: String,
    /// Attachment type discriminator
    pub r#type: AttachmentSelectionType,
}

/// Cancellation result for a user-requested shell command.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CancelUserRequestedShellCommandResult {
    /// Whether an in-flight execution was found and signalled to cancel
    pub cancelled: bool,
}

/// Canvas action that the agent or host can invoke. To discover the input schema for a particular action, call the list_canvas_capabilities tool.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasAction {
    /// Description of the action
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// JSON Schema for the action input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_schema: Option<serde_json::Value>,
    /// Action name exposed by the canvas provider
    pub name: String,
}

/// Canvas action invocation parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasActionInvokeRequest {
    /// Action name to invoke
    pub action_name: String,
    /// Action input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
    /// Open canvas instance identifier
    pub instance_id: String,
}

/// Canvas action invocation result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasActionInvokeResult {
    /// Provider-supplied action result
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
}

/// Canvas close parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasCloseRequest {
    /// Open canvas instance identifier
    pub instance_id: String,
}

/// Host capabilities
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasHostContextCapabilities {
    /// Whether canvas rendering is supported
    #[serde(skip_serializing_if = "Option::is_none")]
    pub canvases: Option<bool>,
}

/// Host context supplied by the runtime.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasHostContext {
    /// Host capabilities
    #[serde(skip_serializing_if = "Option::is_none")]
    pub capabilities: Option<CanvasHostContextCapabilities>,
}

/// Canvas available in the current session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredCanvas {
    /// Actions the agent or host may invoke on an open instance
    #[serde(skip_serializing_if = "Option::is_none")]
    pub actions: Option<Vec<CanvasAction>>,
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Short, single-sentence description shown to the agent in canvas catalogs.
    pub description: String,
    /// Human-readable canvas name
    pub display_name: String,
    /// Owning provider identifier
    pub extension_id: String,
    /// Owning extension display name, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_name: Option<String>,
    /// JSON Schema for canvas open input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_schema: Option<serde_json::Value>,
}

/// Declared canvases available in this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasList {
    /// Declared canvases available in this session
    pub canvases: Vec<DiscoveredCanvas>,
}

/// Open canvas instance snapshot.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OpenCanvasInstance {
    /// Runtime-controlled routing state for an open canvas instance.
    pub availability: CanvasInstanceAvailability,
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Owning provider identifier
    pub extension_id: String,
    /// Owning extension display name, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_name: Option<String>,
    /// Input supplied when the instance was opened
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
    /// Stable caller-supplied canvas instance identifier
    pub instance_id: String,
    /// Whether this snapshot came from an idempotent reopen
    pub reopen: bool,
    /// Provider-supplied status text
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// Rendered title
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// URL for web-rendered canvases
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Live open-canvas snapshot.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasListOpenResult {
    /// Currently open canvas instances
    pub open_canvases: Vec<OpenCanvasInstance>,
}

/// Canvas open parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasOpenRequest {
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Owning provider identifier. Optional when the canvasId is unique across providers; required to disambiguate when multiple providers register the same canvasId.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_id: Option<String>,
    /// Canvas open input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
    /// Caller-supplied stable instance identifier
    pub instance_id: String,
}

/// Session context supplied by the runtime.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasSessionContext {
    /// Active session working directory, when known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// Canvas close parameters sent to the provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasProviderCloseRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Owning provider identifier
    pub extension_id: String,
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Canvas instance identifier
    pub instance_id: String,
    /// Host context supplied by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<CanvasHostContext>,
    /// Session context supplied by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session: Option<CanvasSessionContext>,
}

/// Canvas action invocation parameters sent to the provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasProviderInvokeActionRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Owning provider identifier
    pub extension_id: String,
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Canvas instance identifier
    pub instance_id: String,
    /// Action name to invoke
    pub action_name: String,
    /// Action input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
    /// Host context supplied by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<CanvasHostContext>,
    /// Session context supplied by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session: Option<CanvasSessionContext>,
}

/// Canvas open parameters sent to the provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasProviderOpenRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Owning provider identifier
    pub extension_id: String,
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Stable caller-supplied canvas instance identifier
    pub instance_id: String,
    /// Canvas open input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
    /// Host context supplied by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<CanvasHostContext>,
    /// Session context supplied by the runtime.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session: Option<CanvasSessionContext>,
}

/// Canvas open result returned by the provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasProviderOpenResult {
    /// Provider-supplied status text
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// Provider-supplied title
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// URL for web-rendered canvases
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Optional unstructured input hint
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandInput {
    /// Optional completion hint for the input (e.g. 'directory' for filesystem path completion)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completion: Option<SlashCommandInputCompletion>,
    /// Hint to display when command input has not been provided
    pub hint: String,
    /// When true, clients should pass the full text after the command name as a single argument rather than splitting on whitespace
    #[serde(skip_serializing_if = "Option::is_none")]
    pub preserve_multiline_input: Option<bool>,
    /// When true, the command requires non-empty input; clients should render the input hint as required
    #[serde(skip_serializing_if = "Option::is_none")]
    pub required: Option<bool>,
}

/// Schema for the `SlashCommandInfo` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandInfo {
    /// Canonical aliases without leading slashes
    #[serde(skip_serializing_if = "Option::is_none")]
    pub aliases: Option<Vec<String>>,
    /// Whether the command may run while an agent turn is active
    pub allow_during_agent_execution: bool,
    /// Human-readable command description
    pub description: String,
    /// Whether the command is experimental
    #[serde(skip_serializing_if = "Option::is_none")]
    pub experimental: Option<bool>,
    /// Optional unstructured input hint
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<SlashCommandInput>,
    /// Coarse command category for grouping and behavior: runtime built-in, skill-backed command, or SDK/client-owned command
    pub kind: SlashCommandKind,
    /// Canonical command name without a leading slash
    pub name: String,
    /// Whether the command may be the target of `/every` / `/after` schedules. Resolution happens at every tick, so only set this when the command is safe to re-invoke and produces an agent prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub schedulable: Option<bool>,
}

/// Slash commands available in the session, after applying any include/exclude filters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandList {
    /// Commands available in this session
    pub commands: Vec<SlashCommandInfo>,
}

/// Pending command request ID and an optional error if the client handler failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsHandlePendingCommandRequest {
    /// Error message if the command handler failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Request ID from the command invocation event
    pub request_id: RequestId,
}

/// Indicates whether the pending client-handled command was completed successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsHandlePendingCommandResult {
    /// Whether the command was handled successfully
    pub success: bool,
}

/// Slash command name and optional raw input string to invoke.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsInvokeRequest {
    /// Raw input after the command name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<String>,
    /// Command name. Leading slashes are stripped and the name is matched case-insensitively.
    pub name: String,
}

/// Optional filters controlling which command sources to include in the listing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsListRequest {
    /// Include runtime built-in commands
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_builtins: Option<bool>,
    /// Include commands registered by protocol clients, including SDK clients and extensions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_client_commands: Option<bool>,
    /// Include enabled user-invocable skills and commands
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_skills: Option<bool>,
}

/// Queued-command request ID and the result indicating whether the host executed it (and whether to stop processing further queued commands).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsRespondToQueuedCommandRequest {
    /// Request ID from the `command.queued` event the host is responding to.
    pub request_id: RequestId,
    /// Result of the queued command execution.
    pub result: serde_json::Value,
}

/// Indicates whether the queued-command response was matched to a pending request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CommandsRespondToQueuedCommandResult {
    /// Whether a pending queued command with the given request ID was found and resolved. False when the request was already resolved, cancelled, or unknown.
    pub success: bool,
}

/// Params to attach or detach an in-process ExtensionController delegate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ConfigureSessionExtensionsParams {
    /// In-process ExtensionController delegate (CLI-only optimization). Marked internal: this field is excluded from the public SDK surface. The post-SDK extension surface exposes list/enable/disable/reload via dedicated RPCs served by the runtime.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) controller: Option<serde_json::Value>,
    /// Session to attach the extension controller delegate to.
    pub session_id: SessionId,
}

/// Repository associated with the connected remote session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectedRemoteSessionMetadataRepository {
    /// Branch associated with the remote session.
    pub branch: String,
    /// Repository name.
    pub name: String,
    /// Repository owner or organization login.
    pub owner: String,
}

/// Metadata for a connected remote session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectedRemoteSessionMetadata {
    /// Neutral SDK discriminator for the connected remote session kind.
    pub kind: ConnectedRemoteSessionMetadataKind,
    /// Last session update time as an ISO 8601 string.
    pub modified_time: String,
    /// Optional friendly session name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Pull request number associated with the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pull_request_number: Option<i64>,
    /// Repository associated with the connected remote session.
    pub repository: ConnectedRemoteSessionMetadataRepository,
    /// Original remote resource identifier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resource_id: Option<String>,
    /// SDK session ID for the connected remote session.
    pub session_id: SessionId,
    /// Remote session staleness deadline as an ISO 8601 string.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stale_at: Option<String>,
    /// Session start time as an ISO 8601 string.
    pub start_time: String,
    /// Remote session state returned by the backing service.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub state: Option<String>,
    /// Optional session summary.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
}

/// Remote session connection parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConnectRemoteSessionParams {
    /// Session ID to connect to.
    pub session_id: SessionId,
}

/// Optional connection token presented by the SDK client during the handshake.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ConnectRequest {
    /// Connection token; required when the server was started with COPILOT_CONNECTION_TOKEN
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token: Option<String>,
}

/// Handshake result reporting the server's protocol version and package version on success.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct ConnectResult {
    /// Always true on success
    pub ok: bool,
    /// Server protocol version number
    pub protocol_version: i64,
    /// Server package version
    pub version: String,
}

/// Schema for the `CopilotApiTokenAuthInfo` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CopilotApiTokenAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Authentication host (always the public GitHub host).
    pub host: CopilotApiTokenAuthInfoHost,
    /// Direct Copilot API authentication via the `GITHUB_COPILOT_API_TOKEN` + `COPILOT_API_URL` environment-variable pair. The token itself is read from the environment by the runtime, not carried in this struct.
    pub r#type: CopilotApiTokenAuthInfoType,
}

/// The currently selected model, reasoning effort, and context tier for the session. The context tier reflects `Session.getContextTier()`, restored from the session journal on resume.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CurrentModel {
    /// Context tier for models that support multiple context-window sizes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<ContextTier>,
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
    /// Reasoning effort level currently applied to the active model, when one is set. Reads `Session.getReasoningEffort()` synchronously after `getSelectedModel()` resolves so the two values are reported as a snapshot.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
}

/// Lightweight metadata for a currently initialized session tool
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CurrentToolMetadata {
    /// Whether the tool is loaded on demand via tool search
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_loading: Option<bool>,
    /// Tool description
    pub description: String,
    /// JSON Schema for tool input
    #[serde(rename = "input_schema", skip_serializing_if = "Option::is_none")]
    pub input_schema: Option<HashMap<String, serde_json::Value>>,
    /// MCP server name for MCP-backed tools
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_server_name: Option<String>,
    /// Raw MCP tool name for MCP-backed tools
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mcp_tool_name: Option<String>,
    /// Model-facing tool name
    pub name: String,
    /// Optional MCP/config namespaced tool name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub namespaced_name: Option<String>,
}

/// Schema for the `DiscoveredMcpServer` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct DiscoveredMcpServer {
    /// Whether the server is enabled (not in the disabled list)
    pub enabled: bool,
    /// Server name (config key)
    pub name: String,
    /// Configuration source: user, workspace, plugin, or builtin
    pub source: McpServerSource,
    /// Plugin name that provided this server, when source is plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin: Option<String>,
    /// Plugin version that provided this server, when source is plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin_version: Option<String>,
    /// Server transport type: stdio, http, sse (deprecated), or memory
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<DiscoveredMcpServerType>,
}

/// Slash-prefixed command string to enqueue for FIFO processing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EnqueueCommandParams {
    /// Slash-prefixed command string to enqueue, e.g. '/compact' or '/model gpt-4'. Queued FIFO with any in-flight items; if the session is idle, processing kicks off immediately.
    pub command: String,
}

/// Indicates whether the command was accepted into the local execution queue.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EnqueueCommandResult {
    /// True when the command was accepted into the local execution queue. False when the call targets a session that does not support local command queueing (e.g. remote sessions).
    pub queued: bool,
}

/// Schema for the `EnvAuthInfo` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EnvAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Name of the environment variable the token was sourced from.
    pub env_var: String,
    /// Authentication host (e.g. https://github.com or a GHES host).
    pub host: String,
    /// User login associated with the token. Undefined for server-to-server tokens (those starting with `ghs_`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// The token value itself. Treat as a secret.
    pub token: String,
    /// Personal access token (PAT) or server-to-server token sourced from an environment variable.
    pub r#type: EnvAuthInfoType,
}

/// Cursor, batch size, and optional long-poll/filter parameters for reading session events.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EventLogReadRequest {
    /// Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_scope: Option<EventsAgentScope>,
    /// Opaque cursor returned by a previous read. Omit on the first call to start from the beginning of the session's persisted history.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cursor: Option<String>,
    /// Maximum number of events to return in this batch (1–1000, default 200).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max: Option<i64>,
    /// Either '*' to receive all event types, or a non-empty list of event types to receive
    #[serde(skip_serializing_if = "Option::is_none")]
    pub types: Option<serde_json::Value>,
    /// Milliseconds to wait for new events when the cursor is at the tail of history. 0 (default) returns immediately even if no events are available. Capped at 30000ms. Ephemeral events that arrive during the wait are delivered in this batch but are NOT replayable on a subsequent read (use a non-zero waitMs in your next call to capture future ephemerals as they happen).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wait_ms: Option<i32>,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EventLogReleaseInterestResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Snapshot of the current tail cursor without returning any events. Use this when a consumer wants to subscribe to live events going forward without first paginating through the entire persisted history (which would happen if `read` were called without a cursor on a long-lived session).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EventLogTailResult {
    /// Opaque cursor pointing at the current tail of the session's persisted-events history. Pass back to `read` to receive only events that arrive AFTER this snapshot. When the session has no events, this returns the same sentinel as an unset cursor (i.e. equivalent to omitting the cursor on a first read).
    pub cursor: String,
}

/// Batch of session events returned by a read, with cursor and continuation metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct EventsReadResult {
    /// Opaque cursor for the next read. Pass back unchanged in the next read.cursor to continue from where this read left off. Always present, even when no events were returned.
    pub cursor: String,
    /// Cursor status: 'ok' means the cursor was applied successfully; 'expired' means the cursor referred to an event that no longer exists in history (e.g. truncated or compacted away) and the read started from the beginning of the remaining history.
    pub cursor_status: EventsCursorStatus,
    /// Events are delivered in two batches per read: persisted events first (in append order), then ephemeral events (in seq order). When `waitMs > 0` and the catch-up batches were empty, post-wait events follow the same two-batch ordering. Persisted and ephemeral events do not interleave within a single read.
    pub events: Vec<SessionEvent>,
    /// True when the read returned `max` events and more events are available immediately. When false, the next read with a non-zero `waitMs` will block until a new event arrives or the wait expires.
    pub has_more: bool,
}

/// Slash command name and argument string to execute synchronously.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExecuteCommandParams {
    /// Argument string to pass to the command (empty string if none).
    pub args: String,
    /// Name of the slash command to invoke (without the leading '/').
    pub command_name: String,
}

/// Error message produced while executing the command, if any.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExecuteCommandResult {
    /// Error message produced while executing the command, if any. Omitted when the handler succeeded.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

/// Schema for the `Extension` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Extension {
    /// Source-qualified ID (e.g., 'project:my-ext', 'user:auth-helper', 'plugin:my-plugin:my-ext')
    pub id: String,
    /// Extension name (directory name)
    pub name: String,
    /// Process ID if the extension is running
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pid: Option<i64>,
    /// Discovery source: project (.github/extensions/), user (~/.copilot/extensions/), plugin (installed plugin), or session (session-state/<id>/extensions/)
    pub source: ExtensionSource,
    /// Current status: running, disabled, failed, or starting
    pub status: ExtensionStatus,
}

/// Slim input shape for extension_context attachments; identity fields are runtime-derived.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionContextPushInput {
    /// Caller-supplied JSON payload (required, may be null but not undefined)
    pub payload: serde_json::Value,
    /// Human-readable composer pill label
    pub title: String,
    /// Attachment type discriminator
    pub r#type: ExtensionContextPushInputType,
}

/// Extensions discovered for the session, with their current status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionList {
    /// Discovered extensions and their current status
    pub extensions: Vec<Extension>,
}

/// Source-qualified extension identifier to disable for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionsDisableRequest {
    /// Source-qualified extension ID to disable
    pub id: String,
}

/// Source-qualified extension identifier to enable for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExtensionsEnableRequest {
    /// Source-qualified extension ID to enable
    pub id: String,
}

/// Binary result returned by a tool for the model
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmBinaryResultsForLlm {
    /// Base64-encoded binary data
    pub data: String,
    /// Human-readable description of the binary data
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Optional metadata from the producing tool.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<HashMap<String, serde_json::Value>>,
    /// MIME type of the binary data
    pub mime_type: String,
    /// Binary result type discriminator. Use "image" for images and "resource" for other binary data.
    pub r#type: ExternalToolTextResultForLlmBinaryResultsForLlmType,
}

/// Expanded external tool result payload
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlm {
    /// Base64-encoded binary results returned to the model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub binary_results_for_llm: Option<Vec<ExternalToolTextResultForLlmBinaryResultsForLlm>>,
    /// Structured content blocks from the tool
    #[serde(skip_serializing_if = "Option::is_none")]
    pub contents: Option<Vec<serde_json::Value>>,
    /// Optional error message for failed executions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Execution outcome classification. Optional for back-compat; normalized to 'success' (or 'failure' when error is present) when missing or unrecognized.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result_type: Option<String>,
    /// Detailed log content for timeline display
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_log: Option<String>,
    /// Text result returned to the model
    pub text_result_for_llm: String,
    /// Optional tool-specific telemetry
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_telemetry: Option<HashMap<String, serde_json::Value>>,
}

/// Audio content block with base64-encoded data
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentAudio {
    /// Base64-encoded audio data
    pub data: String,
    /// MIME type of the audio (e.g., audio/wav, audio/mpeg)
    pub mime_type: String,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentAudioType,
}

/// Image content block with base64-encoded data
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentImage {
    /// Base64-encoded image data
    pub data: String,
    /// MIME type of the image (e.g., image/png, image/jpeg)
    pub mime_type: String,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentImageType,
}

/// Embedded resource content block with inline text or binary data
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentResource {
    /// The embedded resource contents, either text or base64-encoded binary
    pub resource: serde_json::Value,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentResourceType,
}

/// Icon image for a resource
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentResourceLinkIcon {
    /// MIME type of the icon image
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Available icon sizes (e.g., ['16x16', '32x32'])
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sizes: Option<Vec<String>>,
    /// URL or path to the icon image
    pub src: String,
    /// Theme variant this icon is intended for
    #[serde(skip_serializing_if = "Option::is_none")]
    pub theme: Option<ExternalToolTextResultForLlmContentResourceLinkIconTheme>,
}

/// Resource link content block referencing an external resource
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentResourceLink {
    /// Human-readable description of the resource
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Icons associated with this resource
    #[serde(skip_serializing_if = "Option::is_none")]
    pub icons: Option<Vec<ExternalToolTextResultForLlmContentResourceLinkIcon>>,
    /// MIME type of the resource content
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Resource name identifier
    pub name: String,
    /// Size of the resource in bytes
    #[serde(skip_serializing_if = "Option::is_none")]
    pub size: Option<i64>,
    /// Human-readable display title for the resource
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentResourceLinkType,
    /// URI identifying the resource
    pub uri: String,
}

/// Terminal/shell output content block with optional exit code and working directory
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentTerminal {
    /// Working directory where the command was executed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Process exit code, if the command has completed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exit_code: Option<i64>,
    /// Terminal/shell output text
    pub text: String,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentTerminalType,
}

/// Plain text content block
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExternalToolTextResultForLlmContentText {
    /// The text content
    pub text: String,
    /// Content block type discriminator
    pub r#type: ExternalToolTextResultForLlmContentTextType,
}

/// Optional user prompt to combine with the fleet orchestration instructions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FleetStartRequest {
    /// Optional user prompt to combine with fleet instructions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prompt: Option<String>,
}

/// Indicates whether fleet mode was successfully activated.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FleetStartResult {
    /// Whether fleet mode was successfully activated
    pub started: bool,
}

/// Folder path to add to trusted folders.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FolderTrustAddParams {
    /// Folder path to mark as trusted
    pub path: String,
}

/// Folder path to check for trust.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FolderTrustCheckParams {
    /// Folder path to check
    pub path: String,
}

/// Folder trust check result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FolderTrustCheckResult {
    /// Whether the folder is trusted
    pub trusted: bool,
}

/// Schema for the `GhCliAuthInfo` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GhCliAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Authentication host.
    pub host: String,
    /// User login as reported by `gh auth status`.
    pub login: String,
    /// The token returned by `gh auth token`. Treat as a secret.
    pub token: String,
    /// Authentication via the `gh` CLI's saved credentials.
    pub r#type: GhCliAuthInfoType,
}

/// Pending external tool call request ID, with the tool result or an error describing why it failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HandlePendingToolCallRequest {
    /// Error message if the tool call failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Request ID of the pending tool call
    pub request_id: RequestId,
    /// Tool call result (string or expanded result object)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
}

/// Indicates whether the external tool call result was handled successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HandlePendingToolCallResult {
    /// Whether the tool call result was handled successfully
    pub success: bool,
}

/// Indicates whether an in-progress manual compaction was aborted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryAbortManualCompactionResult {
    /// Whether an in-progress manual compaction was aborted. False when no manual compaction was running, when its abort controller was already aborted, or when the session is remote.
    pub aborted: bool,
}

/// Indicates whether an in-progress background compaction was cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryCancelBackgroundCompactionResult {
    /// Whether an in-progress background compaction was cancelled. False when no compaction was running, when the session is remote, or when the underlying processor was unavailable.
    pub cancelled: bool,
}

/// Post-compaction context window usage breakdown
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryCompactContextWindow {
    /// Token count from non-system messages (user, assistant, tool)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub conversation_tokens: Option<i64>,
    /// Current total tokens in the context window (system + conversation + tool definitions)
    pub current_tokens: i64,
    /// Current number of messages in the conversation
    pub messages_length: i64,
    /// Token count from system message(s)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub system_tokens: Option<i64>,
    /// Maximum token count for the model's context window
    pub token_limit: i64,
    /// Token count from tool definitions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_definitions_tokens: Option<i64>,
}

/// Optional compaction parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryCompactRequest {
    /// Optional user-provided instructions to focus the compaction summary
    #[serde(skip_serializing_if = "Option::is_none")]
    pub custom_instructions: Option<String>,
}

/// Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryCompactResult {
    /// Post-compaction context window usage breakdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_window: Option<HistoryCompactContextWindow>,
    /// Number of messages removed during compaction
    pub messages_removed: i64,
    /// Whether compaction completed successfully
    pub success: bool,
    /// Summary text produced by compaction. Omitted when compaction did not produce a summary (e.g. failure path).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary_content: Option<String>,
    /// Number of tokens freed by compaction
    pub tokens_removed: i64,
}

/// Markdown summary of the conversation context (empty when not available).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistorySummarizeForHandoffResult {
    /// Markdown summary of the conversation context produced by an LLM. Empty string when there are no messages or when the session does not support local summarization.
    pub summary: String,
}

/// Identifier of the event to truncate to; this event and all later events are removed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryTruncateRequest {
    /// Event ID to truncate to. This event and all events after it are removed from the session.
    pub event_id: String,
}

/// Number of events that were removed by the truncation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HistoryTruncateResult {
    /// Number of events that were removed
    pub events_removed: i64,
}

/// Schema for the `HMACAuthInfo` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HMACAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// HMAC secret used to sign requests.
    pub hmac: String,
    /// Authentication host. HMAC auth always targets the public GitHub host.
    pub host: HMACAuthInfoHost,
    /// HMAC-based authentication used by GitHub-internal services.
    pub r#type: HMACAuthInfoType,
}

/// Schema for the `InstalledPlugin` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledPlugin {
    /// Path where the plugin is cached locally
    #[serde(rename = "cache_path", skip_serializing_if = "Option::is_none")]
    pub cache_path: Option<String>,
    /// Whether the plugin is currently enabled
    pub enabled: bool,
    /// Installation timestamp
    #[serde(rename = "installed_at")]
    pub installed_at: String,
    /// Marketplace the plugin came from (empty string for direct repo installs)
    pub marketplace: String,
    /// Plugin name
    pub name: String,
    /// Source for direct repo installs (when marketplace is empty)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<serde_json::Value>,
    /// Version installed (if available)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Information about an installed plugin tracked in global state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledPluginInfo {
    /// Whether the plugin is currently enabled for new sessions
    pub enabled: bool,
    /// Marketplace the plugin came from. Empty string ("") for direct repo / URL / local installs.
    pub marketplace: String,
    /// Plugin name
    pub name: String,
    /// Installed version (when reported by the plugin manifest)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Schema for the `InstalledPluginSourceGitHub` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledPluginSourceGitHub {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#ref: Option<String>,
    pub repo: String,
    /// Constant value. Always "github".
    pub source: InstalledPluginSourceGitHubSource,
}

/// Schema for the `InstalledPluginSourceLocal` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledPluginSourceLocal {
    pub path: String,
    /// Constant value. Always "local".
    pub source: InstalledPluginSourceLocalSource,
}

/// Schema for the `InstalledPluginSourceUrl` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledPluginSourceUrl {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#ref: Option<String>,
    /// Constant value. Always "url".
    pub source: InstalledPluginSourceUrlSource,
    pub url: String,
}

/// Schema for the `InstructionDiscoveryPath` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionDiscoveryPath {
    /// Whether the target is a single file or a directory of instruction files
    pub kind: InstructionDiscoveryPathKind,
    /// Which tier this target belongs to
    pub location: InstructionDiscoveryPathLocation,
    /// Absolute path of the file or directory (may not exist on disk yet)
    pub path: String,
    /// Whether this is the canonical target to create new instructions in its tier. At most one entry per tier is preferred.
    pub preferred_for_creation: bool,
    /// The input project path this target was derived from (only for repository targets)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_path: Option<String>,
}

/// Canonical files and directories where custom instructions can be created so the runtime will recognize them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionDiscoveryPathList {
    /// Canonical instruction create/discovery files and directories, in priority order
    pub paths: Vec<InstructionDiscoveryPath>,
}

/// Optional project paths to include in instruction discovery.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsDiscoverRequest {
    /// When true, omit the host's instruction sources (user/home-level files and plugin rules), leaving only repository and working-directory sources. For multitenant deployments.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_host_instructions: Option<bool>,
    /// Optional list of project directory paths to scan for repository/working-directory instruction sources. When omitted or empty, only user-level and plugin instruction sources are returned (no project scan).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
}

/// Optional project paths to include when enumerating instruction discovery targets.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsGetDiscoveryPathsRequest {
    /// When true, omit the host's user-level instruction targets, leaving only repository targets. For multitenant deployments (mirrors `discover`'s `excludeHostInstructions`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_host_instructions: Option<bool>,
    /// Optional list of project directory paths. When omitted or empty, only the user-level targets are returned.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
}

/// Schema for the `InstructionSource` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionSource {
    /// Glob pattern(s) from frontmatter — when set, this instruction applies only to matching files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub apply_to: Option<Vec<String>>,
    /// Raw content of the instruction file
    pub content: String,
    /// When true, this source starts disabled and must be toggled on by the user
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_disabled: Option<bool>,
    /// Short description (body after frontmatter) for use in instruction tables
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Unique identifier for this source (used for toggling)
    pub id: String,
    /// Human-readable label
    pub label: String,
    /// Where this source lives — used for UI grouping
    pub location: InstructionSourceLocation,
    /// The project path this source was discovered from. Only set by sessionless discovery for repository/working-directory sources, where it disambiguates same-named files (e.g. .github/copilot-instructions.md) across multiple workspace roots. The session-scoped getSources leaves it unset.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_path: Option<String>,
    /// File path relative to repo or absolute for home
    pub source_path: String,
    /// Category of instruction source — used for merge logic
    pub r#type: InstructionSourceType,
}

/// Instruction sources loaded for the session, in merge order.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsGetSourcesResult {
    /// Instruction sources for the session
    pub sources: Vec<InstructionSource>,
}

/// A request body chunk or cancellation signal.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpRequestChunkRequest {
    /// When true, `data` is base64-encoded bytes. When absent or false, `data` is UTF-8 text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub binary: Option<bool>,
    /// When true, the runtime is cancelling the in-flight request (e.g. upstream consumer aborted). `data` is ignored. Implies end-of-request.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cancel: Option<bool>,
    /// Optional human-readable reason for the cancellation, propagated for logging.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cancel_reason: Option<String>,
    /// Body byte range. UTF-8 text when `binary` is absent or false; base64-encoded bytes when `binary` is true. May be empty.
    pub data: String,
    /// When true, this is the final body chunk for the request. The SDK may rely on having received an end-marked chunk before treating the request body as complete.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub end: Option<bool>,
    /// Matches the requestId from the originating httpRequestStart frame.
    pub request_id: RequestId,
}

/// Acknowledgement. The SDK is free to ignore the ack and treat chunk delivery as fire-and-forget.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpRequestChunkResult {}

/// The head of an outbound model-layer HTTP request.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpRequestStartRequest {
    pub headers: HashMap<String, Vec<String>>,
    /// HTTP method, e.g. GET, POST.
    pub method: String,
    /// Opaque runtime-minted id, unique per in-flight request. The SDK uses this to correlate httpRequestChunk frames and to address its httpResponseStart / httpResponseChunk replies back to the runtime.
    pub request_id: RequestId,
    /// Id of the runtime session that triggered this request, when one is in scope. Absent for requests issued outside any session (e.g. startup model-catalog or capability resolution). This is a payload field — not a dispatch key — because the client-global API is registered process-wide rather than per session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// Transport the runtime would otherwise use for this request. `http` (the default when absent) covers plain HTTP and SSE responses; `websocket` indicates a full-duplex message channel where each body chunk maps to one WebSocket message and the `binary` flag distinguishes text from binary frames. The SDK consumer uses this to decide whether to service the request with an HTTP client or a WebSocket client. It is the one piece of request metadata the consumer cannot reliably infer from the URL or headers alone.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub transport: Option<LlmInferenceHttpRequestStartTransport>,
    /// Absolute request URL.
    pub url: String,
}

/// Acknowledgement. Returning successfully simply means the SDK accepted the start frame; it does not imply the request will succeed.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpRequestStartResult {}

/// Set to terminate the response with a transport-level failure. Implies end-of-stream; any further chunks for this requestId are ignored.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpResponseChunkError {
    /// Optional machine-readable error code.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub code: Option<String>,
    /// Human-readable failure description.
    pub message: String,
}

/// A response body chunk or terminal error.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpResponseChunkRequest {
    /// When true, `data` is base64-encoded bytes. When absent or false, `data` is UTF-8 text.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub binary: Option<bool>,
    /// Body byte range. UTF-8 text when `binary` is absent or false; base64-encoded bytes when `binary` is true. May be empty (e.g. when the response body is empty: send a single chunk with empty data and end=true).
    pub data: String,
    /// When true, this is the final body chunk for the response. The runtime treats the response body as complete after receiving an end-marked chunk.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub end: Option<bool>,
    /// Set to terminate the response with a transport-level failure. Implies end-of-stream; any further chunks for this requestId are ignored.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<LlmInferenceHttpResponseChunkError>,
    /// Matches the requestId from the originating httpRequestStart frame.
    pub request_id: RequestId,
}

/// Whether the chunk was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpResponseChunkResult {
    /// True when the chunk was matched to a pending request; false when unknown.
    pub accepted: bool,
}

/// Response head.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpResponseStartRequest {
    pub headers: HashMap<String, Vec<String>>,
    /// Matches the requestId from the originating httpRequestStart frame.
    pub request_id: RequestId,
    /// HTTP status code.
    pub status: i64,
    /// Optional HTTP status reason phrase.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_text: Option<String>,
}

/// Whether the start frame was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceHttpResponseStartResult {
    /// True when the response start was matched to a pending request; false when unknown.
    pub accepted: bool,
}

/// Indicates whether the calling client was registered as the LLM inference provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LlmInferenceSetProviderResult {
    /// Whether the provider was set successfully
    pub success: bool,
}

/// Pre-resolved working-directory context for session startup.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionContext {
    /// Active git branch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Most recent working directory for this session
    pub cwd: String,
    /// Git repository root, if the cwd was inside a git repo
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Repository host type
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_type: Option<SessionContextHostType>,
    /// Repository slug in `owner/name` form, when known
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
}

/// Schema for the `LocalSessionMetadataValue` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LocalSessionMetadataValue {
    /// Runtime client name that created/last resumed this session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Pre-resolved working-directory context for session startup.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<SessionContext>,
    /// True for detached maintenance sessions that should be hidden from normal resume lists.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_detached: Option<bool>,
    /// Always false for local sessions.
    pub is_remote: bool,
    /// GitHub task ID, when this local session is bound to one. Only present for local sessions exported to remote control.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mc_task_id: Option<String>,
    /// Last-modified time of the session's persisted state, as ISO 8601
    pub modified_time: String,
    /// Optional human-friendly name set via /rename
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Stable session identifier
    pub session_id: SessionId,
    /// Session creation time as an ISO 8601 timestamp
    pub start_time: String,
    /// Short summary of the session, when one has been derived
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
}

/// Message text, optional severity level, persistence flag, optional follow-up URL, and optional tip.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LogRequest {
    /// When true, the message is transient and not persisted to the session event log on disk
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ephemeral: Option<bool>,
    /// Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub level: Option<SessionLogLevel>,
    /// Human-readable message
    pub message: String,
    /// Optional actionable tip displayed alongside the message. Only honored on `level: "info"`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tip: Option<String>,
    /// Domain category for this log entry (e.g., "mcp", "subscription", "policy", "model"). Maps to `infoType`/`warningType`/`errorType` on the emitted event. Defaults to "notification".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<String>,
    /// Optional URL the user can open in their browser for more details
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Identifier of the session event that was emitted for the log message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LogResult {
    /// The unique identifier of the emitted session event
    pub event_id: String,
}

/// Parameters for (re)loading the merged LSP configuration set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LspInitializeRequest {
    /// Force re-initialization even when LSP configs were already loaded for the working directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force: Option<bool>,
    /// Git root used as the boundary when traversing for project-level LSP configs (supports monorepos).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Working directory used to load project-level LSP configs. Defaults to the session working directory when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// Result of registering a new marketplace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceAddResult {
    /// Final name of the marketplace as resolved from its manifest
    pub name: String,
}

/// Plugin entry advertised by a marketplace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplacePluginInfo {
    /// Short description from the marketplace catalog, when present
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Plugin name as listed in the marketplace catalog
    pub name: String,
}

/// Plugins advertised by the marketplace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceBrowseResult {
    /// Plugins advertised by the marketplace
    pub plugins: Vec<MarketplacePluginInfo>,
}

/// Registered marketplace summary.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceInfo {
    /// True when this is a default marketplace shipped with the runtime. Defaults are not removable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_default: Option<bool>,
    /// Marketplace name (matches the @marketplace suffix in plugin specs)
    pub name: String,
    /// Human-readable description of where the marketplace data is fetched from (e.g. "GitHub: owner/repo").
    pub source: String,
}

/// All registered marketplaces, including built-in defaults.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceListResult {
    /// Registered marketplaces
    pub marketplaces: Vec<MarketplaceInfo>,
}

/// Schema for the `MarketplaceRefreshEntry` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceRefreshEntry {
    /// Error message (failure only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Marketplace name that was refreshed
    pub name: String,
    /// Whether the refresh succeeded
    pub success: bool,
}

/// Result of refreshing one or more marketplace catalogs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceRefreshResult {
    /// Per-marketplace refresh results in deterministic order.
    pub results: Vec<MarketplaceRefreshEntry>,
}

/// Outcome of the remove attempt, including dependent-plugin info when applicable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MarketplaceRemoveResult {
    /// Names of installed plugins that prevented removal. Populated only when `removed=false`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub dependent_plugins: Option<Vec<String>>,
    /// True when the marketplace was actually removed. False when removal was skipped because the marketplace has dependent plugins and `force` was not set.
    pub removed: bool,
}

/// Schema for the `McpAllowedServer` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAllowedServer {
    /// Allowed server name
    pub name: String,
    /// PII-free note explaining why the server was allowed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub redacted_note: Option<String>,
}

/// MCP server, tool name, and arguments to invoke from an MCP App view.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsCallToolRequest {
    /// Tool arguments
    #[serde(skip_serializing_if = "Option::is_none")]
    pub arguments: Option<HashMap<String, serde_json::Value>>,
    /// **Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.
    pub origin_server_name: String,
    /// MCP server hosting the tool
    pub server_name: String,
    /// MCP tool name
    pub tool_name: String,
}

/// Capability negotiation snapshot
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsDiagnoseCapability {
    /// Whether the runtime advertises `extensions.io.modelcontextprotocol/ui` to MCP servers
    pub advertised: bool,
    /// Whether the MCP_APPS feature flag (or COPILOT_MCP_APPS env override) is on
    pub feature_flag_enabled: bool,
    /// Whether the session has the `mcp-apps` capability
    pub session_has_mcp_apps: bool,
}

/// MCP server to diagnose MCP Apps wiring for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsDiagnoseRequest {
    /// MCP server to probe
    pub server_name: String,
}

/// What the server returned for this session
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsDiagnoseServer {
    /// Whether the named server is currently connected
    pub connected: bool,
    /// Up to 5 tool names with `_meta.ui` for quick inspection
    pub sample_tool_names: Vec<String>,
    /// Total tools returned by the server's tools/list
    pub tool_count: f64,
    /// Tools whose `_meta.ui` is populated (resourceUri and/or visibility set)
    pub tools_with_ui_meta: f64,
}

/// Diagnostic snapshot of MCP Apps wiring for the named server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsDiagnoseResult {
    /// Capability negotiation snapshot
    pub capability: McpAppsDiagnoseCapability,
    /// What the server returned for this session
    pub server: McpAppsDiagnoseServer,
}

/// Current host context
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsHostContextDetails {
    /// Display modes the host supports
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available_display_modes: Option<Vec<McpAppsHostContextDetailsAvailableDisplayMode>>,
    /// Current display mode (SEP-1865)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_mode: Option<McpAppsHostContextDetailsDisplayMode>,
    /// BCP-47 locale, e.g. 'en-US'
    #[serde(skip_serializing_if = "Option::is_none")]
    pub locale: Option<String>,
    /// Platform type for responsive design
    #[serde(skip_serializing_if = "Option::is_none")]
    pub platform: Option<McpAppsHostContextDetailsPlatform>,
    /// UI theme preference per SEP-1865
    #[serde(skip_serializing_if = "Option::is_none")]
    pub theme: Option<McpAppsHostContextDetailsTheme>,
    /// IANA timezone, e.g. 'America/New_York'
    #[serde(skip_serializing_if = "Option::is_none")]
    pub time_zone: Option<String>,
    /// Host application identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_agent: Option<String>,
}

/// Current host context advertised to MCP App guests.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsHostContext {
    /// Current host context
    pub context: McpAppsHostContextDetails,
}

/// MCP server to list app-callable tools for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsListToolsRequest {
    /// **Required.** Server whose ui:// view issued the request. Per SEP-1865 ('callable by the app from this server only'), the call is rejected when this differs from `serverName`, and rejected outright when missing.
    pub origin_server_name: String,
    /// MCP server hosting the app
    pub server_name: String,
}

/// App-callable tools from the named MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsListToolsResult {
    /// App-callable tools from the server
    pub tools: Vec<HashMap<String, serde_json::Value>>,
}

/// MCP server and resource URI to fetch.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsReadResourceRequest {
    /// Name of the MCP server hosting the resource
    pub server_name: String,
    /// Resource URI (typically ui://...)
    pub uri: String,
}

/// Schema for the `McpAppsResourceContent` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsResourceContent {
    /// Resource-level metadata (CSP, permissions, etc.)
    #[serde(rename = "_meta", skip_serializing_if = "Option::is_none")]
    pub meta: Option<HashMap<String, serde_json::Value>>,
    /// Base64-encoded binary content
    #[serde(skip_serializing_if = "Option::is_none")]
    pub blob: Option<String>,
    /// MIME type of the content
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Text content (e.g. HTML)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub text: Option<String>,
    /// The resource URI (typically ui://...)
    pub uri: String,
}

/// Resource contents returned by the MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsReadResourceResult {
    /// Resource contents returned by the server
    pub contents: Vec<McpAppsResourceContent>,
}

/// Host context advertised to MCP App guests
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsSetHostContextDetails {
    /// Display modes the host supports
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available_display_modes: Option<Vec<McpAppsSetHostContextDetailsAvailableDisplayMode>>,
    /// Current display mode (SEP-1865)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_mode: Option<McpAppsSetHostContextDetailsDisplayMode>,
    /// BCP-47 locale, e.g. 'en-US'
    #[serde(skip_serializing_if = "Option::is_none")]
    pub locale: Option<String>,
    /// Platform type for responsive design
    #[serde(skip_serializing_if = "Option::is_none")]
    pub platform: Option<McpAppsSetHostContextDetailsPlatform>,
    /// UI theme preference per SEP-1865
    #[serde(skip_serializing_if = "Option::is_none")]
    pub theme: Option<McpAppsSetHostContextDetailsTheme>,
    /// IANA timezone, e.g. 'America/New_York'
    #[serde(skip_serializing_if = "Option::is_none")]
    pub time_zone: Option<String>,
    /// Host application identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_agent: Option<String>,
}

/// Host context to advertise to MCP App guests.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpAppsSetHostContextRequest {
    /// Host context advertised to MCP App guests
    pub context: McpAppsSetHostContextDetails,
}

/// The requestId previously passed to executeSampling that should be cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpCancelSamplingExecutionParams {
    /// The requestId previously passed to executeSampling that should be cancelled
    pub request_id: RequestId,
}

/// Indicates whether an in-flight sampling execution with the given requestId was found and cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpCancelSamplingExecutionResult {
    /// True if an in-flight execution with the given requestId was found and signalled to cancel. False when no such execution is in flight (already completed, never started, or cancelled by another caller).
    pub cancelled: bool,
}

/// MCP server name and configuration to add to user configuration.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigAddRequest {
    /// MCP server configuration (stdio process or remote HTTP/SSE)
    pub config: serde_json::Value,
    /// Unique name for the MCP server
    pub name: String,
}

/// MCP server names to disable for new sessions.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigDisableRequest {
    /// Names of MCP servers to disable. Each server is added to the persisted disabled list so new sessions skip it. Already-disabled names are ignored. Active sessions keep their current connections until they end.
    pub names: Vec<String>,
}

/// MCP server names to enable for new sessions.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigEnableRequest {
    /// Names of MCP servers to enable. Each server is removed from the persisted disabled list so new sessions spawn it. Unknown or already-enabled names are ignored.
    pub names: Vec<String>,
}

/// User-configured MCP servers, keyed by server name.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigList {
    /// All MCP servers from user config, keyed by name
    pub servers: HashMap<String, serde_json::Value>,
}

/// MCP server name to remove from user configuration.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigRemoveRequest {
    /// Name of the MCP server to remove
    pub name: String,
}

/// MCP server name and replacement configuration to write to user configuration.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigUpdateRequest {
    /// MCP server configuration (stdio process or remote HTTP/SSE)
    pub config: serde_json::Value,
    /// Name of the MCP server to update
    pub name: String,
}

/// Opaque auth info used to configure GitHub MCP.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpConfigureGitHubRequest {
    /// Opaque runtime auth info for GitHub MCP configuration. Marked internal: an in-process runtime shape (configureGitHubMcp is a no-op over the wire).
    #[doc(hidden)]
    pub(crate) auth_info: serde_json::Value,
}

/// Result of configuring GitHub MCP.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigureGitHubResult {
    /// Whether GitHub MCP configuration changed.
    pub changed: bool,
}

/// Name of the MCP server to disable for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDisableRequest {
    /// Name of the MCP server to disable
    pub server_name: String,
}

/// Optional working directory used as context for MCP server discovery.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDiscoverRequest {
    /// Working directory used as context for discovery (e.g., plugin resolution)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// MCP servers discovered from user, workspace, plugin, and built-in sources.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpDiscoverResult {
    /// MCP servers discovered from all sources
    pub servers: Vec<DiscoveredMcpServer>,
}

/// Name of the MCP server to enable for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpEnableRequest {
    /// Name of the MCP server to enable
    pub server_name: String,
}

/// Raw MCP CreateMessageRequest params, as received in the `sampling.requested` event. Treated as opaque at the schema layer; the runtime converts the embedded MCP messages into the OpenAI chat-completion shape internally.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpExecuteSamplingRequest {}

/// Identifiers and raw MCP CreateMessageRequest params used to run a sampling inference.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpExecuteSamplingParams {
    /// The original MCP JSON-RPC request ID (string or number). Used by the runtime to correlate the inference with the originating MCP request for telemetry; this is distinct from `requestId` (which is the schema-level cancellation handle).
    pub mcp_request_id: serde_json::Value,
    /// Raw MCP CreateMessageRequest params, as received in the `sampling.requested` event. Treated as opaque at the schema layer; the runtime converts the embedded MCP messages into the OpenAI chat-completion shape internally.
    pub request: McpExecuteSamplingRequest,
    /// Caller-provided unique identifier for this sampling execution. Use this same ID with cancelSamplingExecution to cancel the in-flight call. Must be unique within the session for the lifetime of the call.
    pub request_id: RequestId,
    /// Name of the MCP server that initiated the sampling request
    pub server_name: String,
}

/// Schema for the `McpFilteredServer` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpFilteredServer {
    /// Enterprise login associated with an allowlist policy
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enterprise_name: Option<String>,
    /// Filtered server name
    pub name: String,
    /// Human-readable filter reason
    pub reason: String,
    /// PII-free filter reason
    #[serde(skip_serializing_if = "Option::is_none")]
    pub redacted_reason: Option<String>,
}

/// Recorded MCP server connection failure.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerFailureInfo {
    /// Failure message produced when the MCP server connection failed.
    pub message: String,
    /// epoch-ms timestamp at which the failure was recorded.
    pub timestamp: i64,
}

/// Recorded MCP server pending-auth state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerNeedsAuthInfo {
    /// epoch-ms timestamp at which the server signalled it needs authentication.
    pub timestamp: i64,
}

/// Host-level state, omitted when no MCP host is initialized.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpHostState {
    /// Names of currently-connected MCP clients.
    pub clients: Vec<String>,
    /// Configured servers that are explicitly disabled.
    pub disabled_servers: Vec<String>,
    /// Map of server name to recorded connection failure.
    pub failed_servers: HashMap<String, McpServerFailureInfo>,
    /// Configured servers filtered out by enterprise allowlist policy.
    pub filtered_servers: Vec<String>,
    /// Whether third-party MCP servers are policy-enabled for this session.
    pub mcp3p_enabled: bool,
    /// Map of server name to recorded pending-auth state.
    pub needs_auth_servers: HashMap<String, McpServerNeedsAuthInfo>,
    /// Names of servers with in-flight connection attempts.
    pub pending_connections: Vec<String>,
}

/// Server name to check running status for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpIsServerRunningRequest {
    /// Name of the MCP server to check
    pub server_name: String,
}

/// Whether the named MCP server is running.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpIsServerRunningResult {
    /// True if the server has an active client and transport.
    pub running: bool,
}

/// Server name whose tool list should be returned.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpListToolsRequest {
    /// Name of the connected MCP server whose tools to list.
    pub server_name: String,
}

/// Schema for the `McpTools` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpTools {
    /// Tool description, when provided.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Tool name.
    pub name: String,
}

/// Tools exposed by the connected MCP server. Throws when the server is not connected.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpListToolsResult {
    /// Tools exposed by the server.
    pub tools: Vec<McpTools>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthPendingRequestResponseToken {
    /// Access token acquired by the SDK host
    pub access_token: String,
    /// Token lifetime in seconds, if known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expires_in: Option<i64>,
    pub kind: McpOauthPendingRequestResponseTokenKind,
    /// Refresh token supplied by the host, if available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub refresh_token: Option<String>,
    /// OAuth token type. Defaults to Bearer when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token_type: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthPendingRequestResponseCancelled {
    pub kind: McpOauthPendingRequestResponseCancelledKind,
}

/// Pending MCP OAuth request ID and host-provided token or cancellation response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthHandlePendingRequest {
    /// OAuth request identifier from the mcp.oauth_required event
    pub request_id: RequestId,
    /// Host response to the pending OAuth request.
    pub result: McpOauthPendingRequestResponse,
}

/// Indicates whether the pending MCP OAuth response was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthHandlePendingResult {
    /// Whether the response was accepted. False if the request was unknown, timed out, or already resolved.
    pub success: bool,
}

/// Remote MCP server name and optional overrides controlling reauthentication, OAuth client display name, and the callback success-page copy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthLoginRequest {
    /// Optional override for the body text shown on the OAuth loopback callback success page. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass surface-specific copy telling the user where to return.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub callback_success_message: Option<String>,
    /// Optional override for the OAuth client display name shown on the consent screen. Applies to newly registered dynamic clients only — existing registrations keep the name they were created with. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass their own surface-specific label so the consent screen matches the product the user sees.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// When true, clears any cached OAuth token for the server and runs a full new authorization. Use when the user explicitly wants to switch accounts or believes their session is stuck.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force_reauth: Option<bool>,
    /// Name of the remote MCP server to authenticate
    pub server_name: String,
}

/// OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthLoginResult {
    /// URL the caller should open in a browser to complete OAuth. Omitted when cached tokens were still valid and no browser interaction was needed — the server is already reconnected in that case. When present, the runtime starts the callback listener before returning and continues the flow in the background; completion is signaled via session.mcp_server_status_changed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_url: Option<String>,
}

/// MCP OAuth request id and optional provider response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpOauthRespondRequest {
    /// In-process OAuthClientProvider instance, or omitted to deny. Marked internal: cannot be serialized across the JSON-RPC boundary.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) provider: Option<serde_json::Value>,
    /// OAuth request identifier from mcp.oauth_required
    pub request_id: RequestId,
}

/// Empty result after recording the MCP OAuth response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpOauthRespondResult {}

/// Registration parameters for an external MCP client.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpRegisterExternalClientRequest {
    /// In-process MCP Client instance. Marked internal: cannot be serialized across the JSON-RPC boundary.
    #[doc(hidden)]
    pub(crate) client: serde_json::Value,
    /// In-process server config (MCPServerConfig) paired with the in-process client/transport. Marked internal alongside its companions.
    #[doc(hidden)]
    pub(crate) config: serde_json::Value,
    /// Logical server name for the external client
    pub server_name: String,
    /// In-process MCP Transport instance. Marked internal: cannot be serialized across the JSON-RPC boundary.
    #[doc(hidden)]
    pub(crate) transport: serde_json::Value,
}

/// Opaque MCP reload configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpReloadWithConfigRequest {
    /// Opaque runtime MCP reload configuration. Marked internal: an in-process runtime shape (reloadMcpServers throws over the wire).
    #[doc(hidden)]
    pub(crate) config: serde_json::Value,
}

/// Indicates whether the auto-managed `github` MCP server was removed (false when nothing to remove).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpRemoveGitHubResult {
    /// True when the auto-managed `github` MCP server was removed; false when no removal happened (e.g. user has explicitly configured a `github` server, or the server was not registered).
    pub removed: bool,
}

/// Server name and opaque configuration for an individual MCP server restart.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpRestartServerRequest {
    /// Opaque server configuration (MCPServerConfig). Marked internal: an in-process runtime shape supplied only by in-process CLI callers.
    #[doc(hidden)]
    pub(crate) config: serde_json::Value,
    /// Name of the MCP server to restart
    pub server_name: String,
}

/// Outcome of an MCP sampling execution: success result, failure error, or cancellation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpSamplingExecutionResult {
    /// Outcome of the sampling inference. 'success' produced a response; 'failure' encountered an error (including agent-side rejection by content filter or criteria); 'cancelled' the caller cancelled this execution via cancelSamplingExecution.
    pub action: McpSamplingExecutionAction,
    /// Error description, present when action='failure'.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// MCP CreateMessageResult payload (with optional 'tools' extension), present when action='success'. Treated as opaque at the schema layer; consumers should construct/consume it per the MCP CreateMessageResult shape.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<McpExecuteSamplingResult>,
}

/// Schema for the `McpServer` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServer {
    /// Error message if the server failed to connect
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Server name (config key)
    pub name: String,
    /// Configuration source: user, workspace, plugin, or builtin
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<McpServerSource>,
    /// Plugin name that provided this server, when source is plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin: Option<String>,
    /// Plugin version that provided this server, when source is plugin.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source_plugin_version: Option<String>,
    /// Connection status: connected, failed, needs-auth, pending, disabled, or not_configured
    pub status: McpServerStatus,
}

/// Authentication settings with optional redirect port configuration.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerAuthConfigRedirectPort {
    /// Fixed port for the OAuth redirect callback server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub redirect_port: Option<i32>,
}

/// Remote MCP server configuration accessed over HTTP or SSE.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerConfigHttp {
    /// Set to `true` to use defaults, or provide an object with additional auth or OIDC settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth: Option<serde_json::Value>,
    /// Controls if tools provided by this server can be loaded on demand via tool search (auto) or always included in the initial tool list (never)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_tools: Option<McpServerConfigDeferTools>,
    /// Content filtering mode to apply to all tools, or a map of tool name to content filtering mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filter_mapping: Option<serde_json::Value>,
    /// HTTP headers to include in requests to the remote MCP server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
    /// Whether this server is a built-in fallback used when the user has not configured their own server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_default_server: Option<bool>,
    /// OAuth client ID for a pre-registered remote MCP OAuth client.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oauth_client_id: Option<String>,
    /// OAuth grant type to use when authenticating to the remote MCP server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oauth_grant_type: Option<McpServerConfigHttpOauthGrantType>,
    /// Whether the configured OAuth client is public and does not require a client secret.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oauth_public_client: Option<bool>,
    /// Set to `true` to use defaults, or provide an object with additional auth or OIDC settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oidc: Option<serde_json::Value>,
    /// Timeout in milliseconds for tool calls to this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
    /// Tools to include. Defaults to all tools if not specified.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
    /// Remote transport type. Defaults to "http" when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<McpServerConfigHttpType>,
    /// URL of the remote MCP server endpoint.
    pub url: String,
}

/// Stdio MCP server configuration launched as a child process.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerConfigStdio {
    /// Command-line arguments passed to the Stdio MCP server process.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub args: Option<Vec<String>>,
    /// Set to `true` to use defaults, or provide an object with additional auth or OIDC settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth: Option<serde_json::Value>,
    /// Executable command used to start the Stdio MCP server process.
    pub command: String,
    /// Working directory for the Stdio MCP server process.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Controls if tools provided by this server can be loaded on demand via tool search (auto) or always included in the initial tool list (never)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_tools: Option<McpServerConfigDeferTools>,
    /// Environment variables to pass to the Stdio MCP server process.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env: Option<HashMap<String, String>>,
    /// Content filtering mode to apply to all tools, or a map of tool name to content filtering mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filter_mapping: Option<serde_json::Value>,
    /// Whether this server is a built-in fallback used when the user has not configured their own server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_default_server: Option<bool>,
    /// Set to `true` to use defaults, or provide an object with additional auth or OIDC settings.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub oidc: Option<serde_json::Value>,
    /// Timeout in milliseconds for tool calls to this server.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
    /// Tools to include. Defaults to all tools if not specified.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tools: Option<Vec<String>>,
}

/// MCP servers configured for the session, with their connection status and host-level state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpServerList {
    /// Host-level state, omitted when no MCP host is initialized.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<McpHostState>,
    /// Configured MCP servers
    pub servers: Vec<McpServer>,
}

/// Mode controlling how MCP server env values are resolved (`direct` or `indirect`).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpSetEnvValueModeParams {
    /// How environment-variable values supplied to MCP servers are resolved. "direct" passes literal string values; "indirect" treats values as references (e.g. names of environment variables on the host) that the runtime resolves before launch. Defaults to the runtime's startup mode; clients that intentionally launch MCP servers with literal values (e.g. CLI prompt mode and ACP) set this to "direct".
    pub mode: McpSetEnvValueModeDetails,
}

/// Env-value mode recorded on the session after the update.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpSetEnvValueModeResult {
    /// Mode recorded on the session after the update
    pub mode: McpSetEnvValueModeDetails,
}

/// Server name and opaque configuration for an individual MCP server start.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpStartServerRequest {
    /// Opaque server configuration (MCPServerConfig). Marked internal: an in-process runtime shape supplied only by in-process CLI callers.
    #[doc(hidden)]
    pub(crate) config: serde_json::Value,
    /// Name of the MCP server to start
    pub server_name: String,
}

/// MCP server startup filtering result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpStartServersResult {
    /// Non-default servers allowed by policy
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allowed_servers: Option<Vec<McpAllowedServer>>,
    /// Servers filtered out before startup
    pub filtered_servers: Vec<McpFilteredServer>,
}

/// Server name for an individual MCP server stop.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpStopServerRequest {
    /// Name of the MCP server to stop
    pub server_name: String,
}

/// Server name identifying the external client to remove.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct McpUnregisterExternalClientRequest {
    /// Server name of the external client to unregister
    pub server_name: String,
}

/// Memory configuration for this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MemoryConfiguration {
    /// Whether memory is enabled for the session.
    pub enabled: bool,
}

/// Model identifier and token limits used to compute the context-info breakdown.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataContextInfoRequest {
    /// Maximum output tokens allowed by the target model. Pass 0 if unknown.
    pub output_token_limit: i64,
    /// Maximum prompt tokens allowed by the target model. Pass 0 to use the runtime default.
    pub prompt_token_limit: i64,
    /// Model identifier used for tokenization. Omit to use the session default. Used both for token counting and to compute display values.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_model: Option<String>,
}

/// Token-usage breakdown for the session's current context window
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataContextInfoResultContextInfo {
    /// Output reserve plus tokens after the buffer-exhaustion blocking threshold (default 95%)
    pub buffer_tokens: i64,
    /// Token count at which background compaction starts (configurable percentage of promptTokenLimit)
    pub compaction_threshold: i64,
    /// Tokens consumed by user/assistant/tool messages
    pub conversation_tokens: i64,
    /// Prompt token limit plus the model's full output token limit.
    pub limit: i64,
    /// Tokens consumed by MCP tool definitions (subset of toolDefinitionsTokens, excludes deferred tools)
    pub mcp_tools_tokens: i64,
    /// The model used for token counting
    pub model_name: String,
    /// Maximum prompt tokens allowed by the model (or DEFAULT_TOKEN_LIMIT if unspecified)
    pub prompt_token_limit: i64,
    /// Tokens consumed by the system prompt
    pub system_tokens: i64,
    /// Tokens consumed by tool definitions sent to the model (excludes deferred tools)
    pub tool_definitions_tokens: i64,
    /// Sum of system, conversation and tool-definition tokens
    pub total_tokens: i64,
}

/// Token breakdown for the session's current context window, or null if uninitialized.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataContextInfoResult {
    /// Token breakdown for the current context window, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
    pub context_info: Option<MetadataContextInfoResultContextInfo>,
}

/// Indicates whether the local session is currently processing a turn or background continuation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataIsProcessingResult {
    /// Whether the session is currently processing user/agent messages. False for non-local sessions (which don't run a local agentic loop). Reflects an in-flight turn or background continuation.
    pub processing: bool,
}

/// Model identifier to use when re-tokenizing the session's existing messages.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataRecomputeContextTokensRequest {
    /// Model identifier used for tokenization. The runtime token-counts both chat-context and system-context messages against this model.
    pub model_id: String,
}

/// Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataRecomputeContextTokensResult {
    /// Tokens contributed by user/assistant/tool messages (excludes system/developer prompts).
    pub messages_token_count: i64,
    /// Tokens contributed by system/developer prompt snapshots.
    pub system_token_count: i64,
    /// Sum of tokens across chat-context and system-context messages currently held by the session.
    pub total_tokens: i64,
}

/// Updated working directory and git context. Emitted as the new payload of `session.context_changed`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkingDirectoryContext {
    /// Merge-base commit SHA (fork point from the remote default branch)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_commit: Option<String>,
    /// Current git branch name
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Current working directory path
    pub cwd: String,
    /// Root directory of the git repository, resolved via git rev-parse
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Head commit of the current git branch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub head_commit: Option<String>,
    /// Hosting platform type of the repository
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host_type: Option<SessionWorkingDirectoryContextHostType>,
    /// Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// Raw host string from the git remote URL (e.g. "github.com", "dev.azure.com")
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository_host: Option<String>,
}

/// Updated working-directory/git context to record on the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataRecordContextChangeRequest {
    /// Updated working directory and git context. Emitted as the new payload of `session.context_changed`.
    pub context: SessionWorkingDirectoryContext,
}

/// Notify the session that its working directory context has changed. Emits a `session.context_changed` event so consumers (telemetry, OTel tracker, ACP, the timeline UI) can react. Use this when the host has detected a cwd/branch/repo change outside the session's normal lifecycle (e.g., after a shell command in interactive mode).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataRecordContextChangeResult {}

/// Absolute path to set as the session's new working directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataSetWorkingDirectoryRequest {
    /// Absolute path to set as the session's working directory. The runtime updates the session's recorded cwd so subsequent operations (shell tools, file lookups, telemetry) anchor to it.
    pub working_directory: String,
}

/// Update the session's working directory. Used by the host when the user explicitly changes cwd (e.g., the `/cd` slash command). The host is responsible for `process.chdir` and any related side-effects (file index, etc.); this method only updates the session's own recorded path.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataSetWorkingDirectoryResult {
    /// Working directory after the update
    pub working_directory: String,
}

/// The repository the remote session targets.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataSnapshotRemoteMetadataRepository {
    /// The branch the remote session is operating on.
    pub branch: String,
    /// The GitHub repository name (without owner).
    pub name: String,
    /// The GitHub owner (user or organization) of the target repository.
    pub owner: String,
}

/// Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are immutable for the lifetime of the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct MetadataSnapshotRemoteMetadata {
    /// The pull request number the remote session is associated with, if any.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pull_request_number: Option<i64>,
    /// The repository the remote session targets.
    pub repository: MetadataSnapshotRemoteMetadataRepository,
    /// The original resource identifier (task ID or PR node ID), preserved across event-replay reconstructions. Falls back to `sessionId` when absent.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resource_id: Option<String>,
    /// Whether the remote task originated from Copilot Coding Agent (cca) or a CLI `--remote` invocation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task_type: Option<MetadataSnapshotRemoteMetadataTaskType>,
}

/// Long context tier pricing (available for models with extended context windows)
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelBillingTokenPricesLongContext {
    /// Deprecated: use cacheReadPrice. AI Credits cost per billing batch of cached tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_price: Option<f64>,
    /// AI Credits cost per billing batch of cached (read) tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_read_price: Option<f64>,
    /// AI Credits cost per billing batch of cache-write (cache creation) tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_write_price: Option<f64>,
    /// Deprecated: use maxPromptTokens. Prompt token budget for the long context tier. The total context window is this value plus the model's max_output_tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_max: Option<i64>,
    /// AI Credits cost per billing batch of input tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_price: Option<f64>,
    /// Prompt token budget for the long context tier. The total context window is this value plus the model's max_output_tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    /// AI Credits cost per billing batch of output tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_price: Option<f64>,
}

/// Token-level pricing information for this model
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelBillingTokenPrices {
    /// Number of tokens per standard billing batch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub batch_size: Option<i64>,
    /// Deprecated: use cacheReadPrice. AI Credits cost per billing batch of cached tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_price: Option<f64>,
    /// AI Credits cost per billing batch of cached (read) tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_read_price: Option<f64>,
    /// AI Credits cost per billing batch of cache-write (cache creation) tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cache_write_price: Option<f64>,
    /// Deprecated: use maxPromptTokens. Prompt token budget for the default tier. The total context window is this value plus the model's max_output_tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_max: Option<i64>,
    /// AI Credits cost per billing batch of input tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input_price: Option<f64>,
    /// Long context tier pricing (available for models with extended context windows)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub long_context: Option<ModelBillingTokenPricesLongContext>,
    /// Prompt token budget for the default tier. The total context window is this value plus the model's max_output_tokens.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    /// AI Credits cost per billing batch of output tokens
    #[serde(skip_serializing_if = "Option::is_none")]
    pub output_price: Option<f64>,
}

/// Billing information
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelBilling {
    /// Billing cost multiplier relative to the base rate
    #[serde(skip_serializing_if = "Option::is_none")]
    pub multiplier: Option<f64>,
    /// Token-level pricing information for this model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token_prices: Option<ModelBillingTokenPrices>,
}

/// Vision-specific limits
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesLimitsVision {
    /// Maximum image size in bytes
    #[serde(rename = "max_prompt_image_size")]
    pub max_prompt_image_size: i64,
    /// Maximum number of images per prompt
    #[serde(rename = "max_prompt_images")]
    pub max_prompt_images: i64,
    /// MIME types the model accepts
    #[serde(rename = "supported_media_types")]
    pub supported_media_types: Vec<String>,
}

/// Token limits for prompts, outputs, and context window
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesLimits {
    /// Maximum total context window size in tokens
    #[serde(
        rename = "max_context_window_tokens",
        skip_serializing_if = "Option::is_none"
    )]
    pub max_context_window_tokens: Option<i64>,
    /// Maximum number of output/completion tokens
    #[serde(rename = "max_output_tokens", skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<i64>,
    /// Maximum number of prompt/input tokens
    #[serde(rename = "max_prompt_tokens", skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    /// Vision-specific limits
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<ModelCapabilitiesLimitsVision>,
}

/// Feature flags indicating what the model supports
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesSupports {
    /// Whether this model supports reasoning effort configuration
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<bool>,
    /// Whether this model supports vision/image input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<bool>,
}

/// Model capabilities and limits
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilities {
    /// Token limits for prompts, outputs, and context window
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limits: Option<ModelCapabilitiesLimits>,
    /// Feature flags indicating what the model supports
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supports: Option<ModelCapabilitiesSupports>,
}

/// Policy state (if applicable)
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelPolicy {
    /// Current policy state for this model
    pub state: ModelPolicyState,
    /// Usage terms or conditions for this model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub terms: Option<String>,
}

/// Schema for the `Model` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Model {
    /// Billing information
    #[serde(skip_serializing_if = "Option::is_none")]
    pub billing: Option<ModelBilling>,
    /// Model capabilities and limits
    pub capabilities: ModelCapabilities,
    /// Default reasoning effort level (only present if model supports reasoning effort)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default_reasoning_effort: Option<String>,
    /// Model identifier (e.g., "claude-sonnet-4.5")
    pub id: String,
    /// Model capability category for grouping in the model picker
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_picker_category: Option<ModelPickerCategory>,
    /// Relative cost tier for token-based billing users
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_picker_price_category: Option<ModelPickerPriceCategory>,
    /// Display name
    pub name: String,
    /// Policy state (if applicable)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub policy: Option<ModelPolicy>,
    /// Supported reasoning effort levels (only present if model supports reasoning effort)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supported_reasoning_efforts: Option<Vec<String>>,
}

/// Vision-specific limits
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverrideLimitsVision {
    /// Maximum image size in bytes
    #[serde(
        rename = "max_prompt_image_size",
        skip_serializing_if = "Option::is_none"
    )]
    pub max_prompt_image_size: Option<i64>,
    /// Maximum number of images per prompt
    #[serde(rename = "max_prompt_images", skip_serializing_if = "Option::is_none")]
    pub max_prompt_images: Option<i64>,
    /// MIME types the model accepts
    #[serde(
        rename = "supported_media_types",
        skip_serializing_if = "Option::is_none"
    )]
    pub supported_media_types: Option<Vec<String>>,
}

/// Token limits for prompts, outputs, and context window
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverrideLimits {
    /// Maximum total context window size in tokens
    #[serde(
        rename = "max_context_window_tokens",
        skip_serializing_if = "Option::is_none"
    )]
    pub max_context_window_tokens: Option<i64>,
    /// Maximum number of output/completion tokens
    #[serde(rename = "max_output_tokens", skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<i64>,
    /// Maximum number of prompt/input tokens
    #[serde(rename = "max_prompt_tokens", skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<i64>,
    /// Vision-specific limits
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<ModelCapabilitiesOverrideLimitsVision>,
}

/// Feature flags indicating what the model supports
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverrideSupports {
    /// Whether this model supports reasoning effort configuration
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<bool>,
    /// Whether this model supports vision/image input
    #[serde(skip_serializing_if = "Option::is_none")]
    pub vision: Option<bool>,
}

/// Optional capability overrides (vision, tool_calls, reasoning, etc.).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelCapabilitiesOverride {
    /// Token limits for prompts, outputs, and context window
    #[serde(skip_serializing_if = "Option::is_none")]
    pub limits: Option<ModelCapabilitiesOverrideLimits>,
    /// Feature flags indicating what the model supports
    #[serde(skip_serializing_if = "Option::is_none")]
    pub supports: Option<ModelCapabilitiesOverrideSupports>,
}

/// List of Copilot models available to the resolved user, including capabilities and billing metadata.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelList {
    /// List of available models with full metadata
    pub models: Vec<Model>,
}

/// Optional listing options.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelListRequest {
    /// If true, bypasses the per-session model list cache and re-fetches from CAPI.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_cache: Option<bool>,
}

/// Reasoning effort level to apply to the currently selected model.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSetReasoningEffortRequest {
    /// Reasoning effort level to apply to the currently selected model. The host is responsible for validating the value against the model's supported levels before calling.
    pub reasoning_effort: String,
}

/// Update the session's reasoning effort without changing the selected model. Use `switchTo` instead when you also need to change the model. The runtime stores the effort on the session and applies it to subsequent turns.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSetReasoningEffortResult {
    /// Reasoning effort level recorded on the session after the update
    pub reasoning_effort: String,
}

/// Optional GitHub token used to list models for a specific user instead of the global auth context.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelsListRequest {
    /// GitHub token for per-user model listing. When provided, resolves this token to determine the user's Copilot plan and available models instead of using the global auth.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_hub_token: Option<String>,
}

/// Target model identifier and optional reasoning effort, summary, capability overrides, and context tier.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToRequest {
    /// Explicit context tier for the selected model. `"default"` / `"long_context"` apply the requested tier; omit this field to use normal model behavior with no explicit tier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<ContextTier>,
    /// Override individual model capabilities resolved by the runtime
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities: Option<ModelCapabilitiesOverride>,
    /// Model selection id to switch to, as returned by `list`. A bare id (e.g. `claude-sonnet-4.6`) names a Copilot (CAPI) model; a provider-qualified id (`provider/id`, e.g. `acme/claude-sonnet`) targets a registry BYOK model.
    pub model_id: String,
    /// Reasoning effort level to use for the model. "none" disables reasoning.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Reasoning summary mode to request for supported model clients
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summary: Option<ReasoningSummary>,
}

/// The model identifier active on the session after the switch.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelSwitchToResult {
    /// Currently active model identifier after the switch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

/// Agent interaction mode to apply to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModeSetRequest {
    /// The session mode the agent is operating in
    pub mode: SessionMode,
}

/// Azure-specific provider options.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderConfigAzure {
    /// API version. When set, uses the versioned deployment route. When omitted, uses the GA versionless v1 route.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_version: Option<String>,
}

/// A named BYOK provider connection (transport + credentials).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NamedProviderConfig {
    /// API key. Optional for local providers like Ollama.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_key: Option<String>,
    /// Azure-specific provider options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub azure: Option<ProviderConfigAzure>,
    /// API endpoint URL.
    pub base_url: String,
    /// Bearer token for authentication. Sets the Authorization header directly. Takes precedence over apiKey when both are set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub bearer_token: Option<String>,
    /// Custom HTTP headers to include in all outbound requests to the provider.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
    /// Stable identifier referenced by BYOK model definitions. Must not contain '/'.
    pub name: String,
    /// Provider type. Defaults to "openai" for generic OpenAI-compatible APIs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<ProviderConfigType>,
    /// Wire API format (openai/azure only). Defaults to "completions".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_api: Option<ProviderConfigWireApi>,
}

/// The session's friendly name, or null when not yet set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameGetResult {
    /// The session name (user-set or auto-generated), or null if not yet set
    pub name: Option<String>,
}

/// Auto-generated session summary to apply as the session's name when no user-set name exists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameSetAutoRequest {
    /// Auto-generated session summary. Empty/whitespace-only values are ignored; values are trimmed before persisting.
    pub summary: String,
}

/// Indicates whether the auto-generated summary was applied as the session's name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameSetAutoResult {
    /// Whether the auto-generated summary was persisted. False if the session already has a user-set name, the summary normalized to empty, or the session does not have a workspace.
    pub applied: bool,
}

/// New friendly name to apply to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NameSetRequest {
    /// New session name (1–100 characters, trimmed of leading/trailing whitespace)
    pub name: String,
}

/// Schema for the `OptionsUpdateAdditionalContentExclusionPolicyRuleSource` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OptionsUpdateAdditionalContentExclusionPolicyRuleSource {
    pub name: String,
    pub r#type: String,
}

/// Schema for the `OptionsUpdateAdditionalContentExclusionPolicyRule` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OptionsUpdateAdditionalContentExclusionPolicyRule {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub if_any_match: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub if_none_match: Option<Vec<String>>,
    pub paths: Vec<String>,
    /// Schema for the `OptionsUpdateAdditionalContentExclusionPolicyRuleSource` type.
    pub source: OptionsUpdateAdditionalContentExclusionPolicyRuleSource,
}

/// Schema for the `OptionsUpdateAdditionalContentExclusionPolicy` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct OptionsUpdateAdditionalContentExclusionPolicy {
    #[serde(rename = "last_updated_at")]
    pub last_updated_at: serde_json::Value,
    pub rules: Vec<OptionsUpdateAdditionalContentExclusionPolicyRule>,
    /// Allowed values for the `OptionsUpdateAdditionalContentExclusionPolicyScope` enumeration.
    pub scope: OptionsUpdateAdditionalContentExclusionPolicyScope,
}

/// Schema for the `PendingPermissionRequest` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PendingPermissionRequest {
    /// The user-facing permission prompt details (commands, write, read, mcp, url, memory, custom-tool, path, hook)
    pub request: PermissionPromptRequest,
    /// Unique identifier for the pending permission request
    pub request_id: RequestId,
}

/// List of pending permission requests reconstructed from event history.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PendingPermissionRequestList {
    /// Pending permission prompts reconstructed from the session's event history. Equivalent to the set of `permission.requested` events that have not yet been followed by a matching `permission.completed` event. Used by clients (e.g. the CLI) to hydrate UI for prompts that were emitted before the client attached to the session.
    pub items: Vec<PendingPermissionRequest>,
}

/// Schema for the `PermissionDecisionApproveOnce` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveOnce {
    /// Approve this single request only
    pub kind: PermissionDecisionApproveOnceKind,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalCommands` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalCommands {
    /// Command identifiers covered by this approval.
    pub command_identifiers: Vec<String>,
    /// Approval scoped to specific command identifiers.
    pub kind: PermissionDecisionApproveForSessionApprovalCommandsKind,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalRead` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalRead {
    /// Approval covering read-only filesystem operations.
    pub kind: PermissionDecisionApproveForSessionApprovalReadKind,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalWrite` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalWrite {
    /// Approval covering filesystem write operations.
    pub kind: PermissionDecisionApproveForSessionApprovalWriteKind,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalMcp` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalMcp {
    /// Approval covering an MCP tool.
    pub kind: PermissionDecisionApproveForSessionApprovalMcpKind,
    /// MCP server name.
    pub server_name: String,
    /// MCP tool name, or null to cover every tool on the server.
    pub tool_name: Option<String>,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalMcpSampling` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalMcpSampling {
    /// Approval covering MCP sampling requests for a server.
    pub kind: PermissionDecisionApproveForSessionApprovalMcpSamplingKind,
    /// MCP server name.
    pub server_name: String,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalMemory` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalMemory {
    /// Approval covering writes to long-term memory.
    pub kind: PermissionDecisionApproveForSessionApprovalMemoryKind,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalCustomTool` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalCustomTool {
    /// Approval covering a custom tool.
    pub kind: PermissionDecisionApproveForSessionApprovalCustomToolKind,
    /// Custom tool name.
    pub tool_name: String,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalExtensionManagement` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalExtensionManagement {
    /// Approval covering extension lifecycle operations such as enable, disable, or reload.
    pub kind: PermissionDecisionApproveForSessionApprovalExtensionManagementKind,
    /// Optional operation identifier; when omitted, the approval covers all extension management operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub operation: Option<String>,
}

/// Schema for the `PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess {
    /// Extension name.
    pub extension_name: String,
    /// Approval covering an extension's request to access a permission-gated capability.
    pub kind: PermissionDecisionApproveForSessionApprovalExtensionPermissionAccessKind,
}

/// Schema for the `PermissionDecisionApproveForSession` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForSession {
    /// Session-scoped approval to remember (tool prompts only; omitted for path/url prompts)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approval: Option<PermissionDecisionApproveForSessionApproval>,
    /// URL domain to approve for the rest of the session (URL prompts only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub domain: Option<String>,
    /// Approve and remember for the rest of the session
    pub kind: PermissionDecisionApproveForSessionKind,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalCommands` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalCommands {
    /// Command identifiers covered by this approval.
    pub command_identifiers: Vec<String>,
    /// Approval scoped to specific command identifiers.
    pub kind: PermissionDecisionApproveForLocationApprovalCommandsKind,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalRead` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalRead {
    /// Approval covering read-only filesystem operations.
    pub kind: PermissionDecisionApproveForLocationApprovalReadKind,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalWrite` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalWrite {
    /// Approval covering filesystem write operations.
    pub kind: PermissionDecisionApproveForLocationApprovalWriteKind,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalMcp` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalMcp {
    /// Approval covering an MCP tool.
    pub kind: PermissionDecisionApproveForLocationApprovalMcpKind,
    /// MCP server name.
    pub server_name: String,
    /// MCP tool name, or null to cover every tool on the server.
    pub tool_name: Option<String>,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalMcpSampling` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalMcpSampling {
    /// Approval covering MCP sampling requests for a server.
    pub kind: PermissionDecisionApproveForLocationApprovalMcpSamplingKind,
    /// MCP server name.
    pub server_name: String,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalMemory` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalMemory {
    /// Approval covering writes to long-term memory.
    pub kind: PermissionDecisionApproveForLocationApprovalMemoryKind,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalCustomTool` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalCustomTool {
    /// Approval covering a custom tool.
    pub kind: PermissionDecisionApproveForLocationApprovalCustomToolKind,
    /// Custom tool name.
    pub tool_name: String,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalExtensionManagement` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalExtensionManagement {
    /// Approval covering extension lifecycle operations such as enable, disable, or reload.
    pub kind: PermissionDecisionApproveForLocationApprovalExtensionManagementKind,
    /// Optional operation identifier; when omitted, the approval covers all extension management operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub operation: Option<String>,
}

/// Schema for the `PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess {
    /// Extension name.
    pub extension_name: String,
    /// Approval covering an extension's request to access a permission-gated capability.
    pub kind: PermissionDecisionApproveForLocationApprovalExtensionPermissionAccessKind,
}

/// Schema for the `PermissionDecisionApproveForLocation` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproveForLocation {
    /// Approval to persist for this location
    pub approval: PermissionDecisionApproveForLocationApproval,
    /// Approve and persist for this project location
    pub kind: PermissionDecisionApproveForLocationKind,
    /// Location key (git root or cwd) to persist the approval to
    pub location_key: String,
}

/// Schema for the `PermissionDecisionApprovePermanently` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApprovePermanently {
    /// URL domain to approve permanently
    pub domain: String,
    /// Approve and persist across sessions (URL prompts only)
    pub kind: PermissionDecisionApprovePermanentlyKind,
}

/// Schema for the `PermissionDecisionReject` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionReject {
    /// Optional feedback explaining the rejection
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feedback: Option<String>,
    /// Reject the request
    pub kind: PermissionDecisionRejectKind,
}

/// Schema for the `PermissionDecisionUserNotAvailable` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionUserNotAvailable {
    /// No user is available to confirm the request
    pub kind: PermissionDecisionUserNotAvailableKind,
}

/// Schema for the `PermissionDecisionApproved` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApproved {
    /// The permission request was approved
    pub kind: PermissionDecisionApprovedKind,
}

/// Schema for the `PermissionDecisionApprovedForSession` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApprovedForSession {
    /// The approval to add as a session-scoped rule
    pub approval: UserToolSessionApproval,
    /// Approved and remembered for the rest of the session
    pub kind: PermissionDecisionApprovedForSessionKind,
}

/// Schema for the `PermissionDecisionApprovedForLocation` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionApprovedForLocation {
    /// The approval to persist for this location
    pub approval: UserToolSessionApproval,
    /// Approved and persisted for this project location
    pub kind: PermissionDecisionApprovedForLocationKind,
    /// The location key (git root or cwd) to persist the approval to
    pub location_key: String,
}

/// Schema for the `PermissionDecisionCancelled` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionCancelled {
    /// The permission request was cancelled before a response was used
    pub kind: PermissionDecisionCancelledKind,
    /// Optional explanation of why the request was cancelled
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
}

/// Schema for the `PermissionDecisionDeniedByRules` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionDeniedByRules {
    /// Denied because approval rules explicitly blocked it
    pub kind: PermissionDecisionDeniedByRulesKind,
    /// Rules that denied the request
    pub rules: Vec<PermissionRule>,
}

/// Schema for the `PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser {
    /// Denied because no approval rule matched and user confirmation was unavailable
    pub kind: PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind,
}

/// Schema for the `PermissionDecisionDeniedInteractivelyByUser` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionDeniedInteractivelyByUser {
    /// Optional feedback from the user explaining the denial
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feedback: Option<String>,
    /// Whether to force-reject the current agent turn
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force_reject: Option<bool>,
    /// Denied by the user during an interactive prompt
    pub kind: PermissionDecisionDeniedInteractivelyByUserKind,
}

/// Schema for the `PermissionDecisionDeniedByContentExclusionPolicy` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionDeniedByContentExclusionPolicy {
    /// Denied by the organization's content exclusion policy
    pub kind: PermissionDecisionDeniedByContentExclusionPolicyKind,
    /// Human-readable explanation of why the path was excluded
    pub message: String,
    /// File path that triggered the exclusion
    pub path: String,
}

/// Schema for the `PermissionDecisionDeniedByPermissionRequestHook` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionDeniedByPermissionRequestHook {
    /// Whether to interrupt the current agent turn
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interrupt: Option<bool>,
    /// Denied by a permission request hook registered by an extension or plugin
    pub kind: PermissionDecisionDeniedByPermissionRequestHookKind,
    /// Optional message from the hook explaining the denial
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

/// Pending permission request ID and the decision to apply (approve/reject and scope).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionDecisionRequest {
    /// Request ID of the pending permission request
    pub request_id: RequestId,
    /// The client's response to the pending permission prompt
    pub result: PermissionDecision,
}

/// Schema for the `PermissionsLocationsAddToolApprovalDetailsCommands` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsCommands {
    /// Command identifiers covered by this approval.
    pub command_identifiers: Vec<String>,
    /// Approval scoped to specific command identifiers.
    pub kind: PermissionsLocationsAddToolApprovalDetailsCommandsKind,
}

/// Schema for the `PermissionsLocationsAddToolApprovalDetailsRead` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsRead {
    /// Approval covering read-only filesystem operations.
    pub kind: PermissionsLocationsAddToolApprovalDetailsReadKind,
}

/// Schema for the `PermissionsLocationsAddToolApprovalDetailsWrite` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsWrite {
    /// Approval covering filesystem write operations.
    pub kind: PermissionsLocationsAddToolApprovalDetailsWriteKind,
}

/// Schema for the `PermissionsLocationsAddToolApprovalDetailsMcp` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsMcp {
    /// Approval covering an MCP tool.
    pub kind: PermissionsLocationsAddToolApprovalDetailsMcpKind,
    /// MCP server name.
    pub server_name: String,
    /// MCP tool name, or null to cover every tool on the server.
    pub tool_name: Option<String>,
}

/// Schema for the `PermissionsLocationsAddToolApprovalDetailsMcpSampling` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsMcpSampling {
    /// Approval covering MCP sampling requests for a server.
    pub kind: PermissionsLocationsAddToolApprovalDetailsMcpSamplingKind,
    /// MCP server name.
    pub server_name: String,
}

/// Schema for the `PermissionsLocationsAddToolApprovalDetailsMemory` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsMemory {
    /// Approval covering writes to long-term memory.
    pub kind: PermissionsLocationsAddToolApprovalDetailsMemoryKind,
}

/// Schema for the `PermissionsLocationsAddToolApprovalDetailsCustomTool` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsCustomTool {
    /// Approval covering a custom tool.
    pub kind: PermissionsLocationsAddToolApprovalDetailsCustomToolKind,
    /// Custom tool name.
    pub tool_name: String,
}

/// Schema for the `PermissionsLocationsAddToolApprovalDetailsExtensionManagement` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsExtensionManagement {
    /// Approval covering extension lifecycle operations such as enable, disable, or reload.
    pub kind: PermissionsLocationsAddToolApprovalDetailsExtensionManagementKind,
    /// Optional operation identifier; when omitted, the approval covers all extension management operations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub operation: Option<String>,
}

/// Schema for the `PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess {
    /// Extension name.
    pub extension_name: String,
    /// Approval covering an extension's request to access a permission-gated capability.
    pub kind: PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccessKind,
}

/// Location-scoped tool approval to persist.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionLocationAddToolApprovalParams {
    /// Tool approval to persist and apply
    pub approval: PermissionsLocationsAddToolApprovalDetails,
    /// Location key (git root or cwd) to persist the approval to
    pub location_key: String,
}

/// Working directory to load persisted location permissions for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionLocationApplyParams {
    /// Working directory whose persisted location permissions should be applied
    pub working_directory: String,
}

/// Summary of persisted location permissions applied to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionLocationApplyResult {
    /// Number of persisted allowed directories added to the live path manager
    pub applied_directory_count: i64,
    /// Number of location-scoped rules added to the live permission service
    pub applied_rule_count: i64,
    /// Location-scoped rules applied to the live permission service
    pub applied_rules: Vec<PermissionRule>,
    /// Whether a different location was applied since the previous apply call
    pub changed: bool,
    /// Location key used in the location-permissions store
    pub location_key: String,
    /// Whether the location is a git repo or directory
    pub location_type: PermissionLocationType,
}

/// Working directory to resolve into a location-permissions key.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionLocationResolveParams {
    /// Working directory whose permission location should be resolved
    pub working_directory: String,
}

/// Resolved location-permissions key and type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionLocationResolveResult {
    /// Location key used in the location-permissions store
    pub location_key: String,
    /// Whether the location is a git repo or directory
    pub location_type: PermissionLocationType,
}

/// Directory path to add to the session's allowed directories.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsAddParams {
    /// Directory to add to the allow-list. The runtime resolves and validates the path before adding.
    pub path: String,
}

/// Path to evaluate against the session's allowed directories.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsAllowedCheckParams {
    /// Path to check against the session's allowed directories
    pub path: String,
}

/// Indicates whether the supplied path is within the session's allowed directories.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsAllowedCheckResult {
    /// Whether the path is within the session's allowed directories
    pub allowed: bool,
}

/// If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsConfig {
    /// Additional directories to allow tool access to (in addition to the session's working directory). When `unrestricted` is true, these are still pre-populated on the UnrestrictedPathManager so they remain visible via getDirectories() (e.g. for @-mention completion).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_directories: Option<Vec<String>>,
    /// Whether to include the system temp directory in the allowed list (defaults to true). Ignored when `unrestricted` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_temp_directory: Option<bool>,
    /// If true, the runtime allows access to all paths without prompting. Equivalent to constructing an UnrestrictedPathManager.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unrestricted: Option<bool>,
    /// Workspace root path (special-cased to be allowed even before the directory exists). Ignored when `unrestricted` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub workspace_path: Option<String>,
}

/// Snapshot of the session's allow-listed directories and primary working directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsList {
    /// All directories currently allowed for tool access on this session.
    pub directories: Vec<String>,
    /// The primary working directory for this session.
    pub primary: String,
}

/// Directory path to set as the session's new primary working directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsUpdatePrimaryParams {
    /// Directory to set as the new primary working directory for the session's permission policy.
    pub path: String,
}

/// Path to evaluate against the session's workspace (primary) directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsWorkspaceCheckParams {
    /// Path to check against the session workspace directory
    pub path: String,
}

/// Indicates whether the supplied path is within the session's workspace directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPathsWorkspaceCheckResult {
    /// Whether the path is within the session workspace directory
    pub allowed: bool,
}

/// Notification payload describing the permission prompt that the client just rendered.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionPromptShownNotification {
    /// Human-readable description of the prompt the user is being asked to approve. Used by the runtime to fire the registered `permission_prompt` notification hook (e.g. terminal bell, desktop notification).
    pub message: String,
}

/// Indicates whether the permission decision was applied; false when the request was already resolved.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRequestResult {
    /// Whether the permission request was handled successfully
    pub success: bool,
}

/// If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionRulesSet {
    /// Rules that auto-approve matching requests
    pub approved: Vec<PermissionRule>,
    /// Rules that auto-deny matching requests
    pub denied: Vec<PermissionRule>,
}

/// Schema for the `PermissionsConfigureAdditionalContentExclusionPolicyRuleSource` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsConfigureAdditionalContentExclusionPolicyRuleSource {
    pub name: String,
    pub r#type: String,
}

/// Schema for the `PermissionsConfigureAdditionalContentExclusionPolicyRule` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsConfigureAdditionalContentExclusionPolicyRule {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub if_any_match: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub if_none_match: Option<Vec<String>>,
    pub paths: Vec<String>,
    /// Schema for the `PermissionsConfigureAdditionalContentExclusionPolicyRuleSource` type.
    pub source: PermissionsConfigureAdditionalContentExclusionPolicyRuleSource,
}

/// Schema for the `PermissionsConfigureAdditionalContentExclusionPolicy` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsConfigureAdditionalContentExclusionPolicy {
    #[serde(rename = "last_updated_at")]
    pub last_updated_at: serde_json::Value,
    pub rules: Vec<PermissionsConfigureAdditionalContentExclusionPolicyRule>,
    /// Allowed values for the `PermissionsConfigureAdditionalContentExclusionPolicyScope` enumeration.
    pub scope: PermissionsConfigureAdditionalContentExclusionPolicyScope,
}

/// If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionUrlsConfig {
    /// Initial list of allowed URL/domain patterns. Patterns may include path components. Ignored when `unrestricted` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initial_allowed: Option<Vec<String>>,
    /// If true, the runtime allows access to all URLs without prompting. Initial allow-list is ignored when this is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unrestricted: Option<bool>,
}

/// Patch of permission policy fields to apply (omit a field to leave it unchanged).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsConfigureParams {
    /// If specified, replaces the host-supplied GitHub Content Exclusion policies on the session (combined with natively-discovered policies when evaluating tool/file access). Omit to leave the current policies unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_content_exclusion_policies:
        Option<Vec<PermissionsConfigureAdditionalContentExclusionPolicy>>,
    /// If specified, sets whether path/URL read permission requests are auto-approved. Omit to leave the current value unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approve_all_read_permission_requests: Option<bool>,
    /// If specified, sets whether tool permission requests are auto-approved without prompting. Omit to leave the current value unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub approve_all_tool_permission_requests: Option<bool>,
    /// If specified, replaces the session's path-permission policy. The runtime constructs the appropriate PathManager based on these inputs (rooted at the session's working directory). Omit to leave the current path policy unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub paths: Option<PermissionPathsConfig>,
    /// If specified, replaces the session's approved/denied permission rules. Omit to leave the current rules unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub rules: Option<PermissionRulesSet>,
    /// If specified, replaces the session's URL-permission policy. The runtime constructs a fresh DefaultUrlManager based on these inputs. Omit to leave the current URL policy unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub urls: Option<PermissionUrlsConfig>,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsConfigureResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsFolderTrustAddTrustedResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// No parameters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsGetAllowAllRequest {}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsLocationsAddToolApprovalResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Scope and add/remove instructions for modifying session- or location-scoped permission rules.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsModifyRulesParams {
    /// Rules to add to the scope. Applied before `remove`/`removeAll`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub add: Option<Vec<PermissionRule>>,
    /// Specific rules to remove from the scope. Ignored when `removeAll` is true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remove: Option<Vec<PermissionRule>>,
    /// When true, removes every rule currently in the scope (after any `add` is applied). Useful for clearing the location scope wholesale.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remove_all: Option<bool>,
    /// Whether the change applies to ephemeral session-scoped rules (cleared at session end) or to location-scoped rules persisted via the location-permissions config file.
    pub scope: PermissionsModifyRulesScope,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsModifyRulesResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsNotifyPromptShownResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsPathsAddResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// No parameters; returns the session's allow-listed directories.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsPathsListRequest {}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsPathsUpdatePrimaryResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// No parameters; returns currently-pending permission requests for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsPendingRequestsRequest {}

/// No parameters; clears all session-scoped tool permission approvals.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsResetSessionApprovalsRequest {}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsResetSessionApprovalsResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Whether to enable full allow-all permissions for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetAllowAllRequest {
    /// Whether to enable full allow-all permissions
    pub enabled: bool,
    /// Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<PermissionsSetAllowAllSource>,
}

/// Allow-all toggle for tool permission requests, with an optional telemetry source.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetApproveAllRequest {
    /// Whether to auto-approve all tool permission requests
    pub enabled: bool,
    /// Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<PermissionsSetApproveAllSource>,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetApproveAllResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Toggles whether permission prompts should be bridged into session events for this client.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetRequiredRequest {
    /// Whether the client wants `permission.requested` events bridged from the session-owned permission service. CLI clients that render prompt UI set this to `true` for as long as their listener is mounted; headless callers leave it unset (the default is `false`).
    pub required: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsSetRequiredResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionsUrlsSetUnrestrictedModeResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Whether the URL-permission policy should run in unrestricted mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PermissionUrlsSetUnrestrictedModeParams {
    /// Whether to allow access to all URLs without prompting. Toggles the runtime's URL-permission policy in place.
    pub enabled: bool,
}

/// Optional message to echo back to the caller.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingRequest {
    /// Optional message to echo back
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

/// Server liveness response, including the echoed message, current server timestamp, and protocol version.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PingResult {
    /// Echoed message (or default greeting)
    pub message: String,
    /// Server protocol version number
    pub protocol_version: i64,
    /// ISO 8601 timestamp when the server handled the ping
    pub timestamp: String,
}

/// Existence, contents, and resolved path of the session plan file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanReadResult {
    /// The content of the plan file, or null if it does not exist
    pub content: Option<String>,
    /// Whether the plan file exists in the workspace
    pub exists: bool,
    /// Absolute file path of the plan file, or null if workspace is not enabled
    pub path: Option<String>,
}

/// A single todo row read from the session SQL `todos` table. All fields are optional because the SQL schema is best-effort and the agent may not have populated every column.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanSqlTodosRow {
    /// Todo description.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Todo identifier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<String>,
    /// Todo status.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// Todo title.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
}

/// Todo rows read from the session SQL database. Empty when no session database is available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanReadSqlTodosResult {
    /// Rows from the session SQL todos table, ordered by creation time and id.
    pub rows: Vec<PlanSqlTodosRow>,
}

/// A single dependency edge read from the session SQL `todo_deps` table, indicating that one todo must complete before another.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanSqlTodoDependency {
    /// ID of the todo it depends on.
    pub depends_on: String,
    /// ID of the todo that has the dependency.
    pub todo_id: String,
}

/// Todo rows + dependency edges read from the session SQL database.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanReadSqlTodosWithDependenciesResult {
    /// Edges from the session SQL todo_deps table. Empty when no database, no todo_deps table, or the SELECT failed. Read independently from `rows`, so a broken todo_deps table does not affect the rows result and vice versa.
    pub dependencies: Vec<PlanSqlTodoDependency>,
    /// Rows from the session SQL todos table, ordered by creation time and id. Empty when no database, no todos table, or the SELECT failed.
    pub rows: Vec<PlanSqlTodosRow>,
}

/// Replacement contents to write to the session plan file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanUpdateRequest {
    /// The new content for the plan file
    pub content: String,
}

/// Schema for the `Plugin` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Plugin {
    /// Whether the plugin is currently enabled
    pub enabled: bool,
    /// Marketplace the plugin came from
    pub marketplace: String,
    /// Plugin name
    pub name: String,
    /// Installed version
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Result of installing a plugin.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginInstallResult {
    /// Set when the install path is deprecated (e.g. direct repo / URL / local installs). Callers should surface this to end users.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deprecation_warning: Option<String>,
    /// The newly installed plugin's metadata
    pub plugin: InstalledPluginInfo,
    /// Optional post-install message provided by the plugin (e.g. setup instructions)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub post_install_message: Option<String>,
    /// Number of skills discovered and installed from the plugin
    pub skills_installed: i64,
}

/// Plugins installed for the session, with their enabled state and version metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginList {
    /// Installed plugins
    pub plugins: Vec<Plugin>,
}

/// Plugins installed in user/global state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginListResult {
    /// Installed plugins
    pub plugins: Vec<InstalledPluginInfo>,
}

/// Plugin names (or specs) to disable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsDisableRequest {
    /// Plugin names or "plugin@marketplace" specs to disable. Unknown names are ignored. Non-marketplace direct installs cannot be disabled via this API; uninstall them instead. Plugin-owned MCP servers are stopped in active sessions immediately; other plugin contributions remain available until each session reloads plugins.
    pub names: Vec<String>,
}

/// Plugin names (or specs) to enable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsEnableRequest {
    /// Plugin names or "plugin@marketplace" specs to enable. Unknown names are ignored. Non-marketplace direct installs are always enabled and cannot be toggled via this API.
    pub names: Vec<String>,
}

/// Plugin source and optional working directory for relative-path resolution.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsInstallRequest {
    /// Plugin install spec. Accepts the same forms as the CLI: "plugin@marketplace" (marketplace install), "owner/repo" or "owner/repo:subpath" (GitHub direct), an http/https/ssh URL, or a local path. Direct (non-marketplace) installs are deprecated and will produce a deprecationWarning in the result.
    pub source: String,
    /// Working directory used to resolve relative local paths in `source`. Defaults to the server's current working directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// Marketplace source to register.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsMarketplacesAddRequest {
    /// Marketplace source. Accepts the same forms as the CLI: "owner/repo" or "owner/repo#ref" (GitHub), an http/https/ssh URL (optionally with #ref), a git scp-style URL (user@host:path), or a local path. The marketplace's own name (from its manifest) is used as the registration key.
    pub source: String,
}

/// Name of the marketplace whose plugin catalog to fetch.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsMarketplacesBrowseRequest {
    /// Marketplace name to browse
    pub name: String,
}

/// Optional marketplace name; omit to refresh all.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsMarketplacesRefreshRequest {
    /// Marketplace name to refresh. When omitted, every registered marketplace is refreshed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
}

/// Name of the marketplace to remove and an optional force flag.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsMarketplacesRemoveRequest {
    /// When true, also uninstall every plugin sourced from this marketplace. When false (default), removal is a no-op if any plugin from this marketplace is installed and the dependent plugin names are returned in the result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force: Option<bool>,
    /// Marketplace name to remove
    pub name: String,
}

/// Optional flags controlling which side effects the reload performs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsReloadRequest {
    /// When true, skip repo-level hooks during the hook reload. Use before folder trust is confirmed; load them post-trust via `sessions.loadDeferredRepoHooks`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_repo_hooks: Option<bool>,
    /// Re-run custom-agent discovery after refreshing plugins. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reload_custom_agents: Option<bool>,
    /// Re-load user, plugin, and (subject to `deferRepoHooks`) repo hooks. Defaults to true. Has no effect when the host has not registered a hook reloader (e.g. remote sessions).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reload_hooks: Option<bool>,
    /// Reload MCP server connections after refreshing plugins. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reload_mcp: Option<bool>,
}

/// Name (or spec) of the plugin to uninstall.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsUninstallRequest {
    /// Plugin name or "plugin@marketplace" spec to uninstall. When ambiguous, prefer the fully-qualified spec.
    pub name: String,
}

/// Name (or spec) of the plugin to update.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsUpdateRequest {
    /// Plugin name or "plugin@marketplace" spec to update.
    pub name: String,
}

/// Schema for the `PluginUpdateAllEntry` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginUpdateAllEntry {
    /// Error message (failure only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Marketplace the plugin came from. Empty string ("") for direct installs.
    pub marketplace: String,
    /// Plugin name that was updated
    pub name: String,
    /// Version after the update, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_version: Option<String>,
    /// Previously installed version, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_version: Option<String>,
    /// Number of skills installed after the update (success only)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skills_installed: Option<i64>,
    /// Whether the update succeeded for this plugin
    pub success: bool,
}

/// Result of updating all installed plugins.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginUpdateAllResult {
    /// Per-plugin update results in deterministic order.
    pub results: Vec<PluginUpdateAllEntry>,
}

/// Result of updating a single plugin.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginUpdateResult {
    /// Version after the update, when reported by the plugin manifest
    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_version: Option<String>,
    /// Version that was previously installed, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_version: Option<String>,
    /// Number of skills discovered and installed after the update
    pub skills_installed: i64,
}

/// Schema for the `SessionsPollSpawnedSessionsEvent` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsPollSpawnedSessionsEvent {
    /// Session id of the newly-spawned session.
    pub session_id: SessionId,
}

/// Batch of spawn events plus a cursor for follow-up polls.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PollSpawnedSessionsResult {
    /// Opaque cursor to pass back to receive only events after this batch.
    pub cursor: String,
    /// Spawn events emitted since the supplied cursor.
    pub events: Vec<SessionsPollSpawnedSessionsEvent>,
}

/// Custom model-provider configuration (BYOK).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderConfig {
    /// API key. Optional for local providers like Ollama.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_key: Option<String>,
    /// Azure-specific provider options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub azure: Option<ProviderConfigAzure>,
    /// API endpoint URL.
    pub base_url: String,
    /// Bearer token for authentication. Sets the Authorization header directly. Takes precedence over apiKey when both are set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub bearer_token: Option<String>,
    /// Custom HTTP headers to include in all outbound requests to the provider.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub headers: Option<HashMap<String, String>>,
    /// Maximum context window tokens for the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_context_window_tokens: Option<f64>,
    /// Maximum output tokens for the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<f64>,
    /// Maximum prompt/input tokens for the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<f64>,
    /// Well-known model ID used for capability lookup. When set, agent behavior config and token limits are inferred from this model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
    /// Provider type. Defaults to "openai" for generic OpenAI-compatible APIs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<ProviderConfigType>,
    /// Wire API format (openai/azure only). Defaults to "completions".
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_api: Option<ProviderConfigWireApi>,
    /// The model identifier sent to the provider API for inference (the "wire" model), as opposed to modelId which is the well-known base.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_model: Option<String>,
}

/// Short-lived, rotating credential the caller must send on every request, in addition to `apiKey` if one is present. Omitted when the endpoint does not require one.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderSessionToken {
    /// When the token expires, if known. Callers should refresh by calling `getEndpoint` again before this time, or reactively on any 401/403 response from `baseUrl`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expires_at: Option<String>,
    /// HTTP header name the token must be sent under.
    pub header: String,
    /// The model the token is bound to, when applicable. When set, the token is only valid for requests against this model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// The short-lived token value.
    pub token: String,
}

/// A snapshot of the provider endpoint the session is currently configured to talk to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderEndpoint {
    /// A credential the caller should use with this endpoint. Omitted only when the endpoint accepts unauthenticated requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_key: Option<String>,
    /// Base URL to pass to the LLM client library.
    pub base_url: String,
    /// HTTP headers the caller must include on every outbound request.
    pub headers: HashMap<String, String>,
    /// Short-lived, rotating credential the caller must send on every request, in addition to `apiKey` if one is present. Omitted when the endpoint does not require one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_token: Option<ProviderSessionToken>,
    /// Provider family. Matches the `type` field of a BYOK provider config.
    pub r#type: ProviderEndpointType,
    /// Wire API to be used, when required for the provider type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_api: Option<ProviderEndpointWireApi>,
}

/// Optional model identifier to scope the endpoint snapshot to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderGetEndpointRequest {
    /// Model identifier the caller intends to use against the returned endpoint. Used to pick the correct wire shape. Omit to use whichever model the session is currently using.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

/// A BYOK model definition referencing a named provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ProviderModelConfig {
    /// Optional capability overrides (vision, tool_calls, reasoning, etc.).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub capabilities: Option<ModelCapabilitiesOverride>,
    /// Provider-local model id, unique within its provider. The session-wide selection id (shown in the model list and passed to switchTo) is the provider-qualified `provider/id`.
    pub id: String,
    /// Maximum context window tokens for the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_context_window_tokens: Option<f64>,
    /// Maximum output tokens for the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_output_tokens: Option<f64>,
    /// Maximum prompt/input tokens for the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_prompt_tokens: Option<f64>,
    /// Well-known base model id used for behavior/capability/config lookup. Defaults to `id`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
    /// Display name for model pickers. Defaults to the provider-qualified selection id (`provider/id`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Name of the NamedProviderConfig that serves this model.
    pub provider: String,
    /// The model name sent to the provider API for inference. Defaults to `id`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_model: Option<String>,
}

/// Blob attachment with inline base64-encoded data
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentBlob {
    /// Base64-encoded content
    pub data: String,
    /// User-facing display name for the attachment
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    /// MIME type of the inline data
    pub mime_type: String,
    /// Attachment type discriminator
    pub r#type: PushAttachmentBlobType,
}

/// Directory attachment
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentDirectory {
    /// User-facing display name for the attachment
    pub display_name: String,
    /// Absolute directory path
    pub path: String,
    /// Attachment type discriminator
    pub r#type: PushAttachmentDirectoryType,
}

/// Optional line range to scope the attachment to a specific section of the file
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentFileLineRange {
    /// End line number (1-based, inclusive)
    pub end: i64,
    /// Start line number (1-based)
    pub start: i64,
}

/// File attachment
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentFile {
    /// User-facing display name for the attachment
    pub display_name: String,
    /// Optional line range to scope the attachment to a specific section of the file
    #[serde(skip_serializing_if = "Option::is_none")]
    pub line_range: Option<PushAttachmentFileLineRange>,
    /// Absolute file path
    pub path: String,
    /// Attachment type discriminator
    pub r#type: PushAttachmentFileType,
}

/// GitHub issue, pull request, or discussion reference
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentGitHubReference {
    /// Issue, pull request, or discussion number
    pub number: i64,
    /// Type of GitHub reference
    pub reference_type: PushAttachmentGitHubReferenceType,
    /// Current state of the referenced item (e.g., open, closed, merged)
    pub state: String,
    /// Title of the referenced item
    pub title: String,
    /// Attachment type discriminator
    pub r#type: PushAttachmentGitHubReferenceType,
    /// URL to the referenced item on GitHub
    pub url: String,
}

/// End position of the selection
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentSelectionDetailsEnd {
    /// End character offset within the line (0-based)
    pub character: i64,
    /// End line number (0-based)
    pub line: i64,
}

/// Start position of the selection
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentSelectionDetailsStart {
    /// Start character offset within the line (0-based)
    pub character: i64,
    /// Start line number (0-based)
    pub line: i64,
}

/// Position range of the selection within the file
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentSelectionDetails {
    /// End position of the selection
    pub end: PushAttachmentSelectionDetailsEnd,
    /// Start position of the selection
    pub start: PushAttachmentSelectionDetailsStart,
}

/// Code selection attachment from an editor
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushAttachmentSelection {
    /// User-facing display name for the selection
    pub display_name: String,
    /// Absolute path to the file containing the selection
    pub file_path: String,
    /// Position range of the selection within the file
    pub selection: PushAttachmentSelectionDetails,
    /// The selected text content
    pub text: String,
    /// Attachment type discriminator
    pub r#type: PushAttachmentSelectionType,
}

/// Schema for the `QueuedCommandHandled` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueuedCommandHandled {
    /// The host actually executed the queued command.
    pub handled: bool,
    /// When true, the runtime will not process subsequent queued commands until a new request comes in.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stop_processing_queue: Option<bool>,
}

/// Schema for the `QueuedCommandNotHandled` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueuedCommandNotHandled {
    /// The host did not execute the queued command. Unblocks the queue without claiming the command was processed (e.g. when the handler threw before completing).
    pub handled: bool,
}

/// Schema for the `QueuePendingItems` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueuePendingItems {
    /// Human-readable text to display for this queue entry in the UI
    pub display_text: String,
    /// Whether this item is a queued user message or a queued slash command / model change
    pub kind: QueuePendingItemsKind,
}

/// Snapshot of the session's pending queued items and immediate-steering messages.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueuePendingItemsResult {
    /// Pending queued items in submission order. Includes user messages, queued slash commands, and queued model changes; omits internal system items.
    pub items: Vec<QueuePendingItems>,
    /// Display text for messages currently in the immediate steering queue (interjections sent during a running turn).
    pub steering_messages: Vec<String>,
}

/// Indicates whether a user-facing pending item was removed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct QueueRemoveMostRecentResult {
    /// True if a user-facing pending item was removed (LIFO across both queues); false when no removable items remained.
    pub removed: bool,
}

/// Event type to register consumer interest for, used by runtime gating logic.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RegisterEventInterestParams {
    /// The event type the consumer wants the runtime to treat as 'observed' for behavior-switching gating. Some runtime code paths inspect whether any consumer is interested in a specific event type and choose a different implementation accordingly (e.g. `mcp.oauth_required`: when interest is registered the runtime delegates the full interactive OAuth flow to the consumer; when no interest is registered the runtime installs a browserless fallback that silently reuses cached tokens). SDK clients that long-poll events do NOT automatically appear as listeners to these gating checks — they must explicitly call `registerInterest` for each event type they want the runtime to count as having a consumer. Multiple registrations for the same event type from the same or different consumers are tracked independently and must each be released. See: `mcp.oauth_required`, `sampling.requested`, `auto_mode_switch.requested`, `user_input.requested`, `elicitation.requested`, `command.queued`, `exit_plan_mode.requested`.
    pub event_type: String,
}

/// Opaque handle representing an event-type interest registration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RegisterEventInterestResult {
    /// Opaque handle for this registration. Pass to releaseInterest to release. Each call to registerInterest produces a fresh handle, even when the same eventType is registered multiple times.
    pub handle: String,
}

/// Optional registration options.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsRegisterExtensionToolsOnSessionOptions {
    /// In-process `() => boolean` gating callback (CLI-only optimization). Marked internal: replaced by runtime-side enable/disable RPCs in the SDK migration.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) enabled: Option<serde_json::Value>,
}

/// Params to attach an extension loader's tools to a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RegisterExtensionToolsParams {
    /// In-process ExtensionLoader handle (CLI-only optimization). Marked internal: this field is excluded from the public SDK surface. When the CLI migrates to a process-separated SDK, extension discovery/launch moves entirely into the runtime — the CLI passes pure config (search paths, disabled ids) via SessionOptions instead.
    #[doc(hidden)]
    pub(crate) loader: serde_json::Value,
    /// Optional registration options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<SessionsRegisterExtensionToolsOnSessionOptions>,
    /// Session to register extension tools on.
    pub session_id: SessionId,
}

/// Handle for releasing the extension tool registration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct RegisterExtensionToolsResult {
    /// In-process unsubscribe function (CLI-only optimization). Marked internal: replaced by an explicit `extensions.unregister` RPC in the SDK migration.
    #[doc(hidden)]
    pub(crate) unsubscribe: serde_json::Value,
}

/// Opaque handle previously returned by `registerInterest` to release.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ReleaseEventInterestParams {
    /// Handle returned by a previous `registerInterest` call. Idempotent: releasing an unknown or already-released handle is a no-op (returns success). When the last outstanding handle for an event type is released, the runtime reverts to its 'no consumer' code path for that event type.
    pub handle: String,
}

/// Reattach to an existing MC session without creating a new one.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlConfigExistingMcSession {
    /// Existing MC session ID to reattach to.
    pub mc_session_id: String,
    /// Existing MC task ID for the reattached session.
    pub mc_task_id: String,
}

/// Configuration for the runtime-managed remote-control singleton.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlConfig {
    /// Reattach to an existing MC session without creating a new one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub existing_mc_session: Option<RemoteControlConfigExistingMcSession>,
    /// Whether the user explicitly requested remote (vs. implicit session-sync). Controls warning surfacing for missing-repo cases.
    pub explicit: bool,
    /// Whether remote export should be enabled.
    pub remote: bool,
    /// When true, suppresses timeline messages on successful setup.
    pub silent: bool,
    /// Whether the MC session may steer the local session (write mode).
    pub steerable: bool,
    /// Existing Mission Control task ID to attach the exported session to.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task_id: Option<String>,
}

/// Remote control is connected to a local session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlStatusActive {
    /// Session id remote control is pointed at.
    pub attached_session_id: String,
    /// MC frontend URL for this session, when known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub frontend_url: Option<String>,
    /// Whether the MC session may steer this session.
    pub is_steerable: bool,
    /// In-process prompt-manager handle (CLI-only optimization). Marked internal: this field is excluded from the public SDK surface. When the CLI migrates to a process-separated SDK, the same bidirectional prompt-routing handshake is expressed via dedicated remote-control RPCs (register/resolve) rather than a shared in-process object.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) prompt_manager: Option<serde_json::Value>,
    /// Remote control state tag: active.
    pub state: RemoteControlStatusActiveState,
}

/// Remote control is in the middle of initial setup.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlStatusConnecting {
    /// Session id the connection is attaching to.
    pub attached_session_id: String,
    /// Remote control state tag: connecting.
    pub state: RemoteControlStatusConnectingState,
}

/// The last setup attempt failed. The singleton is otherwise off.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlStatusError {
    /// Session id the failing setup attempt targeted, when known.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attached_session_id: Option<String>,
    /// Human-readable error message from the last setup attempt.
    pub error: String,
    /// Remote control state tag: setup failed.
    pub state: RemoteControlStatusErrorState,
}

/// Remote control is not connected.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlStatusOff {
    /// Remote control state tag: not connected.
    pub state: RemoteControlStatusOffState,
}

/// Wrapper for the singleton's current status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlStatusResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
}

/// Outcome of a stopRemoteControl call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlStopResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
    /// Whether the singleton was actually torn down by this call.
    pub stopped: bool,
}

/// Outcome of a transferRemoteControl call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteControlTransferResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
    /// Whether the rebinding actually happened.
    pub transferred: bool,
}

/// Optional remote session mode ("off", "export", or "on"); defaults to enabling both export and remote steering.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteEnableRequest {
    /// Per-session remote mode. "off" disables remote, "export" exports session events to GitHub without enabling remote steering, "on" enables both export and remote steering.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<RemoteSessionMode>,
}

/// GitHub URL for the session and a flag indicating whether remote steering is enabled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteEnableResult {
    /// Whether remote steering is enabled
    pub remote_steerable: bool,
    /// GitHub frontend URL for this session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// New remote-steerability state to persist as a `session.remote_steerable_changed` event.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteNotifySteerableChangedRequest {
    /// Whether the session now supports remote steering via GitHub. The runtime persists this as a `session.remote_steerable_changed` event so resume/replay sees the up-to-date capability.
    pub remote_steerable: bool,
}

/// Persist a steerability change as a `session.remote_steerable_changed` event. Used by the host (CLI / SDK consumer) when it has just finished enabling or disabling steering on a remote exporter that the runtime does not directly own.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteNotifySteerableChangedResult {}

/// Remote session connection result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteSessionConnectionResult {
    /// Metadata for a connected remote session.
    pub metadata: ConnectedRemoteSessionMetadata,
    /// SDK session ID for the connected remote session.
    pub session_id: SessionId,
}

/// GitHub repository the remote session belongs to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteSessionMetadataRepository {
    /// Branch associated with the remote session.
    pub branch: String,
    /// Repository name.
    pub name: String,
    /// Repository owner.
    pub owner: String,
}

/// Remote session metadata for the session to hand off (typically obtained from `sessions.list` with `source: "remote"`).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteSessionMetadataValue {
    /// Most recent working directory context.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<SessionContext>,
    /// Always true for remote sessions.
    pub is_remote: bool,
    /// Last-modified time as an ISO 8601 timestamp.
    pub modified_time: String,
    /// Optional human-friendly name set via /rename.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Pull request number associated with the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pull_request_number: Option<i64>,
    /// Backing remote session IDs (most recent first).
    pub remote_session_ids: Vec<String>,
    /// GitHub repository the remote session belongs to.
    pub repository: RemoteSessionMetadataRepository,
    /// Original remote resource identifier (task ID or PR node ID).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resource_id: Option<String>,
    /// Stable session identifier.
    pub session_id: SessionId,
    /// Deadline (ISO 8601) at which a CLI remote session becomes stale without further heartbeats.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub stale_at: Option<String>,
    /// Session creation time as an ISO 8601 timestamp.
    pub start_time: String,
    /// Server-side task state returned by GitHub.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub state: Option<String>,
    /// Short summary of the session, when one has been derived.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    /// Whether the remote task originated from CCA or CLI `--remote`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task_type: Option<RemoteSessionMetadataTaskType>,
}

/// Repository context for the remote session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct RemoteSessionRepository {
    /// Optional branch associated with the remote session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Repository name.
    pub name: String,
    /// Repository owner or organization login.
    pub owner: String,
}

/// macOS seatbelt experimental options.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfigUserPolicyExperimentalSeatbelt {
    /// Whether the macOS seatbelt profile may access the keychain.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub keychain_access: Option<bool>,
}

/// Platform-specific experimental policy fields.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfigUserPolicyExperimental {
    /// macOS seatbelt experimental options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub seatbelt: Option<SandboxConfigUserPolicyExperimentalSeatbelt>,
}

/// Filesystem rules to merge into the base policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfigUserPolicyFilesystem {
    /// Whether to clear the policy when the session exits.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub clear_policy_on_exit: Option<bool>,
    /// Paths explicitly denied.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub denied_paths: Option<Vec<String>>,
    /// Paths granted read-only access.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub readonly_paths: Option<Vec<String>>,
    /// Paths granted read/write access.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub readwrite_paths: Option<Vec<String>>,
}

/// Network rules to merge into the base policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfigUserPolicyNetwork {
    /// Hosts allowed in addition to the base policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allowed_hosts: Option<Vec<String>>,
    /// Whether traffic to local/loopback addresses is allowed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow_local_network: Option<bool>,
    /// Whether outbound network traffic is allowed at all.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allow_outbound: Option<bool>,
    /// Hosts explicitly blocked.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub blocked_hosts: Option<Vec<String>>,
}

/// User-managed sandbox policy fragment merged into the auto-discovered base policy.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfigUserPolicy {
    /// Platform-specific experimental policy fields.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub experimental: Option<SandboxConfigUserPolicyExperimental>,
    /// Filesystem rules to merge into the base policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filesystem: Option<SandboxConfigUserPolicyFilesystem>,
    /// Network rules to merge into the base policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub network: Option<SandboxConfigUserPolicyNetwork>,
}

/// Resolved sandbox configuration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SandboxConfig {
    /// Whether to auto-add the current working directory to readwritePaths. Default: true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub add_current_working_directory: Option<bool>,
    /// Raw `ContainerConfig` (per `@microsoft/mxc-sdk`) passed directly to `spawnSandboxFromConfig`, bypassing policy merging.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config: Option<serde_json::Value>,
    /// Whether sandboxing is enabled for the session.
    pub enabled: bool,
    /// User-managed sandbox policy fragment merged into the auto-discovered base policy.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_policy: Option<SandboxConfigUserPolicy>,
}

/// Schema for the `ScheduleEntry` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleEntry {
    /// Absolute fire time (epoch milliseconds) for a one-shot calendar schedule.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub at: Option<i64>,
    /// 5-field cron expression for a recurring calendar schedule, evaluated in `tz`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cron: Option<String>,
    /// Display-only label for the prompt as shown in the UI (e.g. `/skill-name` for a skill-invocation schedule). The actual enqueued prompt is `prompt`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_prompt: Option<String>,
    /// Sequential id assigned by the runtime within the session. Stable across resumes (rebuilt from the event log).
    pub id: i64,
    /// Interval between scheduled ticks, in milliseconds (relative-interval schedules).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub interval_ms: Option<i64>,
    /// ISO 8601 timestamp when the next tick is scheduled to fire.
    pub next_run_at: String,
    /// Prompt text that gets enqueued on every tick.
    pub prompt: String,
    /// Whether the schedule re-arms after each tick (`/every`) or fires once (`/after`).
    pub recurring: bool,
    /// IANA timezone the `cron` expression is evaluated in.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tz: Option<String>,
}

/// Snapshot of the currently active recurring prompts for this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleList {
    /// Active scheduled prompts, ordered by id.
    pub entries: Vec<ScheduleEntry>,
}

/// Identifier of the scheduled prompt to remove.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleStopRequest {
    /// Id of the scheduled prompt to remove.
    pub id: i64,
}

/// Remove a scheduled prompt by id. The result entry is omitted if the id was unknown.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ScheduleStopResult {
    /// The removed entry, or omitted if no entry matched.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entry: Option<ScheduleEntry>,
}

/// Secret values to add to the redaction filter.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SecretsAddFilterValuesRequest {
    /// Raw secret values to register for redaction
    pub values: Vec<String>,
}

/// Confirmation that the secret values were registered.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SecretsAddFilterValuesResult {
    /// Whether the values were successfully registered
    pub ok: bool,
}

/// Parameters for session.extensions.sendAttachmentsToMessage.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendAttachmentsToMessageParams {
    /// Attachments to push into the next user-message turn. extension_context entries take the slim shape; standard variants take their full AttachmentSchema shape.
    pub attachments: Vec<serde_json::Value>,
    /// Optional canvas instance binding the push for provenance. When supplied, the runtime resolves the canvas, verifies it is owned by the calling extension, and stamps canvasId/instanceId onto each extension_context entry. When omitted, no resolution runs and those fields stay unset on the attachment.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub instance_id: Option<String>,
}

/// Parameters for sending a user message to the session
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendRequest {
    /// The UI mode the agent was in when this message was sent. Defaults to the session's current mode.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_mode: Option<SendAgentMode>,
    /// Optional attachments (files, directories, selections, blobs, GitHub references) to include with the message
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attachments: Option<Vec<serde_json::Value>>,
    /// If false, this message will not trigger a Premium Request Unit charge. User messages default to billable.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub billable: Option<bool>,
    /// If provided, this is shown in the timeline instead of `prompt`
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_prompt: Option<String>,
    /// How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<SendMode>,
    /// If true, adds the message to the front of the queue instead of the end
    #[serde(skip_serializing_if = "Option::is_none")]
    pub prepend: Option<bool>,
    /// The user message text
    pub prompt: String,
    /// Custom HTTP headers to include in outbound model requests for this turn. Merged with session-level provider headers; per-turn headers augment and overwrite session-level headers with the same key.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub request_headers: Option<HashMap<String, String>>,
    /// If set, the request will fail if the named tool is not available when this message is among the user messages at the start of the current exchange
    #[serde(skip_serializing_if = "Option::is_none")]
    pub required_tool: Option<String>,
    /// Optional provenance tag copied to the resulting user.message event. Must match one of three forms: the literal `system`, `command-<command-id>` for messages originating from a command (e.g. slash command, Mission Control command), or `schedule-<numeric-id>` for messages originating from a scheduled job.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) source: Option<String>,
    /// W3C Trace Context traceparent header for distributed tracing of this agent turn
    #[serde(skip_serializing_if = "Option::is_none")]
    pub traceparent: Option<String>,
    /// W3C Trace Context tracestate header for distributed tracing
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tracestate: Option<String>,
    /// If true, await completion of the agentic loop for this message before returning. Defaults to false (fire-and-forget). When true, the result still contains the same `messageId`; the caller can rely on the agent having processed the message before the call resolves.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wait: Option<bool>,
}

/// Result of sending a user message
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SendResult {
    /// Unique identifier assigned to the message
    pub message_id: String,
}

/// Agents discovered across user, project, plugin, and remote sources.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ServerAgentList {
    /// All discovered agents across all sources
    pub agents: Vec<AgentInfo>,
}

/// Instruction sources discovered across user, repository, and plugin sources.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ServerInstructionSourceList {
    /// All discovered instruction sources
    pub sources: Vec<InstructionSource>,
}

/// Schema for the `ServerSkill` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ServerSkill {
    /// Description of what the skill does
    pub description: String,
    /// Whether the skill is currently enabled (based on global config)
    pub enabled: bool,
    /// Unique identifier for the skill
    pub name: String,
    /// Absolute path to the skill file
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// The project path this skill belongs to (only for project/inherited skills)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_path: Option<String>,
    /// Source location type (e.g., project, personal-copilot, plugin, builtin)
    pub source: SkillSource,
    /// Whether the skill can be invoked by the user as a slash command
    pub user_invocable: bool,
}

/// Skills discovered across global and project sources.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ServerSkillList {
    /// All discovered skills across all sources
    pub skills: Vec<ServerSkill>,
}

/// Current activity flags for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionActivity {
    /// Whether an in-flight operation can currently be aborted.
    pub abortable: bool,
    /// Whether the session currently has active work, including running turns or tasks.
    pub has_active_work: bool,
}

/// Authentication status and account metadata for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAuthStatus {
    /// Authentication type
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_type: Option<AuthInfoType>,
    /// Copilot plan tier (e.g., individual_pro, business)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_plan: Option<String>,
    /// Authentication host URL
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    /// Whether the session has resolved authentication
    pub is_authenticated: bool,
    /// Authenticated login/username, if available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// Human-readable authentication status description
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_message: Option<String>,
}

/// Map of sessionId -> bytes freed by removing the session's workspace directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionBulkDeleteResult {
    /// Map of sessionId -> bytes freed by removing the session's workspace directory. Sessions whose deletion failed are omitted from this map (failures are logged on the server but not surfaced per-id; check the map for absent IDs to detect them).
    pub freed_bytes: HashMap<String, i64>,
}

/// Token breakdown for the current context window, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionContextInfo {
    /// Output reserve plus tokens after the buffer-exhaustion blocking threshold (default 95%)
    pub buffer_tokens: i64,
    /// Token count at which background compaction starts (configurable percentage of promptTokenLimit)
    pub compaction_threshold: i64,
    /// Tokens consumed by user/assistant/tool messages
    pub conversation_tokens: i64,
    /// Prompt token limit plus the model's full output token limit.
    pub limit: i64,
    /// Tokens consumed by MCP tool definitions (subset of toolDefinitionsTokens, excludes deferred tools)
    pub mcp_tools_tokens: i64,
    /// The model used for token counting
    pub model_name: String,
    /// Maximum prompt tokens allowed by the model (or DEFAULT_TOKEN_LIMIT if unspecified)
    pub prompt_token_limit: i64,
    /// Tokens consumed by the system prompt
    pub system_tokens: i64,
    /// Tokens consumed by tool definitions sent to the model (excludes deferred tools)
    pub tool_definitions_tokens: i64,
    /// Sum of system, conversation and tool-definition tokens
    pub total_tokens: i64,
}

/// The enriched metadata records, with summary and context fields backfilled where available. Sessions confirmed empty and unnamed are omitted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEnrichMetadataResult {
    /// Enriched records, with summary and context backfilled. Sessions confirmed empty and unnamed may be omitted.
    pub sessions: Vec<LocalSessionMetadataValue>,
}

/// File path, content to append, and optional mode for the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsAppendFileRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
    /// Content to append
    pub content: String,
    /// Optional POSIX-style mode for newly created files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<i64>,
}

/// Describes a filesystem error.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsError {
    /// Error classification
    pub code: SessionFsErrorCode,
    /// Free-form detail about the error, for logging/diagnostics
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
}

/// Path to test for existence in the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsExistsRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
}

/// Indicates whether the requested path exists in the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsExistsResult {
    /// Whether the path exists
    pub exists: bool,
}

/// Directory path to create in the client-provided session filesystem, with options for recursive creation and POSIX mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsMkdirRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
    /// Create parent directories as needed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recursive: Option<bool>,
    /// Optional POSIX-style mode for newly created directories
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<i64>,
}

/// Directory path whose entries should be listed from the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
}

/// Names of entries in the requested directory, or a filesystem error if the read failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirResult {
    /// Entry names in the directory
    pub entries: Vec<String>,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

/// Schema for the `SessionFsReaddirWithTypesEntry` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesEntry {
    /// Entry name
    pub name: String,
    /// Entry type
    pub r#type: SessionFsReaddirWithTypesEntryType,
}

/// Directory path whose entries (with type information) should be listed from the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
}

/// Entries in the requested directory paired with file/directory type information, or a filesystem error if the read failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReaddirWithTypesResult {
    /// Directory entries with type information
    pub entries: Vec<SessionFsReaddirWithTypesEntry>,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

/// Path of the file to read from the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
}

/// File content as a UTF-8 string, or a filesystem error if the read failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsReadFileResult {
    /// File content as UTF-8 string
    pub content: String,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
}

/// Source and destination paths for renaming or moving an entry in the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsRenameRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Source path using SessionFs conventions
    pub src: String,
    /// Destination path using SessionFs conventions
    pub dest: String,
}

/// Path to remove from the client-provided session filesystem, with options for recursive removal and force.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsRmRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
    /// Remove directories and their contents recursively
    #[serde(skip_serializing_if = "Option::is_none")]
    pub recursive: Option<bool>,
    /// Ignore errors if the path does not exist
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force: Option<bool>,
}

/// Optional capabilities declared by the provider
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSetProviderCapabilities {
    /// Whether the provider supports SQLite query/exists operations
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sqlite: Option<bool>,
}

/// Initial working directory, session-state path layout, and path conventions used to register the calling SDK client as the session filesystem provider.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSetProviderRequest {
    /// Optional capabilities declared by the provider
    #[serde(skip_serializing_if = "Option::is_none")]
    pub capabilities: Option<SessionFsSetProviderCapabilities>,
    /// Path conventions used by this filesystem
    pub conventions: SessionFsSetProviderConventions,
    /// Initial working directory for sessions
    pub initial_cwd: String,
    /// Path within each session's SessionFs where the runtime stores files for that session
    pub session_state_path: String,
}

/// Indicates whether the calling client was registered as the session filesystem provider.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSetProviderResult {
    /// Whether the provider was set successfully
    pub success: bool,
}

/// Indicates whether the per-session SQLite database already exists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSqliteExistsResult {
    /// Whether the session database already exists
    pub exists: bool,
}

/// SQL query, query type, and optional bind parameters for executing a SQLite query against the per-session database.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSqliteQueryRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// SQL query to execute
    pub query: String,
    /// How to execute the query: 'exec' for DDL/multi-statement (no results), 'query' for SELECT (returns rows), 'run' for INSERT/UPDATE/DELETE (returns rowsAffected)
    pub query_type: SessionFsSqliteQueryType,
    /// Optional named bind parameters
    #[serde(skip_serializing_if = "Option::is_none")]
    pub params: Option<HashMap<String, serde_json::Value>>,
}

/// Query results including rows, columns, and rows affected, or a filesystem error if execution failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSqliteQueryResult {
    /// Column names from the result set
    pub columns: Vec<String>,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
    /// SQLite last_insert_rowid() value for INSERT.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_insert_rowid: Option<i64>,
    /// For SELECT: array of row objects. For others: empty array.
    pub rows: Vec<HashMap<String, serde_json::Value>>,
    /// Number of rows affected (for INSERT/UPDATE/DELETE)
    pub rows_affected: i64,
}

/// Path whose metadata should be returned from the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsStatRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
}

/// Filesystem metadata for the requested path, or a filesystem error if the stat failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsStatResult {
    /// ISO 8601 timestamp of creation
    pub birthtime: String,
    /// Describes a filesystem error.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<SessionFsError>,
    /// Whether the path is a directory
    pub is_directory: bool,
    /// Whether the path is a file
    pub is_file: bool,
    /// ISO 8601 timestamp of last modification
    pub mtime: String,
    /// File size in bytes
    pub size: i64,
}

/// File path, content to write, and optional mode for the client-provided session filesystem.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsWriteFileRequest {
    /// Target session identifier
    pub session_id: SessionId,
    /// Path using SessionFs conventions
    pub path: String,
    /// Content to write
    pub content: String,
    /// Optional POSIX-style mode for newly created files
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<i64>,
}

/// Schema for the `SessionInstalledPlugin` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstalledPlugin {
    /// Path where the plugin is cached locally
    #[serde(rename = "cache_path", skip_serializing_if = "Option::is_none")]
    pub cache_path: Option<String>,
    /// Whether the plugin is currently enabled
    pub enabled: bool,
    /// Installation timestamp (ISO-8601)
    #[serde(rename = "installed_at")]
    pub installed_at: String,
    /// Marketplace the plugin came from (empty string for direct repo installs)
    pub marketplace: String,
    /// Plugin name
    pub name: String,
    /// Source descriptor for direct repo installs (when marketplace is empty)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<serde_json::Value>,
    /// Installed version, if known
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

/// Schema for the `SessionInstalledPluginSourceGitHub` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstalledPluginSourceGitHub {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#ref: Option<String>,
    pub repo: String,
    /// Constant value. Always "github".
    pub source: SessionInstalledPluginSourceGitHubSource,
}

/// Schema for the `SessionInstalledPluginSourceLocal` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstalledPluginSourceLocal {
    pub path: String,
    /// Constant value. Always "local".
    pub source: SessionInstalledPluginSourceLocalSource,
}

/// Schema for the `SessionInstalledPluginSourceUrl` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstalledPluginSourceUrl {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#ref: Option<String>,
    /// Constant value. Always "url".
    pub source: SessionInstalledPluginSourceUrlSource,
    pub url: String,
}

/// Sessions matching the filter, ordered most-recently-modified first.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionList {
    /// Sessions ordered most-recently-modified first. Discriminated by `isRemote`.
    pub sessions: Vec<serde_json::Value>,
}

/// Optional filter applied to the returned sessions
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionListFilter {
    /// Match sessions whose context.branch equals this value
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// Match sessions whose context.cwd equals this value
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Match sessions whose context.gitRoot equals this value
    #[serde(skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Match sessions whose context.repository equals this value
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
}

/// Queued repo-level startup prompts and the total hook command count after loading.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionLoadDeferredRepoHooksResult {
    /// Total hook command count (user + plugin + repo) loaded for the session by this call. Captured atomically with startupPrompts so callers don't need to read a separate counter.
    pub hook_count: i64,
    /// Repo-level startup prompts queued from repo hook configs. Empty on resume, when no repo configs were pending, or when disableAllHooks is set.
    pub startup_prompts: Vec<String>,
}

/// Public-facing projection of workspace metadata for SDK / TUI consumers
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataSnapshotWorkspace {
    /// Branch checked out at session start, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// ISO 8601 timestamp when the workspace was created
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    /// Current working directory at session start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Resolved git root for cwd, if any
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Repository host type, if known
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspaceSummaryHostType>,
    /// Workspace identifier (1:1 with sessionId)
    pub id: String,
    /// Display name for the session, if set
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Repository identifier in 'owner/repo' or 'org/project/repo' format, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// ISO 8601 timestamp when the workspace was last updated
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    /// Whether the display name was explicitly set by the user
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Point-in-time snapshot of slow-changing session identifier and state fields
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataSnapshot {
    /// True when the session was detected to be in use by another process at construction time. Local consumers may surface a confirmation prompt before fully attaching. Always false for new sessions.
    pub already_in_use: bool,
    /// Runtime client name associated with the session (telemetry identifier).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot')
    pub current_mode: MetadataSnapshotCurrentMode,
    /// User-provided name supplied at session construction (via `--name`), if any. Immutable after construction.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initial_name: Option<String>,
    /// Whether this is a remote session (i.e., one whose runtime executes elsewhere and is steered through this process)
    pub is_remote: bool,
    /// ISO 8601 timestamp of when the session's persisted state was last modified on disk. For new sessions, equals startTime. For resumed sessions, reflects the previous modification time at construction.
    pub modified_time: String,
    /// Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are immutable for the lifetime of the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_metadata: Option<MetadataSnapshotRemoteMetadata>,
    /// Currently selected model identifier, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_model: Option<String>,
    /// The unique identifier of the session
    pub session_id: SessionId,
    /// ISO 8601 timestamp of when the session started
    pub start_time: String,
    /// Short human-readable summary of the session, if known. Omitted when no summary has been generated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    /// Absolute path to the session's current working directory
    pub working_directory: String,
    /// Public-facing workspace metadata for this session, or null if the session has no associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count, internal flags).
    pub workspace: Option<SessionMetadataSnapshotWorkspace>,
    /// Absolute path to the session's workspace directory on disk, or null if the session has no associated workspace
    pub workspace_path: Option<String>,
}

/// The list of models available to this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelList {
    /// Available models, ordered with the most preferred default first. Includes both Copilot (CAPI) models and any registry BYOK models; a BYOK model appears under its provider-qualified selection id (`provider/id`).
    pub list: Vec<serde_json::Value>,
    /// Per-quota snapshots returned alongside the model list, keyed by quota type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub quota_snapshots: Option<HashMap<String, serde_json::Value>>,
}

/// Schema for the `SessionOpenOptionsAdditionalContentExclusionPolicyRuleSource` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionOpenOptionsAdditionalContentExclusionPolicyRuleSource {
    pub name: String,
    pub r#type: String,
}

/// Schema for the `SessionOpenOptionsAdditionalContentExclusionPolicyRule` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionOpenOptionsAdditionalContentExclusionPolicyRule {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub if_any_match: Option<Vec<String>>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub if_none_match: Option<Vec<String>>,
    pub paths: Vec<String>,
    /// Schema for the `SessionOpenOptionsAdditionalContentExclusionPolicyRuleSource` type.
    pub source: SessionOpenOptionsAdditionalContentExclusionPolicyRuleSource,
}

/// Schema for the `SessionOpenOptionsAdditionalContentExclusionPolicy` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionOpenOptionsAdditionalContentExclusionPolicy {
    #[serde(rename = "last_updated_at")]
    pub last_updated_at: serde_json::Value,
    pub rules: Vec<SessionOpenOptionsAdditionalContentExclusionPolicyRule>,
    /// Allowed values for the `SessionOpenOptionsAdditionalContentExclusionPolicyScope` enumeration.
    pub scope: SessionOpenOptionsAdditionalContentExclusionPolicyScope,
}

/// Session construction options.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionOpenOptions {
    /// Additional content-exclusion policies to merge into the session policy set.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_content_exclusion_policies:
        Option<Vec<SessionOpenOptionsAdditionalContentExclusionPolicy>>,
    /// Runtime context discriminator for agent filtering.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_context: Option<String>,
    /// Whether ask_user is explicitly disabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ask_user_disabled: Option<bool>,
    /// Initial authentication info for the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_info: Option<serde_json::Value>,
    /// Allowlist of available tool names.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available_tools: Option<Vec<String>>,
    /// Structured client kind used for runtime behavior gates.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_kind: Option<String>,
    /// Identifier of the client driving the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Whether commit-message coauthor trailers are enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub coauthor_enabled: Option<bool>,
    /// Override Copilot configuration directory.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub config_dir: Option<String>,
    /// Whether auto-mode continuation is enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub continue_on_auto_mode: Option<bool>,
    /// Override URL for the Copilot API endpoint.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_url: Option<String>,
    /// Whether custom agents default to local-only execution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub custom_agents_local_only: Option<bool>,
    /// Parent engagement ID for detached child telemetry rollup.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detached_from_spawning_parent_engagement_id: Option<String>,
    /// Parent session ID for detached child telemetry rollup.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub detached_from_spawning_parent_session_id: Option<String>,
    /// Instruction source IDs disabled for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_instruction_sources: Option<Vec<String>>,
    /// Skill IDs disabled for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_skills: Option<Vec<String>>,
    /// Experimental: enable native model citations (Anthropic models today), normalized onto the `assistant.message` event. Off by default; may change or be removed while the citations surface is experimental.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_citations: Option<bool>,
    /// Whether on-demand custom instruction discovery is enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_on_demand_instruction_discovery: Option<bool>,
    /// Whether shell-script safety heuristics are enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_script_safety: Option<bool>,
    /// Whether model responses stream as delta events.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_streaming: Option<bool>,
    /// How MCP server environment values are interpreted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env_value_mode: Option<SessionOpenOptionsEnvValueMode>,
    /// Override directory for session event logs.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub events_log_directory: Option<String>,
    /// Denylist of tool names.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub excluded_tools: Option<Vec<String>>,
    /// ExP assignment ('flight') data injected by an SDK integrator, in the same JSON shape the Copilot CLI fetches from the experimentation service (CopilotExpAssignmentResponse). When supplied this is fed into the FeatureFlagService exactly like CLI-fetched assignments and ExP-backed flags wait for it. When absent the session does not block on ExP.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) exp_assignments: Option<serde_json::Value>,
    /// Feature-flag values resolved by the host.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feature_flags: Option<HashMap<String, bool>>,
    /// Installed plugins visible to the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub installed_plugins: Option<Vec<InstalledPlugin>>,
    /// Stable integration identifier for analytics.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub integration_id: Option<String>,
    /// Whether experimental behavior is enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_experimental_mode: Option<bool>,
    /// Whether interactive shell sessions are logged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub log_interactive_shells: Option<bool>,
    /// Identifier sent to LSP-style integrations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub lsp_client_name: Option<String>,
    /// Maximum decoded byte size of a single inline model-facing binary tool result persisted in session events (default 10 MB).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_inline_binary_bytes: Option<i64>,
    /// Memory configuration for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub memory: Option<MemoryConfiguration>,
    /// Initial model identifier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Initial model capability overrides.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities_overrides: Option<ModelCapabilitiesOverride>,
    /// BYOK model definitions added to the selectable model list, each referencing a provider name.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub models: Option<Vec<ProviderModelConfig>>,
    /// Optional human-friendly session name.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Custom model-provider configuration (BYOK).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ProviderConfig>,
    /// Named BYOK provider connections, additive to CAPI auth. Combining with `provider` is rejected.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub providers: Option<Vec<NamedProviderConfig>>,
    /// Initial reasoning effort level.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Initial reasoning summary mode for supported model clients.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summary: Option<SessionOpenOptionsReasoningSummary>,
    /// Telemetry-only remote-defaulted flag.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_defaulted_on: Option<bool>,
    /// Telemetry-only remote exporting flag.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_exporting: Option<bool>,
    /// Whether this session supports remote steering.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
    /// Whether the host is an interactive UI.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub running_in_interactive_mode: Option<bool>,
    /// Resolved sandbox configuration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_config: Option<SandboxConfig>,
    /// Capabilities enabled for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_capabilities: Option<Vec<SessionCapability>>,
    /// Optional stable session identifier to use for a new session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// Shell init profile.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell_init_profile: Option<String>,
    /// Per-shell process flags.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell_process_flags: Option<Vec<String>>,
    /// Additional directories to search for skills.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_directories: Option<Vec<String>>,
    /// Whether to skip custom instruction sources.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_custom_instructions: Option<bool>,
    /// Optional trajectory output file path.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub trajectory_file: Option<String>,
    /// Working directory to anchor the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
    /// Pre-resolved working-directory context for session startup.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory_context: Option<SessionContext>,
}

/// Parameters for creating a new local session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenCreate {
    /// Whether to emit session.start during creation. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub emit_start: Option<bool>,
    /// Create a new local session.
    pub kind: SessionsOpenCreateKind,
    /// Session construction options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<SessionOpenOptions>,
}

/// Parameters for resuming a specific local session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenResume {
    /// Resume a specific local session by ID or prefix.
    pub kind: SessionsOpenResumeKind,
    /// Session resume options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<SessionOpenOptions>,
    /// Whether to emit session.resume after loading. Defaults to true.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resume: Option<bool>,
    /// Session ID or unique prefix to resume.
    pub session_id: SessionId,
    /// Suppress workspace.yaml metadata writeback when resuming from an incidental cwd.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_resume_workspace_metadata_writeback: Option<bool>,
}

/// Parameters for resuming the most relevant local session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenResumeLast {
    /// Working-directory context used to choose the most relevant session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<SessionContext>,
    /// Resume the most relevant existing local session.
    pub kind: SessionsOpenResumeLastKind,
    /// Session resume options.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<SessionOpenOptions>,
    /// Suppress workspace.yaml metadata writeback when resuming from an incidental cwd.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_resume_workspace_metadata_writeback: Option<bool>,
}

/// Parameters for attaching to an already-active session by ID.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenAttach {
    /// Attach to an already-active in-process session by ID. Unlike `resume`, this does NOT re-load from disk; the session must already be loaded by an earlier `create`/`resume` call. Returns `status: 'not_found'` when no active session matches the id. Useful for in-process consumers that need a fresh API handle to a session opened elsewhere (e.g., a peer foreground-session switch).
    pub kind: SessionsOpenAttachKind,
    /// Session ID to attach to.
    pub session_id: SessionId,
}

/// Parameters for connecting to a live remote session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenRemote {
    /// Connect to a live remote session.
    pub kind: SessionsOpenRemoteKind,
    /// Session options for the connection.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<SessionOpenOptions>,
    /// Remote session identifier to connect to.
    pub remote_session_id: SessionId,
    /// Repository context for the remote session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<RemoteSessionRepository>,
}

/// Parameters for creating a new cloud session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenCloud {
    /// Create a new cloud (coding-agent) session.
    pub kind: SessionsOpenCloudKind,
    /// In-process callback invoked when the cloud task is created (before connection). Marked internal because a function reference cannot cross the JSON-RPC boundary. Disappears in the SDK migration: the field is purely cosmetic (it flips a single CLI phase label from 'creating' to 'connecting') and the wire-clean version just drops the intermediate phase.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) on_task_created: Option<serde_json::Value>,
    /// Session options for cloud session creation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<SessionOpenOptions>,
    /// Optional owner (user or organization login) to associate with the cloud session when no repository is provided. Ignored when `repository` is set (the repo's owner takes precedence).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub owner: Option<String>,
    /// Repository for the cloud session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<RemoteSessionRepository>,
}

/// Parameters for fetching a remote session and handing it off to a new local session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenHandoff {
    /// Fetch a remote session and hand it off to a new local session.
    pub kind: SessionsOpenHandoffKind,
    /// Remote session metadata for the session to hand off (typically obtained from `sessions.list` with `source: "remote"`).
    pub metadata: RemoteSessionMetadataValue,
    /// In-process progress callback `(update) => void` invoked for each handoff step. Marked internal because a function reference cannot cross the JSON-RPC boundary. The host-side `handoffSession` is already declared as `AsyncGenerator<HandoffProgress, HandoffResult>`; the schema layer flattens it because it does not yet support streaming methods. The wire-clean replacement is to expose the AsyncGenerator directly (or use vscode-jsonrpc `$/progress` notifications) once the schema/transport layer supports it.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) on_progress: Option<serde_json::Value>,
    /// Session construction options for the new local session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub options: Option<SessionOpenOptions>,
    /// Task type determines the handoff strategy (CCA fetches events; CLI prepares a transient session).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task_type: Option<SessionsOpenHandoffTaskType>,
}

/// Schema for the `SessionsOpenProgress` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenProgress {
    /// Optional step message.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// Step status.
    pub status: SessionsOpenProgressStatus,
    /// Handoff step.
    pub step: SessionsOpenProgressStep,
}

/// Result of opening a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionOpenResult {
    /// Remote session metadata, present when status is `connected`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<RemoteSessionMetadataValue>,
    /// Handoff progress steps, present when status is `handed_off`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub progress: Option<Vec<SessionsOpenProgress>>,
    /// Remote session ID, present when status is `connected`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_session_id: Option<SessionId>,
    /// In-process SessionClientApi handle for the opened session, returned to CLI callers as a transitional shortcut. Marked internal so the public SDK surface does not expose it; SDK consumers should construct per-session clients from `sessionId` instead.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) session_api: Option<serde_json::Value>,
    /// Opened session ID. Omitted when status is `not_found`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// Startup prompts queued by user-level hook configs at session creation. Only populated when status is `created`; resumed sessions return an empty array.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub startup_prompts: Option<Vec<String>>,
    /// Outcome of the open request.
    pub status: SessionsOpenStatus,
}

/// Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPruneResult {
    /// Session IDs that would be deleted in dry-run mode (always empty otherwise)
    pub candidates: Vec<String>,
    /// Session IDs that were deleted (always empty in dry-run mode)
    pub deleted: Vec<String>,
    /// True when no deletions were actually performed
    pub dry_run: bool,
    /// Total bytes freed (actual when not dry-run, projected when dry-run)
    pub freed_bytes: i64,
    /// Session IDs that were skipped (e.g., named sessions)
    pub skipped: Vec<String>,
}

/// Session IDs to close, deactivate, and delete from disk.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsBulkDeleteRequest {
    /// Session IDs to close, deactivate, and delete from disk
    pub session_ids: Vec<String>,
}

/// Session IDs to test for live in-use locks.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsCheckInUseRequest {
    /// Session IDs to test for live in-use locks
    pub session_ids: Vec<String>,
}

/// Session IDs from the input set that are currently in use by another process.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsCheckInUseResult {
    /// Session IDs from the input set that are currently held by another running process via an alive lock file
    pub in_use: Vec<String>,
}

/// Session ID to close.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsCloseRequest {
    /// Session ID to close
    pub session_id: SessionId,
}

/// Closes a session: emits shutdown, flushes pending events to disk, releases the in-use lock, disposes the active session. Idempotent: succeeds even if the session is not currently active.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsCloseResult {}

/// Session metadata records to enrich with summary and context information.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsEnrichMetadataRequest {
    /// Session metadata records to enrich. Records that already have summary and context are returned unchanged.
    pub sessions: Vec<LocalSessionMetadataValue>,
}

/// New auth credentials to install on the session. Omit to leave credentials unchanged.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSetCredentialsParams {
    /// The new auth credentials to install on the session. When omitted or `undefined`, the call is a no-op and the session's existing credentials are preserved. The runtime stores the value verbatim and uses it for outbound model/API requests; it does NOT re-validate or re-fetch the associated Copilot user response. Several variants carry secret material; treat this method's params as containing secrets at rest and in transit.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub credentials: Option<serde_json::Value>,
}

/// Indicates whether the credential update succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSetCredentialsResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// UUID prefix to resolve to a unique session ID.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsFindByPrefixRequest {
    /// UUID prefix (>=7 hex chars, <36 chars). Returns the unique session ID, or undefined when there is no match or the prefix matches multiple sessions.
    pub prefix: String,
}

/// Session ID matching the prefix, omitted when no unique match exists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsFindByPrefixResult {
    /// Omitted when no unique session matches the prefix (no match or ambiguous)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

/// GitHub task ID to look up.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsFindByTaskIDRequest {
    /// GitHub task ID to look up
    pub task_id: String,
}

/// ID of the local session bound to the given GitHub task, or omitted when none.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsFindByTaskIDResult {
    /// Omitted when no local session is bound to that GitHub task
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

/// Source session identifier to fork from, optional event-ID boundary, and optional friendly name for the new session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsForkRequest {
    /// Optional friendly name to assign to the forked session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Source session ID to fork from
    pub session_id: SessionId,
    /// Optional event ID boundary. When provided, the fork includes only events before this ID (exclusive). When omitted, all events are included.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub to_event_id: Option<String>,
}

/// Identifier and optional friendly name assigned to the newly forked session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsForkResult {
    /// Friendly name assigned to the forked session, if any.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// The new forked session's ID
    pub session_id: SessionId,
}

/// Session ID whose board entry count should be returned.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetBoardEntryCountRequest {
    /// Session ID whose board entry count should be returned.
    pub session_id: SessionId,
}

/// Dynamic-context board entry count, when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetBoardEntryCountResult {
    /// Board entry count, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub count: Option<i64>,
}

/// Session ID whose event-log file path to compute.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetEventFilePathRequest {
    /// Session ID whose event-log file path to compute
    pub session_id: SessionId,
}

/// Absolute path to the session's events.jsonl file on disk.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetEventFilePathResult {
    /// Absolute path to the session's events.jsonl file
    pub file_path: String,
}

/// Optional working-directory context used to score session relevance.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetLastForContextRequest {
    /// Optional working-directory context used to score session relevance. When omitted the most-recently-modified session wins.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context: Option<SessionContext>,
}

/// Most-relevant session ID for the supplied context, or omitted when no sessions exist.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetLastForContextResult {
    /// Most-relevant session ID for the supplied context, or omitted when no sessions exist
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

/// Session ID to look up the persisted remote-steerable flag for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetPersistedRemoteSteerableRequest {
    /// Session ID to look up the persisted remote-steerable flag for
    pub session_id: SessionId,
}

/// The session's persisted remote-steerable flag, or omitted when no value has been persisted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetPersistedRemoteSteerableResult {
    /// The session's persisted remote-steerable flag if recorded; omitted when no value has been persisted
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
}

/// Map of sessionId -> on-disk size in bytes for each session's workspace directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSizes {
    /// Map of sessionId -> on-disk size in bytes for the session's workspace directory
    pub sizes: HashMap<String, i64>,
}

/// Optional source filter, metadata-load limit, and context filter applied to the returned sessions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsListRequest {
    /// Optional filter applied to the returned sessions
    #[serde(skip_serializing_if = "Option::is_none")]
    pub filter: Option<SessionListFilter>,
    /// When true, include detached maintenance sessions. Defaults to false for user-facing session lists.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_detached: Option<bool>,
    /// When provided, only the first N local sessions (sorted by modification time, newest first) load full metadata; remaining sessions return basic info only. Use 0 to return only basic info for every local session. Has no effect on remote entries (which always carry their full shape).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata_limit: Option<i64>,
    /// Which session sources to include. Defaults to `local` for backward compatibility.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub source: Option<SessionSource>,
    /// Only meaningful when `source` includes remote. When true, propagates errors from the remote service instead of silently returning an empty remote list. Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub throw_on_error: Option<bool>,
}

/// Active session ID whose deferred repo-level hooks should be loaded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsLoadDeferredRepoHooksRequest {
    /// Active session ID whose deferred repo-level hooks should be loaded
    pub session_id: SessionId,
}

/// Cursor and optional long-poll wait for polling runtime-spawned sessions.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsPollSpawnedSessionsRequest {
    /// Opaque cursor returned by a previous poll. Omit on the first call to receive any spawn events buffered since the runtime started.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cursor: Option<String>,
    /// Milliseconds to wait for new spawn events when the cursor is at the tail. 0 (default) returns immediately even if no events are buffered. Capped at 60000ms.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wait_ms: Option<i32>,
}

/// Age threshold and optional flags controlling which old sessions are pruned (or simulated when dryRun is true).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsPruneOldRequest {
    /// When true, only report what would be deleted without performing any deletion
    #[serde(skip_serializing_if = "Option::is_none")]
    pub dry_run: Option<bool>,
    /// Session IDs that should never be considered for pruning
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_session_ids: Option<Vec<String>>,
    /// When true, named sessions (set via /rename) are also eligible for pruning
    #[serde(skip_serializing_if = "Option::is_none")]
    pub include_named: Option<bool>,
    /// Delete sessions whose modifiedTime is at least this many days old
    pub older_than_days: i64,
}

/// Session ID whose in-use lock should be released.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsReleaseLockRequest {
    /// Session ID whose in-use lock should be released
    pub session_id: SessionId,
}

/// Release the in-use lock held by this process for the given session. No-op when this process does not currently hold a lock for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsReleaseLockResult {}

/// Active session ID and an optional flag for deferring repo-level hooks until folder trust.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsReloadPluginHooksRequest {
    /// When true, skip repo-level hooks. Use before folder trust is confirmed; loadDeferredRepoHooks loads them post-trust.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub defer_repo_hooks: Option<bool>,
    /// Active session ID to reload hooks for
    pub session_id: SessionId,
}

/// Reload all hooks (user, plugin, optionally repo) and apply them to the active session. Call after installing or removing plugins so their hooks take effect immediately. No-op when no active session matches the given sessionId.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsReloadPluginHooksResult {}

/// Session ID whose pending events should be flushed to disk.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsSaveRequest {
    /// Session ID whose pending events should be flushed to disk
    pub session_id: SessionId,
}

/// Flush a session's pending events to disk. No-op when no writer exists for the session (e.g., already closed).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsSaveResult {}

/// Manager-wide additional plugins to register; replaces any previously-configured set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsSetAdditionalPluginsRequest {
    /// Manager-wide additional plugins to register. Replaces any previously-configured set. Pass an empty array to clear.
    pub plugins: Vec<InstalledPlugin>,
}

/// Replace the manager-wide additional plugins. New session creations and subsequent hook reloads see the new set; already-running sessions keep their existing hook installation until the next reload.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsSetAdditionalPluginsResult {}

/// Patch for the singleton's steering state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsSetRemoteControlSteeringRequest {
    /// Target steering state. Today only `true` is actionable on the underlying exporter; `false` is reserved for future use.
    pub enabled: bool,
}

/// Parameters for attaching the remote-control singleton to a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsStartRemoteControlRequest {
    /// Configuration for the runtime-managed remote-control singleton.
    pub config: RemoteControlConfig,
    /// Local session id to attach remote control to.
    pub session_id: SessionId,
}

/// Parameters for stopping the remote-control singleton.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsStopRemoteControlRequest {
    /// When provided, the stop is rejected unless the singleton currently points at this session id (compare-and-swap semantics).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expected_session_id: Option<String>,
    /// When true, the singleton is unconditionally torn down regardless of `expectedSessionId`. Use during shutdown or explicit `/remote off`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub force: Option<bool>,
}

/// Parameters for atomically rebinding the remote-control singleton.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsTransferRemoteControlRequest {
    /// When provided, the transfer is rejected unless the singleton currently points at this session id (compare-and-swap semantics to avoid clobbering newer state).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub expected_from_session_id: Option<String>,
    /// Local session id to point remote control at.
    pub to_session_id: String,
}

/// Telemetry engagement ID for the session, when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTelemetryEngagement {
    /// Current telemetry engagement ID, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub engagement_id: Option<String>,
}

/// Patch of mutable session options to apply to the running session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUpdateOptionsParams {
    /// Additional content-exclusion policies to merge into the session's policy set.
    ///
    /// <div class="warning">
    ///
    /// **Experimental.** This type is part of an experimental wire-protocol surface
    /// and may change or be removed in future SDK or CLI releases.
    ///
    /// </div>
    #[serde(skip_serializing_if = "Option::is_none")]
    pub additional_content_exclusion_policies:
        Option<Vec<OptionsUpdateAdditionalContentExclusionPolicy>>,
    /// Runtime context discriminator (e.g., `cli`, `actions`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agent_context: Option<String>,
    /// Whether to disable the `ask_user` tool (encourages autonomous behavior).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ask_user_disabled: Option<bool>,
    /// Allowlist of tool names available to this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub available_tools: Option<Vec<String>>,
    /// Identifier of the client driving the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// Whether to include the `Co-authored-by` trailer in commit messages.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub coauthor_enabled: Option<bool>,
    /// Context tier for models with tiered pricing. The session uses this to derive effective `modelCapabilitiesOverrides` so compaction, truncation, token display, and request limits honor the selected tier.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<OptionsUpdateContextTier>,
    /// Whether to allow auto-mode continuation across turns.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub continue_on_auto_mode: Option<bool>,
    /// Override URL for the Copilot API endpoint.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_url: Option<String>,
    /// Whether to default custom agents to local-only execution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub custom_agents_local_only: Option<bool>,
    /// Instruction source IDs to exclude from the system prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_instruction_sources: Option<Vec<String>>,
    /// Skill IDs that should be excluded from this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_skills: Option<Vec<String>>,
    /// Whether to enable loading of `.github/hooks/` filesystem hooks. Separate from the SDK callback hook mechanism.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_file_hooks: Option<bool>,
    /// Whether to enable host git operations (context resolution, child repo scanning, git info in system prompt).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_host_git_operations: Option<bool>,
    /// Whether to discover custom instructions on demand after successful file views (AGENTS.md / CLAUDE.md / .github/copilot-instructions.md surfacing). Combined with `skipCustomInstructions` and the runtime-side `ON_DEMAND_INSTRUCTIONS` feature flag.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_on_demand_instruction_discovery: Option<bool>,
    /// Whether to surface reasoning-summary events from the model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_reasoning_summaries: Option<bool>,
    /// Whether shell-script safety heuristics are enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_script_safety: Option<bool>,
    /// Whether to enable cross-session store writes and reads.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_session_store: Option<bool>,
    /// Whether to enable skill directory scanning and loading. Falls back to enableConfigDiscovery when unset.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_skills: Option<bool>,
    /// Whether to stream model responses.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enable_streaming: Option<bool>,
    /// How env values are passed to MCP servers (`direct` inlines literal values; `indirect` resolves at launch).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub env_value_mode: Option<OptionsUpdateEnvValueMode>,
    /// Override directory for the session-events log. When unset, the runtime's default events log directory is used.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub events_log_directory: Option<String>,
    /// Denylist of tool names for this session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub excluded_tools: Option<Vec<String>>,
    /// Map of feature-flag IDs to their boolean enabled state.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feature_flags: Option<HashMap<String, bool>>,
    /// Full set of installed plugins for the session. Replaces the existing list; the runtime invalidates the skills cache only when the list materially changes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub installed_plugins: Option<Vec<SessionInstalledPlugin>>,
    /// Stable integration identifier used for analytics and rate-limit attribution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub integration_id: Option<String>,
    /// Whether experimental capabilities are enabled.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_experimental_mode: Option<bool>,
    /// Whether interactive shell sessions are logged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub log_interactive_shells: Option<bool>,
    /// Identifier sent to LSP-style integrations.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub lsp_client_name: Option<String>,
    /// Whether to expose the `manage_schedule` tool to the agent. The runtime always owns the per-session schedule registry; this flag only controls tool exposure (typically gated to staff users).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub manage_schedule_enabled: Option<bool>,
    /// Maximum decoded byte size of a single model-facing binary tool result (e.g. an image) persisted inline in session events and re-presented to the model on later turns / resume. Larger results are persisted as a metadata-only marker and shown to the model as a short text note. Defaults to 10 MB.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_inline_binary_bytes: Option<i64>,
    /// The model ID to use for assistant turns.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Per-property model capability overrides for the selected model.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_capabilities_overrides: Option<ModelCapabilitiesOverride>,
    /// Organization-level custom instructions to inject into the system prompt.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub organization_custom_instructions: Option<String>,
    /// Custom model-provider configuration (BYOK).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub provider: Option<ProviderConfig>,
    /// Reasoning effort for the selected model (model-defined enum).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
    /// Reasoning summary mode for supported model clients.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_summary: Option<OptionsUpdateReasoningSummary>,
    /// Whether the session is running in an interactive UI.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub running_in_interactive_mode: Option<bool>,
    /// Resolved sandbox configuration.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub sandbox_config: Option<SandboxConfig>,
    /// Replaces the session's capability set with the given list. Use to enable or disable capabilities mid-session (e.g., remove `memory` for reproducible scripted runs). Omit the field to leave the existing capability set unchanged.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_capabilities: Option<Vec<SessionCapability>>,
    /// Shell init profile (`None` or `NonInteractive`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell_init_profile: Option<String>,
    /// Per-shell process flags (e.g., `pwsh` arguments).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shell_process_flags: Option<Vec<String>>,
    /// Additional directories to search for skills.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_directories: Option<Vec<String>>,
    /// Whether to skip loading custom instruction sources.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_custom_instructions: Option<bool>,
    /// Whether to skip embedding retrieval pipeline initialization and execution.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skip_embedding_retrieval: Option<bool>,
    /// When true, the selected custom agent's prompt is not injected into the user message (skill context is still injected). Used by automation triggers where the agent prompt is already in the problem statement.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub suppress_custom_agent_prompt: Option<bool>,
    /// Controls how availableTools (allowlist) and excludedTools (denylist) combine when both are set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tool_filter_precedence: Option<OptionsUpdateToolFilterPrecedence>,
    /// Optional path for trajectory output.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub trajectory_file: Option<String>,
    /// Absolute working-directory path for shell tools.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub working_directory: Option<String>,
}

/// Indicates whether the session options patch was applied successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUpdateOptionsResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// User-requested shell execution cancellation handle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellCancelUserRequestedRequest {
    /// Request ID previously passed to executeUserRequested
    pub request_id: RequestId,
}

/// Shell command to run, with optional working directory and timeout in milliseconds.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellExecRequest {
    /// Shell command to execute
    pub command: String,
    /// Working directory (defaults to session working directory)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Timeout in milliseconds (default: 30000)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<i64>,
}

/// Identifier of the spawned process, used to correlate streamed output and exit notifications.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellExecResult {
    /// Unique identifier for tracking streamed output
    pub process_id: String,
}

/// User-requested shell command and cancellation handle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellExecuteUserRequestedRequest {
    /// Shell command to execute
    pub command: String,
    /// Caller-provided cancellation handle for this execution
    pub request_id: RequestId,
}

/// Identifier of a process previously returned by "shell.exec" and the signal to send.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellKillRequest {
    /// Process identifier returned by shell.exec
    pub process_id: String,
    /// Signal to send (default: SIGTERM)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub signal: Option<ShellKillSignal>,
}

/// Indicates whether the signal was delivered; false if the process was unknown or already exited.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShellKillResult {
    /// Whether the signal was sent successfully
    pub killed: bool,
}

/// Parameters for shutting down the session
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ShutdownRequest {
    /// Optional human-readable reason. Typically the message of the error that triggered shutdown when type is 'error'.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reason: Option<String>,
    /// Why the session is being shut down. Defaults to "routine" when omitted.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub r#type: Option<ShutdownType>,
}

/// Schema for the `Skill` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Skill {
    /// Description of what the skill does
    pub description: String,
    /// Whether the skill is currently enabled
    pub enabled: bool,
    /// Unique identifier for the skill
    pub name: String,
    /// Absolute path to the skill file
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Name of the plugin that provides the skill, when source is 'plugin'
    #[serde(skip_serializing_if = "Option::is_none")]
    pub plugin_name: Option<String>,
    /// Source location type (e.g., project, personal-copilot, plugin, builtin)
    pub source: SkillSource,
    /// Whether the skill can be invoked by the user as a slash command
    pub user_invocable: bool,
}

/// Schema for the `SkillDiscoveryPath` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillDiscoveryPath {
    /// Absolute path of the create/discovery target (may not exist on disk yet)
    pub path: String,
    /// Whether this is the canonical directory to create a new skill in its tier. At most one entry per tier is preferred; the `personal-agents` and `custom` scopes are never preferred.
    pub preferred_for_creation: bool,
    /// The input project path this directory was derived from (only for project scope)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_path: Option<String>,
    /// Which tier this directory belongs to
    pub scope: SkillDiscoveryScope,
}

/// Canonical locations where skills can be created so the runtime will recognize them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillDiscoveryPathList {
    /// Canonical skill create/discovery directories, in priority order
    pub paths: Vec<SkillDiscoveryPath>,
}

/// Skills available to the session, with their enabled state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillList {
    /// Available skills
    pub skills: Vec<Skill>,
}

/// Skill names to mark as disabled in global configuration, replacing any previous list.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsConfigSetDisabledSkillsRequest {
    /// List of skill names to disable
    pub disabled_skills: Vec<String>,
}

/// Name of the skill to disable for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDisableRequest {
    /// Name of the skill to disable
    pub name: String,
}

/// Optional project paths and additional skill directories to include in discovery.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDiscoverRequest {
    /// When true, omit skills from the host's global sources (personal, custom, plugin, and built-in), returning only project-scoped skills. For multitenant deployments.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_host_skills: Option<bool>,
    /// Optional list of project directory paths to scan for project-scoped skills
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
    /// Optional list of additional skill directory paths to include
    #[serde(skip_serializing_if = "Option::is_none")]
    pub skill_directories: Option<Vec<String>>,
}

/// Name of the skill to enable for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsEnableRequest {
    /// Name of the skill to enable
    pub name: String,
}

/// Optional project paths to enumerate.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsGetDiscoveryPathsRequest {
    /// When true, omit the host's personal and custom skill directories, leaving only project directories. For multitenant deployments.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exclude_host_skills: Option<bool>,
    /// Optional list of project directory paths. When omitted or empty, only personal and custom directories are returned.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub project_paths: Option<Vec<String>>,
}

/// Schema for the `SkillsInvokedSkill` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsInvokedSkill {
    /// Tools that should be auto-approved when this skill is active, captured at invocation time
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allowed_tools: Option<Vec<String>>,
    /// Full content of the skill file
    pub content: String,
    /// Turn number when the skill was invoked
    pub invoked_at_turn: i64,
    /// Unique identifier for the skill
    pub name: String,
    /// Path to the SKILL.md file
    pub path: String,
}

/// Skills invoked during this session, ordered by invocation time (most recent last).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsGetInvokedResult {
    /// Skills invoked during this session, ordered by invocation time (most recent last)
    pub skills: Vec<SkillsInvokedSkill>,
}

/// Diagnostics from reloading skill definitions, with warnings and errors as separate lists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsLoadDiagnostics {
    /// Errors emitted while loading skills (e.g. skills that failed to load entirely)
    pub errors: Vec<String>,
    /// Warnings emitted while loading skills (e.g. skills that loaded but had issues)
    pub warnings: Vec<String>,
}

/// Schema for the `SlashCommandAgentPromptResult` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandAgentPromptResult {
    /// Prompt text to display to the user
    pub display_prompt: String,
    /// Agent prompt result discriminator
    pub kind: SlashCommandAgentPromptResultKind,
    /// Optional target session mode for the agent prompt
    #[serde(skip_serializing_if = "Option::is_none")]
    pub mode: Option<SessionMode>,
    /// Prompt to submit to the agent
    pub prompt: String,
    /// True when the invocation mutated user runtime settings; consumers caching settings should refresh
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
}

/// Schema for the `SlashCommandCompletedResult` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandCompletedResult {
    /// Completed result discriminator
    pub kind: SlashCommandCompletedResultKind,
    /// Optional user-facing message describing the completed command
    #[serde(skip_serializing_if = "Option::is_none")]
    pub message: Option<String>,
    /// True when the invocation mutated user runtime settings; consumers caching settings should refresh
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
}

/// Schema for the `SlashCommandTextResult` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandTextResult {
    /// Text result discriminator
    pub kind: SlashCommandTextResultKind,
    /// Whether text contains Markdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub markdown: Option<bool>,
    /// Whether ANSI sequences should be preserved
    #[serde(skip_serializing_if = "Option::is_none")]
    pub preserve_ansi: Option<bool>,
    /// True when the invocation mutated user runtime settings; consumers caching settings should refresh
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
    /// Text output for the client to render
    pub text: String,
}

/// Schema for the `SlashCommandSelectSubcommandOption` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandSelectSubcommandOption {
    /// Human-readable description of the subcommand
    pub description: String,
    /// Optional group label for organizing options
    #[serde(skip_serializing_if = "Option::is_none")]
    pub group: Option<String>,
    /// Subcommand name to invoke
    pub name: String,
}

/// Schema for the `SlashCommandSelectSubcommandResult` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SlashCommandSelectSubcommandResult {
    /// Parent command name that requires subcommand selection
    pub command: String,
    /// Select subcommand result discriminator
    pub kind: SlashCommandSelectSubcommandResultKind,
    /// Available subcommand options for the client to present
    pub options: Vec<SlashCommandSelectSubcommandOption>,
    /// True when the invocation mutated user runtime settings; consumers caching settings should refresh
    #[serde(skip_serializing_if = "Option::is_none")]
    pub runtime_settings_changed: Option<bool>,
    /// Human-readable title for the selection UI
    pub title: String,
}

/// Subagent model, reasoning effort, and context tier settings
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentSettingsEntry {
    /// Context tier override for matching subagents
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<SubagentSettingsEntryContextTier>,
    /// Reasoning effort override for matching subagents
    #[serde(skip_serializing_if = "Option::is_none")]
    pub effort_level: Option<String>,
    /// Model override for matching subagents
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
}

/// Subagent settings to apply, or null to clear the live session override
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SubagentSettings {
    /// Per-agent settings keyed by subagent agent_type
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agents: Option<HashMap<String, SubagentSettingsEntry>>,
    /// Names of subagents the user has turned off; they cannot be dispatched
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_subagents: Option<Vec<String>>,
}

/// Schema for the `TaskAgentInfo` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskAgentInfo {
    /// ISO 8601 timestamp when the current active period began
    #[serde(skip_serializing_if = "Option::is_none")]
    pub active_started_at: Option<String>,
    /// Accumulated active execution time in milliseconds
    #[serde(skip_serializing_if = "Option::is_none")]
    pub active_time_ms: Option<i64>,
    /// Type of agent running this task
    pub agent_type: String,
    /// Whether the task is currently in the original sync wait and can be moved to background mode. False once it is already backgrounded, idle, finished, or no longer has a promotable sync waiter.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub can_promote_to_background: Option<bool>,
    /// ISO 8601 timestamp when the task finished
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completed_at: Option<String>,
    /// Short description of the task
    pub description: String,
    /// Error message when the task failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether task execution is synchronously awaited or managed in the background
    #[serde(skip_serializing_if = "Option::is_none")]
    pub execution_mode: Option<TaskExecutionMode>,
    /// Unique task identifier
    pub id: String,
    /// ISO 8601 timestamp when the agent entered idle state
    #[serde(skip_serializing_if = "Option::is_none")]
    pub idle_since: Option<String>,
    /// Most recent response text from the agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub latest_response: Option<String>,
    /// Requested model override for the task when specified
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Most recent prompt delivered to the agent. Updated whenever the agent receives a follow-up message.
    pub prompt: String,
    /// Runtime model resolved for the task when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub resolved_model: Option<String>,
    /// Result text from the task when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<String>,
    /// ISO 8601 timestamp when the task was started
    pub started_at: String,
    /// Current lifecycle status of the task
    pub status: TaskStatus,
    /// Tool call ID associated with this agent task
    pub tool_call_id: String,
    /// Task kind
    pub r#type: TaskAgentInfoType,
}

/// Schema for the `TaskProgressLine` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskProgressLine {
    /// Display message, e.g., "▸ bash", "✓ edit src/foo.ts"
    pub message: String,
    /// ISO 8601 timestamp when this event occurred
    pub timestamp: String,
}

/// Schema for the `TaskAgentProgress` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskAgentProgress {
    /// The most recent intent reported by the agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub latest_intent: Option<String>,
    /// Recent tool execution events converted to display lines
    pub recent_activity: Vec<TaskProgressLine>,
    /// Progress kind
    pub r#type: TaskAgentProgressType,
}

/// Background tasks currently tracked by the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskList {
    /// Currently tracked tasks
    pub tasks: Vec<serde_json::Value>,
}

/// Identifier of the background task to cancel.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksCancelRequest {
    /// Task identifier
    pub id: String,
}

/// Indicates whether the background task was successfully cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksCancelResult {
    /// Whether the task was successfully cancelled
    pub cancelled: bool,
}

/// The first sync-waiting task that can currently be promoted to background mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksGetCurrentPromotableResult {
    /// The first sync-waiting task (agent first, then shell) that can currently be promoted to background mode. Omitted if no such task exists. The returned task is guaranteed to have executionMode='sync' and canPromoteToBackground=true at the time of the call.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task: Option<serde_json::Value>,
}

/// Identifier of the background task to fetch progress for.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksGetProgressRequest {
    /// Task identifier (agent ID or shell ID)
    pub id: String,
}

/// Progress information for the task, or null when no task with that ID is tracked.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksGetProgressResult {
    /// Progress information for the task, discriminated by type. Returns null when no task with this ID is currently tracked.
    pub progress: Option<serde_json::Value>,
}

/// Schema for the `TaskShellInfo` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskShellInfo {
    /// Whether the shell runs inside a managed PTY session or as an independent background process
    pub attachment_mode: TaskShellInfoAttachmentMode,
    /// Whether this shell task can be promoted to background mode
    #[serde(skip_serializing_if = "Option::is_none")]
    pub can_promote_to_background: Option<bool>,
    /// Command being executed
    pub command: String,
    /// ISO 8601 timestamp when the task finished
    #[serde(skip_serializing_if = "Option::is_none")]
    pub completed_at: Option<String>,
    /// Short description of the task
    pub description: String,
    /// Whether task execution is synchronously awaited or managed in the background
    #[serde(skip_serializing_if = "Option::is_none")]
    pub execution_mode: Option<TaskExecutionMode>,
    /// Unique task identifier
    pub id: String,
    /// Path to the detached shell log, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub log_path: Option<String>,
    /// Process ID when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pid: Option<i64>,
    /// ISO 8601 timestamp when the task was started
    pub started_at: String,
    /// Current lifecycle status of the task
    pub status: TaskStatus,
    /// Task kind
    pub r#type: TaskShellInfoType,
}

/// Schema for the `TaskShellProgress` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TaskShellProgress {
    /// Process ID when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub pid: Option<i64>,
    /// Recent stdout/stderr lines from the running shell command
    pub recent_output: String,
    /// Progress kind
    pub r#type: TaskShellProgressType,
}

/// The promoted task as it now exists in background mode, omitted if no promotable task was waiting.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksPromoteCurrentToBackgroundResult {
    /// The promoted task as it now exists in background mode, omitted if no promotable task was waiting. Atomic operation: avoids the race window of getCurrentPromotable + promoteToBackground.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task: Option<serde_json::Value>,
}

/// Identifier of the task to promote to background mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksPromoteToBackgroundRequest {
    /// Task identifier
    pub id: String,
}

/// Indicates whether the task was successfully promoted to background mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksPromoteToBackgroundResult {
    /// Whether the task was successfully promoted to background mode
    pub promoted: bool,
}

/// Refresh metadata for any detached background shells the runtime knows about. Use after a long pause to pick up exit/output state for shells running outside the agent loop.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksRefreshResult {}

/// Identifier of the completed or cancelled task to remove from tracking.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksRemoveRequest {
    /// Task identifier
    pub id: String,
}

/// Indicates whether the task was removed. False when the task does not exist or is still running/idle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksRemoveResult {
    /// Whether the task was removed. Returns false if the task does not exist or is still running/idle (cancel it first).
    pub removed: bool,
}

/// Identifier of the target agent task, message content, and optional sender agent ID.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksSendMessageRequest {
    /// Agent ID of the sender, if sent on behalf of another agent
    #[serde(skip_serializing_if = "Option::is_none")]
    pub from_agent_id: Option<String>,
    /// Agent task identifier
    pub id: String,
    /// Message content to send to the agent
    pub message: String,
}

/// Indicates whether the message was delivered, with an error message when delivery failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksSendMessageResult {
    /// Error message if delivery failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether the message was successfully delivered or steered
    pub sent: bool,
}

/// Agent type, prompt, name, and optional description and model override for the new task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksStartAgentRequest {
    /// Type of agent to start (e.g., 'explore', 'task', 'general-purpose')
    pub agent_type: String,
    /// Short description of the task
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Optional model override
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
    /// Short name for the agent, used to generate a human-readable ID
    pub name: String,
    /// Task prompt for the agent
    pub prompt: String,
}

/// Identifier assigned to the newly started background agent task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksStartAgentResult {
    /// Generated agent ID for the background task
    pub agent_id: String,
}

/// Wait until all in-flight background tasks (agents + shells) and any follow-up turns scheduled by their completions have settled. Returns when the runtime is fully drained or after an internal timeout (default 10 minutes; configurable via COPILOT_TASK_WAIT_TIMEOUT_SECONDS).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TasksWaitForPendingResult {}

/// Feature override key/value pairs to attach to subsequent telemetry events from this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TelemetrySetFeatureOverridesRequest {
    /// Override key/value pairs to attach to subsequent telemetry events from this session. Replaces any previously-set overrides.
    pub features: HashMap<String, String>,
}

/// Schema for the `TokenAuthInfo` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TokenAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Authentication host.
    pub host: String,
    /// The token value itself. Treat as a secret.
    pub token: String,
    /// SDK-side token authentication; the host configured the token directly via the SDK.
    pub r#type: TokenAuthInfoType,
}

/// Schema for the `Tool` type.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Tool {
    /// Description of what the tool does
    pub description: String,
    /// Optional instructions for how to use this tool effectively
    #[serde(skip_serializing_if = "Option::is_none")]
    pub instructions: Option<String>,
    /// Tool identifier (e.g., "bash", "grep", "str_replace_editor")
    pub name: String,
    /// Optional namespaced name for declarative filtering (e.g., "playwright/navigate" for MCP tools)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub namespaced_name: Option<String>,
    /// JSON Schema for the tool's input parameters
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parameters: Option<HashMap<String, serde_json::Value>>,
}

/// Built-in tools available for the requested model, with their parameters and instructions.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolList {
    /// List of available built-in tools with metadata
    pub tools: Vec<Tool>,
}

/// Current lightweight tool metadata snapshot for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsGetCurrentMetadataResult {
    /// Current tool metadata, or null when tools have not been initialized yet
    pub tools: Option<Vec<CurrentToolMetadata>>,
}

/// Resolve, build, and validate the runtime tool list for this session. Subagent sessions and consumer flows that need an initialized tool set before `send` invoke this. Default base-class implementation is a no-op for sessions that don't support tool validation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsInitializeAndValidateResult {}

/// Optional model identifier whose tool overrides should be applied to the listing.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsListRequest {
    /// Optional model ID — when provided, the returned tool list reflects model-specific overrides
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model: Option<String>,
}

/// Empty result after applying subagent settings
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsUpdateSubagentSettingsResult {}

/// Schema for the `UIElicitationArrayAnyOfFieldItemsAnyOf` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayAnyOfFieldItemsAnyOf {
    /// Value submitted when this option is selected.
    pub r#const: String,
    /// Display label for this option.
    pub title: String,
}

/// Schema applied to each item in the array.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayAnyOfFieldItems {
    /// Selectable options, each with a value and a display label.
    pub any_of: Vec<UIElicitationArrayAnyOfFieldItemsAnyOf>,
}

/// Multi-select string field where each option pairs a value with a display label.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayAnyOfField {
    /// Default values selected when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<Vec<String>>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Schema applied to each item in the array.
    pub items: UIElicitationArrayAnyOfFieldItems,
    /// Maximum number of items the user may select.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_items: Option<i64>,
    /// Minimum number of items the user must select.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub min_items: Option<i64>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "array".
    pub r#type: UIElicitationArrayAnyOfFieldType,
}

/// Schema applied to each item in the array.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayEnumFieldItems {
    /// Allowed string values for each selected item.
    pub r#enum: Vec<String>,
    /// Type discriminator. Always "string".
    pub r#type: UIElicitationArrayEnumFieldItemsType,
}

/// Multi-select string field whose allowed values are defined inline.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationArrayEnumField {
    /// Default values selected when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<Vec<String>>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Schema applied to each item in the array.
    pub items: UIElicitationArrayEnumFieldItems,
    /// Maximum number of items the user may select.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_items: Option<i64>,
    /// Minimum number of items the user must select.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub min_items: Option<i64>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "array".
    pub r#type: UIElicitationArrayEnumFieldType,
}

/// JSON Schema describing the form fields to present to the user
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchema {
    /// Form field definitions, keyed by field name
    pub properties: HashMap<String, serde_json::Value>,
    /// List of required field names
    #[serde(skip_serializing_if = "Option::is_none")]
    pub required: Option<Vec<String>>,
    /// Schema type indicator (always 'object')
    pub r#type: UIElicitationSchemaType,
}

/// Prompt message and JSON schema describing the form fields to elicit from the user.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationRequest {
    /// Message describing what information is needed from the user
    pub message: String,
    /// JSON Schema describing the form fields to present to the user
    pub requested_schema: UIElicitationSchema,
}

/// The elicitation response (accept with form values, decline, or cancel)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationResponse {
    /// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
    pub action: UIElicitationResponseAction,
    /// The form values submitted by the user (present when action is 'accept')
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<HashMap<String, serde_json::Value>>,
}

/// Indicates whether the elicitation response was accepted; false if it was already resolved by another client.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationResult {
    /// Whether the response was accepted. False if the request was already resolved by another client.
    pub success: bool,
}

/// Boolean field rendered as a yes/no toggle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchemaPropertyBoolean {
    /// Default value selected when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<bool>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "boolean".
    pub r#type: UIElicitationSchemaPropertyBooleanType,
}

/// Numeric field accepting either a number or an integer.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchemaPropertyNumber {
    /// Default value populated in the input when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<f64>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Maximum allowed value (inclusive).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub maximum: Option<f64>,
    /// Minimum allowed value (inclusive).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub minimum: Option<f64>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Numeric type accepted by the field.
    pub r#type: UIElicitationSchemaPropertyNumberType,
}

/// Free-text string field with optional length and format constraints.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationSchemaPropertyString {
    /// Default value populated in the input when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<String>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Optional format hint that constrains the accepted input.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub format: Option<UIElicitationSchemaPropertyStringFormat>,
    /// Maximum number of characters allowed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_length: Option<i64>,
    /// Minimum number of characters required.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub min_length: Option<i64>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "string".
    pub r#type: UIElicitationSchemaPropertyStringType,
}

/// Single-select string field whose allowed values are defined inline.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationStringEnumField {
    /// Default value selected when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<String>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Allowed string values.
    pub r#enum: Vec<String>,
    /// Optional display labels for each enum value, in the same order as `enum`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub enum_names: Option<Vec<String>>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "string".
    pub r#type: UIElicitationStringEnumFieldType,
}

/// Schema for the `UIElicitationStringOneOfFieldOneOf` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationStringOneOfFieldOneOf {
    /// Value submitted when this option is selected.
    pub r#const: String,
    /// Display label for this option.
    pub title: String,
}

/// Single-select string field where each option pairs a value with a display label.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIElicitationStringOneOfField {
    /// Default value selected when the form is first shown.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub default: Option<String>,
    /// Help text describing the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// Selectable options, each with a value and a display label.
    pub one_of: Vec<UIElicitationStringOneOfFieldOneOf>,
    /// Human-readable label for the field.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// Type discriminator. Always "string".
    pub r#type: UIElicitationStringOneOfFieldType,
}

/// Transient question to answer without adding it to conversation history.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIEphemeralQueryRequest {
    /// In-process `AbortSignal` forwarded to the model client to cancel an in-flight request. Marked internal: excluded from the public SDK surface. Replaced by an explicit cancellation token + cancel RPC in the SDK migration.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) abort_signal: Option<serde_json::Value>,
    /// In-process streaming callback `(text) => void` invoked with each token as the model emits it. Marked internal: excluded from the public SDK surface. In a process-separated SDK this is replaced by a streaming RPC that yields chunks and a final answer.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) on_chunk: Option<serde_json::Value>,
    /// Question to answer from the current conversation context.
    pub question: String,
}

/// Transient answer generated from current conversation context.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIEphemeralQueryResult {
    /// Full assistant response text.
    pub answer: String,
}

/// Schema for the `UIExitPlanModeResponse` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIExitPlanModeResponse {
    /// Whether the plan was approved.
    pub approved: bool,
    /// Whether subsequent edits should be auto-approved without confirmation.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auto_approve_edits: Option<bool>,
    /// Feedback from the user when they declined the plan or requested changes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub feedback: Option<String>,
    /// The action the user selected. Defaults to 'autopilot' when autoApproveEdits is true, otherwise 'interactive'.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_action: Option<UIExitPlanModeAction>,
}

/// Request ID of a pending `auto_mode_switch.requested` event and the user's response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingAutoModeSwitchRequest {
    /// The unique request ID from the auto_mode_switch.requested event
    pub request_id: RequestId,
    /// User's choice for auto-mode switching: yes (allow this turn), yes_always (allow + persist as setting), or no (decline).
    pub response: UIAutoModeSwitchResponse,
}

/// Pending elicitation request ID and the user's response (accept/decline/cancel + form values).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingElicitationRequest {
    /// The unique request ID from the elicitation.requested event
    pub request_id: RequestId,
    /// The elicitation response (accept with form values, decline, or cancel)
    pub result: UIElicitationResponse,
}

/// Request ID of a pending `exit_plan_mode.requested` event and the user's response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingExitPlanModeRequest {
    /// The unique request ID from the exit_plan_mode.requested event
    pub request_id: RequestId,
    /// Schema for the `UIExitPlanModeResponse` type.
    pub response: UIExitPlanModeResponse,
}

/// Indicates whether the pending UI request was resolved by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingResult {
    /// True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
    pub success: bool,
}

/// Optional sampling result payload. Omit to reject/cancel the sampling request without providing a result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingSamplingResponse {}

/// Request ID of a pending `sampling.requested` event and an optional sampling result payload (omit to reject).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingSamplingRequest {
    /// The unique request ID from the sampling.requested event
    pub request_id: RequestId,
    /// Optional sampling result payload. Omit to reject/cancel the sampling request without providing a result.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub response: Option<UIHandlePendingSamplingResponse>,
}

/// Schema for the `UIUserInputResponse` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIUserInputResponse {
    /// The user's answer text
    pub answer: String,
    /// True if the user typed a freeform response, false if they selected a presented choice. Used by telemetry to differentiate between free text input and choice selection.
    pub was_freeform: bool,
}

/// Request ID of a pending `user_input.requested` event and the user's response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIHandlePendingUserInputRequest {
    /// The unique request ID from the user_input.requested event
    pub request_id: RequestId,
    /// Schema for the `UIUserInputResponse` type.
    pub response: UIUserInputResponse,
}

/// Register an in-process handler for `auto_mode_switch.requested` events. The caller still attaches the actual listener via the standard event-subscription mechanism; this registration solely tells the server bridge to skip its own dispatch (so a remote client doesn't race the in-process handler for the same requestId).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIRegisterDirectAutoModeSwitchHandlerResult {
    /// Opaque handle representing the registration. Pass this same handle to `unregisterDirectAutoModeSwitchHandler` when the in-process handler is no longer active. Multiple registrations are reference-counted; the server bridge will only dispatch auto-mode-switch requests when no handles are active.
    pub handle: String,
}

/// Opaque handle previously returned by `registerDirectAutoModeSwitchHandler` to release.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIUnregisterDirectAutoModeSwitchHandlerRequest {
    /// Handle previously returned by `registerDirectAutoModeSwitchHandler`
    pub handle: String,
}

/// Indicates whether the handle was active and the registration count was decremented.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UIUnregisterDirectAutoModeSwitchHandlerResult {
    /// True if the handle was active and decremented the counter; false if the handle was unknown.
    pub unregistered: bool,
}

/// Configured per-agent subagent overrides
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UpdateSubagentSettingsRequestSubagents {
    /// Per-agent settings keyed by subagent agent_type
    #[serde(skip_serializing_if = "Option::is_none")]
    pub agents: Option<HashMap<String, SubagentSettingsEntry>>,
    /// Names of subagents the user has turned off; they cannot be dispatched
    #[serde(skip_serializing_if = "Option::is_none")]
    pub disabled_subagents: Option<Vec<String>>,
}

/// Subagent settings to apply to the current session
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UpdateSubagentSettingsRequest {
    /// Subagent settings to apply, or null to clear the live session override
    pub subagents: Option<UpdateSubagentSettingsRequestSubagents>,
}

/// Aggregated code change metrics
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsCodeChanges {
    /// Distinct file paths modified during the session
    pub files_modified: Vec<String>,
    /// Number of distinct files modified
    pub files_modified_count: i64,
    /// Total lines of code added
    pub lines_added: i64,
    /// Total lines of code removed
    pub lines_removed: i64,
}

/// Request count and cost metrics for this model
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsModelMetricRequests {
    /// User-initiated premium request cost (with multiplier applied)
    pub cost: f64,
    /// Number of API requests made with this model
    pub count: i64,
}

/// Schema for the `UsageMetricsModelMetricTokenDetail` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsModelMetricTokenDetail {
    /// Accumulated token count for this token type
    pub token_count: i64,
}

/// Token usage metrics for this model
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsModelMetricUsage {
    /// Total tokens read from prompt cache
    pub cache_read_tokens: i64,
    /// Total tokens written to prompt cache
    pub cache_write_tokens: i64,
    /// Total input tokens consumed
    pub input_tokens: i64,
    /// Total output tokens produced
    pub output_tokens: i64,
    /// Total output tokens used for reasoning
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_tokens: Option<i64>,
}

/// Schema for the `UsageMetricsModelMetric` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsModelMetric {
    /// Request count and cost metrics for this model
    pub requests: UsageMetricsModelMetricRequests,
    /// Token count details per type
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token_details: Option<HashMap<String, UsageMetricsModelMetricTokenDetail>>,
    /// Accumulated nano-AI units cost for this model
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_nano_aiu: Option<f64>,
    /// Token usage metrics for this model
    pub usage: UsageMetricsModelMetricUsage,
}

/// Schema for the `UsageMetricsTokenDetail` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageMetricsTokenDetail {
    /// Accumulated token count for this token type
    pub token_count: i64,
}

/// Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UsageGetMetricsResult {
    /// Aggregated code change metrics
    pub code_changes: UsageMetricsCodeChanges,
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub current_model: Option<String>,
    /// Input tokens from the most recent main-agent API call
    pub last_call_input_tokens: i64,
    /// Output tokens from the most recent main-agent API call
    pub last_call_output_tokens: i64,
    /// Per-model token and request metrics, keyed by model identifier
    pub model_metrics: HashMap<String, UsageMetricsModelMetric>,
    /// ISO 8601 timestamp when the session started
    pub session_start_time: String,
    /// Session-wide per-token-type accumulated token counts
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token_details: Option<HashMap<String, UsageMetricsTokenDetail>>,
    /// Total time spent in model API calls (milliseconds)
    pub total_api_duration_ms: i64,
    /// Session-wide accumulated nano-AI units cost
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_nano_aiu: Option<f64>,
    /// Total user-initiated premium request cost across all models (may be fractional due to multipliers)
    pub total_premium_request_cost: f64,
    /// Raw count of user-initiated API requests
    pub total_user_requests: i64,
}

/// Schema for the `UserAuthInfo` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserAuthInfo {
    /// Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_user: Option<CopilotUserResponse>,
    /// Authentication host.
    pub host: String,
    /// OAuth user login.
    pub login: String,
    /// OAuth user authentication. The token itself is held in the runtime's secret token store (keyed by host+login) and is NOT carried in this struct.
    pub r#type: UserAuthInfoType,
}

/// Result of a user-requested shell command.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UserRequestedShellCommandResult {
    /// Error output when the execution failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Process exit code, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exit_code: Option<i64>,
    /// Captured command output
    pub output: String,
    /// Whether the command completed successfully
    pub success: bool,
    /// Tool call id emitted for the shell execution
    pub tool_call_id: String,
}

/// A single changed file and its unified diff.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspaceDiffFileChange {
    /// Type of change represented by this file diff.
    pub change_type: WorkspaceDiffFileChangeType,
    /// Unified diff content for the file. Empty when the diff was truncated.
    pub diff: String,
    /// Whether the diff content was omitted because it exceeded the per-file size limit.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub is_truncated: Option<bool>,
    /// Original file path for renamed files.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub old_path: Option<String>,
    /// Path to the changed file, relative to the workspace root.
    pub path: String,
}

/// Workspace diff result for the requested mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspaceDiffResult {
    /// Default branch used for a branch diff, when branch mode was requested.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_branch: Option<String>,
    /// Changed files and their unified diffs.
    pub changes: Vec<WorkspaceDiffFileChange>,
    /// Whether a requested branch diff fell back to unstaged changes because branch diff failed.
    pub is_fallback: bool,
    /// Effective mode used for the returned changes.
    pub mode: WorkspaceDiffMode,
    /// Diff mode requested by the client.
    pub requested_mode: WorkspaceDiffMode,
}

/// Schema for the `WorkspacesCheckpoints` type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesCheckpoints {
    /// Filename of the checkpoint within the workspace checkpoints directory
    pub filename: String,
    /// Checkpoint number assigned by the workspace manager
    pub number: i64,
    /// Human-readable checkpoint title
    pub title: String,
}

/// Relative path and UTF-8 content for the workspace file to create or overwrite.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesCreateFileRequest {
    /// File content to write as a UTF-8 string
    pub content: String,
    /// Relative path within the workspace files directory
    pub path: String,
}

/// Parameters for computing a workspace diff.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesDiffRequest {
    /// When true, ignore whitespace-only changes (git `--ignore-all-space`). Defaults to false.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub ignore_whitespace: Option<bool>,
    /// Diff mode requested by the client.
    pub mode: WorkspaceDiffMode,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesGetWorkspaceResultWorkspace {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    #[serde(
        rename = "chronicle_sync_dismissed",
        skip_serializing_if = "Option::is_none"
    )]
    pub chronicle_sync_dismissed: Option<bool>,
    #[serde(rename = "client_name", skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspacesWorkspaceDetailsHostType>,
    pub id: String,
    #[serde(rename = "mc_last_event_id", skip_serializing_if = "Option::is_none")]
    pub mc_last_event_id: Option<String>,
    #[serde(rename = "mc_session_id", skip_serializing_if = "Option::is_none")]
    pub mc_session_id: Option<String>,
    #[serde(rename = "mc_task_id", skip_serializing_if = "Option::is_none")]
    pub mc_task_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(rename = "remote_steerable", skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    #[serde(rename = "summary_count", skip_serializing_if = "Option::is_none")]
    pub summary_count: Option<i64>,
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Current workspace metadata for the session, including its absolute filesystem path when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesGetWorkspaceResult {
    /// Absolute filesystem path to the workspace directory. Omitted when the session has no workspace (e.g. remote sessions).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Current workspace metadata, or null if not available
    pub workspace: Option<WorkspacesGetWorkspaceResultWorkspace>,
}

/// Workspace checkpoints in chronological order; empty when the workspace is not enabled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesListCheckpointsResult {
    /// Workspace checkpoints in chronological order. Empty when workspace is not enabled.
    pub checkpoints: Vec<WorkspacesCheckpoints>,
}

/// Relative paths of files stored in the session workspace files directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesListFilesResult {
    /// Relative file paths in the workspace files directory
    pub files: Vec<String>,
}

/// Checkpoint number to read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadCheckpointRequest {
    /// Checkpoint number to read
    pub number: i64,
}

/// Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadCheckpointResult {
    /// Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing
    pub content: Option<String>,
}

/// Relative path of the workspace file to read.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadFileRequest {
    /// Relative path within the workspace files directory
    pub path: String,
}

/// Contents of the requested workspace file as a UTF-8 string.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesReadFileResult {
    /// File content as a UTF-8 string
    pub content: String,
}

/// Pasted content to save as a UTF-8 file in the session workspace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesSaveLargePasteRequest {
    /// Pasted content to save as a UTF-8 file
    pub content: String,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesSaveLargePasteResultSaved {
    /// Filename within the workspace files directory
    pub filename: String,
    /// Absolute filesystem path to the saved paste file
    pub file_path: String,
    /// Size of the saved file in bytes
    pub size_bytes: i64,
}

/// Descriptor for the saved paste file, or null when the workspace is unavailable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspacesSaveLargePasteResult {
    /// Saved-paste descriptor, or null when the workspace is unavailable (e.g. CCA runtime, non-infinite sessions, remote sessions)
    pub saved: Option<WorkspacesSaveLargePasteResultSaved>,
}

/// Public-facing workspace metadata for this session, or null if the session has no associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count, internal flags).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WorkspaceSummary {
    /// Branch checked out at session start, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// ISO 8601 timestamp when the workspace was created
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    /// Current working directory at session start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Resolved git root for cwd, if any
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Repository host type, if known
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspaceSummaryHostType>,
    /// Workspace identifier (1:1 with sessionId)
    pub id: String,
    /// Display name for the session, if set
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Repository identifier in 'owner/repo' or 'org/project/repo' format, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// ISO 8601 timestamp when the workspace was last updated
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    /// Whether the display name was explicitly set by the user
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// List of Copilot models available to the resolved user, including capabilities and billing metadata.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ModelsListResult {
    /// List of available models with full metadata
    pub models: Vec<Model>,
}

/// Built-in tools available for the requested model, with their parameters and instructions.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ToolsListResult {
    /// List of available built-in tools with metadata
    pub tools: Vec<Tool>,
}

/// User-configured MCP servers, keyed by server name.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct McpConfigListResult {
    /// All MCP servers from user config, keyed by name
    pub servers: HashMap<String, serde_json::Value>,
}

/// Plugins installed in user/global state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsListResult {
    /// Installed plugins
    pub plugins: Vec<InstalledPluginInfo>,
}

/// Result of installing a plugin.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsInstallResult {
    /// Set when the install path is deprecated (e.g. direct repo / URL / local installs). Callers should surface this to end users.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deprecation_warning: Option<String>,
    /// The newly installed plugin's metadata
    pub plugin: InstalledPluginInfo,
    /// Optional post-install message provided by the plugin (e.g. setup instructions)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub post_install_message: Option<String>,
    /// Number of skills discovered and installed from the plugin
    pub skills_installed: i64,
}

/// Result of updating a single plugin.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsUpdateResult {
    /// Version after the update, when reported by the plugin manifest
    #[serde(skip_serializing_if = "Option::is_none")]
    pub new_version: Option<String>,
    /// Version that was previously installed, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_version: Option<String>,
    /// Number of skills discovered and installed after the update
    pub skills_installed: i64,
}

/// Result of updating all installed plugins.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsUpdateAllResult {
    /// Per-plugin update results in deterministic order.
    pub results: Vec<PluginUpdateAllEntry>,
}

/// All registered marketplaces, including built-in defaults.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsMarketplacesListResult {
    /// Registered marketplaces
    pub marketplaces: Vec<MarketplaceInfo>,
}

/// Result of registering a new marketplace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsMarketplacesAddResult {
    /// Final name of the marketplace as resolved from its manifest
    pub name: String,
}

/// Outcome of the remove attempt, including dependent-plugin info when applicable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsMarketplacesRemoveResult {
    /// Names of installed plugins that prevented removal. Populated only when `removed=false`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub dependent_plugins: Option<Vec<String>>,
    /// True when the marketplace was actually removed. False when removal was skipped because the marketplace has dependent plugins and `force` was not set.
    pub removed: bool,
}

/// Plugins advertised by the marketplace.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsMarketplacesBrowseResult {
    /// Plugins advertised by the marketplace
    pub plugins: Vec<MarketplacePluginInfo>,
}

/// Result of refreshing one or more marketplace catalogs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PluginsMarketplacesRefreshResult {
    /// Per-marketplace refresh results in deterministic order.
    pub results: Vec<MarketplaceRefreshEntry>,
}

/// Skills discovered across global and project sources.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsDiscoverResult {
    /// All discovered skills across all sources
    pub skills: Vec<ServerSkill>,
}

/// Canonical locations where skills can be created so the runtime will recognize them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SkillsGetDiscoveryPathsResult {
    /// Canonical skill create/discovery directories, in priority order
    pub paths: Vec<SkillDiscoveryPath>,
}

/// Agents discovered across user, project, plugin, and remote sources.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentsDiscoverResult {
    /// All discovered agents across all sources
    pub agents: Vec<AgentInfo>,
}

/// Canonical locations where custom agents can be created so the runtime will recognize them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AgentsGetDiscoveryPathsResult {
    /// Canonical agent create/discovery directories, in priority order
    pub paths: Vec<AgentDiscoveryPath>,
}

/// Instruction sources discovered across user, repository, and plugin sources.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsDiscoverResult {
    /// All discovered instruction sources
    pub sources: Vec<InstructionSource>,
}

/// Canonical files and directories where custom instructions can be created so the runtime will recognize them.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstructionsGetDiscoveryPathsResult {
    /// Canonical instruction create/discovery files and directories, in priority order
    pub paths: Vec<InstructionDiscoveryPath>,
}

/// Result of opening a session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsOpenResult {
    /// Remote session metadata, present when status is `connected`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub metadata: Option<RemoteSessionMetadataValue>,
    /// Handoff progress steps, present when status is `handed_off`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub progress: Option<Vec<SessionsOpenProgress>>,
    /// Remote session ID, present when status is `connected`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_session_id: Option<SessionId>,
    /// In-process SessionClientApi handle for the opened session, returned to CLI callers as a transitional shortcut. Marked internal so the public SDK surface does not expose it; SDK consumers should construct per-session clients from `sessionId` instead.
    #[doc(hidden)]
    #[serde(skip_serializing_if = "Option::is_none")]
    pub(crate) session_api: Option<serde_json::Value>,
    /// Opened session ID. Omitted when status is `not_found`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
    /// Startup prompts queued by user-level hook configs at session creation. Only populated when status is `created`; resumed sessions return an empty array.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub startup_prompts: Option<Vec<String>>,
    /// Outcome of the open request.
    pub status: SessionsOpenStatus,
}

/// Remote session connection result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsConnectResult {
    /// Metadata for a connected remote session.
    pub metadata: ConnectedRemoteSessionMetadata,
    /// SDK session ID for the connected remote session.
    pub session_id: SessionId,
}

/// Sessions matching the filter, ordered most-recently-modified first.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsListResult {
    /// Sessions ordered most-recently-modified first. Discriminated by `isRemote`.
    pub sessions: Vec<serde_json::Value>,
}

/// ID of the local session bound to the given GitHub task, or omitted when none.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsFindByTaskIdResult {
    /// Omitted when no local session is bound to that GitHub task
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_id: Option<SessionId>,
}

/// Map of sessionId -> on-disk size in bytes for each session's workspace directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetSizesResult {
    /// Map of sessionId -> on-disk size in bytes for the session's workspace directory
    pub sizes: HashMap<String, i64>,
}

/// Map of sessionId -> bytes freed by removing the session's workspace directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsBulkDeleteResult {
    /// Map of sessionId -> bytes freed by removing the session's workspace directory. Sessions whose deletion failed are omitted from this map (failures are logged on the server but not surfaced per-id; check the map for absent IDs to detect them).
    pub freed_bytes: HashMap<String, i64>,
}

/// Outcome of the prune operation: deleted IDs, dry-run candidates, skipped IDs, total bytes freed, and the dry-run flag.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsPruneOldResult {
    /// Session IDs that would be deleted in dry-run mode (always empty otherwise)
    pub candidates: Vec<String>,
    /// Session IDs that were deleted (always empty in dry-run mode)
    pub deleted: Vec<String>,
    /// True when no deletions were actually performed
    pub dry_run: bool,
    /// Total bytes freed (actual when not dry-run, projected when dry-run)
    pub freed_bytes: i64,
    /// Session IDs that were skipped (e.g., named sessions)
    pub skipped: Vec<String>,
}

/// The enriched metadata records, with summary and context fields backfilled where available. Sessions confirmed empty and unnamed are omitted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsEnrichMetadataResult {
    /// Enriched records, with summary and context backfilled. Sessions confirmed empty and unnamed may be omitted.
    pub sessions: Vec<LocalSessionMetadataValue>,
}

/// Queued repo-level startup prompts and the total hook command count after loading.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsLoadDeferredRepoHooksResult {
    /// Total hook command count (user + plugin + repo) loaded for the session by this call. Captured atomically with startupPrompts so callers don't need to read a separate counter.
    pub hook_count: i64,
    /// Repo-level startup prompts queued from repo hook configs. Empty on resume, when no repo configs were pending, or when disableAllHooks is set.
    pub startup_prompts: Vec<String>,
}

/// Wrapper for the singleton's current status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsStartRemoteControlResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
}

/// Outcome of a transferRemoteControl call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsTransferRemoteControlResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
    /// Whether the rebinding actually happened.
    pub transferred: bool,
}

/// Wrapper for the singleton's current status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsSetRemoteControlSteeringResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
}

/// Outcome of a stopRemoteControl call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsStopRemoteControlResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
    /// Whether the singleton was actually torn down by this call.
    pub stopped: bool,
}

/// Wrapper for the singleton's current status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsGetRemoteControlStatusResult {
    /// State of the runtime-managed remote-control singleton.
    pub status: serde_json::Value,
}

/// Batch of spawn events plus a cursor for follow-up polls.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionsPollSpawnedSessionsResult {
    /// Opaque cursor to pass back to receive only events after this batch.
    pub cursor: String,
    /// Spawn events emitted since the supplied cursor.
    pub events: Vec<SessionsPollSpawnedSessionsEvent>,
}

/// Handle for releasing the extension tool registration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(crate) struct SessionsRegisterExtensionToolsOnSessionResult {
    /// In-process unsubscribe function (CLI-only optimization). Marked internal: replaced by an explicit `extensions.unregister` RPC in the SDK migration.
    #[doc(hidden)]
    pub(crate) unsubscribe: serde_json::Value,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSuspendParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Result of sending a user message
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSendResult {
    /// Unique identifier assigned to the message
    pub message_id: String,
}

/// Result of aborting the current turn
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAbortResult {
    /// Error message if the abort failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether the abort completed successfully
    pub success: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAuthGetStatusParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Authentication status and account metadata for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAuthGetStatusResult {
    /// Authentication type
    #[serde(skip_serializing_if = "Option::is_none")]
    pub auth_type: Option<AuthInfoType>,
    /// Copilot plan tier (e.g., individual_pro, business)
    #[serde(skip_serializing_if = "Option::is_none")]
    pub copilot_plan: Option<String>,
    /// Authentication host URL
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<String>,
    /// Whether the session has resolved authentication
    pub is_authenticated: bool,
    /// Authenticated login/username, if available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub login: Option<String>,
    /// Human-readable authentication status description
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status_message: Option<String>,
}

/// Indicates whether the credential update succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAuthSetCredentialsResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCanvasListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Declared canvases available in this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCanvasListResult {
    /// Declared canvases available in this session
    pub canvases: Vec<DiscoveredCanvas>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCanvasListOpenParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Live open-canvas snapshot.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCanvasListOpenResult {
    /// Currently open canvas instances
    pub open_canvases: Vec<OpenCanvasInstance>,
}

/// Open canvas instance snapshot.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCanvasOpenResult {
    /// Runtime-controlled routing state for an open canvas instance.
    pub availability: CanvasInstanceAvailability,
    /// Provider-local canvas identifier
    pub canvas_id: String,
    /// Owning provider identifier
    pub extension_id: String,
    /// Owning extension display name, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub extension_name: Option<String>,
    /// Input supplied when the instance was opened
    #[serde(skip_serializing_if = "Option::is_none")]
    pub input: Option<serde_json::Value>,
    /// Stable caller-supplied canvas instance identifier
    pub instance_id: String,
    /// Whether this snapshot came from an idempotent reopen
    pub reopen: bool,
    /// Provider-supplied status text
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// Rendered title
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// URL for web-rendered canvases
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Canvas action invocation result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCanvasActionInvokeResult {
    /// Provider-supplied action result
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<serde_json::Value>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelGetCurrentParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The currently selected model, reasoning effort, and context tier for the session. The context tier reflects `Session.getContextTier()`, restored from the session journal on resume.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelGetCurrentResult {
    /// Context tier for models that support multiple context-window sizes.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_tier: Option<ContextTier>,
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
    /// Reasoning effort level currently applied to the active model, when one is set. Reads `Session.getReasoningEffort()` synchronously after `getSelectedModel()` resolves so the two values are reported as a snapshot.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reasoning_effort: Option<String>,
}

/// The model identifier active on the session after the switch.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelSwitchToResult {
    /// Currently active model identifier after the switch
    #[serde(skip_serializing_if = "Option::is_none")]
    pub model_id: Option<String>,
}

/// Update the session's reasoning effort without changing the selected model. Use `switchTo` instead when you also need to change the model. The runtime stores the effort on the session and applies it to subsequent turns.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelSetReasoningEffortResult {
    /// Reasoning effort level recorded on the session after the update
    pub reasoning_effort: String,
}

/// The list of models available to this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModelListResult {
    /// Available models, ordered with the most preferred default first. Includes both Copilot (CAPI) models and any registry BYOK models; a BYOK model appears under its provider-qualified selection id (`provider/id`).
    pub list: Vec<serde_json::Value>,
    /// Per-quota snapshots returned alongside the model list, keyed by quota type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub quota_snapshots: Option<HashMap<String, serde_json::Value>>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionModeGetParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionNameGetParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The session's friendly name, or null when not yet set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionNameGetResult {
    /// The session name (user-set or auto-generated), or null if not yet set
    pub name: Option<String>,
}

/// Indicates whether the auto-generated summary was applied as the session's name.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionNameSetAutoResult {
    /// Whether the auto-generated summary was persisted. False if the session already has a user-set name, the summary normalized to empty, or the session does not have a workspace.
    pub applied: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Existence, contents, and resolved path of the session plan file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadResult {
    /// The content of the plan file, or null if it does not exist
    pub content: Option<String>,
    /// Whether the plan file exists in the workspace
    pub exists: bool,
    /// Absolute file path of the plan file, or null if workspace is not enabled
    pub path: Option<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanDeleteParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadSqlTodosParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Todo rows read from the session SQL database. Empty when no session database is available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadSqlTodosResult {
    /// Rows from the session SQL todos table, ordered by creation time and id.
    pub rows: Vec<PlanSqlTodosRow>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadSqlTodosWithDependenciesParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Todo rows + dependency edges read from the session SQL database.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPlanReadSqlTodosWithDependenciesResult {
    /// Edges from the session SQL todo_deps table. Empty when no database, no todo_deps table, or the SELECT failed. Read independently from `rows`, so a broken todo_deps table does not affect the rows result and vice versa.
    pub dependencies: Vec<PlanSqlTodoDependency>,
    /// Rows from the session SQL todos table, ordered by creation time and id. Empty when no database, no todos table, or the SELECT failed.
    pub rows: Vec<PlanSqlTodosRow>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesGetWorkspaceParams {
    /// Target session identifier
    pub session_id: SessionId,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesGetWorkspaceResultWorkspace {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    #[serde(
        rename = "chronicle_sync_dismissed",
        skip_serializing_if = "Option::is_none"
    )]
    pub chronicle_sync_dismissed: Option<bool>,
    #[serde(rename = "client_name", skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspacesWorkspaceDetailsHostType>,
    pub id: String,
    #[serde(rename = "mc_last_event_id", skip_serializing_if = "Option::is_none")]
    pub mc_last_event_id: Option<String>,
    #[serde(rename = "mc_session_id", skip_serializing_if = "Option::is_none")]
    pub mc_session_id: Option<String>,
    #[serde(rename = "mc_task_id", skip_serializing_if = "Option::is_none")]
    pub mc_task_id: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(rename = "remote_steerable", skip_serializing_if = "Option::is_none")]
    pub remote_steerable: Option<bool>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    #[serde(rename = "summary_count", skip_serializing_if = "Option::is_none")]
    pub summary_count: Option<i64>,
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Current workspace metadata for the session, including its absolute filesystem path when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesGetWorkspaceResult {
    /// Absolute filesystem path to the workspace directory. Omitted when the session has no workspace (e.g. remote sessions).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub path: Option<String>,
    /// Current workspace metadata, or null if not available
    pub workspace: Option<SessionWorkspacesGetWorkspaceResultWorkspace>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesListFilesParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Relative paths of files stored in the session workspace files directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesListFilesResult {
    /// Relative file paths in the workspace files directory
    pub files: Vec<String>,
}

/// Contents of the requested workspace file as a UTF-8 string.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesReadFileResult {
    /// File content as a UTF-8 string
    pub content: String,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesListCheckpointsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Workspace checkpoints in chronological order; empty when the workspace is not enabled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesListCheckpointsResult {
    /// Workspace checkpoints in chronological order. Empty when workspace is not enabled.
    pub checkpoints: Vec<WorkspacesCheckpoints>,
}

/// Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesReadCheckpointResult {
    /// Checkpoint content as a UTF-8 string, or null when the checkpoint or workspace is missing
    pub content: Option<String>,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesSaveLargePasteResultSaved {
    /// Filename within the workspace files directory
    pub filename: String,
    /// Absolute filesystem path to the saved paste file
    pub file_path: String,
    /// Size of the saved file in bytes
    pub size_bytes: i64,
}

/// Descriptor for the saved paste file, or null when the workspace is unavailable.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesSaveLargePasteResult {
    /// Saved-paste descriptor, or null when the workspace is unavailable (e.g. CCA runtime, non-infinite sessions, remote sessions)
    pub saved: Option<SessionWorkspacesSaveLargePasteResultSaved>,
}

/// Workspace diff result for the requested mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionWorkspacesDiffResult {
    /// Default branch used for a branch diff, when branch mode was requested.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub base_branch: Option<String>,
    /// Changed files and their unified diffs.
    pub changes: Vec<WorkspaceDiffFileChange>,
    /// Whether a requested branch diff fell back to unstaged changes because branch diff failed.
    pub is_fallback: bool,
    /// Effective mode used for the returned changes.
    pub mode: WorkspaceDiffMode,
    /// Diff mode requested by the client.
    pub requested_mode: WorkspaceDiffMode,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstructionsGetSourcesParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Instruction sources loaded for the session, in merge order.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionInstructionsGetSourcesResult {
    /// Instruction sources for the session
    pub sources: Vec<InstructionSource>,
}

/// Indicates whether fleet mode was successfully activated.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFleetStartResult {
    /// Whether fleet mode was successfully activated
    pub started: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Custom agents available to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentListResult {
    /// Available custom agents
    pub agents: Vec<AgentInfo>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentGetCurrentParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The currently selected custom agent, or null when using the default agent.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentGetCurrentResult {
    /// Currently selected custom agent, or null if using the default agent
    pub agent: AgentInfo,
}

/// The newly selected custom agent.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentSelectResult {
    /// The newly selected custom agent
    pub agent: AgentInfo,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentDeselectParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Custom agents available to the session after reloading definitions from disk.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionAgentReloadResult {
    /// Reloaded custom agents
    pub agents: Vec<AgentInfo>,
}

/// Identifier assigned to the newly started background agent task.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksStartAgentResult {
    /// Generated agent ID for the background task
    pub agent_id: String,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Background tasks currently tracked by the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksListResult {
    /// Currently tracked tasks
    pub tasks: Vec<serde_json::Value>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksRefreshParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Refresh metadata for any detached background shells the runtime knows about. Use after a long pause to pick up exit/output state for shells running outside the agent loop.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksRefreshResult {}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksWaitForPendingParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Wait until all in-flight background tasks (agents + shells) and any follow-up turns scheduled by their completions have settled. Returns when the runtime is fully drained or after an internal timeout (default 10 minutes; configurable via COPILOT_TASK_WAIT_TIMEOUT_SECONDS).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksWaitForPendingResult {}

/// Progress information for the task, or null when no task with that ID is tracked.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksGetProgressResult {
    /// Progress information for the task, discriminated by type. Returns null when no task with this ID is currently tracked.
    pub progress: Option<serde_json::Value>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksGetCurrentPromotableParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The first sync-waiting task that can currently be promoted to background mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksGetCurrentPromotableResult {
    /// The first sync-waiting task (agent first, then shell) that can currently be promoted to background mode. Omitted if no such task exists. The returned task is guaranteed to have executionMode='sync' and canPromoteToBackground=true at the time of the call.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task: Option<serde_json::Value>,
}

/// Indicates whether the task was successfully promoted to background mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksPromoteToBackgroundResult {
    /// Whether the task was successfully promoted to background mode
    pub promoted: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksPromoteCurrentToBackgroundParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// The promoted task as it now exists in background mode, omitted if no promotable task was waiting.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksPromoteCurrentToBackgroundResult {
    /// The promoted task as it now exists in background mode, omitted if no promotable task was waiting. Atomic operation: avoids the race window of getCurrentPromotable + promoteToBackground.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub task: Option<serde_json::Value>,
}

/// Indicates whether the background task was successfully cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksCancelResult {
    /// Whether the task was successfully cancelled
    pub cancelled: bool,
}

/// Indicates whether the task was removed. False when the task does not exist or is still running/idle.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksRemoveResult {
    /// Whether the task was removed. Returns false if the task does not exist or is still running/idle (cancel it first).
    pub removed: bool,
}

/// Indicates whether the message was delivered, with an error message when delivery failed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTasksSendMessageResult {
    /// Error message if delivery failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Whether the message was successfully delivered or steered
    pub sent: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Skills available to the session, with their enabled state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsListResult {
    /// Available skills
    pub skills: Vec<Skill>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsGetInvokedParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Skills invoked during this session, ordered by invocation time (most recent last).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsGetInvokedResult {
    /// Skills invoked during this session, ordered by invocation time (most recent last)
    pub skills: Vec<SkillsInvokedSkill>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Diagnostics from reloading skill definitions, with warnings and errors as separate lists.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsReloadResult {
    /// Errors emitted while loading skills (e.g. skills that failed to load entirely)
    pub errors: Vec<String>,
    /// Warnings emitted while loading skills (e.g. skills that loaded but had issues)
    pub warnings: Vec<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionSkillsEnsureLoadedParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// MCP servers configured for the session, with their connection status and host-level state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpListResult {
    /// Host-level state, omitted when no MCP host is initialized.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub host: Option<McpHostState>,
    /// Configured MCP servers
    pub servers: Vec<McpServer>,
}

/// Tools exposed by the connected MCP server. Throws when the server is not connected.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpListToolsResult {
    /// Tools exposed by the server.
    pub tools: Vec<McpTools>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// MCP server startup filtering result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpReloadWithConfigResult {
    /// Non-default servers allowed by policy
    #[serde(skip_serializing_if = "Option::is_none")]
    pub allowed_servers: Option<Vec<McpAllowedServer>>,
    /// Servers filtered out before startup
    pub filtered_servers: Vec<McpFilteredServer>,
}

/// Outcome of an MCP sampling execution: success result, failure error, or cancellation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpExecuteSamplingResult {
    /// Outcome of the sampling inference. 'success' produced a response; 'failure' encountered an error (including agent-side rejection by content filter or criteria); 'cancelled' the caller cancelled this execution via cancelSamplingExecution.
    pub action: McpSamplingExecutionAction,
    /// Error description, present when action='failure'.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// MCP CreateMessageResult payload (with optional 'tools' extension), present when action='success'. Treated as opaque at the schema layer; consumers should construct/consume it per the MCP CreateMessageResult shape.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<McpExecuteSamplingResult>,
}

/// Indicates whether an in-flight sampling execution with the given requestId was found and cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpCancelSamplingExecutionResult {
    /// True if an in-flight execution with the given requestId was found and signalled to cancel. False when no such execution is in flight (already completed, never started, or cancelled by another caller).
    pub cancelled: bool,
}

/// Env-value mode recorded on the session after the update.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpSetEnvValueModeResult {
    /// Mode recorded on the session after the update
    pub mode: McpSetEnvValueModeDetails,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpRemoveGitHubParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether the auto-managed `github` MCP server was removed (false when nothing to remove).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpRemoveGitHubResult {
    /// True when the auto-managed `github` MCP server was removed; false when no removal happened (e.g. user has explicitly configured a `github` server, or the server was not registered).
    pub removed: bool,
}

/// Result of configuring GitHub MCP.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpConfigureGitHubResult {
    /// Whether GitHub MCP configuration changed.
    pub changed: bool,
}

/// Whether the named MCP server is running.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpIsServerRunningResult {
    /// True if the server has an active client and transport.
    pub running: bool,
}

/// Empty result after recording the MCP OAuth response.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthRespondResult {}

/// Indicates whether the pending MCP OAuth response was accepted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthHandlePendingRequestResult {
    /// Whether the response was accepted. False if the request was unknown, timed out, or already resolved.
    pub success: bool,
}

/// OAuth authorization URL the caller should open, or empty when cached tokens already authenticated the server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpOauthLoginResult {
    /// URL the caller should open in a browser to complete OAuth. Omitted when cached tokens were still valid and no browser interaction was needed — the server is already reconnected in that case. When present, the runtime starts the callback listener before returning and continues the flow in the background; completion is signaled via session.mcp_server_status_changed.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub authorization_url: Option<String>,
}

/// Resource contents returned by the MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpAppsReadResourceResult {
    /// Resource contents returned by the server
    pub contents: Vec<McpAppsResourceContent>,
}

/// App-callable tools from the named MCP server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpAppsListToolsResult {
    /// App-callable tools from the server
    pub tools: Vec<HashMap<String, serde_json::Value>>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpAppsGetHostContextParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Current host context advertised to MCP App guests.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpAppsGetHostContextResult {
    /// Current host context
    pub context: McpAppsHostContextDetails,
}

/// Diagnostic snapshot of MCP Apps wiring for the named server.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMcpAppsDiagnoseResult {
    /// Capability negotiation snapshot
    pub capability: McpAppsDiagnoseCapability,
    /// What the server returned for this session
    pub server: McpAppsDiagnoseServer,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Plugins installed for the session, with their enabled state and version metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPluginsListResult {
    /// Installed plugins
    pub plugins: Vec<Plugin>,
}

/// A snapshot of the provider endpoint the session is currently configured to talk to.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionProviderGetEndpointResult {
    /// A credential the caller should use with this endpoint. Omitted only when the endpoint accepts unauthenticated requests.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub api_key: Option<String>,
    /// Base URL to pass to the LLM client library.
    pub base_url: String,
    /// HTTP headers the caller must include on every outbound request.
    pub headers: HashMap<String, String>,
    /// Short-lived, rotating credential the caller must send on every request, in addition to `apiKey` if one is present. Omitted when the endpoint does not require one.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub session_token: Option<ProviderSessionToken>,
    /// Provider family. Matches the `type` field of a BYOK provider config.
    pub r#type: ProviderEndpointType,
    /// Wire API to be used, when required for the provider type.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub wire_api: Option<ProviderEndpointWireApi>,
}

/// Indicates whether the session options patch was applied successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionOptionsUpdateResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Extensions discovered for the session, with their current status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsListResult {
    /// Discovered extensions and their current status
    pub extensions: Vec<Extension>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionExtensionsReloadParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether the external tool call result was handled successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsHandlePendingToolCallResult {
    /// Whether the tool call result was handled successfully
    pub success: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsInitializeAndValidateParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Resolve, build, and validate the runtime tool list for this session. Subagent sessions and consumer flows that need an initialized tool set before `send` invoke this. Default base-class implementation is a no-op for sessions that don't support tool validation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsInitializeAndValidateResult {}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsGetCurrentMetadataParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Current lightweight tool metadata snapshot for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsGetCurrentMetadataResult {
    /// Current tool metadata, or null when tools have not been initialized yet
    pub tools: Option<Vec<CurrentToolMetadata>>,
}

/// Empty result after applying subagent settings
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionToolsUpdateSubagentSettingsResult {}

/// Slash commands available in the session, after applying any include/exclude filters.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsListResult {
    /// Commands available in this session
    pub commands: Vec<SlashCommandInfo>,
}

/// Indicates whether the pending client-handled command was completed successfully.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsHandlePendingCommandResult {
    /// Whether the command was handled successfully
    pub success: bool,
}

/// Error message produced while executing the command, if any.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsExecuteResult {
    /// Error message produced while executing the command, if any. Omitted when the handler succeeded.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
}

/// Indicates whether the command was accepted into the local execution queue.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsEnqueueResult {
    /// True when the command was accepted into the local execution queue. False when the call targets a session that does not support local command queueing (e.g. remote sessions).
    pub queued: bool,
}

/// Indicates whether the queued-command response was matched to a pending request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionCommandsRespondToQueuedCommandResult {
    /// Whether a pending queued command with the given request ID was found and resolved. False when the request was already resolved, cancelled, or unknown.
    pub success: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTelemetryGetEngagementIdParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Telemetry engagement ID for the session, when available.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionTelemetryGetEngagementIdResult {
    /// Current telemetry engagement ID, when available.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub engagement_id: Option<String>,
}

/// Transient answer generated from current conversation context.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiEphemeralQueryResult {
    /// Full assistant response text.
    pub answer: String,
}

/// The elicitation response (accept with form values, decline, or cancel)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiElicitationResult {
    /// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
    pub action: UIElicitationResponseAction,
    /// The form values submitted by the user (present when action is 'accept')
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<HashMap<String, serde_json::Value>>,
}

/// Indicates whether the elicitation response was accepted; false if it was already resolved by another client.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandlePendingElicitationResult {
    /// Whether the response was accepted. False if the request was already resolved by another client.
    pub success: bool,
}

/// Indicates whether the pending UI request was resolved by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandlePendingUserInputResult {
    /// True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
    pub success: bool,
}

/// Indicates whether the pending UI request was resolved by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandlePendingSamplingResult {
    /// True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
    pub success: bool,
}

/// Indicates whether the pending UI request was resolved by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandlePendingAutoModeSwitchResult {
    /// True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
    pub success: bool,
}

/// Indicates whether the pending UI request was resolved by this call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiHandlePendingExitPlanModeResult {
    /// True if the request was still pending and was resolved by this call. False if the request ID was unknown, already resolved by another client (e.g. GitHub), expired, or otherwise no longer pending.
    pub success: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiRegisterDirectAutoModeSwitchHandlerParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Register an in-process handler for `auto_mode_switch.requested` events. The caller still attaches the actual listener via the standard event-subscription mechanism; this registration solely tells the server bridge to skip its own dispatch (so a remote client doesn't race the in-process handler for the same requestId).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiRegisterDirectAutoModeSwitchHandlerResult {
    /// Opaque handle representing the registration. Pass this same handle to `unregisterDirectAutoModeSwitchHandler` when the in-process handler is no longer active. Multiple registrations are reference-counted; the server bridge will only dispatch auto-mode-switch requests when no handles are active.
    pub handle: String,
}

/// Indicates whether the handle was active and the registration count was decremented.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUiUnregisterDirectAutoModeSwitchHandlerResult {
    /// True if the handle was active and decremented the counter; false if the handle was unknown.
    pub unregistered: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsConfigureResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the permission decision was applied; false when the request was already resolved.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsHandlePendingPermissionRequestResult {
    /// Whether the permission request was handled successfully
    pub success: bool,
}

/// List of pending permission requests reconstructed from event history.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsPendingRequestsResult {
    /// Pending permission prompts reconstructed from the session's event history. Equivalent to the set of `permission.requested` events that have not yet been followed by a matching `permission.completed` event. Used by clients (e.g. the CLI) to hydrate UI for prompts that were emitted before the client attached to the session.
    pub items: Vec<PendingPermissionRequest>,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsSetApproveAllResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded and reports the post-mutation state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsSetAllowAllResult {
    /// Authoritative allow-all state after the mutation
    pub enabled: bool,
    /// Whether the operation succeeded
    pub success: bool,
}

/// Current full allow-all permission state.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsGetAllowAllResult {
    /// Whether full allow-all permissions are currently active
    pub enabled: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsModifyRulesResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsSetRequiredResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsResetSessionApprovalsResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsNotifyPromptShownResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Snapshot of the session's allow-listed directories and primary working directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsPathsListResult {
    /// All directories currently allowed for tool access on this session.
    pub directories: Vec<String>,
    /// The primary working directory for this session.
    pub primary: String,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsPathsAddResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsPathsUpdatePrimaryResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the supplied path is within the session's allowed directories.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsPathsIsPathWithinAllowedDirectoriesResult {
    /// Whether the path is within the session's allowed directories
    pub allowed: bool,
}

/// Indicates whether the supplied path is within the session's workspace directory.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsPathsIsPathWithinWorkspaceResult {
    /// Whether the path is within the session workspace directory
    pub allowed: bool,
}

/// Resolved location-permissions key and type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsLocationsResolveResult {
    /// Location key used in the location-permissions store
    pub location_key: String,
    /// Whether the location is a git repo or directory
    pub location_type: PermissionLocationType,
}

/// Summary of persisted location permissions applied to the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsLocationsApplyResult {
    /// Number of persisted allowed directories added to the live path manager
    pub applied_directory_count: i64,
    /// Number of location-scoped rules added to the live permission service
    pub applied_rule_count: i64,
    /// Location-scoped rules applied to the live permission service
    pub applied_rules: Vec<PermissionRule>,
    /// Whether a different location was applied since the previous apply call
    pub changed: bool,
    /// Location key used in the location-permissions store
    pub location_key: String,
    /// Whether the location is a git repo or directory
    pub location_type: PermissionLocationType,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsLocationsAddToolApprovalResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Folder trust check result.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsFolderTrustIsTrustedResult {
    /// Whether the folder is trusted
    pub trusted: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsFolderTrustAddTrustedResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionPermissionsUrlsSetUnrestrictedModeResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Identifier of the session event that was emitted for the log message.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionLogResult {
    /// The unique identifier of the emitted session event
    pub event_id: String,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataSnapshotParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Public-facing projection of workspace metadata for SDK / TUI consumers
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataSnapshotResultWorkspace {
    /// Branch checked out at session start, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub branch: Option<String>,
    /// ISO 8601 timestamp when the workspace was created
    #[serde(rename = "created_at", skip_serializing_if = "Option::is_none")]
    pub created_at: Option<String>,
    /// Current working directory at session start
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cwd: Option<String>,
    /// Resolved git root for cwd, if any
    #[serde(rename = "git_root", skip_serializing_if = "Option::is_none")]
    pub git_root: Option<String>,
    /// Repository host type, if known
    #[serde(rename = "host_type", skip_serializing_if = "Option::is_none")]
    pub host_type: Option<WorkspaceSummaryHostType>,
    /// Workspace identifier (1:1 with sessionId)
    pub id: String,
    /// Display name for the session, if set
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    /// Repository identifier in 'owner/repo' or 'org/project/repo' format, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub repository: Option<String>,
    /// ISO 8601 timestamp when the workspace was last updated
    #[serde(rename = "updated_at", skip_serializing_if = "Option::is_none")]
    pub updated_at: Option<String>,
    /// Whether the display name was explicitly set by the user
    #[serde(rename = "user_named", skip_serializing_if = "Option::is_none")]
    pub user_named: Option<bool>,
}

/// Point-in-time snapshot of slow-changing session identifier and state fields
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataSnapshotResult {
    /// True when the session was detected to be in use by another process at construction time. Local consumers may surface a confirmation prompt before fully attaching. Always false for new sessions.
    pub already_in_use: bool,
    /// Runtime client name associated with the session (telemetry identifier).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub client_name: Option<String>,
    /// The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot')
    pub current_mode: MetadataSnapshotCurrentMode,
    /// User-provided name supplied at session construction (via `--name`), if any. Immutable after construction.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub initial_name: Option<String>,
    /// Whether this is a remote session (i.e., one whose runtime executes elsewhere and is steered through this process)
    pub is_remote: bool,
    /// ISO 8601 timestamp of when the session's persisted state was last modified on disk. For new sessions, equals startTime. For resumed sessions, reflects the previous modification time at construction.
    pub modified_time: String,
    /// Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are immutable for the lifetime of the session.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub remote_metadata: Option<MetadataSnapshotRemoteMetadata>,
    /// Currently selected model identifier, if any
    #[serde(skip_serializing_if = "Option::is_none")]
    pub selected_model: Option<String>,
    /// The unique identifier of the session
    pub session_id: SessionId,
    /// ISO 8601 timestamp of when the session started
    pub start_time: String,
    /// Short human-readable summary of the session, if known. Omitted when no summary has been generated.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary: Option<String>,
    /// Absolute path to the session's current working directory
    pub working_directory: String,
    /// Public-facing workspace metadata for this session, or null if the session has no associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count, internal flags).
    pub workspace: Option<SessionMetadataSnapshotResultWorkspace>,
    /// Absolute path to the session's workspace directory on disk, or null if the session has no associated workspace
    pub workspace_path: Option<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataIsProcessingParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether the local session is currently processing a turn or background continuation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataIsProcessingResult {
    /// Whether the session is currently processing user/agent messages. False for non-local sessions (which don't run a local agentic loop). Reflects an in-flight turn or background continuation.
    pub processing: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataActivityParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Current activity flags for the session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataActivityResult {
    /// Whether an in-flight operation can currently be aborted.
    pub abortable: bool,
    /// Whether the session currently has active work, including running turns or tasks.
    pub has_active_work: bool,
}

/// Token-usage breakdown for the session's current context window
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataContextInfoResultContextInfo {
    /// Output reserve plus tokens after the buffer-exhaustion blocking threshold (default 95%)
    pub buffer_tokens: i64,
    /// Token count at which background compaction starts (configurable percentage of promptTokenLimit)
    pub compaction_threshold: i64,
    /// Tokens consumed by user/assistant/tool messages
    pub conversation_tokens: i64,
    /// Prompt token limit plus the model's full output token limit.
    pub limit: i64,
    /// Tokens consumed by MCP tool definitions (subset of toolDefinitionsTokens, excludes deferred tools)
    pub mcp_tools_tokens: i64,
    /// The model used for token counting
    pub model_name: String,
    /// Maximum prompt tokens allowed by the model (or DEFAULT_TOKEN_LIMIT if unspecified)
    pub prompt_token_limit: i64,
    /// Tokens consumed by the system prompt
    pub system_tokens: i64,
    /// Tokens consumed by tool definitions sent to the model (excludes deferred tools)
    pub tool_definitions_tokens: i64,
    /// Sum of system, conversation and tool-definition tokens
    pub total_tokens: i64,
}

/// Token breakdown for the session's current context window, or null if uninitialized.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataContextInfoResult {
    /// Token breakdown for the current context window, or null if the session has not yet been initialized (no system prompt or tool metadata cached).
    pub context_info: Option<SessionMetadataContextInfoResultContextInfo>,
}

/// Notify the session that its working directory context has changed. Emits a `session.context_changed` event so consumers (telemetry, OTel tracker, ACP, the timeline UI) can react. Use this when the host has detected a cwd/branch/repo change outside the session's normal lifecycle (e.g., after a shell command in interactive mode).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataRecordContextChangeResult {}

/// Update the session's working directory. Used by the host when the user explicitly changes cwd (e.g., the `/cd` slash command). The host is responsible for `process.chdir` and any related side-effects (file index, etc.); this method only updates the session's own recorded path.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataSetWorkingDirectoryResult {
    /// Working directory after the update
    pub working_directory: String,
}

/// Re-tokenize the session's existing messages against `modelId` and return the token totals. Useful for hosts that want an initial estimate of context usage on session resume, before the next agent turn fires `session.context_info_changed` events. Returns zeros for an empty session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionMetadataRecomputeContextTokensResult {
    /// Tokens contributed by user/assistant/tool messages (excludes system/developer prompts).
    pub messages_token_count: i64,
    /// Tokens contributed by system/developer prompt snapshots.
    pub system_token_count: i64,
    /// Sum of tokens across chat-context and system-context messages currently held by the session.
    pub total_tokens: i64,
}

/// Identifier of the spawned process, used to correlate streamed output and exit notifications.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShellExecResult {
    /// Unique identifier for tracking streamed output
    pub process_id: String,
}

/// Indicates whether the signal was delivered; false if the process was unknown or already exited.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShellKillResult {
    /// Whether the signal was sent successfully
    pub killed: bool,
}

/// Result of a user-requested shell command.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShellExecuteUserRequestedResult {
    /// Error output when the execution failed
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<String>,
    /// Process exit code, when available
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exit_code: Option<i64>,
    /// Captured command output
    pub output: String,
    /// Whether the command completed successfully
    pub success: bool,
    /// Tool call id emitted for the shell execution
    pub tool_call_id: String,
}

/// Cancellation result for a user-requested shell command.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionShellCancelUserRequestedResult {
    /// Whether an in-flight execution was found and signalled to cancel
    pub cancelled: bool,
}

/// Compaction outcome with the number of tokens and messages removed, summary text, and the resulting context window breakdown.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryCompactResult {
    /// Post-compaction context window usage breakdown
    #[serde(skip_serializing_if = "Option::is_none")]
    pub context_window: Option<HistoryCompactContextWindow>,
    /// Number of messages removed during compaction
    pub messages_removed: i64,
    /// Whether compaction completed successfully
    pub success: bool,
    /// Summary text produced by compaction. Omitted when compaction did not produce a summary (e.g. failure path).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub summary_content: Option<String>,
    /// Number of tokens freed by compaction
    pub tokens_removed: i64,
}

/// Number of events that were removed by the truncation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryTruncateResult {
    /// Number of events that were removed
    pub events_removed: i64,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryCancelBackgroundCompactionParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether an in-progress background compaction was cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryCancelBackgroundCompactionResult {
    /// Whether an in-progress background compaction was cancelled. False when no compaction was running, when the session is remote, or when the underlying processor was unavailable.
    pub cancelled: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryAbortManualCompactionParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether an in-progress manual compaction was aborted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistoryAbortManualCompactionResult {
    /// Whether an in-progress manual compaction was aborted. False when no manual compaction was running, when its abort controller was already aborted, or when the session is remote.
    pub aborted: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistorySummarizeForHandoffParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Markdown summary of the conversation context (empty when not available).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionHistorySummarizeForHandoffResult {
    /// Markdown summary of the conversation context produced by an LLM. Empty string when there are no messages or when the session does not support local summarization.
    pub summary: String,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueuePendingItemsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Snapshot of the session's pending queued items and immediate-steering messages.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueuePendingItemsResult {
    /// Pending queued items in submission order. Includes user messages, queued slash commands, and queued model changes; omits internal system items.
    pub items: Vec<QueuePendingItems>,
    /// Display text for messages currently in the immediate steering queue (interjections sent during a running turn).
    pub steering_messages: Vec<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueRemoveMostRecentParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Indicates whether a user-facing pending item was removed.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueRemoveMostRecentResult {
    /// True if a user-facing pending item was removed (LIFO across both queues); false when no removable items remained.
    pub removed: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionQueueClearParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Batch of session events returned by a read, with cursor and continuation metadata.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEventLogReadResult {
    /// Opaque cursor for the next read. Pass back unchanged in the next read.cursor to continue from where this read left off. Always present, even when no events were returned.
    pub cursor: String,
    /// Cursor status: 'ok' means the cursor was applied successfully; 'expired' means the cursor referred to an event that no longer exists in history (e.g. truncated or compacted away) and the read started from the beginning of the remaining history.
    pub cursor_status: EventsCursorStatus,
    /// Events are delivered in two batches per read: persisted events first (in append order), then ephemeral events (in seq order). When `waitMs > 0` and the catch-up batches were empty, post-wait events follow the same two-batch ordering. Persisted and ephemeral events do not interleave within a single read.
    pub events: Vec<SessionEvent>,
    /// True when the read returned `max` events and more events are available immediately. When false, the next read with a non-zero `waitMs` will block until a new event arrives or the wait expires.
    pub has_more: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEventLogTailParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Snapshot of the current tail cursor without returning any events. Use this when a consumer wants to subscribe to live events going forward without first paginating through the entire persisted history (which would happen if `read` were called without a cursor on a long-lived session).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEventLogTailResult {
    /// Opaque cursor pointing at the current tail of the session's persisted-events history. Pass back to `read` to receive only events that arrive AFTER this snapshot. When the session has no events, this returns the same sentinel as an unset cursor (i.e. equivalent to omitting the cursor on a first read).
    pub cursor: String,
}

/// Opaque handle representing an event-type interest registration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEventLogRegisterInterestResult {
    /// Opaque handle for this registration. Pass to releaseInterest to release. Each call to registerInterest produces a fresh handle, even when the same eventType is registered multiple times.
    pub handle: String,
}

/// Indicates whether the operation succeeded.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionEventLogReleaseInterestResult {
    /// Whether the operation succeeded
    pub success: bool,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUsageGetMetricsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionUsageGetMetricsResult {
    /// Aggregated code change metrics
    pub code_changes: UsageMetricsCodeChanges,
    /// Currently active model identifier
    #[serde(skip_serializing_if = "Option::is_none")]
    pub current_model: Option<String>,
    /// Input tokens from the most recent main-agent API call
    pub last_call_input_tokens: i64,
    /// Output tokens from the most recent main-agent API call
    pub last_call_output_tokens: i64,
    /// Per-model token and request metrics, keyed by model identifier
    pub model_metrics: HashMap<String, UsageMetricsModelMetric>,
    /// ISO 8601 timestamp when the session started
    pub session_start_time: String,
    /// Session-wide per-token-type accumulated token counts
    #[serde(skip_serializing_if = "Option::is_none")]
    pub token_details: Option<HashMap<String, UsageMetricsTokenDetail>>,
    /// Total time spent in model API calls (milliseconds)
    pub total_api_duration_ms: i64,
    /// Session-wide accumulated nano-AI units cost
    #[serde(skip_serializing_if = "Option::is_none")]
    pub total_nano_aiu: Option<f64>,
    /// Total user-initiated premium request cost across all models (may be fractional due to multipliers)
    pub total_premium_request_cost: f64,
    /// Raw count of user-initiated API requests
    pub total_user_requests: i64,
}

/// GitHub URL for the session and a flag indicating whether remote steering is enabled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionRemoteEnableResult {
    /// Whether remote steering is enabled
    pub remote_steerable: bool,
    /// GitHub frontend URL for this session
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionRemoteDisableParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Persist a steerability change as a `session.remote_steerable_changed` event. Used by the host (CLI / SDK consumer) when it has just finished enabling or disabling steering on a remote exporter that the runtime does not directly own.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionRemoteNotifySteerableChangedResult {}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleListParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Snapshot of the currently active recurring prompts for this session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleListResult {
    /// Active scheduled prompts, ordered by id.
    pub entries: Vec<ScheduleEntry>,
}

/// Remove a scheduled prompt by id. The result entry is omitted if the id was unknown.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionScheduleStopResult {
    /// The removed entry, or omitted if no entry matched.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub entry: Option<ScheduleEntry>,
}

/// Identifies the target session.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SessionFsSqliteExistsParams {
    /// Target session identifier
    pub session_id: SessionId,
}

/// Canvas open result returned by the provider.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CanvasOpenResult {
    /// Provider-supplied status text
    #[serde(skip_serializing_if = "Option::is_none")]
    pub status: Option<String>,
    /// Provider-supplied title
    #[serde(skip_serializing_if = "Option::is_none")]
    pub title: Option<String>,
    /// URL for web-rendered canvases
    #[serde(skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// HTTP headers as a map from lowercased header name to a list of values. Multi-valued headers (e.g. Set-Cookie) preserve all values.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type LlmInferenceHeaders = HashMap<String, Vec<String>>;

/// MCP CreateMessageResult payload (with optional 'tools' extension), present when action='success'. Treated as opaque at the schema layer; consumers should construct/consume it per the MCP CreateMessageResult shape.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type McpExecuteSamplingResult = HashMap<String, serde_json::Value>;

/// The form values submitted by the user (present when action is 'accept')
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type UIElicitationResponseContent = HashMap<String, serde_json::Value>;

/// Standard MCP CallToolResult
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
pub type SessionMcpAppsCallToolResult = HashMap<String, serde_json::Value>;

/// Which tier this directory belongs to
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentDiscoveryPathScope {
    /// The user's personal agent configuration directory.
    #[serde(rename = "user")]
    User,
    /// A project's repository agent directory.
    #[serde(rename = "project")]
    Project,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Where the agent definition was loaded from
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentInfoSource {
    /// Agent loaded from the user's personal agent configuration.
    #[serde(rename = "user")]
    User,
    /// Agent loaded from the current project's repository configuration.
    #[serde(rename = "project")]
    Project,
    /// Agent inherited from a parent project or workspace.
    #[serde(rename = "inherited")]
    Inherited,
    /// Agent provided by a remote runtime or service.
    #[serde(rename = "remote")]
    Remote,
    /// Agent contributed by an installed plugin.
    #[serde(rename = "plugin")]
    Plugin,
    /// Agent built into the Copilot runtime.
    #[serde(rename = "builtin")]
    Builtin,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Kind of attention required when status === "attention". Meaningful only when status === "attention".
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistryLiveTargetEntryAttentionKind {
    /// Session is blocked on an unrecoverable error
    #[serde(rename = "error")]
    Error,
    /// Session is waiting for a tool-permission decision
    #[serde(rename = "permission")]
    Permission,
    /// Session is waiting for the user to approve or reject a plan
    #[serde(rename = "exit_plan")]
    ExitPlan,
    /// Session is waiting on an elicitation prompt
    #[serde(rename = "elicitation")]
    Elicitation,
    /// Session is waiting for free-form user input
    #[serde(rename = "user_input")]
    UserInput,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Process kind tag for the registry entry
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistryLiveTargetEntryKind {
    /// Interactive Copilot CLI exposing a UI server (legacy/normal CLI process)
    #[serde(rename = "ui-server")]
    UiServer,
    /// Headless `--server --managed-server` child spawned by a controller
    #[serde(rename = "managed-server")]
    ManagedServer,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How the most recent turn ended (clean vs aborted). Lets the renderer distinguish done from done_cancelled.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistryLiveTargetEntryLastTerminalEvent {
    /// Last turn ended cleanly (model returned a final assistant message)
    #[serde(rename = "turn_end")]
    TurnEnd,
    /// Last turn was aborted (e.g. user interrupted)
    #[serde(rename = "abort")]
    Abort,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Coarse lifecycle status of the foreground session
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistryLiveTargetEntryStatus {
    /// Session is actively processing a turn
    #[serde(rename = "working")]
    Working,
    /// Session is idle, waiting for input
    #[serde(rename = "waiting")]
    Waiting,
    /// Last turn completed successfully
    #[serde(rename = "done")]
    Done,
    /// Session needs user attention (see attentionKind for the specific reason)
    #[serde(rename = "attention")]
    Attention,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Categorized reason for log-open failure
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistryLogCaptureOpenErrorReason {
    /// Filesystem permission denied opening the log file
    #[serde(rename = "permission")]
    Permission,
    /// No space left on device
    #[serde(rename = "disk_full")]
    DiskFull,
    /// Other / uncategorized open failure
    #[serde(rename = "other")]
    Other,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: child_process.spawn itself failed
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnErrorKind {
    #[serde(rename = "spawn-error")]
    #[default]
    SpawnError,
}

/// Permission posture for the new session. 'yolo' requires the controller-local session to currently be in allow-all mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnPermissionMode {
    /// Standard permission posture (prompts for each request)
    #[serde(rename = "default")]
    Default,
    /// Full allow-all (requires the controller-local session to currently be in allow-all mode)
    #[serde(rename = "yolo")]
    Yolo,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: spawn succeeded but child never registered
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnRegistryTimeoutKind {
    #[serde(rename = "registry-timeout")]
    #[default]
    RegistryTimeout,
}

/// Discriminator: managed-server child spawned successfully
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnSpawnedKind {
    #[serde(rename = "spawned")]
    #[default]
    Spawned,
}

/// Which parameter field was invalid. Omitted when the rejection is not field-specific.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnValidationErrorField {
    /// The cwd parameter
    #[serde(rename = "cwd")]
    Cwd,
    /// The session name parameter
    #[serde(rename = "name")]
    Name,
    /// The agentName parameter
    #[serde(rename = "agentName")]
    AgentName,
    /// The model parameter
    #[serde(rename = "model")]
    Model,
    /// The permissionMode parameter
    #[serde(rename = "permissionMode")]
    PermissionMode,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discriminator: synchronous pre-validation rejected the request
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnValidationErrorKind {
    #[serde(rename = "validation-error")]
    #[default]
    ValidationError,
}

/// Categorized reason for the rejection. Low-cardinality enum so telemetry can aggregate by reason without leaking raw paths or agent/model names.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AgentRegistrySpawnValidationErrorReason {
    /// Provided cwd does not exist on disk
    #[serde(rename = "cwd-not-found")]
    CwdNotFound,
    /// Provided cwd exists but is not a directory
    #[serde(rename = "cwd-not-directory")]
    CwdNotDirectory,
    /// Session name failed validateSessionName
    #[serde(rename = "invalid-name")]
    InvalidName,
    /// Requested agent name was not found in builtin or custom agents
    #[serde(rename = "unknown-agent")]
    UnknownAgent,
    /// Requested model is not available to this session
    #[serde(rename = "unknown-model")]
    UnknownModel,
    /// Caller asked for permissionMode='yolo' but the controller is not currently in allow-all mode
    #[serde(rename = "yolo-not-allowed")]
    YoloNotAllowed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Outcome of an agentRegistry.spawn call.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum AgentRegistrySpawnResult {
    Spawned(AgentRegistrySpawnSpawned),
    SpawnError(AgentRegistrySpawnError),
    RegistryTimeout(AgentRegistrySpawnRegistryTimeout),
    ValidationError(AgentRegistrySpawnValidationError),
}

/// API-key authentication for non-GitHub LLM providers (e.g. when running BYOM-style).
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ApiKeyAuthInfoType {
    #[serde(rename = "api-key")]
    #[default]
    ApiKey,
}

/// Why the binary data is absent: it exceeded the inline size limit, or its asset was unavailable
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum OmittedBinaryOmittedReason {
    /// Bytes exceeded the session's inline size limit.
    #[serde(rename = "too_large")]
    TooLarge,
    /// The referenced binary asset could not be found (e.g. a truncated log).
    #[serde(rename = "asset_unavailable")]
    AssetUnavailable,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentBlobType {
    #[serde(rename = "blob")]
    #[default]
    Blob,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentDirectoryType {
    #[serde(rename = "directory")]
    #[default]
    Directory,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentExtensionContextType {
    #[serde(rename = "extension_context")]
    #[default]
    ExtensionContext,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentFileType {
    #[serde(rename = "file")]
    #[default]
    File,
}

/// Type of GitHub reference
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentGitHubReferenceType {
    /// GitHub issue reference.
    #[serde(rename = "issue")]
    Issue,
    /// GitHub pull request reference.
    #[serde(rename = "pr")]
    Pr,
    /// GitHub discussion reference.
    #[serde(rename = "discussion")]
    Discussion,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AttachmentSelectionType {
    #[serde(rename = "selection")]
    #[default]
    Selection,
}

/// Authentication type
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum AuthInfoType {
    /// Authentication provided by a GitHub App HMAC credential.
    #[serde(rename = "hmac")]
    Hmac,
    /// Authentication resolved from environment-provided credentials.
    #[serde(rename = "env")]
    Env,
    /// Authentication from an interactive user sign-in.
    #[serde(rename = "user")]
    User,
    /// Authentication delegated to the GitHub CLI.
    #[serde(rename = "gh-cli")]
    GhCli,
    /// Authentication from an API key credential.
    #[serde(rename = "api-key")]
    ApiKey,
    /// Authentication from a GitHub token.
    #[serde(rename = "token")]
    Token,
    /// Authentication from a Copilot API token.
    #[serde(rename = "copilot-api-token")]
    CopilotApiToken,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Runtime-controlled routing state for an open canvas instance.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CanvasInstanceAvailability {
    /// The owning provider is currently connected and routing calls will be dispatched normally.
    #[serde(rename = "ready")]
    Ready,
    /// The owning provider is not currently connected. Routing calls fail with canvas_provider_unavailable until the agent re-issues open_canvas (which rehydrates via a fresh canvas.open) or the provider reconnects.
    #[serde(rename = "stale")]
    Stale,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Optional completion hint for the input (e.g. 'directory' for filesystem path completion)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandInputCompletion {
    /// Input should complete filesystem directories.
    #[serde(rename = "directory")]
    Directory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Coarse command category for grouping and behavior: runtime built-in, skill-backed command, or SDK/client-owned command
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandKind {
    /// Command implemented by the runtime.
    #[serde(rename = "builtin")]
    Builtin,
    /// Command backed by a skill.
    #[serde(rename = "skill")]
    Skill,
    /// Command registered by an SDK client or extension.
    #[serde(rename = "client")]
    Client,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Neutral SDK discriminator for the connected remote session kind.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ConnectedRemoteSessionMetadataKind {
    /// Remote CLI session.
    #[serde(rename = "remote-session")]
    RemoteSession,
    /// GitHub Copilot coding agent session.
    #[serde(rename = "coding-agent")]
    CodingAgent,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Controls how MCP tool result content is filtered: none leaves content unchanged, markdown sanitizes HTML while preserving Markdown-friendly output, and hidden_characters removes characters that can hide directives.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ContentFilterMode {
    /// Leave MCP tool result content unchanged.
    #[serde(rename = "none")]
    None,
    /// Sanitize HTML while preserving Markdown-friendly output.
    #[serde(rename = "markdown")]
    Markdown,
    /// Remove characters that can hide directives.
    #[serde(rename = "hidden_characters")]
    HiddenCharacters,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Authentication host (always the public GitHub host).
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CopilotApiTokenAuthInfoHost {
    #[serde(rename = "https://github.com")]
    #[default]
    HttpsGitHubCom,
}

/// Direct Copilot API authentication via the `GITHUB_COPILOT_API_TOKEN` + `COPILOT_API_URL` environment-variable pair. The token itself is read from the environment by the runtime, not carried in this struct.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum CopilotApiTokenAuthInfoType {
    #[serde(rename = "copilot-api-token")]
    #[default]
    CopilotApiToken,
}

/// Server transport type: stdio, http, sse (deprecated), or memory
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum DiscoveredMcpServerType {
    /// Server communicates over stdio with a local child process.
    #[serde(rename = "stdio")]
    Stdio,
    /// Server communicates over streamable HTTP.
    #[serde(rename = "http")]
    Http,
    /// Server communicates over Server-Sent Events (deprecated).
    #[serde(rename = "sse")]
    Sse,
    /// Server is backed by an in-memory runtime implementation.
    #[serde(rename = "memory")]
    Memory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Personal access token (PAT) or server-to-server token sourced from an environment variable.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EnvAuthInfoType {
    #[serde(rename = "env")]
    #[default]
    Env,
}

/// Agent-scope filter: 'primary' returns only main-agent events plus events whose type starts with 'subagent.' (matching the typed-subscription default behavior); 'all' returns events from all agents (matching wildcard-subscription behavior). Default is 'all' to preserve wildcard semantics for catch-up callers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EventsAgentScope {
    /// Return main-agent events and typed subagent lifecycle events.
    #[serde(rename = "primary")]
    Primary,
    /// Return events from all agents.
    #[serde(rename = "all")]
    All,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Cursor status: 'ok' means the cursor was applied successfully; 'expired' means the cursor referred to an event that no longer exists in history (e.g. truncated or compacted away) and the read started from the beginning of the remaining history.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum EventsCursorStatus {
    /// The cursor was applied successfully.
    #[serde(rename = "ok")]
    Ok,
    /// The cursor referred to history that is no longer available.
    #[serde(rename = "expired")]
    Expired,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Discovery source: project (.github/extensions/), user (~/.copilot/extensions/), plugin (installed plugin), or session (session-state/<id>/extensions/)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExtensionSource {
    /// Extension discovered from the current project's .github/extensions directory.
    #[serde(rename = "project")]
    Project,
    /// Extension discovered from the user's ~/.copilot/extensions directory.
    #[serde(rename = "user")]
    User,
    /// Extension contributed by an installed plugin.
    #[serde(rename = "plugin")]
    Plugin,
    /// Extension discovered from the current session's state directory (loaded only for this session).
    #[serde(rename = "session")]
    Session,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current status: running, disabled, failed, or starting
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExtensionStatus {
    /// The extension process is running.
    #[serde(rename = "running")]
    Running,
    /// The extension is installed but disabled.
    #[serde(rename = "disabled")]
    Disabled,
    /// The extension failed to start or crashed.
    #[serde(rename = "failed")]
    Failed,
    /// The extension process is starting.
    #[serde(rename = "starting")]
    Starting,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExtensionContextPushInputType {
    #[serde(rename = "extension_context")]
    #[default]
    ExtensionContext,
}

/// Binary result type discriminator. Use "image" for images and "resource" for other binary data.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmBinaryResultsForLlmType {
    /// Binary image data.
    #[serde(rename = "image")]
    Image,
    /// Other binary resource data.
    #[serde(rename = "resource")]
    Resource,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentAudioType {
    #[serde(rename = "audio")]
    #[default]
    Audio,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentImageType {
    #[serde(rename = "image")]
    #[default]
    Image,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentResourceType {
    #[serde(rename = "resource")]
    #[default]
    Resource,
}

/// Theme variant this icon is intended for
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentResourceLinkIconTheme {
    /// Icon intended for light themes.
    #[serde(rename = "light")]
    Light,
    /// Icon intended for dark themes.
    #[serde(rename = "dark")]
    Dark,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentResourceLinkType {
    #[serde(rename = "resource_link")]
    #[default]
    ResourceLink,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentTerminalType {
    #[serde(rename = "terminal")]
    #[default]
    Terminal,
}

/// Content block type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ExternalToolTextResultForLlmContentTextType {
    #[serde(rename = "text")]
    #[default]
    Text,
}

/// Authentication via the `gh` CLI's saved credentials.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum GhCliAuthInfoType {
    #[serde(rename = "gh-cli")]
    #[default]
    GhCli,
}

/// Authentication host. HMAC auth always targets the public GitHub host.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HMACAuthInfoHost {
    #[serde(rename = "https://github.com")]
    #[default]
    HttpsGitHubCom,
}

/// HMAC-based authentication used by GitHub-internal services.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum HMACAuthInfoType {
    #[serde(rename = "hmac")]
    #[default]
    Hmac,
}

/// Constant value. Always "github".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstalledPluginSourceGitHubSource {
    #[serde(rename = "github")]
    #[default]
    GitHub,
}

/// Constant value. Always "local".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstalledPluginSourceLocalSource {
    #[serde(rename = "local")]
    #[default]
    Local,
}

/// Constant value. Always "url".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstalledPluginSourceUrlSource {
    #[serde(rename = "url")]
    #[default]
    Url,
}

/// Whether the target is a single file or a directory of instruction files
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstructionDiscoveryPathKind {
    /// The target is a single instruction file.
    #[serde(rename = "file")]
    File,
    /// The target is a directory that holds instruction files.
    #[serde(rename = "directory")]
    Directory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Which tier this target belongs to
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstructionDiscoveryPathLocation {
    /// Instructions live in user-level configuration.
    #[serde(rename = "user")]
    User,
    /// Instructions live in repository-level configuration.
    #[serde(rename = "repository")]
    Repository,
    /// Instructions live under the current working directory.
    #[serde(rename = "working-directory")]
    WorkingDirectory,
    /// Instructions live in plugin-provided configuration.
    #[serde(rename = "plugin")]
    Plugin,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Where this source lives — used for UI grouping
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstructionSourceLocation {
    /// Instructions live in user-level configuration.
    #[serde(rename = "user")]
    User,
    /// Instructions live in repository-level configuration.
    #[serde(rename = "repository")]
    Repository,
    /// Instructions live under the current working directory.
    #[serde(rename = "working-directory")]
    WorkingDirectory,
    /// Instructions live in plugin-provided configuration.
    #[serde(rename = "plugin")]
    Plugin,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Category of instruction source — used for merge logic
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum InstructionSourceType {
    /// Instructions loaded from the user's home configuration.
    #[serde(rename = "home")]
    Home,
    /// Instructions loaded from repository-scoped files.
    #[serde(rename = "repo")]
    Repo,
    /// Instructions loaded from model-specific files.
    #[serde(rename = "model")]
    Model,
    /// Instructions loaded from VS Code instruction files.
    #[serde(rename = "vscode")]
    Vscode,
    /// Instructions discovered from nested agent files.
    #[serde(rename = "nested-agents")]
    NestedAgents,
    /// Instructions inherited from child instruction files.
    #[serde(rename = "child-instructions")]
    ChildInstructions,
    /// Instructions supplied by an installed plugin.
    #[serde(rename = "plugin")]
    Plugin,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Transport the runtime would otherwise use for this request. `http` (the default when absent) covers plain HTTP and SSE responses; `websocket` indicates a full-duplex message channel where each body chunk maps to one WebSocket message and the `binary` flag distinguishes text from binary frames. The SDK consumer uses this to decide whether to service the request with an HTTP client or a WebSocket client. It is the one piece of request metadata the consumer cannot reliably infer from the URL or headers alone.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum LlmInferenceHttpRequestStartTransport {
    /// Plain HTTP or SSE response. Each body chunk is an opaque byte range; the response is a status line, headers, and a (possibly streamed) body.
    #[serde(rename = "http")]
    Http,
    /// Full-duplex WebSocket channel. Each body chunk maps to exactly one WebSocket message and the `binary` flag distinguishes text from binary frames; request and response chunks flow concurrently.
    #[serde(rename = "websocket")]
    Websocket,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Repository host type
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionContextHostType {
    /// Session repository is hosted on GitHub.
    #[serde(rename = "github")]
    GitHub,
    /// Session repository is hosted on Azure DevOps.
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Log severity level. Determines how the message is displayed in the timeline. Defaults to "info".
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionLogLevel {
    /// Informational message.
    #[serde(rename = "info")]
    Info,
    /// Warning message that may require attention.
    #[serde(rename = "warning")]
    Warning,
    /// Error message describing a failure.
    #[serde(rename = "error")]
    Error,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Allowed values for the `McpAppsHostContextDetailsAvailableDisplayMode` enumeration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpAppsHostContextDetailsAvailableDisplayMode {
    /// Rendered inline within the host conversation surface
    #[serde(rename = "inline")]
    Inline,
    /// Rendered as a fullscreen overlay
    #[serde(rename = "fullscreen")]
    Fullscreen,
    /// Rendered as a picture-in-picture floating panel
    #[serde(rename = "pip")]
    Pip,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current display mode (SEP-1865)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpAppsHostContextDetailsDisplayMode {
    /// Rendered inline within the host conversation surface
    #[serde(rename = "inline")]
    Inline,
    /// Rendered as a fullscreen overlay
    #[serde(rename = "fullscreen")]
    Fullscreen,
    /// Rendered as a picture-in-picture floating panel
    #[serde(rename = "pip")]
    Pip,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Platform type for responsive design
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpAppsHostContextDetailsPlatform {
    /// Host runs in a web browser
    #[serde(rename = "web")]
    Web,
    /// Host runs as a desktop application
    #[serde(rename = "desktop")]
    Desktop,
    /// Host runs on a mobile device
    #[serde(rename = "mobile")]
    Mobile,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// UI theme preference per SEP-1865
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpAppsHostContextDetailsTheme {
    /// Light UI theme
    #[serde(rename = "light")]
    Light,
    /// Dark UI theme
    #[serde(rename = "dark")]
    Dark,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Allowed values for the `McpAppsSetHostContextDetailsAvailableDisplayMode` enumeration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpAppsSetHostContextDetailsAvailableDisplayMode {
    /// Rendered inline within the host conversation surface
    #[serde(rename = "inline")]
    Inline,
    /// Rendered as a fullscreen overlay
    #[serde(rename = "fullscreen")]
    Fullscreen,
    /// Rendered as a picture-in-picture floating panel
    #[serde(rename = "pip")]
    Pip,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current display mode (SEP-1865)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpAppsSetHostContextDetailsDisplayMode {
    /// Rendered inline within the host conversation surface
    #[serde(rename = "inline")]
    Inline,
    /// Rendered as a fullscreen overlay
    #[serde(rename = "fullscreen")]
    Fullscreen,
    /// Rendered as a picture-in-picture floating panel
    #[serde(rename = "pip")]
    Pip,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Platform type for responsive design
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpAppsSetHostContextDetailsPlatform {
    /// Host runs in a web browser
    #[serde(rename = "web")]
    Web,
    /// Host runs as a desktop application
    #[serde(rename = "desktop")]
    Desktop,
    /// Host runs on a mobile device
    #[serde(rename = "mobile")]
    Mobile,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// UI theme preference per SEP-1865
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpAppsSetHostContextDetailsTheme {
    /// Light UI theme
    #[serde(rename = "light")]
    Light,
    /// Dark UI theme
    #[serde(rename = "dark")]
    Dark,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthPendingRequestResponseTokenKind {
    #[serde(rename = "token")]
    #[default]
    Token,
}

#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpOauthPendingRequestResponseCancelledKind {
    #[serde(rename = "cancelled")]
    #[default]
    Cancelled,
}

/// Host response to the pending OAuth request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum McpOauthPendingRequestResponse {
    Token(McpOauthPendingRequestResponseToken),
    Cancelled(McpOauthPendingRequestResponseCancelled),
}

/// Outcome of the sampling inference. 'success' produced a response; 'failure' encountered an error (including agent-side rejection by content filter or criteria); 'cancelled' the caller cancelled this execution via cancelSamplingExecution.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpSamplingExecutionAction {
    /// The sampling inference completed and produced a result.
    #[serde(rename = "success")]
    Success,
    /// The sampling inference failed or was rejected.
    #[serde(rename = "failure")]
    Failure,
    /// The sampling inference was cancelled before completion.
    #[serde(rename = "cancelled")]
    Cancelled,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Controls if tools provided by this server can be loaded on demand via tool search (auto) or always included in the initial tool list (never)
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerConfigDeferTools {
    /// Tools may be deferred under certain conditions
    #[serde(rename = "auto")]
    Auto,
    /// Tools are always included in the initial tool list, even when tool search is enabled.
    #[serde(rename = "never")]
    Never,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// OAuth grant type to use when authenticating to the remote MCP server.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerConfigHttpOauthGrantType {
    /// Interactive browser-based authorization code flow with PKCE.
    #[serde(rename = "authorization_code")]
    AuthorizationCode,
    /// Headless client credentials flow using the configured OAuth client.
    #[serde(rename = "client_credentials")]
    ClientCredentials,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Remote transport type. Defaults to "http" when omitted.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpServerConfigHttpType {
    /// Streamable HTTP transport.
    #[serde(rename = "http")]
    Http,
    /// Server-Sent Events transport.
    #[serde(rename = "sse")]
    Sse,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How environment-variable values supplied to MCP servers are resolved. "direct" passes literal string values; "indirect" treats values as references (e.g. names of environment variables on the host) that the runtime resolves before launch. Defaults to the runtime's startup mode; clients that intentionally launch MCP servers with literal values (e.g. CLI prompt mode and ACP) set this to "direct".
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum McpSetEnvValueModeDetails {
    /// Treat MCP server environment values as literal strings.
    #[serde(rename = "direct")]
    Direct,
    /// Treat MCP server environment values as host-side references to resolve before launch.
    #[serde(rename = "indirect")]
    Indirect,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Hosting platform type of the repository
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionWorkingDirectoryContextHostType {
    /// The working directory repository is hosted on GitHub.
    #[serde(rename = "github")]
    GitHub,
    /// The working directory repository is hosted on Azure DevOps.
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot')
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum MetadataSnapshotCurrentMode {
    /// The agent is responding interactively to the user.
    #[serde(rename = "interactive")]
    Interactive,
    /// The agent is preparing a plan before making changes.
    #[serde(rename = "plan")]
    Plan,
    /// The agent is working autonomously toward task completion.
    #[serde(rename = "autopilot")]
    Autopilot,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the remote task originated from Copilot Coding Agent (cca) or a CLI `--remote` invocation.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum MetadataSnapshotRemoteMetadataTaskType {
    /// Remote task originated from Copilot Coding Agent.
    #[serde(rename = "cca")]
    Cca,
    /// Remote task originated from a CLI remote-session invocation.
    #[serde(rename = "cli")]
    Cli,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Model capability category for grouping in the model picker
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelPickerCategory {
    /// Lightweight model category optimized for faster, lower-cost interactions.
    #[serde(rename = "lightweight")]
    Lightweight,
    /// Versatile model category suitable for a broad range of tasks.
    #[serde(rename = "versatile")]
    Versatile,
    /// Powerful model category optimized for complex tasks.
    #[serde(rename = "powerful")]
    Powerful,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Relative cost tier for token-based billing users
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelPickerPriceCategory {
    /// Lowest relative token cost tier.
    #[serde(rename = "low")]
    Low,
    /// Medium relative token cost tier.
    #[serde(rename = "medium")]
    Medium,
    /// High relative token cost tier.
    #[serde(rename = "high")]
    High,
    /// Highest relative token cost tier.
    #[serde(rename = "very_high")]
    VeryHigh,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current policy state for this model
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ModelPolicyState {
    /// The model is enabled by policy.
    #[serde(rename = "enabled")]
    Enabled,
    /// The model is disabled by policy.
    #[serde(rename = "disabled")]
    Disabled,
    /// No explicit policy is configured for the model.
    #[serde(rename = "unconfigured")]
    Unconfigured,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Provider type. Defaults to "openai" for generic OpenAI-compatible APIs.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProviderConfigType {
    /// Generic OpenAI-compatible API.
    #[serde(rename = "openai")]
    Openai,
    /// Azure OpenAI Service endpoint.
    #[serde(rename = "azure")]
    Azure,
    /// Anthropic API endpoint.
    #[serde(rename = "anthropic")]
    Anthropic,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Wire API format (openai/azure only). Defaults to "completions".
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProviderConfigWireApi {
    /// OpenAI Chat Completions wire format.
    #[serde(rename = "completions")]
    Completions,
    /// OpenAI Responses API wire format.
    #[serde(rename = "responses")]
    Responses,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Allowed values for the `OptionsUpdateAdditionalContentExclusionPolicyScope` enumeration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum OptionsUpdateAdditionalContentExclusionPolicyScope {
    /// The content exclusion policy applies to the current repository.
    #[serde(rename = "repo")]
    Repo,
    /// The content exclusion policy applies across all repositories.
    #[serde(rename = "all")]
    All,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Context tier for models with tiered pricing. The session uses this to derive effective `modelCapabilitiesOverrides` so compaction, truncation, token display, and request limits honor the selected tier.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum OptionsUpdateContextTier {
    /// Use the model's default context tier and its standard token limits / pricing.
    #[serde(rename = "default")]
    Default,
    /// Use the model's long-context tier (when available) so larger inputs are accepted and tier-specific pricing applies.
    #[serde(rename = "long_context")]
    LongContext,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How env values are passed to MCP servers (`direct` inlines literal values; `indirect` resolves at launch).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum OptionsUpdateEnvValueMode {
    /// Pass MCP server environment values as literal strings.
    #[serde(rename = "direct")]
    Direct,
    /// Resolve MCP server environment values from host-side references.
    #[serde(rename = "indirect")]
    Indirect,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Reasoning summary mode for supported model clients.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum OptionsUpdateReasoningSummary {
    /// Do not request reasoning summaries from the model.
    #[serde(rename = "none")]
    None,
    /// Request a concise summary of model reasoning.
    #[serde(rename = "concise")]
    Concise,
    /// Request a detailed summary of model reasoning.
    #[serde(rename = "detailed")]
    Detailed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Controls how availableTools (allowlist) and excludedTools (denylist) combine when both are set.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum OptionsUpdateToolFilterPrecedence {
    /// If availableTools is set, it is the only constraint that applies (excludedTools is ignored). Preserves CLI / pre-existing client behavior. Default.
    #[serde(rename = "available")]
    Available,
    /// A tool is enabled if and only if it matches the allowlist (or the allowlist is unset) AND it does not match the denylist. Makes 'all except X' expressible by combining the two lists.
    #[serde(rename = "excluded")]
    Excluded,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Approve this single request only
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveOnceKind {
    #[serde(rename = "approve-once")]
    #[default]
    ApproveOnce,
}

/// Approval scoped to specific command identifiers.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalCommandsKind {
    #[serde(rename = "commands")]
    #[default]
    Commands,
}

/// Approval covering read-only filesystem operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalReadKind {
    #[serde(rename = "read")]
    #[default]
    Read,
}

/// Approval covering filesystem write operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalWriteKind {
    #[serde(rename = "write")]
    #[default]
    Write,
}

/// Approval covering an MCP tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalMcpKind {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// Approval covering MCP sampling requests for a server.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalMcpSamplingKind {
    #[serde(rename = "mcp-sampling")]
    #[default]
    McpSampling,
}

/// Approval covering writes to long-term memory.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalMemoryKind {
    #[serde(rename = "memory")]
    #[default]
    Memory,
}

/// Approval covering a custom tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalCustomToolKind {
    #[serde(rename = "custom-tool")]
    #[default]
    CustomTool,
}

/// Approval covering extension lifecycle operations such as enable, disable, or reload.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalExtensionManagementKind {
    #[serde(rename = "extension-management")]
    #[default]
    ExtensionManagement,
}

/// Approval covering an extension's request to access a permission-gated capability.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionApprovalExtensionPermissionAccessKind {
    #[serde(rename = "extension-permission-access")]
    #[default]
    ExtensionPermissionAccess,
}

/// Session-scoped approval to remember (tool prompts only; omitted for path/url prompts)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionDecisionApproveForSessionApproval {
    Commands(PermissionDecisionApproveForSessionApprovalCommands),
    Read(PermissionDecisionApproveForSessionApprovalRead),
    Write(PermissionDecisionApproveForSessionApprovalWrite),
    Mcp(PermissionDecisionApproveForSessionApprovalMcp),
    McpSampling(PermissionDecisionApproveForSessionApprovalMcpSampling),
    Memory(PermissionDecisionApproveForSessionApprovalMemory),
    CustomTool(PermissionDecisionApproveForSessionApprovalCustomTool),
    ExtensionManagement(PermissionDecisionApproveForSessionApprovalExtensionManagement),
    ExtensionPermissionAccess(PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess),
}

/// Approve and remember for the rest of the session
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForSessionKind {
    #[serde(rename = "approve-for-session")]
    #[default]
    ApproveForSession,
}

/// Approval scoped to specific command identifiers.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalCommandsKind {
    #[serde(rename = "commands")]
    #[default]
    Commands,
}

/// Approval covering read-only filesystem operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalReadKind {
    #[serde(rename = "read")]
    #[default]
    Read,
}

/// Approval covering filesystem write operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalWriteKind {
    #[serde(rename = "write")]
    #[default]
    Write,
}

/// Approval covering an MCP tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalMcpKind {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// Approval covering MCP sampling requests for a server.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalMcpSamplingKind {
    #[serde(rename = "mcp-sampling")]
    #[default]
    McpSampling,
}

/// Approval covering writes to long-term memory.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalMemoryKind {
    #[serde(rename = "memory")]
    #[default]
    Memory,
}

/// Approval covering a custom tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalCustomToolKind {
    #[serde(rename = "custom-tool")]
    #[default]
    CustomTool,
}

/// Approval covering extension lifecycle operations such as enable, disable, or reload.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalExtensionManagementKind {
    #[serde(rename = "extension-management")]
    #[default]
    ExtensionManagement,
}

/// Approval covering an extension's request to access a permission-gated capability.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationApprovalExtensionPermissionAccessKind {
    #[serde(rename = "extension-permission-access")]
    #[default]
    ExtensionPermissionAccess,
}

/// Approval to persist for this location
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionDecisionApproveForLocationApproval {
    Commands(PermissionDecisionApproveForLocationApprovalCommands),
    Read(PermissionDecisionApproveForLocationApprovalRead),
    Write(PermissionDecisionApproveForLocationApprovalWrite),
    Mcp(PermissionDecisionApproveForLocationApprovalMcp),
    McpSampling(PermissionDecisionApproveForLocationApprovalMcpSampling),
    Memory(PermissionDecisionApproveForLocationApprovalMemory),
    CustomTool(PermissionDecisionApproveForLocationApprovalCustomTool),
    ExtensionManagement(PermissionDecisionApproveForLocationApprovalExtensionManagement),
    ExtensionPermissionAccess(
        PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess,
    ),
}

/// Approve and persist for this project location
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApproveForLocationKind {
    #[serde(rename = "approve-for-location")]
    #[default]
    ApproveForLocation,
}

/// Approve and persist across sessions (URL prompts only)
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApprovePermanentlyKind {
    #[serde(rename = "approve-permanently")]
    #[default]
    ApprovePermanently,
}

/// Reject the request
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionRejectKind {
    #[serde(rename = "reject")]
    #[default]
    Reject,
}

/// No user is available to confirm the request
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionUserNotAvailableKind {
    #[serde(rename = "user-not-available")]
    #[default]
    UserNotAvailable,
}

/// The permission request was approved
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApprovedKind {
    #[serde(rename = "approved")]
    #[default]
    Approved,
}

/// Approved and remembered for the rest of the session
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApprovedForSessionKind {
    #[serde(rename = "approved-for-session")]
    #[default]
    ApprovedForSession,
}

/// Approved and persisted for this project location
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionApprovedForLocationKind {
    #[serde(rename = "approved-for-location")]
    #[default]
    ApprovedForLocation,
}

/// The permission request was cancelled before a response was used
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionCancelledKind {
    #[serde(rename = "cancelled")]
    #[default]
    Cancelled,
}

/// Denied because approval rules explicitly blocked it
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionDeniedByRulesKind {
    #[serde(rename = "denied-by-rules")]
    #[default]
    DeniedByRules,
}

/// Denied because no approval rule matched and user confirmation was unavailable
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind {
    #[serde(rename = "denied-no-approval-rule-and-could-not-request-from-user")]
    #[default]
    DeniedNoApprovalRuleAndCouldNotRequestFromUser,
}

/// Denied by the user during an interactive prompt
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionDeniedInteractivelyByUserKind {
    #[serde(rename = "denied-interactively-by-user")]
    #[default]
    DeniedInteractivelyByUser,
}

/// Denied by the organization's content exclusion policy
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionDeniedByContentExclusionPolicyKind {
    #[serde(rename = "denied-by-content-exclusion-policy")]
    #[default]
    DeniedByContentExclusionPolicy,
}

/// Denied by a permission request hook registered by an extension or plugin
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionDecisionDeniedByPermissionRequestHookKind {
    #[serde(rename = "denied-by-permission-request-hook")]
    #[default]
    DeniedByPermissionRequestHook,
}

/// The client's response to the pending permission prompt
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionDecision {
    ApproveOnce(PermissionDecisionApproveOnce),
    ApproveForSession(PermissionDecisionApproveForSession),
    ApproveForLocation(PermissionDecisionApproveForLocation),
    ApprovePermanently(PermissionDecisionApprovePermanently),
    Reject(PermissionDecisionReject),
    UserNotAvailable(PermissionDecisionUserNotAvailable),
    Approved(PermissionDecisionApproved),
    ApprovedForSession(PermissionDecisionApprovedForSession),
    ApprovedForLocation(PermissionDecisionApprovedForLocation),
    Cancelled(PermissionDecisionCancelled),
    DeniedByRules(PermissionDecisionDeniedByRules),
    DeniedNoApprovalRuleAndCouldNotRequestFromUser(
        PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser,
    ),
    DeniedInteractivelyByUser(PermissionDecisionDeniedInteractivelyByUser),
    DeniedByContentExclusionPolicy(PermissionDecisionDeniedByContentExclusionPolicy),
    DeniedByPermissionRequestHook(PermissionDecisionDeniedByPermissionRequestHook),
}

/// Approval scoped to specific command identifiers.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsCommandsKind {
    #[serde(rename = "commands")]
    #[default]
    Commands,
}

/// Approval covering read-only filesystem operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsReadKind {
    #[serde(rename = "read")]
    #[default]
    Read,
}

/// Approval covering filesystem write operations.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsWriteKind {
    #[serde(rename = "write")]
    #[default]
    Write,
}

/// Approval covering an MCP tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsMcpKind {
    #[serde(rename = "mcp")]
    #[default]
    Mcp,
}

/// Approval covering MCP sampling requests for a server.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsMcpSamplingKind {
    #[serde(rename = "mcp-sampling")]
    #[default]
    McpSampling,
}

/// Approval covering writes to long-term memory.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsMemoryKind {
    #[serde(rename = "memory")]
    #[default]
    Memory,
}

/// Approval covering a custom tool.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsCustomToolKind {
    #[serde(rename = "custom-tool")]
    #[default]
    CustomTool,
}

/// Approval covering extension lifecycle operations such as enable, disable, or reload.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsExtensionManagementKind {
    #[serde(rename = "extension-management")]
    #[default]
    ExtensionManagement,
}

/// Approval covering an extension's request to access a permission-gated capability.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccessKind {
    #[serde(rename = "extension-permission-access")]
    #[default]
    ExtensionPermissionAccess,
}

/// Tool approval to persist and apply
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum PermissionsLocationsAddToolApprovalDetails {
    Commands(PermissionsLocationsAddToolApprovalDetailsCommands),
    Read(PermissionsLocationsAddToolApprovalDetailsRead),
    Write(PermissionsLocationsAddToolApprovalDetailsWrite),
    Mcp(PermissionsLocationsAddToolApprovalDetailsMcp),
    McpSampling(PermissionsLocationsAddToolApprovalDetailsMcpSampling),
    Memory(PermissionsLocationsAddToolApprovalDetailsMemory),
    CustomTool(PermissionsLocationsAddToolApprovalDetailsCustomTool),
    ExtensionManagement(PermissionsLocationsAddToolApprovalDetailsExtensionManagement),
    ExtensionPermissionAccess(PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess),
}

/// Whether the location is a git repo or directory
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionLocationType {
    /// The permission location is persisted at the git repository root.
    #[serde(rename = "repo")]
    Repo,
    /// The permission location is persisted at the working directory.
    #[serde(rename = "dir")]
    Dir,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Allowed values for the `PermissionsConfigureAdditionalContentExclusionPolicyScope` enumeration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsConfigureAdditionalContentExclusionPolicyScope {
    /// The content exclusion policy applies to the current repository.
    #[serde(rename = "repo")]
    Repo,
    /// The content exclusion policy applies across all repositories.
    #[serde(rename = "all")]
    All,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the change applies to ephemeral session-scoped rules (cleared at session end) or to location-scoped rules persisted via the location-permissions config file.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsModifyRulesScope {
    /// Apply the rule change only to this session.
    #[serde(rename = "session")]
    Session,
    /// Persist the rule change for this project location.
    #[serde(rename = "location")]
    Location,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsSetAllowAllSource {
    /// Allow-all was enabled from a CLI command-line flag.
    #[serde(rename = "cli_flag")]
    CliFlag,
    /// Allow-all was enabled by a slash command.
    #[serde(rename = "slash_command")]
    SlashCommand,
    /// Allow-all was enabled by confirming autopilot behavior.
    #[serde(rename = "autopilot_confirmation")]
    AutopilotConfirmation,
    /// Allow-all was enabled through an RPC caller.
    #[serde(rename = "rpc")]
    Rpc,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Optional source for allow-all telemetry. Defaults to `rpc` when omitted for SDK callers.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PermissionsSetApproveAllSource {
    /// Allow-all was enabled from a CLI command-line flag.
    #[serde(rename = "cli_flag")]
    CliFlag,
    /// Allow-all was enabled by a slash command.
    #[serde(rename = "slash_command")]
    SlashCommand,
    /// Allow-all was enabled by confirming autopilot behavior.
    #[serde(rename = "autopilot_confirmation")]
    AutopilotConfirmation,
    /// Allow-all was enabled through an RPC caller.
    #[serde(rename = "rpc")]
    Rpc,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Provider family. Matches the `type` field of a BYOK provider config.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProviderEndpointType {
    /// OpenAI-compatible endpoint (use the OpenAI client library).
    #[serde(rename = "openai")]
    Openai,
    /// Azure OpenAI endpoint (use the OpenAI client library with the Azure base URL).
    #[serde(rename = "azure")]
    Azure,
    /// Anthropic endpoint (use the Anthropic client library).
    #[serde(rename = "anthropic")]
    Anthropic,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Wire API to be used, when required for the provider type.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ProviderEndpointWireApi {
    /// Classic chat-completions request shape.
    #[serde(rename = "completions")]
    Completions,
    /// Newer responses request shape.
    #[serde(rename = "responses")]
    Responses,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentBlobType {
    #[serde(rename = "blob")]
    #[default]
    Blob,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentDirectoryType {
    #[serde(rename = "directory")]
    #[default]
    Directory,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentFileType {
    #[serde(rename = "file")]
    #[default]
    File,
}

/// Type of GitHub reference
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentGitHubReferenceType {
    /// GitHub issue reference.
    #[serde(rename = "issue")]
    Issue,
    /// GitHub pull request reference.
    #[serde(rename = "pr")]
    Pr,
    /// GitHub discussion reference.
    #[serde(rename = "discussion")]
    Discussion,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Attachment type discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum PushAttachmentSelectionType {
    #[serde(rename = "selection")]
    #[default]
    Selection,
}

/// Whether this item is a queued user message or a queued slash command / model change
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum QueuePendingItemsKind {
    /// A queued user message.
    #[serde(rename = "message")]
    Message,
    /// A queued slash command or model-change command.
    #[serde(rename = "command")]
    Command,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Remote control state tag: active.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteControlStatusActiveState {
    #[serde(rename = "active")]
    #[default]
    Active,
}

/// Remote control state tag: connecting.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteControlStatusConnectingState {
    #[serde(rename = "connecting")]
    #[default]
    Connecting,
}

/// Remote control state tag: setup failed.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteControlStatusErrorState {
    #[serde(rename = "error")]
    #[default]
    Error,
}

/// Remote control state tag: not connected.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteControlStatusOffState {
    #[serde(rename = "off")]
    #[default]
    Off,
}

/// Per-session remote mode. "off" disables remote, "export" exports session events to GitHub without enabling remote steering, "on" enables both export and remote steering.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteSessionMode {
    /// Disable remote session export and steering.
    #[serde(rename = "off")]
    Off,
    /// Export session events to GitHub without enabling remote steering.
    #[serde(rename = "export")]
    Export,
    /// Enable both remote session export and remote steering.
    #[serde(rename = "on")]
    On,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether the remote task originated from CCA or CLI `--remote`.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum RemoteSessionMetadataTaskType {
    /// GitHub Copilot coding agent task.
    #[serde(rename = "cca")]
    Cca,
    /// CLI remote task.
    #[serde(rename = "cli")]
    Cli,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// The UI mode the agent was in when this message was sent. Defaults to the session's current mode.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SendAgentMode {
    /// The agent is responding interactively to the user.
    #[serde(rename = "interactive")]
    Interactive,
    /// The agent is preparing a plan before making changes.
    #[serde(rename = "plan")]
    Plan,
    /// The agent is working autonomously toward task completion.
    #[serde(rename = "autopilot")]
    Autopilot,
    /// The agent is in shell-focused UI mode.
    #[serde(rename = "shell")]
    Shell,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How to deliver the message. `enqueue` (default) appends to the message queue. `immediate` interjects during an in-progress turn.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SendMode {
    /// Append the message to the normal session queue.
    #[serde(rename = "enqueue")]
    Enqueue,
    /// Interject the message during the in-progress turn.
    #[serde(rename = "immediate")]
    Immediate,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Session capability enabled for this session
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionCapability {
    /// TUI-specific prompt hints such as keyboard shortcuts.
    #[serde(rename = "tui-hints")]
    TuiHints,
    /// Plan-mode handling and instructions.
    #[serde(rename = "plan-mode")]
    PlanMode,
    /// Memory tool and memories prompt section.
    #[serde(rename = "memory")]
    Memory,
    /// Copilot CLI documentation tool and prompt section.
    #[serde(rename = "cli-documentation")]
    CliDocumentation,
    /// Interactive ask_user tool support.
    #[serde(rename = "ask-user")]
    AskUser,
    /// Interactive CLI identity and behavior.
    #[serde(rename = "interactive-mode")]
    InteractiveMode,
    /// Automatic hidden system notifications.
    #[serde(rename = "system-notifications")]
    SystemNotifications,
    /// SDK elicitation support.
    #[serde(rename = "elicitation")]
    Elicitation,
    /// Cross-session history tools and session-store SQL prompt/tool metadata.
    #[serde(rename = "session-store")]
    SessionStore,
    /// MCP Apps UI passthrough.
    #[serde(rename = "mcp-apps")]
    McpApps,
    /// Host-provided canvas rendering support.
    #[serde(rename = "canvas-renderer")]
    CanvasRenderer,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Error classification
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsErrorCode {
    /// The requested path does not exist.
    ENOENT,
    /// The filesystem operation failed for an unspecified reason.
    UNKNOWN,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Entry type
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsReaddirWithTypesEntryType {
    /// The entry is a file.
    #[serde(rename = "file")]
    File,
    /// The entry is a directory.
    #[serde(rename = "directory")]
    Directory,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Path conventions used by this filesystem
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsSetProviderConventions {
    /// Paths use Windows path conventions.
    #[serde(rename = "windows")]
    Windows,
    /// Paths use POSIX path conventions.
    #[serde(rename = "posix")]
    Posix,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How to execute the query: 'exec' for DDL/multi-statement (no results), 'query' for SELECT (returns rows), 'run' for INSERT/UPDATE/DELETE (returns rowsAffected)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionFsSqliteQueryType {
    /// Execute DDL or multi-statement SQL without returning rows.
    #[serde(rename = "exec")]
    Exec,
    /// Execute a SELECT-style query and return rows.
    #[serde(rename = "query")]
    Query,
    /// Execute INSERT, UPDATE, or DELETE SQL and return affected-row metadata.
    #[serde(rename = "run")]
    Run,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Constant value. Always "github".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionInstalledPluginSourceGitHubSource {
    #[serde(rename = "github")]
    #[default]
    GitHub,
}

/// Constant value. Always "local".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionInstalledPluginSourceLocalSource {
    #[serde(rename = "local")]
    #[default]
    Local,
}

/// Constant value. Always "url".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionInstalledPluginSourceUrlSource {
    #[serde(rename = "url")]
    #[default]
    Url,
}

/// Repository host type, if known
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkspaceSummaryHostType {
    /// Workspace summary repository is hosted on GitHub.
    #[serde(rename = "github")]
    GitHub,
    /// Workspace summary repository is hosted on Azure DevOps.
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Allowed values for the `SessionOpenOptionsAdditionalContentExclusionPolicyScope` enumeration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionOpenOptionsAdditionalContentExclusionPolicyScope {
    /// The content exclusion policy applies to the current repository.
    #[serde(rename = "repo")]
    Repo,
    /// The content exclusion policy applies across all repositories.
    #[serde(rename = "all")]
    All,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// How MCP server environment values are interpreted.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionOpenOptionsEnvValueMode {
    /// Pass MCP server environment values as literal strings.
    #[serde(rename = "direct")]
    Direct,
    /// Resolve MCP server environment values from host-side references.
    #[serde(rename = "indirect")]
    Indirect,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Initial reasoning summary mode for supported model clients.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionOpenOptionsReasoningSummary {
    /// Do not request reasoning summaries from the model.
    #[serde(rename = "none")]
    None,
    /// Request a concise summary of model reasoning.
    #[serde(rename = "concise")]
    Concise,
    /// Request a detailed summary of model reasoning.
    #[serde(rename = "detailed")]
    Detailed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Create a new local session.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenCreateKind {
    #[serde(rename = "create")]
    #[default]
    Create,
}

/// Resume a specific local session by ID or prefix.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenResumeKind {
    #[serde(rename = "resume")]
    #[default]
    Resume,
}

/// Resume the most relevant existing local session.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenResumeLastKind {
    #[serde(rename = "resumeLast")]
    #[default]
    ResumeLast,
}

/// Attach to an already-active in-process session by ID. Unlike `resume`, this does NOT re-load from disk; the session must already be loaded by an earlier `create`/`resume` call. Returns `status: 'not_found'` when no active session matches the id. Useful for in-process consumers that need a fresh API handle to a session opened elsewhere (e.g., a peer foreground-session switch).
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenAttachKind {
    #[serde(rename = "attach")]
    #[default]
    Attach,
}

/// Connect to a live remote session.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenRemoteKind {
    #[serde(rename = "remote")]
    #[default]
    Remote,
}

/// Create a new cloud (coding-agent) session.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenCloudKind {
    #[serde(rename = "cloud")]
    #[default]
    Cloud,
}

/// Fetch a remote session and hand it off to a new local session.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenHandoffKind {
    #[serde(rename = "handoff")]
    #[default]
    Handoff,
}

/// Task type determines the handoff strategy (CCA fetches events; CLI prepares a transient session).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenHandoffTaskType {
    /// GitHub Copilot coding agent task.
    #[serde(rename = "cca")]
    Cca,
    /// CLI remote task.
    #[serde(rename = "cli")]
    Cli,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Open a session by creating, resuming, attaching, connecting to a remote, or handing off.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum SessionOpenParams {
    Create(SessionsOpenCreate),
    Resume(SessionsOpenResume),
    ResumeLast(SessionsOpenResumeLast),
    Attach(SessionsOpenAttach),
    Remote(SessionsOpenRemote),
    Cloud(SessionsOpenCloud),
    Handoff(SessionsOpenHandoff),
}

/// Step status.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenProgressStatus {
    /// The step has started and has not yet finished.
    #[serde(rename = "in-progress")]
    InProgress,
    /// The step has completed successfully.
    #[serde(rename = "complete")]
    Complete,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Handoff step.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenProgressStep {
    /// Loading the source session's events from the remote service.
    #[serde(rename = "load-session")]
    LoadSession,
    /// Validating that the local repository matches the remote session's repository.
    #[serde(rename = "validate-repo")]
    ValidateRepo,
    /// Checking the local working tree for uncommitted changes that would block the handoff.
    #[serde(rename = "check-changes")]
    CheckChanges,
    /// Checking out the branch associated with the remote session in the local working tree.
    #[serde(rename = "checkout-branch")]
    CheckoutBranch,
    /// Creating the new local session and seeding it with the source session's events.
    #[serde(rename = "create-session")]
    CreateSession,
    /// Persisting the newly-created local session to disk.
    #[serde(rename = "save-session")]
    SaveSession,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Outcome of the open request.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionsOpenStatus {
    /// A new session was created.
    #[serde(rename = "created")]
    Created,
    /// An existing session was loaded or reattached.
    #[serde(rename = "resumed")]
    Resumed,
    /// No matching persisted session was found.
    #[serde(rename = "not_found")]
    NotFound,
    /// Connected to an existing remote session.
    #[serde(rename = "connected")]
    Connected,
    /// Remote session was handed off to a new local session.
    #[serde(rename = "handed_off")]
    HandedOff,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Which session sources to include. Defaults to `local` for backward compatibility.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SessionSource {
    /// Return only local sessions.
    #[serde(rename = "local")]
    Local,
    /// Return only remote sessions.
    #[serde(rename = "remote")]
    Remote,
    /// Return both local and remote sessions.
    #[serde(rename = "all")]
    All,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Signal to send (default: SIGTERM)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum ShellKillSignal {
    /// Request graceful process termination.
    SIGTERM,
    /// Forcefully terminate the process.
    SIGKILL,
    /// Send an interrupt signal to the process.
    SIGINT,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Which tier this directory belongs to
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SkillDiscoveryScope {
    /// A project's repository skill directory.
    #[serde(rename = "project")]
    Project,
    /// The user's personal Copilot skill directory.
    #[serde(rename = "personal-copilot")]
    PersonalCopilot,
    /// The user's personal agents skill directory.
    #[serde(rename = "personal-agents")]
    PersonalAgents,
    /// A configured custom skill directory.
    #[serde(rename = "custom")]
    Custom,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Agent prompt result discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandAgentPromptResultKind {
    #[serde(rename = "agent-prompt")]
    #[default]
    AgentPrompt,
}

/// Completed result discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandCompletedResultKind {
    #[serde(rename = "completed")]
    #[default]
    Completed,
}

/// Text result discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandTextResultKind {
    #[serde(rename = "text")]
    #[default]
    Text,
}

/// Select subcommand result discriminator
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SlashCommandSelectSubcommandResultKind {
    #[serde(rename = "select-subcommand")]
    #[default]
    SelectSubcommand,
}

/// Result of invoking the slash command (text output, prompt to send to the agent, or completion).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(untagged)]
pub enum SlashCommandInvocationResult {
    Text(SlashCommandTextResult),
    AgentPrompt(SlashCommandAgentPromptResult),
    Completed(SlashCommandCompletedResult),
    SelectSubcommand(SlashCommandSelectSubcommandResult),
}

/// Context tier override for matching subagents
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SubagentSettingsEntryContextTier {
    /// Inherit the parent session's effective context tier at dispatch time.
    #[serde(rename = "inherit")]
    Inherit,
    /// Use the model's default context window.
    #[serde(rename = "default")]
    Default,
    /// Pin the subagent to the long-context tier when supported.
    #[serde(rename = "long_context")]
    LongContext,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Whether task execution is synchronously awaited or managed in the background
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskExecutionMode {
    /// The task was started with synchronous waiting.
    #[serde(rename = "sync")]
    Sync,
    /// The task is managed in the background.
    #[serde(rename = "background")]
    Background,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Current lifecycle status of the task
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskStatus {
    /// The task is actively executing.
    #[serde(rename = "running")]
    Running,
    /// The task is waiting for additional input.
    #[serde(rename = "idle")]
    Idle,
    /// The task finished successfully.
    #[serde(rename = "completed")]
    Completed,
    /// The task finished with an error.
    #[serde(rename = "failed")]
    Failed,
    /// The task was cancelled before completion.
    #[serde(rename = "cancelled")]
    Cancelled,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Task kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskAgentInfoType {
    #[serde(rename = "agent")]
    #[default]
    Agent,
}

/// Progress kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskAgentProgressType {
    #[serde(rename = "agent")]
    #[default]
    Agent,
}

/// Whether the shell runs inside a managed PTY session or as an independent background process
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskShellInfoAttachmentMode {
    /// The shell runs in a managed PTY session.
    #[serde(rename = "attached")]
    Attached,
    /// The shell runs as an independent background process.
    #[serde(rename = "detached")]
    Detached,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Task kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskShellInfoType {
    #[serde(rename = "shell")]
    #[default]
    Shell,
}

/// Progress kind
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TaskShellProgressType {
    #[serde(rename = "shell")]
    #[default]
    Shell,
}

/// SDK-side token authentication; the host configured the token directly via the SDK.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum TokenAuthInfoType {
    #[serde(rename = "token")]
    #[default]
    Token,
}

/// User's choice for auto-mode switching: yes (allow this turn), yes_always (allow + persist as setting), or no (decline).
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIAutoModeSwitchResponse {
    /// Allow the automatic mode switch for this turn.
    #[serde(rename = "yes")]
    Yes,
    /// Allow this mode switch and persist the preference.
    #[serde(rename = "yes_always")]
    YesAlways,
    /// Decline the automatic mode switch.
    #[serde(rename = "no")]
    No,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Type discriminator. Always "array".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationArrayAnyOfFieldType {
    #[serde(rename = "array")]
    #[default]
    Array,
}

/// Type discriminator. Always "string".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationArrayEnumFieldItemsType {
    #[serde(rename = "string")]
    #[default]
    String,
}

/// Type discriminator. Always "array".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationArrayEnumFieldType {
    #[serde(rename = "array")]
    #[default]
    Array,
}

/// Schema type indicator (always 'object')
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaType {
    #[serde(rename = "object")]
    #[default]
    Object,
}

/// The user's response: accept (submitted), decline (rejected), or cancel (dismissed)
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationResponseAction {
    /// The user submitted the requested form values.
    #[serde(rename = "accept")]
    Accept,
    /// The user explicitly declined to provide the requested input.
    #[serde(rename = "decline")]
    Decline,
    /// The user dismissed the elicitation request.
    #[serde(rename = "cancel")]
    Cancel,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Type discriminator. Always "boolean".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyBooleanType {
    #[serde(rename = "boolean")]
    #[default]
    Boolean,
}

/// Numeric type accepted by the field.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyNumberType {
    /// Any JSON number.
    #[serde(rename = "number")]
    Number,
    /// Integer JSON number.
    #[serde(rename = "integer")]
    Integer,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Optional format hint that constrains the accepted input.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyStringFormat {
    /// Email address string format.
    #[serde(rename = "email")]
    Email,
    /// URI string format.
    #[serde(rename = "uri")]
    Uri,
    /// Calendar date string format.
    #[serde(rename = "date")]
    Date,
    /// Date-time string format.
    #[serde(rename = "date-time")]
    DateTime,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Type discriminator. Always "string".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationSchemaPropertyStringType {
    #[serde(rename = "string")]
    #[default]
    String,
}

/// Type discriminator. Always "string".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationStringEnumFieldType {
    #[serde(rename = "string")]
    #[default]
    String,
}

/// Type discriminator. Always "string".
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIElicitationStringOneOfFieldType {
    #[serde(rename = "string")]
    #[default]
    String,
}

/// The action the user selected. Defaults to 'autopilot' when autoApproveEdits is true, otherwise 'interactive'.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UIExitPlanModeAction {
    /// Exit plan mode without starting implementation.
    #[serde(rename = "exit_only")]
    ExitOnly,
    /// Exit plan mode and continue interactively.
    #[serde(rename = "interactive")]
    Interactive,
    /// Exit plan mode and continue in autopilot mode.
    #[serde(rename = "autopilot")]
    Autopilot,
    /// Exit plan mode and continue in autopilot mode with parallel subagent execution.
    #[serde(rename = "autopilot_fleet")]
    AutopilotFleet,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// OAuth user authentication. The token itself is held in the runtime's secret token store (keyed by host+login) and is NOT carried in this struct.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum UserAuthInfoType {
    #[serde(rename = "user")]
    #[default]
    User,
}

/// Type of change represented by this file diff.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkspaceDiffFileChangeType {
    /// The file was added.
    #[serde(rename = "added")]
    Added,
    /// The file was modified.
    #[serde(rename = "modified")]
    Modified,
    /// The file was deleted.
    #[serde(rename = "deleted")]
    Deleted,
    /// The file was renamed.
    #[serde(rename = "renamed")]
    Renamed,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Diff mode requested by the client.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkspaceDiffMode {
    /// Return staged, unstaged, and untracked working tree changes.
    #[serde(rename = "unstaged")]
    Unstaged,
    /// Return changes compared with the default branch.
    #[serde(rename = "branch")]
    Branch,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}

/// Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration.
///
/// <div class="warning">
///
/// **Experimental.** This type is part of an experimental wire-protocol surface
/// and may change or be removed in future SDK or CLI releases.
///
/// </div>
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum WorkspacesWorkspaceDetailsHostType {
    /// Workspace repository is hosted on GitHub.
    #[serde(rename = "github")]
    GitHub,
    /// Workspace repository is hosted on Azure DevOps.
    #[serde(rename = "ado")]
    Ado,
    /// Unknown variant for forward compatibility.
    #[default]
    #[serde(other)]
    Unknown,
}
