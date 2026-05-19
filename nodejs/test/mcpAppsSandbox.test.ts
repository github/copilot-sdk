import { describe, expect, it } from "vitest";
import { buildMcpAppsAllowAttribute, buildMcpAppsCspHeader } from "../src/mcpAppsSandbox.js";

/**
 * SEP-1865 §UI Resource Format → "Restrictive Default" and §Security
 * Implications → "CSP Construction" pin the exact CSP shapes a host MUST emit.
 * These tests pin the spec text to the helper output so any regression is
 * caught against the pinned spec lines, not against an implementation detail.
 */
describe("buildMcpAppsCspHeader", () => {
    it("returns the restrictive default when csp is undefined (spec §UI Resource Format)", () => {
        const header = buildMcpAppsCspHeader(undefined);
        // Restrictive default MUST set connect-src 'none' (no external network).
        expect(header).toContain("default-src 'none'");
        expect(header).toContain("script-src 'self' 'unsafe-inline'");
        expect(header).toContain("style-src 'self' 'unsafe-inline'");
        expect(header).toContain("img-src 'self' data:");
        expect(header).toContain("media-src 'self' data:");
        expect(header).toContain("connect-src 'none'");
        expect(header).toContain("frame-src 'none'");
        expect(header).toContain("object-src 'none'");
        expect(header).toContain("base-uri 'self'");
    });

    it("uses connect-src 'self' (not 'none') when csp is declared with empty arrays", () => {
        // Per spec §Security Implications, a present `csp` block — even with
        // empty arrays — switches to constructed defaults: connect-src 'self'.
        const header = buildMcpAppsCspHeader({});
        expect(header).toContain("connect-src 'self'");
        expect(header).not.toContain("connect-src 'none'");
    });

    it("appends declared connectDomains to connect-src", () => {
        const header = buildMcpAppsCspHeader({
            connectDomains: ["https://api.weather.com", "wss://realtime.service.com"],
        });
        expect(header).toContain("connect-src 'self' https://api.weather.com wss://realtime.service.com");
    });

    it("appends resourceDomains to script-src, style-src, img-src, font-src, media-src", () => {
        const header = buildMcpAppsCspHeader({
            resourceDomains: ["https://cdn.jsdelivr.net"],
        });
        expect(header).toContain("script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net");
        expect(header).toContain("style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net");
        expect(header).toContain("img-src 'self' data: https://cdn.jsdelivr.net");
        expect(header).toContain("font-src 'self' https://cdn.jsdelivr.net");
        expect(header).toContain("media-src 'self' data: https://cdn.jsdelivr.net");
    });

    it("uses declared frameDomains when provided, 'none' otherwise", () => {
        expect(buildMcpAppsCspHeader({})).toContain("frame-src 'none'");
        const header = buildMcpAppsCspHeader({
            frameDomains: ["https://www.youtube.com", "https://player.vimeo.com"],
        });
        expect(header).toContain("frame-src https://www.youtube.com https://player.vimeo.com");
        expect(header).not.toContain("frame-src 'none'");
    });

    it("uses declared baseUriDomains when provided, 'self' otherwise", () => {
        expect(buildMcpAppsCspHeader({})).toContain("base-uri 'self'");
        const header = buildMcpAppsCspHeader({ baseUriDomains: ["https://cdn.example.com"] });
        expect(header).toContain("base-uri https://cdn.example.com");
        expect(header).not.toContain("base-uri 'self'");
    });

    it("always includes object-src 'none' (host MUST block plugins)", () => {
        expect(buildMcpAppsCspHeader(undefined)).toContain("object-src 'none'");
        expect(buildMcpAppsCspHeader({})).toContain("object-src 'none'");
        expect(buildMcpAppsCspHeader({ resourceDomains: ["x"] })).toContain("object-src 'none'");
    });
});

describe("buildMcpAppsAllowAttribute", () => {
    it("returns empty string when permissions is undefined", () => {
        expect(buildMcpAppsAllowAttribute(undefined)).toBe("");
    });

    it("returns empty string when no features are requested", () => {
        expect(buildMcpAppsAllowAttribute({})).toBe("");
    });

    it("maps each requested feature to its Permission Policy name", () => {
        expect(buildMcpAppsAllowAttribute({ camera: {} })).toBe("camera");
        expect(buildMcpAppsAllowAttribute({ microphone: {} })).toBe("microphone");
        expect(buildMcpAppsAllowAttribute({ geolocation: {} })).toBe("geolocation");
        // The hyphenated form per Permission Policy spec.
        expect(buildMcpAppsAllowAttribute({ clipboardWrite: {} })).toBe("clipboard-write");
    });

    it("joins multiple features with '; '", () => {
        const allow = buildMcpAppsAllowAttribute({
            camera: {},
            microphone: {},
            clipboardWrite: {},
        });
        expect(allow).toBe("camera; microphone; clipboard-write");
    });
});
