import { describe, expect, it, vi } from "vitest";
import { CopilotClient, RuntimeConnection, type GitHubTokenProvider } from "../src/index.js";

function createMockClient(
    request: (method: string, params: Record<string, unknown>) => Promise<unknown>
): CopilotClient {
    const client = new CopilotClient({
        connection: RuntimeConnection.forUri("localhost:1234"),
    });
    (client as unknown as { connection: unknown }).connection = {
        sendRequest: request,
        dispose: vi.fn(),
    };
    return client;
}

function getTokenHandler(client: CopilotClient) {
    return (
        client as unknown as {
            clientGlobalHandlers: {
                gitHubToken: {
                    getToken(params: {
                        registrationId: string;
                        host: string;
                        sessionId?: string;
                        reason: "initial" | "refresh";
                    }): Promise<unknown>;
                };
            };
        }
    ).clientGlobalHandlers.gitHubToken.getToken;
}

describe("session GitHub token providers", () => {
    it("rejects a static token and provider together", async () => {
        const client = new CopilotClient({
            connection: RuntimeConnection.forUri("localhost:1234"),
        });

        await expect(
            client.createSession({
                gitHubToken: "static",
                gitHubTokenProvider: async () => ({
                    kind: "token",
                    accessToken: "dynamic",
                    expiresIn: 28_800,
                }),
            })
        ).rejects.toThrow("gitHubToken and gitHubTokenProvider are mutually exclusive");
    });

    it("serializes only the opaque registration and maps token and cancellation results", async () => {
        let createPayload: Record<string, unknown> | undefined;
        const request = vi.fn(async (method: string, params: Record<string, unknown>) => {
            if (method === "session.create") {
                createPayload = params;
                return { sessionId: params.sessionId };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const observed: unknown[] = [];
        const provider: GitHubTokenProvider = vi
            .fn()
            .mockImplementationOnce(async (args) => {
                observed.push(args);
                return {
                    kind: "token",
                    accessToken: "secret-token",
                    tokenType: "Bearer",
                    expiresIn: 28_800,
                };
            })
            .mockImplementationOnce(async (args) => {
                observed.push(args);
                return { kind: "cancelled" };
            });
        const client = createMockClient(request);
        const session = await client.createSession({
            sessionId: "session-one",
            gitHubTokenProvider: provider,
        });

        const registrationId = createPayload?.gitHubTokenProviderRegistrationId;
        expect(registrationId).toMatch(
            /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/
        );
        expect(createPayload).not.toHaveProperty("gitHubTokenProvider");
        expect(createPayload?.gitHubToken).toBeUndefined();

        const handler = getTokenHandler(client);
        await expect(
            handler({
                registrationId: registrationId as string,
                host: "github.example.com",
                reason: "initial",
            })
        ).resolves.toEqual({
            kind: "token",
            accessToken: "secret-token",
            tokenType: "Bearer",
            expiresIn: 28_800,
        });
        await expect(
            handler({
                registrationId: registrationId as string,
                host: "github.example.com",
                sessionId: session.sessionId,
                reason: "refresh",
            })
        ).resolves.toEqual({ kind: "cancelled" });
        expect(observed).toEqual([
            {
                host: "github.example.com",
                sessionId: "session-one",
                reason: "initial",
            },
            {
                host: "github.example.com",
                sessionId: "session-one",
                reason: "refresh",
            },
        ]);
    });

    it("preserves callback errors and rejects unknown registrations", async () => {
        const client = createMockClient(async (_method, params) => ({
            sessionId: params.sessionId,
        }));
        const failure = new Error("credential broker failed");
        await client.createSession({
            sessionId: "error-session",
            gitHubTokenProvider: () => {
                throw failure;
            },
        });
        const registrationId = [
            ...(
                client as unknown as {
                    githubTokenProviders: Map<string, unknown>;
                }
            ).githubTokenProviders.keys(),
        ][0];
        const handler = getTokenHandler(client);

        await expect(
            handler({
                registrationId,
                host: "github.com",
                reason: "initial",
            })
        ).rejects.toBe(failure);
        await expect(
            handler({
                registrationId: "unknown",
                host: "github.com",
                reason: "refresh",
            })
        ).rejects.toThrow("No GitHub token provider registered");
    });

    it("rolls back failed creation and cleans up on session and client close", async () => {
        const failingClient = createMockClient(async () => {
            throw new Error("create failed");
        });
        await expect(
            failingClient.createSession({
                gitHubTokenProvider: async () => ({ kind: "cancelled" }),
            })
        ).rejects.toThrow("create failed");
        expect(
            (
                failingClient as unknown as {
                    githubTokenProviders: Map<string, unknown>;
                }
            ).githubTokenProviders
        ).toHaveLength(0);

        const client = createMockClient(async (method, params) => {
            if (method === "session.create") return { sessionId: params.sessionId };
            if (method === "session.detach") return { success: true };
            if (method === "session.delete") return { success: true };
            throw new Error(`Unexpected method: ${method}`);
        });
        const first = await client.createSession({
            sessionId: "first",
            gitHubTokenProvider: async () => ({ kind: "cancelled" }),
        });
        await client.createSession({
            sessionId: "second",
            gitHubTokenProvider: async () => ({ kind: "cancelled" }),
        });
        const registrations = (
            client as unknown as {
                githubTokenProviders: Map<string, unknown>;
            }
        ).githubTokenProviders;
        expect(registrations).toHaveLength(2);

        await first.disconnect();
        expect(registrations).toHaveLength(1);
        await client.deleteSession("second");
        expect(registrations).toHaveLength(0);
        await client.forceStop();
        expect(registrations).toHaveLength(0);
    });

    it("rotates a resumed session only after resume succeeds", async () => {
        const payloads: Record<string, unknown>[] = [];
        const client = createMockClient(async (method, params) => {
            payloads.push(params);
            if (method === "session.create" || method === "session.resume") {
                return { sessionId: params.sessionId };
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        const firstProvider = vi.fn(async () => ({ kind: "cancelled" as const }));
        const secondProvider = vi.fn(async () => ({ kind: "cancelled" as const }));
        await client.createSession({
            sessionId: "resumed",
            gitHubTokenProvider: firstProvider,
        });
        const firstRegistration = payloads[0].gitHubTokenProviderRegistrationId as string;

        await client.resumeSession("resumed", {
            gitHubTokenProvider: secondProvider,
        });
        const secondRegistration = payloads[1].gitHubTokenProviderRegistrationId as string;
        const handler = getTokenHandler(client);

        await expect(
            handler({
                registrationId: firstRegistration,
                host: "github.com",
                reason: "refresh",
            })
        ).rejects.toThrow("No GitHub token provider registered");
        await expect(
            handler({
                registrationId: secondRegistration,
                host: "github.com",
                reason: "refresh",
            })
        ).resolves.toEqual({ kind: "cancelled" });
        expect(secondProvider).toHaveBeenCalledOnce();
        expect(firstProvider).not.toHaveBeenCalled();
    });

    it("does not retire a concurrent pending registration", async () => {
        const resumeResolvers: Array<(value: { sessionId: string }) => void> = [];
        const payloads: Record<string, unknown>[] = [];
        const client = createMockClient(async (method, params) => {
            payloads.push(params);
            if (method === "session.create") {
                return { sessionId: params.sessionId };
            }
            if (method === "session.resume") {
                return await new Promise<{ sessionId: string }>((resolve) => {
                    resumeResolvers.push(resolve);
                });
            }
            throw new Error(`Unexpected method: ${method}`);
        });
        await client.createSession({
            sessionId: "concurrent",
            gitHubTokenProvider: async () => ({ kind: "cancelled" }),
        });

        const firstResume = client.resumeSession("concurrent", {
            gitHubTokenProvider: async () => ({ kind: "cancelled" }),
        });
        const secondResume = client.resumeSession("concurrent", {
            gitHubTokenProvider: async () => ({ kind: "cancelled" }),
        });
        await vi.waitFor(() => expect(resumeResolvers).toHaveLength(2));

        resumeResolvers[0]({ sessionId: "concurrent" });
        await firstResume;
        const registrations = (
            client as unknown as {
                githubTokenProviders: Map<string, unknown>;
            }
        ).githubTokenProviders;
        expect(registrations).toHaveLength(2);

        resumeResolvers[1]({ sessionId: "concurrent" });
        await secondResume;
        expect(registrations).toHaveLength(1);
        expect(registrations.has(payloads[2].gitHubTokenProviderRegistrationId as string)).toBe(
            true
        );
    });
});
