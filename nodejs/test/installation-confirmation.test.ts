import { once } from "node:events";
import { readFileSync } from "node:fs";
import { createServer, type Socket } from "node:net";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import {
    CancellationTokenSource,
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
    type MessageConnection,
} from "vscode-jsonrpc/node.js";
import {
    CopilotClient,
    RuntimeConnection,
    type InstallationConfirmationContext,
    type InstallationConfirmationHandler,
    type InstallationConfirmationRequest,
    type InstallationDecision,
} from "../src/index.js";

function request(operation: string): InstallationConfirmationRequest {
    const value: InstallationConfirmationRequest = JSON.parse(
        readFileSync(
            new URL("../../rust/tests/fixtures/installation_confirmation.json", import.meta.url),
            "utf8"
        )
    );
    return {
        ...value,
        operationId: operation,
        confirmationId: `challenge-${operation}`,
        reviewFingerprint: `fingerprint-${operation}`,
    };
}

function deferred<T>() {
    let resolve!: (value: T) => void;
    const promise = new Promise<T>((complete) => {
        resolve = complete;
    });
    return { promise, resolve };
}

async function connect(handler?: InstallationConfirmationHandler) {
    const accepted = deferred<{ connection: MessageConnection; socket: Socket }>();
    const server = createServer((socket) => {
        const connection = createMessageConnection(
            new StreamMessageReader(socket),
            new StreamMessageWriter(socket)
        );
        connection.onRequest("connect", () => ({
            ok: true,
            protocolVersion: 3,
            version: "confirmation-transport-test",
        }));
        connection.listen();
        accepted.resolve({ connection, socket });
    });
    server.listen(0, "127.0.0.1");
    await once(server, "listening");
    const address = server.address();
    if (!address || typeof address === "string") throw new Error("Missing test listener");
    const client = new CopilotClient({
        connection: RuntimeConnection.forUri(`127.0.0.1:${address.port}`),
        installationConfirmationHandler: handler,
        autoStart: false,
    });
    onTestFinished(async () => {
        await client.forceStop();
        const peer = await accepted.promise;
        peer.connection.dispose();
        peer.socket.destroy();
        await new Promise<void>((resolve) => server.close(() => resolve()));
    });
    await client.start();
    return { client, ...(await accepted.promise) };
}

