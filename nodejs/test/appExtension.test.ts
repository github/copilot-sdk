import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { afterEach, describe, expect, it, vi } from "vitest";
import { CancellationTokenSource } from "vscode-jsonrpc/node.js";
import {
    defineAppExtension,
    type AppExtensionHost,
    type AppSessionBadgesContribution,
    type AppSessionBadgesHost,
} from "../src/appExtension.js";
import type {
    AppSessionBadgesExtension,
    AppSessionBadgesSnapshot,
} from "../src/appSessionBadges.js";
import {
    onExtensionTransportClosedSymbol,
    registerPrivateAppCanvasSymbol,
    registerPrivateAppExtensionSymbol,
    registerPrivateAppForgeProviderSymbol,
    requestPrivateAppMediatedFetchSymbol,
    unregisterPrivateAppCanvasSymbol,
    unregisterPrivateAppForgeProviderSymbol,
} from "../src/appExtensionClientAccess.js";
import { CopilotClient } from "../src/client.js";
import type { CopilotSession } from "../src/session.js";

describe("defineAppExtension", () => {
    const originalSessionId = process.env.SESSION_ID;

    afterEach(() => {
        if (originalSessionId === undefined) {
            delete process.env.SESSION_ID;
        } else {
            process.env.SESSION_ID = originalSessionId;
        }
        vi.restoreAllMocks();
    });

    function arrange() {
        process.env.SESSION_ID = "hidden-app-session";
        const session = {
            disconnect: vi.fn().mockResolvedValue(undefined),
            clientSessionApis: {},
        } as unknown as CopilotSession;
        vi.spyOn(CopilotClient.prototype, "resumeSessionForExtension").mockResolvedValue(session);
        vi.spyOn(CopilotClient.prototype, registerPrivateAppExtensionSymbol).mockResolvedValue({
            protocolVersion: 1,
            principal: {
                packageId: "bundled:github-app:badges",
                activationId: "activation-7",
            },
            capabilities: { sessionBadges: true },
            contributions: [
                {
                    contributionPoint: "sessionBadges",
                    contributionId: "github-pr",
                },
            ],
        });
        vi.spyOn(CopilotClient.prototype, "stop").mockResolvedValue([]);
        vi.spyOn(CopilotClient.prototype, registerPrivateAppCanvasSymbol).mockResolvedValue();
        vi.spyOn(CopilotClient.prototype, unregisterPrivateAppCanvasSymbol).mockResolvedValue();
        vi.spyOn(
            CopilotClient.prototype,
            registerPrivateAppForgeProviderSymbol
        ).mockResolvedValue();
        vi.spyOn(
            CopilotClient.prototype,
            unregisterPrivateAppForgeProviderSymbol
        ).mockResolvedValue();
        vi.spyOn(CopilotClient.prototype, requestPrivateAppMediatedFetchSymbol).mockResolvedValue({
            status: 200,
            headers: {},
            truncated: false,
        });
        let closeTransport: (() => void) | undefined;
        vi.spyOn(CopilotClient.prototype, onExtensionTransportClosedSymbol).mockImplementation(
            (handler) => {
                closeTransport = handler;
                return () => {
                    closeTransport = undefined;
                };
            }
        );
        return { session, closeTransport: () => closeTransport?.() };
    }

    it("authenticates before activation and exposes only capability-limited host fields", async () => {
        arrange();
        let hostKeys: string[] = [];
        const activation = await defineAppExtension((host) => {
            hostKeys = Object.keys(host).sort();
            expect(host.principal).toEqual({
                packageId: "bundled:github-app:badges",
                activationId: "activation-7",
            });
            expect(host.capabilities).toEqual({ sessionBadges: true });
            expect(host.signal.aborted).toBe(false);
            expect(host).not.toHaveProperty("session");
            expect(host).not.toHaveProperty("client");
            expect(host).not.toHaveProperty("rpc");
            expect(host).not.toHaveProperty("credentials");
            expect(host).not.toHaveProperty("fetch");
        });

        expect(hostKeys).toEqual([
            "canvases",
            "capabilities",
            "contributions",
            "forgeProviders",
            "mediatedFetch",
            "principal",
            "sessionBadges",
            "signal",
        ]);
        expect(Object.keys(activation).sort()).toEqual(["dispose", "principal", "signal"]);
        expect(activation).not.toHaveProperty("client");
        expect(activation).not.toHaveProperty("session");
        expect(activation).not.toHaveProperty("sessionBadges");
        expect(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).toHaveBeenCalledOnce();
        await activation.dispose();
    });

    it("registers app canvases, strips routing identity, and unregisters on disposal", async () => {
        const { session } = arrange();
        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockResolvedValueOnce(
            {
                protocolVersion: 1,
                principal: {
                    packageId: "bundled:github-app:canvas",
                    activationId: "activation-canvas",
                },
                capabilities: { canvases: true },
                contributions: [
                    {
                        contributionPoint: "canvases",
                        contributionId: "repository-overview",
                    },
                ],
            }
        );
        const onOpen = vi.fn().mockReturnValue({
            state: { selected: 1 },
            title: "Repository",
            status: "Ready",
        });
        const onAction = vi.fn().mockReturnValue({ selected: 2 });
        const onClose = vi.fn();
        let host: AppExtensionHost | undefined;
        const activation = await defineAppExtension(async (value) => {
            host = value;
            await value.canvases.register({
                contributionId: value.contributions[0]!.contributionId,
                onOpen,
                onAction,
                onClose,
            });
        });

        expect(CopilotClient.prototype[registerPrivateAppCanvasSymbol]).toHaveBeenCalledWith(
            "repository-overview"
        );
        const context = {
            projectId: "project-1",
            workspaceId: "workspace-1",
            project: {
                forgeProviderId: "github",
                repositoryLocator: { owner: "github", repo: "copilot-sdk" },
                forgeAccountId: "account-1",
            },
        };
        await expect(
            session.clientSessionApis.appCanvas!.open({
                sessionId: "hidden-app-session",
                protocolVersion: 1,
                contributionId: "repository-overview",
                instanceId: "canvas-1",
                input: { tab: "pulls" },
                context,
            })
        ).resolves.toEqual({
            state: { selected: 1 },
            title: "Repository",
            status: "Ready",
        });
        expect(onOpen).toHaveBeenCalledWith({
            instanceId: "canvas-1",
            input: { tab: "pulls" },
            context,
            signal: expect.any(AbortSignal),
        });
        expect(onOpen.mock.calls[0]![0]).not.toHaveProperty("sessionId");

        await expect(
            session.clientSessionApis.appCanvas!.invoke({
                sessionId: "hidden-app-session",
                protocolVersion: 1,
                contributionId: "repository-overview",
                instanceId: "canvas-1",
                actionName: "select",
                input: 2,
                context,
            })
        ).resolves.toEqual({ selected: 2 });
        await session.clientSessionApis.appCanvas!.close({
            sessionId: "hidden-app-session",
            protocolVersion: 1,
            contributionId: "repository-overview",
            instanceId: "canvas-1",
            context,
        });
        expect(onAction.mock.calls[0]![0]).not.toHaveProperty("sessionId");
        expect(onClose.mock.calls[0]![0]).not.toHaveProperty("sessionId");
        expect(host).not.toHaveProperty("session");
        expect(host).not.toHaveProperty("client");

        await activation.dispose();
        expect(CopilotClient.prototype[unregisterPrivateAppCanvasSymbol]).toHaveBeenCalledWith(
            "repository-overview"
        );
        expect(session.clientSessionApis.appCanvas).toBeUndefined();
    });

    it("registers forge operations and mediates bounded credential-free fetch", async () => {
        const { session } = arrange();
        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockResolvedValueOnce(
            {
                protocolVersion: 1,
                principal: {
                    packageId: "bundled:github-app:forge",
                    activationId: "activation-forge",
                },
                capabilities: { forgeProvider: true, mediatedFetch: true },
                contributions: [
                    {
                        contributionPoint: "forgeProvider",
                        contributionId: "github",
                    },
                ],
            }
        );
        const getPullRequest = vi.fn().mockReturnValue({ number: 2574 });
        const listPullRequests = vi.fn().mockReturnValue([]);
        const fetch = vi
            .mocked(CopilotClient.prototype[requestPrivateAppMediatedFetchSymbol])
            .mockResolvedValue({
                status: 200,
                headers: { "content-type": "application/json" },
                body: '{"number":2574}',
                truncated: false,
            });
        let host: AppExtensionHost | undefined;
        const activation = await defineAppExtension(async (value) => {
            host = value;
            const contributionId = value.contributions[0]!.contributionId;
            await value.forgeProviders.register({
                contributionId,
                operations: { getPullRequest, listPullRequests },
            });
        });
        const contributionId = host!.contributions[0]!.contributionId;

        expect(CopilotClient.prototype[registerPrivateAppForgeProviderSymbol]).toHaveBeenCalledWith(
            "github",
            ["getPullRequest", "listPullRequests"]
        );
        await expect(
            session.clientSessionApis.appForgeProvider!.invoke({
                sessionId: "hidden-app-session",
                protocolVersion: 1,
                contributionId: "github",
                operation: "getPullRequest",
                accountId: "account-1",
                input: { number: 2574 },
            })
        ).resolves.toEqual({ number: 2574 });
        expect(getPullRequest).toHaveBeenCalledWith({
            operation: "getPullRequest",
            accountId: "account-1",
            input: { number: 2574 },
            signal: expect.any(AbortSignal),
        });
        expect(getPullRequest.mock.calls[0]![0]).not.toHaveProperty("sessionId");

        await expect(
            host!.mediatedFetch.request({
                contributionId,
                accountId: "account-1",
                operation: "getPullRequest",
                method: "GET",
                path: "/repos/github/copilot-sdk/pulls/2574",
                headers: { Accept: "application/json" },
            })
        ).resolves.toEqual({
            status: 200,
            headers: { "content-type": "application/json" },
            body: '{"number":2574}',
            truncated: false,
        });
        expect(fetch).toHaveBeenCalledWith(
            {
                protocolVersion: 1,
                contributionId: "github",
                accountId: "account-1",
                operation: "getPullRequest",
                request: {
                    method: "GET",
                    path: "/repos/github/copilot-sdk/pulls/2574",
                    headers: { Accept: "application/json" },
                },
            },
            expect.any(AbortSignal)
        );
        await expect(
            host!.mediatedFetch.request({
                contributionId,
                accountId: "account-1",
                operation: "getPullRequest",
                method: "GET",
                path: "https://api.github.com/repos/github/copilot-sdk",
            })
        ).rejects.toThrow("root-relative URL path");
        await expect(
            host!.mediatedFetch.request({
                contributionId,
                accountId: "account-1",
                operation: "getPullRequest",
                method: "GET",
                path: "/repos/github/copilot-sdk",
                headers: { Authorization: "secret" },
            })
        ).rejects.toThrow("header Authorization is not permitted");

        await activation.dispose();
        expect(
            CopilotClient.prototype[unregisterPrivateAppForgeProviderSymbol]
        ).toHaveBeenCalledWith("github");
        expect(session.clientSessionApis.appForgeProvider).toBeUndefined();
    });

    it("requires a live forge registration and aborts in-flight provider callbacks", async () => {
        const { session } = arrange();
        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockResolvedValueOnce(
            {
                protocolVersion: 1,
                principal: {
                    packageId: "bundled:github-app:forge",
                    activationId: "activation-forge",
                },
                capabilities: { forgeProvider: true, mediatedFetch: true },
                contributions: [
                    {
                        contributionPoint: "forgeProvider",
                        contributionId: "github",
                    },
                ],
            }
        );
        let host: AppExtensionHost | undefined;
        let callbackSignal: AbortSignal | undefined;
        let finishCallback: ((value: object) => void) | undefined;
        const activation = await defineAppExtension(async (value) => {
            host = value;
            await value.forgeProviders.register({
                contributionId: value.contributions[0]!.contributionId,
                operations: {
                    pending: ({ signal }) =>
                        new Promise((resolve) => {
                            callbackSignal = signal;
                            finishCallback = resolve;
                        }),
                },
            });
        });
        const contributionId = host!.contributions[0]!.contributionId;
        const invocation = session.clientSessionApis.appForgeProvider!.invoke({
            sessionId: "hidden-app-session",
            protocolVersion: 1,
            contributionId: "github",
            operation: "pending",
        });
        await vi.waitFor(() => expect(callbackSignal).toBeDefined());

        await activation.dispose();

        expect(callbackSignal!.aborted).toBe(true);
        await expect(
            host!.mediatedFetch.request({
                contributionId,
                accountId: "account-1",
                operation: "pending",
                method: "GET",
                path: "/user",
            })
        ).rejects.toThrow("must be registered before mediated fetch");
        finishCallback!({});
        await expect(invocation).resolves.toEqual({});
    });

    it("propagates peer cancellation to app canvas callbacks", async () => {
        const { session } = arrange();
        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockResolvedValueOnce(
            {
                protocolVersion: 1,
                principal: {
                    packageId: "bundled:github-app:canvas",
                    activationId: "activation-canvas",
                },
                capabilities: { canvases: true },
                contributions: [
                    {
                        contributionPoint: "canvases",
                        contributionId: "repository-overview",
                    },
                ],
            }
        );
        let callbackSignal: AbortSignal | undefined;
        let finishCallback: (() => void) | undefined;
        const activation = await defineAppExtension(async (host) => {
            await host.canvases.register({
                contributionId: host.contributions[0]!.contributionId,
                onOpen: ({ signal }) =>
                    new Promise((resolve) => {
                        callbackSignal = signal;
                        finishCallback = () => resolve({});
                    }),
                onAction: () => null,
            });
        });
        const cancellation = new CancellationTokenSource();
        const invocation = session.clientSessionApis.appCanvas!.open(
            {
                sessionId: "hidden-app-session",
                protocolVersion: 1,
                contributionId: "repository-overview",
                instanceId: "canvas-1",
            },
            cancellation.token
        );
        await vi.waitFor(() => expect(callbackSignal).toBeDefined());

        cancellation.cancel();

        expect(callbackSignal!.aborted).toBe(true);
        finishCallback!();
        await expect(invocation).resolves.toEqual({});
        cancellation.dispose();
        await activation.dispose();
    });

    it("unregisters a canvas whose registration completes after activation disposal", async () => {
        arrange();
        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockResolvedValueOnce(
            {
                protocolVersion: 1,
                principal: {
                    packageId: "bundled:github-app:canvas",
                    activationId: "activation-canvas",
                },
                capabilities: { canvases: true },
                contributions: [
                    {
                        contributionPoint: "canvases",
                        contributionId: "repository-overview",
                    },
                ],
            }
        );
        let finishRegistration: (() => void) | undefined;
        vi.mocked(CopilotClient.prototype[registerPrivateAppCanvasSymbol]).mockImplementation(
            () =>
                new Promise<void>((resolve) => {
                    finishRegistration = resolve;
                })
        );
        let registration: Promise<unknown> | undefined;
        const activation = await defineAppExtension((host) => {
            registration = host.canvases.register({
                contributionId: host.contributions[0]!.contributionId,
                onOpen: () => ({}),
                onAction: () => null,
            });
            void registration.catch(() => {});
        });
        await vi.waitFor(() => expect(finishRegistration).toBeDefined());

        const disposal = activation.dispose();
        finishRegistration!();

        await expect(registration).rejects.toThrow("disposed during registration");
        await disposal;
        expect(CopilotClient.prototype[unregisterPrivateAppCanvasSymbol]).toHaveBeenCalledWith(
            "repository-overview"
        );
    });

    it("retains remote registration state so failed unregister can be retried", async () => {
        arrange();
        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockResolvedValueOnce(
            {
                protocolVersion: 1,
                principal: {
                    packageId: "bundled:github-app:canvas",
                    activationId: "activation-canvas",
                },
                capabilities: { canvases: true },
                contributions: [
                    {
                        contributionPoint: "canvases",
                        contributionId: "repository-overview",
                    },
                ],
            }
        );
        vi.mocked(CopilotClient.prototype[unregisterPrivateAppCanvasSymbol])
            .mockRejectedValueOnce(new Error("temporary unregister failure"))
            .mockResolvedValueOnce(undefined);
        let registration: Awaited<ReturnType<AppExtensionHost["canvases"]["register"]>> | undefined;
        const activation = await defineAppExtension(async (host) => {
            registration = await host.canvases.register({
                contributionId: host.contributions[0]!.contributionId,
                onOpen: () => ({}),
                onAction: () => null,
            });
        });

        await expect(registration!.dispose()).rejects.toThrow("temporary unregister failure");
        await expect(registration!.dispose()).resolves.toBeUndefined();
        expect(CopilotClient.prototype[unregisterPrivateAppCanvasSymbol]).toHaveBeenCalledTimes(2);
        await activation.dispose();
    });

    it("registers one principal-owned badge contribution through the compatibility transport", async () => {
        arrange();
        const unsubscribe = vi.fn();
        const delegate = {
            snapshot: undefined,
            onSnapshot: vi.fn((handler: (snapshot: object) => void) => {
                handler({ protocolVersion: 1, revision: 1, sessions: [] });
                return unsubscribe;
            }),
            setBadge: vi.fn().mockResolvedValue(undefined),
            setBadges: vi.fn().mockResolvedValue(undefined),
            clearBadge: vi.fn().mockResolvedValue(undefined),
            dispose: vi.fn(),
        } as unknown as AppSessionBadgesExtension;
        vi.spyOn(CopilotClient.prototype, "registerAppSessionBadges").mockResolvedValue(delegate);
        let contribution: AppSessionBadgesContribution | undefined;
        const snapshotHandler = vi.fn();

        const activation = await defineAppExtension(async (host) => {
            const registered = await host.sessionBadges.register({
                onSnapshot: (_snapshot, identity) => {
                    expect(contribution).toBe(registered);
                    expect(identity.principal).toBe(host.principal);
                    expect(identity.contributionPoint).toBe("sessionBadges");
                    expect(identity.contributionId).toBe("github-pr");
                    snapshotHandler();
                },
            });
            contribution = registered;
            await registered.setBadges([
                {
                    workspaceId: "workspace-1",
                    sessionId: "session-1",
                    badge: null,
                },
            ]);
            contribution = registered;
        });

        expect(CopilotClient.prototype.registerAppSessionBadges).toHaveBeenCalledOnce();
        expect(delegate.setBadges).toHaveBeenCalledWith([
            {
                workspaceId: "workspace-1",
                sessionId: "session-1",
                badge: null,
            },
        ]);
        expect(contribution).not.toHaveProperty("session");
        expect(contribution).not.toHaveProperty("connection");
        await vi.waitFor(() => expect(snapshotHandler).toHaveBeenCalledOnce());
        await activation.dispose();
        expect(unsubscribe).toHaveBeenCalledOnce();
        expect(delegate.dispose).toHaveBeenCalledOnce();
    });

    it("aborts cancellation and runs cleanup once on explicit disposal", async () => {
        const { session } = arrange();
        let signal: AbortSignal | undefined;
        const disposer = vi.fn(() => {
            expect(signal?.aborted).toBe(true);
        });
        const activation = await defineAppExtension((host) => {
            signal = host.signal;
            return disposer;
        });

        await Promise.all([activation.dispose(), activation.dispose()]);

        expect(signal?.aborted).toBe(true);
        expect(disposer).toHaveBeenCalledOnce();
        expect(session.disconnect).toHaveBeenCalledOnce();
        expect(CopilotClient.prototype.stop).toHaveBeenCalledOnce();
    });

    it("supports object disposers", async () => {
        arrange();
        const disposer = { dispose: vi.fn().mockResolvedValue(undefined) };
        const activation = await defineAppExtension(() => disposer);

        await activation.dispose();

        expect(disposer.dispose).toHaveBeenCalledOnce();
    });

    it("cancels and disposes activation when the runtime transport closes", async () => {
        const arranged = arrange();
        const disposer = vi.fn();
        const activation = await defineAppExtension(() => disposer);

        arranged.closeTransport();
        await vi.waitFor(() => {
            expect(activation.signal.aborted).toBe(true);
            expect(disposer).toHaveBeenCalledOnce();
        });
        expect(arranged.session.disconnect).not.toHaveBeenCalled();
    });

    it("disposes late activation cleanup when transport closes during activation", async () => {
        const arranged = arrange();
        let finishActivation: ((disposer: () => void) => void) | undefined;
        const disposer = vi.fn();
        const activation = defineAppExtension(
            () =>
                new Promise<() => void>((resolve) => {
                    finishActivation = resolve;
                })
        );
        await vi.waitFor(() => {
            expect(finishActivation).toBeDefined();
        });

        arranged.closeTransport();
        finishActivation!(disposer);

        await expect(activation).rejects.toThrow("transport closed during activation");
        expect(disposer).toHaveBeenCalledOnce();
        expect(arranged.session.disconnect).not.toHaveBeenCalled();
    });

    it("rejects unavailable, duplicate, and malformed badge registrations before transport use", async () => {
        arrange();
        const registerDelegate = vi
            .spyOn(CopilotClient.prototype, "registerAppSessionBadges")
            .mockResolvedValue({
                dispose: vi.fn(),
            } as unknown as AppSessionBadgesExtension);
        let badgesHost: AppSessionBadgesHost | undefined;
        const activation = await defineAppExtension((host) => {
            badgesHost = host.sessionBadges;
        });

        await expect(badgesHost!.register({ onSnapshot: "invalid" } as never)).rejects.toThrow(
            "onSnapshot must be a function"
        );
        expect(registerDelegate).not.toHaveBeenCalled();

        const contribution = await badgesHost!.register();
        await expect(badgesHost!.register()).rejects.toThrow("already registered sessionBadges");
        expect(registerDelegate).toHaveBeenCalledOnce();
        contribution.dispose();
        await activation.dispose();
        await expect(badgesHost!.register()).rejects.toThrow(
            "app extension activation is disposed"
        );

        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockResolvedValue({
            protocolVersion: 1,
            principal: {
                packageId: "bundled:github-app:no-badges",
                activationId: "activation-8",
            },
            capabilities: {},
            contributions: [],
        });
        await defineAppExtension(async (host) => {
            await expect(host.sessionBadges.register()).rejects.toThrow(
                "not granted sessionBadges"
            );
        }).then((result) => result.dispose());
    });

    it("rejects concurrent badge registration and disposes a late delegate after cancellation", async () => {
        const arranged = arrange();
        let resolveDelegate: ((delegate: AppSessionBadgesExtension) => void) | undefined;
        const delegate = {
            dispose: vi.fn(),
        } as unknown as AppSessionBadgesExtension;
        vi.spyOn(CopilotClient.prototype, "registerAppSessionBadges").mockImplementation(
            () =>
                new Promise((resolve) => {
                    resolveDelegate = resolve;
                })
        );
        let badgesHost: AppSessionBadgesHost | undefined;
        const activation = await defineAppExtension((host) => {
            badgesHost = host.sessionBadges;
        });

        const firstRegistration = badgesHost!.register();
        await vi.waitFor(() => expect(resolveDelegate).toBeDefined());
        await expect(badgesHost!.register()).rejects.toThrow("already registering sessionBadges");

        arranged.closeTransport();
        resolveDelegate!(delegate);

        await expect(firstRegistration).rejects.toThrow("disposed during registration");
        expect(delegate.dispose).toHaveBeenCalledOnce();
        expect(activation.signal.aborted).toBe(true);
    });

    it("observes async snapshot callback failures without disposing the contribution", async () => {
        arrange();
        let notify: ((snapshot: AppSessionBadgesSnapshot) => void) | undefined;
        const delegate = {
            snapshot: undefined,
            onSnapshot: vi.fn((handler: (snapshot: AppSessionBadgesSnapshot) => void) => {
                notify = handler;
                return vi.fn();
            }),
            dispose: vi.fn(),
        } as unknown as AppSessionBadgesExtension;
        vi.spyOn(CopilotClient.prototype, "registerAppSessionBadges").mockResolvedValue(delegate);
        const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
        const callback = vi
            .fn()
            .mockRejectedValueOnce(new Error("snapshot failed"))
            .mockResolvedValue(undefined);
        let contribution: AppSessionBadgesContribution | undefined;
        const activation = await defineAppExtension(async (host) => {
            contribution = await host.sessionBadges.register({ onSnapshot: callback });
        });
        await vi.waitFor(() => expect(notify).toBeDefined());

        notify!({ protocolVersion: 1, revision: 1, sessions: [] });
        notify!({ protocolVersion: 1, revision: 2, sessions: [] });

        await vi.waitFor(() => {
            expect(callback).toHaveBeenCalledTimes(2);
            expect(consoleError).toHaveBeenCalledWith(
                "App session badge snapshot handler failed",
                expect.any(Error)
            );
        });
        expect(contribution).toBeDefined();
        expect(delegate.dispose).not.toHaveBeenCalled();
        await activation.dispose();
    });

    it("rejects malformed runtime identity and capability grants before activation", async () => {
        const { session } = arrange();
        const definition = vi.fn();
        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockResolvedValueOnce(
            {
                protocolVersion: 1,
                principal: {
                    packageId: "",
                    activationId: "activation-7",
                },
                capabilities: { sessionBadges: true },
                contributions: [
                    {
                        contributionPoint: "sessionBadges",
                        contributionId: "github-pr",
                    },
                ],
            }
        );

        await expect(defineAppExtension(definition)).rejects.toThrow(
            "principal.packageId must be a non-empty string"
        );
        expect(definition).not.toHaveBeenCalled();
        expect(session.disconnect).toHaveBeenCalledOnce();

        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockResolvedValueOnce(
            {
                protocolVersion: 1,
                principal: {
                    packageId: "bundled:github-app:badges",
                    activationId: "activation-8",
                },
                capabilities: { sessionBadges: false },
                contributions: [
                    {
                        contributionPoint: "sessionBadges",
                        contributionId: "github-pr",
                    },
                ],
            } as never
        );
        await expect(defineAppExtension(definition)).rejects.toThrow(
            "capabilities.sessionBadges must be true when present"
        );
        expect(definition).not.toHaveBeenCalled();
    });

    it("cleans up registrations when activation fails", async () => {
        arrange();
        const delegate = {
            dispose: vi.fn(),
        } as unknown as AppSessionBadgesExtension;
        vi.spyOn(CopilotClient.prototype, "registerAppSessionBadges").mockResolvedValue(delegate);

        await expect(
            defineAppExtension(async (host) => {
                await host.sessionBadges.register();
                throw new Error("activation failed");
            })
        ).rejects.toThrow("activation failed");

        expect(delegate.dispose).toHaveBeenCalledOnce();
        expect(CopilotClient.prototype.stop).toHaveBeenCalledOnce();
    });

    it("requires one trusted session badge declaration and rejects duplicate identities", async () => {
        arrange();
        const registerDelegate = vi.spyOn(CopilotClient.prototype, "registerAppSessionBadges");
        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockResolvedValueOnce(
            {
                protocolVersion: 1,
                principal: {
                    packageId: "bundled:github-app:badges",
                    activationId: "activation-zero",
                },
                capabilities: { sessionBadges: true },
                contributions: [],
            }
        );
        await defineAppExtension(async (host) => {
            await expect(host.sessionBadges.register()).rejects.toThrow(
                "exactly one sessionBadges contribution; received 0"
            );
        }).then((activation) => activation.dispose());
        expect(registerDelegate).not.toHaveBeenCalled();

        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockResolvedValueOnce(
            {
                protocolVersion: 1,
                principal: {
                    packageId: "bundled:github-app:badges",
                    activationId: "activation-multiple",
                },
                capabilities: { sessionBadges: true },
                contributions: [
                    { contributionPoint: "sessionBadges", contributionId: "github-pr" },
                    { contributionPoint: "sessionBadges", contributionId: "checks" },
                ],
            }
        );
        await defineAppExtension(async (host) => {
            await expect(host.sessionBadges.register()).rejects.toThrow(
                "exactly one sessionBadges contribution; received 2"
            );
        }).then((activation) => activation.dispose());
        expect(registerDelegate).not.toHaveBeenCalled();

        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockResolvedValueOnce(
            {
                protocolVersion: 1,
                principal: {
                    packageId: "bundled:github-app:badges",
                    activationId: "activation-duplicate",
                },
                capabilities: { sessionBadges: true },
                contributions: [
                    { contributionPoint: "sessionBadges", contributionId: "github-pr" },
                    { contributionPoint: "sessionBadges", contributionId: "github-pr" },
                ],
            }
        );
        await expect(defineAppExtension(() => undefined)).rejects.toThrow(
            "contributions contains duplicate identity sessionBadges/github-pr"
        );
    });

    it("rejects mediatedFetch as a contribution identity", async () => {
        arrange();
        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockResolvedValueOnce(
            {
                protocolVersion: 1,
                principal: {
                    packageId: "bundled:github-app:provider",
                    activationId: "activation-fetch",
                },
                capabilities: { forgeProvider: true, mediatedFetch: true },
                contributions: [
                    {
                        contributionPoint: "mediatedFetch" as never,
                        contributionId: "fetch",
                    },
                ],
            }
        );

        await expect(defineAppExtension(() => undefined)).rejects.toThrow(
            "contributions[0].contributionPoint is not supported"
        );
    });

    it("surfaces client stop errors from explicit disposal", async () => {
        arrange();
        vi.mocked(CopilotClient.prototype.stop).mockResolvedValue([
            new Error("runtime shutdown failed"),
        ]);
        const activation = await defineAppExtension(() => undefined);

        await expect(activation.dispose()).rejects.toThrow("Failed to dispose app extension");
    });

    it("rejects unauthenticated activation and disconnects without invoking user code", async () => {
        const { session } = arrange();
        vi.mocked(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).mockRejectedValue(
            new Error("not allowlisted")
        );
        const definition = vi.fn();

        await expect(defineAppExtension(definition)).rejects.toThrow("not allowlisted");

        expect(definition).not.toHaveBeenCalled();
        expect(session.disconnect).toHaveBeenCalledOnce();
        expect(CopilotClient.prototype.stop).toHaveBeenCalledOnce();
    });

    it("ships only through the explicit private package export", () => {
        const packageJson = JSON.parse(
            readFileSync(resolve(import.meta.dirname, "..", "package.json"), "utf8")
        ) as { exports: Record<string, unknown> };

        expect(packageJson.exports["./private/app-extension"]).toBeDefined();
        expect(packageJson.exports["."]).not.toHaveProperty("defineAppExtension");
        expect(packageJson.exports["./extension"]).not.toHaveProperty("defineAppExtension");
    });
});
