import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parse } from "yaml";

interface UnsafeIndicator {
    description: string;
    pattern: RegExp;
}

type Mapping = Record<string, unknown>;

function mapping(value: unknown, label: string): Mapping {
    assert(
        typeof value === "object" && value !== null && !Array.isArray(value),
        `${label} must be a mapping.`
    );
    return value as Mapping;
}

function normalizedExpression(value: unknown, label: string): string {
    assert.equal(typeof value, "string", `${label} must be a string expression.`);
    return value.replace(/\s+/g, " ").trim();
}

function normalizedCommand(value: unknown, label: string): string {
    assert.equal(typeof value, "string", `${label} must be a string command.`);
    return value.replace(/["']/g, "").replace(/\s+/g, " ").trim();
}

const allowedActions = new Set([
    "actions/checkout@de0fac2e4500dabe0009e67214ff5f5447ce83dd",
    "actions/setup-node@249970729cb0ef3589644e2896645e5dc5ba9c38",
    "actions/upload-artifact@bbbca2ddaa5d8feaa63e36b76fdaad77386f024f",
    "actions/download-artifact@70fc10c6e5e1ce46ad2ea6f2b72d43f7d47b13c3",
    "azure/login@532459ea530d8321f2fb9bb10d1e0bcf23869a43",
]);

const expectedJobPermissions: Record<string, Mapping> = {
    "validate-dispatch": { actions: "read", contents: "read" },
    plan: { actions: "read", contents: "read" },
    "acquire-runtime": { contents: "read", packages: "read" },
    test: { contents: "read" },
    package: { contents: "read" },
    "publish-internal": { actions: "read", contents: "read", "id-token": "write" },
};

const expectedJobActions: Record<string, string[]> = {
    "validate-dispatch": [
        "actions/checkout@de0fac2e4500dabe0009e67214ff5f5447ce83dd",
        "actions/setup-node@249970729cb0ef3589644e2896645e5dc5ba9c38",
    ],
    plan: [
        "actions/checkout@de0fac2e4500dabe0009e67214ff5f5447ce83dd",
        "actions/setup-node@249970729cb0ef3589644e2896645e5dc5ba9c38",
    ],
    "acquire-runtime": [
        "actions/checkout@de0fac2e4500dabe0009e67214ff5f5447ce83dd",
        "actions/setup-node@249970729cb0ef3589644e2896645e5dc5ba9c38",
        "actions/upload-artifact@bbbca2ddaa5d8feaa63e36b76fdaad77386f024f",
    ],
    test: [
        "actions/checkout@de0fac2e4500dabe0009e67214ff5f5447ce83dd",
        "actions/setup-node@249970729cb0ef3589644e2896645e5dc5ba9c38",
        "actions/download-artifact@70fc10c6e5e1ce46ad2ea6f2b72d43f7d47b13c3",
    ],
    package: [
        "actions/checkout@de0fac2e4500dabe0009e67214ff5f5447ce83dd",
        "actions/setup-node@249970729cb0ef3589644e2896645e5dc5ba9c38",
        "actions/download-artifact@70fc10c6e5e1ce46ad2ea6f2b72d43f7d47b13c3",
        "actions/upload-artifact@bbbca2ddaa5d8feaa63e36b76fdaad77386f024f",
    ],
    "publish-internal": [
        "actions/checkout@de0fac2e4500dabe0009e67214ff5f5447ce83dd",
        "actions/setup-node@249970729cb0ef3589644e2896645e5dc5ba9c38",
        "actions/download-artifact@70fc10c6e5e1ce46ad2ea6f2b72d43f7d47b13c3",
        "azure/login@532459ea530d8321f2fb9bb10d1e0bcf23869a43",
    ],
};

const azureFeedUrl = "https://pkgs.dev.azure.com/devdiv/_packaging/copilot-canary/npm/registry/";
const exactPublicationCommand =
    /^node\s+nodejs\/scripts\/npm-release\.js\s+publish-manifest\s+dist\/release-manifest\.json\s+dist\s+"\$DIST_TAG"\s+"https:\/\/pkgs\.dev\.azure\.com\/devdiv\/_packaging\/copilot-canary\/npm\/registry\/"\s+azure\s*$/;

const unsafeIndicators: UnsafeIndicator[] = [
    { description: "public publication job", pattern: /^\s{2}publish-public:/m },
    { description: "public runtime publication job", pattern: /^\s{2}runtime-publish-public:/m },
    { description: "GitHub release mutation", pattern: /\bgh\s+release\s+create\b/i },
    { description: "source tag mutation", pattern: /\bgit\s+(?:tag|push)\b/i },
    { description: "obsolete runtime workflow path", pattern: /runtime-sdk\.yml/i },
    {
        description: "obsolete dispatch contract",
        pattern:
            /inputs\.(?:channel|runtime_version|runtime_sha|runtime_run_id)|^\s{6}(?:channel|runtime_version|runtime_sha|runtime_run_id|version):\s*$/im,
    },
    {
        description: "obsolete release mode",
        pattern: /(?:^\s{10}-\s+tests-only\s*$|inputs\.mode\s*==\s*['"]internal['"])/im,
    },
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
    const document = mapping(parse(workflow), "Test workflow");
    const triggers = mapping(document.on, "Test workflow trigger section");
    assert.deepEqual(
        Object.keys(triggers),
        ["workflow_dispatch"],
        "Test workflow must remain manual workflow_dispatch only."
    );
    const dispatch = mapping(triggers.workflow_dispatch, "workflow_dispatch configuration");
    const inputs = mapping(dispatch.inputs, "Test workflow dispatch inputs");
    assert.deepEqual(
        Object.keys(inputs),
        ["dist-tag", "mode", "test-policy", "runtime"],
        "Test workflow must expose exactly the four approved dispatch inputs."
    );
    const distTagInput = mapping(inputs["dist-tag"], "dist-tag input");
    assert.deepEqual(
        distTagInput.options,
        ["canary", "unstable"],
        "Test workflow dist-tags must be exactly canary and unstable."
    );
    assert.equal(distTagInput.required, true);
    assert.equal(distTagInput.type, "choice");
    const modeInput = mapping(inputs.mode, "mode input");
    assert.deepEqual(
        modeInput.options,
        ["dry-run", "publish"],
        "Test workflow modes must be exactly dry-run and publish."
    );
    assert.equal(modeInput.required, true);
    assert.equal(modeInput.type, "choice");
    assert.equal(modeInput.default, "dry-run");
    const testPolicyInput = mapping(inputs["test-policy"], "test-policy input");
    assert.deepEqual(
        testPolicyInput.options,
        ["required", "advisory", "skipped"],
        "Test workflow policies must be exactly required, advisory, and skipped."
    );
    assert.equal(testPolicyInput.required, true);
    assert.equal(testPolicyInput.type, "choice");
    assert.equal(testPolicyInput.default, "required");
    const runtimeInput = mapping(inputs.runtime, "runtime input");
    assert.equal(runtimeInput.required, true);
    assert.equal(runtimeInput.type, "string");
    const jobs = mapping(document.jobs, "Test workflow jobs");
    assert.deepEqual(
        Object.keys(jobs),
        ["validate-dispatch", "plan", "acquire-runtime", "test", "package", "publish-internal"],
        "Test workflow must contain exactly the approved jobs."
    );
    assert.deepEqual(document.permissions, { contents: "read" });
    const parsedSteps: Array<{ jobName: string; step: Mapping }> = [];
    for (const [jobName, value] of Object.entries(jobs)) {
        const parsedJob = mapping(value, `${jobName} job`);
        assert.deepEqual(
            parsedJob.permissions,
            expectedJobPermissions[jobName],
            `${jobName} job permissions must match the approved least-privilege set.`
        );
        const steps = parsedJob.steps;
        assert(Array.isArray(steps), `${jobName} steps must be a sequence.`);
        const jobActions: string[] = [];
        for (const [index, stepValue] of steps.entries()) {
            const step = mapping(stepValue, `${jobName} step ${index + 1}`);
            parsedSteps.push({ jobName, step });
            assert(
                !Object.hasOwn(step, "shell"),
                `${jobName} steps must not override the approved job shell.`
            );
            if (step.uses !== undefined) {
                assert.equal(typeof step.uses, "string", `${jobName} action must be a string.`);
                assert(
                    allowedActions.has(step.uses),
                    `Test workflow contains unapproved action '${step.uses}'.`
                );
                jobActions.push(step.uses);
                if (step.uses.startsWith("actions/checkout@")) {
                    const checkoutWith =
                        step.with === undefined ? {} : mapping(step.with, "checkout configuration");
                    assert.deepEqual(
                        checkoutWith,
                        jobName === "plan" ? { "fetch-depth": 0 } : {},
                        `${jobName} checkout configuration must not override the reviewed repository, ref, or path.`
                    );
                }
            }
            if (step.run !== undefined) {
                const command = normalizedCommand(step.run, `${jobName} run command`);
                for (const [description, pattern] of [
                    ["GitHub release mutation", /\bgh release create\b/i],
                    ["source tag mutation", /\bgit (?:tag|push)\b/i],
                    ["npm package publication command", /\bnpm publish\b/i],
                    ["npm dist-tag mutation", /\bnpm dist-tag\b/i],
                    ["public package access", /--access public\b/i],
                    [
                        "npm trusted publication setup",
                        /trusted[\s-]*publish|npm install --global npm@/i,
                    ],
                    [
                        "public npm write command",
                        /(?:npm (?:publish|dist-tag)|publish-manifest).*registry\.npmjs\.org|registry\.npmjs\.org.*(?:npm (?:publish|dist-tag)|publish-manifest|_authToken)/i,
                    ],
                ] as const) {
                    assert(!pattern.test(command), `Unsafe test workflow contains ${description}.`);
                }
            }
        }
        assert.deepEqual(
            jobActions,
            expectedJobActions[jobName],
            `${jobName} job action sequence must match the approved workflow.`
        );
    }
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
    assert.match(
        workflow,
        /^\s{2}TEST_WORKFLOW_PATH:\s*\.github\/workflows\/sdk-canary\.yml\s*$/m,
        "Test manifest validation must use the registered legacy workflow path."
    );

    const validationStart = workflow.indexOf("  validate-dispatch:");
    const planStart = workflow.indexOf("  plan:");
    const acquisitionStart = workflow.indexOf("  acquire-runtime:");
    const testStart = workflow.indexOf("  test:", acquisitionStart);
    assert(
        validationStart >= 0 &&
            planStart > validationStart &&
            acquisitionStart > planStart &&
            testStart > acquisitionStart,
        "Runtime acquisition job is missing."
    );
    const validationJob = workflow.slice(validationStart, planStart);
    const planJob = workflow.slice(planStart, acquisitionStart);
    assert(!/^\s{4}needs:/m.test(validationJob), "Dispatch validation must be the root job.");
    assert(
        validationJob.includes("npx tsx scripts/runtime-release-identity.ts"),
        "Runtime release inputs must be validated before acquisition."
    );
    for (const binding of [
        "DIST_TAG: ${{ inputs.dist-tag }}",
        "MODE: ${{ inputs.mode }}",
        "RUNTIME_JSON: ${{ inputs.runtime }}",
        "TEST_POLICY: ${{ inputs.test-policy }}",
        'VERSION_OVERRIDE: ""',
    ]) {
        assert(validationJob.includes(binding), `Runtime validation must include '${binding}'.`);
    }
    assert(
        validationJob.includes('= "runtime"'),
        "Test workflow must reject the direct release path."
    );
    assert(
        validationJob.includes("test_policy: ${{ steps.validate.outputs.test_policy }}"),
        "Validated test policy must be exported for downstream jobs."
    );
    assert(
        planJob.includes("needs: validate-dispatch"),
        "Release planning must depend on dispatch validation."
    );
    assert(
        planJob.includes("WORKFLOW_RUN_ID: ${{ github.run_id }}"),
        "Unstable SDK identity must use the repository-wide workflow run ID."
    );
    assert(
        planJob.includes(
            'echo "Runtime E2E test policy: ${{ needs.validate-dispatch.outputs.test_policy }}" >> "$GITHUB_STEP_SUMMARY"'
        ),
        "Release planning must summarize the validated test policy."
    );
    assert(
        validationStart < planStart,
        "Runtime inputs must be validated before release planning."
    );
    const acquisitionJob = workflow.slice(acquisitionStart, testStart);
    assert.match(acquisitionJob, /^\s{6}packages:\s*read\s*$/m);
    assert.match(acquisitionJob, /NODE_AUTH_TOKEN:\s*\$\{\{\s*github\.token\s*\}\}/);
    assert.match(
        acquisitionJob,
        /echo "\/\/npm\.pkg\.github\.com\/:_authToken=\$\{NODE_AUTH_TOKEN\}" > "\$HOME\/\.npmrc"/
    );
    assert(
        !acquisitionJob.includes("--registry"),
        "Runtime acquisition must not accept a workflow-level registry override."
    );
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
    assert.equal(
        (workflow.match(/npm\.pkg\.github\.com/g) ?? []).length,
        1,
        "GitHub Packages authentication must appear only in runtime acquisition."
    );

    const packageStart = workflow.indexOf("  package:", testStart);
    const publicationStart = workflow.indexOf("  publish-internal:", packageStart);
    assert(
        packageStart > testStart && publicationStart > packageStart,
        "Runtime artifact consumer jobs are missing."
    );
    const testJob = workflow.slice(testStart, packageStart);
    const packageJob = workflow.slice(packageStart, publicationStart);
    const publicationJob = workflow.slice(publicationStart);
    const parsedTestJob = mapping(jobs.test, "test job");
    const parsedPackageJob = mapping(jobs.package, "package job");
    const parsedPublicationJob = mapping(jobs["publish-internal"], "publish-internal job");
    assert.equal(
        parsedTestJob.if,
        "needs.validate-dispatch.outputs.test_policy != 'skipped'",
        "Skipped policy must be the only reason to omit the runtime test job."
    );
    assert.equal(
        normalizedExpression(parsedPackageJob.if, "package job condition"),
        normalizedExpression(
            `always() &&
            !cancelled() &&
            needs.validate-dispatch.result == 'success' &&
            needs.plan.result == 'success' &&
            needs.acquire-runtime.result == 'success' &&
            (
              needs.test.result == 'success' ||
              (needs.validate-dispatch.outputs.test_policy == 'skipped' && needs.test.result == 'skipped')
            )`,
            "expected package job condition"
        ),
        "Package job must block unless every prerequisite succeeds or tests are intentionally skipped."
    );
    assert.equal(
        normalizedExpression(parsedPublicationJob.if, "publish-internal job condition"),
        normalizedExpression(
            `always() &&
            !cancelled() &&
            inputs.mode == 'publish' &&
            needs.plan.result == 'success' &&
            needs.package.result == 'success'`,
            "expected publish-internal job condition"
        ),
        "Azure publication must require publish mode and a successful retained package."
    );
    const continueOnErrorSteps = parsedSteps
        .map(({ step }) => step)
        .filter((step) => Object.hasOwn(step, "continue-on-error"));
    assert.equal(
        continueOnErrorSteps.length,
        1,
        "Only the Node SDK test command may tolerate advisory failures."
    );
    assert.deepEqual(
        {
            continueOnError: continueOnErrorSteps[0]["continue-on-error"],
            name: continueOnErrorSteps[0].name,
            run: continueOnErrorSteps[0].run,
        },
        {
            continueOnError: "${{ needs.validate-dispatch.outputs.test_policy == 'advisory' }}",
            name: "Run Node SDK tests",
            run: "npm test",
        },
        "Only the exact Node SDK test command may tolerate advisory failures."
    );
    assert(
        testJob.includes("if: needs.validate-dispatch.outputs.test_policy != 'skipped'"),
        "Skipped policy must omit the runtime test job."
    );
    assert.match(
        testJob,
        /- name: Run Node SDK tests\s+id: e2e\s+continue-on-error: \$\{\{ needs\.validate-dispatch\.outputs\.test_policy == 'advisory' \}\}[\s\S]*?run: npm test/
    );
    assert.equal(
        (workflow.match(/continue-on-error:/g) ?? []).length,
        1,
        "Only the Node SDK test command may tolerate advisory failures."
    );
    assert(
        testJob.includes(
            "needs.validate-dispatch.outputs.test_policy == 'advisory' && steps.e2e.outcome == 'failure'"
        ) &&
            testJob.includes("::warning::Runtime-backed Node SDK E2E tests failed") &&
            testJob.includes("### Advisory runtime E2E failure"),
        "Advisory test failures must emit warning and summary evidence."
    );
    for (const condition of [
        "always()",
        "!cancelled()",
        "needs.validate-dispatch.result == 'success'",
        "needs.plan.result == 'success'",
        "needs.acquire-runtime.result == 'success'",
        "needs.test.result == 'success'",
        "needs.validate-dispatch.outputs.test_policy == 'skipped' && needs.test.result == 'skipped'",
    ]) {
        assert(
            packageJob.includes(condition),
            `Package job must retain blocking condition '${condition}'.`
        );
    }
    assert(
        publicationJob.includes("inputs.mode == 'publish'"),
        "Azure test publication must require publish mode."
    );
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
    assert(
        packageJob.includes("release:manifest -- create") &&
            packageJob.includes("release:manifest -- verify"),
        "Package construction must create and verify the test release manifest."
    );
    assert(
        packageJob.includes("TEST_POLICY: ${{ needs.validate-dispatch.outputs.test_policy }}"),
        "Test policy must be recorded in the retained release manifest."
    );
    assert(
        publicationJob.includes(".workflow.testPolicy dist/release-manifest.json") &&
            publicationJob.includes("needs.validate-dispatch.outputs.test_policy"),
        "Publication must verify the retained test policy."
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
