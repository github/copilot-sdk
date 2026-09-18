import assert from "node:assert/strict";
import test from "node:test";
import type { JSONSchema7 } from "json-schema";

import {
    collectNestedDiscriminatedUnionTypeNames,
    renderEventVariantClass,
    renderRpcTypes,
    schemaTypeToJava,
} from "./java.js";
import { RPC_VARIANT_OWNERS } from "./rpc-variant-owners.js";

function renderPayload(dataSchema: JSONSchema7): string {
    return renderEventVariantClass({
        typeName: "example.notification",
        className: "ExampleNotificationEvent",
        dataSchema,
    }, "com.github.copilot.generated");
}

for (const keyword of ["anyOf", "oneOf"] as const) {
    test(`root ${keyword} payload preserves raw JSON and existing data descriptors`, () => {
        const source = renderPayload({
            [keyword]: [
                { type: "object", properties: { kind: { const: "first" }, count: { type: "number" } } },
                { type: "object", properties: { kind: { const: "second" }, active: { type: "boolean" } } },
                { type: "null" },
            ],
        });

        assert.match(source, /public record ExampleNotificationEventData\(JsonNode raw\)/);
        assert.match(source, /@JsonCreator\(mode = JsonCreator\.Mode\.DELEGATING\)\s+public ExampleNotificationEventData\s*\{/);
        assert.match(source, /@JsonValue\s+public JsonNode raw\(\) \{ return raw; \}/);
        assert.match(source, /public ExampleNotificationEventData\(\) \{\s+this\(JsonNodeFactory\.instance\.objectNode\(\)\);/);
        assert.match(source, /public ExampleNotificationEventData getData\(\)/);
        assert.match(source, /public void setData\(ExampleNotificationEventData data\)/);
    });

    test(`root ${keyword} with only one non-null alternative is not a raw union`, () => {
        const source = renderPayload({
            [keyword]: [
                { type: "object", properties: { message: { type: "string" } } },
                { type: "null" },
                { type: ["null"] },
                { const: null },
                { enum: [null] },
            ],
        });
        assert.doesNotMatch(source, /JsonNode|DELEGATING|JsonValue/);
    });
}

test("ordinary and empty root objects retain their existing records", () => {
    const source = renderPayload({
        type: "object",
        properties: { message: { type: "string" } },
    });
    assert.match(source, /@JsonProperty\("message"\) String message/);
    assert.doesNotMatch(source, /JsonNode|DELEGATING|JsonValue/);

    const empty = renderPayload({ type: "object", properties: {} });
    assert.match(empty, /public record ExampleNotificationEventData\(\)/);
    assert.doesNotMatch(empty, /JsonNode|DELEGATING|JsonValue/);
});

test("nested discriminated array items use their named Java type", () => {
    const definitions: Record<string, JSONSchema7> = {
        MessageResult: {
            anyOf: [
                { $ref: "#/definitions/MessageDelivered" },
                { $ref: "#/definitions/MessageRejected" },
            ],
        },
        MessageDelivered: {
            type: "object",
            additionalProperties: false,
            properties: {
                status: { const: "delivered" },
                actions: {
                    type: "array",
                    items: { $ref: "#/definitions/ActionChoice" },
                },
            },
        },
        MessageRejected: {
            type: "object",
            additionalProperties: false,
            properties: {
                status: { const: "rejected" },
                reason: { type: "string" },
            },
        },
        ActionChoice: {
            anyOf: [
                { $ref: "#/definitions/PhoneAction" },
                { $ref: "#/definitions/EmailAction" },
            ],
        },
        PhoneAction: {
            type: "object",
            title: "PhoneAction",
            additionalProperties: false,
            properties: {
                kind: { const: "phone" },
                number: { type: "string" },
                sources: {
                    type: "object",
                    additionalProperties: { $ref: "#/definitions/ActionSource" },
                },
            },
        },
        EmailAction: {
            type: "object",
            title: "EmailAction",
            additionalProperties: false,
            properties: {
                kind: { const: "email" },
                address: { type: "string" },
                sources: {
                    type: "object",
                    additionalProperties: { $ref: "#/definitions/ActionSource" },
                },
            },
        },
        ActionSource: {
            anyOf: [
                { $ref: "#/definitions/LocalActionSource" },
                { $ref: "#/definitions/RemoteActionSource" },
            ],
        },
        LocalActionSource: {
            type: "object",
            title: "LocalActionSource",
            additionalProperties: false,
            properties: {
                location: { const: "local" },
            },
        },
        RemoteActionSource: {
            type: "object",
            title: "RemoteActionSource",
            additionalProperties: false,
            properties: {
                location: { const: "remote" },
                url: { type: "string" },
            },
        },
    };
    const standaloneTypes = new Map<string, JSONSchema7>();
    const promotedUnionTypes = collectNestedDiscriminatedUnionTypeNames(
        { $ref: "#/definitions/MessageResult" },
        definitions
    );

    const result = schemaTypeToJava(
        {
            type: "array",
            items: { $ref: "#/definitions/ActionChoice" },
        },
        false,
        "MessageEnvelope",
        "actions",
        new Map(),
        {
            definitions,
            standaloneTypes,
            promotedUnionTypes,
        }
    );

    assert.equal(result.javaType, "List<ActionChoice>");
    assert.deepEqual([...standaloneTypes.keys()], ["ActionChoice"]);
    assert.deepEqual([...promotedUnionTypes], ["ActionChoice", "ActionSource"]);
});

function sharedUnionFixture(roots: string[]) {
    const definitions: Record<string, JSONSchema7> = {
        SharedError: {
            title: "SharedError",
            type: "object",
            additionalProperties: false,
            properties: { kind: { const: "error" }, message: { type: "string" } },
        },
    };
    const server: Record<string, unknown> = {};
    for (const root of roots) {
        definitions[root] = {
            anyOf: [
                { $ref: "#/definitions/SharedError" },
                { type: "object", additionalProperties: false, properties: { kind: { const: "ok" } } },
            ],
        };
        server[root] = { rpcMethod: root, params: null, result: { $ref: `#/definitions/${root}` } };
    }
    return { definitions, server };
}

function rpcSource(files: Map<string, string>, name: string): string {
    const source = files.get(`sdk/src/generated/java/com/github/copilot/generated/rpc/${name}.java`);
    assert.ok(source, `missing ${name}.java`);
    return source;
}

for (const roots of [
    ["FirstResult", "HistoricalResult"],
    ["FirstResult", "HistoricalResult", "LastResult"],
]) {
    test(`${roots.length} shared roots preserve historical ownership regardless of discovery order`, async () => {
        const owners = { SharedError: "HistoricalResult" };
        const fixture = sharedUnionFixture(roots);
        const files = await renderRpcTypes(fixture, owners);
        assert.match(rpcSource(files, "SharedError"), /class SharedError extends HistoricalResult/);
        for (const root of roots.filter((name) => name !== "HistoricalResult")) {
            assert.match(rpcSource(files, `${root}SharedError`), new RegExp(`class ${root}SharedError extends ${root}`));
            assert.match(rpcSource(files, root), new RegExp(`value = ${root}SharedError.class, name = "error"`));
        }
        const reversed = sharedUnionFixture([...roots].reverse());
        reversed.definitions = Object.fromEntries(Object.entries(reversed.definitions).reverse());
        assert.deepEqual(
            [...(await renderRpcTypes(reversed, owners))].sort(),
            [...files].sort()
        );
        for (const root of roots) reversed.definitions[root].anyOf?.reverse();
        const reordered = await renderRpcTypes(reversed, owners);
        for (const [file, source] of files) {
            const updated = reordered.get(file);
            assert.ok(updated);
            // Membership ordering is not API identity; retain the schema's
            // annotation order without changing ownership or output names.
            assert.deepEqual(updated.split("\n").map((line) => line.replace(/,$/, "")).sort(),
                source.split("\n").map((line) => line.replace(/,$/, "")).sort());
        }
    });
}

test("RPC planning rejects unregistered shared ownership and missing historical owners", async () => {
    const fixture = sharedUnionFixture(["FirstResult", "SecondResult"]);
    await assert.rejects(renderRpcTypes(fixture, {}), /Ambiguous Java RPC owner for "SharedError"/);
    await assert.rejects(renderRpcTypes(fixture, { SharedError: "MissingResult" }), /Missing historical Java RPC owner/);
    await assert.rejects(renderRpcTypes(fixture, { RemovedVariant: "FirstResult" }), /Missing historical Java RPC owner/);
    // A failed plan must not poison the next generation.
    assert.ok((await renderRpcTypes(fixture, { SharedError: "FirstResult" })).size > 0);
});

test("RPC planning rejects incompatible shared schemas and contextual name collisions", async () => {
    const incompatible = sharedUnionFixture(["FirstResult", "SecondResult"]);
    incompatible.definitions.SecondResult.anyOf = [
        {
            ...incompatible.definitions.SharedError,
            properties: { kind: { const: "error" }, message: { type: "number" } },
        },
        { type: "object", properties: { kind: { const: "ok" } } },
    ];
    await assert.rejects(renderRpcTypes(incompatible, { SharedError: "FirstResult" }), /Incompatible schemas.*SharedError/);

    const collision = sharedUnionFixture(["FirstResult", "SecondResult"]);
    collision.definitions.FirstResult.anyOf?.push({
        title: "SecondResultSharedError", type: "object", properties: { kind: { const: "extra" } },
    });
    await assert.rejects(renderRpcTypes(collision, { SharedError: "FirstResult" }), /Conflicting Java RPC contextual name/);

    const rootCollision = sharedUnionFixture(["SharedError", "SecondResult"]);
    // Avoid the fixture's self-reference: a concrete title collides with a root.
    rootCollision.definitions.SharedError.anyOf = [
        { title: "SharedError", type: "object", properties: { kind: { const: "error" } } },
        { type: "object", properties: { kind: { const: "ok" } } },
    ];
    delete rootCollision.server.SecondResult;
    await assert.rejects(renderRpcTypes(rootCollision, {}), /Conflicting Java RPC variant name "SharedError"/);
});

test("nested standalone union memberships participate in the ownership plan", async () => {
    const fixture = sharedUnionFixture(["FirstResult", "NestedResult"]);
    delete fixture.server.NestedResult;
    fixture.definitions.SharedError.properties!.detail = {
        type: "array", items: { $ref: "#/definitions/NestedResult" },
    };
    const files = await renderRpcTypes(fixture, { SharedError: "FirstResult" });
    assert.match(rpcSource(files, "NestedResultSharedError"), /extends NestedResult/);
    assert.match(rpcSource(files, "SharedError"), /extends FirstResult/);
});

test("identical repeated root references emit each variant only once", async () => {
    const fixture = sharedUnionFixture(["FirstResult"]);
    const original = await renderRpcTypes(fixture, {});
    fixture.server.alias = { rpcMethod: "alias", params: null, result: { $ref: "#/definitions/FirstResult" } };
    assert.deepEqual([...(await renderRpcTypes(fixture, {}))].sort(), [...original].sort());
});

test("historical ABI registry retains the complete pre-intake ownership set", () => {
    assert.equal(Object.keys(RPC_VARIANT_OWNERS).length, 61);
    assert.equal(new Set(Object.values(RPC_VARIANT_OWNERS)).size, 12);
    for (const name of ["CatalogNegotiationRefusedError", "CatalogInvalidRequestError", "CatalogUnavailableError"]) {
        assert.equal(RPC_VARIANT_OWNERS[name], "CatalogSearchResult");
    }
});
