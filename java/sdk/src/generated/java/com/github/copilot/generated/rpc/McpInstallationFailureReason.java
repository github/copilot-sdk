/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Bounded refusal categories, without echoing handles, credentials or configuration.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum McpInstallationFailureReason {
    /** The {@code invalid-request} variant. */
    INVALID_REQUEST("invalid-request"),
    /** The {@code operation-limit} variant. */
    OPERATION_LIMIT("operation-limit"),
    /** The {@code cancelled} variant. */
    CANCELLED("cancelled"),
    /** The {@code capability-required} variant. */
    CAPABILITY_REQUIRED("capability-required"),
    /** The {@code confirmation-unavailable} variant. */
    CONFIRMATION_UNAVAILABLE("confirmation-unavailable"),
    /** The {@code confirmation-invalid} variant. */
    CONFIRMATION_INVALID("confirmation-invalid"),
    /** The {@code policy-context-unavailable} variant. */
    POLICY_CONTEXT_UNAVAILABLE("policy-context-unavailable"),
    /** The {@code policy-changed} variant. */
    POLICY_CHANGED("policy-changed"),
    /** The {@code policy-denied} variant. */
    POLICY_DENIED("policy-denied"),
    /** The {@code configuration-changed} variant. */
    CONFIGURATION_CHANGED("configuration-changed"),
    /** The {@code configuration-modified} variant. */
    CONFIGURATION_MODIFIED("configuration-modified"),
    /** The {@code resource-not-found} variant. */
    RESOURCE_NOT_FOUND("resource-not-found"),
    /** The {@code plan-expired} variant. */
    PLAN_EXPIRED("plan-expired"),
    /** The {@code plan-replayed} variant. */
    PLAN_REPLAYED("plan-replayed"),
    /** The {@code foreign-runtime} variant. */
    FOREIGN_RUNTIME("foreign-runtime"),
    /** The {@code replan-required} variant. */
    REPLAN_REQUIRED("replan-required"),
    /** The {@code source-revalidation-unavailable} variant. */
    SOURCE_REVALIDATION_UNAVAILABLE("source-revalidation-unavailable"),
    /** The {@code source-changed} variant. */
    SOURCE_CHANGED("source-changed"),
    /** The {@code source-unavailable} variant. */
    SOURCE_UNAVAILABLE("source-unavailable"),
    /** The {@code registry-unavailable} variant. */
    REGISTRY_UNAVAILABLE("registry-unavailable"),
    /** The {@code secret-store-unavailable} variant. */
    SECRET_STORE_UNAVAILABLE("secret-store-unavailable"),
    /** The {@code lifecycle-unavailable} variant. */
    LIFECYCLE_UNAVAILABLE("lifecycle-unavailable"),
    /** The {@code write-failed} variant. */
    WRITE_FAILED("write-failed");

    private final String value;
    McpInstallationFailureReason(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static McpInstallationFailureReason fromValue(String value) {
        for (McpInstallationFailureReason v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown McpInstallationFailureReason value: " + value);
    }
}
