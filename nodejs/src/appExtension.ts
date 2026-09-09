/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { CopilotClient } from "./client.js";
import type { CopilotSession } from "./session.js";
import type { JsonValue } from "./factory.js";
import type { CancellationToken } from "vscode-jsonrpc/node.js";
import type {
    AppCanvasActionCallbackRequest,
    AppCanvasCloseCallbackRequest,
    AppCanvasContext as WireAppCanvasContext,
    AppCanvasOpenCallbackRequest,
    AppCanvasOpenResult as WireAppCanvasOpenResult,
    AppForgeInvokeCallbackRequest,
    AppMediatedFetchRequest as WireAppMediatedFetchRequest,
    AppMediatedFetchResponse as WireAppMediatedFetchResponse,
} from "./generated/rpc.js";
import {
    type AppSessionBadge,
    type AppSessionBadgesSnapshot,
    type AppSessionBadgeTargetIdentity,
    type AppSessionBadgeUpdate,
    type AppSessionBadgesExtension,
} from "./appSessionBadges.js";
import {
    onExtensionTransportClosedSymbol,
    registerPrivateAppCanvasSymbol,
    registerPrivateAppExtensionSymbol,
    registerPrivateAppForgeProviderSymbol,
    registerPrivateAppSessionBadgesSymbol,
    requestPrivateAppMediatedFetchSymbol,
    unregisterPrivateAppCanvasSymbol,
    unregisterPrivateAppForgeProviderSymbol,
} from "./appExtensionClientAccess.js";
import { joinExtensionSession } from "./extensionSession.js";

const APP_EXTENSION_PROTOCOL_VERSION = 1 as const;
const MAX_CONTRIBUTION_ID_LENGTH = 256;
const MAX_OPERATION_NAME_LENGTH = 256;
const MAX_CANVAS_INSTANCE_ID_LENGTH = 256;
const MAX_CANVAS_METADATA_LENGTH = 512;
const MAX_JSON_PAYLOAD_BYTES = 256 * 1024;
const MAX_FETCH_PATH_LENGTH = 8192;
const MAX_FETCH_HEADERS = 64;
const MAX_FETCH_HEADER_BYTES = 32 * 1024;
const MAX_FETCH_BODY_BYTES = 256 * 1024;
const MAX_FETCH_RESPONSE_BODY_BYTES = 1024 * 1024;

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
export interface AppExtensionContributionIdentity<
    TContributionPoint extends AppExtensionContributionPoint = AppExtensionContributionPoint,
> {
    readonly principal: AppExtensionPrincipal;
    readonly contributionPoint: TContributionPoint;
    readonly contributionId: AppExtensionContributionId;
}

/** Declared app-canvas contribution identity. */
export interface AppCanvasContributionDeclaration {
    readonly contributionPoint: "canvases";
    readonly contributionId: AppExtensionContributionId;
}

/** Declared session-badge contribution identity. */
export interface AppSessionBadgesContributionDeclaration {
    readonly contributionPoint: "sessionBadges";
    readonly contributionId: AppExtensionContributionId;
}

/** Declared forge-provider contribution identity. */
export interface AppForgeProviderContributionDeclaration {
    readonly contributionPoint: "forgeProvider";
    readonly contributionId: AppExtensionContributionId;
}

/** Runtime-authenticated declaration union for capability-specific registration APIs. */
export type AppExtensionDeclaredContribution =
    | AppSessionBadgesContributionDeclaration
    | AppCanvasContributionDeclaration
    | AppForgeProviderContributionDeclaration;

/** Alias for a runtime-authenticated app-extension contribution declaration. */
export type AppExtensionContributionDeclaration = AppExtensionDeclaredContribution;

/** Trusted project context supplied by the app host to an app canvas. */
export interface AppCanvasProjectContext {
    readonly forgeProviderId: string;
    readonly repositoryLocator: JsonValue;
    readonly forgeAccountId?: string;
}

/** Optional trusted application context associated with a canvas instance. */
export interface AppCanvasContext {
    readonly projectId?: string;
    readonly workspaceId?: string;
    readonly project?: AppCanvasProjectContext;
}

/** Request delivered when the app opens a canvas contribution. */
export interface AppCanvasOpenRequest {
    readonly instanceId: string;
    readonly input?: JsonValue;
    readonly context?: AppCanvasContext;
    readonly signal: AbortSignal;
}

/** Request delivered when the app invokes a canvas action. */
export interface AppCanvasActionRequest {
    readonly instanceId: string;
    readonly actionName: string;
    readonly input?: JsonValue;
    readonly context?: AppCanvasContext;
    readonly signal: AbortSignal;
}

/** Request delivered when the app closes a canvas instance. */
export interface AppCanvasCloseRequest {
    readonly instanceId: string;
    readonly context?: AppCanvasContext;
    readonly signal: AbortSignal;
}

/** Bounded state and display metadata returned when a canvas opens. */
export interface AppCanvasOpenResult {
    readonly state?: JsonValue;
    readonly title?: string;
    readonly status?: string;
}