describe("installation confirmation on the actual client transport", () => {
    it("presents typed full review and echoes the original challenge and fingerprint", async () => {
        const handler = vi.fn<InstallationConfirmationHandler>(async (incoming, context) => {
            expect(context.requestCancelled.isCancellationRequested).toBe(false);
            expect(context.connectionClosed.isCancellationRequested).toBe(false);
            expect(incoming).toEqual(request("a"));
            expect(incoming.review.review.action).toBe("install");
            if (incoming.review.review.action !== "install") throw new Error("Unexpected review");
            expect(incoming.review.review.effectiveConfiguration).toEqual({
                transport: "streamable-http",
                url: "https://example.test/mcp",
                headers: { "X-Region": "eu" },
                tools: ["*"],
            });
            incoming.confirmationId = "mutated-by-handler";
            incoming.reviewFingerprint = "mutated-by-handler";
            return "confirm";
        });
        const { connection } = await connect(handler);

        await expect(
            connection.sendRequest("installations.confirm", request("a"))
        ).resolves.toEqual({
            confirmationId: "challenge-a",
            reviewFingerprint: "fingerprint-a",
            decision: "confirm",
        });
        expect(handler).toHaveBeenCalledOnce();
    });

    it("keeps concurrent same-session reviews independent and allows out-of-order decisions", async () => {
        const a = deferred<InstallationDecision>();
        const b = deferred<InstallationDecision>();
        const handler = vi.fn<InstallationConfirmationHandler>((incoming) =>
            incoming.operationId === "a" ? a.promise : b.promise
        );
        const { connection } = await connect(handler);
        const first = connection.sendRequest("installations.confirm", request("a"));
        const second = connection.sendRequest("installations.confirm", request("b"));
        await vi.waitFor(() => expect(handler).toHaveBeenCalledTimes(2));
        await expect(
            connection.sendRequest("gitHubToken.getToken", {
                registrationId: "unknown",
                host: "github.com",
                reason: "initial",
            })
        ).rejects.toThrow("No GitHub token provider");

        b.resolve("decline");
        await expect(second).resolves.toEqual({
            confirmationId: "challenge-b",
            reviewFingerprint: "fingerprint-b",
            decision: "decline",
        });
        a.resolve("confirm");
        await expect(first).resolves.toMatchObject({ confirmationId: "challenge-a" });
    });

    it("retires cancelled A before a late decision and does not rebind it to successor B", async () => {
        const cancellation = new CancellationTokenSource();
        onTestFinished(() => cancellation.dispose());
        const a = deferred<InstallationDecision>();
        const contexts = new Map<string, InstallationConfirmationContext>();
        const handler: InstallationConfirmationHandler = (incoming, context) => {
            contexts.set(incoming.operationId, context);
            return incoming.operationId === "a" ? a.promise : "confirm";
        };
        const { connection } = await connect(handler);
        const first = connection.sendRequest(
            "installations.confirm",
            request("a"),
            cancellation.token
        );
        const cancelled = expect(first).rejects.toMatchObject({ code: -32800 });
        await vi.waitFor(() => expect(contexts.has("a")).toBe(true));
        cancellation.cancel();
        await cancelled;
        expect(contexts.get("a")?.requestCancelled.isCancellationRequested).toBe(true);
        expect(contexts.get("a")?.connectionClosed.isCancellationRequested).toBe(false);
        a.resolve("confirm");
        await expect(
            connection.sendRequest("installations.confirm", request("b"))
        ).resolves.toMatchObject({
            confirmationId: "challenge-b",
            reviewFingerprint: "fingerprint-b",
        });
        expect(contexts.get("b")?.requestCancelled.isCancellationRequested).toBe(false);
    });

    it("distinguishes connection loss from request cancellation across original connections", async () => {
        const pending = deferred<InstallationDecision>();
        let original: InstallationConfirmationContext | undefined;
        const first = await connect((_request, context) => {
            original = context;
            return pending.promise;
        });
        const second = await connect(() => "decline");
        const response = first.connection.sendRequest("installations.confirm", request("a"));
        const closed = expect(response).rejects.toThrow();
        await vi.waitFor(() => expect(original).toBeDefined());
        first.socket.destroy();
        await vi.waitFor(() =>
            expect(original?.connectionClosed.isCancellationRequested).toBe(true)
        );
        expect(original?.requestCancelled.isCancellationRequested).toBe(false);
        first.connection.dispose();
        await closed;
        pending.resolve("confirm");
        await expect(
            second.connection.sendRequest("installations.confirm", request("b"))
        ).resolves.toMatchObject({
            confirmationId: "challenge-b",
            decision: "decline",
        });
    });

    it("refuses missing handlers and propagates explicit handler refusal", async () => {
        const missing = await connect();
        await expect(
            missing.connection.sendRequest("installations.confirm", request("a"))
        ).rejects.toThrow("No installations client-global handler registered");
        const refusing = await connect(() => {
            throw new Error("Unknown original operation or incomplete review");
        });
        await expect(
            refusing.connection.sendRequest("installations.confirm", request("a"))
        ).rejects.toThrow("Unknown original operation or incomplete review");
    });

    it("does not infer optional legacy session authority", async () => {
        const incoming = request("legacy");
        delete incoming.policySessionId;
        const handler = vi.fn<InstallationConfirmationHandler>((params) => {
            expect(params.policySessionId).toBeUndefined();
            return "decline";
        });
        const { connection } = await connect(handler);
        await expect(
            connection.sendRequest("installations.confirm", incoming)
        ).resolves.toMatchObject({
            decision: "decline",
        });
    });
});
