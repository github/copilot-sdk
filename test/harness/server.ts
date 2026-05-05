/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { ReplayingCapiProxy } from "./replayingCapiProxy";
import { ConnectProxy } from "./connectProxy";
import { createE2eRequestHandler } from "./mockHandlers";

// Starts up an instance of the ReplayingCapiProxy server
// The intention is for this to be usable in E2E tests across all languages

const proxy = new ReplayingCapiProxy("https://api.githubcopilot.com");
const proxyUrl = await proxy.start();
const blockedHosts: string[] = [];
const unhandledRequests: string[] = [];

const connectProxy = new ConnectProxy(
  createE2eRequestHandler({
    capiProxyUrl: proxyUrl,
    onUnhandled: (host, method, requestPath) => {
      const entry = `${method} ${host}${requestPath}`;
      unhandledRequests.push(entry);
      console.error(`[E2E proxy] Unhandled intercepted request: ${entry}`);
    },
  }),
  {
    interceptDomains: [
      "api.githubcopilot.com",
      "api.github.com",
      "github.com",
      "api.mcp.github.com",
    ],
    passthroughDomains: ["registry.npmjs.org"],
    onBlockedConnection: (host, port) => {
      const entry = `${host}:${port}`;
      blockedHosts.push(entry);
      console.error(`[E2E proxy] Blocked connection to: ${entry}`);
    },
  },
);
await connectProxy.start();

proxy.onStopRequested = async () => {
  if (blockedHosts.length || unhandledRequests.length) {
    const details = [
      ...blockedHosts.map((host) => `blocked ${host}`),
      ...unhandledRequests.map((request) => `unhandled ${request}`),
    ].join(", ");
    console.error(`[E2E proxy] Unexpected network activity: ${details}`);
  }
  await connectProxy.stop();
};

console.log(
  `Listening: ${proxyUrl} ${JSON.stringify({
    connectProxyUrl: connectProxy.proxyUrl,
    caFilePath: connectProxy.caFilePath,
  })}`,
);