/** Handlers for one statically declared app-canvas contribution. */
export interface AppCanvasRegistrationOptions {
    readonly contributionId: AppExtensionContributionId;
    readonly onOpen: (
        request: AppCanvasOpenRequest
    ) => AppCanvasOpenResult | Promise<AppCanvasOpenResult>;
    readonly onAction: (request: AppCanvasActionRequest) => JsonValue | Promise<JsonValue>;
    readonly onClose?: (request: AppCanvasCloseRequest) => void | Promise<void>;
}

/** Disposable app-canvas registration. */
export interface AppCanvasRegistration {
    readonly identity: AppExtensionContributionIdentity<"canvases">;
    dispose(): Promise<void>;
}

/** Registration surface for app-scoped canvases. */
export interface AppCanvasesHost {
    register(options: AppCanvasRegistrationOptions): Promise<AppCanvasRegistration>;
}

/** Request delivered to one registered forge-provider operation. */
export interface AppForgeOperationRequest {
    readonly operation: string;
    readonly accountId?: string;
    readonly input?: JsonValue;
    readonly signal: AbortSignal;
}

/** Handler for one forge-provider operation. */
export type AppForgeOperationHandler = (
    request: AppForgeOperationRequest
) => JsonValue | Promise<JsonValue>;

/** Registration options for one statically declared forge provider. */
export interface AppForgeProviderRegistrationOptions {
    readonly contributionId: AppExtensionContributionId;
    readonly operations: Readonly<Record<string, AppForgeOperationHandler>>;
}

/** Disposable forge-provider registration. */
export interface AppForgeProviderRegistration {
    readonly identity: AppExtensionContributionIdentity<"forgeProvider">;
    readonly operations: readonly string[];
    dispose(): Promise<void>;
}

/** Registration surface for app-scoped forge providers. */
export interface AppForgeProvidersHost {
    register(options: AppForgeProviderRegistrationOptions): Promise<AppForgeProviderRegistration>;
}

/** HTTP methods supported by mediated fetch. */
export type AppMediatedFetchMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

/** Capability-gated mediated-fetch request. */
export interface AppMediatedFetchRequest {
    readonly contributionId: AppExtensionContributionId;
    readonly accountId: string;
    readonly operation: string;
    readonly method: AppMediatedFetchMethod;
    readonly path: string;
    readonly headers?: Readonly<Record<string, string>>;
    readonly body?: string;
    readonly signal?: AbortSignal;
}

/** Bounded sanitized mediated-fetch response. */
export interface AppMediatedFetchResponse {
    readonly status: number;
    readonly headers: Readonly<Record<string, string>>;
    readonly body?: string;
    readonly truncated: boolean;
}

/** Capability-limited mediated network surface for a registered forge contribution. */
export interface AppMediatedFetchHost {
    request(options: AppMediatedFetchRequest): Promise<AppMediatedFetchResponse>;
}

/** Principal-aware callback for replacement badge snapshots. */
export type AppSessionBadgesRegistrationHandler = (
    snapshot: AppSessionBadgesSnapshot,
    identity: AppExtensionContributionIdentity<"sessionBadges">
) => void | Promise<void>;

/** Options for registering the activation's single badge contribution. */
export interface AppSessionBadgesRegistration {
    readonly onSnapshot?: AppSessionBadgesRegistrationHandler;
}

/** Capability-limited badge contribution owned by an app-extension principal. */
export interface AppSessionBadgesContribution {
    readonly identity: AppExtensionContributionIdentity<"sessionBadges">;
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
    readonly contributions: readonly AppExtensionDeclaredContribution[];
    readonly signal: AbortSignal;
    readonly sessionBadges: AppSessionBadgesHost;
    readonly canvases: AppCanvasesHost;
    readonly forgeProviders: AppForgeProvidersHost;
    readonly mediatedFetch: AppMediatedFetchHost;
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
    readonly identity: AppExtensionContributionIdentity<"sessionBadges">;
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

class CanvasRegistration implements AppCanvasRegistration {
    readonly identity: AppExtensionContributionIdentity<"canvases">;
    readonly publicRegistration: AppCanvasRegistration;
    remoteRegistered = false;
    disposed = false;
    readonly #controllers = new Set<AbortController>();

    constructor(
        private readonly owner: CanvasesRegistrar,
        principal: AppExtensionPrincipal,
        readonly contributionId: AppExtensionContributionId,
        readonly options: AppCanvasRegistrationOptions
    ) {
        this.identity = Object.freeze({
            principal,
            contributionPoint: "canvases",
            contributionId,
        });
        this.publicRegistration = Object.freeze(
            Object.defineProperty({ identity: this.identity }, "dispose", {
                value: this.dispose.bind(this),
            })
        ) as AppCanvasRegistration;
    }

    dispose(): Promise<void> {
        return this.owner.unregister(this, true);
    }

    abort(): void {
        if (this.disposed) return;
        this.disposed = true;
        for (const controller of this.#controllers) {
            controller.abort();
        }
        this.#controllers.clear();
    }

