/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

export const DEFAULT_DATADOG_SITE = "datadoghq.com";

/**
 * @param {string} site
 * @param {string} apiKey
 * @param {import("./datadog-metrics.mjs").DatadogSeriesPayload} payload
 * @param {import("./datadog-metrics.mjs").SubmitDatadogMetricOptions} [options]
 */
export async function submitDatadogMetric(site, apiKey, payload, options = {}) {
    const apiHost = datadogApiHost(site);
    const fetchImpl = options.fetchImpl ?? globalThis.fetch;
    const response = await fetchImpl(`https://${apiHost}/api/v1/series`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "DD-API-KEY": apiKey,
        },
        body: JSON.stringify(payload),
        signal: AbortSignal.timeout(options.timeoutMs ?? 30_000),
    });
    if (!response.ok) {
        throw new Error(`Datadog metric submission failed (${response.status}): ${await response.text()}`);
    }
}

/**
 * @param {string} site
 * @param {string} apiKey
 * @param {import("./datadog-metrics.mjs").DatadogDistributionPayload} payload
 * @param {import("./datadog-metrics.mjs").SubmitDatadogMetricOptions} [options]
 */
export async function submitDatadogDistributions(site, apiKey, payload, options = {}) {
    const apiHost = datadogApiHost(site);
    const fetchImpl = options.fetchImpl ?? globalThis.fetch;
    const response = await fetchImpl(`https://${apiHost}/api/v1/distribution_points`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "DD-API-KEY": apiKey,
        },
        body: JSON.stringify(payload),
        signal: AbortSignal.timeout(options.timeoutMs ?? 30_000),
    });
    if (!response.ok) {
        throw new Error(`Datadog distribution submission failed (${response.status}): ${await response.text()}`);
    }
}

/** @param {string} value */
export function normalizeDatadogTagValue(value) {
    return (
        value
            .trim()
            .toLowerCase()
            .replace(/[^a-z0-9_./-]+/g, "_")
            .replace(/^_+|_+$/g, "") || "unknown"
    );
}

/**
 * Standalone workflow scripts cannot load the native runtime error formatter.
 * Keep their Datadog error conversion in one shared place instead.
 *
 * @param {unknown} error
 */
export function datadogErrorText(error) {
    if (error instanceof Error) {
        return error.message;
    }
    return String(error);
}

/**
 * @param {string} site
 * @throws {Error} When the site cannot form a Datadog API hostname.
 */
export function validateDatadogSite(site) {
    datadogApiHost(site);
}

/** @param {string} site */
export function datadogApiHost(site) {
    const normalizedSite = site.trim().toLowerCase();
    const siteWithoutAppPrefix = normalizedSite.startsWith("app.") ? normalizedSite.slice(4) : normalizedSite;
    const apiHost = siteWithoutAppPrefix.startsWith("api.") ? siteWithoutAppPrefix : `api.${siteWithoutAppPrefix}`;
    const labels = apiHost.split(".");
    if (
        apiHost.length > 253 ||
        labels.some(
            (label) => label.length === 0 || label.length > 63 || !/^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$/.test(label),
        )
    ) {
        throw new Error(`Invalid Datadog site: ${site}`);
    }
    return apiHost;
}
