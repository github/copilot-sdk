/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.Generated;

/**
 * Optional evaluations and readiness barriers for one feature snapshot.
 * <p>
 * Required inputs are constructor arguments. Optional inputs have fluent setters.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SessionFeatureFlagsReadRequest {

    /** Wait for the current assignment. Omission does not wait. */
    @JsonProperty("waitForExpResponse")
    private Boolean waitForExpResponse;

    /** Feature names evaluated with their canonical experiment mappings. */
    @JsonProperty("flags")
    private List<String> flags;

    /** Explicit experiment/static queries, in result order. */
    @JsonProperty("overrides")
    private List<FeatureFlagExpOverrideQuery> overrides;

    /** Wait for a different revision or service disposal using the existing change notification. */
    @JsonProperty("afterRevision")
    private Long afterRevision;

    /** Return only the redacted UI projection, without assignment data or requested evaluations. */
    @JsonProperty("displayOnly")
    private Boolean displayOnly;

    /** Original display generation; a replaced service returns an unavailable projection. */
    @JsonProperty("serviceId")
    private String serviceId;

    /**
     * Creates a request with its required inputs.
     */
    public SessionFeatureFlagsReadRequest() {
    }

    /**
     * Returns the {@code waitForExpResponse} property.
     *
     * @return Wait for the current assignment. Omission does not wait.
     */
    public Boolean getWaitForExpResponse() {
        return waitForExpResponse;
    }

    /**
     * Returns the {@code flags} property.
     *
     * @return Feature names evaluated with their canonical experiment mappings.
     */
    public List<String> getFlags() {
        return flags;
    }

    /**
     * Returns the {@code overrides} property.
     *
     * @return Explicit experiment/static queries, in result order.
     */
    public List<FeatureFlagExpOverrideQuery> getOverrides() {
        return overrides;
    }

    /**
     * Returns the {@code afterRevision} property.
     *
     * @return Wait for a different revision or service disposal using the existing change notification.
     */
    public Long getAfterRevision() {
        return afterRevision;
    }

    /**
     * Returns the {@code displayOnly} property.
     *
     * @return Return only the redacted UI projection, without assignment data or requested evaluations.
     */
    public Boolean getDisplayOnly() {
        return displayOnly;
    }

    /**
     * Returns the {@code serviceId} property.
     *
     * @return Original display generation; a replaced service returns an unavailable projection.
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * Sets the {@code waitForExpResponse} property.
     *
     * @param value Wait for the current assignment. Omission does not wait.
     * @return this request
     */
    public SessionFeatureFlagsReadRequest setWaitForExpResponse(Boolean value) {
        this.waitForExpResponse = value;
        return this;
    }

    /**
     * Sets the {@code flags} property.
     *
     * @param value Feature names evaluated with their canonical experiment mappings.
     * @return this request
     */
    public SessionFeatureFlagsReadRequest setFlags(List<String> value) {
        this.flags = value;
        return this;
    }

    /**
     * Sets the {@code overrides} property.
     *
     * @param value Explicit experiment/static queries, in result order.
     * @return this request
     */
    public SessionFeatureFlagsReadRequest setOverrides(List<FeatureFlagExpOverrideQuery> value) {
        this.overrides = value;
        return this;
    }

    /**
     * Sets the {@code afterRevision} property.
     *
     * @param value Wait for a different revision or service disposal using the existing change notification.
     * @return this request
     */
    public SessionFeatureFlagsReadRequest setAfterRevision(Long value) {
        this.afterRevision = value;
        return this;
    }

    /**
     * Sets the {@code displayOnly} property.
     *
     * @param value Return only the redacted UI projection, without assignment data or requested evaluations.
     * @return this request
     */
    public SessionFeatureFlagsReadRequest setDisplayOnly(Boolean value) {
        this.displayOnly = value;
        return this;
    }

    /**
     * Sets the {@code serviceId} property.
     *
     * @param value Original display generation; a replaced service returns an unavailable projection.
     * @return this request
     */
    public SessionFeatureFlagsReadRequest setServiceId(String value) {
        this.serviceId = value;
        return this;
    }
}
