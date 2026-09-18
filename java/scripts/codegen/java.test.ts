import assert from "node:assert/strict";
import test from "node:test";
import type { JSONSchema7 } from "json-schema";

import {
    collectNestedDiscriminatedUnionTypeNames,
    renderEventVariantClass,
    schemaTypeToJava,
} from "./java.js";

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
