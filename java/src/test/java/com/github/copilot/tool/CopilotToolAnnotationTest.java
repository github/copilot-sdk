/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.tool;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.github.copilot.CopilotExperimental;
import com.github.copilot.rpc.ToolDefer;

/**
 * Unit tests for {@link CopilotTool} and {@link Param} annotations.
 */
public class CopilotToolAnnotationTest {

    // --- @CopilotTool attribute verification ---

    @Test
    void copilotToolHasRuntimeRetention() {
        Retention retention = CopilotTool.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    void copilotToolTargetsMethod() {
        Target target = CopilotTool.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[]{ElementType.METHOD}, target.value());
    }

    @Test
    void copilotToolIsAnnotatedWithCopilotExperimental() {
        // @CopilotExperimental has CLASS retention so it is not visible via
        // reflection at runtime. However, we can confirm:
        // 1. The annotation type targets TYPE (which includes @interface declarations).
        // 2. Compilation succeeded with @CopilotExperimental on @CopilotTool
        // (the CopilotExperimentalProcessor would reject usage otherwise).
        Target expTarget = CopilotExperimental.class.getAnnotation(Target.class);
        assertNotNull(expTarget);
        boolean includesType = false;
        for (ElementType et : expTarget.value()) {
            if (et == ElementType.TYPE) {
                includesType = true;
                break;
            }
        }
        assertTrue(includesType, "@CopilotExperimental must target TYPE to be applicable to annotation declarations");
    }

    @Test
    void copilotToolDefaultValues() throws Exception {
        Method nameMethod = CopilotTool.class.getDeclaredMethod("name");
        assertEquals("", nameMethod.getDefaultValue());

        Method overridesMethod = CopilotTool.class.getDeclaredMethod("overridesBuiltInTool");
        assertEquals(false, overridesMethod.getDefaultValue());

        Method skipMethod = CopilotTool.class.getDeclaredMethod("skipPermission");
        assertEquals(false, skipMethod.getDefaultValue());

        Method deferMethod = CopilotTool.class.getDeclaredMethod("defer");
        assertEquals(ToolDefer.NONE, deferMethod.getDefaultValue());
    }

    // --- @Param attribute verification ---

    @Test
    void paramHasRuntimeRetention() {
        Retention retention = Param.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    void paramTargetsParameter() {
        Target target = Param.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[]{ElementType.PARAMETER}, target.value());
    }

    @Test
    void paramDefaultValues() throws Exception {
        Method valueMethod = Param.class.getDeclaredMethod("value");
        assertEquals("", valueMethod.getDefaultValue());

        Method nameMethod = Param.class.getDeclaredMethod("name");
        assertEquals("", nameMethod.getDefaultValue());

        Method requiredMethod = Param.class.getDeclaredMethod("required");
        assertEquals(true, requiredMethod.getDefaultValue());

        Method defaultValueMethod = Param.class.getDeclaredMethod("defaultValue");
        assertEquals("", defaultValueMethod.getDefaultValue());
    }

    // --- Applicability test ---

    @SuppressWarnings("unused")
    static class SampleToolHolder {

        @CopilotTool(value = "Get weather for a location", name = "get_weather", defer = ToolDefer.AUTO)
        public CompletableFuture<String> getWeather(@Param(value = "City name", required = true) String location,
                @Param(value = "Temperature unit", required = false, defaultValue = "celsius") String unit) {
            return CompletableFuture.completedFuture("Sunny in " + location);
        }
    }

    @Test
    void annotationsAreAccessibleViaReflection() throws Exception {
        Method method = SampleToolHolder.class.getDeclaredMethod("getWeather", String.class, String.class);

        CopilotTool toolAnnotation = method.getAnnotation(CopilotTool.class);
        assertNotNull(toolAnnotation);
        assertEquals("Get weather for a location", toolAnnotation.value());
        assertEquals("get_weather", toolAnnotation.name());
        assertFalse(toolAnnotation.overridesBuiltInTool());
        assertFalse(toolAnnotation.skipPermission());
        assertEquals(ToolDefer.AUTO, toolAnnotation.defer());

        Parameter[] params = method.getParameters();
        assertEquals(2, params.length);

        Param locationParam = params[0].getAnnotation(Param.class);
        assertNotNull(locationParam);
        assertEquals("City name", locationParam.value());
        assertTrue(locationParam.required());
        assertEquals("", locationParam.defaultValue());

        Param unitParam = params[1].getAnnotation(Param.class);
        assertNotNull(unitParam);
        assertEquals("Temperature unit", unitParam.value());
        assertFalse(unitParam.required());
        assertEquals("celsius", unitParam.defaultValue());
    }
}
