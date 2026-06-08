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
 * Marks a type or method as experimental. Experimental APIs may change or be
 * removed in future versions without notice.
 *
 * <p>By default, referencing an experimental API from consumer code causes a
 * compile-time error. To opt in, pass the compiler option:
 * <pre>-Acopilot.experimental.allowed=true</pre>
 *
 * @since 1.0.0
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface CopilotExperimental {
}
