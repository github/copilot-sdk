/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { CopilotClient } from "./client.js";
import type { CopilotSession } from "./session.js";
import {
    type AppSessionBadge,
    type AppSessionBadgesSnapshot,
    type AppSessionBadgeTargetIdentity,
    type AppSessionBadgeUpdate,
    type AppSessionBadgesExtension,
} from "./appSessionBadges.js";
import {
    onExtensionTransportClosedSymbol,
    registerPrivateAppExtensionSymbol,
} from "./appExtensionClientAccess.js";
import { joinExtensionSession } from "./extensionSession.js";

const APP_EXTENSION_PROTOCOL_VERSION = 1 as const;

declare const packageIdBrand: unique symbol;
declare const activationIdBrand: unique symbol;
declare const contributionIdBrand: unique symbol;

/** Opaque identity for a bundled or allowlisted app-extension package. */
export type AppExtensionPackageId = string & { readonly [packageIdBrand]: never };

/** Opaque identity for one runtime-authenticated launch generation. */
export type AppExtensionActivationId = string & { readonly [activationIdBrand]: never };

/** Opaque identity for one contribution owned by an activation principal. */
export type AppExtensionContributionId = string & {
    readonly [contributionIdBrand]: never;
};

/** Capability contribution point declared by a trusted app-extension manifest. */
export type AppExtensionContributionPoint = "sessionBadges" | "canvases" | "forgeProvider";

/** Runtime-authenticated identity of one statically declared contribution. */
export interface AppExtensionDeclaredContribution {
    readonly contributionPoint: AppExtensionContributionPoint;
    readonly contributionId: AppExtensionContributionId;
}

/** Runtime-authenticated package and activation identity. */
export interface AppExtensionPrincipal {
    readonly packageId: AppExtensionPackageId;
    readonly activationId: AppExtensionActivationId;
}

/** Capability grants bound to the authenticated principal. */
export interface AppExtensionCapabilityGrants {
    readonly sessionBadges?: true;
    readonly canvases?: true;
    readonly forgeProvider?: true;
    readonly mediatedFetch?: true;
}

/** Identity attached to a capability registration. */
export interface AppExtensionContributionIdentity {
    readonly principal: AppExtensionPrincipal;
    readonly contributionPoint: "sessionBadges";
    readonly contributionId: AppExtensionContributionId;
}

/** Future app-canvas contribution declaration. No canvas host is exposed in v1. */
export interface AppCanvasContributionDeclaration {
    readonly contributionPoint: "canvases";
    readonly protocolVersion: 1;
}

/** Future forge-provider declaration. No provider host is exposed in v1. */
export interface AppForgeProviderContributionDeclaration {
    readonly contributionPoint: "forgeProvider";
    readonly protocolVersion: 1;
}

/** Extensible declaration union for capability-specific registration APIs. */
export type AppExtensionContributionDeclaration =
    AppCanvasContributionDeclaration | AppForgeProviderContributionDeclaration;

/** Principal-aware callback for replacement badge snapshots. */
export type AppSessionBadgesRegistrationHandler = (
    snapshot: AppSessionBadgesSnapshot,
    identity: AppExtensionContributionIdentity
) => void | Promise<void>;

/** Options for registering the activation's single badge contribution. */
export interface AppSessionBadgesRegistration {
    readonly onSnapshot?: AppSessionBadgesRegistrationHandler;
}

/** Capability-limited badge contribution owned by an app-extension principal. */
export interface AppSessionBadgesContribution {
    readonly identity: AppExtensionContributionIdentity;
    readonly snapshot: AppSessionBadgesSnapshot | undefined;
    onSnapshot(handler: AppSessionBadgesRegistrationHandler): () => void;
    setBadge(target: AppSessionBadgeTargetIdentity, badge: AppSessionBadge | null): Promise<void>;
    setBadges(updates: readonly AppSessionBadgeUpdate[]): Promise<void>;
    clearBadge(target: AppSessionBadgeTargetIdentity): Promise<void>;
    dispose(): void;
}

/** Registration surface for the session-badge capability. */
export interface AppSessionBadgesHost {
    register(options?: AppSessionBadgesRegistration): Promise<AppSessionBadgesContribution>;
}

/**
 * Private capability-limited host supplied to an allowlisted app extension.
 *
 * This object deliberately contains no session, client, raw JSON-RPC,
 * credential, generic mutation, or unrestricted network surface.
 */