    async open(
        params: AppCanvasOpenCallbackRequest,
        cancellation?: CancellationToken
    ): Promise<WireAppCanvasOpenResult> {
        this.assertActive();
        assertProtocolVersion(params.protocolVersion);
        assertBoundedString(params.instanceId, "instanceId", MAX_CANVAS_INSTANCE_ID_LENGTH);
        const result = await this.run(
            (signal) =>
                this.options.onOpen(
                    Object.freeze({
                        instanceId: params.instanceId,
                        input: params.input,
                        context: copyCanvasContext(params.context),
                        signal,
                    })
                ),
            cancellation
        );
        return validateCanvasOpenResult(result);
    }

    async action(
        params: AppCanvasActionCallbackRequest,
        cancellation?: CancellationToken
    ): Promise<JsonValue> {
        this.assertActive();
        assertProtocolVersion(params.protocolVersion);
        assertBoundedString(params.instanceId, "instanceId", MAX_CANVAS_INSTANCE_ID_LENGTH);
        assertBoundedString(params.actionName, "actionName", MAX_OPERATION_NAME_LENGTH);
        const result = await this.run(
            (signal) =>
                this.options.onAction(
                    Object.freeze({
                        instanceId: params.instanceId,
                        actionName: params.actionName,
                        input: params.input,
                        context: copyCanvasContext(params.context),
                        signal,
                    })
                ),
            cancellation
        );
        assertJsonPayload(result, "canvas action result");
        return result;
    }

    async close(
        params: AppCanvasCloseCallbackRequest,
        cancellation?: CancellationToken
    ): Promise<void> {
        this.assertActive();
        assertProtocolVersion(params.protocolVersion);
        assertBoundedString(params.instanceId, "instanceId", MAX_CANVAS_INSTANCE_ID_LENGTH);
        if (!this.options.onClose) return;
        await this.run(
            (signal) =>
                this.options.onClose!(
                    Object.freeze({
                        instanceId: params.instanceId,
                        context: copyCanvasContext(params.context),
                        signal,
                    })
                ),
            cancellation
        );
    }

    private async run<T>(
        callback: (signal: AbortSignal) => T | Promise<T>,
        cancellation?: CancellationToken
    ): Promise<T> {
        const controller = new AbortController();
        this.#controllers.add(controller);
        const subscription = cancellation?.onCancellationRequested(() => controller.abort());
        if (this.disposed || cancellation?.isCancellationRequested) {
            controller.abort();
        }
        try {
            return await callback(controller.signal);
        } finally {
            subscription?.dispose();
            this.#controllers.delete(controller);
        }
    }

    private assertActive(): void {
        if (this.disposed) {
            throw new Error("App canvas contribution is disposed");
        }
    }
}

class CanvasesRegistrar implements AppCanvasesHost {
    readonly #registrations = new Map<string, CanvasRegistration>();
    readonly #handler: NonNullable<CopilotSession["clientSessionApis"]["appCanvas"]>;
    #disposed = false;
    #transportAvailable = true;

    constructor(
        private readonly principal: AppExtensionPrincipal,
        private readonly granted: boolean,
        private readonly declaredContributions: readonly AppExtensionDeclaredContribution[],
        private readonly client: CopilotClient,
        private readonly session: CopilotSession
    ) {
        this.#handler = {
            open: (params, cancellation) =>
                this.dispatch(params.contributionId, (registration) =>
                    registration.open(params, cancellation)
                ),
            invoke: (params, cancellation) =>
                this.dispatch(params.contributionId, (registration) =>
                    registration.action(params, cancellation)
                ),
            close: (params, cancellation) =>
                this.dispatch(params.contributionId, (registration) =>
                    registration.close(params, cancellation)
                ),
        };
        this.session.clientSessionApis.appCanvas = this.#handler;
    }

    async register(options: AppCanvasRegistrationOptions): Promise<AppCanvasRegistration> {
        this.assertCanRegister(options);
        const contributionId = resolveDeclaredContribution(
            this.declaredContributions,
            "canvases",
            options.contributionId
        );
        if (this.#registrations.has(contributionId)) {
            throw new Error(`App canvas contribution ${contributionId} is already registered`);
        }

        const registration = new CanvasRegistration(
            this,
            this.principal,
            contributionId,
            Object.freeze({
                contributionId,
                onOpen: options.onOpen,
                onAction: options.onAction,
                onClose: options.onClose,
            })
        );
        this.#registrations.set(contributionId, registration);
        try {
            await this.client[registerPrivateAppCanvasSymbol](contributionId);
            registration.remoteRegistered = true;
            if (this.#disposed || registration.disposed) {
                if (this.#transportAvailable) {
                    await this.client[unregisterPrivateAppCanvasSymbol](contributionId);
                }
                registration.remoteRegistered = false;
                throw new Error("The app extension activation was disposed during registration");
            }
            return registration.publicRegistration;
        } catch (error) {
            if (this.#registrations.get(contributionId) === registration) {
                this.#registrations.delete(contributionId);
            }
            registration.abort();
            throw error;
        }
    }

