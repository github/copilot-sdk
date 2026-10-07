/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import type { JSONSchema7 } from "json-schema";

import {
    collectDirectDiscriminatedUnionTypeNames,
    collectNestedDiscriminatedUnionTypeNames,
    generateApiMethod,
    extractEventVariants,
    generateRpcClass,
    isMainModule,
    renderEventVariantClass,
    renderRpcTypes,
    renderRpcWrappers,
    schemaTypeToJava,
} from "./java.js";
import { RPC_VARIANT_OWNERS } from "./rpc-variant-owners.js";

test("arbitrary handoff maps accept scalar and structured JSON values", () => {
    const result = schemaTypeToJava(
        { type: "object", additionalProperties: true },
        true, "HostCreateSessionParams", "config", new Map(),
    );
    assert.equal(result.javaType, "Map<String, Object>");
    assert.ok(result.imports.has("java.util.Map"));
});

test("preserves whole-event deprecation through a referenced event envelope", () => {
    const schema: JSONSchema7 = {
        definitions: {
            SessionEvent: { anyOf: [{ $ref: "#/definitions/LegacyEvent" }] },
            LegacyEvent: {
                type: "object",
                deprecated: true,
                properties: {
                    type: { const: "tool.execution_partial_result" },
                    data: { $ref: "#/definitions/LegacyData" },
                },
            } as JSONSchema7,
            LegacyData: { type: "object", properties: { partialOutput: { type: "string" } } },
        },
    };
    const variants = extractEventVariants(schema);
    assert.equal(variants.length, 1);
    assert.equal(variants[0].deprecated, true);
    assert.match(renderEventVariantClass(variants[0], "com.github.copilot.generated"), /@Deprecated/);
});

for (const referencedData of [false, true]) {
    for (const eventDeprecated of [false, true]) {
        for (const dataDeprecated of [false, true]) {
            test(`preserves independent payload deprecation (referenced=${referencedData}, event=${eventDeprecated}, data=${dataDeprecated})`, () => {
                const dataSchema: JSONSchema7 & { deprecated: boolean } = {
                    type: "object",
                    deprecated: dataDeprecated,
                    properties: { partialOutput: { type: "string" } },
                };
                const eventSchema: JSONSchema7 & { deprecated: boolean } = {
                    type: "object",
                    deprecated: eventDeprecated,
                    properties: {
                        type: { const: "tool.execution_partial_result" },
                        data: referencedData ? { $ref: "#/definitions/LegacyData" } : dataSchema,
                    },
                };
                const schema: JSONSchema7 = {
                    definitions: {
                        SessionEvent: { anyOf: [{ $ref: "#/definitions/LegacyEvent" }] },
                        LegacyEvent: eventSchema,
                        LegacyData: dataSchema,
                    },
                };
                const variants = extractEventVariants(schema);
                assert.equal(variants.length, 1);
                const source = renderEventVariantClass(variants[0], "com.github.copilot.generated");
                const eventAnnotation = /@Deprecated\n(?:@[^\n]*\n)*public final class ToolExecutionPartialResultEvent\b/;
                const dataAnnotation = /    @Deprecated\n(?:    @[^\n]*\n)*    public record ToolExecutionPartialResultEventData\b/;
                if (eventDeprecated) assert.match(source, eventAnnotation);
                else assert.doesNotMatch(source, eventAnnotation);
                if (dataDeprecated) assert.match(source, dataAnnotation);
                else assert.doesNotMatch(source, dataAnnotation);
            });
        }
    }
}

test("preserves diagnostics configuration for session create and resume", async () => {
    const files = await renderRpcTypes({
        definitions: {
            DiagnosticsConfiguration: {
                type: "object",
                properties: { sources: { $ref: "#/definitions/DiagnosticSourcesConfiguration" } },
                required: ["sources"],
            },
            DiagnosticSourcesConfiguration: {
                type: "object",
                properties: { mcp: { type: "string" } },
            },
        },
    }, {});
    const configuration = [...files].find(([file]) => file.endsWith("/DiagnosticsConfiguration.java"));
    assert.ok(configuration, "startup diagnostics must have a named generated configuration");
    assert.match(configuration[1], /DiagnosticSourcesConfiguration sources/);
    assert.ok([...files.keys()].some((file) => file.endsWith("/DiagnosticSourcesConfiguration.java")));
});

