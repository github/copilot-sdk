/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Bounded refusal categories for verified Skill installation management.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SkillInstallationFailureReason {
    /** The {@code feature-disabled} variant. */
    FEATURE_DISABLED("feature-disabled"),
    /** The {@code invalid-request} variant. */
    INVALID_REQUEST("invalid-request"),
    /** The {@code operation-limit} variant. */
    OPERATION_LIMIT("operation-limit"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled"),
    /** The {@code confirmation-unavailable} variant. */
    CONFIRMATION_UNAVAILABLE("confirmation-unavailable"),
    /** The {@code confirmation-invalid} variant. */
    CONFIRMATION_INVALID("confirmation-invalid"),
    /** The {@code policy-context-unavailable} variant. */
    POLICY_CONTEXT_UNAVAILABLE("policy-context-unavailable"),
    /** The {@code policy-changed} variant. */
    POLICY_CHANGED("policy-changed"),
    /** The {@code resource-not-found} variant. */
    RESOURCE_NOT_FOUND("resource-not-found"),
    /** The {@code plan-expired} variant. */
    PLAN_EXPIRED("plan-expired"),
    /** The {@code plan-replayed} variant. */
    PLAN_REPLAYED("plan-replayed"),
    /** The {@code foreign-runtime} variant. */
    FOREIGN_RUNTIME("foreign-runtime"),
    /** The {@code wrong-kind} variant. */
    WRONG_KIND("wrong-kind"),
    /** The {@code search-mismatch} variant. */
    SEARCH_MISMATCH("search-mismatch"),
    /** The {@code expired} variant. */
    EXPIRED("expired"),
    /** The {@code invalid-candidate} variant. */
    INVALID_CANDIDATE("invalid-candidate"),
    /** The {@code descriptor-unavailable} variant. */
    DESCRIPTOR_UNAVAILABLE("descriptor-unavailable"),
    /** The {@code descriptor-invalid} variant. */
    DESCRIPTOR_INVALID("descriptor-invalid"),
    /** The {@code entrypoint-unavailable} variant. */
    ENTRYPOINT_UNAVAILABLE("entrypoint-unavailable"),
    /** The {@code invalid-skill} variant. */
    INVALID_SKILL("invalid-skill"),
    /** The {@code payload-unavailable} variant. */
    PAYLOAD_UNAVAILABLE("payload-unavailable"),
    /** The {@code payload-mismatch} variant. */
    PAYLOAD_MISMATCH("payload-mismatch"),
    /** The {@code source-changed} variant. */
    SOURCE_CHANGED("source-changed"),
    /** The {@code lifecycle-unavailable} variant. */
    LIFECYCLE_UNAVAILABLE("lifecycle-unavailable"),
    /** The {@code recovery-required} variant. */
    RECOVERY_REQUIRED("recovery-required"),
    /** The {@code review-too-large} variant. */
    REVIEW_TOO_LARGE("review-too-large"),
    /** The {@code busy} variant. */
    BUSY("busy"),
    /** The {@code already-installed} variant. */
    ALREADY_INSTALLED("already-installed"),
    /** The {@code configuration-modified} variant. */
    CONFIGURATION_MODIFIED("configuration-modified"),
    /** The {@code write-failed} variant. */
    WRITE_FAILED("write-failed");

    private final String value;
    SkillInstallationFailureReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SkillInstallationFailureReason fromValue(String value) {
        for (SkillInstallationFailureReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SkillInstallationFailureReason value: " + value);
    }
}