    async unregister(registration: CanvasRegistration, notifyRuntime: boolean): Promise<void> {
        registration.abort();
        if (notifyRuntime && this.#transportAvailable && registration.remoteRegistered) {
            await this.client[unregisterPrivateAppCanvasSymbol](registration.contributionId);
            registration.remoteRegistered = false;
        }
        if (this.#registrations.get(registration.contributionId) === registration) {
            this.#registrations.delete(registration.contributionId);
        }
    }

    async dispose(transportAvailable: boolean): Promise<void> {
        if (this.#disposed) return;
        this.#disposed = true;
        this.#transportAvailable = transportAvailable;
        if (this.session.clientSessionApis.appCanvas === this.#handler) {
            delete this.session.clientSessionApis.appCanvas;
        }
        const registrations = [...this.#registrations.values()];
        await Promise.all(
            registrations.map((registration) => this.unregister(registration, transportAvailable))
        );
    }

    private dispatch<T>(
        contributionId: string,
        callback: (registration: CanvasRegistration) => Promise<T>
    ): Promise<T> {
        const registration = this.#registrations.get(contributionId);
        if (!registration) {
            throw new Error(`No app canvas contribution registered for ${contributionId}`);
        }
        return callback(registration);
    }

    private assertCanRegister(options: AppCanvasRegistrationOptions): void {
        if (this.#disposed) {
            throw new Error("The app extension activation is disposed");
        }
        if (!this.granted) {
            throw new Error("The app extension principal was not granted canvases");
        }
        if (options === null || typeof options !== "object") {
            throw new TypeError("canvases.register options must be an object");
        }
        if (typeof options.onOpen !== "function") {
            throw new TypeError("canvases.register onOpen must be a function");
        }
        if (typeof options.onAction !== "function") {
            throw new TypeError("canvases.register onAction must be a function");
        }
        if (options.onClose !== undefined && typeof options.onClose !== "function") {
            throw new TypeError("canvases.register onClose must be a function");
        }
    }
}

class ForgeProviderRegistration implements AppForgeProviderRegistration {
    readonly identity: AppExtensionContributionIdentity<"forgeProvider">;
    readonly operations: readonly string[];
    readonly publicRegistration: AppForgeProviderRegistration;
    remoteRegistered = false;
    disposed = false;
    readonly #controllers = new Set<AbortController>();

    constructor(
        private readonly owner: ForgeProvidersRegistrar,
        principal: AppExtensionPrincipal,
        readonly contributionId: AppExtensionContributionId,
        readonly handlers: ReadonlyMap<string, AppForgeOperationHandler>
    ) {
        this.identity = Object.freeze({
            principal,
            contributionPoint: "forgeProvider",
            contributionId,
        });
        this.operations = Object.freeze([...handlers.keys()]);
        this.publicRegistration = Object.freeze(
            Object.defineProperty(
                { identity: this.identity, operations: this.operations },
                "dispose",
                { value: this.dispose.bind(this) }
            )
        ) as AppForgeProviderRegistration;
    }

    dispose(): Promise<void> {
        return this.owner.unregister(this, true);
    }

    abort(): void {
        if (this.disposed) return;
        this.disposed = true;
        for (const controller of this.#controllers) {
            controller.abort();
        }
        this.#controllers.clear();
    }

    async invoke(
        params: AppForgeInvokeCallbackRequest,
        cancellation?: CancellationToken
    ): Promise<JsonValue> {
        this.assertActive();
        assertProtocolVersion(params.protocolVersion);
        const handler = this.handlers.get(params.operation);
        if (!handler) {
            throw new Error(
                `Forge provider ${this.contributionId} does not support operation ${params.operation}`
            );
        }
        const controller = new AbortController();
        this.#controllers.add(controller);
        const subscription = cancellation?.onCancellationRequested(() => controller.abort());
        if (this.disposed || cancellation?.isCancellationRequested) {
            controller.abort();
        }
        try {
            const result = await handler(
                Object.freeze({
                    operation: params.operation,
                    accountId: params.accountId,
                    input: params.input,
                    signal: controller.signal,
                })
            );
            assertJsonPayload(result, "forge provider result");
            return result;
        } finally {
            subscription?.dispose();
            this.#controllers.delete(controller);
        }
    }

    private assertActive(): void {
        if (this.disposed) {
            throw new Error("App forge-provider contribution is disposed");
        }
    }
}

class ForgeProvidersRegistrar implements AppForgeProvidersHost {
    readonly #registrations = new Map<string, ForgeProviderRegistration>();
    readonly #handler: NonNullable<CopilotSession["clientSessionApis"]["appForgeProvider"]>;
    #disposed = false;
    #transportAvailable = true;

    constructor(
        private readonly principal: AppExtensionPrincipal,
        private readonly granted: boolean,
        private readonly declaredContributions: readonly AppExtensionDeclaredContribution[],
        private readonly client: CopilotClient,
        private readonly session: CopilotSession
    ) {
        this.#handler = {
            invoke: (params, cancellation) =>
                this.dispatch(params.contributionId, (registration) =>
                    registration.invoke(params, cancellation)
                ),
        };
        this.session.clientSessionApis.appForgeProvider = this.#handler;
    }

    async register(
        options: AppForgeProviderRegistrationOptions
    ): Promise<AppForgeProviderRegistration> {
        const handlers = this.validateOptions(options);
        const contributionId = resolveDeclaredContribution(
            this.declaredContributions,
            "forgeProvider",
            options.contributionId
        );
        if (this.#registrations.has(contributionId)) {
            throw new Error(`Forge-provider contribution ${contributionId} is already registered`);
        }

        const registration = new ForgeProviderRegistration(
            this,
            this.principal,
            contributionId,
            handlers
        );
        this.#registrations.set(contributionId, registration);
        try {
            const [firstOperation, ...remainingOperations] = registration.operations;
            await this.client[registerPrivateAppForgeProviderSymbol](contributionId, [
                firstOperation!,
                ...remainingOperations,
            ]);
            registration.remoteRegistered = true;
            if (this.#disposed || registration.disposed) {
                if (this.#transportAvailable) {
                    await this.client[unregisterPrivateAppForgeProviderSymbol](contributionId);
                }
                registration.remoteRegistered = false;
                throw new Error("The app extension activation was disposed during registration");
            }
            return registration.publicRegistration;
        } catch (error) {
            if (this.#registrations.get(contributionId) === registration) {
                this.#registrations.delete(contributionId);
            }
            registration.abort();
            throw error;
        }
    }

    async unregister(
        registration: ForgeProviderRegistration,
        notifyRuntime: boolean
    ): Promise<void> {
        registration.abort();
        if (notifyRuntime && this.#transportAvailable && registration.remoteRegistered) {
            await this.client[unregisterPrivateAppForgeProviderSymbol](registration.contributionId);
            registration.remoteRegistered = false;
        }
        if (this.#registrations.get(registration.contributionId) === registration) {
            this.#registrations.delete(registration.contributionId);
        }
    }

    async dispose(transportAvailable: boolean): Promise<void> {
        if (this.#disposed) return;
        this.#disposed = true;
        this.#transportAvailable = transportAvailable;
        if (this.session.clientSessionApis.appForgeProvider === this.#handler) {
            delete this.session.clientSessionApis.appForgeProvider;
        }
        const registrations = [...this.#registrations.values()];
        await Promise.all(
            registrations.map((registration) => this.unregister(registration, transportAvailable))
        );
    }

    isRegistered(contributionId: AppExtensionContributionId): boolean {
        const registration = this.#registrations.get(contributionId);
        return (
            registration !== undefined && registration.remoteRegistered && !registration.disposed
        );
    }

    private dispatch<T>(
        contributionId: string,
        callback: (registration: ForgeProviderRegistration) => Promise<T>
    ): Promise<T> {
        const registration = this.#registrations.get(contributionId);
        if (!registration) {
            throw new Error(`No app forge-provider contribution registered for ${contributionId}`);
        }
        return callback(registration);
    }

    private validateOptions(
        options: AppForgeProviderRegistrationOptions
    ): ReadonlyMap<string, AppForgeOperationHandler> {
        if (this.#disposed) {
            throw new Error("The app extension activation is disposed");
        }
        if (!this.granted) {
            throw new Error("The app extension principal was not granted forgeProvider");
        }
        if (options === null || typeof options !== "object") {
            throw new TypeError("forgeProviders.register options must be an object");
        }
        if (
            options.operations === null ||
            typeof options.operations !== "object" ||
            Array.isArray(options.operations)
        ) {
            throw new TypeError("forgeProviders.register operations must be an object");
        }
        const handlers = new Map<string, AppForgeOperationHandler>();
        for (const [operation, handler] of Object.entries(options.operations)) {
            assertBoundedString(operation, "operation", MAX_OPERATION_NAME_LENGTH);
            if (typeof handler !== "function") {
                throw new TypeError(
                    `forgeProviders.register operation ${operation} must be a function`
                );
            }
            handlers.set(operation, handler);
        }
        if (handlers.size === 0) {
            throw new TypeError("forgeProviders.register requires at least one operation");
        }
        return handlers;
    }
}

class MediatedFetchHost implements AppMediatedFetchHost {
    constructor(
        private readonly granted: boolean,
        private readonly declaredContributions: readonly AppExtensionDeclaredContribution[],
        private readonly forgeProviders: ForgeProvidersRegistrar,
        private readonly client: CopilotClient,
        private readonly lifecycleSignal: AbortSignal
    ) {}

    async request(options: AppMediatedFetchRequest): Promise<AppMediatedFetchResponse> {
        if (!this.granted) {
            throw new Error("The app extension principal was not granted mediatedFetch");
        }
        if (options === null || typeof options !== "object") {
            throw new TypeError("mediatedFetch.request options must be an object");
        }
        const contributionId = resolveDeclaredContribution(
            this.declaredContributions,
            "forgeProvider",
            options.contributionId
        );
        if (!this.forgeProviders.isRegistered(contributionId)) {
            throw new Error(
                `Forge-provider contribution ${contributionId} must be registered before mediated fetch`
            );
        }
        assertBoundedString(options.accountId, "accountId", MAX_CONTRIBUTION_ID_LENGTH);
        assertBoundedString(options.operation, "operation", MAX_OPERATION_NAME_LENGTH);
        const request = validateMediatedFetchRequest(options);
        const signal = options.signal
            ? AbortSignal.any([this.lifecycleSignal, options.signal])
            : this.lifecycleSignal;
        if (signal.aborted) {
            throw new DOMException("The mediated fetch request was aborted", "AbortError");
        }
        const response = await this.client[requestPrivateAppMediatedFetchSymbol](
            {
                protocolVersion: APP_EXTENSION_PROTOCOL_VERSION,
                contributionId,
                accountId: options.accountId,
                operation: options.operation,
                request,
            },
            signal
        );
        return validateMediatedFetchResponse(response);
    }
}

class AppExtensionRuntime {
    readonly signal: AbortSignal;
    readonly principal: AppExtensionPrincipal;
    #disposer: AppExtensionDisposer | undefined;
    #disposed = false;
    #disposePromise: Promise<void> | undefined;
    #removeTransportCloseHandler: () => void = () => {};
    readonly #client: CopilotClient;
    readonly #session: CopilotSession;
    readonly #sessionBadges: SessionBadgesRegistrar;
    readonly #canvases: CanvasesRegistrar;
    readonly #forgeProviders: ForgeProvidersRegistrar;

    constructor(
        principal: AppExtensionPrincipal,
        client: CopilotClient,
        session: CopilotSession,
        sessionBadges: SessionBadgesRegistrar,
        canvases: CanvasesRegistrar,
        forgeProviders: ForgeProvidersRegistrar,
        private readonly abortController: AbortController
    ) {
        this.principal = principal;
        this.#client = client;
        this.#session = session;
        this.#sessionBadges = sessionBadges;
        this.#canvases = canvases;
        this.#forgeProviders = forgeProviders;
        this.signal = this.abortController.signal;
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
        this.abortController.abort();
        this.#sessionBadges.dispose();

        const errors: unknown[] = [];
        for (const registrar of [this.#canvases, this.#forgeProviders]) {
            try {
                await registrar.dispose(disconnect);
            } catch (error) {
                errors.push(error);
            }
        }
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
        const abortController = new AbortController();
        const sessionBadges = new SessionBadgesRegistrar(
            principal,
            capabilities.sessionBadges === true,
            contributions,
            () => client[registerPrivateAppSessionBadgesSymbol](session)
        );
        const canvases = new CanvasesRegistrar(
            principal,
            capabilities.canvases === true,
            contributions,
            client,
            session
        );
        const forgeProviders = new ForgeProvidersRegistrar(
            principal,
            capabilities.forgeProvider === true,
            contributions,
            client,
            session
        );
        const mediatedFetch = new MediatedFetchHost(
            capabilities.mediatedFetch === true,
            contributions,
            forgeProviders,
            client,
            abortController.signal
        );
        runtime = new AppExtensionRuntime(
            principal,
            client,
            session,
            sessionBadges,
            canvases,
            forgeProviders,
            abortController
        );
        const host: AppExtensionHost = Object.freeze({
            principal,
            capabilities,
            contributions,
            signal: runtime.signal,
            sessionBadges: Object.freeze({
                register: sessionBadges.register.bind(sessionBadges),
            }),
            canvases: Object.freeze({
                register: canvases.register.bind(canvases),
            }),
            forgeProviders: Object.freeze({
                register: forgeProviders.register.bind(forgeProviders),
            }),
            mediatedFetch: Object.freeze({
                request: mediatedFetch.request.bind(mediatedFetch),
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

function parsePrincipal(registration: unknown): AppExtensionPrincipal {
    if (registration === null || typeof registration !== "object") {
        throw new TypeError("App extension registration must be an object");
    }
    const value = registration as Record<string, unknown>;
    if (value.protocolVersion !== APP_EXTENSION_PROTOCOL_VERSION) {
        throw new TypeError(
            `Unsupported app extension protocol version: ${String(value.protocolVersion)}`
        );
    }
    if (value.principal === null || typeof value.principal !== "object") {
        throw new TypeError("principal must be an object");
    }
    const principal = value.principal as Record<string, unknown>;
    assertBoundedString(principal.packageId, "principal.packageId", MAX_CONTRIBUTION_ID_LENGTH);
    assertBoundedString(
        principal.activationId,
        "principal.activationId",
        MAX_CONTRIBUTION_ID_LENGTH
    );
    return Object.freeze({
        packageId: principal.packageId as AppExtensionPackageId,
        activationId: principal.activationId as AppExtensionActivationId,
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
            assertBoundedString(
                contributionId,
                `contributions[${index}].contributionId`,
                MAX_CONTRIBUTION_ID_LENGTH
            );
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

function parseCapabilities(capabilities: unknown): AppExtensionCapabilityGrants {
    if (capabilities === null || typeof capabilities !== "object") {
        throw new TypeError("capabilities must be an object");
    }
    const grants: {
        sessionBadges?: true;
        canvases?: true;
        forgeProvider?: true;
        mediatedFetch?: true;
    } = {};
    for (const name of ["sessionBadges", "canvases", "forgeProvider", "mediatedFetch"] as const) {
        const value = (capabilities as Record<string, unknown>)[name];
        if (value !== undefined && value !== true) {
            throw new TypeError(`capabilities.${name} must be true when present`);
        }
        if (value === true) {
            grants[name] = true;
        }
    }
    return Object.freeze(grants);
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

function assertBoundedString(
    value: unknown,
    name: string,
    maxLength: number
): asserts value is string {
    assertNonEmptyString(value, name);
    if (value.length > maxLength) {
        throw new TypeError(`${name} must be at most ${maxLength} characters`);
    }
}

function assertProtocolVersion(version: unknown): asserts version is 1 {
    if (version !== APP_EXTENSION_PROTOCOL_VERSION) {
        throw new TypeError(`Unsupported app extension protocol version: ${String(version)}`);
    }
}

function resolveDeclaredContribution<TContributionPoint extends AppExtensionContributionPoint>(
    contributions: readonly AppExtensionDeclaredContribution[],
    contributionPoint: TContributionPoint,
    requestedId: unknown
): AppExtensionContributionId {
    assertBoundedString(requestedId, "contributionId", MAX_CONTRIBUTION_ID_LENGTH);
    const declaration = contributions.find(
        (candidate) =>
            candidate.contributionPoint === contributionPoint &&
            candidate.contributionId === requestedId
    );
    if (!declaration) {
        throw new Error(
            `The app extension principal did not declare ${contributionPoint}/${requestedId}`
        );
    }
    return declaration.contributionId;
}

function copyCanvasContext(
    context: WireAppCanvasContext | undefined
): AppCanvasContext | undefined {
    if (!context) return undefined;
    const project = context.project
        ? Object.freeze({
              forgeProviderId: context.project.forgeProviderId,
              repositoryLocator: context.project.repositoryLocator,
              ...(context.project.forgeAccountId === undefined
                  ? {}
                  : { forgeAccountId: context.project.forgeAccountId }),
          })
        : undefined;
    return Object.freeze({
        ...(context.projectId === undefined ? {} : { projectId: context.projectId }),
        ...(context.workspaceId === undefined ? {} : { workspaceId: context.workspaceId }),
        ...(project === undefined ? {} : { project }),
    });
}

function validateCanvasOpenResult(result: AppCanvasOpenResult): WireAppCanvasOpenResult {
    if (result === null || typeof result !== "object") {
        throw new TypeError("canvas onOpen result must be an object");
    }
    if (result.title !== undefined) {
        assertOptionalBoundedText(result.title, "canvas title", MAX_CANVAS_METADATA_LENGTH);
    }
    if (result.status !== undefined) {
        assertOptionalBoundedText(result.status, "canvas status", MAX_CANVAS_METADATA_LENGTH);
    }
    if (result.state !== undefined) {
        assertJsonPayload(result.state, "canvas state");
    }
    return {
        ...(result.state === undefined ? {} : { state: result.state }),
        ...(result.title === undefined ? {} : { title: result.title }),
        ...(result.status === undefined ? {} : { status: result.status }),
    };
}

function assertOptionalBoundedText(value: unknown, name: string, maxLength: number): void {
    if (typeof value !== "string" || value.length > maxLength) {
        throw new TypeError(`${name} must be a string of at most ${maxLength} characters`);
    }
}

function assertJsonPayload(value: unknown, name: string): asserts value is JsonValue {
    let serialized: string | undefined;
    try {
        serialized = JSON.stringify(value);
    } catch (error) {
        throw new TypeError(`${name} must be JSON-serializable`, { cause: error });
    }
    if (serialized === undefined) {
        throw new TypeError(`${name} must be JSON-serializable`);
    }
    if (Buffer.byteLength(serialized, "utf8") > MAX_JSON_PAYLOAD_BYTES) {
        throw new TypeError(`${name} must not exceed ${MAX_JSON_PAYLOAD_BYTES} bytes`);
    }
}

function validateMediatedFetchRequest(
    options: AppMediatedFetchRequest
): WireAppMediatedFetchRequest["request"] {
    if (!["GET", "POST", "PUT", "PATCH", "DELETE"].includes(options.method)) {
        throw new TypeError(`Unsupported mediated fetch method: ${String(options.method)}`);
    }
    assertBoundedString(options.path, "path", MAX_FETCH_PATH_LENGTH);
    if (
        !options.path.startsWith("/") ||
        /^[a-z][a-z0-9+.-]*:/i.test(options.path) ||
        options.path.startsWith("//") ||
        options.path.includes("\\") ||
        options.path.includes("\r") ||
        options.path.includes("\n")
    ) {
        throw new TypeError("path must be a credential-free root-relative URL path");
    }
    const pathWithoutQuery = options.path.split(/[?#]/, 1)[0]!;
    let decodedPath: string;
    try {
        decodedPath = decodeURIComponent(pathWithoutQuery);
    } catch (error) {
        throw new TypeError("path contains invalid percent encoding", { cause: error });
    }
    if (decodedPath.includes("\\") || decodedPath.split("/").some((segment) => segment === "..")) {
        throw new TypeError("path must not contain parent traversal segments");
    }
    const headers = validateFetchHeaders(options.headers);
    if (options.body !== undefined) {
        if (typeof options.body !== "string") {
            throw new TypeError("body must be a string");
        }
        if (Buffer.byteLength(options.body, "utf8") > MAX_FETCH_BODY_BYTES) {
            throw new TypeError(`body must not exceed ${MAX_FETCH_BODY_BYTES} bytes`);
        }
    }
    return {
        method: options.method,
        path: options.path,
        ...(headers === undefined ? {} : { headers }),
        ...(options.body === undefined ? {} : { body: options.body }),
    };
}

function validateFetchHeaders(
    headers: Readonly<Record<string, string>> | undefined
): Record<string, string> | undefined {
    if (headers === undefined) return undefined;
    if (headers === null || typeof headers !== "object" || Array.isArray(headers)) {
        throw new TypeError("headers must be an object");
    }
    const entries = Object.entries(headers);
    if (entries.length > MAX_FETCH_HEADERS) {
        throw new TypeError(`headers must contain at most ${MAX_FETCH_HEADERS} entries`);
    }
    const denied = /^(authorization|cookie|host|proxy-authorization|x-forwarded-.+)$/i;
    const result: Record<string, string> = {};
    let bytes = 0;
    for (const [name, value] of entries) {
        if (!/^[!#$%&'*+\-.^_`|~0-9A-Za-z]+$/.test(name) || denied.test(name)) {
            throw new TypeError(`header ${name} is not permitted`);
        }
        if (typeof value !== "string" || /[\r\n]/.test(value)) {
            throw new TypeError(`header ${name} must be a single-line string`);
        }
        bytes += Buffer.byteLength(name, "utf8") + Buffer.byteLength(value, "utf8");
        if (bytes > MAX_FETCH_HEADER_BYTES) {
            throw new TypeError(`headers must not exceed ${MAX_FETCH_HEADER_BYTES} bytes`);
        }
        result[name] = value;
    }
    return result;
}

function validateMediatedFetchResponse(
    response: WireAppMediatedFetchResponse
): AppMediatedFetchResponse {
    if (
        response === null ||
        typeof response !== "object" ||
        !Number.isInteger(response.status) ||
        response.status < 100 ||
        response.status > 599
    ) {
        throw new TypeError("mediated fetch response status must be an HTTP status code");
    }
    if (typeof response.truncated !== "boolean") {
        throw new TypeError("mediated fetch response truncated must be a boolean");
    }
    const headers = validateFetchResponseHeaders(response.headers);
    if (response.body !== undefined) {
        if (typeof response.body !== "string") {
            throw new TypeError("mediated fetch response body must be a string");
        }
        if (Buffer.byteLength(response.body, "utf8") > MAX_FETCH_RESPONSE_BODY_BYTES) {
            throw new TypeError(
                `mediated fetch response body must not exceed ${MAX_FETCH_RESPONSE_BODY_BYTES} bytes`
            );
        }
    }
    return Object.freeze({
        status: response.status,
        headers: Object.freeze(headers),
        ...(response.body === undefined ? {} : { body: response.body }),
        truncated: response.truncated,
    });
}

function validateFetchResponseHeaders(headers: unknown): Record<string, string> {
    if (headers === null || typeof headers !== "object" || Array.isArray(headers)) {
        throw new TypeError("mediated fetch response headers must be an object");
    }
    const result: Record<string, string> = {};
    const entries = Object.entries(headers);
    let bytes = 0;
    if (entries.length > MAX_FETCH_HEADERS) {
        throw new TypeError(
            `mediated fetch response headers must contain at most ${MAX_FETCH_HEADERS} entries`
        );
    }
    for (const [name, value] of entries) {
        if (
            !/^[!#$%&'*+\-.^_`|~0-9A-Za-z]+$/.test(name) ||
            typeof value !== "string" ||
            /[\r\n]/.test(value)
        ) {
            throw new TypeError("mediated fetch response headers must contain strings");
        }
        bytes += Buffer.byteLength(name, "utf8") + Buffer.byteLength(value, "utf8");
        if (bytes > MAX_FETCH_HEADER_BYTES) {
            throw new TypeError(
                `mediated fetch response headers must not exceed ${MAX_FETCH_HEADER_BYTES} bytes`
            );
        }
        result[name] = value;
    }
    return result;
}