test("customization reload types are experimental without opening unrelated types", async () => {
    const files = await renderRpcTypes({
        definitions: {
            CustomizationReloadStatus: { type: "string", enum: ["reloaded", "failed"] },
            CustomizationReloadSubsystem: { type: "string", enum: ["skills", "hooks"] },
            CustomizationReloadOutcome: {
                type: "object",
                description: "Result of one customization reload component.",
                properties: { status: { $ref: "#/definitions/CustomizationReloadStatus" } },
            },
            ClosedStatus: { type: "string", enum: ["ready"] },
            ExampleResult: {
                type: "object",
                properties: {
                    status: { $ref: "#/definitions/CustomizationReloadStatus" },
                    subsystem: { $ref: "#/definitions/CustomizationReloadSubsystem" },
                    outcome: { $ref: "#/definitions/CustomizationReloadOutcome" },
                    closed: { $ref: "#/definitions/ClosedStatus" },
                },
            },
        },
        server: {
            example: {
                rpcMethod: "example",
                params: null,
                result: { $ref: "#/definitions/ExampleResult" },
            },
        },
    }, {});
    for (const name of ["CustomizationReloadStatus", "CustomizationReloadSubsystem"]) {
        const source = [...files].find(([file]) => file.endsWith(`/${name}.java`))?.[1];
        assert.ok(source, `missing ${name}.java`);
        assert.match(source, /public static final \w+ UNKNOWN = new \w+\("unknown"\);/);
        assert.match(source, /if \(value != null\) return new \w+\(value\);/);
        assert.match(source, /@com\.fasterxml\.jackson\.annotation\.JsonValue/);
        assert.match(source, /import com\.github\.copilot\.CopilotExperimental;/);
        assert.match(source, /@apiNote This type is experimental and may change in a future version\./);
        assert.match(source, /@CopilotExperimental/);
    }
    const outcome = [...files].find(([file]) => file.endsWith("/CustomizationReloadOutcome.java"))?.[1];
    assert.ok(outcome, "missing CustomizationReloadOutcome.java");
    assert.match(outcome, /import com\.github\.copilot\.CopilotExperimental;/);
    assert.match(outcome, /@apiNote This type is experimental and may change in a future version\./);
    assert.match(outcome, /@CopilotExperimental/);
    const closed = [...files].find(([file]) => file.endsWith("/ClosedStatus.java"))?.[1];
    assert.ok(closed);
    assert.doesNotMatch(closed, /return UNKNOWN/);
    assert.doesNotMatch(closed, /@CopilotExperimental/);
});

test("recognizes an entrypoint reached through a linked directory", (t) => {
    const root = fs.mkdtempSync(path.join(os.tmpdir(), "copilot-java-codegen-entrypoint-"));
    t.after(() => fs.rmSync(root, { recursive: true, force: true }));
    const sourceDirectory = path.join(root, "source");
    const linkedDirectory = path.join(root, "linked");
    fs.mkdirSync(sourceDirectory);
    fs.writeFileSync(path.join(sourceDirectory, "java.ts"), "");
    fs.symlinkSync(sourceDirectory, linkedDirectory, process.platform === "win32" ? "junction" : "dir");

    assert.equal(
        isMainModule(
            path.join(linkedDirectory, "java.ts"),
            path.join(sourceDirectory, "java.ts"),
        ),
        true,
    );
});

function renderPayload(dataSchema: JSONSchema7): string {
    return renderEventVariantClass({
        typeName: "example.notification",
        className: "ExampleNotificationEvent",
        dataSchema,
    }, "com.github.copilot.generated");
}

test("experimental event properties retain their marker without marking the stable event", () => {
    const structuredContent: JSONSchema7 & { stability: string } = {
        description: "Client-only structured progress metadata.",
        stability: "experimental",
    };
    const source = renderPayload({
        type: "object",
        properties: {
            toolCallId: { type: "string" },
            progressMessage: { type: "string" },
            structuredContent,
        },
        required: ["toolCallId", "progressMessage"],
    });

    assert.match(source, /import com\.github\.copilot\.CopilotExperimental;/);
    assert.match(source, /@CopilotExperimental\s+@JsonProperty\("structuredContent"\) Object structuredContent/);
    assert.deepEqual(source.match(/@CopilotExperimental/g), ["@CopilotExperimental"]);
    assert.match(source, /@JsonProperty\("progressMessage"\) String progressMessage,/);
});

