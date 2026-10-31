//! Auto-generated from api.schema.json — do not edit manually.

#![allow(clippy::large_enum_variant)]
#![allow(deprecated)]
#![allow(dead_code)]
#![allow(unused_imports)]
#![allow(rustdoc::invalid_html_tags)]

use std::collections::HashMap;

use serde::{Deserialize, Serialize};

pub(crate) use super::lifecycle_diagnostics::LifecycleDiagnosticEvent;
pub use super::session_events::{
    AbortReason, AgentModelPolicy, AssistedApprovalAssessment, AutoTier, BlockedPermissionAction,
    ContextTier, IndexedSearchState, ManagedPermissionsContext, ManagedSettingsResolvedSource,
    McpOauthHttpResponse, McpOauthWWWAuthenticateParams, McpServerMetadata, McpServerSource,
    McpServerStatus, McpSkillOrigin, ModelChangeSource, ModelProviderKind, ModelProviderRef,
    OmittedBinaryOmittedReason, PermissionDecisionSource, PermissionMode, PermissionPromptRequest,
    PermissionRule, PolicyHelperCapabilities, PolicyHelperFailure, ProviderQuotaState,
    ReasoningSummary, RemediationAction, SessionLimitsConfig, SessionMode, ShutdownType,
    SkillSource, TaskBlocker, TaskCompletionOutcome, UsageGetMetricsResult,
    UserToolSessionApproval, Verbosity,
};
use crate::types::{RequestId, SessionEvent, SessionId};

