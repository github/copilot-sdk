/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Configuration for infinite sessions with automatic context compaction and
 * workspace persistence.
 * <p>
 * When enabled, sessions automatically manage context window limits through
 * background compaction and persist state to a workspace directory.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var infiniteConfig = new InfiniteSessionConfig().setEnabled(true).setBackgroundCompactionThreshold(0.80)
 * 		.setBufferExhaustionThreshold(0.95);
 *
 * var config = new SessionConfig().setInfiniteSessions(infiniteConfig);
 *
 * var session = client.createSession(config).get();
 * }</pre>
 *
 * @see SessionConfig#setInfiniteSessions(InfiniteSessionConfig)
 * @since 1.0.2
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InfiniteSessionConfig {

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("backgroundCompactionThreshold")
    private Double backgroundCompactionThreshold;

    @JsonProperty("bufferExhaustionThreshold")
    private Double bufferExhaustionThreshold;

    /**
     * Gets whether infinite sessions are enabled.
     *
     * @return an {@link Optional} containing the boolean value, or empty to use
     *         default (true)
     */
    @JsonIgnore
    public Optional<Boolean> getEnabled() {
        return Optional.ofNullable(enabled);
    }

    /**
     * Sets whether infinite sessions are enabled.
     * <p>
     * Default: true
     *
     * @param enabled
     *            {@code true} to enable infinite sessions
     * @return this config instance for method chaining
     */
    public InfiniteSessionConfig setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Clears the enabled setting, reverting to the default behavior.
     *
     * @return this instance for method chaining
     */
    public InfiniteSessionConfig clearEnabled() {
        this.enabled = null;
        return this;
    }

    /**
     * Gets the background compaction threshold.
     *
     * @return an {@link OptionalDouble} containing the threshold (0.0-1.0), or
     *         empty to use default
     */
    @JsonIgnore
    public OptionalDouble getBackgroundCompactionThreshold() {
        return backgroundCompactionThreshold == null
                ? OptionalDouble.empty()
                : OptionalDouble.of(backgroundCompactionThreshold);
    }

    /**
     * Sets the context utilization threshold at which background compaction starts.
     * <p>
     * Compaction runs asynchronously, allowing the session to continue processing.
     * Default: 0.80
     *
     * @param backgroundCompactionThreshold
     *            the threshold (0.0-1.0)
     * @return this config instance for method chaining
     */
    public InfiniteSessionConfig setBackgroundCompactionThreshold(double backgroundCompactionThreshold) {
        this.backgroundCompactionThreshold = backgroundCompactionThreshold;
        return this;
    }

    /**
     * Clears the backgroundCompactionThreshold setting, reverting to the default
     * behavior.
     *
     * @return this instance for method chaining
     */
    public InfiniteSessionConfig clearBackgroundCompactionThreshold() {
        this.backgroundCompactionThreshold = null;
        return this;
    }

    /**
     * Gets the buffer exhaustion threshold.
     *
     * @return an {@link OptionalDouble} containing the threshold (0.0-1.0), or
     *         empty to use default
     */
    @JsonIgnore
    public OptionalDouble getBufferExhaustionThreshold() {
        return bufferExhaustionThreshold == null
                ? OptionalDouble.empty()
                : OptionalDouble.of(bufferExhaustionThreshold);
    }

    /**
     * Sets the context utilization threshold at which the session blocks until
     * compaction completes.
     * <p>
     * This prevents context overflow when compaction hasn't finished in time.
     * Default: 0.95
     *
     * @param bufferExhaustionThreshold
     *            the threshold (0.0-1.0)
     * @return this config instance for method chaining
     */
    public InfiniteSessionConfig setBufferExhaustionThreshold(double bufferExhaustionThreshold) {
        this.bufferExhaustionThreshold = bufferExhaustionThreshold;
        return this;
    }

    /**
     * Clears the bufferExhaustionThreshold setting, reverting to the default
     * behavior.
     *
     * @return this instance for method chaining
     */
    public InfiniteSessionConfig clearBufferExhaustionThreshold() {
        this.bufferExhaustionThreshold = null;
        return this;
    }

}
