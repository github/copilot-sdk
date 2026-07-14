/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.copilot.CopilotExperimental;
import com.github.copilot.rpc.ToolDefer;

/**
 * Marks a method as a Copilot tool. The annotated method will be exposed to the
 * model as a callable tool during a session.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * &#64;CopilotTool("Get weather for a location")
 * public CompletableFuture&lt;String&gt; getWeather(
 * 		&#64;CopilotToolParam(value = "City name", required = true) String location) {
 * 	return CompletableFuture.completedFuture("Sunny in " + location);
 * }
 * </pre>
 *
 * @since 1.0.2
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@CopilotExperimental
public @interface CopilotTool {

    /** Tool description (sent to the model). */
    String value();

    /** Tool name. Defaults to method name converted to snake_case. */
    String name() default "";

    /** Whether this tool overrides a built-in tool. */
    boolean overridesBuiltInTool() default false;

    /** Whether to skip permission checks. */
    boolean skipPermission() default false;

    /** Defer configuration for this tool. */
    ToolDefer defer() default ToolDefer.NONE;

    /**
     * Opaque, host-defined metadata for this tool. Keys are namespaced and not part
     * of the stable public API; specific keys may be recognized to inform
     * host-specific behavior.
     *
     * <p>
     * Because annotation members cannot express arbitrary maps, this uses a
     * deliberately shallow representation: each {@link MetadataEntry} maps a string
     * key to a single {@link MetadataValue} that is either a boolean, a string, or
     * a one-level map of named boolean {@link MetadataFlag flags}. Numbers, arrays,
     * and deeper nesting are not supported here; use the programmatic
     * {@code ToolDefinition.createWithMetadata(...)} /
     * {@code ToolDefinition.metadata(...)} API for richer values.
     *
     * <p>
     * Example emitted shape:
     *
     * <pre>
     * Map.of("github.com/copilot:safeForTelemetry", Map.of("name", true, "inputsNames", false))
     * </pre>
     */
    MetadataEntry[] metadata() default {};

    /**
     * A single metadata key/value pair. Used only as a member value of
     * {@link CopilotTool#metadata()}.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface MetadataEntry {

        /** The namespaced metadata key. */
        String key();

        /** The value associated with {@link #key()}. */
        MetadataValue value();
    }

    /**
     * A metadata value. Exactly one representation is intended per value: a map of
     * named boolean {@link #flags()} (when non-empty), otherwise a {@link #str()}
     * (when non-empty), otherwise a {@link #bool()}.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface MetadataValue {

        /**
         * Scalar boolean value. Used when {@link #flags()} and {@link #str()} are
         * unset.
         */
        boolean bool() default false;

        /**
         * Scalar string value. Used when {@link #flags()} is empty and this is
         * non-empty.
         */
        String str() default "";

        /**
         * Object-like value: a one-level map of named boolean flags. Takes precedence
         * when non-empty.
         */
        MetadataFlag[] flags() default {};
    }

    /**
     * A single named boolean flag within a {@link MetadataValue#flags()} map.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface MetadataFlag {

        /** The flag name (map key). */
        String name();

        /** The flag value. */
        boolean value();
    }
}
