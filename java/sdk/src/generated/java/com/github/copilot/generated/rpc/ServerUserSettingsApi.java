/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.copilot.CopilotExperimental;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code user.settings} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ServerUserSettingsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerUserSettingsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Adds or removes one hook key in the user `disabledHooks` setting, rewriting settings.json in place and leaving every other setting untouched. Reports a typed outcome rather than failing the call, so a host can tell an unparseable settings file apart from a write that could not complete.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsSetHookDisabledResult> setHookDisabled(UserSettingsSetHookDisabledParams params) {
        return caller.invoke("user.settings.setHookDisabled", params, UserSettingsSetHookDisabledResult.class);
    }

    /**
     * Reads the user's settings document exactly as `user.settings.load` does, but from a caller-supplied configuration directory instead of the one the server resolved for itself. Use this when a consumer scopes a session to its own Copilot home — the SDK's per-session `configDir` override — so the settings read match the directory that session actually uses. An absent or empty `configDir` resolves the server's own home, making this identical to `user.settings.load`. Hook-validation relaxations are deliberately not caller-supplied: they continue to come from the server's environment, so a consumer cannot widen the host's hook policy through this method.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<JsonNode> loadForConfigDir(UserSettingsLoadForConfigDirParams params) {
        return caller.invoke("user.settings.loadForConfigDir", params, JsonNode.class);
    }

    /**
     * Reads a repository's checked-in settings document as stored, normalized, together with any diagnostic raised while reading it. This is the repository half of the layering `user.settings.get` reports: `user.settings.load` reads the user's own document, and this reads the one a repository checks in to govern the keys `user.settings.governanceKeys` lists as repo-governable. A document that fails to parse, or that sets a key a repository may not govern, is reported through `warning` while the settings that did load are still returned, so a consumer surfaces the diagnostic and continues rather than failing the session. When `repoRoot` is the user's home directory the Claude-compatible configuration is skipped, so a home-directory session does not inherit a personal config as repository policy.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsLoadRepoResult> loadRepo(UserSettingsLoadRepoParams params) {
        return caller.invoke("user.settings.loadRepo", params, UserSettingsLoadRepoResult.class);
    }

    /**
     * Reads a repository's settings and merges them over a supplied user layer in one step, returning the settings a consumer should act on inside that repository. Use this rather than reading the two layers separately: the merge is per setting and collection-aware, so a repository adds to the collections it is allowed to influence without replacing the user's own entries, and a whole-document overlay would get that wrong. Any diagnostic raised while reading the repository layer is returned rather than printed, so the consumer decides how to surface it; the settings that did load are merged regardless.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsLoadAndMergeRepoResult> loadAndMergeRepo(UserSettingsLoadAndMergeRepoParams params) {
        return caller.invoke("user.settings.loadAndMergeRepo", params, UserSettingsLoadAndMergeRepoResult.class);
    }

    /**
     * Reports the on-disk path of the settings document `user.settings.load` and `user.settings.set` read and write. Resolved the same way those methods resolve it, so a host that shows or opens the settings file names the file the runtime actually uses, including the extensionless legacy `settings` file when that is what is present.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsPathResult> getPath() {
        return caller.invoke("user.settings.getPath", java.util.Map.of(), UserSettingsPathResult.class);
    }

    /**
     * Reads the user's stored settings together with the diagnostic raised while reading them. This is `user.settings.load` plus the reason part of the document was ignored: that one answers the document alone, so a caller cannot tell a clean read from a salvaged one. Read this when the caller is about to write the file back, where accepting a partial parse would discard every value the loader could not read. The settings that did load are still reported alongside the warning, so a consumer may surface it and continue instead of failing.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsLoadWithWarningResult> loadWithWarning() {
        return caller.invoke("user.settings.loadWithWarning", java.util.Map.of(), UserSettingsLoadWithWarningResult.class);
    }

    /**
     * Folds a user settings layer over a managed (enterprise policy) layer and returns the merged settings. The managed layer's governance keys win, `autoTier` resolves through its overridable form, and the two per-entry control maps (`enabledPlugins`, `extraKnownMarketplaces`) union across the layers and are then stamped back from the managed layer, so each entry an administrator declares wins while the user keeps control of the rest. The parameters are named for their layers rather than being a generic base/override pair: nothing about a settings object identifies which layer produced it, so the runtime cannot detect a transposed call, and transposing one resolves the governance keys in the user's favour instead of the administrator's. Supplying only one layer returns it unchanged, and supplying neither returns an empty object. Rejects a managed `autoTier` that is neither a user-configurable tier nor a single-key overridable form, rather than silently dropping it.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsMergeManagedAndUserResult> mergeManagedAndUser(UserSettingsMergeManagedAndUserParams params) {
        return caller.invoke("user.settings.mergeManagedAndUser", params, UserSettingsMergeManagedAndUserResult.class);
    }

    /**
     * Folds a repository's settings layer into the user layer. Each repo-overridable key carries a directional policy chosen so a repository can only add restrictions and never relax the user's own. The two per-entry managed control maps are the exception those directional rules cannot express — a repository may legitimately enable plugins and declare marketplaces — so they are re-stamped afterwards from the runtime's ambient managed policy. That makes the result depend on the runtime process's managed state and not on the arguments alone: a host must call this rather than reimplement the fold, or a repository could relax a restriction the administrator imposed. Rejects when either layer is not a JSON object.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsMergeRepoValueResult> mergeRepoValue(UserSettingsMergeRepoValueParams params) {
        return caller.invoke("user.settings.mergeRepoValue", params, UserSettingsMergeRepoValueResult.class);
    }

    /**
     * Runs the one-time migration that moves a staff account's update channel to stable, and records that it ran. Idempotent and self-gating: it takes a lock over the global-state file, re-reads the completion stamp inside it, and returns `current` rather than migrating twice, so two hosts racing at startup cannot both migrate. Only a `migrated` status means the persisted settings changed and the caller should reload them. An account on the experimental channel is deliberately left alone and reported as `excluded-experimental`.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsMigrateStaffUpdateChannelOnceResult> migrateStaffUpdateChannelOnce(UserSettingsMigrateStaffUpdateChannelOnceParams params) {
        return caller.invoke("user.settings.migrateStaffUpdateChannelOnce", params, UserSettingsMigrateStaffUpdateChannelOnceResult.class);
    }

    /**
     * Runs the one-time reset that clears a staff account's pinned model selection, and reports the timestamp the host should record. Idempotent and self-gating: the reset is gated on a fixed cutoff, so a caller that passes back the timestamp it recorded gets `didReset: false` rather than a second reset. It applies to staff accounts only; any other account is reported as not reset without the settings document being opened. Only `didReset: true` means the persisted settings changed, and only then are the updated settings returned for the host to adopt.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsResetStaffModelSelectionOnceResult> resetStaffModelSelectionOnce(UserSettingsResetStaffModelSelectionOnceParams params) {
        return caller.invoke("user.settings.resetStaffModelSelectionOnce", params, UserSettingsResetStaffModelSelectionOnceResult.class);
    }

    /**
     * Reports the top-level keys in the user settings document that the runtime does not recognize, phrased for display to the user. This is a separate question from the warning `user.settings.loadWithWarning` returns: that one describes parts of the document the loader had to ignore to load the rest, whereas an unknown key parses cleanly and is simply not a setting, so a host that surfaces only the load warning silently accepts a misspelled key that will never take effect. Nothing is reported when the document is absent, unreadable, or failed to load, because an unknown-key list derived from a document the runtime could not read would be misleading rather than helpful. Inspection never modifies the document.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsUnknownKeysWarningResult> getUnknownKeysWarning(UserSettingsGetUnknownKeysWarningParams params) {
        return caller.invoke("user.settings.getUnknownKeysWarning", params, UserSettingsUnknownKeysWarningResult.class);
    }

    /**
     * Resolves the effective runtime settings for a host that is about to start a session, layering the given overrides over the settings documents, the environment and the platform defaults. A host cannot assemble this itself: the precedence between a user document, a repository document, the managed layers and the environment is the runtime's own rule, and a host that re-implements it drifts from the server it is configuring. The working directory, environment and platform are taken as arguments rather than read from the server process, so a host resolving settings for a directory other than its own gets that directory's answer. Nothing is written; this only computes.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<JsonNode> buildRuntime(UserSettingsBuildRuntimeParams params) {
        return caller.invoke("user.settings.buildRuntime", params, JsonNode.class);
    }

    /**
     * Runs the one-time migration that moves a staff account's log level to the current default, and records that it ran. Idempotent and self-gating: it takes a lock over the global-state file and re-reads the completion stamp inside it, so two hosts racing at startup cannot both migrate. Only `refreshed` can have changed the persisted settings, and the settings it returns are the document re-read after the write, so a host should adopt them rather than reloading separately and racing the next writer.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsMigrateStaffLogLevelOnceResult> migrateStaffLogLevelOnce(UserSettingsMigrateStaffLogLevelOnceParams params) {
        return caller.invoke("user.settings.migrateStaffLogLevelOnce", params, UserSettingsMigrateStaffLogLevelOnceResult.class);
    }

    /**
     * Runs the one-time cleanup that removes obsolete CLI-owned settings and legacy rubber-duck keys from the user's settings document, and reports what it did. Idempotent and safe to call on every startup: with nothing obsolete left it reports `unchanged` and writes nothing. The cleanup preserves every key it does not recognize, so a setting written by a newer host is never dropped by an older one, and a document that fails to parse is reported `invalid` and left untouched rather than rewritten from a partial read. The cleaned document is not returned: it is written before this replies, so `user.settings.load` reads it authoritatively. A host that surfaces unrecognized keys through `user.settings.unknownKeys` wants this first, because the keys this retires would otherwise be shown to the user as if they were misspelled settings.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsMigrateObsoleteOnDiskResult> migrateObsoleteOnDisk(UserSettingsMigrateObsoleteOnDiskParams params) {
        return caller.invoke("user.settings.migrateObsoleteOnDisk", params, UserSettingsMigrateObsoleteOnDiskResult.class);
    }

    /**
     * Lists every known user setting from settings.json, each with its effective value, its default, and whether it is at the default — so settings the user has never set still appear with their default value. Does not include repository- or enterprise-managed overrides that the runtime layers on top at session time.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<UserSettingsGetResult> get() {
        return caller.invoke("user.settings.get", java.util.Map.of(), UserSettingsGetResult.class);
    }

    /**
     * Resolves the on-disk locations of the settings files this runtime reads, so a consumer can show or open them. The user path is the same file `user.settings.get`, `user.settings.load`, and `user.settings.set` operate on. Supply `repoRoot` to also resolve that repository's checked-in and local settings paths. A path is reported whether or not the file exists.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsGetPathsResult> getPaths(UserSettingsGetPathsParams params) {
        return caller.invoke("user.settings.getPaths", params, UserSettingsGetPathsResult.class);
    }

    /**
     * Writes one or more user settings to settings.json, replacing each provided top-level key. A key whose value is null is removed.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    public CompletableFuture<Void> set(UserSettingsSetParams params) {
        return caller.invoke("user.settings.set", params, Void.class);
    }

    /**
     * Lists the top-level user-setting keys by who is allowed to govern them: `repo` keys a repository may override through `.github/copilot/settings.json`, `claudeRepo` keys a repository may override through a Claude-compatible config, and `managed` keys an enterprise policy may fix. The lists are static for a given runtime build and fail closed: a key absent from a list cannot be governed that way.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsGovernanceKeysResult> listGovernanceKeys() {
        return caller.invoke("user.settings.listGovernanceKeys", java.util.Map.of(), UserSettingsGovernanceKeysResult.class);
    }

    /**
     * Atomically edits one canonical user setting inside its settings-file lock. Reads, canonicalizes touched aliases, applies the edit, validates the resulting top-level setting and writes before releasing the lock. An explicit autoUpdatesChannel set completes the staff migration in the same transaction. Only a malformed existing file returns `invalid`; refused keys, invalid values and I/O errors reject the call.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SettingsEditResult> edit(UserSettingsEditParams params) {
        return caller.invoke("user.settings.edit", params, SettingsEditResult.class);
    }

    /**
     * Rewrites legacy and snake_case setting names in a settings object to their canonical spelling. A caller editing settings.json must normalize before writing: a legacy key left in place survives the write, is re-normalized on the next load, and silently shadows the canonical value the write just stored.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsNormalizeResult> normalize(UserSettingsNormalizeParams params) {
        return caller.invoke("user.settings.normalize", params, UserSettingsNormalizeResult.class);
    }

    /**
     * Diagnoses a set of already-loaded settings layers, reporting each key that will not take effect and why. Settings load tolerantly so that one bad key does not stop a session from starting; this method surfaces what was dropped so a caller can report it instead of silently ignoring the user's intent. Takes the layers as given rather than reading them, so a caller can diagnose exactly the content it is showing, including unsaved edits. Never echoes a setting's value, because a value may be a token or another secret. Marked internal because it exists to carry a CLI call site off the native addon, not to serve a consumer-facing need; it becomes public once an SDK consumer needs to inspect settings health itself.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsProblemsResult> diagnoseProblems(UserSettingsDiagnoseProblemsParams params) {
        return caller.invoke("user.settings.diagnoseProblems", params, UserSettingsProblemsResult.class);
    }

    /**
     * Validates one top-level setting subtree and writes just that key to settings.json as a single step. Doing both here closes the race a caller cannot close itself: between a separate preflight read and the write, the file may change. Rewriting only the touched key keeps legacy siblings out of settings.json. Answers a status rather than failing, so a validation problem and a write problem stay distinguishable.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsApplyTopKeyResult> applyTopKey(UserSettingsApplyTopKeyParams params) {
        return caller.invoke("user.settings.applyTopKey", params, UserSettingsApplyTopKeyResult.class);
    }

    /**
     * Replaces the user settings file's entire content in one write. A key absent from `settings` is removed, which is what distinguishes this from `user.settings.set`: that one merges, so it can never clear a key the caller has dropped. For a caller saving the result of a raw edit, a merge would silently resurrect every key the user deleted. Records the same one-time mutation marks a per-key write records, so a setting the user has now chosen explicitly stops being migrated for them.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> writeAll(UserSettingsWriteAllParams params) {
        return caller.invoke("user.settings.writeAll", params, Void.class);
    }

    /**
     * Merges a partial object into one top-level user setting, re-reading the stored value inside this runtime process's settings.json write lock so that concurrent writers in this process to different sub-fields of the same key do not clobber each other. Unlike `user.settings.set`, which replaces a key's whole value with what the caller last read, this keeps sub-fields the caller did not name when both the stored value and supplied value are objects; otherwise the supplied value replaces the stored value. Use it whenever two independent surfaces own different sub-fields of one setting. Returns the key's stored value after the write, in `value`.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsUpdateResult> update(UserSettingsUpdateParams params) {
        return caller.invoke("user.settings.update", params, UserSettingsUpdateResult.class);
    }

    /**
     * Reads settings.json as stored for a caller that is about to edit it. Unlike `user.settings.get`, this returns file values rather than per-key metadata and defaults. Reports `missing` for an absent file and `invalid` when the existing file will not parse. Retired config.json and extensionless files are not read.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsLoadForEditResult> loadForEdit() {
        return caller.invoke("user.settings.loadForEdit", java.util.Map.of(), UserSettingsLoadForEditResult.class);
    }

    /**
     * Validates one top-level setting subtree without writing it, reporting each problem with the path it applies to. Scoped to a single top-level key on purpose: validating the whole file would reject an edit whenever an unrelated pre-existing field is malformed, locking the user out of the very tool that could fix it.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<UserSettingsValidateResult> validate(UserSettingsValidateParams params) {
        return caller.invoke("user.settings.validate", params, UserSettingsValidateResult.class);
    }

}
