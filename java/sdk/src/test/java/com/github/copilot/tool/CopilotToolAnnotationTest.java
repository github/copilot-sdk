/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.tool;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.github.copilot.CopilotExperimental;
import com.github.copilot.rpc.ToolDefer;

/**
 * Unit tests for {@link CopilotTool} and {@link CopilotToolParam} annotations.
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
    void copilotExperimentalTargetsTypeForAnnotationDeclarations() {
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
    void copilotToolDeclaresCopilotExperimentalInClassFile() throws Exception {
        String classFileResourcePath = "/" + CopilotTool.class.getName().replace('.', '/') + ".class";
        try (InputStream classFile = CopilotTool.class.getResourceAsStream(classFileResourcePath)) {
            assertNotNull(classFile, "CopilotTool class file must be readable as a resource");
            String classFileText = new String(classFile.readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(classFileText.contains("com/github/copilot/CopilotExperimental"));
        }
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

    // --- @CopilotToolParam attribute verification ---

    @Test
    void paramHasRuntimeRetention() {
        Retention retention = CopilotToolParam.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    void paramTargetsParameter() {
        Target target = CopilotToolParam.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[]{ElementType.PARAMETER}, target.value());
    }

    @Test
    void paramDefaultValues() throws Exception {
        Method valueMethod = CopilotToolParam.class.getDeclaredMethod("value");
        assertEquals("", valueMethod.getDefaultValue());

        Method nameMethod = CopilotToolParam.class.getDeclaredMethod("name");
        assertEquals("", nameMethod.getDefaultValue());

        Method requiredMethod = CopilotToolParam.class.getDeclaredMethod("required");
        assertEquals(true, requiredMethod.getDefaultValue());

        Method defaultValueMethod = CopilotToolParam.class.getDeclaredMethod("defaultValue");
        assertEquals("", defaultValueMethod.getDefaultValue());
    }

    // --- Applicability test ---

    @SuppressWarnings("unused")
    static class SampleToolHolder {

        @CopilotTool(value = "Get weather for a location", name = "get_weather", defer = ToolDefer.AUTO)
        public CompletableFuture<String> getWeather(
                @CopilotToolParam(value = "City name", required = true) String location,
                @CopilotToolParam(value = "Temperature unit", required = false, defaultValue = "celsius") String unit) {
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

        CopilotToolParam locationParam = params[0].getAnnotation(CopilotToolParam.class);
        assertNotNull(locationParam);
        assertEquals("City name", locationParam.value());
        assertTrue(locationParam.required());
        assertEquals("", locationParam.defaultValue());

        CopilotToolParam unitParam = params[1].getAnnotation(CopilotToolParam.class);
        assertNotNull(unitParam);
        assertEquals("Temperature unit", unitParam.value());
        assertFalse(unitParam.required());
        assertEquals("celsius", unitParam.defaultValue());
    }
}
