/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import type { ResumeSessionConfig, SessionConfig } from "../src";
import { parseTestBackend, withTestBackend } from "./e2e/harness/testBackend";

describe("E2E backend selection", () => {
    it.each([
        [undefined, "capi"],
        ["", "capi"],
        ["capi", "capi"],
        [" ANTHROPIC-MESSAGES ", "anthropic-messages"],
        ["openai-responses", "openai-responses"],
        ["openai-completions", "openai-completions"],
    ])("parses %s as %s", (value, expected) => {
        expect(parseTestBackend(value)).toBe(expected);
    });

    it("rejects an unknown backend rather than running CAPI", () => {
        expect(() => parseTestBackend("openai-response")).toThrow(
            "Unsupported COPILOT_SDK_E2E_BACKEND"
        );
    });

    it.each([
        ["anthropic-messages", "anthropic", undefined, "claude-sonnet-5"],
        ["openai-responses", "openai", "responses", "gpt-4.1"],
        ["openai-completions", "openai", "completions", "gpt-4.1"],
    ] as const)("configures %s for create and resume", (backend, type, wireApi, model) => {
        const create: SessionConfig = { sessionId: "new-session" };
        const resume: ResumeSessionConfig = { disableResume: true };
        for (const config of [create, resume]) {
            expect(withTestBackend(config, backend, "http://localhost:1234")).toEqual({
                ...config,
                model,
                provider: {
                    baseUrl: "http://localhost:1234",
                    type,
                    wireApi,
                    bearerToken: "fake-byok-credential-for-e2e-tests",
                    modelId: model,
                    wireModel: model,
                },
            });
            expect(config).not.toHaveProperty("provider");
        }
    });

    it("preserves explicit models and provider configurations", () => {
        const explicitModel: SessionConfig = { model: "test-model" };
        expect(
            withTestBackend(explicitModel, "openai-responses", "http://localhost:1234")
        ).toMatchObject({
            model: "test-model",
            provider: { modelId: "test-model", wireModel: "test-model" },
        });
        const configs: SessionConfig[] = [
            { provider: { type: "openai", baseUrl: "https://example.com" } },
            { providers: [] },
        ];
        for (const config of configs) {
            expect(withTestBackend(config, "openai-responses", "http://localhost:1234")).toBe(
                config
            );
        }
    });

    it("leaves CAPI configurations unchanged", () => {
        const config: SessionConfig = { model: "test-model" };
        expect(withTestBackend(config, "capi", "http://localhost:1234")).toBe(config);
    });
});