test("stable event properties do not acquire experimental markers", () => {
    const source = renderPayload({
        type: "object",
        properties: {
            progressMessage: { type: "string", description: "Experimental is ordinary text here." },
            structuredContent: { description: "Client-only structured progress metadata." },
        },
    });

    assert.match(source, /@JsonProperty\("structuredContent"\) Object structuredContent/);
    assert.doesNotMatch(source, /CopilotExperimental/);
});

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

test("session event fields promote directly referenced discriminated response unions", () => {
    const definitions: Record<string, JSONSchema7> = {
        TypedResponse: {
            anyOf: [
                {
                    type: "object",
                    properties: {
                        responseKind: { const: "answer" },
                        answer: { type: "string" },
                    },
                    required: ["responseKind", "answer"],
                },
                {
                    type: "object",
                    properties: {
                        responseKind: { const: "decision" },
                        approved: { type: "boolean" },
                    },
                    required: ["responseKind", "approved"],
                },
            ],
        },
    };
    const dataSchema: JSONSchema7 = {
        type: "object",
        properties: {
            response: { $ref: "#/definitions/TypedResponse" },
        },
    };
    const promoted = collectDirectDiscriminatedUnionTypeNames(
        dataSchema,
        definitions,
        new Set(["TypedResponse"]),
    );
    const standaloneTypes = new Map<string, JSONSchema7>();

    const result = schemaTypeToJava(
        dataSchema.properties!.response as JSONSchema7,
        false,
        "ExampleEventData",
        "response",
        new Map(),
        {
            definitions,
            standaloneTypes,
            promotedUnionTypes: promoted,
        },
    );

    assert.equal(result.javaType, "TypedResponse");
    assert.equal(standaloneTypes.get("TypedResponse"), definitions.TypedResponse);
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

test("RPC records promote nested discriminated union references to sealed interfaces", async () => {
    const definitions: Record<string, JSONSchema7> = {
        InstallationConfirmationRequest: {
            type: "object",
            additionalProperties: false,
            properties: {
                review: { $ref: "#/definitions/InstallationReview" },
            },
        },
        InstallationReview: {
            anyOf: [
                {
                    type: "object",
                    additionalProperties: false,
                    properties: {
                        resource: { type: "string", const: "mcp" },
                        review: { $ref: "#/definitions/McpInstallationReview" },
                    },
                    required: ["resource", "review"],
                },
            ],
        },
        McpInstallationReview: {
            anyOf: [
                {
                    type: "object",
                    additionalProperties: false,
                    properties: {
                        action: { type: "string", const: "install" },
                        selectedChoice: { $ref: "#/definitions/McpPlanTransportChoice" },
                    },
                    required: ["action", "selectedChoice"],
                },
                {
                    type: "object",
                    additionalProperties: false,
                    properties: {
                        action: { type: "string", const: "uninstall" },
                        installationId: { type: "string" },
                    },
                    required: ["action", "installationId"],
                },
            ],
        },
        McpInstallPlan: {
            type: "object",
            additionalProperties: false,
            properties: {
                transportChoices: {
                    type: "array",
                    items: { $ref: "#/definitions/McpPlanTransportChoice" },
                },
            },
        },
        McpPlanTransportChoice: {
            anyOf: [
                { $ref: "#/definitions/McpPlanTransportChoicePackage" },
                { $ref: "#/definitions/McpPlanTransportChoiceRemote" },
            ],
        },
        McpPlanTransportChoicePackage: {
            type: "object",
            title: "McpPlanTransportChoicePackage",
            additionalProperties: false,
            properties: {
                installMethod: { type: "string", const: "package" },
                packageIdentifier: { type: "string" },
            },
            required: ["installMethod", "packageIdentifier"],
        },
        McpPlanTransportChoiceRemote: {
            type: "object",
            title: "McpPlanTransportChoiceRemote",
            additionalProperties: false,
            properties: {
                installMethod: { type: "string", const: "remote" },
                endpoint: { type: "string" },
            },
            required: ["installMethod", "endpoint"],
        },
        McpInstallationManagementResult: {
            anyOf: [
                {
                    type: "object",
                    additionalProperties: false,
                    properties: {
                        kind: { type: "string", const: "outcome" },
                        outcome: { $ref: "#/definitions/McpInstallationManagementOutcome" },
                    },
                    required: ["kind", "outcome"],
                },
                {
                    type: "object",
                    additionalProperties: false,
                    properties: {
                        kind: { type: "string", const: "invalid-request" },
                        message: { type: "string" },
                    },
                    required: ["kind", "message"],
                },
            ],
        },
        McpInstallationManagementOutcome: {
            anyOf: [
                {
                    type: "object",
                    additionalProperties: false,
                    properties: {
                        kind: { type: "string", const: "operation" },
                        operation: { $ref: "#/definitions/McpInstallationOperationStatus" },
                    },
                    required: ["kind", "operation"],
                },
                {
                    type: "object",
                    additionalProperties: false,
                    properties: {
                        kind: { type: "string", const: "refused" },
                        reason: { type: "string" },
                    },
                    required: ["kind", "reason"],
                },
            ],
        },
        McpInstallationOperationStatus: {
            anyOf: [
                {
                    type: "object",
                    additionalProperties: false,
                    properties: {
                        phase: { type: "string", const: "preparing" },
                        operationId: { type: "string" },
                    },
                    required: ["phase", "operationId"],
                },
                {
                    type: "object",
                    additionalProperties: false,
                    properties: {
                        phase: { type: "string", const: "completed" },
                        operationId: { type: "string" },
                    },
                    required: ["phase", "operationId"],
                },
            ],
        },
    };

    const files = await renderRpcTypes({
        definitions,
        clientGlobal: {
            confirm: {
                rpcMethod: "confirm",
                params: { $ref: "#/definitions/InstallationConfirmationRequest" },
                result: null,
            },
            plan: {
                rpcMethod: "plan",
                params: { $ref: "#/definitions/McpInstallPlan" },
                result: null,
            },
        },
        server: {
            status: {
                rpcMethod: "status",
                params: null,
                result: { $ref: "#/definitions/McpInstallationManagementResult" },
            },
        },
    }, {});

    assert.match(rpcSource(files, "InstallationConfirmationRequest"), /@JsonProperty\("review"\) McpInstallationReview review/);
    assert.match(rpcSource(files, "McpInstallPlan"), /@JsonProperty\("transportChoices"\) List<McpPlanTransportChoice> transportChoices/);
    assert.match(rpcSource(files, "McpInstallationManagementResultOutcome"), /private McpInstallationManagementOutcome outcome/);
    assert.match(rpcSource(files, "McpInstallationManagementOutcomeOperation"), /@JsonProperty\("operation"\) McpInstallationOperationStatus operation/);
    assert.match(rpcSource(files, "McpInstallationReview"), /public sealed interface McpInstallationReview permits McpInstallationReviewInstall, McpInstallationReviewUninstall/);
    assert.match(rpcSource(files, "McpInstallationReview"), /JsonTypeInfo\.Id\.NAME, include = JsonTypeInfo\.As\.EXISTING_PROPERTY, property = "action", visible = true/);
    assert.match(rpcSource(files, "McpInstallationReviewInstall"), /public record McpInstallationReviewInstall\(/);
    assert.match(rpcSource(files, "McpInstallationReviewInstall"), /action = "install"/);
    assert.match(rpcSource(files, "McpInstallationReviewInstall"), /import com\.fasterxml\.jackson\.annotation\.JsonTypeInfo;/);
    assert.match(rpcSource(files, "McpInstallationReviewInstall"), /@JsonTypeInfo\(use = JsonTypeInfo\.Id\.NONE\)/);
    assert.match(rpcSource(files, "McpPlanTransportChoice"), /property = "installMethod"/);
    assert.match(rpcSource(files, "McpInstallationOperationStatus"), /property = "phase"/);
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

test("instructions reload reuses getSources result without generating a duplicate Java type", async () => {
    const fixture = {
        definitions: {
            InstructionsGetSourcesResult: {
                type: "object",
                properties: { sources: { type: "array", items: { type: "string" } } },
            },
            OtherResult: {
                type: "object",
                properties: { sources: { type: "array", items: { type: "string" } } },
            },
        },
        session: {
            instructions: {
                getSources: {
                    rpcMethod: "session.instructions.getSources",
                    params: null,
                    result: { $ref: "#/definitions/InstructionsGetSourcesResult" },
                },
                reload: {
                    rpcMethod: "session.instructions.reload",
                    params: null,
                    result: { $ref: "#/definitions/InstructionsGetSourcesResult" },
                },
            },
        },
    };

    const files = await renderRpcTypes(fixture, {});
    assert.match(rpcSource(files, "SessionInstructionsGetSourcesResult"), /List<String> sources/);
    assert.equal([...files.keys()].some((file) => file.endsWith("/SessionInstructionsReloadResult.java")), false);

    fixture.session.instructions.reload.result.$ref = "#/definitions/OtherResult";
    await assert.rejects(renderRpcTypes(fixture, {}), /Incompatible Java RPC result alias/);
});

test("customizations reload uses its shared schema result name in types and wrapper", async () => {
    const fixture = {
        definitions: {
            CustomizationsReloadResult: {
                type: "object",
                properties: { warnings: { type: "array", items: { type: "string" } } },
            },
            OtherResult: {
                type: "object",
                properties: { warnings: { type: "array", items: { type: "string" } } },
            },
        },
        session: {
            customizations: {
                reload: {
                    rpcMethod: "session.customizations.reload",
                    params: null,
                    result: { $ref: "#/definitions/CustomizationsReloadResult" },
                },
            },
        },
    };

    const types = await renderRpcTypes(fixture, {});
    assert.match(rpcSource(types, "CustomizationsReloadResult"), /List<String> warnings/);
    assert.equal([...types.keys()].some((file) => file.endsWith("/SessionCustomizationsReloadResult.java")), false);
    const wrappers = await renderRpcWrappers(fixture);
    assert.match(rpcSource(wrappers, "SessionCustomizationsApi"), /CompletableFuture<CustomizationsReloadResult> reload\(/);

    fixture.session.customizations.reload.result.$ref = "#/definitions/OtherResult";
    await assert.rejects(renderRpcTypes(fixture, {}), /Incompatible Java RPC result name/);
    await assert.rejects(renderRpcWrappers(fixture), /Incompatible Java RPC result name/);
});

test("static OAuth config preserves the legacy four-argument constructor", () => {
    const typeName = "McpOauthRequiredStaticClientConfig";
    const source = generateRpcClass(typeName, {
        type: "object",
        additionalProperties: false,
        properties: {
            clientId: { type: "string" },
            clientSecret: { type: ["string", "null"] },
            publicClient: { type: ["boolean", "null"] },
            grantType: { type: ["string", "null"] },
            scope: { type: ["string", "null"] },
        },
        required: ["clientId"],
    }, new Map(), "com.github.copilot.generated").code;
    assert.match(
        source,
        /public McpOauthRequiredStaticClientConfig\(\s*String clientId,\s*String clientSecret,\s*Boolean publicClient,\s*String grantType\s*\) \{\s*this\(clientId, clientSecret, publicClient, grantType, null\);/s
    );
});

test("nullable referenced RPC results retain their object DTO and typed wrapper", async () => {
    const method = {
        rpcMethod: "session.accounts.getCurrent",
        stability: "experimental",
        params: {
            type: "object",
            properties: { sessionId: { type: "string" } },
            required: ["sessionId"],
        } as JSONSchema7,
        result: { $ref: "#/definitions/SessionAccountResult" },
    };
    const files = await renderRpcTypes({
        session: { accounts: { getCurrent: method } },
        definitions: {
            SessionAccountResult: {
                title: "SessionAccountResult",
                anyOf: [{ $ref: "#/definitions/SessionAccount" }, { type: "null" }],
            },
            SessionAccount: {
                type: "object",
                title: "SessionAccount",
                additionalProperties: false,
                properties: {
                    accountId: { type: "string" },
                    identity: { $ref: "#/definitions/Identity" },
                },
                required: ["accountId", "identity"],
                stability: "experimental",
            } as JSONSchema7,
            Identity: {
                type: "object",
                title: "Identity",
                properties: { login: { type: "string" } },
                required: ["login"],
            },
        },
    }, {});
    assert.match(rpcSource(files, "SessionAccount"), /public record SessionAccount\(/);
    assert.match(rpcSource(files, "SessionAccount"), /@JsonProperty\("identity"\) Identity identity/);
    assert.match(rpcSource(files, "Identity"), /@JsonProperty\("login"\) String login/);
    const wrapper = generateApiMethod("getCurrent", method, true, "this.sessionId").lines.join("\n");
    assert.match(wrapper, /CompletableFuture<SessionAccount> getCurrent\(\)/);
    assert.match(wrapper, /caller\.invoke\("session\.accounts\.getCurrent", .*SessionAccount\.class\)/);
    assert.doesNotMatch(wrapper, /Void/);
});
test("session filesystem capabilities preserve the legacy one-argument constructor", () => {
    const source = generateRpcClass("SessionFsSetProviderCapabilities", {
        type: "object",
        properties: {
            sqlite: { type: "boolean" },
            binary: { type: "boolean" },
        },
    }, new Map(), "com.github.copilot.generated").code;
    assert.match(
        source,
        /public SessionFsSetProviderCapabilities\(\s*Boolean sqlite\s*\) \{\s*this\(sqlite, null\);/s
    );
});

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

type LegacyScope = "server" | "session";

/** A request published with `contract`, `source` and `scope`, then extended with optional fields. */
function legacyRequestFixture(additions: 0 | 1 | 2, scope: LegacyScope = "server", legacy = ["contract", "source", "scope"]) {
    const properties: Record<string, JSONSchema7> = {
        ...(scope === "session" ? { sessionId: { type: "string" } } : {}),
        contract: { type: "string", description: "Caller contract." },
        source: { type: "string" },
        scope: { type: "string" },
    };
    if (additions >= 1) properties.policySessionId = { type: "string" };
    if (additions >= 2) properties.traceId = { type: "string" };
    const request: JSONSchema7 & Record<string, unknown> = {
        type: "object",
        properties,
        required: [...(scope === "session" ? ["sessionId"] : []), "contract", "source"],
        additionalProperties: false,
    };
    if (additions > 0) request["x-legacy-parameters"] = legacy;
    const rpcMethod = `${scope === "session" ? "session." : ""}sample.plan`;
    return {
        [scope]: {
            sample: {
                plan: {
                    rpcMethod,
                    params: { $ref: "#/definitions/SamplePlanRequest" },
                    result: { type: "object", properties: { ok: { type: "boolean" } }, required: ["ok"] },
                },
            },
        },
        definitions: { SamplePlanRequest: request },
    } as Parameters<typeof renderRpcTypes>[0];
}

function generatedFile(files: Map<string, string>, className: string): string | undefined {
    return [...files].find(([file]) => file.endsWith(`/${className}.java`))?.[1];
}

async function renderLegacy(additions: 0 | 1 | 2, scope: LegacyScope = "server") {
    const fixture = legacyRequestFixture(additions, scope);
    const types = await renderRpcTypes(fixture, {});
    const wrappers = await renderRpcWrappers(fixture);
    const prefix = scope === "session" ? "Session" : "";
    return {
        params: generatedFile(types, `${prefix}SamplePlanParams`)!,
        request: generatedFile(types, "SamplePlanRequest"),
        api: generatedFile(wrappers, `${scope === "session" ? "Session" : "Server"}SampleApi`)!,
    };
}

function publicConstructor(source: string): string | undefined {
    return source.match(/public SamplePlanRequest\([^)]*\)/)?.[0];
}

test("x-legacy-parameters keeps the params record and adds a fluent request with a same-name overload", async () => {
    const original = await renderLegacy(0);
    assert.equal(original.request, undefined, "unmarked requests keep their existing generation");
    assert.doesNotMatch(original.api, /SamplePlanRequest/);

    const once = await renderLegacy(1);
    assert.equal(once.params, original.params, "the params record keeps exactly its legacy components");
    assert.ok(once.request);
    assert.match(once.request, /public final class SamplePlanRequest \{/);
    assert.equal(publicConstructor(once.request), "public SamplePlanRequest(String contract, String source)");
    assert.match(once.request, /this\.contract = Objects\.requireNonNull\(contract, "contract"\);/);
    assert.match(once.request, /public SamplePlanRequest setScope\(String value\)/);
    assert.match(once.request, /public SamplePlanRequest setPolicySessionId\(String value\)/);
    assert.match(once.request, /@JsonProperty\("policySessionId"\)\s+private String policySessionId;/);
    assert.match(once.api, /public CompletableFuture<SamplePlanResult> plan\(SamplePlanParams params\) \{\s+return caller\.invoke\("sample\.plan", params,/);
    assert.match(once.api, /public CompletableFuture<SamplePlanResult> plan\(SamplePlanRequest request\) \{\s+return caller\.invoke\("sample\.plan", Objects\.requireNonNull\(request, "request"\),/);

    const twice = await renderLegacy(2);
    assert.equal(twice.params, original.params, "a second optional addition leaves the record unchanged");
    assert.equal(publicConstructor(twice.request!), publicConstructor(once.request));
    assert.match(twice.request!, /public SamplePlanRequest setTraceId\(String value\)/);
    const overloads = (api: string) => api.match(/public CompletableFuture<SamplePlanResult> plan\([^)]*\)/g);
    assert.deepEqual(overloads(twice.api), overloads(once.api));
});

test("x-legacy-parameters session requests keep sessionId injected by the wrapper", async () => {
    const original = await renderLegacy(0, "session");
    const once = await renderLegacy(1, "session");
    assert.equal(once.params, original.params);
    assert.match(once.params, /@JsonProperty\("sessionId"\) String sessionId/);
    assert.doesNotMatch(once.request!, /sessionId/);
    assert.match(
        once.api,
        /plan\(SamplePlanRequest request\) \{\s+com\.fasterxml\.jackson\.databind\.node\.ObjectNode _p = MAPPER\.valueToTree\(Objects\.requireNonNull\(request, "request"\)\);\s+_p\.put\("sessionId", this\.sessionId\);\s+return caller\.invoke\("session\.sample\.plan", _p,/
    );
});

test("trusted human response ingress stays package-private", async () => {
    const fixture = {
        session: {
            ui: {
                handlePendingUserInput: {
                    rpcMethod: "session.ui.handlePendingUserInput",
                    visibility: "public",
                    params: { type: "object", properties: { sessionId: { type: "string" } } },
                    result: { type: "object", properties: { success: { type: "boolean" } } },
                },
                handleHumanUserInput: {
                    rpcMethod: "session.ui.handleHumanUserInput",
                    visibility: "internal",
                    params: { type: "object", properties: { sessionId: { type: "string" } } },
                    result: { type: "object", properties: { success: { type: "boolean" } } },
                },
                handleHumanExitPlanMode: {
                    rpcMethod: "session.ui.handleHumanExitPlanMode",
                    visibility: "internal",
                    params: { type: "object", properties: { sessionId: { type: "string" } } },
                    result: { type: "object", properties: { success: { type: "boolean" } } },
                },
            },
        },
        definitions: {},
    } as Parameters<typeof renderRpcWrappers>[0];

    const wrappers = await renderRpcWrappers(fixture);
    const api = generatedFile(wrappers, "SessionUiApi")!;
    assert.match(api, /public CompletableFuture<SessionUiHandlePendingUserInputResult> handlePendingUserInput\(/);
    assert.match(api, /\n    CompletableFuture<SessionUiHandleHumanUserInputResult> handleHumanUserInput\(/);
    assert.match(api, /\n    CompletableFuture<SessionUiHandleHumanExitPlanModeResult> handleHumanExitPlanMode\(/);
    assert.doesNotMatch(api, /public CompletableFuture<SessionUiHandleHuman(?:UserInput|ExitPlanMode)Result>/);
});

test("x-legacy-parameters rejects metadata that would not preserve the original API", async () => {
    await assert.rejects(
        renderRpcTypes(legacyRequestFixture(1, "server", ["contract", "scope"]), {}),
        /Invalid x-legacy-parameters for sample\.plan: required property source must be a legacy parameter/
    );
    await assert.rejects(
        renderRpcTypes(legacyRequestFixture(1, "server", ["contract", "source", "missing"]), {}),
        /unknown property missing/
    );
    await assert.rejects(
        renderRpcTypes(legacyRequestFixture(1, "session", ["sessionId", "contract", "source"]), {}),
        /implicit property sessionId cannot be a legacy parameter/
    );
    const client = legacyRequestFixture(1) as Record<string, unknown>;
    client.clientSession = client.server;
    delete client.server;
    await assert.rejects(renderRpcTypes(client as Parameters<typeof renderRpcTypes>[0], {}), /only server and session requests are supported/);
});

/** A response record published with `name`, `status` and `error`, then extended with optional fields. */
function legacyRecordSchema(additions: string[], legacy: string[] | undefined = ["name", "status", "error"]): JSONSchema7 {
    const properties: Record<string, JSONSchema7> = {
        name: { type: "string", description: "Server name." },
        status: { type: "string" },
    };
    for (const addition of additions.filter((name) => name === "owned")) properties[addition] = { type: "string" };
    properties.error = { type: "string" };
    for (const addition of additions.filter((name) => name !== "owned")) properties[addition] = { type: "string" };
    const schema: JSONSchema7 & Record<string, unknown> = { type: "object", properties, required: ["name", "status"] };
    if (additions.length > 0 && legacy) schema["x-legacy-parameters"] = legacy;
    return schema;
}

function recordSource(schema: JSONSchema7): string {
    return generateRpcClass("SampleServer", schema, new Map(), "com.github.copilot.generated.rpc").code;
}

function legacyConstructors(source: string): string[] {
    return [...source.matchAll(/public SampleServer\(([^)]*)\) \{\s*this\(([^)]*)\);/g)].map(
        (match) => `${match[1].replace(/\s+/g, " ").trim()} => ${match[2]}`
    );
}

test("x-legacy-parameters response records keep every component and add the previous constructor", () => {
    const original = recordSource(legacyRecordSchema([]));
    assert.deepEqual(legacyConstructors(original), [], "unmarked records keep their existing generation");

    const trailing = recordSource(legacyRecordSchema(["traceId"]));
    assert.match(trailing, /record SampleServer\(\s*\/\*\* Server name\. \*\/\s*@JsonProperty\("name"\) String name,\s*@JsonProperty\("status"\) String status,\s*@JsonProperty\("error"\) String error,\s*@JsonProperty\("traceId"\) String traceId\s*\)/);
    assert.deepEqual(legacyConstructors(trailing), ["String name, String status, String error => name, status, error, null"]);

    const middle = recordSource(legacyRecordSchema(["owned"]));
    assert.deepEqual(legacyConstructors(middle), ["String name, String status, String error => name, status, null, error"]);

    const twice = recordSource(legacyRecordSchema(["owned", "traceId"]));
    assert.deepEqual(legacyConstructors(twice), ["String name, String status, String error => name, status, null, error, null"]);
});

test("x-legacy-parameters response records reject metadata a positional constructor cannot preserve", () => {
    assert.throws(
        () => recordSource(legacyRecordSchema(["traceId"], ["status", "name", "error"])),
        /Invalid x-legacy-parameters for SampleServer: legacy parameters must follow schema property order/
    );
    assert.throws(
        () => recordSource(legacyRecordSchema(["traceId"], ["name", "error"])),
        /required property status must be a legacy parameter/
    );
});

test("x-legacy-parameters response records get the constructor through standalone generation", async () => {
    const files = await renderRpcTypes({
        server: {
            sample: {
                list: {
                    rpcMethod: "sample.list",
                    params: null,
                    result: {
                        type: "object",
                        properties: { servers: { type: "array", items: { $ref: "#/definitions/SampleServer" } } },
                        required: ["servers"],
                    },
                },
            },
        },
        definitions: { SampleServer: legacyRecordSchema(["owned"]) },
    } as Parameters<typeof renderRpcTypes>[0], {});
    assert.deepEqual(legacyConstructors(rpcSource(files, "SampleServer")), [
        "String name, String status, String error => name, status, null, error",
    ]);
});