/// JSON-RPC method name constants.
pub mod rpc_methods {
    /// `sandbox.proxyCa.checkInstallationSupport`
    pub const SANDBOX_PROXYCA_CHECKINSTALLATIONSUPPORT: &str =
        "sandbox.proxyCa.checkInstallationSupport";
    /// `sandbox.proxyCa.checkRelevance`
    pub const SANDBOX_PROXYCA_CHECKRELEVANCE: &str = "sandbox.proxyCa.checkRelevance";
    /// `sandbox.proxyCa.getStatus`
    pub const SANDBOX_PROXYCA_GETSTATUS: &str = "sandbox.proxyCa.getStatus";
    /// `sandbox.proxyCa.create`
    pub const SANDBOX_PROXYCA_CREATE: &str = "sandbox.proxyCa.create";
    /// `sandbox.proxyCa.rotate`
    pub const SANDBOX_PROXYCA_ROTATE: &str = "sandbox.proxyCa.rotate";
    /// `sandbox.proxyCa.trust`
    pub const SANDBOX_PROXYCA_TRUST: &str = "sandbox.proxyCa.trust";
    /// `sandbox.proxyCa.remove`
    pub const SANDBOX_PROXYCA_REMOVE: &str = "sandbox.proxyCa.remove";
    /// `sandbox.proxySecret.getLocation`
    pub(crate) const SANDBOX_PROXYSECRET_GETLOCATION: &str = "sandbox.proxySecret.getLocation";
    /// `sandbox.proxySecret.get`
    pub(crate) const SANDBOX_PROXYSECRET_GET: &str = "sandbox.proxySecret.get";
    /// `sandbox.proxySecret.save`
    pub(crate) const SANDBOX_PROXYSECRET_SAVE: &str = "sandbox.proxySecret.save";
    /// `sandbox.proxySecret.delete`
    pub(crate) const SANDBOX_PROXYSECRET_DELETE: &str = "sandbox.proxySecret.delete";
    /// `sandbox.settings.applyDefaults`
    pub(crate) const SANDBOX_SETTINGS_APPLYDEFAULTS: &str = "sandbox.settings.applyDefaults";
    /// `sandbox.resolveFlooredConfig`
    pub(crate) const SANDBOX_RESOLVEFLOOREDCONFIG: &str = "sandbox.resolveFlooredConfig";
    /// `sandbox.preparePickerPolicy`
    pub(crate) const SANDBOX_PREPAREPICKERPOLICY: &str = "sandbox.preparePickerPolicy";
    /// `sandbox.filterPickerCandidates`
    pub(crate) const SANDBOX_FILTERPICKERCANDIDATES: &str = "sandbox.filterPickerCandidates";
    /// `sandbox.resolveForOptional`
    pub(crate) const SANDBOX_RESOLVEFOROPTIONAL: &str = "sandbox.resolveForOptional";
    /// `sandbox.checkPlatformSupport`
    pub(crate) const SANDBOX_CHECKPLATFORMSUPPORT: &str = "sandbox.checkPlatformSupport";
    /// `sandbox.checkWindowsHostLoopbackSupport`
    pub(crate) const SANDBOX_CHECKWINDOWSHOSTLOOPBACKSUPPORT: &str =
        "sandbox.checkWindowsHostLoopbackSupport";
    /// `sandbox.getUnsupportedMessage`
    pub(crate) const SANDBOX_GETUNSUPPORTEDMESSAGE: &str = "sandbox.getUnsupportedMessage";
    /// `sandbox.getUnsupportedSettingDisabledNotice`
    pub(crate) const SANDBOX_GETUNSUPPORTEDSETTINGDISABLEDNOTICE: &str =
        "sandbox.getUnsupportedSettingDisabledNotice";
    /// `sandbox.getUnsupportedStartupNotice`
    pub(crate) const SANDBOX_GETUNSUPPORTEDSTARTUPNOTICE: &str =
        "sandbox.getUnsupportedStartupNotice";
    /// `sandbox.getPathGrantWarning`
    pub(crate) const SANDBOX_GETPATHGRANTWARNING: &str = "sandbox.getPathGrantWarning";
    /// `sandbox.checkEnforcementCommand`
    pub(crate) const SANDBOX_CHECKENFORCEMENTCOMMAND: &str = "sandbox.checkEnforcementCommand";
    /// `sandbox.getDocsUrl`
    pub(crate) const SANDBOX_GETDOCSURL: &str = "sandbox.getDocsUrl";
    /// `sandbox.getHostSupport`
    pub const SANDBOX_GETHOSTSUPPORT: &str = "sandbox.getHostSupport";
    /// `persistence.writeRawJsonLocked`
    pub(crate) const PERSISTENCE_WRITERAWJSONLOCKED: &str = "persistence.writeRawJsonLocked";
    /// `user.settings.setHookDisabled`
    pub(crate) const USER_SETTINGS_SETHOOKDISABLED: &str = "user.settings.setHookDisabled";
    /// `user.settings.loadForConfigDir`
    pub(crate) const USER_SETTINGS_LOADFORCONFIGDIR: &str = "user.settings.loadForConfigDir";
    /// `user.settings.loadRepo`
    pub(crate) const USER_SETTINGS_LOADREPO: &str = "user.settings.loadRepo";
    /// `user.settings.loadAndMergeRepo`
    pub(crate) const USER_SETTINGS_LOADANDMERGEREPO: &str = "user.settings.loadAndMergeRepo";
    /// `user.settings.getPath`
    pub(crate) const USER_SETTINGS_GETPATH: &str = "user.settings.getPath";
    /// `user.settings.loadWithWarning`
    pub(crate) const USER_SETTINGS_LOADWITHWARNING: &str = "user.settings.loadWithWarning";
    /// `user.settings.mergeManagedAndUser`
    pub(crate) const USER_SETTINGS_MERGEMANAGEDANDUSER: &str = "user.settings.mergeManagedAndUser";
    /// `user.settings.mergeRepoValue`
    pub(crate) const USER_SETTINGS_MERGEREPOVALUE: &str = "user.settings.mergeRepoValue";
    /// `user.settings.migrateStaffUpdateChannelOnce`
    pub(crate) const USER_SETTINGS_MIGRATESTAFFUPDATECHANNELONCE: &str =
        "user.settings.migrateStaffUpdateChannelOnce";
    /// `user.settings.resetStaffModelSelectionOnce`
    pub(crate) const USER_SETTINGS_RESETSTAFFMODELSELECTIONONCE: &str =
        "user.settings.resetStaffModelSelectionOnce";
    /// `user.settings.getUnknownKeysWarning`
    pub(crate) const USER_SETTINGS_GETUNKNOWNKEYSWARNING: &str =
        "user.settings.getUnknownKeysWarning";
    /// `user.settings.buildRuntime`
    pub(crate) const USER_SETTINGS_BUILDRUNTIME: &str = "user.settings.buildRuntime";
    /// `user.settings.migrateStaffLogLevelOnce`
    pub(crate) const USER_SETTINGS_MIGRATESTAFFLOGLEVELONCE: &str =
        "user.settings.migrateStaffLogLevelOnce";
    /// `user.settings.migrateObsoleteOnDisk`
    pub(crate) const USER_SETTINGS_MIGRATEOBSOLETEONDISK: &str =
        "user.settings.migrateObsoleteOnDisk";
    /// `user.settings.get`
    pub const USER_SETTINGS_GET: &str = "user.settings.get";
    /// `user.settings.getPaths`
    pub(crate) const USER_SETTINGS_GETPATHS: &str = "user.settings.getPaths";
    /// `user.settings.set`
    pub const USER_SETTINGS_SET: &str = "user.settings.set";
    /// `user.settings.listGovernanceKeys`
    pub(crate) const USER_SETTINGS_LISTGOVERNANCEKEYS: &str = "user.settings.listGovernanceKeys";
    /// `user.settings.edit`
    pub(crate) const USER_SETTINGS_EDIT: &str = "user.settings.edit";
    /// `user.settings.normalize`
    pub(crate) const USER_SETTINGS_NORMALIZE: &str = "user.settings.normalize";
    /// `user.settings.diagnoseProblems`
    pub(crate) const USER_SETTINGS_DIAGNOSEPROBLEMS: &str = "user.settings.diagnoseProblems";
    /// `user.settings.applyTopKey`
    pub(crate) const USER_SETTINGS_APPLYTOPKEY: &str = "user.settings.applyTopKey";
    /// `user.settings.writeAll`
    pub(crate) const USER_SETTINGS_WRITEALL: &str = "user.settings.writeAll";
    /// `user.settings.update`
    pub(crate) const USER_SETTINGS_UPDATE: &str = "user.settings.update";
    /// `user.settings.loadForEdit`
    pub(crate) const USER_SETTINGS_LOADFOREDIT: &str = "user.settings.loadForEdit";
    /// `user.settings.validate`
    pub(crate) const USER_SETTINGS_VALIDATE: &str = "user.settings.validate";
    /// `repo.settings.edit`
    pub(crate) const REPO_SETTINGS_EDIT: &str = "repo.settings.edit";
    /// `repo.settings.readRaw`
    pub(crate) const REPO_SETTINGS_READRAW: &str = "repo.settings.readRaw";
    /// `repo.settings.writeKey`
    pub(crate) const REPO_SETTINGS_WRITEKEY: &str = "repo.settings.writeKey";
    /// `repo.settings.writeAll`
    pub(crate) const REPO_SETTINGS_WRITEALL: &str = "repo.settings.writeAll";
    /// `featureFlags.resolveStartupGates`
    pub(crate) const FEATUREFLAGS_RESOLVESTARTUPGATES: &str = "featureFlags.resolveStartupGates";
    /// `featureFlags.listMetadata`
    pub(crate) const FEATUREFLAGS_LISTMETADATA: &str = "featureFlags.listMetadata";
    /// `featureFlags.resolve`
    pub(crate) const FEATUREFLAGS_RESOLVE: &str = "featureFlags.resolve";
    /// `permissions.locations.cleanupOrphaned`
    pub(crate) const PERMISSIONS_LOCATIONS_CLEANUPORPHANED: &str =
        "permissions.locations.cleanupOrphaned";
    /// `permissions.folderTrust.checkTrust`
    pub(crate) const PERMISSIONS_FOLDERTRUST_CHECKTRUST: &str =
        "permissions.folderTrust.checkTrust";
    /// `permissions.evaluateManagedPolicy`
    pub(crate) const PERMISSIONS_EVALUATEMANAGEDPOLICY: &str = "permissions.evaluateManagedPolicy";
    /// `managedSettings.readDevice`
    pub(crate) const MANAGEDSETTINGS_READDEVICE: &str = "managedSettings.readDevice";
    /// `managedSettings.retainPolicy`
    pub(crate) const MANAGEDSETTINGS_RETAINPOLICY: &str = "managedSettings.retainPolicy";
    /// `managedSettings.applyControls`
    pub(crate) const MANAGEDSETTINGS_APPLYCONTROLS: &str = "managedSettings.applyControls";
    /// `managedSettings.parseClientValue`
    pub(crate) const MANAGEDSETTINGS_PARSECLIENTVALUE: &str = "managedSettings.parseClientValue";
    /// `managedSettings.resolveEffectiveValue`
    pub(crate) const MANAGEDSETTINGS_RESOLVEEFFECTIVEVALUE: &str =
        "managedSettings.resolveEffectiveValue";
    /// `managedSettings.resolveBypassDisableTransition`
    pub(crate) const MANAGEDSETTINGS_RESOLVEBYPASSDISABLETRANSITION: &str =
        "managedSettings.resolveBypassDisableTransition";
    /// `managedSettings.ingest`
    pub(crate) const MANAGEDSETTINGS_INGEST: &str = "managedSettings.ingest";
    /// `managedSettings.resolveStartupPolicy`
    pub(crate) const MANAGEDSETTINGS_RESOLVESTARTUPPOLICY: &str =
        "managedSettings.resolveStartupPolicy";
    /// `managedSettings.loadRemoteControl`
    pub(crate) const MANAGEDSETTINGS_LOADREMOTECONTROL: &str = "managedSettings.loadRemoteControl";
    /// `managedSettings.resolveDeviceForceLoginOrgsGate`
    pub(crate) const MANAGEDSETTINGS_RESOLVEDEVICEFORCELOGINORGSGATE: &str =
        "managedSettings.resolveDeviceForceLoginOrgsGate";
    /// `managedSettings.read`
    pub const MANAGEDSETTINGS_READ: &str = "managedSettings.read";
    /// `managedSettings.clearCache`
    pub const MANAGEDSETTINGS_CLEARCACHE: &str = "managedSettings.clearCache";
    /// `managedSettings.resolve`
    pub const MANAGEDSETTINGS_RESOLVE: &str = "managedSettings.resolve";
    /// `managedSettings.permissions.evaluate`
    pub const MANAGEDSETTINGS_PERMISSIONS_EVALUATE: &str = "managedSettings.permissions.evaluate";
    /// `managedSettings.schema`
    pub const MANAGEDSETTINGS_SCHEMA: &str = "managedSettings.schema";
    /// `managedSettings.validate`
    pub const MANAGEDSETTINGS_VALIDATE: &str = "managedSettings.validate";
    /// `managedSettings.compose`
    pub const MANAGEDSETTINGS_COMPOSE: &str = "managedSettings.compose";
    /// `environments.list`
    pub const ENVIRONMENTS_LIST: &str = "environments.list";
    /// `environments.get`
    pub const ENVIRONMENTS_GET: &str = "environments.get";
    /// `environments.delete`
    pub const ENVIRONMENTS_DELETE: &str = "environments.delete";
    /// `host.publishSession`
    pub const HOST_PUBLISHSESSION: &str = "host.publishSession";
    /// `host.unpublishSession`
    pub const HOST_UNPUBLISHSESSION: &str = "host.unpublishSession";
    /// `host.listSessions`
    pub const HOST_LISTSESSIONS: &str = "host.listSessions";
    /// `host.createSession`
    pub(crate) const HOST_CREATESESSION: &str = "host.createSession";
    /// `host.releaseSession`
    pub(crate) const HOST_RELEASESESSION: &str = "host.releaseSession";
    /// `host.start`
    pub const HOST_START: &str = "host.start";
    /// `host.dispose`
    pub const HOST_DISPOSE: &str = "host.dispose";
    /// `host.setSessionCreationPolicy`
    pub const HOST_SETSESSIONCREATIONPOLICY: &str = "host.setSessionCreationPolicy";
    /// `host.getEnvironmentCredentials`
    pub(crate) const HOST_GETENVIRONMENTCREDENTIALS: &str = "host.getEnvironmentCredentials";
    /// `host.getConfiguration`
    pub(crate) const HOST_GETCONFIGURATION: &str = "host.getConfiguration";
    /// `host.getAuthenticationCredentials`
    pub(crate) const HOST_GETAUTHENTICATIONCREDENTIALS: &str = "host.getAuthenticationCredentials";
    /// `host.authenticate`
    pub(crate) const HOST_AUTHENTICATE: &str = "host.authenticate";
    /// `host.cancelAuthentication`
    pub(crate) const HOST_CANCELAUTHENTICATION: &str = "host.cancelAuthentication";
    /// `host.ready`
    pub(crate) const HOST_READY: &str = "host.ready";
    /// `ping`
    pub const PING: &str = "ping";
    /// `connect`
    pub(crate) const CONNECT: &str = "connect";
    /// `hooks.discover`
    pub const HOOKS_DISCOVER: &str = "hooks.discover";
    /// `models.list`
    pub const MODELS_LIST: &str = "models.list";
    /// `models.getBuiltInCatalog`
    pub const MODELS_GETBUILTINCATALOG: &str = "models.getBuiltInCatalog";
    /// `models.listTokenPriceUnits`
    pub const MODELS_LISTTOKENPRICEUNITS: &str = "models.listTokenPriceUnits";
    /// `azureMl.catalog`
    pub const AZUREML_CATALOG: &str = "azureMl.catalog";
    /// `azureMl.latest`
    pub const AZUREML_LATEST: &str = "azureMl.latest";
    /// `azureMl.resolve`
    pub const AZUREML_RESOLVE: &str = "azureMl.resolve";
    /// `azureMl.download`
    pub const AZUREML_DOWNLOAD: &str = "azureMl.download";
    /// `azureMl.acquireFamily`
    pub const AZUREML_ACQUIREFAMILY: &str = "azureMl.acquireFamily";
    /// `azureMl.releaseFamily`
    pub const AZUREML_RELEASEFAMILY: &str = "azureMl.releaseFamily";
    /// `azureMl.status`
    pub const AZUREML_STATUS: &str = "azureMl.status";
    /// `azureMl.cancel`
    pub const AZUREML_CANCEL: &str = "azureMl.cancel";
    /// `azureMl.discard`
    pub const AZUREML_DISCARD: &str = "azureMl.discard";
    /// `contentExclusion.validatePolicies`
    pub const CONTENTEXCLUSION_VALIDATEPOLICIES: &str = "contentExclusion.validatePolicies";
    /// `tools.list`
    pub const TOOLS_LIST: &str = "tools.list";
    /// `tools.largeOutput.cleanup`
    pub(crate) const TOOLS_LARGEOUTPUT_CLEANUP: &str = "tools.largeOutput.cleanup";
    /// `account.getQuota`
    pub const ACCOUNT_GETQUOTA: &str = "account.getQuota";
    /// `account.getCurrentAuth`
    pub const ACCOUNT_GETCURRENTAUTH: &str = "account.getCurrentAuth";
    /// `account.getAllUsers`
    pub const ACCOUNT_GETALLUSERS: &str = "account.getAllUsers";
    /// `account.login`
    pub const ACCOUNT_LOGIN: &str = "account.login";
    /// `account.logout`
    pub const ACCOUNT_LOGOUT: &str = "account.logout";
    /// `account.describeAuthInfo`
    pub(crate) const ACCOUNT_DESCRIBEAUTHINFO: &str = "account.describeAuthInfo";
    /// `account.getGistSupport`
    pub(crate) const ACCOUNT_GETGISTSUPPORT: &str = "account.getGistSupport";
    /// `account.getWebUrl`
    pub(crate) const ACCOUNT_GETWEBURL: &str = "account.getWebUrl";
    /// `account.getTokenEnvVar`
    pub(crate) const ACCOUNT_GETTOKENENVVAR: &str = "account.getTokenEnvVar";
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
    /// `mcp.registry.allocateRequestId`
    pub(crate) const MCP_REGISTRY_ALLOCATEREQUESTID: &str = "mcp.registry.allocateRequestId";
    /// `mcp.registry.search`
    pub(crate) const MCP_REGISTRY_SEARCH: &str = "mcp.registry.search";
    /// `mcp.registry.cancel`
    pub(crate) const MCP_REGISTRY_CANCEL: &str = "mcp.registry.cancel";
    /// `mcp.discover`
    pub const MCP_DISCOVER: &str = "mcp.discover";
    /// `mcp.listBuiltinServerNames`
    pub(crate) const MCP_LISTBUILTINSERVERNAMES: &str = "mcp.listBuiltinServerNames";
    /// `mcp.shouldExcludeGitHubTools`
    pub(crate) const MCP_SHOULDEXCLUDEGITHUBTOOLS: &str = "mcp.shouldExcludeGitHubTools";
    /// `mcp.planInstall`
    pub const MCP_PLANINSTALL: &str = "mcp.planInstall";
    /// `mcp.oauth.staticClientConfig`
    pub(crate) const MCP_OAUTH_STATICCLIENTCONFIG: &str = "mcp.oauth.staticClientConfig";
    /// `mcp.oauth.fetchProtectedResourceMetadata`
    pub(crate) const MCP_OAUTH_FETCHPROTECTEDRESOURCEMETADATA: &str =
        "mcp.oauth.fetchProtectedResourceMetadata";
    /// `mcp.oauth.checkDcrSupport`
    pub(crate) const MCP_OAUTH_CHECKDCRSUPPORT: &str = "mcp.oauth.checkDcrSupport";
    /// `mcp.oauth.isMicrosoftAuthorizationServer`
    pub(crate) const MCP_OAUTH_ISMICROSOFTAUTHORIZATIONSERVER: &str =
        "mcp.oauth.isMicrosoftAuthorizationServer";
    /// `mcp.prepareInstall`
    pub const MCP_PREPAREINSTALL: &str = "mcp.prepareInstall";
    /// `mcp.applyInstall`
    pub const MCP_APPLYINSTALL: &str = "mcp.applyInstall";
    /// `mcp.planUninstall`
    pub const MCP_PLANUNINSTALL: &str = "mcp.planUninstall";
    /// `mcp.applyUninstall`
    pub const MCP_APPLYUNINSTALL: &str = "mcp.applyUninstall";
    /// `mcp.installations.list`
    pub const MCP_INSTALLATIONS_LIST: &str = "mcp.installations.list";
    /// `mcp.installations.recover`
    pub const MCP_INSTALLATIONS_RECOVER: &str = "mcp.installations.recover";
    /// `mcp.installations.status`
    pub const MCP_INSTALLATIONS_STATUS: &str = "mcp.installations.status";
    /// `mcp.installations.cancel`
    pub const MCP_INSTALLATIONS_CANCEL: &str = "mcp.installations.cancel";
    /// `extensions.discover`
    pub const EXTENSIONS_DISCOVER: &str = "extensions.discover";
    /// `extensions.enable`
    pub const EXTENSIONS_ENABLE: &str = "extensions.enable";
    /// `extensions.disable`
    pub const EXTENSIONS_DISABLE: &str = "extensions.disable";
    /// `instructions.getInitPrompt`
    pub(crate) const INSTRUCTIONS_GETINITPROMPT: &str = "instructions.getInitPrompt";
    /// `instructions.discover`
    pub const INSTRUCTIONS_DISCOVER: &str = "instructions.discover";
    /// `instructions.discoverRepoSources`
    pub(crate) const INSTRUCTIONS_DISCOVERREPOSOURCES: &str = "instructions.discoverRepoSources";
    /// `instructions.getDiscoveryPaths`
    pub const INSTRUCTIONS_GETDISCOVERYPATHS: &str = "instructions.getDiscoveryPaths";
    /// `instructions.clearCache`
    pub(crate) const INSTRUCTIONS_CLEARCACHE: &str = "instructions.clearCache";
    /// `skills.planInstall`
    pub const SKILLS_PLANINSTALL: &str = "skills.planInstall";
    /// `skills.applyInstall`
    pub const SKILLS_APPLYINSTALL: &str = "skills.applyInstall";
    /// `skills.installations.list`
    pub const SKILLS_INSTALLATIONS_LIST: &str = "skills.installations.list";
    /// `skills.installations.recover`
    pub const SKILLS_INSTALLATIONS_RECOVER: &str = "skills.installations.recover";
    /// `skills.installations.status`
    pub const SKILLS_INSTALLATIONS_STATUS: &str = "skills.installations.status";
    /// `skills.installations.cancel`
    pub const SKILLS_INSTALLATIONS_CANCEL: &str = "skills.installations.cancel";
    /// `skills.installations.setEnabled`
    pub const SKILLS_INSTALLATIONS_SETENABLED: &str = "skills.installations.setEnabled";
    /// `skills.planUninstall`
    pub const SKILLS_PLANUNINSTALL: &str = "skills.planUninstall";
    /// `skills.applyUninstall`
    pub const SKILLS_APPLYUNINSTALL: &str = "skills.applyUninstall";
    /// `skills.config.setDisabledSkills`
    pub const SKILLS_CONFIG_SETDISABLEDSKILLS: &str = "skills.config.setDisabledSkills";
    /// `skills.config.setSkillDisabled`
    pub const SKILLS_CONFIG_SETSKILLDISABLED: &str = "skills.config.setSkillDisabled";
    /// `skills.discover`
    pub const SKILLS_DISCOVER: &str = "skills.discover";
    /// `skills.getDiscoveryPaths`
    pub const SKILLS_GETDISCOVERYPATHS: &str = "skills.getDiscoveryPaths";
    /// `plugins.planInstall`
    pub const PLUGINS_PLANINSTALL: &str = "plugins.planInstall";
    /// `plugins.applyInstall`
    pub const PLUGINS_APPLYINSTALL: &str = "plugins.applyInstall";
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
    /// `plugins.builtin.set`
    pub const PLUGINS_BUILTIN_SET: &str = "plugins.builtin.set";
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
    /// `registerExtensionLaunchProvider`
    pub const REGISTEREXTENSIONLAUNCHPROVIDER: &str = "registerExtensionLaunchProvider";
    /// `catalog.search`
    pub const CATALOG_SEARCH: &str = "catalog.search";
    /// `catalog.getSavedFeed`
    pub const CATALOG_GETSAVEDFEED: &str = "catalog.getSavedFeed";
    /// `catalog.getGeneratedFeed`
    pub const CATALOG_GETGENERATEDFEED: &str = "catalog.getGeneratedFeed";
    /// `catalog.select`
    pub const CATALOG_SELECT: &str = "catalog.select";
    /// `catalog.planInstallFromReference`
    pub const CATALOG_PLANINSTALLFROMREFERENCE: &str = "catalog.planInstallFromReference";
    /// `agents.discover`
    pub const AGENTS_DISCOVER: &str = "agents.discover";
    /// `agents.getDiscoveryPaths`
    pub const AGENTS_GETDISCOVERYPATHS: &str = "agents.getDiscoveryPaths";
    /// `agents.getBuiltins`
    pub(crate) const AGENTS_GETBUILTINS: &str = "agents.getBuiltins";
    /// `agents.getAvailableBuiltins`
    pub(crate) const AGENTS_GETAVAILABLEBUILTINS: &str = "agents.getAvailableBuiltins";
    /// `agents.getBuiltinDefinition`
    pub(crate) const AGENTS_GETBUILTINDEFINITION: &str = "agents.getBuiltinDefinition";
    /// `agents.getBuiltinListingDefinition`
    pub(crate) const AGENTS_GETBUILTINLISTINGDEFINITION: &str =
        "agents.getBuiltinListingDefinition";
    /// `agents.customAgentInitialModelDecision`
    pub(crate) const AGENTS_CUSTOMAGENTINITIALMODELDECISION: &str =
        "agents.customAgentInitialModelDecision";
    /// `globalState.load`
    pub(crate) const GLOBALSTATE_LOAD: &str = "globalState.load";
    /// `globalState.loadForConfigDir`
    pub(crate) const GLOBALSTATE_LOADFORCONFIGDIR: &str = "globalState.loadForConfigDir";
    /// `globalState.writeKey`
    pub(crate) const GLOBALSTATE_WRITEKEY: &str = "globalState.writeKey";
    /// `commands.list`
    pub const COMMANDS_LIST: &str = "commands.list";
    /// `settings.removeRawKey`
    pub(crate) const SETTINGS_REMOVERAWKEY: &str = "settings.removeRawKey";
    /// `settings.redactValueForDisplay`
    pub(crate) const SETTINGS_REDACTVALUEFORDISPLAY: &str = "settings.redactValueForDisplay";
    /// `gitHubRepository.atPath`
    pub(crate) const GITHUBREPOSITORY_ATPATH: &str = "gitHubRepository.atPath";
    /// `gitHubOwners.nextRequestId`
    pub(crate) const GITHUBOWNERS_NEXTREQUESTID: &str = "gitHubOwners.nextRequestId";
    /// `gitHubOwners.list`
    pub(crate) const GITHUBOWNERS_LIST: &str = "gitHubOwners.list";
    /// `gitHubOwners.cancel`
    pub(crate) const GITHUBOWNERS_CANCEL: &str = "gitHubOwners.cancel";
    /// `git.currentBranchRemote`
    pub(crate) const GIT_CURRENTBRANCHREMOTE: &str = "git.currentBranchRemote";
    /// `git.workingDirectoryContext`
    pub(crate) const GIT_WORKINGDIRECTORYCONTEXT: &str = "git.workingDirectoryContext";
    /// `git.reposFromRemotes`
    pub(crate) const GIT_REPOSFROMREMOTES: &str = "git.reposFromRemotes";
    /// `runtime.shutdown`
    pub const RUNTIME_SHUTDOWN: &str = "runtime.shutdown";
    /// `runtime.openBrowser`
    pub(crate) const RUNTIME_OPENBROWSER: &str = "runtime.openBrowser";
    /// `sessionFs.setProvider`
    pub const SESSIONFS_SETPROVIDER: &str = "sessionFs.setProvider";
    /// `llmInference.setProvider`
    pub const LLMINFERENCE_SETPROVIDER: &str = "llmInference.setProvider";
    /// `llmInference.httpResponseStart`
    pub const LLMINFERENCE_HTTPRESPONSESTART: &str = "llmInference.httpResponseStart";
    /// `llmInference.httpResponseChunk`
    pub const LLMINFERENCE_HTTPRESPONSECHUNK: &str = "llmInference.httpResponseChunk";
    /// `kerberosAuth.setProvider`
    pub const KERBEROSAUTH_SETPROVIDER: &str = "kerberosAuth.setProvider";
    /// `sessions.open`
    pub const SESSIONS_OPEN: &str = "sessions.open";
    /// `sessions.fork`
    pub const SESSIONS_FORK: &str = "sessions.fork";
    /// `sessions.connect`
    pub const SESSIONS_CONNECT: &str = "sessions.connect";
    /// `sessions.list`
    pub const SESSIONS_LIST: &str = "sessions.list";
    /// `sessions.getMetadata`
    pub(crate) const SESSIONS_GETMETADATA: &str = "sessions.getMetadata";
    /// `sessions.getClientMetadata`
    pub const SESSIONS_GETCLIENTMETADATA: &str = "sessions.getClientMetadata";
    /// `sessions.readPersistedEvents`
    pub const SESSIONS_READPERSISTEDEVENTS: &str = "sessions.readPersistedEvents";
    /// `sessions.listNonEmptySessionIds`
    pub(crate) const SESSIONS_LISTNONEMPTYSESSIONIDS: &str = "sessions.listNonEmptySessionIds";
    /// `sessions.findByTaskId`
    pub const SESSIONS_FINDBYTASKID: &str = "sessions.findByTaskId";
    /// `sessions.findByPrefix`
    pub const SESSIONS_FINDBYPREFIX: &str = "sessions.findByPrefix";
    /// `sessions.getLastForContext`
    pub const SESSIONS_GETLASTFORCONTEXT: &str = "sessions.getLastForContext";
    /// `sessions.getEventFilePath`
    pub(crate) const SESSIONS_GETEVENTFILEPATH: &str = "sessions.getEventFilePath";
    /// `sessions.getSizes`
    pub const SESSIONS_GETSIZES: &str = "sessions.getSizes";
    /// `sessions.checkInUse`
    pub const SESSIONS_CHECKINUSE: &str = "sessions.checkInUse";
    /// `sessions.getPersistedRemoteSteerable`
    pub(crate) const SESSIONS_GETPERSISTEDREMOTESTEERABLE: &str =
        "sessions.getPersistedRemoteSteerable";
    /// `sessions.close`
    pub const SESSIONS_CLOSE: &str = "sessions.close";
    /// `sessions.bulkDelete`
    pub const SESSIONS_BULKDELETE: &str = "sessions.bulkDelete";
    /// `sessions.delete`
    pub(crate) const SESSIONS_DELETE: &str = "sessions.delete";
    /// `sessions.pruneOld`
    pub const SESSIONS_PRUNEOLD: &str = "sessions.pruneOld";
    /// `sessions.save`
    pub const SESSIONS_SAVE: &str = "sessions.save";
    /// `sessions.releaseLock`
    pub const SESSIONS_RELEASELOCK: &str = "sessions.releaseLock";
    /// `sessions.enrichMetadata`
    pub const SESSIONS_ENRICHMETADATA: &str = "sessions.enrichMetadata";
    /// `sessions.createWorkspace`
    pub(crate) const SESSIONS_CREATEWORKSPACE: &str = "sessions.createWorkspace";
    /// `sessions.loadWorkspace`
    pub(crate) const SESSIONS_LOADWORKSPACE: &str = "sessions.loadWorkspace";
    /// `sessions.updateWorkspaceFields`
    pub(crate) const SESSIONS_UPDATEWORKSPACEFIELDS: &str = "sessions.updateWorkspaceFields";
    /// `sessions.reloadPluginHooks`
    pub const SESSIONS_RELOADPLUGINHOOKS: &str = "sessions.reloadPluginHooks";
    /// `sessions.loadDeferredRepoHooks`
    pub const SESSIONS_LOADDEFERREDREPOHOOKS: &str = "sessions.loadDeferredRepoHooks";
    /// `sessions.setAdditionalPlugins`
    pub const SESSIONS_SETADDITIONALPLUGINS: &str = "sessions.setAdditionalPlugins";
    /// `sessions.getBoardEntryCount`
    pub(crate) const SESSIONS_GETBOARDENTRYCOUNT: &str = "sessions.getBoardEntryCount";
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
    /// `sessions.configureSessionExtensions`
    pub(crate) const SESSIONS_CONFIGURESESSIONEXTENSIONS: &str =
        "sessions.configureSessionExtensions";
    /// `agentRegistry.spawn`
    pub const AGENTREGISTRY_SPAWN: &str = "agentRegistry.spawn";
    /// `agentRegistry.ensureDir`
    pub(crate) const AGENTREGISTRY_ENSUREDIR: &str = "agentRegistry.ensureDir";
    /// `agentRegistry.removeEntry`
    pub(crate) const AGENTREGISTRY_REMOVEENTRY: &str = "agentRegistry.removeEntry";
    /// `agentRegistry.listEntries`
    pub(crate) const AGENTREGISTRY_LISTENTRIES: &str = "agentRegistry.listEntries";
    /// `connectors.getCapabilities`
    pub const CONNECTORS_GETCAPABILITIES: &str = "connectors.getCapabilities";
    /// `connectors.getAccounts`
    pub const CONNECTORS_GETACCOUNTS: &str = "connectors.getAccounts";
    /// `connectors.list`
    pub const CONNECTORS_LIST: &str = "connectors.list";
    /// `connectors.refresh`
    pub const CONNECTORS_REFRESH: &str = "connectors.refresh";
    /// `session.providers.getCatalog`
    pub const SESSION_PROVIDERS_GETCATALOG: &str = "session.providers.getCatalog";
    /// `session.providers.discover`
    pub const SESSION_PROVIDERS_DISCOVER: &str = "session.providers.discover";
    /// `session.providers.getStatus`
    pub const SESSION_PROVIDERS_GETSTATUS: &str = "session.providers.getStatus";
    /// `session.providers.models.list`
    pub const SESSION_PROVIDERS_MODELS_LIST: &str = "session.providers.models.list";
    /// `session.providers.models.prepareConfiguration`
    pub const SESSION_PROVIDERS_MODELS_PREPARECONFIGURATION: &str =
        "session.providers.models.prepareConfiguration";
    /// `session.suspend`
    pub const SESSION_SUSPEND: &str = "session.suspend";
    /// `session.send`
    pub const SESSION_SEND: &str = "session.send";
    /// `session.sendAndWait`
    pub(crate) const SESSION_SENDANDWAIT: &str = "session.sendAndWait";
    /// `session.sendMessages`
    pub const SESSION_SENDMESSAGES: &str = "session.sendMessages";
    /// `session.sandbox.getEnforcementStatus`
    pub const SESSION_SANDBOX_GETENFORCEMENTSTATUS: &str = "session.sandbox.getEnforcementStatus";
    /// `session.sandbox.getCredentialSuggestions`
    pub const SESSION_SANDBOX_GETCREDENTIALSUGGESTIONS: &str =
        "session.sandbox.getCredentialSuggestions";
    /// `session.sandbox.disableForSession`
    pub const SESSION_SANDBOX_DISABLEFORSESSION: &str = "session.sandbox.disableForSession";
    /// `session.sandbox.grantPathForRequest`
    pub const SESSION_SANDBOX_GRANTPATHFORREQUEST: &str = "session.sandbox.grantPathForRequest";
    /// `session.sendSystemNotification`
    pub(crate) const SESSION_SENDSYSTEMNOTIFICATION: &str = "session.sendSystemNotification";
    /// `session.abort`
    pub const SESSION_ABORT: &str = "session.abort";
    /// `session.interruptMainTurn`
    pub const SESSION_INTERRUPTMAINTURN: &str = "session.interruptMainTurn";
    /// `session.cancelAllBackgroundAgents`
    pub const SESSION_CANCELALLBACKGROUNDAGENTS: &str = "session.cancelAllBackgroundAgents";
    /// `session.shutdown`
    pub const SESSION_SHUTDOWN: &str = "session.shutdown";
    /// `session.gitHubAuth.getStatus`
    pub const SESSION_GITHUBAUTH_GETSTATUS: &str = "session.gitHubAuth.getStatus";
    /// `session.gitHubAuth.setCredentials`
    pub const SESSION_GITHUBAUTH_SETCREDENTIALS: &str = "session.gitHubAuth.setCredentials";
    /// `session.gitHubAuth.getCurrentAuthInfo`
    pub(crate) const SESSION_GITHUBAUTH_GETCURRENTAUTHINFO: &str =
        "session.gitHubAuth.getCurrentAuthInfo";
    /// `session.gitHubAuth.getAllAuthAvailable`
    pub(crate) const SESSION_GITHUBAUTH_GETALLAUTHAVAILABLE: &str =
        "session.gitHubAuth.getAllAuthAvailable";
    /// `session.gitHubAuth.refreshCopilotUser`
    pub(crate) const SESSION_GITHUBAUTH_REFRESHCOPILOTUSER: &str =
        "session.gitHubAuth.refreshCopilotUser";
    /// `session.gitHubAuth.login`
    pub(crate) const SESSION_GITHUBAUTH_LOGIN: &str = "session.gitHubAuth.login";
    /// `session.gitHubAuth.switchToAuth`
    pub(crate) const SESSION_GITHUBAUTH_SWITCHTOAUTH: &str = "session.gitHubAuth.switchToAuth";
    /// `session.gitHubAuth.logout`
    pub(crate) const SESSION_GITHUBAUTH_LOGOUT: &str = "session.gitHubAuth.logout";
    /// `session.gitHubAuth.logoutUser`
    pub(crate) const SESSION_GITHUBAUTH_LOGOUTUSER: &str = "session.gitHubAuth.logoutUser";
    /// `session.gitHubAuth.lastAuthErrors`
    pub(crate) const SESSION_GITHUBAUTH_LASTAUTHERRORS: &str = "session.gitHubAuth.lastAuthErrors";
    /// `session.accounts.enumerate`
    pub const SESSION_ACCOUNTS_ENUMERATE: &str = "session.accounts.enumerate";
    /// `session.accounts.get`
    pub const SESSION_ACCOUNTS_GET: &str = "session.accounts.get";
    /// `session.accounts.set`
    pub const SESSION_ACCOUNTS_SET: &str = "session.accounts.set";
    /// `session.accounts.login.begin`
    pub const SESSION_ACCOUNTS_LOGIN_BEGIN: &str = "session.accounts.login.begin";
    /// `session.accounts.login.advance`
    pub const SESSION_ACCOUNTS_LOGIN_ADVANCE: &str = "session.accounts.login.advance";
    /// `session.accounts.login.cancel`
    pub const SESSION_ACCOUNTS_LOGIN_CANCEL: &str = "session.accounts.login.cancel";
    /// `session.debug.collectLogs`
    pub const SESSION_DEBUG_COLLECTLOGS: &str = "session.debug.collectLogs";
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
    /// `session.canvas.provider.register`
    pub(crate) const SESSION_CANVAS_PROVIDER_REGISTER: &str = "session.canvas.provider.register";
    /// `session.canvas.provider.unregister`
    pub(crate) const SESSION_CANVAS_PROVIDER_UNREGISTER: &str =
        "session.canvas.provider.unregister";
    /// `session.workflow.run`
    pub const SESSION_WORKFLOW_RUN: &str = "session.workflow.run";
    /// `session.workflow.resume`
    pub const SESSION_WORKFLOW_RESUME: &str = "session.workflow.resume";
    /// `session.workflow.runFromTool`
    pub(crate) const SESSION_WORKFLOW_RUNFROMTOOL: &str = "session.workflow.runFromTool";
    /// `session.workflow.resumeFromTool`
    pub(crate) const SESSION_WORKFLOW_RESUMEFROMTOOL: &str = "session.workflow.resumeFromTool";
    /// `session.workflow.getRun`
    pub const SESSION_WORKFLOW_GETRUN: &str = "session.workflow.getRun";
    /// `session.workflow.listRuns`
    pub const SESSION_WORKFLOW_LISTRUNS: &str = "session.workflow.listRuns";
    /// `session.workflow.getRunDetail`
    pub const SESSION_WORKFLOW_GETRUNDETAIL: &str = "session.workflow.getRunDetail";
    /// `session.workflow.getRunProgress`
    pub const SESSION_WORKFLOW_GETRUNPROGRESS: &str = "session.workflow.getRunProgress";
    /// `session.workflow.cancel`
    pub const SESSION_WORKFLOW_CANCEL: &str = "session.workflow.cancel";
    /// `session.workflow.pause`
    pub const SESSION_WORKFLOW_PAUSE: &str = "session.workflow.pause";
    /// `session.workflow.pauseAtCheckpoint`
    pub(crate) const SESSION_WORKFLOW_PAUSEATCHECKPOINT: &str =
        "session.workflow.pauseAtCheckpoint";
    /// `session.workflow.log`
    pub const SESSION_WORKFLOW_LOG: &str = "session.workflow.log";
    /// `session.workflow.agent`
    pub const SESSION_WORKFLOW_AGENT: &str = "session.workflow.agent";
    /// `session.workflow.journal.get`
    pub const SESSION_WORKFLOW_JOURNAL_GET: &str = "session.workflow.journal.get";
    /// `session.workflow.journal.put`
    pub const SESSION_WORKFLOW_JOURNAL_PUT: &str = "session.workflow.journal.put";
    /// `session.model.getCurrent`
    pub const SESSION_MODEL_GETCURRENT: &str = "session.model.getCurrent";
    /// `session.model.switchTo`
    pub const SESSION_MODEL_SWITCHTO: &str = "session.model.switchTo";
    /// `session.model.switchAutoTier`
    pub const SESSION_MODEL_SWITCHAUTOTIER: &str = "session.model.switchAutoTier";
    /// `session.model.applyStartupOverlay`
    pub(crate) const SESSION_MODEL_APPLYSTARTUPOVERLAY: &str = "session.model.applyStartupOverlay";
    /// `session.model.clearStartupSeed`
    pub(crate) const SESSION_MODEL_CLEARSTARTUPSEED: &str = "session.model.clearStartupSeed";
    /// `session.model.setAllowedModels`
    pub const SESSION_MODEL_SETALLOWEDMODELS: &str = "session.model.setAllowedModels";
    /// `session.model.setReasoningEffort`
    pub const SESSION_MODEL_SETREASONINGEFFORT: &str = "session.model.setReasoningEffort";
    /// `session.model.getReasoningSummaries`
    pub const SESSION_MODEL_GETREASONINGSUMMARIES: &str = "session.model.getReasoningSummaries";
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
    /// `session.workspaces.updateMetadata`
    pub const SESSION_WORKSPACES_UPDATEMETADATA: &str = "session.workspaces.updateMetadata";
    /// `session.workspaces.ensure`
    pub const SESSION_WORKSPACES_ENSURE: &str = "session.workspaces.ensure";
    /// `session.workspaces.listFiles`
    pub const SESSION_WORKSPACES_LISTFILES: &str = "session.workspaces.listFiles";
    /// `session.workspaces.readFile`
    pub const SESSION_WORKSPACES_READFILE: &str = "session.workspaces.readFile";
    /// `session.workspaces.createFile`
    pub const SESSION_WORKSPACES_CREATEFILE: &str = "session.workspaces.createFile";
    /// `session.workspaces.statFile`
    pub const SESSION_WORKSPACES_STATFILE: &str = "session.workspaces.statFile";
    /// `session.workspaces.createDirectory`
    pub const SESSION_WORKSPACES_CREATEDIRECTORY: &str = "session.workspaces.createDirectory";
    /// `session.workspaces.removePath`
    pub const SESSION_WORKSPACES_REMOVEPATH: &str = "session.workspaces.removePath";
    /// `session.workspaces.renamePath`
    pub const SESSION_WORKSPACES_RENAMEPATH: &str = "session.workspaces.renamePath";
    /// `session.workspaces.listCheckpoints`
    pub const SESSION_WORKSPACES_LISTCHECKPOINTS: &str = "session.workspaces.listCheckpoints";
    /// `session.workspaces.readCheckpoint`
    pub const SESSION_WORKSPACES_READCHECKPOINT: &str = "session.workspaces.readCheckpoint";
    /// `session.workspaces.addSummary`
    pub const SESSION_WORKSPACES_ADDSUMMARY: &str = "session.workspaces.addSummary";
    /// `session.workspaces.truncateSummaries`
    pub const SESSION_WORKSPACES_TRUNCATESUMMARIES: &str = "session.workspaces.truncateSummaries";
    /// `session.workspaces.readAutopilotObjective`
    pub const SESSION_WORKSPACES_READAUTOPILOTOBJECTIVE: &str =
        "session.workspaces.readAutopilotObjective";
    /// `session.workspaces.writeAutopilotObjective`
    pub const SESSION_WORKSPACES_WRITEAUTOPILOTOBJECTIVE: &str =
        "session.workspaces.writeAutopilotObjective";
    /// `session.workspaces.deleteAutopilotObjective`
    pub const SESSION_WORKSPACES_DELETEAUTOPILOTOBJECTIVE: &str =
        "session.workspaces.deleteAutopilotObjective";
    /// `session.workspaces.autopilotObjectiveExists`
    pub const SESSION_WORKSPACES_AUTOPILOTOBJECTIVEEXISTS: &str =
        "session.workspaces.autopilotObjectiveExists";
    /// `session.workspaces.saveLargePaste`
    pub const SESSION_WORKSPACES_SAVELARGEPASTE: &str = "session.workspaces.saveLargePaste";
    /// `session.workspaces.diff`
    pub const SESSION_WORKSPACES_DIFF: &str = "session.workspaces.diff";
    /// `session.autopilotObjective.getState`
    pub const SESSION_AUTOPILOTOBJECTIVE_GETSTATE: &str = "session.autopilotObjective.getState";
    /// `session.autopilot.evaluateIdleContinuation`
    pub(crate) const SESSION_AUTOPILOT_EVALUATEIDLECONTINUATION: &str =
        "session.autopilot.evaluateIdleContinuation";
    /// `session.completions.getTriggerCharacters`
    pub const SESSION_COMPLETIONS_GETTRIGGERCHARACTERS: &str =
        "session.completions.getTriggerCharacters";
    /// `session.completions.request`
    pub const SESSION_COMPLETIONS_REQUEST: &str = "session.completions.request";
    /// `session.instructions.getSources`
    pub const SESSION_INSTRUCTIONS_GETSOURCES: &str = "session.instructions.getSources";
    /// `session.instructions.reload`
    pub const SESSION_INSTRUCTIONS_RELOAD: &str = "session.instructions.reload";
    /// `session.customizations.reload`
    pub const SESSION_CUSTOMIZATIONS_RELOAD: &str = "session.customizations.reload";
    /// `session.fleet.start`
    pub const SESSION_FLEET_START: &str = "session.fleet.start";
    /// `session.agent.list`
    pub const SESSION_AGENT_LIST: &str = "session.agent.list";
    /// `session.agent.setPrompt`
    pub const SESSION_AGENT_SETPROMPT: &str = "session.agent.setPrompt";
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
    /// `session.tasks.register`
    pub const SESSION_TASKS_REGISTER: &str = "session.tasks.register";
    /// `session.tasks.update`
    pub const SESSION_TASKS_UPDATE: &str = "session.tasks.update";
    /// `session.tasks.refresh`
    pub const SESSION_TASKS_REFRESH: &str = "session.tasks.refresh";
    /// `session.tasks.waitForPending`
    pub const SESSION_TASKS_WAITFORPENDING: &str = "session.tasks.waitForPending";
    /// `session.tasks.getProgress`
    pub const SESSION_TASKS_GETPROGRESS: &str = "session.tasks.getProgress";
    /// `session.tasks.readOutput`
    pub const SESSION_TASKS_READOUTPUT: &str = "session.tasks.readOutput";
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
    /// `session.mcp.setConnectedIdeInfo`
    pub(crate) const SESSION_MCP_SETCONNECTEDIDEINFO: &str = "session.mcp.setConnectedIdeInfo";
    /// `session.mcp.list`
    pub const SESSION_MCP_LIST: &str = "session.mcp.list";
    /// `session.mcp.listConfigured`
    pub const SESSION_MCP_LISTCONFIGURED: &str = "session.mcp.listConfigured";
    /// `session.mcp.listTools`
    pub const SESSION_MCP_LISTTOOLS: &str = "session.mcp.listTools";
    /// `session.mcp.enable`
    pub const SESSION_MCP_ENABLE: &str = "session.mcp.enable";
    /// `session.mcp.disable`
    pub const SESSION_MCP_DISABLE: &str = "session.mcp.disable";
    /// `session.mcp.reload`
    pub const SESSION_MCP_RELOAD: &str = "session.mcp.reload";
    /// `session.mcp.moveLoadingToBackground`
    pub const SESSION_MCP_MOVELOADINGTOBACKGROUND: &str = "session.mcp.moveLoadingToBackground";
    /// `session.mcp.reloadWithConfig`
    pub(crate) const SESSION_MCP_RELOADWITHCONFIG: &str = "session.mcp.reloadWithConfig";
    /// `session.mcp.executeSampling`
    pub const SESSION_MCP_EXECUTESAMPLING: &str = "session.mcp.executeSampling";
    /// `session.mcp.cancelSamplingExecution`
    pub const SESSION_MCP_CANCELSAMPLINGEXECUTION: &str = "session.mcp.cancelSamplingExecution";
    /// `session.mcp.setEnvValueMode`
    pub const SESSION_MCP_SETENVVALUEMODE: &str = "session.mcp.setEnvValueMode";
    /// `session.mcp.removeGitHub`
    pub const SESSION_MCP_REMOVEGITHUB: &str = "session.mcp.removeGitHub";
    /// `session.mcp.configureGitHub`
    pub(crate) const SESSION_MCP_CONFIGUREGITHUB: &str = "session.mcp.configureGitHub";
    /// `session.mcp.startServer`
    pub const SESSION_MCP_STARTSERVER: &str = "session.mcp.startServer";
    /// `session.mcp.restartServer`
    pub const SESSION_MCP_RESTARTSERVER: &str = "session.mcp.restartServer";
    /// `session.mcp.stopServer`
    pub const SESSION_MCP_STOPSERVER: &str = "session.mcp.stopServer";
    /// `session.mcp.registerExternalClient`
    pub(crate) const SESSION_MCP_REGISTEREXTERNALCLIENT: &str =
        "session.mcp.registerExternalClient";
    /// `session.mcp.unregisterExternalClient`
    pub(crate) const SESSION_MCP_UNREGISTEREXTERNALCLIENT: &str =
        "session.mcp.unregisterExternalClient";
    /// `session.mcp.isServerRunning`
    pub const SESSION_MCP_ISSERVERRUNNING: &str = "session.mcp.isServerRunning";
    /// `session.mcp.oauth.handlePendingRequest`
    pub const SESSION_MCP_OAUTH_HANDLEPENDINGREQUEST: &str =
        "session.mcp.oauth.handlePendingRequest";
    /// `session.mcp.oauth.authenticationStateChanged`
    pub const SESSION_MCP_OAUTH_AUTHENTICATIONSTATECHANGED: &str =
        "session.mcp.oauth.authenticationStateChanged";
    /// `session.mcp.oauth.prepareLogin`
    pub const SESSION_MCP_OAUTH_PREPARELOGIN: &str = "session.mcp.oauth.prepareLogin";
    /// `session.mcp.oauth.login`
    pub const SESSION_MCP_OAUTH_LOGIN: &str = "session.mcp.oauth.login";
    /// `session.mcp.oauth.complete`
    pub const SESSION_MCP_OAUTH_COMPLETE: &str = "session.mcp.oauth.complete";
    /// `session.mcp.oauth.probe`
    pub const SESSION_MCP_OAUTH_PROBE: &str = "session.mcp.oauth.probe";
    /// `session.mcp.oauth.cancelLogin`
    pub const SESSION_MCP_OAUTH_CANCELLOGIN: &str = "session.mcp.oauth.cancelLogin";
    /// `session.mcp.oauth.respond`
    pub const SESSION_MCP_OAUTH_RESPOND: &str = "session.mcp.oauth.respond";
    /// `session.mcp.headers.handlePendingHeadersRefreshRequest`
    pub const SESSION_MCP_HEADERS_HANDLEPENDINGHEADERSREFRESHREQUEST: &str =
        "session.mcp.headers.handlePendingHeadersRefreshRequest";
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
    /// `session.mcp.resources.read`
    pub const SESSION_MCP_RESOURCES_READ: &str = "session.mcp.resources.read";
    /// `session.mcp.resources.list`
    pub const SESSION_MCP_RESOURCES_LIST: &str = "session.mcp.resources.list";
    /// `session.mcp.resources.listTemplates`
    pub const SESSION_MCP_RESOURCES_LISTTEMPLATES: &str = "session.mcp.resources.listTemplates";
    /// `session.mcp.prompts.list`
    pub const SESSION_MCP_PROMPTS_LIST: &str = "session.mcp.prompts.list";
    /// `session.mcp.prompts.get`
    pub const SESSION_MCP_PROMPTS_GET: &str = "session.mcp.prompts.get";
    /// `session.diagnostics.configure`
    pub const SESSION_DIAGNOSTICS_CONFIGURE: &str = "session.diagnostics.configure";
    /// `session.diagnostics.read`
    pub const SESSION_DIAGNOSTICS_READ: &str = "session.diagnostics.read";
    /// `session.diagnostics.lifecycle.read`
    pub(crate) const SESSION_DIAGNOSTICS_LIFECYCLE_READ: &str =
        "session.diagnostics.lifecycle.read";
    /// `session.connectors.getCapabilities`
    pub const SESSION_CONNECTORS_GETCAPABILITIES: &str = "session.connectors.getCapabilities";
    /// `session.connectors.getAccount`
    pub const SESSION_CONNECTORS_GETACCOUNT: &str = "session.connectors.getAccount";
    /// `session.connectors.getStatus`
    pub const SESSION_CONNECTORS_GETSTATUS: &str = "session.connectors.getStatus";
    /// `session.connectors.list`
    pub const SESSION_CONNECTORS_LIST: &str = "session.connectors.list";
    /// `session.connectors.refresh`
    pub const SESSION_CONNECTORS_REFRESH: &str = "session.connectors.refresh";
    /// `session.connectors.connect`
    pub const SESSION_CONNECTORS_CONNECT: &str = "session.connectors.connect";
    /// `session.connectors.reconnect`
    pub const SESSION_CONNECTORS_RECONNECT: &str = "session.connectors.reconnect";
    /// `session.connectors.continueConnection`
    pub const SESSION_CONNECTORS_CONTINUECONNECTION: &str = "session.connectors.continueConnection";
    /// `session.connectors.disconnect`
    pub const SESSION_CONNECTORS_DISCONNECT: &str = "session.connectors.disconnect";
    /// `session.connectors.reconcile`
    pub const SESSION_CONNECTORS_RECONCILE: &str = "session.connectors.reconcile";
    /// `session.connectors.reconcileForStartup`
    pub(crate) const SESSION_CONNECTORS_RECONCILEFORSTARTUP: &str =
        "session.connectors.reconcileForStartup";
    /// `session.connectors.withdrawProjection`
    pub(crate) const SESSION_CONNECTORS_WITHDRAWPROJECTION: &str =
        "session.connectors.withdrawProjection";
    /// `session.managedSettings.get`
    pub const SESSION_MANAGEDSETTINGS_GET: &str = "session.managedSettings.get";
    /// `session.managedSettings.recheckPolicyHelper`
    pub const SESSION_MANAGEDSETTINGS_RECHECKPOLICYHELPER: &str =
        "session.managedSettings.recheckPolicyHelper";
    /// `session.plugins.list`
    pub const SESSION_PLUGINS_LIST: &str = "session.plugins.list";
    /// `session.plugins.install`
    pub const SESSION_PLUGINS_INSTALL: &str = "session.plugins.install";
    /// `session.plugins.uninstall`
    pub const SESSION_PLUGINS_UNINSTALL: &str = "session.plugins.uninstall";
    /// `session.plugins.update`
    pub const SESSION_PLUGINS_UPDATE: &str = "session.plugins.update";
    /// `session.plugins.enable`
    pub const SESSION_PLUGINS_ENABLE: &str = "session.plugins.enable";
    /// `session.plugins.disable`
    pub const SESSION_PLUGINS_DISABLE: &str = "session.plugins.disable";
    /// `session.plugins.marketplaces.list`
    pub const SESSION_PLUGINS_MARKETPLACES_LIST: &str = "session.plugins.marketplaces.list";
    /// `session.plugins.marketplaces.add`
    pub const SESSION_PLUGINS_MARKETPLACES_ADD: &str = "session.plugins.marketplaces.add";
    /// `session.plugins.marketplaces.remove`
    pub const SESSION_PLUGINS_MARKETPLACES_REMOVE: &str = "session.plugins.marketplaces.remove";
    /// `session.plugins.marketplaces.browse`
    pub const SESSION_PLUGINS_MARKETPLACES_BROWSE: &str = "session.plugins.marketplaces.browse";
    /// `session.plugins.marketplaces.refresh`
    pub const SESSION_PLUGINS_MARKETPLACES_REFRESH: &str = "session.plugins.marketplaces.refresh";
    /// `session.plugins.reload`
    pub const SESSION_PLUGINS_RELOAD: &str = "session.plugins.reload";
    /// `session.plugins.retryManaged`
    pub const SESSION_PLUGINS_RETRYMANAGED: &str = "session.plugins.retryManaged";
    /// `session.provider.getEndpoint`
    pub const SESSION_PROVIDER_GETENDPOINT: &str = "session.provider.getEndpoint";
    /// `session.provider.add`
    pub const SESSION_PROVIDER_ADD: &str = "session.provider.add";
    /// `session.provider.sync`
    pub const SESSION_PROVIDER_SYNC: &str = "session.provider.sync";
    /// `session.provider.withdraw`
    pub const SESSION_PROVIDER_WITHDRAW: &str = "session.provider.withdraw";
    /// `session.options.update`
    pub const SESSION_OPTIONS_UPDATE: &str = "session.options.update";
    /// `session.lsp.initialize`
    pub const SESSION_LSP_INITIALIZE: &str = "session.lsp.initialize";
    /// `session.extensions.getPolicy`
    pub const SESSION_EXTENSIONS_GETPOLICY: &str = "session.extensions.getPolicy";
    /// `session.extensions.list`
    pub const SESSION_EXTENSIONS_LIST: &str = "session.extensions.list";
    /// `session.extensions.enable`
    pub const SESSION_EXTENSIONS_ENABLE: &str = "session.extensions.enable";
    /// `session.extensions.disable`
    pub const SESSION_EXTENSIONS_DISABLE: &str = "session.extensions.disable";
    /// `session.extensions.reload`
    pub const SESSION_EXTENSIONS_RELOAD: &str = "session.extensions.reload";
    /// `session.extensions.reconcile`
    pub const SESSION_EXTENSIONS_RECONCILE: &str = "session.extensions.reconcile";
    /// `session.extensions.sendAttachmentsToMessage`
    pub const SESSION_EXTENSIONS_SENDATTACHMENTSTOMESSAGE: &str =
        "session.extensions.sendAttachmentsToMessage";
    /// `session.tools.execute`
    pub const SESSION_TOOLS_EXECUTE: &str = "session.tools.execute";
    /// `session.tools.getBuiltinDescriptors`
    pub const SESSION_TOOLS_GETBUILTINDESCRIPTORS: &str = "session.tools.getBuiltinDescriptors";
    /// `session.tools.taskCompleteEventData`
    pub const SESSION_TOOLS_TASKCOMPLETEEVENTDATA: &str = "session.tools.taskCompleteEventData";
    /// `session.tools.handlePendingToolCall`
    pub const SESSION_TOOLS_HANDLEPENDINGTOOLCALL: &str = "session.tools.handlePendingToolCall";
    /// `session.tools.listPendingRequests`
    pub const SESSION_TOOLS_LISTPENDINGREQUESTS: &str = "session.tools.listPendingRequests";
    /// `session.tools.initializeAndValidate`
    pub const SESSION_TOOLS_INITIALIZEANDVALIDATE: &str = "session.tools.initializeAndValidate";
    /// `session.tools.getCurrentMetadata`
    pub const SESSION_TOOLS_GETCURRENTMETADATA: &str = "session.tools.getCurrentMetadata";
    /// `session.tools.set`
    pub const SESSION_TOOLS_SET: &str = "session.tools.set";
    /// `session.tools.getSubagentSettings`
    pub const SESSION_TOOLS_GETSUBAGENTSETTINGS: &str = "session.tools.getSubagentSettings";
    /// `session.tools.updateSubagentSettings`
    pub const SESSION_TOOLS_UPDATESUBAGENTSETTINGS: &str = "session.tools.updateSubagentSettings";
    /// `session.commands.list`
    pub const SESSION_COMMANDS_LIST: &str = "session.commands.list";
    /// `session.commands.invoke`
    pub const SESSION_COMMANDS_INVOKE: &str = "session.commands.invoke";
    /// `session.commands.finalizeInvocationEffect`
    pub(crate) const SESSION_COMMANDS_FINALIZEINVOCATIONEFFECT: &str =
        "session.commands.finalizeInvocationEffect";
    /// `session.commands.handlePendingCommand`
    pub const SESSION_COMMANDS_HANDLEPENDINGCOMMAND: &str = "session.commands.handlePendingCommand";
    /// `session.commands.execute`
    pub const SESSION_COMMANDS_EXECUTE: &str = "session.commands.execute";
    /// `session.commands.enqueue`
    pub const SESSION_COMMANDS_ENQUEUE: &str = "session.commands.enqueue";
    /// `session.commands.respondToQueuedCommand`
    pub const SESSION_COMMANDS_RESPONDTOQUEUEDCOMMAND: &str =
        "session.commands.respondToQueuedCommand";
    /// `session.commands.reportQueuedCommandWait`
    pub(crate) const SESSION_COMMANDS_REPORTQUEUEDCOMMANDWAIT: &str =
        "session.commands.reportQueuedCommandWait";
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
    /// `session.ui.handleHumanAskUser`
    pub(crate) const SESSION_UI_HANDLEHUMANASKUSER: &str = "session.ui.handleHumanAskUser";
    /// `session.ui.handlePendingUserInput`
    pub const SESSION_UI_HANDLEPENDINGUSERINPUT: &str = "session.ui.handlePendingUserInput";
    /// `session.ui.handleHumanUserInput`
    pub(crate) const SESSION_UI_HANDLEHUMANUSERINPUT: &str = "session.ui.handleHumanUserInput";
    /// `session.ui.handlePendingSampling`
    pub const SESSION_UI_HANDLEPENDINGSAMPLING: &str = "session.ui.handlePendingSampling";
    /// `session.ui.handlePendingAutoModeSwitch`
    pub const SESSION_UI_HANDLEPENDINGAUTOMODESWITCH: &str =
        "session.ui.handlePendingAutoModeSwitch";
    /// `session.ui.handlePendingSessionLimitsExhausted`
    pub const SESSION_UI_HANDLEPENDINGSESSIONLIMITSEXHAUSTED: &str =
        "session.ui.handlePendingSessionLimitsExhausted";
    /// `session.ui.handlePendingExitPlanMode`
    pub const SESSION_UI_HANDLEPENDINGEXITPLANMODE: &str = "session.ui.handlePendingExitPlanMode";
    /// `session.ui.handleHumanExitPlanMode`
    pub(crate) const SESSION_UI_HANDLEHUMANEXITPLANMODE: &str =
        "session.ui.handleHumanExitPlanMode";
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
    /// `session.permissions.setMode`
    pub const SESSION_PERMISSIONS_SETMODE: &str = "session.permissions.setMode";
    /// `session.permissions.getMode`
    pub const SESSION_PERMISSIONS_GETMODE: &str = "session.permissions.getMode";
    /// `session.permissions.modifyRules`
    pub const SESSION_PERMISSIONS_MODIFYRULES: &str = "session.permissions.modifyRules";
    /// `session.permissions.setRequired`
    pub const SESSION_PERMISSIONS_SETREQUIRED: &str = "session.permissions.setRequired";
    /// `session.permissions.resetSessionApprovals`
    pub const SESSION_PERMISSIONS_RESETSESSIONAPPROVALS: &str =
        "session.permissions.resetSessionApprovals";
    /// `session.permissions.clearResumeEscalation`
    pub const SESSION_PERMISSIONS_CLEARRESUMEESCALATION: &str =
        "session.permissions.clearResumeEscalation";
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
    /// `session.metadata.getClientMetadata`
    pub const SESSION_METADATA_GETCLIENTMETADATA: &str = "session.metadata.getClientMetadata";
    /// `session.metadata.updateClientMetadata`
    pub const SESSION_METADATA_UPDATECLIENTMETADATA: &str = "session.metadata.updateClientMetadata";
    /// `session.metadata.isProcessing`
    pub const SESSION_METADATA_ISPROCESSING: &str = "session.metadata.isProcessing";
    /// `session.metadata.activity`
    pub const SESSION_METADATA_ACTIVITY: &str = "session.metadata.activity";
    /// `session.metadata.contextInfo`
    pub const SESSION_METADATA_CONTEXTINFO: &str = "session.metadata.contextInfo";
    /// `session.metadata.getContextAttribution`
    pub const SESSION_METADATA_GETCONTEXTATTRIBUTION: &str =
        "session.metadata.getContextAttribution";
    /// `session.metadata.getContextHeaviestMessages`
    pub const SESSION_METADATA_GETCONTEXTHEAVIESTMESSAGES: &str =
        "session.metadata.getContextHeaviestMessages";
    /// `session.metadata.recordContextChange`
    pub const SESSION_METADATA_RECORDCONTEXTCHANGE: &str = "session.metadata.recordContextChange";
    /// `session.metadata.setWorkingDirectory`
    pub const SESSION_METADATA_SETWORKINGDIRECTORY: &str = "session.metadata.setWorkingDirectory";
    /// `session.metadata.recomputeContextTokens`
    pub const SESSION_METADATA_RECOMPUTECONTEXTTOKENS: &str =
        "session.metadata.recomputeContextTokens";
    /// `session.settings.snapshot`
    pub(crate) const SESSION_SETTINGS_SNAPSHOT: &str = "session.settings.snapshot";
    /// `session.settings.evaluatePredicate`
    pub(crate) const SESSION_SETTINGS_EVALUATEPREDICATE: &str =
        "session.settings.evaluatePredicate";
    /// `session.contentExclusion.checkPaths`
    pub const SESSION_CONTENTEXCLUSION_CHECKPATHS: &str = "session.contentExclusion.checkPaths";
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
    /// `session.history.listRewindPoints`
    pub const SESSION_HISTORY_LISTREWINDPOINTS: &str = "session.history.listRewindPoints";
    /// `session.history.previewRewind`
    pub const SESSION_HISTORY_PREVIEWREWIND: &str = "session.history.previewRewind";
    /// `session.history.rewind`
    pub const SESSION_HISTORY_REWIND: &str = "session.history.rewind";
    /// `session.history.cancelBackgroundCompaction`
    pub const SESSION_HISTORY_CANCELBACKGROUNDCOMPACTION: &str =
        "session.history.cancelBackgroundCompaction";
    /// `session.history.abortManualCompaction`
    pub const SESSION_HISTORY_ABORTMANUALCOMPACTION: &str = "session.history.abortManualCompaction";
    /// `session.history.summarizeForHandoff`
    pub const SESSION_HISTORY_SUMMARIZEFORHANDOFF: &str = "session.history.summarizeForHandoff";
    /// `session.history.clearContext`
    pub const SESSION_HISTORY_CLEARCONTEXT: &str = "session.history.clearContext";
    /// `session.queue.pendingItems`
    pub const SESSION_QUEUE_PENDINGITEMS: &str = "session.queue.pendingItems";
    /// `session.queue.snapshot`
    pub(crate) const SESSION_QUEUE_SNAPSHOT: &str = "session.queue.snapshot";
    /// `session.queue.moveItem`
    pub const SESSION_QUEUE_MOVEITEM: &str = "session.queue.moveItem";
    /// `session.queue.insertAt`
    pub const SESSION_QUEUE_INSERTAT: &str = "session.queue.insertAt";
    /// `session.queue.removeAt`
    pub const SESSION_QUEUE_REMOVEAT: &str = "session.queue.removeAt";
    /// `session.queue.updateText`
    pub const SESSION_QUEUE_UPDATETEXT: &str = "session.queue.updateText";
    /// `session.queue.withdrawMessage`
    pub const SESSION_QUEUE_WITHDRAWMESSAGE: &str = "session.queue.withdrawMessage";
    /// `session.queue.appendSteering`
    pub const SESSION_QUEUE_APPENDSTEERING: &str = "session.queue.appendSteering";
    /// `session.queue.duplicateAt`
    pub const SESSION_QUEUE_DUPLICATEAT: &str = "session.queue.duplicateAt";
    /// `session.queue.setDrainPaused`
    pub const SESSION_QUEUE_SETDRAINPAUSED: &str = "session.queue.setDrainPaused";
    /// `session.queue.sendNow`
    pub const SESSION_QUEUE_SENDNOW: &str = "session.queue.sendNow";
    /// `session.queue.hasPending`
    pub(crate) const SESSION_QUEUE_HASPENDING: &str = "session.queue.hasPending";
    /// `session.queue.beginDeferredIdleDrain`
    pub(crate) const SESSION_QUEUE_BEGINDEFERREDIDLEDRAIN: &str =
        "session.queue.beginDeferredIdleDrain";
    /// `session.queue.finishDeferredIdleDrain`
    pub(crate) const SESSION_QUEUE_FINISHDEFERREDIDLEDRAIN: &str =
        "session.queue.finishDeferredIdleDrain";
    /// `session.queue.deferSessionIdle`
    pub(crate) const SESSION_QUEUE_DEFERSESSIONIDLE: &str = "session.queue.deferSessionIdle";
    /// `session.queue.removeMostRecent`
    pub const SESSION_QUEUE_REMOVEMOSTRECENT: &str = "session.queue.removeMostRecent";
    /// `session.queue.clear`
    pub const SESSION_QUEUE_CLEAR: &str = "session.queue.clear";
    /// `session.queue.consumeSystemNotifications`
    pub(crate) const SESSION_QUEUE_CONSUMESYSTEMNOTIFICATIONS: &str =
        "session.queue.consumeSystemNotifications";
    /// `session.queue.enqueueResumePending`
    pub(crate) const SESSION_QUEUE_ENQUEUERESUMEPENDING: &str =
        "session.queue.enqueueResumePending";
    /// `session.queue.process`
    pub(crate) const SESSION_QUEUE_PROCESS: &str = "session.queue.process";
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
    /// `session.usage.setCodeChanges`
    pub(crate) const SESSION_USAGE_SETCODECHANGES: &str = "session.usage.setCodeChanges";
    /// `session.quota.get`
    pub const SESSION_QUOTA_GET: &str = "session.quota.get";
    /// `session.quota.refresh`
    pub const SESSION_QUOTA_REFRESH: &str = "session.quota.refresh";
    /// `session.quota.takeWarnings`
    pub const SESSION_QUOTA_TAKEWARNINGS: &str = "session.quota.takeWarnings";
    /// `session.limitPrediction.predict`
    pub const SESSION_LIMITPREDICTION_PREDICT: &str = "session.limitPrediction.predict";
    /// `session.remote.enable`
    pub const SESSION_REMOTE_ENABLE: &str = "session.remote.enable";
    /// `session.remote.disable`
    pub const SESSION_REMOTE_DISABLE: &str = "session.remote.disable";
    /// `session.remote.notifySteerableChanged`
    pub const SESSION_REMOTE_NOTIFYSTEERABLECHANGED: &str = "session.remote.notifySteerableChanged";
    /// `session.visibility.get`
    pub const SESSION_VISIBILITY_GET: &str = "session.visibility.get";
    /// `session.visibility.set`
    pub const SESSION_VISIBILITY_SET: &str = "session.visibility.set";
    /// `session.schedule.list`
    pub const SESSION_SCHEDULE_LIST: &str = "session.schedule.list";
    /// `session.schedule.hydrate`
    pub(crate) const SESSION_SCHEDULE_HYDRATE: &str = "session.schedule.hydrate";
    /// `session.schedule.hasSelfPaced`
    pub(crate) const SESSION_SCHEDULE_HASSELFPACED: &str = "session.schedule.hasSelfPaced";
    /// `session.schedule.add`
    pub(crate) const SESSION_SCHEDULE_ADD: &str = "session.schedule.add";
    /// `session.schedule.addCron`
    pub(crate) const SESSION_SCHEDULE_ADDCRON: &str = "session.schedule.addCron";
    /// `session.schedule.addAt`
    pub(crate) const SESSION_SCHEDULE_ADDAT: &str = "session.schedule.addAt";
    /// `session.schedule.addSelfPaced`
    pub(crate) const SESSION_SCHEDULE_ADDSELFPACED: &str = "session.schedule.addSelfPaced";
    /// `session.schedule.rearmSelfPaced`
    pub(crate) const SESSION_SCHEDULE_REARMSELFPACED: &str = "session.schedule.rearmSelfPaced";
    /// `session.schedule.stop`
    pub const SESSION_SCHEDULE_STOP: &str = "session.schedule.stop";
    /// `skillProvider.list`
    pub(crate) const SKILLPROVIDER_LIST: &str = "skillProvider.list";
    /// `skillProvider.read`
    pub(crate) const SKILLPROVIDER_READ: &str = "skillProvider.read";
    /// `providerToken.getToken`
    pub const PROVIDERTOKEN_GETTOKEN: &str = "providerToken.getToken";
    /// `workflow.execute`
    pub const WORKFLOW_EXECUTE: &str = "workflow.execute";
    /// `workflow.abort`
    pub const WORKFLOW_ABORT: &str = "workflow.abort";
    /// `tasks.cancel`
    pub const TASKS_CANCEL: &str = "tasks.cancel";
    /// `sessionFs.readFile`
    pub const SESSIONFS_READFILE: &str = "sessionFs.readFile";
    /// `sessionFs.readFileBytes`
    pub const SESSIONFS_READFILEBYTES: &str = "sessionFs.readFileBytes";
    /// `sessionFs.readFileRange`
    pub const SESSIONFS_READFILERANGE: &str = "sessionFs.readFileRange";
    /// `sessionFs.writeFile`
    pub const SESSIONFS_WRITEFILE: &str = "sessionFs.writeFile";
    /// `sessionFs.writeFileBytes`
    pub const SESSIONFS_WRITEFILEBYTES: &str = "sessionFs.writeFileBytes";
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
    /// `sessionFs.sqliteTransaction`
    pub const SESSIONFS_SQLITETRANSACTION: &str = "sessionFs.sqliteTransaction";
    /// `sessionFs.sqliteExists`
    pub const SESSIONFS_SQLITEEXISTS: &str = "sessionFs.sqliteExists";
    /// `canvas.open`
    pub const CANVAS_OPEN: &str = "canvas.open";
    /// `canvas.close`
    pub const CANVAS_CLOSE: &str = "canvas.close";
    /// `canvas.action.invoke`
    pub const CANVAS_ACTION_INVOKE: &str = "canvas.action.invoke";
}

