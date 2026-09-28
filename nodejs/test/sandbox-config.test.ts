import { describe, expect, it } from "vitest";

import type { SandboxConfig } from "../src/generated/rpc.js";
import type { SandboxConfigSource } from "../src/index.js";

describe("SandboxConfig", () => {
    it("round-trips allowBypass and omits it when absent", () => {
        const enabled: SandboxConfig = { enabled: true, allowBypass: true };
        const roundTripped = JSON.parse(JSON.stringify(enabled)) as SandboxConfig;

        expect(roundTripped.allowBypass).toBe(true);
        expect(roundTripped).toEqual({ enabled: true, allowBypass: true });

        const omitted: SandboxConfig = { enabled: true };
        expect(JSON.parse(JSON.stringify(omitted))).toEqual({ enabled: true });
    });
});

describe("SandboxConfigSource", () => {
    it("is importable from the package root", () => {
        const source: SandboxConfigSource = "user_disabled";
        expect(source).toBe("user_disabled");
    });
});
