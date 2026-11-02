/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import fs from "node:fs";
import path from "node:path";
import { randomUUID } from "node:crypto";

const BUNDLE_VERSION = "copilot-test-results/v2";

function shardName(environment) {
    const shard = environment.SHARD ?? environment.CI_TEST_SHARD;
    const count = environment.SHARD_COUNT ?? environment.CI_TEST_SHARD_COUNT;
    if (!shard) {
        return "";
    }
    return count ? `${shard}/${count}` : shard;
}

function normalizedStatus(state, flaky) {
    if (flaky) {
        return "flaky";
    }
    switch (state) {
        case "pass":
        case "passed":
            return "passed";
        case "fail":
        case "failed":
            return "failed";
        case "skip":
        case "skipped":
        case "pending":
        case "todo":
            return "skipped";
        default:
            return "unknown";
    }
}

function positiveInteger(value, fallback) {
    const number = Number(value);
    return Number.isSafeInteger(number) && number > 0 ? number : fallback;
}

function producerFields(invocationId, environment) {
    return {
        InvocationId: invocationId,
        ProducerId: environment.CI_TEST_PRODUCER_ID ?? "vitest",
        ProducerVersion: environment.CI_TEST_PRODUCER_VERSION ?? "2",
        SourceFormat: "vitest-reporter",
        SourceFormatVersion: "2",
        SourceArtifact: null,
        Configuration: environment.CI_TEST_CONFIGURATION ?? null,
        Variant: environment.CI_TEST_VARIANT ?? null,
        Toolchain: environment.CI_TEST_TOOLCHAIN ?? "node",
        Runtime: environment.CI_TEST_RUNTIME ?? `node-${process.version}`,
        ProducerAttempt: positiveInteger(environment.CI_TEST_PRODUCER_ATTEMPT, 1),
        Shard: shardName(environment),
    };
}

export function captureTestCase(testCase, invocationId, environment = process.env) {
    const result = testCase.result();
    const diagnostic = testCase.diagnostic();
    const completedAt =
        diagnostic?.startTime === undefined || diagnostic?.duration === undefined
            ? null
            : new Date(diagnostic.startTime + diagnostic.duration).toISOString();

    return {
        SchemaVersion: 2,
        ...producerFields(invocationId, environment),
        CompletedAt: completedAt,
        Framework: "vitest",
        TestTarget: testCase.project?.name ?? "",
        TestFile: testCase.module.relativeModuleId,
        Suite: testCase.parent.fullName ?? "",
        TestName: testCase.name,
        Status: normalizedStatus(result.state, diagnostic?.flaky),
        RawStatus: typeof result.state === "string" ? result.state : "unknown",
        DurationMs: diagnostic?.duration ?? null,
        Attempt: (diagnostic?.retryCount ?? 0) + 1,
        FrameworkRetry: diagnostic?.retryCount ?? 0,
        Cached: false,
        Metadata: {
            testId: testCase.id,
            repeatCount: diagnostic?.repeatCount ?? 0,
            mode: testCase.options.mode,
        },
    };
}

export function captureRunFailure(unhandledErrors, invocationId, reason, environment = process.env) {
    if (unhandledErrors.length === 0) {
        return undefined;
    }
    return {
        SchemaVersion: 2,
        ...producerFields(invocationId, environment),
        CompletedAt: new Date().toISOString(),
        Framework: "vitest",
        TestTarget: "",
        TestFile: "",
        Suite: "",
        TestName: "Vitest run",
        Status: "failed",
        RawStatus: unhandledErrors.length > 0 ? "unhandled-error" : reason || "unknown",
        DurationMs: null,
        Attempt: 1,
        FrameworkRetry: null,
        Cached: false,
        Metadata: {
            testId: `vitest-run:${invocationId}`,
            repeatCount: 0,
            mode: "run",
            reason: reason || "unknown",
            unhandledErrorCount: unhandledErrors.length,
        },
    };
}

export function writeCaptureFile(outputDirectory, payload, fileSystem = fs, logError = console.error) {
    try {
        fileSystem.mkdirSync(outputDirectory, { recursive: true });
        const outputPath = path.join(
            outputDirectory,
            `vitest-${Date.now()}-${process.pid}-${payload.invocationId ?? randomUUID()}.json`,
        );
        fileSystem.writeFileSync(outputPath, JSON.stringify(payload));
        return true;
    } catch (error) {
        logError("CI test result persistence failed; preserving the test process result.", error);
        return false;
    }
}

export default class CiTestResultsReporter {
    constructor() {
        this.invocationId = randomUUID();
        this.results = [];
    }

    onTestCaseResult(testCase) {
        this.results.push(captureTestCase(testCase, this.invocationId));
    }

    onTestRunEnd(_testModules, unhandledErrors = [], reason = "unknown") {
        const outputDirectory = process.env.CI_TEST_RESULTS_DIR;
        if (!outputDirectory) {
            return;
        }

        const runFailure = captureRunFailure(unhandledErrors, this.invocationId, reason);
        if (runFailure) {
            this.results.push(runFailure);
        }
        if (this.results.length === 0) {
            return;
        }
        writeCaptureFile(outputDirectory, {
            bundleVersion: BUNDLE_VERSION,
            schemaVersion: 2,
            invocationId: this.invocationId,
            results: this.results,
        });
    }
}
