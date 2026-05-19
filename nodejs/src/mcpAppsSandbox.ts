/**
 * SEP-1865 sandbox primitives: Content-Security-Policy and Permission Policy
 * builders for hosts that render MCP App `ui://` bundles in iframes.
 *
 * These are pure functions — no DOM, no fetch — so they're safe to call in
 * Node, the renderer process, or a service worker. The spec mandates two
 * different CSP shapes:
 *
 *   1. **Restrictive default** (when the resource has no `_meta.ui.csp` at
 *      all): `connect-src 'none'`, no external resource origins.
 *      See spec §UI Resource Format → "Restrictive Default".
 *   2. **Constructed default** (when the resource declares any `csp` block,
 *      even with empty arrays): `connect-src 'self'` plus declared domains,
 *      `frame-src 'none'` unless overridden, `base-uri 'self'` unless
 *      overridden. See spec §Security Implications → "CSP Construction".
 *
 * The host MUST always set `default-src 'none'` and `object-src 'none'`.
 */

/** Resource-level `_meta.ui.csp` block per SEP-1865. All fields optional. */
export interface McpAppsCspInput {
    /** Origins for network requests (fetch/XHR/WebSocket). Maps to `connect-src`. */
    connectDomains?: string[];
    /**
     * Origins for static resources (scripts, images, styles, fonts, media).
     * Maps to `script-src`, `style-src`, `img-src`, `font-src`, `media-src`.
     */
    resourceDomains?: string[];
    /** Origins for nested iframes. Maps to `frame-src`. */
    frameDomains?: string[];
    /** Allowed base URIs for the document. Maps to `base-uri`. */
    baseUriDomains?: string[];
}

/** Resource-level `_meta.ui.permissions` block per SEP-1865. */
export interface McpAppsPermissionsInput {
    /** Maps to Permission Policy `camera` feature. */
    camera?: Record<string, unknown>;
    /** Maps to Permission Policy `microphone` feature. */
    microphone?: Record<string, unknown>;
    /** Maps to Permission Policy `geolocation` feature. */
    geolocation?: Record<string, unknown>;
    /** Maps to Permission Policy `clipboard-write` feature. */
    clipboardWrite?: Record<string, unknown>;
}

/** Spec-mandated restrictive default applied when `_meta.ui.csp` is entirely absent. */
const RESTRICTIVE_DEFAULT_CSP =
    "default-src 'none'; " +
    "script-src 'self' 'unsafe-inline'; " +
    "style-src 'self' 'unsafe-inline'; " +
    "img-src 'self' data:; " +
    "media-src 'self' data:; " +
    "connect-src 'none'; " +
    "frame-src 'none'; " +
    "object-src 'none'; " +
    "base-uri 'self'";

/**
 * Build the `Content-Security-Policy` header value for an MCP App view per
 * SEP-1865 §UI Resource Format and §Security Implications.
 *
 * Pass `_meta.ui.csp` from the resolved `resources/read` content item. If the
 * resource omits `_meta.ui.csp` entirely, pass `undefined` to apply the
 * restrictive default (`connect-src 'none'`).
 *
 * The host MAY further restrict the returned policy but MUST NOT add
 * undeclared domains (spec §UI Resource Format → "No Loosening").
 *
 * @example
 * ```ts
 * const meta = uiResource._meta?.ui;
 * res.setHeader("Content-Security-Policy", buildMcpAppsCspHeader(meta?.csp));
 * ```
 */
export function buildMcpAppsCspHeader(csp: McpAppsCspInput | undefined): string {
    if (!csp) {
        return RESTRICTIVE_DEFAULT_CSP;
    }
    const resourceDomains = (csp.resourceDomains ?? []).join(" ");
    const connectDomains = (csp.connectDomains ?? []).join(" ");
    const frameDomains = csp.frameDomains?.length ? csp.frameDomains.join(" ") : "'none'";
    const baseUriDomains = csp.baseUriDomains?.length ? csp.baseUriDomains.join(" ") : "'self'";
    const trail = (extra: string) => (extra ? ` ${extra}` : "");
    return [
        "default-src 'none'",
        `script-src 'self' 'unsafe-inline'${trail(resourceDomains)}`,
        `style-src 'self' 'unsafe-inline'${trail(resourceDomains)}`,
        `connect-src 'self'${trail(connectDomains)}`,
        `img-src 'self' data:${trail(resourceDomains)}`,
        `font-src 'self'${trail(resourceDomains)}`,
        `media-src 'self' data:${trail(resourceDomains)}`,
        `frame-src ${frameDomains}`,
        "object-src 'none'",
        `base-uri ${baseUriDomains}`,
    ].join("; ");
}

/**
 * Build the value for the iframe `allow` attribute (Permission Policy) from
 * an MCP App view's `_meta.ui.permissions` block per SEP-1865.
 *
 * Note `clipboardWrite` maps to the hyphenated `clipboard-write` Permission
 * Policy feature name.
 *
 * @example
 * ```ts
 * const allow = buildMcpAppsAllowAttribute(uiResource._meta?.ui?.permissions);
 * iframe.setAttribute("allow", allow);
 * ```
 */
export function buildMcpAppsAllowAttribute(permissions: McpAppsPermissionsInput | undefined): string {
    if (!permissions) return "";
    const features: string[] = [];
    if (permissions.camera) features.push("camera");
    if (permissions.microphone) features.push("microphone");
    if (permissions.geolocation) features.push("geolocation");
    if (permissions.clipboardWrite) features.push("clipboard-write");
    return features.join("; ");
}