#[path = "api_domain_shared.rs"]
mod sdk_api_shared;
pub use sdk_api_shared::{
    AccountAuthInfo, AccountAuthInfoType, AccountKind, AdaptiveThinkingSupport, AgentInfo,
    AgentInfoSource, ApiKeyAuthInfo, ApiKeyAuthInfoType, AuthIdentity, AuthInfo, AuthInfoType,
    AuthLoginAccount, AuthLoginResultDto, AuthLoginResultStatus, AuthLoginStep,
    AuthLoginStepAwaiting, AuthLoginStepAwaitingKind, AuthLoginStepCompleted,
    AuthLoginStepCompletedKind, AuthLoginStepError, AuthLoginStepErrorKind,
    AuthLoginStepInputRequired, AuthLoginStepInputRequiredKind, AuthLoginStepNeedsInteraction,
    AuthLoginStepNeedsInteractionKind, AuthLoginStepOpenUrl, AuthLoginStepOpenUrlKind,
    AuthValidationError, AutoTierBilling, AutoTierBillingPromo, AutoTierDescriptor,
    AutoTierMetadata, AutoTierStatus, AutopilotEvaluateIdleContinuationOutcome,
    AutopilotObjectiveCreditLimit, AutopilotObjectiveState, AutopilotObjectiveStatus,
    AutopilotOptions, BuiltInModelCatalogEntry, CanvasAction, CapiSessionOptions,
    CatalogAuthenticationRequiredError, CatalogAuthenticationRequiredErrorKind,
    CatalogAuthenticationRequiredReason, CatalogCapability, CatalogClientContract,
    CatalogContractViolationError, CatalogContractViolationErrorKind,
    CatalogContractViolationReason, CatalogInvalidRequestError, CatalogInvalidRequestErrorKind,
    CatalogInvalidRequestField, CatalogMalformedCardError, CatalogMalformedCardErrorKind,
    CatalogMalformedCardReason, CatalogMediaType, CatalogNegotiatedContract,
    CatalogNegotiationRefusedError, CatalogNegotiationRefusedErrorKind,
    CatalogNegotiationRefusedReason, CatalogNetworkFailureError, CatalogNetworkFailureErrorKind,
    CatalogNetworkFailureReason, CatalogPolicyRejectedError, CatalogPolicyRejectedErrorKind,
    CatalogSearchPagination, CatalogSearchTotalCountRelation, CatalogUnavailableError,
    CatalogUnavailableErrorKind, CatalogUnavailableReason, CatalogUnsafeRetrievalError,
    CatalogUnsafeRetrievalErrorKind, CatalogUnsafeRetrievalReason, ConnectedRemoteSessionMetadata,
    ConnectedRemoteSessionMetadataKind, ConnectedRemoteSessionMetadataRepository,
    ConnectorAuthorizationRequirement, ConnectorAuthorizationScope, ConnectorAvailability,
    ConnectorCatalogEntry, ConnectorCatalogResult, ConnectorCatalogStatus, ConnectorMcpStatus,
    ConnectorRuntimeStatus, ConnectorStatus, ContentExclusionPathCheck, ContextHeaviestMessage,
    CopilotApiTokenAuthInfo, CopilotApiTokenAuthInfoHost, CopilotApiTokenAuthInfoType,
    CopilotUserResponse, CopilotUserResponseEndpoints, CopilotUserResponseEnterpriseListItem,
    CopilotUserResponseQuotaSnapshots, CopilotUserResponseQuotaSnapshotsChat,
    CopilotUserResponseQuotaSnapshotsCompletions,
    CopilotUserResponseQuotaSnapshotsPremiumInteractions, CurrentModel,
    DebugCollectLogsCollectedEntry, DebugCollectLogsResultKind, DebugCollectLogsSkippedEntry,
    DebugCollectLogsSource, DiagnosticCursorStatus, DiagnosticLogLevel, DiagnosticSeverity,
    DiagnosticSourcesConfiguration, DiscoveredCanvas, DiscoveredExtension, DiscoveredExtensionMode,
    DiscoveredExtensionPlugin, DiscoveredExtensionSource, DiscoveredModel,
    EligiblePluginExtensionEntry, EnvAuthInfo, EnvAuthInfoType, EventsCursorStatus,
    EventsReadDirection, Extension, ExtensionSource, ExtensionStatus,
    ExternalToolTextResultForLlmBinaryResultsForLlm,
    ExternalToolTextResultForLlmBinaryResultsForLlmType, GhCliAuthInfo, GhCliAuthInfoType,
    GitHubRepoRef, HMACAuthInfo, HMACAuthInfoHost, HMACAuthInfoType, HistoryCompactContextWindow,
    HistoryFileRestoreSkipReason, HistoryRewindChangeType, HistoryRewindFilePreview,
    HistoryRewindOutcome, HistoryRewindPoint, HistoryRewindUnavailableReason,
    HistorySkippedFileRestore, InstallationCatalogueIdentity, InstalledPlugin, InstalledPluginInfo,
    InstalledPluginVerifiedSource, InstructionSource, InstructionSourceLocation,
    InstructionSourceType, LoginProviderKind, MarketplaceInfo, MarketplacePluginInfo,
    MarketplaceRefreshEntry, McpAllowedServer, McpAppsDiagnoseCapability, McpAppsDiagnoseServer,
    McpAppsHostContextDetails, McpAppsHostContextDetailsAvailableDisplayMode,
    McpAppsHostContextDetailsDisplayMode, McpAppsHostContextDetailsPlatform,
    McpAppsHostContextDetailsTheme, McpAppsResourceContent, McpConfiguredServer,
    McpConfiguredServerState, McpDiagnosticDetails, McpDiagnosticDirection, McpDiagnosticKind,
    McpDiagnosticSourceConfiguration, McpExecuteSamplingResult, McpFailedServer, McpFilteredServer,
    McpHostState, McpOwnedOauthLoginStatus, McpPlanPolicySource, McpPrompt, McpPromptArgument,
    McpPromptIcon, McpPromptMessage, McpPromptRole, McpResource, McpResourceAnnotations,
    McpResourceContent, McpResourceIcon, McpResourceTemplate, McpSamplingExecutionAction,
    McpServer, McpServerCardMediaType, McpServerFailureInfo, McpServerNeedsAuthInfo,
    McpServerOwnership, McpSetEnvValueModeDetails, McpToolUi, McpToolUiVisibility, McpTools, Model,
    ModelArtifactDetails, ModelBilling, ModelBillingPromo, ModelBillingTokenPrices,
    ModelBillingTokenPricesLongContext, ModelCapabilities, ModelCapabilitiesLimits,
    ModelCapabilitiesLimitsVision, ModelCapabilitiesOverride, ModelCapabilitiesOverrideLimits,
    ModelCapabilitiesOverrideLimitsVision, ModelCapabilitiesOverrideSupports,
    ModelCapabilitiesSupports, ModelMessage, ModelPickerCategory, ModelPickerPriceCategory,
    ModelPickerSettingsContext, ModelPolicy, ModelPolicyState, ModelProviderAdapterDescriptor,
    ModelProviderAdapterOperationDescriptor, ModelProviderAttribution,
    ModelProviderAutomaticDiscoveryMode, ModelProviderAutomaticDiscoveryPolicy,
    ModelProviderConfigurationDisposition, ModelProviderDiscoveryNetworkScope,
    ModelProviderInstance, ModelProviderInstanceReference, ModelProviderOperationOutcome,
    ModelProviderOperationOutcomeCode, ModelProviderProvenance, ModelProviderProvenanceSource,
    ModelProviderWarning, ModelSwitchAutoTierStatus, ModelSwitchConfirmation, ModelWarningText,
    NamedProviderConfig, OpenCanvasInstance, OptionsUpdateToolFilterPrecedence,
    PendingExternalToolRequest, PendingPermissionRequest, PermissionDecisionContext,
    PermissionDecisionOutcome, PermissionDecisionSurface, PermissionResponseCapability,
    PlanSqlTodoDependency, PlanSqlTodosRow, Plugin, PluginInstallPlan, PluginInstallPlanReview,
    PluginInstallPlanReviewAction, PluginInstallStagingMode, PluginInstallationFileReview,
    PluginInstallationLocation, PluginInstallationMcpServerReview, PluginInstallationMcpTransport,
    PluginInstallationSource, ProtocolImageGenerationOptions, ProviderConfigAzure,
    ProviderConfigModelProvider, ProviderConfigTransport, ProviderConfigType,
    ProviderConfigWireApi, ProviderDescriptor, ProviderEndpointTransport, ProviderEndpointType,
    ProviderEndpointWireApi, ProviderModelConfig, ProviderSessionToken, QueuePendingItems,
    QueuePendingItemsKind, QuotaWarningProjection, SandboxConfig, SandboxConfigAuth,
    SandboxConfigUserPolicy, SandboxConfigUserPolicyExperimental,
    SandboxConfigUserPolicyExperimentalSeatbelt, SandboxConfigUserPolicyFilesystem,
    SandboxConfigUserPolicyNetwork, SandboxConfigUserPolicyNetworkProxy,
    SandboxConfigUserPolicySeatbelt, SandboxCredentialSuggestion, SandboxCredentialsConfig,
    SandboxMaskedEnvVar, ScheduleEntry, SendAgentMode, ServerSkill, SessionCompletionItem,
    SessionVisibilityStatus, SessionWorkingDirectoryContextHostType, ShellCredentials,
    ShellInitProfile, ShellInitScript, ShellInitScriptShell, ShellOptions, Skill, SkillInstallPlan,
    SkillInstallationFileReview, SkillInstallationLocation, SkillInstallationOwnershipState,
    SkillInstallationReview, SkillInstallationReviewInstall, SkillInstallationReviewInstallAction,
    SkillInstallationReviewUninstall, SkillInstallationReviewUninstallAction,
    SkillInstallationScope, SkillInstallationSessionState, SkillInstallationSource,
    SkillInstallationSummary, SkillsInvokedSkill, SlashCommandInfo, SlashCommandInput,
    SlashCommandInputChoice, SlashCommandInputCompletion, SlashCommandKind,
    StandaloneExtensionAdmission, SubagentSettingsConfig, SubagentSettingsEntry,
    SubagentSettingsEntryContextTier, TokenAuthInfo, TokenAuthInfoType, TokenProviderAuthInfo,
    TokenProviderAuthInfoType, UIElicitationResponseAction, UserAuthInfo, UserAuthInfoType,
    WorkflowAgentContextObservation, WorkflowAgentModelObservation,
    WorkflowAgentReasoningEffortObservation, WorkflowAgentSummary, WorkflowCurrentPhase,
    WorkflowDeclaredLimits, WorkflowLogLineKind, WorkflowPauseCheckpointAction,
    WorkflowPhaseObservation, WorkflowPhaseStatus, WorkflowProgressLine, WorkflowProgressPage,
    WorkflowRunConsumed, WorkflowRunResult, WorkflowRunStatus, WorkflowRunSummary,
    WorkflowRunTerminal, WorkspaceDiffContents, WorkspaceDiffFileChange,
    WorkspaceDiffFileChangeType, WorkspaceDiffMode, WorkspaceSummaryHostType,
    WorkspacesCheckpoints, WorkspacesWorkspaceDetailsHostType,
};
pub(crate) use sdk_api_shared::{SettingsEditOperation, SettingsEditStatus};

