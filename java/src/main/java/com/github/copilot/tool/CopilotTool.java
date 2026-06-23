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
 * public CompletableFuture&lt;String&gt; getWeather(&#64;Param(value = "City name", required = true) String location) {
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
}
