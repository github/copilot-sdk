import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";

const repositoryRoot = join(import.meta.dirname, "..", "..");
const workflow = (name: string) =>
    readFileSync(join(repositoryRoot, ".github", "workflows", name), "utf8");
const canary = workflow("sdk-canary.yml");
const publish = workflow("publish.yml");
const shared = workflow("runtime-backed-node-release.yml");

describe("SDK canary workflow contract", () => {
    it("accepts only the exact Azure canary handoff", () => {
        for (const input of [
            "channel:",
            "runtime_version:",
            "runtime_sha:",
            "runtime_source:",
            "runtime_run_id:",
            "mode:",
        ]) {
            expect(canary).toContain(input);
        }
        expect(canary).toContain("- canary");
        expect(canary).toContain("- azure");
        expect(canary).toContain("- tests-only");
        expect(canary).toContain("- internal");
    });

    it("delegates implementation without granting public capability", () => {
        expect(canary).toContain("uses: ./.github/workflows/runtime-backed-node-release.yml");
        expect(canary).toContain("channel: canary");
        expect(canary).not.toContain("registry.npmjs.org");
        expect(canary).not.toContain("unstable-publish-public");
    });
});

describe("shared runtime-backed Node pipeline", () => {
    it("enforces the channel, source, and mode matrix again", () => {
        expect(shared).toContain("canary:azure:tests-only");
        expect(shared).toContain("canary:azure:internal");
        expect(shared).toContain("unstable:github-packages:internal");
        expect(shared).not.toContain("registry.npmjs.org");
    });

    it("owns acquisition, cross-platform tests, packaging, and internal verification", () => {
        expect(shared).toContain("os: [ubuntu-latest, macos-latest, windows-latest]");
        expect(shared).toContain("npm run acquire:runtime-packages");
        expect(shared).toContain("npm run verify:release-packages");
        expect(shared).toContain("publish-manifest");
        expect(shared.indexOf("npm run verify:release-packages")).toBeLessThan(
            shared.indexOf("publish-manifest")
        );
    });
});

describe("unstable publishing workflow contract", () => {
    it("requires the authenticated GitHub Packages runtime handoff", () => {
        expect(publish).toContain("runtime_source:");
        expect(publish).toContain("- github-packages");
        expect(shared).toContain("packages: read");
        expect(shared).toContain("//npm.pkg.github.com/:_authToken=");
        expect(shared).not.toContain("@github:registry=https://npm.pkg.github.com");
    });

    it("freezes identity, delegates internal preparation, then publishes publicly", () => {
        expect(publish).toContain("scripts/unstable-version.ts");
        expect(publish).toContain("uses: ./.github/workflows/runtime-backed-node-release.yml");
        expect(shared).toContain("release-manifest.json");
        expect(shared).toContain("COPILOT_CLI_USE_NPM_PACKAGE = false");
        expect(publish.indexOf("unstable-runtime-backed-release:")).toBeLessThan(
            publish.indexOf("unstable-publish-public:")
        );
        expect(publish).toContain("needs: [unstable-plan, unstable-runtime-backed-release]");
    });

    it("supports retained-artifact recovery without enabling non-Node release paths", () => {
        expect(publish).toContain("resume_run_id:");
        expect(publish).toContain("run-id: ${{ inputs.resume_run_id }}");
        expect(shared).toContain("run-id: ${{ inputs.resume_run_id }}");
        expect(publish).toContain("Manifest workflow run ID does not match resume_run_id");
        expect(
            publish.match(/github\.event\.inputs\.dist-tag != 'unstable'/g)?.length
        ).toBeGreaterThan(3);
        expect(publish).toContain("github.event.inputs.dist-tag != 'unstable' &&");
    });
});