#[path = "api_domain_session.rs"]
mod sdk_api_session;
pub use sdk_api_session::{
    AuthenticatedAuthorityGrant, AuthenticatedAuthorityUpdate, BuiltinToolDescriptor,
    BuiltinToolFormat, BuiltinToolFormatType, BuiltinToolInputSchema, BuiltinToolInputSchemaType,
    CurrentToolMetadata, FleetStartRequest, FleetStartResult, LocalSessionMetadataValue,
    ManagedMcpServerConfig, ManagedPluginRetryEntry, ManagedPluginRetryStatus, MemoryConfiguration,
    MemoryMode, MetadataSnapshotCurrentMode, MetadataSnapshotRemoteMetadata,
    MetadataSnapshotRemoteMetadataRepository, MetadataSnapshotRemoteMetadataTaskType,
    ModelProviderDescriptor, OptionsUpdateAdditionalContentExclusionPolicy,
    OptionsUpdateAdditionalContentExclusionPolicyRule,
    OptionsUpdateAdditionalContentExclusionPolicyRuleSource,
    OptionsUpdateAdditionalContentExclusionPolicyScope, OptionsUpdateContextTier,
    OptionsUpdateEnvValueMode, OptionsUpdateReasoningSummary, PermissionDecision,
    PermissionDecisionApproveForLocation, PermissionDecisionApproveForLocationApproval,
    PermissionDecisionApproveForLocationApprovalCommands,
    PermissionDecisionApproveForLocationApprovalCommandsKind,
    PermissionDecisionApproveForLocationApprovalCustomTool,
    PermissionDecisionApproveForLocationApprovalCustomToolKind,
    PermissionDecisionApproveForLocationApprovalExtensionEnvAccess,
    PermissionDecisionApproveForLocationApprovalExtensionEnvAccessKind,
    PermissionDecisionApproveForLocationApprovalExtensionManagement,
    PermissionDecisionApproveForLocationApprovalExtensionManagementKind,
    PermissionDecisionApproveForLocationApprovalExtensionPermissionAccess,
    PermissionDecisionApproveForLocationApprovalExtensionPermissionAccessKind,
    PermissionDecisionApproveForLocationApprovalMcp,
    PermissionDecisionApproveForLocationApprovalMcpKind,
    PermissionDecisionApproveForLocationApprovalMcpSampling,
    PermissionDecisionApproveForLocationApprovalMcpSamplingKind,
    PermissionDecisionApproveForLocationApprovalMemory,
    PermissionDecisionApproveForLocationApprovalMemoryKind,
    PermissionDecisionApproveForLocationApprovalRead,
    PermissionDecisionApproveForLocationApprovalReadKind,
    PermissionDecisionApproveForLocationApprovalWorkflow,
    PermissionDecisionApproveForLocationApprovalWorkflowKind,
    PermissionDecisionApproveForLocationApprovalWrite,
    PermissionDecisionApproveForLocationApprovalWriteKind,
    PermissionDecisionApproveForLocationKind, PermissionDecisionApproveForSession,
    PermissionDecisionApproveForSessionApproval,
    PermissionDecisionApproveForSessionApprovalCommands,
    PermissionDecisionApproveForSessionApprovalCommandsKind,
    PermissionDecisionApproveForSessionApprovalCustomTool,
    PermissionDecisionApproveForSessionApprovalCustomToolKind,
    PermissionDecisionApproveForSessionApprovalExtensionEnvAccess,
    PermissionDecisionApproveForSessionApprovalExtensionEnvAccessKind,
    PermissionDecisionApproveForSessionApprovalExtensionManagement,
    PermissionDecisionApproveForSessionApprovalExtensionManagementKind,
    PermissionDecisionApproveForSessionApprovalExtensionPermissionAccess,
    PermissionDecisionApproveForSessionApprovalExtensionPermissionAccessKind,
    PermissionDecisionApproveForSessionApprovalMcp,
    PermissionDecisionApproveForSessionApprovalMcpKind,
    PermissionDecisionApproveForSessionApprovalMcpSampling,
    PermissionDecisionApproveForSessionApprovalMcpSamplingKind,
    PermissionDecisionApproveForSessionApprovalMemory,
    PermissionDecisionApproveForSessionApprovalMemoryKind,
    PermissionDecisionApproveForSessionApprovalRead,
    PermissionDecisionApproveForSessionApprovalReadKind,
    PermissionDecisionApproveForSessionApprovalWorkflow,
    PermissionDecisionApproveForSessionApprovalWorkflowKind,
    PermissionDecisionApproveForSessionApprovalWrite,
    PermissionDecisionApproveForSessionApprovalWriteKind, PermissionDecisionApproveForSessionKind,
    PermissionDecisionApproveOnce, PermissionDecisionApproveOnceKind,
    PermissionDecisionApprovePermanently, PermissionDecisionApprovePermanentlyKind,
    PermissionDecisionApproveReadOnlyForSession, PermissionDecisionApproveReadOnlyForSessionKind,
    PermissionDecisionApproved, PermissionDecisionApprovedForLocation,
    PermissionDecisionApprovedForLocationKind, PermissionDecisionApprovedForSession,
    PermissionDecisionApprovedForSessionKind, PermissionDecisionApprovedKind,
    PermissionDecisionCancelled, PermissionDecisionCancelledKind,
    PermissionDecisionDeniedByContentExclusionPolicy,
    PermissionDecisionDeniedByContentExclusionPolicyKind,
    PermissionDecisionDeniedByPermissionRequestHook,
    PermissionDecisionDeniedByPermissionRequestHookKind, PermissionDecisionDeniedByRules,
    PermissionDecisionDeniedByRulesKind, PermissionDecisionDeniedInteractivelyByUser,
    PermissionDecisionDeniedInteractivelyByUserKind,
    PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUser,
    PermissionDecisionDeniedNoApprovalRuleAndCouldNotRequestFromUserKind, PermissionDecisionReject,
    PermissionDecisionRejectKind, PermissionDecisionRequest, PermissionDecisionUserNotAvailable,
    PermissionDecisionUserNotAvailableKind, PermissionLocationAddToolApprovalParams,
    PermissionLocationApplyParams, PermissionLocationApplyResult, PermissionLocationResolveParams,
    PermissionLocationResolveResult, PermissionLocationType, PermissionModeSource,
    PermissionPathsAddParams, PermissionPathsAllowedCheckParams, PermissionPathsAllowedCheckResult,
    PermissionPathsConfig, PermissionPathsList, PermissionPathsUpdatePrimaryParams,
    PermissionPathsWorkspaceCheckParams, PermissionPathsWorkspaceCheckResult,
    PermissionPromptShownNotification, PermissionRequestResult, PermissionRulesSet,
    PermissionUrlsConfig, PermissionUrlsSetUnrestrictedModeParams,
    PermissionsClearResumeEscalationRequest, PermissionsClearResumeEscalationResult,
    PermissionsConfigureAdditionalContentExclusionPolicy,
    PermissionsConfigureAdditionalContentExclusionPolicyRule,
    PermissionsConfigureAdditionalContentExclusionPolicyRuleSource,
    PermissionsConfigureAdditionalContentExclusionPolicyScope,
    PermissionsConfigureAssistedApprovalContext, PermissionsConfigureOptions,
    PermissionsConfigureParams, PermissionsConfigureResult, PermissionsEvaluateManagedPolicyResult,
    PermissionsFolderTrustAddTrustedResult, PermissionsFolderTrustCheckTrustResult,
    PermissionsGetModeRequest, PermissionsGetModeResult,
    PermissionsLocationsAddToolApprovalDetails, PermissionsLocationsAddToolApprovalDetailsCommands,
    PermissionsLocationsAddToolApprovalDetailsCommandsKind,
    PermissionsLocationsAddToolApprovalDetailsCustomTool,
    PermissionsLocationsAddToolApprovalDetailsCustomToolKind,
    PermissionsLocationsAddToolApprovalDetailsExtensionEnvAccess,
    PermissionsLocationsAddToolApprovalDetailsExtensionEnvAccessKind,
    PermissionsLocationsAddToolApprovalDetailsExtensionManagement,
    PermissionsLocationsAddToolApprovalDetailsExtensionManagementKind,
    PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccess,
    PermissionsLocationsAddToolApprovalDetailsExtensionPermissionAccessKind,
    PermissionsLocationsAddToolApprovalDetailsMcp,
    PermissionsLocationsAddToolApprovalDetailsMcpKind,
    PermissionsLocationsAddToolApprovalDetailsMcpSampling,
    PermissionsLocationsAddToolApprovalDetailsMcpSamplingKind,
    PermissionsLocationsAddToolApprovalDetailsMemory,
    PermissionsLocationsAddToolApprovalDetailsMemoryKind,
    PermissionsLocationsAddToolApprovalDetailsRead,
    PermissionsLocationsAddToolApprovalDetailsReadKind,
    PermissionsLocationsAddToolApprovalDetailsWorkflow,
    PermissionsLocationsAddToolApprovalDetailsWorkflowKind,
    PermissionsLocationsAddToolApprovalDetailsWrite,
    PermissionsLocationsAddToolApprovalDetailsWriteKind, PermissionsLocationsAddToolApprovalResult,
    PermissionsModifyRulesParams, PermissionsModifyRulesResult, PermissionsModifyRulesScope,
    PermissionsNotifyPromptShownResult, PermissionsPathsAddResult, PermissionsPathsListRequest,
    PermissionsPathsUpdatePrimaryResult, PermissionsPendingRequestsRequest,
    PermissionsResetSessionApprovalsRequest, PermissionsResetSessionApprovalsResult,
    PermissionsSetApproveAllRequest, PermissionsSetApproveAllResult,
    PermissionsSetApproveAllSource, PermissionsSetModeRequest, PermissionsSetModeResult,
    PermissionsSetRequiredRequest, PermissionsSetRequiredResult,
    PermissionsUrlsSetUnrestrictedModeResult, ProtocolExternalToolDefer,
    ProtocolExternalToolDefinition, ProtocolExternalToolNamespace, ProviderConfig,
    RemoteControlConfig, RemoteControlConfigExistingMcSession, RemoteSessionHostStatus,
    RemoteSessionMetadataRepository, RemoteSessionMetadataTaskType, RemoteSessionMetadataValue,
    RemoteSessionRepository, SandboxConfigSource, SessionAbortResult,
    SessionAccountsLoginBeginResult, SessionAccountsSetResult, SessionActivity,
    SessionAgentDeselectParams, SessionAgentGetCurrentParams, SessionAgentGetCurrentResult,
    SessionAgentListResult, SessionAgentReloadParams, SessionAgentReloadResult,
    SessionAgentSelectResult, SessionAuthInfoResult, SessionAuthLoginRequest,
    SessionAuthLogoutUserRequest, SessionAuthStatus, SessionAuthSwitchRequest,
    SessionAutopilotEvaluateIdleContinuationParams, SessionAutopilotEvaluateIdleContinuationResult,
    SessionAutopilotObjectiveGetStateParams, SessionAutopilotObjectiveGetStateResult,
    SessionBulkDeleteResult, SessionCancelAllBackgroundAgentsParams,
    SessionCancelAllBackgroundAgentsResult, SessionCanvasActionInvokeResult,
    SessionCanvasListOpenParams, SessionCanvasListOpenResult, SessionCanvasListParams,
    SessionCanvasListResult, SessionCanvasOpenResult, SessionCapability,
    SessionCommandsExecuteResult, SessionCommandsFinalizeInvocationEffectResult,
    SessionCommandsHandlePendingCommandResult, SessionCommandsListResult,
    SessionCommandsReportQueuedCommandWaitResult, SessionCommandsRespondToQueuedCommandResult,
    SessionCompletionsGetTriggerCharactersParams, SessionCompletionsGetTriggerCharactersResult,
    SessionCompletionsRequestResult, SessionConnectedIdeInfo, SessionConnectorsDisconnectResult,
    SessionConnectorsGetAccountParams, SessionConnectorsGetCapabilitiesParams,
    SessionConnectorsGetCapabilitiesResult, SessionConnectorsGetStatusParams,
    SessionConnectorsGetStatusResult, SessionConnectorsListResult,
    SessionConnectorsReconcileForStartupResult, SessionConnectorsReconcileResult,
    SessionConnectorsRefreshResult, SessionConnectorsWithdrawProjectionParams,
    SessionConnectorsWithdrawProjectionResult, SessionContentExclusionCheckPathsResult,
    SessionContext, SessionContextAttribution, SessionContextAttributionCategories,
    SessionContextAttributionCompactions, SessionContextAttributionEntriesItem,
    SessionContextHostType, SessionContextInfo, SessionCustomizationsReloadParams,
    SessionDebugCollectLogsResult, SessionDiagnosticsConfigureResult, SessionDiagnosticsReadResult,
    SessionDiagnosticsReadResultEntriesItem, SessionDiagnosticsReadResultEntriesItemSource,
    SessionEnrichMetadataResult, SessionEventLogReadResult, SessionEventLogRegisterInterestResult,
    SessionEventLogReleaseInterestResult, SessionEventLogTailParams, SessionEventLogTailResult,
    SessionExtensionsGetPolicyParams, SessionExtensionsGetPolicyResult,
    SessionExtensionsListParams, SessionExtensionsListResult, SessionExtensionsReconcileParams,
    SessionExtensionsReconcileResult, SessionExtensionsReloadParams, SessionFleetStartResult,
    SessionFsAppendFileRequest, SessionFsError, SessionFsErrorCode, SessionFsExistsRequest,
    SessionFsExistsResult, SessionFsMkdirRequest, SessionFsReadFileBytesRequest,
    SessionFsReadFileBytesResult, SessionFsReadFileRangeRequest, SessionFsReadFileRangeResult,
    SessionFsReadFileRequest, SessionFsReadFileResult, SessionFsReaddirRequest,
    SessionFsReaddirResult, SessionFsReaddirWithTypesEntry, SessionFsReaddirWithTypesEntryType,
    SessionFsReaddirWithTypesRequest, SessionFsReaddirWithTypesResult, SessionFsRenameRequest,
    SessionFsRmRequest, SessionFsSetProviderCapabilities, SessionFsSetProviderConventions,
    SessionFsSetProviderRequest, SessionFsSetProviderResult, SessionFsSqliteExistsParams,
    SessionFsSqliteExistsResult, SessionFsSqliteQueryRequest, SessionFsSqliteQueryResult,
    SessionFsSqliteQueryType, SessionFsSqliteTransactionError,
    SessionFsSqliteTransactionErrorClass, SessionFsSqliteTransactionRequest,
    SessionFsSqliteTransactionResult, SessionFsSqliteTransactionStatement, SessionFsStatRequest,
    SessionFsStatResult, SessionFsWriteFileBytesRequest, SessionFsWriteFileRequest,
    SessionGitHubAuthGetAllAuthAvailableParams, SessionGitHubAuthGetAllAuthAvailableResult,
    SessionGitHubAuthGetCurrentAuthInfoParams, SessionGitHubAuthGetStatusParams,
    SessionGitHubAuthGetStatusResult, SessionGitHubAuthLastAuthErrorsParams,
    SessionGitHubAuthLastAuthErrorsResult, SessionGitHubAuthLogoutParams,
    SessionGitHubAuthLogoutResult, SessionGitHubAuthLogoutUserResult,
    SessionGitHubAuthRefreshCopilotUserParams, SessionGitHubAuthSetCredentialsResult,
    SessionHistoryAbortManualCompactionParams, SessionHistoryAbortManualCompactionResult,
    SessionHistoryCancelBackgroundCompactionParams, SessionHistoryCancelBackgroundCompactionResult,
    SessionHistoryClearContextResult, SessionHistoryCompactResult,
    SessionHistoryListRewindPointsParams, SessionHistoryListRewindPointsResult,
    SessionHistoryPreviewRewindResult, SessionHistoryRewindResult,
    SessionHistorySummarizeForHandoffParams, SessionHistorySummarizeForHandoffResult,
    SessionHistoryTruncateResult, SessionInstalledPlugin, SessionInstalledPluginSourceGitHub,
    SessionInstalledPluginSourceGitHubSource, SessionInstalledPluginSourceLocal,
    SessionInstalledPluginSourceLocalSource, SessionInstalledPluginSourceUrl,
    SessionInstalledPluginSourceUrlSource, SessionInstructionsGetSourcesParams,
    SessionInstructionsGetSourcesResult, SessionInstructionsReloadParams,
    SessionInterruptMainTurnResult, SessionLimitPredictionBaselineData,
    SessionLimitPredictionClientType, SessionLimitPredictionDetails, SessionLimitPredictionRequest,
    SessionLimitPredictionResult, SessionLimitPredictionResultAvailable,
    SessionLimitPredictionResultAvailableKind, SessionLimitPredictionResultUnavailable,
    SessionLimitPredictionResultUnavailableKind, SessionLimitPredictionSource,
    SessionLimitPredictionTier, SessionLimitPredictionTierOption,
    SessionLimitPredictionUnavailableReason, SessionList, SessionListFilter,
    SessionLoadDeferredRepoHooksResult, SessionLogResult, SessionManagedPermissions,
    SessionManagedSettings, SessionManagedSettingsGetParams, SessionManagedSettingsGetResult,
    SessionManagedSettingsRecheckPolicyHelperParams, SessionMcpAppsCallToolResult,
    SessionMcpAppsDiagnoseResult, SessionMcpAppsGetHostContextParams,
    SessionMcpAppsGetHostContextResult, SessionMcpAppsListToolsResult,
    SessionMcpAppsReadResourceResult, SessionMcpCancelSamplingExecutionResult,
    SessionMcpConfigureGitHubResult, SessionMcpExecuteSamplingResult,
    SessionMcpHeadersHandlePendingHeadersRefreshRequestResult, SessionMcpIsServerRunningResult,
    SessionMcpListConfiguredParams, SessionMcpListConfiguredResult, SessionMcpListParams,
    SessionMcpListResult, SessionMcpListToolsResult, SessionMcpMoveLoadingToBackgroundParams,
    SessionMcpMoveLoadingToBackgroundResult, SessionMcpOauthCancelLoginParams,
    SessionMcpOauthCancelLoginResult, SessionMcpOauthHandlePendingRequestResult,
    SessionMcpOauthLoginResult, SessionMcpOauthPrepareLoginParams,
    SessionMcpOauthPrepareLoginResult, SessionMcpOauthRespondResult, SessionMcpPromptsGetResult,
    SessionMcpPromptsListResult, SessionMcpReloadParams, SessionMcpReloadWithConfigResult,
    SessionMcpRemoveGitHubParams, SessionMcpRemoveGitHubResult, SessionMcpResourcesListResult,
    SessionMcpResourcesListTemplatesResult, SessionMcpResourcesReadResult,
    SessionMcpSetConnectedIdeInfoParams, SessionMcpSetEnvValueModeResult,
    SessionMetadataActivityParams, SessionMetadataActivityResult, SessionMetadataContextInfoResult,
    SessionMetadataContextInfoResultContextInfo, SessionMetadataGetClientMetadataParams,
    SessionMetadataGetClientMetadataResult, SessionMetadataGetContextAttributionParams,
    SessionMetadataGetContextAttributionResult,
    SessionMetadataGetContextAttributionResultContextAttribution,
    SessionMetadataGetContextAttributionResultContextAttributionCategories,
    SessionMetadataGetContextAttributionResultContextAttributionCompactions,
    SessionMetadataGetContextAttributionResultContextAttributionEntriesItem,
    SessionMetadataGetContextHeaviestMessagesResult, SessionMetadataIsProcessingParams,
    SessionMetadataIsProcessingResult, SessionMetadataRecomputeContextTokensResult,
    SessionMetadataRecordContextChangeResult, SessionMetadataSetWorkingDirectoryResult,
    SessionMetadataSnapshot, SessionMetadataSnapshotParams, SessionMetadataSnapshotResult,
    SessionMetadataSnapshotResultWorkspace, SessionMetadataSnapshotWorkspace,
    SessionMetadataUpdateClientMetadataResult, SessionModeGetParams, SessionModeSetResult,
    SessionModelApplyStartupOverlayResult, SessionModelGetCurrentParams,
    SessionModelGetCurrentResult, SessionModelGetReasoningSummariesResult, SessionModelList,
    SessionModelListResult, SessionModelPriceCategory, SessionModelSetAllowedModelsResult,
    SessionModelSetReasoningEffortResult, SessionModelSwitchAutoTierResult,
    SessionModelSwitchToResult, SessionNameGetParams, SessionNameGetResult,
    SessionNameSetAutoResult, SessionOpenOptions,
    SessionOpenOptionsAdditionalContentExclusionPolicy,
    SessionOpenOptionsAdditionalContentExclusionPolicyRule,
    SessionOpenOptionsAdditionalContentExclusionPolicyRuleSource,
    SessionOpenOptionsAdditionalContentExclusionPolicyScope, SessionOpenOptionsEnvValueMode,
    SessionOpenOptionsReasoningSummary, SessionOpenParams, SessionOpenResult,
    SessionOptionsUpdateResult, SessionPermissionsClearResumeEscalationResult,
    SessionPermissionsConfigureResult, SessionPermissionsFolderTrustAddTrustedResult,
    SessionPermissionsFolderTrustIsTrustedResult, SessionPermissionsGetModeResult,
    SessionPermissionsHandlePendingPermissionRequestResult,
    SessionPermissionsLocationsAddToolApprovalResult, SessionPermissionsLocationsApplyResult,
    SessionPermissionsLocationsResolveResult, SessionPermissionsModifyRulesResult,
    SessionPermissionsNotifyPromptShownResult, SessionPermissionsPathsAddResult,
    SessionPermissionsPathsIsPathWithinAllowedDirectoriesResult,
    SessionPermissionsPathsIsPathWithinWorkspaceResult, SessionPermissionsPathsListResult,
    SessionPermissionsPathsUpdatePrimaryResult, SessionPermissionsPendingRequestsResult,
    SessionPermissionsResetSessionApprovalsResult, SessionPermissionsSetApproveAllResult,
    SessionPermissionsSetModeResult, SessionPermissionsSetRequiredResult,
    SessionPermissionsUrlsSetUnrestrictedModeResult, SessionPlanDeleteParams,
    SessionPlanReadParams, SessionPlanReadResult, SessionPlanReadSqlTodosParams,
    SessionPlanReadSqlTodosResult, SessionPlanReadSqlTodosWithDependenciesParams,
    SessionPlanReadSqlTodosWithDependenciesResult, SessionPluginsDisableRequest,
    SessionPluginsEnableRequest, SessionPluginsInstallRequest, SessionPluginsInstallResult,
    SessionPluginsListParams, SessionPluginsListResult, SessionPluginsMarketplacesAddResult,
    SessionPluginsMarketplacesBrowseResult, SessionPluginsMarketplacesListParams,
    SessionPluginsMarketplacesListResult, SessionPluginsMarketplacesRefreshResult,
    SessionPluginsMarketplacesRemoveResult, SessionPluginsRetryManagedRequest,
    SessionPluginsRetryManagedResult, SessionPluginsUpdateResult, SessionProviderAddResult,
    SessionProviderGetEndpointResult, SessionProviderSyncResult, SessionProviderWithdrawResult,
    SessionProvidersDiscoverResult, SessionProvidersGetCatalogParams,
    SessionProvidersGetCatalogResult, SessionProvidersGetStatusResult,
    SessionProvidersModelsListResult, SessionProvidersModelsPrepareConfigurationResult,
    SessionPruneResult, SessionQueueAppendSteeringResult, SessionQueueBeginDeferredIdleDrainResult,
    SessionQueueClearParams, SessionQueueConsumeSystemNotificationsResult,
    SessionQueueDuplicateAtResult, SessionQueueEnqueueResumePendingParams,
    SessionQueueEnqueueResumePendingResult, SessionQueueFinishDeferredIdleDrainResult,
    SessionQueueHasPendingParams, SessionQueueHasPendingResult, SessionQueueInsertAtResult,
    SessionQueueMoveItemResult, SessionQueuePendingItemsParams, SessionQueuePendingItemsResult,
    SessionQueueProcessParams, SessionQueueRemoveAtResult, SessionQueueRemoveMostRecentParams,
    SessionQueueRemoveMostRecentResult, SessionQueueSendNowResult, SessionQueueSnapshotParams,
    SessionQueueSnapshotResult, SessionQueueUpdateTextResult, SessionQueueWithdrawMessageResult,
    SessionQuotaDelegateWarning, SessionQuotaGetParams, SessionQuotaGetResult,
    SessionQuotaPlanTier, SessionQuotaProjection, SessionQuotaRefreshParams,
    SessionQuotaRefreshResult, SessionQuotaSnapshot, SessionQuotaTakeWarningsParams,
    SessionQuotaTakeWarningsResult, SessionRemoteDisableParams, SessionRemoteEnableResult,
    SessionRemoteNotifySteerableChangedResult, SessionSandboxDisableForSessionResult,
    SessionSandboxGetCredentialSuggestionsParams, SessionSandboxGetCredentialSuggestionsResult,
    SessionSandboxGetEnforcementStatusParams, SessionSandboxGetEnforcementStatusResult,
    SessionSandboxGrantPathForRequestResult, SessionScheduleAddAtResult,
    SessionScheduleAddCronResult, SessionScheduleAddResult, SessionScheduleAddSelfPacedResult,
    SessionScheduleHasSelfPacedParams, SessionScheduleHasSelfPacedResult,
    SessionScheduleHydrateParams, SessionScheduleListParams, SessionScheduleListResult,
    SessionScheduleRearmSelfPacedResult, SessionScheduleStopResult, SessionSendAndWaitResult,
    SessionSendMessagesResult, SessionSendResult, SessionSetCredentialsParams,
    SessionSetCredentialsResult, SessionSettingsBuiltInToolAvailabilitySnapshot,
    SessionSettingsEvaluatePredicateRequest, SessionSettingsEvaluatePredicateResult,
    SessionSettingsJobSnapshot, SessionSettingsModelSnapshot,
    SessionSettingsOnlineEvaluationSnapshot, SessionSettingsPredicateName,
    SessionSettingsRepoSnapshot, SessionSettingsSnapshot, SessionSettingsSnapshotParams,
    SessionSettingsSnapshotResult, SessionSettingsValidationSnapshot,
    SessionShellCancelUserRequestedResult, SessionShellExecResult,
    SessionShellExecuteUserRequestedResult, SessionShellKillResult, SessionSizes,
    SessionSkillsEnsureLoadedParams, SessionSkillsGetInvokedParams, SessionSkillsGetInvokedResult,
    SessionSkillsListParams, SessionSkillsListResult, SessionSkillsReloadParams,
    SessionSkillsReloadResult, SessionSource, SessionSuspendParams, SessionTasksCancelResult,
    SessionTasksGetCurrentPromotableParams, SessionTasksGetCurrentPromotableResult,
    SessionTasksGetProgressResult, SessionTasksListParams, SessionTasksListResult,
    SessionTasksPromoteCurrentToBackgroundParams, SessionTasksPromoteCurrentToBackgroundResult,
    SessionTasksPromoteToBackgroundResult, SessionTasksReadOutputResult, SessionTasksRefreshParams,
    SessionTasksRefreshResult, SessionTasksRegisterResult, SessionTasksRemoveResult,
    SessionTasksSendMessageResult, SessionTasksStartAgentResult, SessionTasksUpdateResult,
    SessionTasksWaitForPendingParams, SessionTasksWaitForPendingResult, SessionTelemetryEngagement,
    SessionTelemetryGetEngagementIdParams, SessionTelemetryGetEngagementIdResult,
    SessionToolsGetBuiltinDescriptorsResult, SessionToolsGetCurrentMetadataParams,
    SessionToolsGetCurrentMetadataResult, SessionToolsGetSubagentSettingsParams,
    SessionToolsGetSubagentSettingsResult, SessionToolsHandlePendingToolCallResult,
    SessionToolsInitializeAndValidateParams, SessionToolsInitializeAndValidateResult,
    SessionToolsListPendingRequestsResult, SessionToolsSetResult,
    SessionToolsTaskCompleteEventDataResult, SessionToolsUpdateSubagentSettingsResult,
    SessionUiElicitationResult, SessionUiEphemeralQueryResult, SessionUiHandleHumanAskUserResult,
    SessionUiHandleHumanExitPlanModeResult, SessionUiHandleHumanUserInputResult,
    SessionUiHandlePendingAutoModeSwitchResult, SessionUiHandlePendingElicitationResult,
    SessionUiHandlePendingExitPlanModeResult, SessionUiHandlePendingSamplingResult,
    SessionUiHandlePendingSessionLimitsExhaustedResult, SessionUiHandlePendingUserInputResult,
    SessionUiRegisterDirectAutoModeSwitchHandlerParams,
    SessionUiRegisterDirectAutoModeSwitchHandlerResult,
    SessionUiUnregisterDirectAutoModeSwitchHandlerResult, SessionUpdateOptionsParams,
    SessionUpdateOptionsResult, SessionUsageGetMetricsParams, SessionVisibilityGetParams,
    SessionVisibilityGetResult, SessionVisibilitySetResult, SessionWorkflowAgentResult,
    SessionWorkflowCancelResult, SessionWorkflowGetRunDetailResult,
    SessionWorkflowGetRunProgressResult, SessionWorkflowGetRunResult,
    SessionWorkflowJournalGetResult, SessionWorkflowJournalPutResult,
    SessionWorkflowListRunsResult, SessionWorkflowLogResult,
    SessionWorkflowPauseAtCheckpointResult, SessionWorkflowPauseResult,
    SessionWorkflowResumeFromToolResult, SessionWorkflowResumeResult,
    SessionWorkflowRunFromToolResult, SessionWorkflowRunResult,
    SessionWorkingDirectoryContextWithClient, SessionWorkspacesAddSummaryResult,
    SessionWorkspacesAutopilotObjectiveExistsParams,
    SessionWorkspacesAutopilotObjectiveExistsResult,
    SessionWorkspacesDeleteAutopilotObjectiveParams,
    SessionWorkspacesDeleteAutopilotObjectiveResult, SessionWorkspacesDiffResult,
    SessionWorkspacesEnsureResult, SessionWorkspacesEnsureResultWorkspace,
    SessionWorkspacesGetWorkspaceParams, SessionWorkspacesGetWorkspaceResult,
    SessionWorkspacesGetWorkspaceResultWorkspace, SessionWorkspacesListCheckpointsParams,
    SessionWorkspacesListCheckpointsResult, SessionWorkspacesListFilesParams,
    SessionWorkspacesListFilesResult, SessionWorkspacesReadAutopilotObjectiveParams,
    SessionWorkspacesReadAutopilotObjectiveResult, SessionWorkspacesReadCheckpointResult,
    SessionWorkspacesReadFileResult, SessionWorkspacesSaveLargePasteResult,
    SessionWorkspacesSaveLargePasteResultSaved, SessionWorkspacesStatFileResult,
    SessionWorkspacesTruncateSummariesResult, SessionWorkspacesTruncateSummariesResultWorkspace,
    SessionWorkspacesUpdateMetadataResult, SessionWorkspacesUpdateMetadataResultWorkspace,
    SessionWorkspacesWriteAutopilotObjectiveResult, SessionsBulkDeleteRequest,
    SessionsBulkDeleteResult, SessionsCheckInUseRequest, SessionsCheckInUseResult,
    SessionsCloseRequest, SessionsCloseResult, SessionsConnectResult,
    SessionsCreateWorkspaceRequest, SessionsCreateWorkspaceResult, SessionsDeleteRequest,
    SessionsEnrichMetadataRequest, SessionsEnrichMetadataResult, SessionsFindByPrefixRequest,
    SessionsFindByPrefixResult, SessionsFindByTaskIDRequest, SessionsFindByTaskIDResult,
    SessionsFindByTaskIdResult, SessionsForkRequest, SessionsForkResult,
    SessionsGetBoardEntryCountRequest, SessionsGetBoardEntryCountResult,
    SessionsGetClientMetadataRequest, SessionsGetClientMetadataResult,
    SessionsGetEventFilePathRequest, SessionsGetEventFilePathResult,
    SessionsGetLastForContextRequest, SessionsGetLastForContextResult, SessionsGetMetadataRequest,
    SessionsGetMetadataResult, SessionsGetPersistedRemoteSteerableRequest,
    SessionsGetPersistedRemoteSteerableResult, SessionsGetRemoteControlStatusResult,
    SessionsGetSizesResult, SessionsListNonEmptySessionIdsRequest,
    SessionsListNonEmptySessionIdsResult, SessionsListRequest, SessionsListResult,
    SessionsLoadDeferredRepoHooksRequest, SessionsLoadDeferredRepoHooksResult,
    SessionsLoadWorkspaceRequest, SessionsLoadWorkspaceResult, SessionsOpenAttach,
    SessionsOpenAttachKind, SessionsOpenCloud, SessionsOpenCloudKind, SessionsOpenCreate,
    SessionsOpenCreateKind, SessionsOpenHandoff, SessionsOpenHandoffKind,
    SessionsOpenHandoffTaskType, SessionsOpenProgress, SessionsOpenProgressStatus,
    SessionsOpenProgressStep, SessionsOpenRemote, SessionsOpenRemoteKind, SessionsOpenResult,
    SessionsOpenResume, SessionsOpenResumeKind, SessionsOpenResumeLast, SessionsOpenResumeLastKind,
    SessionsOpenStatus, SessionsPruneOldRequest, SessionsPruneOldResult,
    SessionsReadPersistedEventsRequest, SessionsReadPersistedEventsResult,
    SessionsReleaseLockRequest, SessionsReleaseLockResult, SessionsReloadPluginHooksRequest,
    SessionsReloadPluginHooksResult, SessionsSaveRequest, SessionsSaveResult,
    SessionsSetAdditionalPluginsRequest, SessionsSetAdditionalPluginsResult,
    SessionsSetRemoteControlSteeringRequest, SessionsSetRemoteControlSteeringResult,
    SessionsStartRemoteControlRequest, SessionsStartRemoteControlResult,
    SessionsStopRemoteControlRequest, SessionsStopRemoteControlResult,
    SessionsTransferRemoteControlRequest, SessionsTransferRemoteControlResult,
    SessionsUpdateWorkspaceFieldsRequest, SessionsUpdateWorkspaceFieldsResult, SettableAuthInfo,
    SettableTokenAuthInfo, SettableTokenAuthInfoType, TaskAgentInfo, TaskAgentInfoType,
    TaskAgentProgress, TaskAgentProgressType, TaskClientActiveStatus, TaskClientExecutionMode,
    TaskClientInfo, TaskClientOwner, TaskClientOwnerKind, TaskClientOwnerPresence,
    TaskClientProgress, TaskClientStatus, TaskClientType, TaskClientUpdate,
    TaskClientUpdateCancelled, TaskClientUpdateCancelledKind, TaskClientUpdateCompleted,
    TaskClientUpdateCompletedKind, TaskClientUpdateFailed, TaskClientUpdateFailedKind,
    TaskClientUpdateProgress, TaskClientUpdateProgressKind, TaskCompleteData,
    TaskCompletionDecision, TaskExecutionMode, TaskList, TaskProgressLine, TaskShellInfo,
    TaskShellInfoAttachmentMode, TaskShellInfoType, TaskShellOutputExit, TaskShellProgress,
    TaskShellProgressType, TaskStatus, TasksCancelRequest, TasksCancelResult,
    TasksGetCurrentPromotableResult, TasksGetProgressRequest, TasksGetProgressResult,
    TasksPromoteCurrentToBackgroundResult, TasksPromoteToBackgroundRequest,
    TasksPromoteToBackgroundResult, TasksReadOutputRequest, TasksReadOutputResult,
    TasksRefreshResult, TasksRegisterRequest, TasksRegisterResult, TasksRemoveRequest,
    TasksRemoveResult, TasksSendMessageRequest, TasksSendMessageResult, TasksStartAgentRequest,
    TasksStartAgentResult, TasksUpdateRequest, TasksUpdateResult, TasksWaitForPendingResult, Tool,
    ToolList, ToolResult, ToolResultExpanded, ToolResultNewMessage, ToolResultType,
    ToolsExecuteRequest, ToolsGetBuiltinDescriptorsRequest, ToolsGetBuiltinDescriptorsResult,
    ToolsGetCurrentMetadataResult, ToolsGetSubagentSettingsResult,
    ToolsInitializeAndValidateResult, ToolsListPendingRequestsRequest, ToolsListRequest,
    ToolsListResult, ToolsSetRequest, ToolsSetResult, ToolsShellDescriptorConfig,
    ToolsTaskCompleteEventDataRequest, ToolsUpdateSubagentSettingsResult,
};
pub(crate) use sdk_api_session::{
    LifecycleDiagnosticsConfiguration, PermissionsEvaluateManagedPolicyParams,
    PermissionsLocationsCleanupOrphanedResult, SessionDiagnosticsLifecycleReadResult,
    SessionModelClearStartupSeedParams, SessionModelClearStartupSeedResult,
    SessionUsageSetCodeChangesParams,
};

