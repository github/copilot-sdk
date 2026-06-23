/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.tool;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a parameter of a {@link CopilotTool}-annotated method to provide
 * metadata about the parameter that is sent to the model.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * &#64;CopilotTool("Search for issues")
 * public CompletableFuture&lt;String&gt; searchIssues(&#64;Param(value = "Search query", required = true) String query,
 * 		&#64;Param(value = "Max results", required = false, defaultValue = "10") int limit) {
 * 	// ...
 * }
 * </pre>
 *
 * @since 1.0.2
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Param {

    /** Parameter description (sent to the model). */
    String value() default "";

    /** Parameter name override. Defaults to the actual parameter name. */
    String name() default "";

    /** Whether this parameter is required. Default true. */
    boolean required() default true;

    /** Optional default value when the argument is omitted. */
    String defaultValue() default "";
}
