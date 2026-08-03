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
 * Disables an annotated test class or method when the E2E suite runs under the
 * in-process (FFI) transport, i.e. when {@code COPILOT_SDK_DEFAULT_CONNECTION}
 * is set to {@code inprocess}.
 *
 * <p>
 * Use this for tests that rely on per-client process settings the in-process
 * transport cannot honor — for example per-client environment variables, since
 * the in-process runtime shares the host process's single environment (see
 * {@link com.github.copilot.rpc.InProcessRuntimeConnection} and
 * <a href="https://github.com/github/copilot-sdk/issues/1934">issue #1934</a>).
 * </p>
 *
 * <p>
 * Mirrors {@code skip_inprocess(reason)} in the Rust E2E harness.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(SkipInProcess.Condition.class)
public @interface SkipInProcess {

    /**
     * Explains why the annotated test is incompatible with the in-process
     * transport.
     *
     * @return the skip reason
     */
    String value() default "Not supported under the in-process (FFI) transport";

    /**
     * JUnit 5 execution condition backing {@link SkipInProcess}.
     */
    final class Condition implements org.junit.jupiter.api.extension.ExecutionCondition {

        private static final String DEFAULT_CONNECTION_ENV_VAR = "COPILOT_SDK_DEFAULT_CONNECTION";

        @Override
        public org.junit.jupiter.api.extension.ConditionEvaluationResult evaluateExecutionCondition(
                org.junit.jupiter.api.extension.ExtensionContext context) {
            String envValue = System.getenv(DEFAULT_CONNECTION_ENV_VAR);
            if (!"inprocess".equalsIgnoreCase(envValue)) {
                return org.junit.jupiter.api.extension.ConditionEvaluationResult
                        .enabled("Not running under the in-process transport");
            }
            String reason = context.getElement().map(element -> element.getAnnotation(SkipInProcess.class))
                    .map(SkipInProcess::value).orElse("Not supported under the in-process (FFI) transport");
            return org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled(reason);
        }
    }
}