#[path = "api_domain_configuration.rs"]
mod sdk_api_configuration;
pub(crate) use sdk_api_configuration::{
    ConfigureSessionExtensionsParams, FeatureFlagAvailability, FeatureFlagMetadata,
    FeatureFlagsListMetadataResult, FeatureFlagsMetadataResult, FeatureFlagsResolveRequest,
    FeatureFlagsResolveStartupGatesRequest, ForceLoginOrgsDecision,
    ManagedSettingsApplyControlsParams, ManagedSettingsApplyControlsResult,
    ManagedSettingsBypassDisableSource, ManagedSettingsBypassPolicyResolution,
    ManagedSettingsIngestParams, ManagedSettingsIngestResult,
    ManagedSettingsLoadRemoteControlParams, ManagedSettingsLoadRemoteControlResult,
    ManagedSettingsLogLevel, ManagedSettingsLogMessage, ManagedSettingsParseClientValueParams,
    ManagedSettingsParseClientValueResult, ManagedSettingsPolicyHelperAuthority,
    ManagedSettingsPolicyHelperReason, ManagedSettingsReadDeviceResult,
    ManagedSettingsResolveBypassDisableTransitionParams,
    ManagedSettingsResolveBypassDisableTransitionResult,
    ManagedSettingsResolveDeviceForceLoginOrgsGateParams,
    ManagedSettingsResolveDeviceForceLoginOrgsGateResult,
    ManagedSettingsResolveEffectiveValueParams, ManagedSettingsResolveEffectiveValueResult,
    ManagedSettingsResolveStartupPolicyParams, ManagedSettingsResolveStartupPolicyResult,
    ManagedSettingsRetainPolicyParams, ManagedSettingsServerPolicyCacheMode,
    ManagedSettingsServerPolicyResolution, UserSettingsBuildRuntimeRequest,
    UserSettingsEditRequest, UserSettingsEditResult, UserSettingsGetUnknownKeysWarningResult,
    UserSettingsLoadAndMergeRepoRequest, UserSettingsLoadAndMergeRepoResult,
    UserSettingsLoadForConfigDirRequest, UserSettingsMergeManagedAndUserParams,
    UserSettingsMergeRepoValueParams, UserSettingsMigrateObsoleteOnDiskRequest,
    UserSettingsMigrateObsoleteOnDiskResult, UserSettingsMigrateObsoleteOnDiskStatus,
    UserSettingsMigrateStaffLogLevelOnceRequest, UserSettingsMigrateStaffLogLevelOnceResult,
    UserSettingsMigrateStaffUpdateChannelOnceResult,
    UserSettingsResetStaffModelSelectionOnceRequest,
    UserSettingsResetStaffModelSelectionOnceResult, UserSettingsSetHookDisabledRequest,
    UserSettingsSetHookDisabledResult, UserSettingsUnknownKeysWarningRequest,
    UserSettingsUnknownKeysWarningResult, UserSettingsUpdateRequest, UserSettingsUpdateResult,
};
pub use sdk_api_configuration::{
    FeatureFlagsResolveResult, FeatureFlagsResolveStartupGatesResult, ManagedCommunicationSettings,
    ManagedCommunicationSupport, ManagedFusionTier, ManagedPermissionEvaluation,
    ManagedPermissionOperation, ManagedPermissionOperationKind, ManagedPermissionVerdict,
    ManagedPermissionsEvaluateRequest, ManagedPermissionsEvaluateResult, ManagedSettingMeta,
    ManagedSettingsChannel, ManagedSettingsComposeLayer, ManagedSettingsComposeRequest,
    ManagedSettingsComposeResult, ManagedSettingsDiagnostic, ManagedSettingsDiagnosticSeverity,
    ManagedSettingsLayer, ManagedSettingsMeta, ManagedSettingsPermissionsEvaluateResult,
    ManagedSettingsReadResult, ManagedSettingsResolveRequest, ManagedSettingsResolveResult,
    ManagedSettingsResolvedData, ManagedSettingsSchemaResult, ManagedSettingsValidateRequest,
    ManagedSettingsValidateResult, ManagedSettingsValues, StaffLogLevelMigrationStatus,
    StaffUpdateChannelMigrationStatus, UserRequestedShellCommandResult, UserSettingMetadata,
    UserSettingsApplyTopKeyRequest, UserSettingsApplyTopKeyResult, UserSettingsBuildRuntimeResult,
    UserSettingsDiagnoseProblemsResult, UserSettingsGetPathResult, UserSettingsGetPathsRequest,
    UserSettingsGetPathsResult, UserSettingsGetResult, UserSettingsGovernanceKeysResult,
    UserSettingsHostContext, UserSettingsListGovernanceKeysResult,
    UserSettingsLoadForConfigDirResult, UserSettingsLoadForEditResult, UserSettingsLoadRepoRequest,
    UserSettingsLoadRepoResult, UserSettingsLoadWithWarningResult,
    UserSettingsMergeManagedAndUserResult, UserSettingsMergeRepoValueResult,
    UserSettingsNormalizeRequest, UserSettingsNormalizeResult, UserSettingsPathResult,
    UserSettingsProblem, UserSettingsProblemLocation, UserSettingsProblemSource,
    UserSettingsProblemsRequest, UserSettingsProblemsResult, UserSettingsSetRequest,
    UserSettingsValidateRequest, UserSettingsValidateResult, UserSettingsValidationIssue,
    UserSettingsWriteAllRequest,
};

