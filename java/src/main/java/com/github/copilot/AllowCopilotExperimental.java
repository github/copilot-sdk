/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opts a declaration into using {@link CopilotExperimental} APIs.
 *
 * <p>
 * Apply this annotation to a type to allow declaration-level references to
 * experimental APIs anywhere within that type, or apply it to a method or
 * constructor to allow experimental API usage in that executable's signature.
 * This is a code-level alternative to the compiler option
 * {@code -Acopilot.experimental.allowed=true}.
 *
 * <p>
 * This opt-in has the same declaration-level scope as the processor itself. It
 * does not affect expression-only usages inside method bodies that are not
 * visible to standard JSR 269 annotation processing.
 *
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface AllowCopilotExperimental {
}
