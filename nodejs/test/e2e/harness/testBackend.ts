/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { ProviderConfig, SessionConfig } from "../../../src";
import type { ReplayBackend } from "../../../../test/harness/replayingCapiProxy";

export function parseTestBackend(value: string | undefined): ReplayBackend {
    const backend = value?.trim().toLowerCase() || "capi";
    switch (backend) {
        case "capi":
        case "anthropic-messages":
        case "openai-responses":
        case "openai-completions":
            return backend;
        default:
            throw new Error(`Unsupported COPILOT_SDK_E2E_BACKEND: ${value}`);
    }
}

export const testBackend = parseTestBackend(process.env.COPILOT_SDK_E2E_BACKEND);
export const isByokBackend = testBackend !== "capi";

export function withTestBackend<T extends Pick<SessionConfig, "model" | "provider" | "providers">>(
    config: T,
    backend: ReplayBackend,
    proxyUrl: string
): T {
    if (backend === "capi" || config.provider !== undefined || config.providers !== undefined) {
        return config;
    }

    const model =
        config.model ?? (backend === "anthropic-messages" ? "claude-sonnet-5" : "gpt-4.1");
    const provider: ProviderConfig = {
        type: backend === "anthropic-messages" ? "anthropic" : "openai",
        wireApi:
            backend === "anthropic-messages"
                ? undefined
                : backend === "openai-responses"
                  ? "responses"
                  : "completions",
        baseUrl: proxyUrl,
        bearerToken: "fake-byok-credential-for-e2e-tests",
        modelId: model,
        wireModel: model,
    };
    return { ...config, model, provider };
}