#[path = "api_domain_integrations.rs"]
mod sdk_api_integrations;
pub use sdk_api_integrations::{
    CanvasActionInvokeRequest, CanvasActionInvokeResult, CanvasCloseRequest, CanvasHostContext,
    CanvasHostContextCapabilities, CanvasList, CanvasListOpenResult, CanvasOpenRequest,
    CanvasOpenResult, CanvasProviderCloseRequest, CanvasProviderInvokeActionRequest,
    CanvasProviderOpenRequest, CanvasProviderOpenResult, CanvasProviderRegisterRequest,
    CanvasProviderUnregisterRequest, CanvasSessionContext, CardDigest, CardDigestAlgorithm,
    CatalogHandleRejectedError, CatalogHandleRejectedErrorKind, CatalogHandleRejectionReason,
    CatalogHandleType, CatalogNotInstallableError, CatalogNotInstallableErrorKind,
    CatalogNotInstallableReason, CatalogUnavailableTransportError,
    CatalogUnavailableTransportErrorKind, CatalogUnavailableTransportReason, DiscoveredMcpServer,
    DiscoveredMcpServerType, ExtensionContextPushInput, ExtensionContextPushInputType,
    ExtensionLaunchProfile, ExtensionLaunchProviderResolveRequest,
    ExtensionLaunchProviderResolveResult, ExtensionList, ExtensionManagedPolicy,
    ExtensionsDisableRequest, ExtensionsDiscoverResult, ExtensionsEnableRequest,
    InstallationConfirmationRequest, InstallationConfirmationRequestReview,
    InstallationConfirmationRequestReviewResource, InstallationConfirmationResponse,
    InstallationDecision, InstallationReview, InstallationReviewMcp, InstallationReviewMcpResource,
    InstallationReviewPlugin, InstallationReviewPluginResource, InstallationReviewPluginReview,
    InstallationReviewPluginReviewAction, InstallationReviewResource, InstallationReviewSkill,
    InstallationReviewSkillResource, McpApplyInstallRequest, McpApplyUninstallRequest,
    McpAppsCallToolRequest, McpAppsDiagnoseRequest, McpAppsDiagnoseResult, McpAppsHostContext,
    McpAppsListToolsRequest, McpAppsListToolsResult, McpAppsReadResourceRequest,
    McpAppsReadResourceResult, McpAppsSetHostContextDetails,
    McpAppsSetHostContextDetailsAvailableDisplayMode, McpAppsSetHostContextDetailsDisplayMode,
    McpAppsSetHostContextDetailsPlatform, McpAppsSetHostContextDetailsTheme,
    McpAppsSetHostContextRequest, McpCancelSamplingExecutionParams,
    McpCancelSamplingExecutionResult, McpConfigAddRequest, McpConfigDisableRequest,
    McpConfigEnableRequest, McpConfigList, McpConfigListResult, McpConfigRemoveRequest,
    McpConfigUpdateRequest, McpConfigureGitHubResult, McpConfiguredServerList, McpDisableOptions,
    McpDisableRequest, McpDiscoverRequest, McpDiscoverResult, McpEnableOptions, McpEnableRequest,
    McpExecuteSamplingParams, McpExecuteSamplingRequest,
    McpHeadersHandlePendingHeadersRefreshRequest,
    McpHeadersHandlePendingHeadersRefreshRequestError,
    McpHeadersHandlePendingHeadersRefreshRequestErrorKind,
    McpHeadersHandlePendingHeadersRefreshRequestHeaders,
    McpHeadersHandlePendingHeadersRefreshRequestHeadersKind,
    McpHeadersHandlePendingHeadersRefreshRequestNone,
    McpHeadersHandlePendingHeadersRefreshRequestNoneKind,
    McpHeadersHandlePendingHeadersRefreshRequestRequest,
    McpHeadersHandlePendingHeadersRefreshRequestResult, McpInstallPlan,
    McpInstallationFailureReason, McpInstallationInput, McpInstallationManagementOutcome,
    McpInstallationManagementOutcomeInstallPrepared,
    McpInstallationManagementOutcomeInstallPreparedKind, McpInstallationManagementOutcomeListed,
    McpInstallationManagementOutcomeListedKind, McpInstallationManagementOutcomeOperation,
    McpInstallationManagementOutcomeOperationKind, McpInstallationManagementOutcomeRecovered,
    McpInstallationManagementOutcomeRecoveredKind,
    McpInstallationManagementOutcomeRecoveryRequired,
    McpInstallationManagementOutcomeRecoveryRequiredKind, McpInstallationManagementOutcomeRefused,
    McpInstallationManagementOutcomeRefusedKind, McpInstallationManagementOutcomeUninstallPlanned,
    McpInstallationManagementOutcomeUninstallPlannedKind, McpInstallationManagementResult,
    McpInstallationManagementResultOutcome, McpInstallationManagementResultOutcomeKind,
    McpInstallationOperationRequest, McpInstallationOperationStatus,
    McpInstallationOperationStatusApplying, McpInstallationOperationStatusApplyingPhase,
    McpInstallationOperationStatusAwaitingConfirmation,
    McpInstallationOperationStatusAwaitingConfirmationPhase,
    McpInstallationOperationStatusCompleted, McpInstallationOperationStatusCompletedPhase,
    McpInstallationOperationStatusPrepared, McpInstallationOperationStatusPreparedPhase,
    McpInstallationOperationStatusPreparing, McpInstallationOperationStatusPreparingPhase,
    McpInstallationOperationStatusRevalidating, McpInstallationOperationStatusRevalidatingPhase,
    McpInstallationOutcome, McpInstallationOutcomeCancelled, McpInstallationOutcomeCancelledKind,
    McpInstallationOutcomeDeclined, McpInstallationOutcomeDeclinedKind,
    McpInstallationOutcomeInstalled, McpInstallationOutcomeInstalledKind,
    McpInstallationOutcomeRecoveryRequired, McpInstallationOutcomeRecoveryRequiredKind,
    McpInstallationOutcomeRefused, McpInstallationOutcomeRefusedKind,
    McpInstallationOutcomeRolledBack, McpInstallationOutcomeRolledBackKind,
    McpInstallationOutcomeUninstalled, McpInstallationOutcomeUninstalledKind,
    McpInstallationRemoteConfiguration, McpInstallationResult, McpInstallationResultOutcome,
    McpInstallationResultOutcomeKind, McpInstallationReview, McpInstallationReviewInstall,
    McpInstallationReviewInstallAction, McpInstallationReviewUninstall,
    McpInstallationReviewUninstallAction, McpInstallationSecret, McpInstallationSecretStorage,
    McpInstallationState, McpInstallationSummary, McpInstallationsRequest,
    McpIsServerRunningRequest, McpIsServerRunningResult, McpListToolsRequest, McpListToolsResult,
    McpOauthAuthenticationStateChangedRequest, McpOauthCancelLoginRequest,
    McpOauthCancelLoginResult, McpOauthCompleteRequest, McpOauthHandlePendingRequest,
    McpOauthHandlePendingResult, McpOauthLoginGrantType, McpOauthLoginOptions,
    McpOauthLoginRequest, McpOauthLoginResult, McpOauthPendingRequestResponse,
    McpOauthPendingRequestResponseCancelled, McpOauthPendingRequestResponseCancelledKind,
    McpOauthPendingRequestResponseRuntimeRefresh, McpOauthPendingRequestResponseRuntimeRefreshKind,
    McpOauthPendingRequestResponseToken, McpOauthPendingRequestResponseTokenKind,
    McpOauthPrepareLoginRequest, McpOauthPrepareLoginResult, McpOauthProbeNeedsAuthReason,
    McpOauthProbeOptions, McpOauthProbeRequest, McpOauthProbeResult,
    McpOauthProbeResultAuthenticated, McpOauthProbeResultAuthenticatedStatus,
    McpOauthProbeResultFailed, McpOauthProbeResultFailedStatus, McpOauthProbeResultNeedsAuth,
    McpOauthProbeResultNeedsAuthStatus, McpOauthProbeResultNoAuthRequired,
    McpOauthProbeResultNoAuthRequiredStatus, McpOauthRespondRequest, McpOauthRespondResult,
    McpPlanConfigurationChange, McpPlanConfigurationOperation, McpPlanEnumValueType,
    McpPlanInstallOptions, McpPlanInstallPlanned, McpPlanInstallPlannedKind, McpPlanInstallRequest,
    McpPlanInstallResult, McpPlanInstallSource, McpPlanInstallSourceCandidate,
    McpPlanInstallSourceCandidateKind, McpPlanInstallSourceCard, McpPlanInstallSourceCardKind,
    McpPlanPackageInstallMethod, McpPlanPackageTransport, McpPlanPolicyDecision,
    McpPlanPolicyResult, McpPlanProvenance, McpPlanRemoteInstallMethod, McpPlanRemoteTransport,
    McpPlanRequiredValue, McpPlanRequiredValueEnum, McpPlanRequiredValueEnumKind,
    McpPlanRequiredValueScalar, McpPlanRequiredValueScalarKind, McpPlanResourceIdentity,
    McpPlanScalarValueType, McpPlanScope, McpPlanSecretPlaceholder, McpPlanSecretReference,
    McpPlanTarget, McpPlanTransportChoice, McpPlanTransportChoicePackage,
    McpPlanTransportChoiceRemote, McpPlanUninstallRequest, McpPlanValueCategory,
    McpPrepareInstallRequest, McpPreparedInstall, McpPromptsGetRequest, McpPromptsGetResult,
    McpPromptsListRequest, McpPromptsListResult, McpRemoveGitHubResult, McpResourcesListRequest,
    McpResourcesListResult, McpResourcesListTemplatesRequest, McpResourcesListTemplatesResult,
    McpResourcesReadRequest, McpResourcesReadResult, McpRestartServerOptions,
    McpRestartServerRequest, McpSafeForTelemetryFields, McpSamplingExecutionResult,
    McpServerAuthConfigRedirectPort, McpServerCardEmbedded, McpServerCardEmbeddedKind,
    McpServerCardReference, McpServerCardUrl, McpServerCardUrlKind, McpServerConfigDeferTools,
    McpServerConfigHttp, McpServerConfigHttpOauthGrantType, McpServerConfigHttpType,
    McpServerConfigStdio, McpServerConfigStdioType, McpServerList, McpSetEnvValueModeParams,
    McpSetEnvValueModeResult, McpSourceFile, McpSourcePlugin, McpSourceRef, McpStartServerOptions,
    McpStartServerRequest, McpStartServersResult, McpStopServerOptions, McpStopServerRequest,
    McpUninstallPlan, PluginApplyInstallRequest, PluginInstallResult,
    PluginInstallationFailureReason, PluginInstallationManagementOutcome,
    PluginInstallationManagementOutcomeInstallPlanned,
    PluginInstallationManagementOutcomeInstallPlannedKind,
    PluginInstallationManagementOutcomeRefused, PluginInstallationManagementOutcomeRefusedKind,
    PluginInstallationManagementResult, PluginInstallationManagementResultOutcome,
    PluginInstallationManagementResultOutcomeKind, PluginInstallationOutcome,
    PluginInstallationOutcomeCancelled, PluginInstallationOutcomeCancelledKind,
    PluginInstallationOutcomeDeclined, PluginInstallationOutcomeDeclinedKind,
    PluginInstallationOutcomeInstalled, PluginInstallationOutcomeInstalledKind,
    PluginInstallationOutcomeRefused, PluginInstallationOutcomeRefusedKind,
    PluginInstallationResult, PluginInstallationResultOutcome, PluginInstallationResultOutcomeKind,
    PluginInstallationReview, PluginInstallationReviewAction, PluginInstallationSummary,
    PluginList, PluginListResult, PluginPlanInstallRequest, PluginUpdateAllEntry,
    PluginUpdateAllResult, PluginUpdateResult, PluginsBuiltinSetRequest, PluginsDisableRequest,
    PluginsEnableRequest, PluginsInstallRequest, PluginsInstallResult, PluginsListResult,
    PluginsMarketplacesAddRequest, PluginsMarketplacesAddResult, PluginsMarketplacesBrowseRequest,
    PluginsMarketplacesBrowseResult, PluginsMarketplacesListResult,
    PluginsMarketplacesRefreshRequest, PluginsMarketplacesRefreshResult,
    PluginsMarketplacesRemoveRequest, PluginsMarketplacesRemoveResult, PluginsReloadRequest,
    PluginsUninstallRequest, PluginsUpdateAllResult, PluginsUpdateRequest, PluginsUpdateResult,
    SkillApplyInstallRequest, SkillApplyUninstallRequest, SkillDiscoveryPath,
    SkillDiscoveryPathList, SkillDiscoveryScope, SkillInstallationFailureReason,
    SkillInstallationManagementOutcome, SkillInstallationManagementOutcomeEnabledChanged,
    SkillInstallationManagementOutcomeEnabledChangedKind,
    SkillInstallationManagementOutcomeInstallPlanned,
    SkillInstallationManagementOutcomeInstallPlannedKind, SkillInstallationManagementOutcomeListed,
    SkillInstallationManagementOutcomeListedKind, SkillInstallationManagementOutcomeOperation,
    SkillInstallationManagementOutcomeOperationKind, SkillInstallationManagementOutcomeRecovered,
    SkillInstallationManagementOutcomeRecoveredKind,
    SkillInstallationManagementOutcomeRecoveryRequired,
    SkillInstallationManagementOutcomeRecoveryRequiredKind,
    SkillInstallationManagementOutcomeRefused, SkillInstallationManagementOutcomeRefusedKind,
    SkillInstallationManagementOutcomeRolledBack, SkillInstallationManagementOutcomeRolledBackKind,
    SkillInstallationManagementOutcomeUninstallPlanned,
    SkillInstallationManagementOutcomeUninstallPlannedKind, SkillInstallationManagementResult,
    SkillInstallationManagementResultOutcome, SkillInstallationManagementResultOutcomeKind,
    SkillInstallationOperationRequest, SkillInstallationOperationStatus,
    SkillInstallationOperationStatusApplying, SkillInstallationOperationStatusApplyingPhase,
    SkillInstallationOperationStatusAwaitingConfirmation,
    SkillInstallationOperationStatusAwaitingConfirmationPhase,
    SkillInstallationOperationStatusCompleted, SkillInstallationOperationStatusCompletedPhase,
    SkillInstallationOperationStatusPrepared, SkillInstallationOperationStatusPreparedPhase,
    SkillInstallationOperationStatusPreparing, SkillInstallationOperationStatusPreparingPhase,
    SkillInstallationOperationStatusRevalidating,
    SkillInstallationOperationStatusRevalidatingPhase, SkillInstallationOutcome,
    SkillInstallationOutcomeCancelled, SkillInstallationOutcomeCancelledKind,
    SkillInstallationOutcomeDeclined, SkillInstallationOutcomeDeclinedKind,
    SkillInstallationOutcomeInstalled, SkillInstallationOutcomeInstalledKind,
    SkillInstallationOutcomeRecoveryRequired, SkillInstallationOutcomeRecoveryRequiredKind,
    SkillInstallationOutcomeRefused, SkillInstallationOutcomeRefusedKind,
    SkillInstallationOutcomeRolledBack, SkillInstallationOutcomeRolledBackKind,
    SkillInstallationOutcomeUninstalled, SkillInstallationOutcomeUninstalledKind,
    SkillInstallationResult, SkillInstallationResultOutcome, SkillInstallationResultOutcomeKind,
    SkillInstallationsRequest, SkillList, SkillPlanInstallRequest, SkillPlanUninstallRequest,
    SkillProviderDescriptor, SkillProviderListParams, SkillSetEnabledRequest, SkillUninstallPlan,
    SkillsConfigSetDisabledSkillsRequest, SkillsConfigSetSkillDisabledRequest,
    SkillsDisableRequest, SkillsDiscoverRequest, SkillsDiscoverResult, SkillsEnableRequest,
    SkillsGetDiscoveryPathsOptions, SkillsGetDiscoveryPathsRequest, SkillsGetDiscoveryPathsResult,
    SkillsGetInvokedResult, SkillsLoadDiagnostics,
};
pub(crate) use sdk_api_integrations::{
    McpConfigureGitHubRequest, McpListBuiltinServerNamesResult, McpOauthCheckDcrSupportRequest,
    McpOauthCheckDcrSupportResult, McpOauthFetchProtectedResourceMetadataRequest,
    McpOauthFetchProtectedResourceMetadataResult, McpOauthIsMicrosoftAuthorizationServerRequest,
    McpOauthIsMicrosoftAuthorizationServerResult, McpOauthStaticClientConfig,
    McpOauthStaticClientConfigRequest, McpOauthStaticClientConfigResult,
    McpRegisterExternalClientRequest, McpRegistryAllocateRequestIdResult, McpRegistryCancelRequest,
    McpRegistryCancelResult, McpRegistryRequestIdResult, McpRegistrySearchRequest,
    McpRegistrySearchResult, McpReloadConfig, McpReloadWithConfigRequest, McpServerConfigMemory,
    McpServerConfigMemoryType, McpShouldExcludeGitHubToolsRequest,
    McpShouldExcludeGitHubToolsResult, McpUnregisterExternalClientRequest, SkillProviderListResult,
    SkillProviderReadRequest, SkillProviderReadResult,
};

