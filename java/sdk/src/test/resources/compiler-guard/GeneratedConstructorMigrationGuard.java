import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

public final class GeneratedConstructorMigrationGuard {
    private static final String MODEL_SWITCH_DESCRIPTOR =
            "(Ljava/lang/String;Ljava/lang/String;Lcom/github/copilot/generated/rpc/AutoTier;Ljava/lang/String;"
                    + "Lcom/github/copilot/generated/rpc/ReasoningSummary;Lcom/github/copilot/generated/rpc/Verbosity;"
                    + "Lcom/github/copilot/generated/rpc/ModelCapabilitiesOverride;"
                    + "Lcom/github/copilot/generated/rpc/ContextTier;"
                    + "Lcom/github/copilot/generated/rpc/ModelChangeSource;Ljava/lang/Boolean;Ljava/lang/String;"
                    + "Ljava/lang/Boolean;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;"
                    + "Lcom/github/copilot/generated/rpc/ModelPickerPersistenceRequest;)V";
    private static final List<CompatibilityConstruction> APPROVED = List.of(
            new CompatibilityConstruction(
                    new Construction(
                            "src/main/java/com/github/copilot/CopilotSession.java",
                            "com.github.copilot.CopilotSession#setModel(Ljava/lang/String;Ljava/lang/String;)"
                                    + "Ljava/util/concurrent/CompletableFuture;",
                            "com.github.copilot.generated.rpc.SessionModelSwitchToParams",
                            MODEL_SWITCH_DESCRIPTOR,
                            "new"),
                    "Preserves the released model-switch request constructor for the existing setModel overload."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/AutoModeRecoveryTest.java",
                            "com.github.copilot.AutoModeRecoveryTest"
                                    + "#prefixLookupKeepsLegacyRecordsMethodReferencesAndNullCalls()V",
                            "com.github.copilot.generated.rpc.SessionsFindByPrefixParams",
                            "(Ljava/lang/String;)V",
                            "new"),
                    "Protects the released prefix-lookup record and its method-reference and null-call compatibility."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/AutoModeRecoveryTest.java",
                            "com.github.copilot.AutoModeRecoveryTest#uiCapabilityChangesKeepThePublishedConstructor()V",
                            "com.github.copilot.generated.CapabilitiesChangedUI",
                            "(Ljava/lang/Boolean;Ljava/lang/Boolean;Ljava/lang/Boolean;)V",
                            "new"),
                    "Protects the released UI capability constructor and omission of the later Auto recovery field."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/AutoModeRecoveryTest.java",
                            "com.github.copilot.AutoModeRecoveryTest#directRegistrationRecordsKeepThePublishedConstructors()V",
                            "com.github.copilot.generated.rpc.SessionUiRegisterDirectAutoModeSwitchHandlerParams",
                            "(Ljava/lang/String;)V",
                            "new"),
                    "Protects the released registration request descriptor and omission of recovery enrollment."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/AutoModeRecoveryTest.java",
                            "com.github.copilot.AutoModeRecoveryTest#directRegistrationRecordsKeepThePublishedConstructors()V",
                            "com.github.copilot.generated.rpc.SessionUiRegisterDirectAutoModeSwitchHandlerResult",
                            "(Ljava/lang/String;)V",
                            "new"),
                    "Protects the released registration result descriptor and omission of the recovery acknowledgment."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/ClientTurnIdCompatibilityTest.java",
                            "com.github.copilot.ClientTurnIdCompatibilityTest"
                                    + "#releasedUserMessageConstructorKeepsArgumentMeaning()V",
                            "com.github.copilot.generated.UserMessageEvent$UserMessageEventData",
                            "(Ljava/util/List;Ljava/lang/String;Lcom/github/copilot/generated/ResponsesReasoning;"
                                    + "Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/util/List;Ljava/util/List;"
                                    + "Ljava/lang/String;Lcom/github/copilot/generated/UserMessageDelivery;"
                                    + "Lcom/github/copilot/generated/UserMessageAgentMode;Ljava/lang/Boolean;"
                                    + "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                            "new"),
                    "Protects released user-message argument meanings and omission of the later client-turn ID."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/ClientTurnIdCompatibilityTest.java",
                            "com.github.copilot.ClientTurnIdCompatibilityTest"
                                    + "#releasedSendParamsKeepTheirShapeAndRequestsCarryClientTurnId()V",
                            "com.github.copilot.generated.rpc.SessionSendParams",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/util/List;"
                                    + "Lcom/github/copilot/generated/rpc/SendMode;Ljava/lang/Boolean;Ljava/lang/Boolean;"
                                    + "Ljava/lang/String;Ljava/lang/String;Lcom/github/copilot/generated/rpc/SendAgentMode;"
                                    + "Ljava/util/Map;"
                                    + "Lcom/github/copilot/generated/rpc/SessionSendParams$SessionSendParamsResponseFormat;"
                                    + "Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;)V",
                            "new"),
                    "Protects the released send descriptor and its wire shape alongside client-turn request support."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/ClientTurnIdCompatibilityTest.java",
                            "com.github.copilot.ClientTurnIdCompatibilityTest"
                                    + "#releasedSendMessagesParamsKeepTheirShapeAndRequestsCarryClientTurnId()V",
                            "com.github.copilot.generated.rpc.SessionSendMessagesParams",
                            "(Ljava/lang/String;Ljava/util/List;Lcom/github/copilot/generated/rpc/SendMode;"
                                    + "Ljava/lang/Boolean;Lcom/github/copilot/generated/rpc/SendAgentMode;Ljava/util/Map;"
                                    + "Lcom/github/copilot/generated/rpc/SessionSendMessagesParams"
                                    + "$SessionSendMessagesParamsResponseFormat;"
                                    + "Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;)V",
                            "new"),
                    "Protects the released send-messages descriptor and omission of the later client-turn ID."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/main/java/com/github/copilot/CopilotSession.java",
                            "com.github.copilot.CopilotSession#setModel(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                                    + "Lcom/github/copilot/rpc/ModelCapabilitiesOverride;)"
                                    + "Ljava/util/concurrent/CompletableFuture;",
                            "com.github.copilot.generated.rpc.SessionModelSwitchToParams",
                            MODEL_SWITCH_DESCRIPTOR,
                            "new"),
                    "Preserves the released model-switch request constructor for the existing setModel overload."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/LegacyRequestCompatibilityTest.java",
                            "com.github.copilot.LegacyRequestCompatibilityTest#existingRecordCallsOmitTheAddedFields()V",
                            "com.github.copilot.generated.rpc.McpPlanInstallParams",
                            "(Lcom/github/copilot/generated/rpc/CatalogClientContract;Ljava/lang/Object;"
                                    + "Lcom/github/copilot/generated/rpc/McpPlanScope;)V",
                            "new"),
                    "Protects the released record descriptor and omission of the later policy-session field."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/LegacyRequestCompatibilityTest.java",
                            "com.github.copilot.LegacyRequestCompatibilityTest#existingRecordCallsOmitTheAddedFields()V",
                            "com.github.copilot.generated.rpc.CatalogSearchParams",
                            "(Lcom/github/copilot/generated/rpc/CatalogClientContract;Ljava/lang/String;Ljava/lang/Long;"
                                    + "Ljava/util/List;Lcom/github/copilot/generated/rpc/CatalogSearchPage;)V",
                            "new"),
                    "Protects the released record descriptor and omission of the later policy-session field."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/ImageGenerationConfigTest.java",
                            "com.github.copilot.ImageGenerationConfigTest"
                                    + "#protocolOptionsReleasedConstructorPreservesOmittedFields(Ljava/lang/Boolean;)V",
                            "com.github.copilot.generated.rpc.ProtocolImageGenerationOptions",
                            "(Ljava/lang/Boolean;)V",
                            "new"),
                    "Protects the released one-component descriptor and omission of later image-generation fields."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/McpServerLegacyConstructorCompatibilityTest.java",
                            "com.github.copilot.McpServerLegacyConstructorCompatibilityTest"
                                    + "#releasedConstructorOmitsTheOwnedMarker()V",
                            "com.github.copilot.generated.rpc.McpServer",
                            "(Ljava/lang/String;Lcom/github/copilot/generated/rpc/McpServerStatus;"
                                    + "Lcom/github/copilot/generated/rpc/McpServerSource;Ljava/lang/String;"
                                    + "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                                    + "Lcom/github/copilot/generated/rpc/McpServerMetadata;)V",
                            "new"),
                    "Protects the released McpServer descriptor and omission of the new owned marker."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/OwnedMcpLegacyRequestCompatibilityTest.java",
                            "com.github.copilot.OwnedMcpLegacyRequestCompatibilityTest"
                                    + "#existingRecordCallsOmitOwnedIdentity()V",
                            "com.github.copilot.generated.rpc.SessionMcpEnableParams",
                            "(Ljava/lang/String;Ljava/lang/String;)V",
                            "new"),
                    "Protects the released two-component descriptor and omission of owned-identity fields."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/OwnedMcpLegacyRequestCompatibilityTest.java",
                            "com.github.copilot.OwnedMcpLegacyRequestCompatibilityTest"
                                    + "#existingRecordCallsOmitOwnedIdentity()V",
                            "com.github.copilot.generated.rpc.SessionMcpOauthLoginParams",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;Ljava/lang/String;"
                                    + "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;"
                                    + "Lcom/github/copilot/generated/rpc/McpOauthLoginGrantType;)V",
                            "new"),
                    "Protects the released nine-component descriptor and omission of owned-identity fields."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/GeneratedTypesJacksonRoundTripTest.java",
                            "com.github.copilot.generated.GeneratedTypesJacksonRoundTripTest"
                                    + "#sandboxConfigLegacyConstructorPreservesRoutingAndWireDefaults()V",
                            "com.github.copilot.generated.rpc.SandboxConfig",
                            "(Ljava/lang/Boolean;Lcom/github/copilot/generated/rpc/SandboxConfigUserPolicy;"
                                    + "Ljava/lang/Boolean;Ljava/lang/Boolean;Ljava/lang/Boolean;Ljava/lang/Boolean;"
                                    + "Ljava/lang/Boolean;Ljava/lang/Boolean;Lcom/github/copilot/generated/rpc/SandboxConfigAuth;"
                                    + "Lcom/github/copilot/generated/rpc/SandboxCredentialsConfig;Ljava/lang/Boolean;)V",
                            "new"),
                    "Protects the released SandboxConfig descriptor and its routing defaults and wire omission."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/GeneratedTypesJacksonRoundTripTest.java",
                            "com.github.copilot.generated.GeneratedTypesJacksonRoundTripTest"
                                    + "#sandboxFilesystemLegacyConstructorOmitsEnumeratePaths()V",
                            "com.github.copilot.generated.rpc.SandboxConfigUserPolicyFilesystem",
                            "(Ljava/util/List;Ljava/util/List;Ljava/util/List;)V",
                            "new"),
                    "Protects the mainline filesystem constructor and omission of the later enumerate-only paths."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/SessionInfoConstructorCompatibilityTest.java",
                            "com.github.copilot.SessionInfoConstructorCompatibilityTest"
                                    + "#releasedConstructorPreservesUrlAndTip()V",
                            "com.github.copilot.generated.SessionInfoEvent$SessionInfoEventData",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                            "new"),
                    "Protects the released session-info descriptor and omission of later reasoning metadata."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/PluginLegacyConstructorCompatibilityTest.java",
                            "com.github.copilot.PluginLegacyConstructorCompatibilityTest"
                                    + "#releasedConstructorsOmitTheAssignment()V",
                            "com.github.copilot.generated.rpc.Plugin",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;"
                                    + "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                                    + "Ljava/lang/Boolean;Ljava/lang/Boolean;Ljava/lang/Boolean;)V",
                            "new"),
                    "Protects the released Plugin descriptor and omission of the later managed assignment."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/PluginLegacyConstructorCompatibilityTest.java",
                            "com.github.copilot.PluginLegacyConstructorCompatibilityTest"
                                    + "#releasedConstructorsOmitTheAssignment()V",
                            "com.github.copilot.generated.rpc.InstalledPluginInfo",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                                    + "Ljava/lang/Boolean;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;"
                                    + "Ljava/lang/Boolean;Ljava/lang/Boolean;Ljava/lang/String;"
                                    + "Lcom/github/copilot/generated/rpc/InstalledPluginVerifiedSource;Ljava/lang/String;)V",
                            "new"),
                    "Protects the released InstalledPluginInfo descriptor and omission of the later managed assignment."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/PluginLegacyConstructorCompatibilityTest.java",
                            "com.github.copilot.PluginLegacyConstructorCompatibilityTest"
                                    + "#releasedConstructorsOmitTheAssignment()V",
                            "com.github.copilot.generated.rpc.MarketplacePluginInfo",
                            "(Ljava/lang/String;Ljava/lang/String;)V",
                            "new"),
                    "Protects the released MarketplacePluginInfo descriptor and omission of the later managed assignment."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/SessionEventHandlingTest.java",
                            "com.github.copilot.SessionEventHandlingTest"
                                    + "#testLegacyLifecycleEventConstructorsRemainAvailable()V",
                            "com.github.copilot.generated.SessionStartEvent$SessionStartEventData",
                            "(Ljava/lang/String;Ljava/lang/Long;Ljava/lang/String;Ljava/lang/String;"
                                    + "Ljava/time/OffsetDateTime;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                                    + "Lcom/github/copilot/generated/ReasoningSummary;"
                                    + "Lcom/github/copilot/generated/Verbosity;Lcom/github/copilot/generated/ContextTier;"
                                    + "Lcom/github/copilot/generated/AutoTier;"
                                    + "Lcom/github/copilot/generated/SessionLimitsConfig;"
                                    + "Lcom/github/copilot/generated/WorkingDirectoryContext;"
                                    + "Lcom/github/copilot/generated/GitHubMcpToolConfig;Ljava/lang/Boolean;"
                                    + "Ljava/lang/Boolean;Ljava/lang/String;)V",
                            "new"),
                    "Protects the released lifecycle-start descriptor for callers using positional event payloads."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/SessionEventHandlingTest.java",
                            "com.github.copilot.SessionEventHandlingTest"
                                    + "#testLegacyLifecycleEventConstructorsRemainAvailable()V",
                            "com.github.copilot.generated.SessionResumeEvent$SessionResumeEventData",
                            "(Ljava/time/OffsetDateTime;Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/String;"
                                    + "Ljava/lang/String;Ljava/lang/String;Lcom/github/copilot/generated/ReasoningSummary;"
                                    + "Lcom/github/copilot/generated/Verbosity;Lcom/github/copilot/generated/ContextTier;"
                                    + "Lcom/github/copilot/generated/AutoTier;"
                                    + "Lcom/github/copilot/generated/SessionLimitsConfig;"
                                    + "Lcom/github/copilot/generated/WorkingDirectoryContext;Ljava/lang/Boolean;"
                                    + "Ljava/lang/Boolean;Ljava/lang/Boolean;Ljava/lang/Boolean;)V",
                            "new"),
                    "Protects the released lifecycle-resume descriptor for callers using positional event payloads."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/rpc/GeneratedRpcRecordsCoverageTest.java",
                            "com.github.copilot.generated.rpc.GeneratedRpcRecordsCoverageTest"
                                    + "#sessionFsSetProviderCapabilities_preserves_legacy_constructor_and_binary_wire_field()V",
                            "com.github.copilot.generated.rpc.SessionFsSetProviderCapabilities",
                            "(Ljava/lang/Boolean;)V",
                            "new"),
                    "Protects the released one-component descriptor and omission of the later binary field."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/rpc/GeneratedRpcRecordsCoverageTest.java",
                            "com.github.copilot.generated.rpc.GeneratedRpcRecordsCoverageTest"
                                    + "#managedSettingsResolve_preserves_legacy_constructors()V",
                            "com.github.copilot.generated.rpc.ManagedSettingsResolveParams",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                            "new"),
                    "Protects the released four-component managed-settings descriptor."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/rpc/GeneratedRpcRecordsCoverageTest.java",
                            "com.github.copilot.generated.rpc.GeneratedRpcRecordsCoverageTest"
                                    + "#managedSettingsResolve_preserves_legacy_constructors()V",
                            "com.github.copilot.generated.rpc.ManagedSettingsResolveParams",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                            "new"),
                    "Protects the released three-component managed-settings descriptor."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/rpc/GeneratedRpcRecordsCoverageTest.java",
                            "com.github.copilot.generated.rpc.GeneratedRpcRecordsCoverageTest"
                                    + "#sessionEventLogRead_preserves_legacy_constructor_and_wire_fields()V",
                            "com.github.copilot.generated.rpc.SessionEventLogReadParams",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/Object;"
                                    + "Lcom/github/copilot/generated/rpc/EventsAgentScope;Ljava/util/List;"
                                    + "Lcom/github/copilot/generated/rpc/EventsReadDirection;Ljava/lang/Boolean;)V",
                            "new"),
                    "Protects the released session-event-log read descriptor."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/rpc/GeneratedRpcRecordsCoverageTest.java",
                            "com.github.copilot.generated.rpc.GeneratedRpcRecordsCoverageTest"
                                    + "#customizationLegacyConstructors()Ljava/util/stream/Stream;",
                            "com.github.copilot.generated.rpc.AgentInfo",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                                    + "Lcom/github/copilot/generated/rpc/AgentInfoSource;Ljava/lang/Boolean;"
                                    + "Ljava/lang/Boolean;Ljava/util/List;Ljava/lang/String;Ljava/util/List;"
                                    + "Lcom/github/copilot/generated/rpc/AgentModelPolicy;Ljava/lang/String;"
                                    + "Ljava/util/Map;Ljava/util/List;Ljava/lang/String;Ljava/util/Map;)V",
                            "new"),
                    "Protects the pre-plugin-metadata AgentInfo descriptor and omission of pluginId."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/rpc/GeneratedRpcRecordsCoverageTest.java",
                            "com.github.copilot.generated.rpc.GeneratedRpcRecordsCoverageTest"
                                    + "#customizationLegacyConstructors()Ljava/util/stream/Stream;",
                            "com.github.copilot.generated.rpc.DiscoveredExtensionPlugin",
                            "(Ljava/lang/String;)V",
                            "new"),
                    "Protects the pre-plugin-metadata DiscoveredExtensionPlugin descriptor and omission of id."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/rpc/GeneratedRpcRecordsCoverageTest.java",
                            "com.github.copilot.generated.rpc.GeneratedRpcRecordsCoverageTest"
                                    + "#customizationLegacyConstructors()Ljava/util/stream/Stream;",
                            "com.github.copilot.generated.rpc.DiscoveredHook",
                            "(Ljava/lang/String;Lcom/github/copilot/generated/rpc/HookType;"
                                    + "Lcom/github/copilot/generated/rpc/HookOrigin;Ljava/lang/String;"
                                    + "Ljava/lang/String;Ljava/lang/Boolean;Ljava/lang/String;)V",
                            "new"),
                    "Protects the pre-plugin-metadata DiscoveredHook descriptor and omission of ownership fields."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/rpc/GeneratedRpcRecordsCoverageTest.java",
                            "com.github.copilot.generated.rpc.GeneratedRpcRecordsCoverageTest"
                                    + "#customizationLegacyConstructors()Ljava/util/stream/Stream;",
                            "com.github.copilot.generated.rpc.InstalledPluginInfo",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                                    + "Ljava/lang/Boolean;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;"
                                    + "Ljava/lang/Boolean;Ljava/lang/Boolean;Ljava/lang/String;"
                                    + "Lcom/github/copilot/generated/rpc/InstalledPluginVerifiedSource;"
                                    + "Ljava/lang/String;)V",
                            "new"),
                    "Protects the pre-plugin-metadata InstalledPluginInfo descriptor and omission of id and rootPath."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/rpc/GeneratedRpcRecordsCoverageTest.java",
                            "com.github.copilot.generated.rpc.GeneratedRpcRecordsCoverageTest"
                                    + "#customizationLegacyConstructors()Ljava/util/stream/Stream;",
                            "com.github.copilot.generated.rpc.InstructionSource",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                                    + "Lcom/github/copilot/generated/rpc/InstructionSourceType;"
                                    + "Lcom/github/copilot/generated/rpc/InstructionSourceLocation;"
                                    + "Ljava/util/List;Ljava/lang/String;Ljava/lang/Boolean;Ljava/lang/String;)V",
                            "new"),
                    "Protects the pre-plugin-metadata InstructionSource descriptor and omission of pluginId."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/rpc/GeneratedRpcRecordsCoverageTest.java",
                            "com.github.copilot.generated.rpc.GeneratedRpcRecordsCoverageTest"
                                    + "#customizationLegacyConstructors()Ljava/util/stream/Stream;",
                            "com.github.copilot.generated.rpc.Plugin",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Boolean;"
                                    + "Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                                    + "Ljava/lang/Boolean;Ljava/lang/Boolean;Ljava/lang/Boolean;)V",
                            "new"),
                    "Protects the pre-plugin-metadata Plugin descriptor and omission of id and rootPath."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/rpc/GeneratedRpcRecordsCoverageTest.java",
                            "com.github.copilot.generated.rpc.GeneratedRpcRecordsCoverageTest"
                                    + "#customizationLegacyConstructors()Ljava/util/stream/Stream;",
                            "com.github.copilot.generated.rpc.ServerSkill",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                                    + "Lcom/github/copilot/generated/rpc/SkillSource;Ljava/lang/Boolean;"
                                    + "Ljava/lang/Boolean;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                            "new"),
                    "Protects the pre-plugin-metadata ServerSkill descriptor and omission of pluginId."),
            new CompatibilityConstruction(
                    new Construction(
                            "src/test/java/com/github/copilot/generated/rpc/GeneratedRpcRecordsCoverageTest.java",
                            "com.github.copilot.generated.rpc.GeneratedRpcRecordsCoverageTest"
                                    + "#customizationLegacyConstructors()Ljava/util/stream/Stream;",
                            "com.github.copilot.generated.rpc.Skill",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;"
                                    + "Lcom/github/copilot/generated/rpc/SkillSource;"
                                    + "Lcom/github/copilot/generated/rpc/McpSkillOrigin;Ljava/lang/Boolean;"
                                    + "Ljava/lang/Boolean;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                            "new"),
                    "Protects the pre-plugin-metadata Skill descriptor and omission of pluginId."));

    private record Construction(String source, String enclosing, String target, String descriptor, String kind) {}

    private record CompatibilityConstruction(Construction construction, String protectedBehavior) {}

    private record Inspection(Set<String> selectedTypes, List<Construction> constructions, List<String> unresolved,
            List<String> errors) {}

    public static void main(String[] args) throws Exception {
        Path moduleRoot = Path.of(args[0]).toAbsolutePath().normalize();
        Path generatedRoot = moduleRoot.resolve("src/generated/java");
        Path productionRoot = moduleRoot.resolve("src/main/java");
        Path testRoot = moduleRoot.resolve("src/test/java");
        List<Path> generated = javaSources(generatedRoot);
        List<Path> production = javaSources(productionRoot);
        List<Path> tests = javaSources(testRoot);
        require(!generated.isEmpty(), "Generated source scope must be nonempty");
        require(!production.isEmpty(), "Production source scope must be nonempty");
        require(!tests.isEmpty(), "Test source scope must be nonempty");

        List<Path> sources = new ArrayList<>(generated);
        sources.addAll(production);
        sources.addAll(tests);
        Inspection inspection = inspect(moduleRoot, generatedRoot, productionRoot, testRoot, sources, args[1]);
        require(inspection.errors().isEmpty(),
                "Compiler errors prevent attribution:\n" + String.join("\n", inspection.errors()));
        require(!inspection.selectedTypes().isEmpty(), "Generated DTO ownership scope must be nonempty");
        require(inspection.unresolved().isEmpty(),
                "Unresolved constructor sites:\n" + String.join("\n", inspection.unresolved()));

        Set<Construction> exceptionKeys = new HashSet<>();
        for (CompatibilityConstruction exception : APPROVED) {
            exceptionKeys.add(exception.construction());
            require(!exception.protectedBehavior().isBlank(),
                    "Compatibility exception must name the protected behavior: " + exception.construction());
        }
        require(exceptionKeys.size() == APPROVED.size(), "Duplicate compatibility exceptions");
        Map<Construction, Integer> exceptionCounts = new HashMap<>();
        for (CompatibilityConstruction exception : APPROVED) {
            exceptionCounts.put(exception.construction(), 0);
        }
        List<String> violations = new ArrayList<>();
        for (Construction site : inspection.constructions()) {
            Construction key = new Construction(site.source(), site.enclosing(), site.target(), site.descriptor(),
                    site.kind());
            if (exceptionKeys.contains(key)) {
                exceptionCounts.compute(key, (ignored, count) -> count + 1);
            } else {
                violations.add(site.source() + ":" + site.enclosing() + " constructs " + site.target()
                        + site.descriptor() + " using " + site.kind());
            }
        }
        for (CompatibilityConstruction exception : APPROVED) {
            int count = exceptionCounts.get(exception.construction());
            require(count == 1, "Stale or multiplied compatibility exception (" + exception.protectedBehavior()
                    + "): " + exception.construction());
        }
        require(violations.isEmpty(),
                "Unapproved generated DTO constructor sites:\n" + String.join("\n", violations));
        System.out.println("Resolved " + inspection.selectedTypes().size() + " generated DTO identities; "
                + inspection.constructions().size() + " positional sites match " + APPROVED.size()
                + " exact compatibility exceptions.");
    }

    private static List<Path> javaSources(Path root) throws Exception {
        require(Files.isDirectory(root), "Missing source scope: " + root);
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .filter(path -> !path.getFileName().toString().equals("module-info.java"))
                    .sorted(Comparator.comparing(Path::toString)).toList();
        }
    }

    private static Inspection inspect(Path moduleRoot, Path generatedRoot, Path productionRoot, Path testRoot,
            List<Path> sources, String classpath) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        require(compiler != null, "A full JDK is required for resolved constructor attribution");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            var options = List.of("--release", "17", "-encoding", "UTF-8", "-proc:none", "-classpath", classpath,
                    "-implicit:none");
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics, options, null,
                    manager.getJavaFileObjectsFromPaths(sources));
            List<CompilationUnitTree> units = new ArrayList<>();
            task.parse().forEach(units::add);
            task.analyze();

            Trees trees = Trees.instance(task);
            Types types = task.getTypes();
            Elements elements = task.getElements();
            TypeElement sessionEvent = elements.getTypeElement("com.github.copilot.generated.SessionEvent");
            Set<String> selectedTypes = new HashSet<>();
            for (CompilationUnitTree unit : units) {
                Path source = Path.of(unit.getSourceFile().toUri()).toAbsolutePath().normalize();
                if (!source.startsWith(generatedRoot)) {
                    continue;
                }
                new TreePathScanner<Void, Void>() {
                    @Override
                    public Void visitClass(ClassTree tree, Void ignored) {
                        Element element = trees.getElement(getCurrentPath());
                        if (element instanceof TypeElement type && isSelected(type, sessionEvent, types)) {
                            selectedTypes.add(elements.getBinaryName(type).toString());
                        }
                        return super.visitClass(tree, ignored);
                    }
                }.scan(unit, null);
            }

            List<Construction> constructions = new ArrayList<>();
            List<String> unresolved = new ArrayList<>();
            for (CompilationUnitTree unit : units) {
                Path source = Path.of(unit.getSourceFile().toUri()).toAbsolutePath().normalize();
                if (!source.startsWith(productionRoot) && !source.startsWith(testRoot)) {
                    continue;
                }
                String logicalSource = moduleRoot.relativize(source).toString().replace(File.separatorChar, '/');
                new TreePathScanner<Void, Void>() {
                    private String enclosing() {
                        for (TreePath cursor = getCurrentPath(); cursor != null; cursor = cursor.getParentPath()) {
                            if (cursor.getLeaf() instanceof MethodTree) {
                                Element element = trees.getElement(cursor);
                                if (element instanceof ExecutableElement executable
                                        && executable.getEnclosingElement() instanceof TypeElement owner) {
                                    return elements.getBinaryName(owner) + "#" + executable.getSimpleName()
                                            + descriptor(executable, types, elements);
                                }
                            }
                        }
                        return "<initializer>";
                    }

                    private void record(Tree tree, String kind) {
                        Element element = trees.getElement(getCurrentPath());
                        if (!(element instanceof ExecutableElement executable)
                                || !(executable.getEnclosingElement() instanceof TypeElement owner)) {
                            if (tree instanceof MemberReferenceTree reference
                                    && reference.getQualifierExpression().getKind() == Tree.Kind.ARRAY_TYPE) {
                                return;
                            }
                            unresolved.add(logicalSource + " unresolved " + kind + ": " + tree);
                            return;
                        }
                        String target = elements.getBinaryName(owner).toString();
                        if (selectedTypes.contains(target)) {
                            constructions.add(new Construction(logicalSource, enclosing(), target,
                                    descriptor(executable, types, elements), kind));
                        }
                    }

                    @Override
                    public Void visitNewClass(NewClassTree tree, Void ignored) {
                        record(tree, "new");
                        return super.visitNewClass(tree, ignored);
                    }

                    @Override
                    public Void visitMemberReference(MemberReferenceTree tree, Void ignored) {
                        if (tree.getMode() == MemberReferenceTree.ReferenceMode.NEW) {
                            record(tree, "constructor-reference");
                        }
                        return super.visitMemberReference(tree, ignored);
                    }

                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree tree, Void ignored) {
                        Element element = trees.getElement(getCurrentPath());
                        if (element instanceof ExecutableElement executable
                                && executable.getKind() == ElementKind.CONSTRUCTOR) {
                            record(tree, "constructor-delegation");
                        }
                        return super.visitMethodInvocation(tree, ignored);
                    }
                }.scan(unit, null);
            }

            List<String> errors = diagnostics.getDiagnostics().stream()
                    .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                    .map(GeneratedConstructorMigrationGuard::formatDiagnostic).toList();
            return new Inspection(selectedTypes, constructions, unresolved, errors);
        }
    }

    private static boolean isSelected(TypeElement type, TypeElement sessionEvent, Types types) {
        if (hasJsonValue(type)) {
            return false;
        }
        if (type.getKind() == ElementKind.RECORD) {
            return true;
        }
        if (type.getKind() != ElementKind.CLASS || type.getModifiers().contains(Modifier.ABSTRACT)
                || sessionEvent != null && types.isSubtype(type.asType(), sessionEvent.asType())) {
            return false;
        }
        TypeMirror superclass = type.getSuperclass();
        if (!(superclass instanceof DeclaredType declared)) {
            return false;
        }
        TypeElement parent = (TypeElement) declared.asElement();
        return hasAnnotation(parent, "com.fasterxml.jackson.annotation.JsonTypeInfo");
    }

    private static boolean hasAnnotation(TypeElement type, String annotationName) {
        return type.getAnnotationMirrors().stream()
                .anyMatch(annotation -> annotation.getAnnotationType().toString().equals(annotationName));
    }

    private static boolean hasJsonValue(TypeElement type) {
        return type.getEnclosedElements().stream().filter(element -> element instanceof ExecutableElement)
                .map(element -> (ExecutableElement) element)
                .anyMatch(method -> method.getAnnotationMirrors().stream().anyMatch(annotation -> annotation
                        .getAnnotationType().toString().equals("com.fasterxml.jackson.annotation.JsonValue")));
    }

    private static String descriptor(ExecutableElement executable, Types types, Elements elements) {
        String parameters = executable.getParameters().stream()
                .map(parameter -> descriptor(types.erasure(parameter.asType()), types, elements))
                .reduce("", String::concat);
        String result = executable.getKind() == ElementKind.CONSTRUCTOR ? "V"
                : descriptor(types.erasure(executable.getReturnType()), types, elements);
        return "(" + parameters + ")" + result;
    }

    private static String descriptor(TypeMirror type, Types types, Elements elements) {
        TypeMirror erased = types.erasure(type);
        return switch (erased.getKind()) {
            case BOOLEAN -> "Z";
            case BYTE -> "B";
            case SHORT -> "S";
            case INT -> "I";
            case LONG -> "J";
            case CHAR -> "C";
            case FLOAT -> "F";
            case DOUBLE -> "D";
            case VOID -> "V";
            case ARRAY -> "[" + descriptor(((ArrayType) erased).getComponentType(), types, elements);
            case DECLARED -> "L" + elements.getBinaryName((TypeElement) ((DeclaredType) erased).asElement())
                    .toString().replace('.', '/') + ";";
            default -> throw new IllegalArgumentException("Unresolved descriptor type: " + erased);
        };
    }

    private static String formatDiagnostic(Diagnostic<? extends JavaFileObject> diagnostic) {
        return (diagnostic.getSource() == null ? "" : diagnostic.getSource().getName()) + ":"
                + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(null);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
