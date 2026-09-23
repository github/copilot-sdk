/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a response schema using the same annotation processor and type
 * mappings as {@code @CopilotTool}. Annotate a concrete record or class, then
 * pass its class to {@link CopilotSession#sendAndWait(String, Class)}. Custom
 * Jackson naming/converter schemas should instead be supplied explicitly via
 * {@link com.github.copilot.rpc.MessageOptions#setResponseSchema(java.util.Map)}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@CopilotExperimental
public @interface CopilotResponse {
}