#[path = "api_domain_models.rs"]
mod sdk_api_models;
pub use sdk_api_models::{
    ImageToolModel, ImageToolModelAvailability, ImageToolModelOperation, ImageToolModelPricing,
    ModelApplyStartupOverlayRequest, ModelGetReasoningSummariesRequest,
    ModelGetReasoningSummariesResult, ModelList, ModelListRequest, ModelPickerPersistenceRequest,
    ModelProviderAdapterCatalog, ModelProviderConfigurationPlan, ModelProviderDiscoverRequest,
    ModelProviderDiscoverResult, ModelProviderGetStatusRequest, ModelProviderModelsListRequest,
    ModelProviderPrepareConfigurationRequest, ModelProviderStatus, ModelSetAllowedModelsRequest,
    ModelSetAllowedModelsResult, ModelSetReasoningEffortRequest, ModelSetReasoningEffortResult,
    ModelSwitchAutoTierRequest, ModelSwitchAutoTierResult, ModelSwitchToOptions,
    ModelSwitchToRequest, ModelSwitchToResult, ModelTokenPriceUnit, ModelTokenPriceUnitList,
    ModelsGetBuiltInCatalogResult, ModelsListRequest, ModelsListResult,
    ModelsListTokenPriceUnitsResult, ProviderAddRequest, ProviderAddResult, ProviderEndpoint,
    ProviderGetEndpointRequest, ProviderSyncRequest, ProviderSyncResult,
    ProviderTokenAcquireRequest, ProviderTokenAcquireResult, ProviderTokenGetTokenResult,
    ProviderWithdrawRequest, ProviderWithdrawResult, TokenPriceUnit,
};
pub(crate) use sdk_api_models::{ModelClearStartupSeedRequest, ModelClearStartupSeedResult};

#[path = "api_domain_host.rs"]
mod sdk_api_host;
pub use sdk_api_host::{
    AccountAllUsers, AccountGetAllUsersResult, AccountGetCurrentAuthResult, AccountGetQuotaRequest,
    AccountGetQuotaResult, AccountHealth, AccountLoginRequest, AccountLoginResult,
    AccountLogoutRequest, AccountLogoutResult, AccountQuotaSnapshot, AccountStatus,
    AccountsEnumerateRequest, AccountsGetRequest, AccountsSetRequest, AuthEnumerateQuery,
    AuthEnumerateQueryAccounts, AuthEnumerateQueryAccountsKind, AuthEnumerateQueryLoginAccounts,
    AuthEnumerateQueryLoginAccountsKind, AuthEnumerateQueryProviders,
    AuthEnumerateQueryProvidersKind, AuthEnumerateValue, AuthEnumerateValueAccounts,
    AuthEnumerateValueAccountsKind, AuthEnumerateValueLoginAccounts,
    AuthEnumerateValueLoginAccountsKind, AuthEnumerateValueProviders,
    AuthEnumerateValueProvidersKind, AuthLoginAdvanceRequest, AuthLoginBeginOptions,
    AuthLoginBeginRequest, AuthLoginBegun, AuthLoginCancelRequest, AuthLoginIdentity,
    AuthLoginMode, AuthReadQuery, AuthReadQueryActiveAccount, AuthReadQueryActiveAccountKind,
    AuthReadQueryLastErrors, AuthReadQueryLastErrorsKind, AuthReadQueryStatus,
    AuthReadQueryStatusKind, AuthReadValue, AuthReadValueActiveAccount,
    AuthReadValueActiveAccountKind, AuthReadValueLastErrors, AuthReadValueLastErrorsKind,
    AuthReadValueStatus, AuthReadValueStatusKind, AuthStatusDto, AuthValidationErrors, AuthWrite,
    AuthWriteLogout, AuthWriteLogoutKind, AuthWriteResult, AuthWriteSetCredentials,
    AuthWriteSetCredentialsKind, AuthWriteSwitchActive, AuthWriteSwitchActiveKind, ClientMetadata,
    ClientTaskCancelReason, ClientTaskCancelRequest, ClientTaskCancelResult, DiagnosticEntry,
    DiagnosticEntrySource, DiagnosticSource, DiagnosticsConfiguration, DiagnosticsConfigureRequest,
    DiagnosticsReadRequest, DiagnosticsReadResult, DiagnosticsReadResultEntriesItem,
    DiagnosticsReadResultEntriesItemSource, GitHubRepositoryIdentity, GitHubTelemetryClientInfo,
    GitHubTelemetryEvent, GitHubTelemetryNotification, GitHubTokenAcquireReason,
    GitHubTokenAcquireRequest, GitHubTokenAcquireResult, GitHubTokenAcquireResultCancelled,
    GitHubTokenAcquireResultCancelledKind, GitHubTokenAcquireResultToken,
    GitHubTokenAcquireResultTokenKind, GitWorkingDirectoryContextResult, RemoteControlStatusActive,
    RemoteControlStatusActiveState, RemoteControlStatusConnecting,
    RemoteControlStatusConnectingState, RemoteControlStatusError, RemoteControlStatusErrorState,
    RemoteControlStatusOff, RemoteControlStatusOffState, RemoteControlStatusResult,
    RemoteControlStopResult, RemoteControlTransferResult, RemoteEnableRequest, RemoteEnableResult,
    RemoteNotifySteerableChangedRequest, RemoteNotifySteerableChangedResult,
    RemoteSessionConnectionResult, RemoteSessionMode, ServerAgentList, ServerInstructionSourceList,
    ServerSkillList, WorkspaceDiffResult, WorkspaceSummary, WorkspacesAddSummaryRequest,
    WorkspacesAddSummaryResult, WorkspacesAutopilotObjectiveExistsResult,
    WorkspacesCreateDirectoryRequest, WorkspacesCreateFileRequest,
    WorkspacesDeleteAutopilotObjectiveResult, WorkspacesDiffOptions, WorkspacesDiffRequest,
    WorkspacesEnsureRequest, WorkspacesGetWorkspaceResult, WorkspacesGetWorkspaceResultWorkspace,
    WorkspacesListCheckpointsResult, WorkspacesListFilesResult,
    WorkspacesReadAutopilotObjectiveResult, WorkspacesReadCheckpointRequest,
    WorkspacesReadCheckpointResult, WorkspacesReadFileRequest, WorkspacesReadFileResult,
    WorkspacesRemovePathRequest, WorkspacesRenamePathRequest, WorkspacesSaveLargePasteRequest,
    WorkspacesSaveLargePasteResult, WorkspacesSaveLargePasteResultSaved, WorkspacesStatFileRequest,
    WorkspacesStatFileResult, WorkspacesTruncateSummariesRequest, WorkspacesUpdateMetadataRequest,
    WorkspacesWriteAutopilotObjectiveRequest, WorkspacesWriteAutopilotObjectiveResult,
};
pub(crate) use sdk_api_host::{
    AccountDescribeAuthInfoRequest, AccountDescribeAuthInfoResult, AccountGetGistSupportRequest,
    AccountGetGistSupportResult, AccountGetTokenEnvVarResult, AccountGetWebUrlRequest,
    AccountGetWebUrlResult, GitCurrentBranchRemoteResult, GitCwdRequest, GitHubOwnerOption,
    GitHubOwnersCancelRequest, GitHubOwnersCancelResult, GitHubOwnersListRequest,
    GitHubOwnersListResult, GitHubOwnersNextRequestIdResult, GitHubOwnersRequestIdResult,
    GitHubRepositoryAtPathRequest, GitHubRepositoryAtPathResult, GitRemoteRepository,
    GitReposFromRemotesRequest, GitReposFromRemotesResult,
};

#[path = "api_domain_sandbox.rs"]
mod sdk_api_sandbox;
pub use sdk_api_sandbox::{
    SandboxApplyDefaultsRequest, SandboxCheckEnforcementCommandResult,
    SandboxCheckPlatformSupportResult, SandboxCheckWindowsHostLoopbackSupportResult,
    SandboxCredentialSuggestionsResult, SandboxDisableForSessionRequest,
    SandboxDisableForSessionResult, SandboxDocsUrlResult, SandboxEnforcementStatus,
    SandboxGetDocsUrlResult, SandboxGetHostSupportResult, SandboxGetUnsupportedMessageResult,
    SandboxGetUnsupportedSettingDisabledNoticeResult, SandboxGrantPathForRequestRequest,
    SandboxGrantPathForRequestResult, SandboxHostCapability, SandboxHostCapabilityName,
    SandboxHostSupport, SandboxProxyCaCheckInstallationSupportResult,
    SandboxProxyCaCheckRelevanceResult, SandboxProxyCaCreateResult, SandboxProxyCaGetStatusResult,
    SandboxProxyCaRemoveResult, SandboxProxyCaRequest, SandboxProxyCaRotateResult,
    SandboxProxyCaState, SandboxProxyCaStatus, SandboxProxyCaTrustResult,
    SandboxProxySecretGetLocationResult, SandboxProxySecretGetResult, SandboxProxySecretLocation,
    SandboxProxySecretSaveRequest, SandboxProxySecretSaveResult, SandboxResolveFlooredConfigResult,
    SandboxSettingsApplyDefaultsResult,
};
pub(crate) use sdk_api_sandbox::{
    SandboxEffectiveForOptionalParams, SandboxEffectiveForOptionalResult,
    SandboxEnforcementCommandAllowedParams, SandboxFilterPickerCandidatesParams,
    SandboxFilterPickerCandidatesResult, SandboxGetPathGrantWarningResult,
    SandboxGetUnsupportedStartupNoticeResult, SandboxPathGrantWarningParams,
    SandboxPathGrantWarningResult, SandboxPickerCandidate, SandboxPreparePickerPolicyParams,
    SandboxPreparePickerPolicyResult, SandboxResolveFlooredConfigParams,
    SandboxResolveForOptionalResult, SandboxUnsupportedStartupNoticeParams,
    SandboxUnsupportedStartupNoticeResult,
};

#[path = "api_domain_workflow.rs"]
mod sdk_api_workflow;
pub use sdk_api_workflow::{
    WorkflowAbortRequest, WorkflowAbortResult, WorkflowAckResult, WorkflowAgentOptions,
    WorkflowAgentRequest, WorkflowAgentResult, WorkflowCancelRequest, WorkflowDurableOperation,
    WorkflowExecuteRequest, WorkflowExecuteResult, WorkflowGetRunProgressRequest,
    WorkflowGetRunRequest, WorkflowJournalGetRequest, WorkflowJournalGetResult,
    WorkflowJournalPutRequest, WorkflowListRunsRequest, WorkflowListRunsResult, WorkflowLogLine,
    WorkflowLogRequest, WorkflowPauseCheckpointRequest, WorkflowPauseCheckpointResult,
    WorkflowPauseRequest, WorkflowResumeRequest, WorkflowResumeResult, WorkflowRunDetail,
    WorkflowRunFailureKind, WorkflowRunLimits, WorkflowRunOptions, WorkflowRunRequest,
};
pub(crate) use sdk_api_workflow::{
    WorkflowToolResumeRequest, WorkflowToolRunOptions, WorkflowToolRunRequest,
};

