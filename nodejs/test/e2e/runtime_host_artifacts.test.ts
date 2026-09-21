/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { fileURLToPath } from "node:url";
import { afterEach, describe, expect, it, vi } from "vitest";
import { localHostArtifacts } from "./harness/runtimeHost.js";
import { candidateHostArtifacts } from "./harness/runtimeHostCandidate.js";

afterEach(() => vi.unstubAllEnvs());

describe("Runtime host artifact selection", () => {
    it("requires an absolute candidate provenance manifest", () => {
        expect(() => candidateHostArtifacts("candidate.json")).toThrow(
            "Candidate manifest path must be absolute"
        );
    });

    it("fails closed instead of falling back when the selected candidate is missing", () => {
        const missing = fileURLToPath(new URL(`./missing-${randomUUID()}.json`, import.meta.url));
        vi.stubEnv("COPILOT_RUNTIME_HOST_CANDIDATE_MANIFEST", missing);
        vi.stubEnv("COPILOT_CLI_PATH", "/unrelated-development-runtime");
        vi.stubEnv("COPILOT_RUNTIME_PROVIDER_LIB", "/unrelated-development-provider");
        vi.stubEnv("COPILOTD_LITE_PATH", "/unrelated-development-lite");
        expect(() => localHostArtifacts()).toThrow(missing);
    });

    it("does not accept an npm artifact as an unattested local source build", () => {
        vi.stubEnv("COPILOT_RUNTIME_HOST_CANDIDATE_MANIFEST", undefined);
        vi.stubEnv(
            "COPILOT_CLI_PATH",
            fileURLToPath(new URL("../../node_modules/tsx/dist/cli.mjs", import.meta.url))
        );
        expect(() => localHostArtifacts()).toThrow("must not use a released runtime package");
    });
});
