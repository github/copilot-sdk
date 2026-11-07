/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Which release asset to download, the credential to download it under, and the request id that makes the download cancellable.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = GitHubReleaseDownloadAssetParams.$NullableDeserializer.class)
record GitHubReleaseDownloadAssetParams(
    /** Request id from `gitHubRelease.allocateRequestId`. An id that was never registered, or one already used, is refused rather than silently running uncancellable. */
    @JsonProperty("requestId") Long requestId,
    /** The asset's `browser_download_url` from the release JSON: an HTTPS release download URL of a repository that `gitHubRelease.lookup` serves. */
    @JsonProperty("assetUrl") String assetUrl,
    /** The credential the download runs under, carried opaquely because its shape is the host's own and the runtime only resolves a token from it. No credential travels: this selects one the runtime already holds. Omitted when the caller has no credential, which still succeeds against the public release repository at a lower rate limit. */
    @JsonProperty("authInfo") Object authInfo,
    /** Expected SHA-256 of the asset: 64 hex digits in either case, or the release asset's `digest` value as is (`sha256:` and the hex digits). When given and the downloaded bytes differ, the file is removed and the result is `digest-mismatch`. */
    @JsonProperty("expectedSha256") String expectedSha256,
    /** Snapshot request id from this connection. The download retains that snapshot's original credential source instead of authInfo. Missing, foreign, canceled, or revoked references fail without fallback. Omission preserves ordinary downloads. The download still uses its own requestId for progress and cancellation. */
    @JsonProperty("snapshotRequestId") Long snapshotRequestId,
    @com.fasterxml.jackson.annotation.JsonIgnore java.util.Set<String> $explicitNulls
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitHubReleaseDownloadAssetParams { $explicitNulls = $explicitNulls == null ? java.util.Set.of() : java.util.Set.copyOf($explicitNulls); }

    /**
     * Creates a record with legacy null-as-omission semantics.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitHubReleaseDownloadAssetParams(Long requestId, String assetUrl, Object authInfo, String expectedSha256, Long snapshotRequestId) {
        this(requestId, assetUrl, authInfo, expectedSha256, snapshotRequestId, java.util.Set.of());
    }

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private Long requestId;
        private String assetUrl;
        private Object authInfo;
        private boolean $presence2;
        private String expectedSha256;
        private Long snapshotRequestId;

        /**
         * Sets the {@code requestId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requestId(Long value) {
            this.requestId = value;
            return this;
        }

        /**
         * Sets the {@code assetUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder assetUrl(String value) {
            this.assetUrl = value;
            return this;
        }

        /**
         * Sets the {@code authInfo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authInfo(Object value) {
            this.authInfo = value;
            this.$presence2 = true;
            return this;
        }

        /**
         * Sets the {@code expectedSha256} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder expectedSha256(String value) {
            this.expectedSha256 = value;
            return this;
        }

        /**
         * Sets the {@code snapshotRequestId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder snapshotRequestId(Long value) {
            this.snapshotRequestId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubReleaseDownloadAssetParams build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (assetUrl == null) throw new IllegalStateException("Missing required property: assetUrl");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if ($presence2 && authInfo == null) $explicitNulls.add("authInfo");
            return new GitHubReleaseDownloadAssetParams(requestId, assetUrl, authInfo, expectedSha256, snapshotRequestId, $explicitNulls);
        }
    }

    @com.fasterxml.jackson.annotation.JsonAnyGetter
    @JsonInclude(value = JsonInclude.Include.ALWAYS, content = JsonInclude.Include.ALWAYS)
    public java.util.Map<String, Object> $nullableProperties() {
        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        if ($explicitNulls.contains("authInfo") && authInfo == null) values.put("authInfo", null);
        return values;
    }

    public static final class $NullableDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<GitHubReleaseDownloadAssetParams> {
        @Override
        public GitHubReleaseDownloadAssetParams deserialize(com.fasterxml.jackson.core.JsonParser parser,
                com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            com.fasterxml.jackson.databind.JsonNode tree = context.readTree(parser);
            if (!tree.isObject()) return context.reportInputMismatch(GitHubReleaseDownloadAssetParams.class, "Expected a JSON object");
            java.util.Set<String> $explicitNulls = new java.util.LinkedHashSet<>();
            if (tree.has("authInfo") && tree.get("authInfo").isNull()) $explicitNulls.add("authInfo");
            return new GitHubReleaseDownloadAssetParams(readProperty(context, tree, "requestId", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), readProperty(context, tree, "assetUrl", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "authInfo", new com.fasterxml.jackson.core.type.TypeReference<Object>() {}), readProperty(context, tree, "expectedSha256", new com.fasterxml.jackson.core.type.TypeReference<String>() {}), readProperty(context, tree, "snapshotRequestId", new com.fasterxml.jackson.core.type.TypeReference<Long>() {}), $explicitNulls);
        }

        private static <T> T readProperty(com.fasterxml.jackson.databind.DeserializationContext context,
                com.fasterxml.jackson.databind.JsonNode tree, String name,
                com.fasterxml.jackson.core.type.TypeReference<T> type) throws java.io.IOException {
            if (!tree.has(name)) return null;
            return context.readTreeAsValue(tree.get(name), context.getTypeFactory().constructType(type));
        }
    }
}
