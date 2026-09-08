import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
    defineAppExtension,
    type AppSessionBadgesContribution,
    type AppSessionBadgesHost,
} from "../src/appExtension.js";
import type {
    AppSessionBadgesExtension,
    AppSessionBadgesSnapshot,
} from "../src/appSessionBadges.js";
import {
    onExtensionTransportClosedSymbol,
    registerPrivateAppExtensionSymbol,
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
        } as unknown as CopilotSession;
        vi.spyOn(CopilotClient.prototype, "resumeSessionForExtension").mockResolvedValue(session);
        vi.spyOn(CopilotClient.prototype, registerPrivateAppExtensionSymbol).mockResolvedValue({
            protocolVersion: 1,
            principal: {
                packageId: "bundled:github-app:badges",
                activationId: "activation-7",
            },
            capabilities: { sessionBadges: true },
        });
        vi.spyOn(CopilotClient.prototype, "stop").mockResolvedValue([]);
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

        expect(hostKeys).toEqual(["capabilities", "principal", "sessionBadges", "signal"]);
        expect(Object.keys(activation).sort()).toEqual(["dispose", "principal", "signal"]);
        expect(activation).not.toHaveProperty("client");
        expect(activation).not.toHaveProperty("session");
        expect(activation).not.toHaveProperty("sessionBadges");
        expect(CopilotClient.prototype[registerPrivateAppExtensionSymbol]).toHaveBeenCalledOnce();
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
                    expect(identity.contributionId).toBe("default");
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
        const disposer = vi.fn();
        let signal: AbortSignal | undefined;
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
