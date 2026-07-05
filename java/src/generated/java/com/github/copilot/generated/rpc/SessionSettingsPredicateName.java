/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Rust-owned settings predicates exposed across the SDK boundary. Raw feature-flag names are intentionally not part of the contract.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SessionSettingsPredicateName {
    /** The {@code securityToolsEnabled} variant. */
    SECURITYTOOLSENABLED("securityToolsEnabled"),
    /** The {@code thirdPartySecurityPromptEnabled} variant. */
    THIRDPARTYSECURITYPROMPTENABLED("thirdPartySecurityPromptEnabled"),
    /** The {@code parallelValidationEnabled} variant. */
    PARALLELVALIDATIONENABLED("parallelValidationEnabled"),
    /** The {@code runtimeTimingTelemetryEnabled} variant. */
    RUNTIMETIMINGTELEMETRYENABLED("runtimeTimingTelemetryEnabled"),
    /** The {@code coAuthorHookEnabled} variant. */
    COAUTHORHOOKENABLED("coAuthorHookEnabled"),
    /** The {@code chronicleEnabled} variant. */
    CHRONICLEENABLED("chronicleEnabled"),
    /** The {@code contentExclusionSelfFetchEnabled} variant. */
    CONTENTEXCLUSIONSELFFETCHENABLED("contentExclusionSelfFetchEnabled"),
    /** The {@code capClaudeOpusTokenLimitsEnabled} variant. */
    CAPCLAUDEOPUSTOKENLIMITSENABLED("capClaudeOpusTokenLimitsEnabled"),
    /** The {@code codeReviewFeatureEnabled} variant. */
    CODEREVIEWFEATUREENABLED("codeReviewFeatureEnabled"),
    /** The {@code ccaUseTsAutofindEnabled} variant. */
    CCAUSETSAUTOFINDENABLED("ccaUseTsAutofindEnabled"),
    /** The {@code dependencyCheckerEnabled} variant. */
    DEPENDENCYCHECKERENABLED("dependencyCheckerEnabled"),
    /** The {@code dependabotCheckerEnabled} variant. */
    DEPENDABOTCHECKERENABLED("dependabotCheckerEnabled"),
    /** The {@code codeqlCheckerEnabled} variant. */
    CODEQLCHECKERENABLED("codeqlCheckerEnabled"),
    /** The {@code trivialChangeEnabled} variant. */
    TRIVIALCHANGEENABLED("trivialChangeEnabled"),
    /** The {@code trivialChangeSkipEnabled} variant. */
    TRIVIALCHANGESKIPENABLED("trivialChangeSkipEnabled"),
    /** The {@code trivialChangeEnabledForCodeReview} variant. */
    TRIVIALCHANGEENABLEDFORCODEREVIEW("trivialChangeEnabledForCodeReview"),
    /** The {@code trivialChangeSkipEnabledForCodeReview} variant. */
    TRIVIALCHANGESKIPENABLEDFORCODEREVIEW("trivialChangeSkipEnabledForCodeReview"),
    /** The {@code trivialChangeEnabledForTool} variant. */
    TRIVIALCHANGEENABLEDFORTOOL("trivialChangeEnabledForTool"),
    /** The {@code trivialChangeSkipEnabledForTool} variant. */
    TRIVIALCHANGESKIPENABLEDFORTOOL("trivialChangeSkipEnabledForTool");

    private final String value;
    SessionSettingsPredicateName(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SessionSettingsPredicateName fromValue(String value) {
        for (SessionSettingsPredicateName v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SessionSettingsPredicateName value: " + value);
    }
}