#[path = "api_domain_services.rs"]
mod sdk_api_services;
pub use sdk_api_services::{
    AbortRequest, AbortResult, AcceptedEnqueueCommandResult, AgentDiscoveryPath,
    AgentDiscoveryPathList, AgentDiscoveryPathScope, AgentGetCurrentResult, AgentList,
    AgentListRequest, AgentRegistryEnsureDirResult, AgentRegistryError,
    AgentRegistryListEntriesRequest, AgentRegistryListEntriesResult, AgentRegistryLiveTargetEntry,
    AgentRegistryLiveTargetEntryAttentionKind, AgentRegistryLiveTargetEntryKind,
    AgentRegistryLiveTargetEntryLastTerminalEvent, AgentRegistryLiveTargetEntryStatus,
    AgentRegistryLocationRequest, AgentRegistryLogCapture, AgentRegistryLogCaptureOpenErrorReason,
    AgentRegistryPidRequest, AgentRegistryRemoveEntryResult, AgentRegistrySpawnError,
    AgentRegistrySpawnErrorKind, AgentRegistrySpawnPermissionMode,
    AgentRegistrySpawnRegistryTimeout, AgentRegistrySpawnRegistryTimeoutKind,
    AgentRegistrySpawnRequest, AgentRegistrySpawnResult, AgentRegistrySpawnSpawned,
    AgentRegistrySpawnSpawnedKind, AgentRegistrySpawnValidationError,
    AgentRegistrySpawnValidationErrorField, AgentRegistrySpawnValidationErrorKind,
    AgentRegistrySpawnValidationErrorReason, AgentReloadResult, AgentSelectRequest,
    AgentSelectResult, AgentSetPromptRequest, AgentsDiscoverOptions, AgentsDiscoverRequest,
    AgentsDiscoverResult, AgentsGetDiscoveryPathsOptions, AgentsGetDiscoveryPathsRequest,
    AgentsGetDiscoveryPathsResult, AttachmentBlob, AttachmentBlobType, AttachmentDirectory,
    AttachmentDirectoryType, AttachmentExtensionContext, AttachmentExtensionContextType,
    AttachmentFile, AttachmentFileLineRange, AttachmentFileType, AttachmentGitHubActionsJob,
    AttachmentGitHubActionsJobType, AttachmentGitHubCommit, AttachmentGitHubCommitType,
    AttachmentGitHubFile, AttachmentGitHubFileDiff, AttachmentGitHubFileDiffSide,
    AttachmentGitHubFileDiffType, AttachmentGitHubFileType, AttachmentGitHubReference,
    AttachmentGitHubReferenceType, AttachmentGitHubRelease, AttachmentGitHubReleaseType,
    AttachmentGitHubRepository, AttachmentGitHubRepositoryType, AttachmentGitHubSnippet,
    AttachmentGitHubSnippetType, AttachmentGitHubTreeComparison,
    AttachmentGitHubTreeComparisonSide, AttachmentGitHubTreeComparisonType, AttachmentGitHubUrl,
    AttachmentGitHubUrlType, AttachmentSelection, AttachmentSelectionDetails,
    AttachmentSelectionDetailsEnd, AttachmentSelectionDetailsStart, AttachmentSelectionType,
    AuthIdentityMetadata, AutopilotEvaluateIdleContinuationResult,
    AutopilotObjectiveGetStateResult, AzureMlAcquireFamilyResult, AzureMlCancelResult,
    AzureMlCatalogRequest, AzureMlCatalogResult, AzureMlCatalogRetryMode, AzureMlCatalogWarning,
    AzureMlDiscardRequest, AzureMlDiscardResult, AzureMlDownloadIdRequest, AzureMlDownloadRequest,
    AzureMlDownloadResult, AzureMlDownloadStatus, AzureMlFamilyLeaseResult,
    AzureMlFamilyReleaseRequest, AzureMlFamilyRequest, AzureMlLatestRequest, AzureMlLatestResult,
    AzureMlLicense, AzureMlModel, AzureMlProfile, AzureMlRegistry, AzureMlReleaseFamilyResult,
    AzureMlResolveRequest, AzureMlResolveResult, AzureMlStatusResult, BuiltInModelCatalog,
    BuiltinToolSafeTelemetryFields, CancelUserRequestedShellCommandResult, CardDigestValue,
    CatalogAgentPluginCandidate, CatalogAgentPluginCandidateKind,
    CatalogAgentPluginCandidateProvenance, CatalogAgentPluginCompatibilityTag,
    CatalogAgentPluginComponent, CatalogAgentPluginInstallPlan, CatalogAgentPluginInstallSource,
    CatalogAgentPluginInstallability, CatalogAgentPluginMarketplaceSource,
    CatalogAgentPluginMarketplaceSourceKind, CatalogAgentPluginMaterializationSource,
    CatalogAgentPluginMaterializationSourceKind, CatalogAgentPluginMediaType,
    CatalogAgentPluginRepositorySource, CatalogAgentPluginRepositorySourceKind,
    CatalogAiSkillCandidate, CatalogAiSkillCandidateKind, CatalogAiSkillCandidateProvenance,
    CatalogAiSkillInstallability, CatalogAiSkillMediaType, CatalogCandidate, CatalogCandidateKind,
    CatalogCandidateSource, CatalogCandidateSourceEmbedded, CatalogCandidateSourceEmbeddedKind,
    CatalogCandidateSourceUrl, CatalogCandidateSourceUrlKind, CatalogCapabilityId,
    CatalogFeedAvailableEntry, CatalogFeedAvailableStatus, CatalogFeedEntry,
    CatalogFeedFilteredEntry, CatalogFeedFilteredStatus, CatalogFeedUnavailableEntry,
    CatalogFeedUnavailableReason, CatalogFeedUnavailableStatus, CatalogGeneratedFeedDescription,
    CatalogGetGeneratedFeedRequest, CatalogGetGeneratedFeedSucceeded,
    CatalogGetGeneratedFeedSucceededKind, CatalogGetSavedFeedRequest, CatalogGetSavedFeedSucceeded,
    CatalogGetSavedFeedSucceededKind, CatalogMaterializationReference, CatalogMcpServerCandidate,
    CatalogMcpServerCandidateKind, CatalogMcpServerCandidateProvenance,
    CatalogMcpServerInstallability, CatalogPlanInstallFromReferenceRequest,
    CatalogPluginRepositorySource, CatalogReferenceInstallationFailureReason,
    CatalogReferenceInstallationOutcome, CatalogReferenceInstallationOutcomePluginInstallPlanned,
    CatalogReferenceInstallationOutcomePluginInstallPlannedKind,
    CatalogReferenceInstallationOutcomeRefused, CatalogReferenceInstallationOutcomeRefusedKind,
    CatalogReferenceInstallationOutcomeSkillInstallPlanned,
    CatalogReferenceInstallationOutcomeSkillInstallPlannedKind, CatalogReferenceInstallationResult,
    CatalogReferenceInstallationResultOutcome, CatalogReferenceInstallationResultOutcomeKind,
    CatalogResourceGeneratedFeedResult, CatalogResourceIdentity, CatalogResourceSavedFeedResult,
    CatalogResourceVersion, CatalogSavedFeedDescription, CatalogSearchOptions, CatalogSearchPage,
    CatalogSearchRequest, CatalogSearchResult, CatalogSearchSucceeded, CatalogSearchSucceededKind,
    CatalogSelectionCancelled, CatalogSelectionCancelledKind, CatalogSelectionDecision,
    CatalogSelectionDeclined, CatalogSelectionDeclinedKind, CatalogSelectionForeign,
    CatalogSelectionForeignKind, CatalogSelectionInvalid, CatalogSelectionInvalidKind,
    CatalogSelectionReplayed, CatalogSelectionReplayedKind, CatalogSelectionRequest,
    CatalogSelectionResult, CatalogSelectionSelected, CatalogSelectionSelectedKind,
    CatalogSelectionStale, CatalogSelectionStaleKind, CatalogSelectionTimedOut,
    CatalogSelectionTimedOutKind, CatalogSelectionWrongKind, CatalogSelectionWrongKindKind,
    CatalogTrustEligibility, CatalogTrustProvenance, CatalogTrustSnapshotAbsent,
    CatalogTrustSnapshotAbsentStatus, CatalogTrustSnapshotCurrent,
    CatalogTrustSnapshotCurrentStatus, CatalogTrustSnapshotDowngraded,
    CatalogTrustSnapshotDowngradedStatus, CatalogTrustSnapshotMalformed,
    CatalogTrustSnapshotMalformedStatus, CatalogTrustSnapshotRevoked,
    CatalogTrustSnapshotRevokedStatus, CatalogTrustSnapshotSchemaVersion,
    CatalogTrustSnapshotStale, CatalogTrustSnapshotStaleStatus, CatalogTrustSnapshotUnsupported,
    CatalogTrustSnapshotUnsupportedStatus, CatalogTrustSource, CatalogTrustTier,
    CatalogUnsupportedKindError, CatalogUnsupportedKindErrorKind, CommandList,
    CommandsFinalizeInvocationEffectRequest, CommandsFinalizeInvocationEffectResult,
    CommandsHandlePendingCommandRequest, CommandsHandlePendingCommandResult,
    CommandsInvocationEffectOutcome, CommandsInvocationOrigin, CommandsInvokeRequest,
    CommandsListRequest, CommandsListResult, CommandsReportQueuedCommandWaitRequest,
    CommandsReportQueuedCommandWaitResult, CommandsRespondToQueuedCommandRequest,
    CommandsRespondToQueuedCommandResult, CompletionsGetTriggerCharactersResult,
    CompletionsRequestRequest, CompletionsRequestResult, ConnectRemoteSessionParams,
    ConnectorAccountRequest, ConnectorCapabilities, ConnectorConnectRequest,
    ConnectorConnectResult, ConnectorConnectResultConnected, ConnectorConnectResultConnectedKind,
    ConnectorConnectResultConsentRequired, ConnectorConnectResultConsentRequiredKind,
    ConnectorConnectResultPending, ConnectorConnectResultPendingKind, ConnectorContinueRequest,
    ConnectorDisconnectResult, ConnectorDiscoveryAccount, ConnectorDiscoveryAccountList,
    ConnectorDiscoveryAccountRequest, ConnectorDiscoveryAuthInfo, ConnectorDiscoveryAvailability,
    ConnectorDiscoveryCapabilities, ConnectorDiscoveryCatalogEntry,
    ConnectorDiscoveryCatalogResult, ConnectorReconcileOptions, ConnectorReconcileRequest,
    ConnectorSessionAccount, ConnectorSessionAccountResult, ConnectorsGetAccountsResult,
    ConnectorsGetCapabilitiesResult, ConnectorsListResult, ConnectorsRefreshResult,
    ContentExclusionCheckPathsRequest, ContentExclusionCheckPathsResult,
    ContentExclusionValidatePoliciesRequest, ContentExclusionValidatePoliciesResult,
    ContentFilterMode, CustomizationReloadOutcome, CustomizationReloadStatus,
    CustomizationReloadSubsystem, CustomizationsReloadResult, DebugCollectLogsDestination,
    DebugCollectLogsDestinationArchive, DebugCollectLogsDestinationArchiveKind,
    DebugCollectLogsDestinationDirectory, DebugCollectLogsDestinationDirectoryKind,
    DebugCollectLogsEntry, DebugCollectLogsEntryKind, DebugCollectLogsInclude,
    DebugCollectLogsRedaction, DebugCollectLogsRequest, DebugCollectLogsResult,
    DiscoveredExtensions, DiscoveredExtensionsDisableRequest, DiscoveredExtensionsEnableRequest,
    DiscoveredHook, DiscoveredModelList, EnqueueCommandParams, EnqueueCommandResult,
    EntraAuthConfig, EntraTokenAcquireReason, EntraTokenAcquireRequest,
    EntraTokenAcquireRequestIdentity, EntraTokenAcquireRequestIdentityKind,
    EntraTokenAcquireRequestToken, EntraTokenAcquireRequestTokenKind, EntraTokenAcquireResult,
    EntraTokenAcquireResultCancelled, EntraTokenAcquireResultCancelledKind,
    EntraTokenAcquireResultIdentity, EntraTokenAcquireResultIdentityKind,
    EntraTokenAcquireResultToken, EntraTokenAcquireResultTokenKind,
    EntraTokenAcquireResultUnavailable, EntraTokenAcquireResultUnavailableKind,
    EntraTokenGetTokenRequest, EnvironmentCapabilities, EnvironmentKind, EnvironmentsDeleteRequest,
    EnvironmentsDeleteResult, EnvironmentsGetRequest, EnvironmentsGetResult,
    EnvironmentsListRequest, EnvironmentsListResult, EventLogReadOptions, EventLogReadRequest,
    EventLogReleaseInterestResult, EventLogTailResult, EventsAgentScope, EventsReadResult,
    EventsReadView, ExecuteCommandParams, ExecuteCommandResult, ExternalToolTextResultForLlm,
    ExternalToolTextResultForLlmContentAudio, ExternalToolTextResultForLlmContentAudioType,
    ExternalToolTextResultForLlmContentImage, ExternalToolTextResultForLlmContentImageType,
    ExternalToolTextResultForLlmContentResource, ExternalToolTextResultForLlmContentResourceLink,
    ExternalToolTextResultForLlmContentResourceLinkIcon,
    ExternalToolTextResultForLlmContentResourceLinkIconTheme,
    ExternalToolTextResultForLlmContentResourceLinkType,
    ExternalToolTextResultForLlmContentResourceType, ExternalToolTextResultForLlmContentShellExit,
    ExternalToolTextResultForLlmContentShellExitType, ExternalToolTextResultForLlmContentTerminal,
    ExternalToolTextResultForLlmContentTerminalType, ExternalToolTextResultForLlmContentText,
    ExternalToolTextResultForLlmContentTextType, FolderTrustAddParams, FolderTrustCheckParams,
    FolderTrustCheckResult, GitHubEnvironment, HandlePendingToolCallRequest,
    HandlePendingToolCallResult, HistoryAbortManualCompactionResult,
    HistoryCancelBackgroundCompactionResult, HistoryClearContextRequest, HistoryClearContextResult,
    HistoryCompactRequest, HistoryCompactRequestTrigger, HistoryCompactResult,
    HistoryListRewindPointsResult, HistoryPreviewRewindRequest, HistoryPreviewRewindResult,
    HistoryRewindMode, HistoryRewindRequest, HistoryRewindResult, HistorySummarizeForHandoffResult,
    HistoryTruncateRequest, HistoryTruncateResult, HookOrigin, HookType, HooksDiscoverOptions,
    HooksDiscoverRequest, HooksDiscoverResult, HostAuthenticateRequest, HostAuthenticateResult,
    HostAuthenticationCancelledNotification, HostAuthenticationResult,
    HostCancelAuthenticationRequest, HostCancelAuthenticationResult, HostConfiguration,
    HostCreateSessionResult, HostDiagnostics, HostDisposeRequest, HostDisposeResult,
    HostEmptyResult, HostEnvironmentCredentials, HostErrorReportingStatus, HostExitReason,
    HostExitedNotification, HostGetAuthenticationCredentialsResult, HostGetConfigurationResult,
    HostGetEnvironmentCredentialsResult, HostGitHubEnvironmentOptions, HostListSessionsRequest,
    HostListSessionsResult, HostLocalServerConfiguration, HostLocalServerOptions,
    HostPublishSessionRequest, HostPublishSessionResult, HostReadyRequest, HostReadyResult,
    HostRegisterSessionRequest, HostReleaseSessionResult, HostSessionCreateCallback,
    HostSessionCreateRequest, HostSessionCreateResult, HostSessionReleaseRequest,
    HostSessionReleasedNotification, HostSessionSummary, HostSetSessionCreationPolicyRequest,
    HostSetSessionCreationPolicyResult, HostStartOptions, HostStartRequest, HostStartResult,
    HostUnpublishSessionRequest, HostUnpublishSessionResult, HostUnregisterSessionRequest,
    HostValidateAuthenticationRequest, InstalledPluginSourceGitHub,
    InstalledPluginSourceGitHubSource, InstalledPluginSourceLocal,
    InstalledPluginSourceLocalSource, InstalledPluginSourceUrl, InstalledPluginSourceUrlSource,
    InstructionDiscoveryPath, InstructionDiscoveryPathKind, InstructionDiscoveryPathList,
    InstructionDiscoveryPathLocation, InstructionsDiscoverOptions,
    InstructionsDiscoverRepoSourcesResult, InstructionsDiscoverRequest, InstructionsDiscoverResult,
    InstructionsGetDiscoveryPathsOptions, InstructionsGetDiscoveryPathsRequest,
    InstructionsGetDiscoveryPathsResult, InstructionsGetSourcesResult, InterruptMainTurnRequest,
    InterruptMainTurnResult, JsonSchemaResponseFormat, KerberosAuthAcquireRequest,
    KerberosAuthAcquireResult, KerberosAuthMode, KerberosAuthScope, KerberosAuthSetProviderRequest,
    LlmInferenceHeaders, LlmInferenceHttpRequestChunkRequest, LlmInferenceHttpRequestChunkResult,
    LlmInferenceHttpRequestStartRequest, LlmInferenceHttpRequestStartResult,
    LlmInferenceHttpRequestStartTransport, LlmInferenceHttpResponseChunkError,
    LlmInferenceHttpResponseChunkRequest, LlmInferenceHttpResponseChunkResult,
    LlmInferenceHttpResponseStartRequest, LlmInferenceHttpResponseStartResult,
    LlmInferenceSetProviderResult, LogRequest, LogResult, LspInitializeRequest,
    MarketplaceAddResult, MarketplaceBrowseResult, MarketplaceListResult, MarketplaceRefreshResult,
    MarketplaceRemoveResult, McpElicitationFormMode, McpTaskMetadata,
    MetadataContextAttributionResult, MetadataContextAttributionResultContextAttribution,
    MetadataContextAttributionResultContextAttributionCategories,
    MetadataContextAttributionResultContextAttributionCompactions,
    MetadataContextAttributionResultContextAttributionEntriesItem,
    MetadataContextHeaviestMessagesRequest, MetadataContextHeaviestMessagesResult,
    MetadataContextInfoRequest, MetadataContextInfoResult, MetadataContextInfoResultContextInfo,
    MetadataIsProcessingResult, MetadataRecomputeContextTokensRequest,
    MetadataRecomputeContextTokensResult, MetadataRecordContextChangeRequest,
    MetadataRecordContextChangeResult, MetadataSetWorkingDirectoryRequest,
    MetadataSetWorkingDirectoryResult, MetadataUpdateClientMetadataRequest, ModeSetRequest,
    ModeSetResult, MoveMcpLoadingToBackgroundResult, NameGetResult, NameSetAutoRequest,
    NameSetAutoResult, NameSetRequest, PendingExternalToolRequestList,
    PendingPermissionRequestList, PersistenceWriteRawJsonLockedRequest,
    PersistenceWriteRawJsonLockedResult, PingRequest, PingResult, PlanReadResult,
    PlanReadSqlTodosResult, PlanReadSqlTodosWithDependenciesResult, PlanUpdateRequest,
    ProtocolAppendMode, ProtocolCustomizeMode, ProtocolReplaceMode, ProtocolStaticSectionAction,
    ProtocolStaticSectionOverride, ProtocolStatusResponse, ProtocolSystemMessageAppendConfig,
    ProtocolSystemMessageCustomizeConfig, ProtocolSystemMessageReplaceConfig, PushAttachmentBlob,
    PushAttachmentBlobType, PushAttachmentDirectory, PushAttachmentDirectoryType,
    PushAttachmentFile, PushAttachmentFileLineRange, PushAttachmentFileType,
    PushAttachmentGitHubActionsJob, PushAttachmentGitHubActionsJobType, PushAttachmentGitHubCommit,
    PushAttachmentGitHubCommitType, PushAttachmentGitHubFile, PushAttachmentGitHubFileDiff,
    PushAttachmentGitHubFileDiffSide, PushAttachmentGitHubFileDiffType,
    PushAttachmentGitHubFileType, PushAttachmentGitHubReference, PushAttachmentGitHubReferenceType,
    PushAttachmentGitHubRelease, PushAttachmentGitHubReleaseType, PushAttachmentGitHubRepository,
    PushAttachmentGitHubRepositoryType, PushAttachmentGitHubSnippet,
    PushAttachmentGitHubSnippetType, PushAttachmentGitHubTreeComparison,
    PushAttachmentGitHubTreeComparisonSide, PushAttachmentGitHubTreeComparisonType,
    PushAttachmentGitHubUrl, PushAttachmentGitHubUrlType, PushAttachmentSelection,
    PushAttachmentSelectionDetails, PushAttachmentSelectionDetailsEnd,
    PushAttachmentSelectionDetailsStart, PushAttachmentSelectionType, PushGitHubRepoRef,
    QueueAppendSteeringRequest, QueueBeginDeferredIdleDrainRequest,
    QueueBeginDeferredIdleDrainResult, QueueConsumeSystemNotificationsRequest,
    QueueDeferSessionIdleRequest, QueueDuplicateAtRequest, QueueDuplicateAtResult,
    QueueEnqueueResumePendingResult, QueueFinishDeferredIdleDrainRequest,
    QueueFinishDeferredIdleDrainResult, QueueHasPendingResult, QueueInsertAtRequest,
    QueueInsertAtResult, QueueInsertMessage, QueueMoveItemRequest, QueueMoveItemResult,
    QueuePendingItemsResult, QueueRemoveAtRequest, QueueRemoveAtResult,
    QueueRemoveMostRecentResult, QueueSendNowRequest, QueueSendNowResult,
    QueueSetDrainPausedRequest, QueueSnapshotResult, QueueUpdateTextRequest, QueueUpdateTextResult,
    QueueWithdrawMessageRequest, QueueWithdrawMessageResult, QueuedCommandHandled,
    QueuedCommandNotHandled, QueuedCommandWaitRequirement, QuotaTakeWarningsResult,
    RegisterEventInterestParams, RegisterEventInterestResult, ReleaseEventInterestParams,
    ResponseFormat, ResponseFormatType, RuntimeOpenBrowserRequest, SandboxSessionChange,
    ScheduleAddAtRequest, ScheduleAddCronRequest, ScheduleAddRequest, ScheduleAddResult,
    ScheduleAddSelfPacedRequest, ScheduleHasSelfPacedResult, ScheduleList,
    ScheduleRearmSelfPacedRequest, ScheduleStopRequest, ScheduleStopResult,
    SecretsAddFilterValuesRequest, SecretsAddFilterValuesResult, SendAndWaitResult,
    SendAttachmentsToMessageParams, SendMessageItem, SendMessagesRequest,
    SendMessagesRequestResponseFormat, SendMessagesRequestResponseFormatType, SendMessagesResult,
    SendMode, SendRequest, SendRequestResponseFormat, SendRequestResponseFormatType, SendResult,
    SendSystemNotificationRequest, SessionLogLevel, SessionWorkingDirectoryContext,
    ShellCancelUserRequestedRequest, ShellExecRequest, ShellExecResult,
    ShellExecuteUserRequestedRequest, ShellKillRequest, ShellKillResult, ShellKillSignal,
    ShutdownRequest, SlashCommandAddTimelineEntryResult, SlashCommandAddTimelineEntryResultKind,
    SlashCommandAgentPromptResult, SlashCommandAgentPromptResultKind, SlashCommandCompletedResult,
    SlashCommandCompletedResultKind, SlashCommandInvocationResult, SlashCommandModelPickerDialog,
    SlashCommandModelPickerDialogKind, SlashCommandSelectSubcommandOption,
    SlashCommandSelectSubcommandResult, SlashCommandSelectSubcommandResultKind,
    SlashCommandSetModelResult, SlashCommandSetModelResultKind, SlashCommandSetPlanModelResult,
    SlashCommandSetPlanModelResultKind, SlashCommandShowDialogResult,
    SlashCommandShowDialogResultKind, SlashCommandTextResult, SlashCommandTextResultKind,
    SlashCommandTimelineEntry, SubagentSettings, SystemMessageBlock, TaskKind,
    TelemetrySetFeatureOverridesRequest, UIAutoModeSwitchResponse, UIElicitationArrayAnyOfField,
    UIElicitationArrayAnyOfFieldItems, UIElicitationArrayAnyOfFieldItemsAnyOf,
    UIElicitationArrayAnyOfFieldType, UIElicitationArrayEnumField,
    UIElicitationArrayEnumFieldItems, UIElicitationArrayEnumFieldItemsType,
    UIElicitationArrayEnumFieldType, UIElicitationRequest, UIElicitationResponse,
    UIElicitationResponseContent, UIElicitationResult, UIElicitationSchema,
    UIElicitationSchemaPropertyBoolean, UIElicitationSchemaPropertyBooleanType,
    UIElicitationSchemaPropertyNumber, UIElicitationSchemaPropertyNumberType,
    UIElicitationSchemaPropertyString, UIElicitationSchemaPropertyStringFormat,
    UIElicitationSchemaPropertyStringType, UIElicitationSchemaType, UIElicitationStringEnumField,
    UIElicitationStringEnumFieldType, UIElicitationStringOneOfField,
    UIElicitationStringOneOfFieldOneOf, UIElicitationStringOneOfFieldType, UIEphemeralQueryRequest,
    UIEphemeralQueryResult, UIExitPlanModeAction, UIExitPlanModeResponse,
    UIHandlePendingAutoModeSwitchRequest, UIHandlePendingElicitationRequest,
    UIHandlePendingExitPlanModeRequest, UIHandlePendingResult, UIHandlePendingSamplingRequest,
    UIHandlePendingSamplingResponse, UIHandlePendingSessionLimitsExhaustedRequest,
    UIHandlePendingUserInputRequest, UIRegisterDirectAutoModeSwitchHandlerResult,
    UISessionLimitsExhaustedResponse, UISessionLimitsExhaustedResponseAction,
    UIUnregisterDirectAutoModeSwitchHandlerRequest, UIUnregisterDirectAutoModeSwitchHandlerResult,
    UIUserInputResponse, UiThreadRunRequest, UnsupportedEnqueueCommandResult,
    UpdateSubagentSettingsRequest, VisibilityGetResult, VisibilitySetRequest, VisibilitySetResult,
};
pub(crate) use sdk_api_services::{
    AgentsCustomAgentInitialModelDecisionParams, AgentsCustomAgentInitialModelDecisionResult,
    AgentsGetAvailableBuiltinsRequest, AgentsGetAvailableBuiltinsResult,
    AgentsGetBuiltinDefinitionRequest, AgentsGetBuiltinDefinitionResult,
    AgentsGetBuiltinListingDefinitionRequest, AgentsGetBuiltinListingDefinitionResult,
    AgentsGetBuiltinsResult, BuiltinAgentSummary, ConnectClientInfo, ConnectRequest, ConnectResult,
    FolderTrustIsTrustedParams, GlobalStateLoadForConfigDirRequest,
    GlobalStateLoadForConfigDirResult, GlobalStateLoadResult, GlobalStateWriteKeyRequest,
    HookInvokeRequest, HookInvokeResponse, InstructionsDiscoverRepoSourcesRequest,
    InstructionsGetInitPromptResult, InstructionsInitPromptResult, LifecycleDiagnosticsReadRequest,
    LifecycleDiagnosticsReadResult, LocationPermissionsCleanupResult, LoggedInUser,
    RepoSettingsEditRequest, RepoSettingsEditResult, RepoSettingsEditScope,
    RepoSettingsReadRawRequest, RepoSettingsReadRawResult, RepoSettingsWriteAllRequest,
    RepoSettingsWriteKeyRequest, SettingsEditResult, SettingsRedactValueForDisplayRequest,
    SettingsRedactValueForDisplayResult, SettingsRemoveRawKeyRequest, SettingsRemoveRawKeyResult,
    UsageSetCodeChangesRequest,
};