export interface AppExtensionHost {
    readonly principal: AppExtensionPrincipal;
    readonly capabilities: AppExtensionCapabilityGrants;
    readonly signal: AbortSignal;
    readonly sessionBadges: AppSessionBadgesHost;
}

/** Cleanup returned by an app-extension activation callback. */
export type AppExtensionDisposer =
    (() => void | Promise<void>) | { dispose(): void | Promise<void> };

/** App-extension activation callback. */
export type AppExtensionDefinition = (
    host: AppExtensionHost
) => void | AppExtensionDisposer | Promise<void | AppExtensionDisposer>;

/** Lifecycle handle returned after a private app extension activates. */
export interface AppExtensionActivation {
    readonly principal: AppExtensionPrincipal;
    readonly signal: AbortSignal;
    dispose(): Promise<void>;
}

class SessionBadgesContribution implements AppSessionBadgesContribution {
    readonly identity: AppExtensionContributionIdentity;
    #delegate: AppSessionBadgesExtension;
    #subscriptions = new Set<() => void>();
    #disposed = false;

    constructor(
        principal: AppExtensionPrincipal,
        contributionId: AppExtensionContributionId,
        delegate: AppSessionBadgesExtension
    ) {
        this.identity = Object.freeze({
            principal,
            contributionPoint: "sessionBadges",
            contributionId,
        });
        this.#delegate = delegate;
    }

    get snapshot(): AppSessionBadgesSnapshot | undefined {
        return this.#delegate.snapshot;
    }

    onSnapshot(handler: AppSessionBadgesRegistrationHandler): () => void {
        this.assertActive();
        const unsubscribe = this.#delegate.onSnapshot((snapshot) => {
            try {
                Promise.resolve(handler(snapshot, this.identity)).catch((error) => {
                    console.error("App session badge snapshot handler failed", error);
                });
            } catch (error) {
                console.error("App session badge snapshot handler failed", error);
            }
        });
        this.#subscriptions.add(unsubscribe);
        return () => {
            if (this.#subscriptions.delete(unsubscribe)) {
                unsubscribe();
            }
        };
    }

    async setBadge(
        target: AppSessionBadgeTargetIdentity,
        badge: AppSessionBadge | null
    ): Promise<void> {
        this.assertActive();
        await this.#delegate.setBadge(target, badge);
    }

    async setBadges(updates: readonly AppSessionBadgeUpdate[]): Promise<void> {
        this.assertActive();
        await this.#delegate.setBadges(updates);
    }

    async clearBadge(target: AppSessionBadgeTargetIdentity): Promise<void> {
        this.assertActive();
        await this.#delegate.clearBadge(target);
    }

    dispose(): void {
        if (this.#disposed) return;
        this.#disposed = true;
        for (const unsubscribe of this.#subscriptions) {
            unsubscribe();
        }
        this.#subscriptions.clear();
        this.#delegate.dispose();
    }

    deferSnapshotHandler(handler: AppSessionBadgesRegistrationHandler): void {
        const timeout = setTimeout(() => {
            this.#subscriptions.delete(cancel);
            if (!this.#disposed) {
                this.onSnapshot(handler);
            }
        }, 0);
        const cancel = () => clearTimeout(timeout);
        this.#subscriptions.add(cancel);
    }

    private assertActive(): void {
        if (this.#disposed) {
            throw new Error("App session badge contribution is disposed");
        }
    }
}

class SessionBadgesRegistrar implements AppSessionBadgesHost {
    #registration: SessionBadgesContribution | undefined;
    #registering = false;
    #disposed = false;

    constructor(
        private readonly principal: AppExtensionPrincipal,
        private readonly granted: boolean,
        private readonly declaredContributions: readonly AppExtensionDeclaredContribution[],
        private readonly registerDelegate: () => Promise<AppSessionBadgesExtension>
    ) {}

