const assert = require("node:assert/strict");
const path = require("node:path");
const test = require("node:test");
const { ESLint } = require("eslint");

function getResult(results, fileName) {
    const result = results.find((entry) => path.basename(entry.filePath) === fileName);
    assert.ok(result, `Missing ESLint result for ${fileName}`);
    return result;
}

function getReportedNames(messages) {
    return messages
        .map((message) => message.message.match(/'([^']+)'/)?.[1])
        .filter(Boolean)
        .sort();
}

function assertRuleIds(messages) {
    for (const message of messages) {
        assert.strictEqual(message.ruleId, "@github/copilot-sdk/no-experimental-api");
    }
}

test("flags experimental references and ignores stable or suppressed uses", async () => {
    const eslint = new ESLint({ cwd: path.join(__dirname, "..") });
    const results = await eslint.lintFiles(["test/fixtures/consumer-*.ts"]);

    const experimentalMessages = getResult(results, "consumer-experimental.ts").messages;
    assert.strictEqual(
        experimentalMessages.length,
        3,
        `expected 3 diagnostics, got:\n${JSON.stringify(experimentalMessages, null, 2)}`,
    );
    assertRuleIds(experimentalMessages);
    assert.deepStrictEqual(getReportedNames(experimentalMessages), [
        "CanvasOptions",
        "enableMcpApps",
        "startCanvas",
    ]);

    assert.deepStrictEqual(getResult(results, "consumer-stable.ts").messages, []);
    assert.deepStrictEqual(getResult(results, "consumer-suppressed.ts").messages, []);

    const aliasedMessages = getResult(results, "consumer-aliased.ts").messages;
    assert.strictEqual(
        aliasedMessages.length,
        1,
        `expected 1 diagnostic, got:\n${JSON.stringify(aliasedMessages, null, 2)}`,
    );
    assertRuleIds(aliasedMessages);
    assert.deepStrictEqual(getReportedNames(aliasedMessages), ["launchCanvas"]);
});
