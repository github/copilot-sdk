/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import http from "http";
import type { RequestHandler } from "./connectProxy";

export function createE2eRequestHandler(options: {
  capiProxyUrl: string;
  onUnhandled?: (host: string, method: string, path: string) => void;
}): RequestHandler {
  return async (req, res, targetHost) => {
    if (targetHost === "api.githubcopilot.com") {
      return forwardToCapiProxy(req, res, options.capiProxyUrl);
    }

    if (targetHost === "api.github.com") {
      return handleGitHubApi(req, res, options);
    }

    if (targetHost === "github.com") {
      respondJson(res, 404, { message: "Not Found (e2e mock)" });
      return true;
    }

    if (targetHost === "api.mcp.github.com") {
      return handleMcpRegistry(req, res);
    }

    options.onUnhandled?.(targetHost, req.method ?? "GET", req.url ?? "/");
    return false;
  };
}

function handleGitHubApi(
  req: http.IncomingMessage,
  res: http.ServerResponse,
  options: { capiProxyUrl: string },
): boolean {
  const url = req.url ?? "/";

  if (req.method === "GET" && url === "/user") {
    respondJson(res, 200, {
      login: "sdk-e2e-user",
      id: 12345,
      type: "User",
      name: "SDK E2E User",
    });
    return true;
  }

  if (req.method === "GET" && url.startsWith("/user/copilot_billing")) {
    respondJson(res, 200, {
      seat: { plan: { plan_type: "business" } },
    });
    return true;
  }

  if (req.method === "GET" && url.startsWith("/copilot_internal/user")) {
    respondJson(res, 200, {
      login: "sdk-e2e-user",
      analytics_tracking_id: "sdk-e2e-tracking-id",
      organization_list: [],
      copilot_plan: "individual_pro",
      is_mcp_enabled: true,
      endpoints: {
        api: options.capiProxyUrl,
        telemetry: "https://localhost:1/telemetry",
      },
    });
    return true;
  }

  if (req.method === "POST" && url === "/graphql") {
    respondJson(res, 401, {
      message: "Requires authentication",
      documentation_url: "https://docs.github.com/graphql",
    });
    return true;
  }

  respondJson(res, 404, { message: "Not Found (e2e mock)" });
  return true;
}

function handleMcpRegistry(
  req: http.IncomingMessage,
  res: http.ServerResponse,
): boolean {
  const url = new URL(req.url ?? "/", "https://api.mcp.github.com");

  if (req.method === "GET" && url.pathname.startsWith("/v0.1/servers")) {
    respondJson(res, 200, { servers: [], metadata: {} });
    return true;
  }

  respondJson(res, 404, { error: "Not Found (e2e mock)" });
  return true;
}

function respondJson(
  res: http.ServerResponse,
  statusCode: number,
  body: unknown,
): void {
  const data = JSON.stringify(body);
  res.writeHead(statusCode, {
    "content-type": "application/json",
    "content-length": Buffer.byteLength(data),
  });
  res.end(data);
}

function forwardToCapiProxy(
  clientReq: http.IncomingMessage,
  clientRes: http.ServerResponse,
  capiProxyUrl: string,
): Promise<boolean> {
  return new Promise((resolve) => {
    const target = new URL(capiProxyUrl);
    const chunks: Buffer[] = [];
    clientReq.on("data", (chunk: Buffer) => chunks.push(chunk));
    clientReq.on("error", (err) => {
      if (!clientRes.headersSent) {
        clientRes.writeHead(502, { "content-type": "text/plain" });
        clientRes.end(`E2E proxy: client request error: ${err.message}`);
      } else {
        clientRes.destroy(err);
      }
      resolve(true);
    });
    clientReq.on("end", () => {
      const proxyReq = http.request(
        {
          hostname: target.hostname,
          port: target.port,
          path: clientReq.url,
          method: clientReq.method,
          headers: {
            ...clientReq.headers,
            host: target.host,
          },
        },
        (proxyRes) => {
          clientRes.writeHead(proxyRes.statusCode ?? 502, proxyRes.headers);
          proxyRes.pipe(clientRes);
          proxyRes.on("end", () => resolve(true));
          proxyRes.on("error", (err) => {
            clientRes.destroy(err);
            resolve(true);
          });
        },
      );
      proxyReq.on("error", (err) => {
        if (!clientRes.headersSent) {
          clientRes.writeHead(502, {
            "content-type": "application/json",
            "x-github-request-id": "e2e-proxy-error",
          });
          clientRes.end(
            JSON.stringify({
              error: `E2E proxy: CAPI forward error: ${err.message}`,
            }),
          );
        }
        resolve(true);
      });
      if (chunks.length > 0) {
        proxyReq.write(Buffer.concat(chunks));
      }
      proxyReq.end();
    });
  });
}
