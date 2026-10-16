/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code repo.settings} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerRepoSettingsApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerRepoSettingsApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Atomically edits one allowed top-level repository setting. Reads the raw file, preserves unrelated keys, applies one scalar or list edit, validates the result and writes under one file lock, so concurrent sibling or list edits are not lost. A malformed or unsafe existing file returns `invalid` without a write; rejected keys, invalid values and I/O errors reject the call.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<SettingsEditResult> edit(RepoSettingsEditParams params) {
        return caller.invoke("repo.settings.edit", params, SettingsEditResult.class);
    }

    /**
     * Reads one repository settings file without settings normalization or layering. The existing JSONC parser accepts comments and trailing commas and applies its prototype-safety rules. Missing or blank files return an empty valid config. Invalid files return an empty config with invalid=true; other read failures reject.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<RepoSettingsReadRawResult> readRaw(RepoSettingsReadRawParams params) {
        return caller.invoke("repo.settings.readRaw", params, RepoSettingsReadRawResult.class);
    }

    /**
     * Writes or removes one allowed top-level repository setting through the existing validator and file lock. Preserves unrelated stored keys. Invalid keys, values, existing documents, and I/O errors reject without replacing the file. Deletion requires delete=true; otherwise value must be present.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> writeKey(RepoSettingsWriteKeyParams params) {
        return caller.invoke("repo.settings.writeKey", params, Void.class);
    }

    /**
     * Replaces the repository-overridable settings in one locked write. Allowed keys present in settings are validated and written; allowed keys absent from settings are removed. Other input keys are ignored, and unknown keys already stored in the file remain unchanged. Invalid values or an invalid existing file reject without a partial write.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<Void> writeAll(RepoSettingsWriteAllParams params) {
        return caller.invoke("repo.settings.writeAll", params, Void.class);
    }

}