    async register(
        options: AppSessionBadgesRegistration = {}
    ): Promise<AppSessionBadgesContribution> {
        if (this.#disposed) {
            throw new Error("The app extension activation is disposed");
        }
        if (options === null || typeof options !== "object") {
            throw new TypeError("sessionBadges.register options must be an object");
        }
        if (options.onSnapshot !== undefined && typeof options.onSnapshot !== "function") {
            throw new TypeError("sessionBadges.register onSnapshot must be a function");
        }
        if (!this.granted) {
            throw new Error("The app extension principal was not granted sessionBadges");
        }
        const declarations = this.declaredContributions.filter(
            (contribution) => contribution.contributionPoint === "sessionBadges"
        );
        if (declarations.length !== 1) {
            throw new Error(
                `The app extension must declare exactly one sessionBadges contribution; received ${declarations.length}`
            );
        }
        if (this.#registration) {
            throw new Error("The app extension already registered sessionBadges");
        }
        if (this.#registering) {
            throw new Error("The app extension is already registering sessionBadges");
        }
        this.#registering = true;
        let delegate: AppSessionBadgesExtension | undefined;
        let contribution: SessionBadgesContribution | undefined;
        try {
            delegate = await this.registerDelegate();
            if (this.#disposed) {
                delegate.dispose();
                throw new Error("The app extension activation was disposed during registration");
            }
            contribution = new SessionBadgesContribution(
                this.principal,
                declarations[0]!.contributionId,
                delegate
            );
            this.#registration = contribution;
            if (options.onSnapshot) {
                contribution.deferSnapshotHandler(options.onSnapshot);
            }
            return contribution;
        } catch (error) {
            if (contribution) {
                contribution.dispose();
                if (this.#registration === contribution) {
                    this.#registration = undefined;
                }
            } else if (delegate && !this.#disposed) {
                delegate.dispose();
            }
            throw error;
        } finally {
            this.#registering = false;
        }
    }

    dispose(): void {
        if (this.#disposed) return;
        this.#disposed = true;
        this.#registration?.dispose();
        this.#registration = undefined;
    }
}

class AppExtensionRuntime {
    readonly signal: AbortSignal;
    readonly principal: AppExtensionPrincipal;
    readonly #abortController = new AbortController();
    #disposer: AppExtensionDisposer | undefined;
    #disposed = false;
    #disposePromise: Promise<void> | undefined;
    #removeTransportCloseHandler: () => void = () => {};
    readonly #client: CopilotClient;
    readonly #session: CopilotSession;
    readonly #sessionBadges: SessionBadgesRegistrar;

