import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

interface UnsafeIndicator {
    description: string;
    pattern: RegExp;
}

const azureFeedUrl = "https://pkgs.dev.azure.com/devdiv/_packaging/copilot-canary/npm/registry/";
const githubPackagesRegistry = "https://npm.pkg.github.com";
const exactPublicationCommand =
    /^node\s+nodejs\/scripts\/npm-release\.js\s+publish-manifest\s+dist\/release-manifest\.json\s+dist\s+"\$DIST_TAG"\s+"https:\/\/pkgs\.dev\.azure\.com\/devdiv\/_packaging\/copilot-canary\/npm\/registry\/"\s+azure\s*$/;

const unsafeIndicators: UnsafeIndicator[] = [
    { description: "public publication job", pattern: /^\s{2}publish-public:/m },
    { description: "runtime dispatch claim job", pattern: /^\s{2}claim-runtime-dispatch:/m },
    { description: "runtime dispatch marker", pattern: /sdk-runtime-test-dispatch-/i },
    { description: "runtime dispatch ledger", pattern: /runtime-dispatch-ledger/i },
    {
        description: "canonical run mirroring",
        pattern: /canonical[_ -]?run|CANONICAL_RUN_ID|gh\s+run\s+watch/i,
    },
    {
        description: "runtime-run concurrency",
        pattern: /^\s*group:.*runtime_run_id/im,
    },
    { description: "package write permission", pattern: /packages:\s*write/i },
    { description: "public package access", pattern: /--access\s+public/i },
    { description: "npm package publication command", pattern: /npm\s+publish\b/i },
    {
        description: "publication feed reassignment",
        pattern: /\bFEED_URL\s*(?:\+?=|:[-=?+])/,
    },
    {
        description: "public publication mode",
        pattern: /\b[A-Z_]*MODE\s*[:=]\s*["']?public\b/i,
    },
    {
        description: "npm trusted publication setup",
        pattern: /trusted[\s-]*publish|npm\s+install\s+--global\s+npm@/i,
    },
    {
        description: "public npm write command",
        pattern:
            /(?:npm\s+(?:publish|dist-tag)|publish-manifest)[^\n]*registry\.npmjs\.org|registry\.npmjs\.org[^\n]*(?:npm\s+(?:publish|dist-tag)|publish-manifest|_authToken)/i,
    },
];

export function assertSafeTestWorkflow(workflow: string): void {
    const normalizedWorkflow = workflow.replace(/\\\r?\n\s*/g, " ");
    for (const indicator of unsafeIndicators) {
        assert(
            !indicator.pattern.test(workflow) && !indicator.pattern.test(normalizedWorkflow),
            `Unsafe test workflow contains ${indicator.description}.`
        );
    }
    const inputsSection = workflow.match(/inputs:\r?\n([\s\S]*?)\r?\npermissions:/)?.[1];
    assert(inputsSection, "Test workflow dispatch inputs are missing.");
    const inputNames = [...inputsSection.matchAll(/^\s{6}([a-z][a-z0-9_]*):\s*$/gm)]
        .map((match) => match[1])
        .sort();
    assert.deepEqual(
        inputNames,
        ["channel", "mode", "runtime_run_id", "runtime_sha", "runtime_version"].sort(),
        "Test workflow must expose exactly the five approved dispatch inputs."
    );
    const configuredFeeds = [...workflow.matchAll(/^\s*FEED_URL:\s*(\S+)\s*$/gm)];
    assert.equal(
        configuredFeeds.length,
        1,
        "Test workflow must configure exactly one publication feed."
    );
    assert.equal(
        configuredFeeds[0][1],
        azureFeedUrl,
        "Test publication feed must be the exact Azure copilot-canary registry."
    );

    const planStart = workflow.indexOf("  plan:");
    const acquisitionStart = workflow.indexOf("  acquire-runtime:");
    const testStart = workflow.indexOf("  test:", acquisitionStart);
    assert(
        planStart >= 0 && acquisitionStart > planStart && testStart > acquisitionStart,
        "Runtime acquisition job is missing."
    );
    const planJob = workflow.slice(planStart, acquisitionStart);
    assert(!/^\s{4}needs:/m.test(planJob), "Release planning must be the root job.");
    assert(
        planJob.includes("npx tsx scripts/runtime-release-identity.ts"),
        "Runtime release inputs must be validated before acquisition."
    );
    assert(
        planJob.indexOf("Validate runtime release inputs") <
            planJob.indexOf("Calculate the collision-resistant test release identity"),
        "Runtime inputs must be validated before calculating the SDK release identity."
    );
    const acquisitionJob = workflow.slice(acquisitionStart, testStart);
    assert.match(acquisitionJob, /^\s{6}packages:\s*read\s*$/m);
    assert.match(acquisitionJob, /NODE_AUTH_TOKEN:\s*\$\{\{\s*github\.token\s*\}\}/);
    assert.match(
        acquisitionJob,
        /echo "\/\/npm\.pkg\.github\.com\/:_authToken=\$\{NODE_AUTH_TOKEN\}" > "\$HOME\/\.npmrc"/
    );
    assert.match(acquisitionJob, /--registry https:\/\/npm\.pkg\.github\.com\b/);
    assert(
        acquisitionJob.includes(
            'tar -czf "$RUNNER_TEMP/runtime-packages.tar.gz" -C "$RUNNER_TEMP" runtime-packages'
        ),
        "Runtime packages must be archived after validation."
    );
    assert(
        acquisitionJob.includes("path: ${{ runner.temp }}/runtime-packages.tar.gz"),
        "Runtime artifact upload must contain only the validated archive."
    );
    assert(
        !/path:\s*\$\{\{\s*runner\.temp\s*\}\}\/runtime-packages\s*$/m.test(acquisitionJob),
        "Runtime artifact upload must not transport the directory directly."
    );
    assert(
        acquisitionJob.indexOf("npm run acquire:runtime-packages") <
            acquisitionJob.indexOf("Archive validated runtime packages") &&
            acquisitionJob.indexOf("Archive validated runtime packages") <
                acquisitionJob.indexOf("Upload validated runtime packages"),
        "Runtime packages must be validated before archiving and uploading."
    );
    assert(
        !acquisitionJob.includes("azure/login"),
        "Runtime acquisition must not use Azure login."
    );
    assert(
        !acquisitionJob.includes("FEED_URL"),
        "Runtime acquisition must not use the Azure feed."
    );
    assert(
        !`${workflow.slice(0, acquisitionStart)}${workflow.slice(testStart)}`.includes(
            githubPackagesRegistry
        ),
        "GitHub Packages must be used only by runtime acquisition."
    );

    const packageStart = workflow.indexOf("  package:", testStart);
    const publicationStart = workflow.indexOf("  publish-internal:", packageStart);
    assert(
        packageStart > testStart && publicationStart > packageStart,
        "Runtime artifact consumer jobs are missing."
    );
    const testJob = workflow.slice(testStart, packageStart);
    const packageJob = workflow.slice(packageStart, publicationStart);
    const archiveDownloadPath = "path: ${{ runner.temp }}/runtime-package-artifact";
    const archiveExtraction =
        'tar -xzf "$runner_temp/runtime-package-artifact/runtime-packages.tar.gz" -C "$runner_temp"';
    const directWindowsArchivePath =
        'tar -xzf "$RUNNER_TEMP/runtime-package-artifact/runtime-packages.tar.gz"';
    for (const [name, job, nextStep] of [
        ["test", testJob, "Select the acquired runtime"],
        ["package", packageJob, "Build and verify exact package set"],
    ] as const) {
        assert(job.includes(archiveDownloadPath), `${name} job must download the runtime archive.`);
        assert(
            job.includes("if command -v cygpath >/dev/null 2>&1; then") &&
                job.includes('runner_temp="$(cygpath -u "$runner_temp")"'),
            `${name} job must normalize RUNNER_TEMP for Windows Git Bash.`
        );
        assert(job.includes(archiveExtraction), `${name} job must extract the runtime archive.`);
        assert(
            !job.includes(directWindowsArchivePath),
            `${name} job must not pass a Windows RUNNER_TEMP path directly to tar.`
        );
        assert(
            !/path:\s*\$\{\{\s*runner\.temp\s*\}\}\/runtime-packages\s*$/m.test(job),
            `${name} job must not transport the runtime directory directly.`
        );
        assert(
            job.indexOf("Extract validated runtime packages") < job.indexOf(nextStep),
            `${name} job must extract the runtime archive before consuming it.`
        );
    }
    assert(
        testJob.includes(
            'echo "COPILOT_SDK_RUNTIME_PACKAGE_DIR=$COPILOT_SDK_RUNTIME_PACKAGE_DIR" >> "$GITHUB_ENV"'
        ),
        "Test jobs must persist the restored runtime package directory."
    );
    assert(
        testJob.includes('echo "COPILOT_CLI_PATH=$runtime_path" >> "$GITHUB_ENV"'),
        "Test jobs must persist the selected runtime executable."
    );
    assert(
        packageJob.includes("COPILOT_SDK_RUNTIME_PACKAGE_DIR: ${{ runner.temp }}/runtime-packages"),
        "Package construction must use the restored runtime directory."
    );

    const publicationCommands = normalizedWorkflow
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter((line) => /npm-release\.js\s+(?:publish|publish-manifest)\b/.test(line));
    assert.equal(
        publicationCommands.length,
        1,
        "Test workflow must contain exactly one SDK publication command."
    );
    assert.match(
        publicationCommands[0],
        exactPublicationCommand,
        "Test workflow must use the exact Azure publish-manifest command and mode."
    );
}

function main(): void {
    const [workflowPath] = process.argv.slice(2);
    if (!workflowPath) {
        throw new Error("Usage: sdk-canary-safety.ts <workflow-path>");
    }
    assertSafeTestWorkflow(readFileSync(workflowPath, "utf8"));
}

if (process.argv[1] && fileURLToPath(import.meta.url) === resolve(process.argv[1])) {
    try {
        main();
    } catch (error) {
        console.error(`::error::${error instanceof Error ? error.message : String(error)}`);
        process.exitCode = 1;
    }
}
