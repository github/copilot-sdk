/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

final class ResponseSchemas {
    private ResponseSchemas() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> forType(Class<?> type) {
        try {
            Class<?> metadata = Class.forName(type.getName() + "$$CopilotResponseMeta", true, type.getClassLoader());
            return (Map<String, Object>) metadata.getMethod("schema").invoke(null);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("No response schema for " + type.getName()
                    + ". Annotate the type with @CopilotResponse and enable CopilotResponseProcessor.", e);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Cannot load response schema for " + type.getName(), e);
        }
    }
}