    constructor(
        principal: AppExtensionPrincipal,
        client: CopilotClient,
        session: CopilotSession,
        sessionBadges: SessionBadgesRegistrar
    ) {
        this.principal = principal;
        this.#client = client;
        this.#session = session;
        this.#sessionBadges = sessionBadges;
        this.signal = this.#abortController.signal;
        this.#removeTransportCloseHandler = this.#client[onExtensionTransportClosedSymbol](() => {
            void this.#dispose(false).catch((error) => {
                console.error("App extension transport cleanup failed", error);
            });
        });
    }

    async adoptDisposer(disposer: void | AppExtensionDisposer): Promise<void> {
        if (this.#disposed) {
            if (disposer !== undefined) {
                await invokeDisposer(disposer);
            }
            throw new Error("App extension transport closed during activation");
        }
        if (disposer !== undefined) {
            this.#disposer = disposer;
        }
    }

    activation(): AppExtensionActivation {
        return Object.freeze({
            principal: this.principal,
            signal: this.signal,
            dispose: () => this.#dispose(true),
        });
    }

    cleanupAfterActivationFailure(): Promise<void> {
        return this.#dispose(true);
    }

    #dispose(disconnect: boolean): Promise<void> {
        if (!this.#disposePromise) {
            this.#disposePromise = this.#disposeOnce(disconnect);
        }
        return this.#disposePromise;
    }

    async #disposeOnce(disconnect: boolean): Promise<void> {
        if (this.#disposed) return;
        this.#disposed = true;
        this.#removeTransportCloseHandler();
        this.#abortController.abort();
        this.#sessionBadges.dispose();

        const errors: unknown[] = [];
        try {
            await invokeDisposer(this.#disposer);
        } catch (error) {
            errors.push(error);
        }
        if (disconnect) {
            try {
                await this.#session.disconnect();
            } catch (error) {
                errors.push(error);
            }
            try {
                errors.push(...(await this.#client.stop()));
            } catch (error) {
                errors.push(error);
            }
        }
        if (errors.length > 0) {
            throw new AggregateError(errors, "Failed to dispose app extension");
        }
    }
}

/**
 * Define and activate a private bundled app extension.
 *
 * The runtime authenticates the package and launch generation before the
 * callback runs. Unallowlisted or legacy extension connections are rejected.
 *
 * @internal This entry point is intended only for app-bundled packages.
 */
export async function defineAppExtension(
    definition: AppExtensionDefinition
): Promise<AppExtensionActivation> {
    if (typeof definition !== "function") {
        throw new TypeError("defineAppExtension requires an activation function");
    }

    const { client, session } = await joinExtensionSession({});
    let runtime: AppExtensionRuntime | undefined;
    try {
        const registration = await client[registerPrivateAppExtensionSymbol]();
        const principal = parsePrincipal(registration);
        const capabilities = parseCapabilities(registration.capabilities);
        const contributions = parseContributions(registration.contributions);
        const sessionBadges = new SessionBadgesRegistrar(
            principal,
            capabilities.sessionBadges === true,
            contributions,
            () => client.registerAppSessionBadges(session)
        );
        runtime = new AppExtensionRuntime(principal, client, session, sessionBadges);
        const host: AppExtensionHost = Object.freeze({
            principal,
            capabilities,
            signal: runtime.signal,
            sessionBadges: Object.freeze({
                register: sessionBadges.register.bind(sessionBadges),
            }),
        });
        await runtime.adoptDisposer(await definition(host));
        return runtime.activation();
    } catch (error) {
        if (runtime) {
            try {
                await runtime.cleanupAfterActivationFailure();
            } catch (cleanupError) {
                throw new AggregateError(
                    [error, cleanupError],
                    "Failed to activate and dispose app extension"
                );
            }
        } else {
            const cleanupErrors: unknown[] = [];
            try {
                await session.disconnect();
            } catch (cleanupError) {
                cleanupErrors.push(cleanupError);
            }
            try {
                cleanupErrors.push(...(await client.stop()));
            } catch (cleanupError) {
                cleanupErrors.push(cleanupError);
            }
            if (cleanupErrors.length > 0) {
                throw new AggregateError(
                    [error, ...cleanupErrors],
                    "Failed to authenticate and disconnect app extension"
                );
            }
        }
        throw error;
    }
}

function parsePrincipal(registration: {
    protocolVersion: 1;
    principal: { packageId: string; activationId: string };
}): AppExtensionPrincipal {
    if (registration.protocolVersion !== APP_EXTENSION_PROTOCOL_VERSION) {
        throw new TypeError(
            `Unsupported app extension protocol version: ${String(registration.protocolVersion)}`
        );
    }
    assertNonEmptyString(registration.principal.packageId, "principal.packageId");
    assertNonEmptyString(registration.principal.activationId, "principal.activationId");
    return Object.freeze({
        packageId: registration.principal.packageId as AppExtensionPackageId,
        activationId: registration.principal.activationId as AppExtensionActivationId,
    });
}

function parseContributions(contributions: unknown): readonly AppExtensionDeclaredContribution[] {
    if (!Array.isArray(contributions)) {
        throw new TypeError("contributions must be an array");
    }
    const seen = new Set<string>();
    return Object.freeze(
        contributions.map((contribution, index) => {
            if (contribution === null || typeof contribution !== "object") {
                throw new TypeError(`contributions[${index}] must be an object`);
            }
            const { contributionPoint, contributionId } = contribution as Record<string, unknown>;
            if (
                contributionPoint !== "sessionBadges" &&
                contributionPoint !== "canvases" &&
                contributionPoint !== "forgeProvider"
            ) {
                throw new TypeError(`contributions[${index}].contributionPoint is not supported`);
            }
            assertNonEmptyString(contributionId, `contributions[${index}].contributionId`);
            const key = `${contributionPoint}\0${contributionId}`;
            if (seen.has(key)) {
                throw new TypeError(
                    `contributions contains duplicate identity ${contributionPoint}/${contributionId}`
                );
            }
            seen.add(key);
            return Object.freeze({
                contributionPoint,
                contributionId: contributionId as AppExtensionContributionId,
            });
        })
    );
}

function parseCapabilities(capabilities: {
    sessionBadges?: true;
    canvases?: true;
    forgeProvider?: true;
    mediatedFetch?: true;
}): AppExtensionCapabilityGrants {
    if (capabilities === null || typeof capabilities !== "object") {
        throw new TypeError("capabilities must be an object");
    }
    for (const [name, value] of Object.entries(capabilities)) {
        if (
            !["sessionBadges", "canvases", "forgeProvider", "mediatedFetch"].includes(name) ||
            value !== true
        ) {
            throw new TypeError(`capabilities.${name} must be true when present`);
        }
    }
    return Object.freeze({ ...capabilities });
}

async function invokeDisposer(disposer: AppExtensionDisposer | undefined): Promise<void> {
    if (typeof disposer === "function") {
        await disposer();
    } else if (disposer) {
        await disposer.dispose();
    }
}

function assertNonEmptyString(value: unknown, name: string): asserts value is string {
    if (typeof value !== "string" || value.length === 0) {
        throw new TypeError(`${name} must be a non-empty string`);
    }
}
