import { describe, expect, it } from "vitest";

import type { SandboxConfig } from "../src/generated/rpc.js";

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
