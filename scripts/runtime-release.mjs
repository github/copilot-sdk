/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Resolve a pinned runtime's public release, preserving the caller's mirror override.
 * @param {string} version
 * @param {string | null | undefined} [baseUrl] Pass null to ignore ambient mirror settings.
 */
export function runtimeReleaseUrl(version, baseUrl = process.env.COPILOT_CLI_DOWNLOAD_BASE_URL) {
    const unstable =
        /^(?:0|[1-9][0-9]*)(?:\.(?:0|[1-9][0-9]*)){2}(?:-unstable|-(?:0|[1-9][0-9]*)\.unstable)\.r[1-9][0-9]*\.g[0-9a-f]{7}$/.test(
            version,
        );
    const repository = unstable ? "copilot-sdk" : "copilot-cli";
    const tag = unstable ? `runtime-${version}` : `v${version}`;
    const base = baseUrl ?? `https://github.com/github/${repository}/releases/download`;
    return `${base.replace(/\/+$/, "")}/${tag}`;
}
