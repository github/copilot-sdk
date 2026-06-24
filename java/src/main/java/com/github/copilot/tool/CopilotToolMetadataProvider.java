/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.tool;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.CopilotExperimental;
import com.github.copilot.rpc.ToolDefinition;

/**
 * Contract for classes that provide {@link ToolDefinition} metadata for
 * {@code @CopilotTool}-annotated methods.
 *
 * <p>
 * The {@link CopilotToolProcessor} annotation processor generates an
 * implementation of this interface as a {@code $$CopilotToolMeta} companion
 * class. Users may also implement this interface directly for full manual
 * control over tool registration without using annotation processing.
 *
 * @param <T>
 *            the tool class whose methods are described by this provider
 * @since 1.0.2
 */
@CopilotExperimental
public interface CopilotToolMetadataProvider<T> {

    /**
     * Returns tool definitions for the given instance.
     *
     * @param instance
     *            the object containing tool methods, or {@code null} for static
     *            methods
     * @param mapper
     *            the SDK-configured {@link ObjectMapper} for argument
     *            deserialization
     * @return list of tool definitions with working invocation handlers
     */
    List<ToolDefinition> definitions(T instance, ObjectMapper mapper);
}
