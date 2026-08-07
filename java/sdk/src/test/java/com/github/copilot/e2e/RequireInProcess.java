/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.e2e;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Enables an annotated test class or method only when the E2E suite runs under
 * the in-process (FFI) transport, i.e. when
 * {@code COPILOT_SDK_DEFAULT_CONNECTION} is set to {@code inprocess}.
 *
 * <p>
 * Use this for tests that require the real {@code runtime.node} native library
 * to be present on the classpath, which only the {@code -Pinprocess} Maven
 * profile guarantees (see {@link InProcessTransportIT}). Without this profile,
 * standard {@code mvn verify} runs would fail with a
 * {@code FileNotFoundException} because the classifier JAR providing
 * {@code runtime.node} is not on the classpath.
 * </p>
 *
 * <p>
 * The inverse of {@link SkipInProcess}.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(RequireInProcess.Condition.class)
public @interface RequireInProcess {

    /**
     * Explains why the annotated test requires the in-process transport.
     *
     * @return the skip reason used when the in-process transport is not active
     */
    String value() default "Requires the -Pinprocess Maven profile";

    /**
     * JUnit 5 execution condition backing {@link RequireInProcess}.
     */
    public static final class Condition implements org.junit.jupiter.api.extension.ExecutionCondition {

        private static final String DEFAULT_CONNECTION_ENV_VAR = "COPILOT_SDK_DEFAULT_CONNECTION";

        @Override
        public org.junit.jupiter.api.extension.ConditionEvaluationResult evaluateExecutionCondition(
                org.junit.jupiter.api.extension.ExtensionContext context) {
            String envValue = System.getenv(DEFAULT_CONNECTION_ENV_VAR);
            if ("inprocess".equalsIgnoreCase(envValue)) {
                return org.junit.jupiter.api.extension.ConditionEvaluationResult
                        .enabled("Running under the in-process transport");
            }
            String reason = context.getElement().map(element -> element.getAnnotation(RequireInProcess.class))
                    .map(RequireInProcess::value).orElse("Requires the -Pinprocess Maven profile");
            return org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled(reason);
        }
    }
}
