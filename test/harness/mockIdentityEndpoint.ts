/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import http from "http";
import type { AddressInfo } from "net";

/** Fixed fake AAD token the mock identity endpoint mints for every request. */
export const MOCK_IDENTITY_TOKEN = "fake-managed-identity-token";

/** Secret the runtime must echo back in the `x-identity-header` header. */
export const MOCK_IDENTITY_HEADER_SECRET = "fake-identity-header-secret";

/** Path the token is served from (the value baked into `IDENTITY_ENDPOINT`). */
const TOKEN_PATH = "/msi/token";
/** Control path: GET returns the recorded token requests as JSON. */
const RECORDED_PATH = "/__identity/recorded";
/** Control path: POST clears the recorded token requests. */
const RESET_PATH = "/__identity/reset";
/** Control path: POST closes the endpoint so its process can exit cleanly. */
const STOP_PATH = "/__identity/stop";
/** Control path: POST sets the token lifetime / rotation behaviour. */
const CONFIGURE_PATH = "/__identity/configure";

/** Default token lifetime, comfortably outside the runtime's refresh buffer. */
const DEFAULT_EXPIRES_IN_SECONDS = 3600;

/** App Service / Functions managed identity selector query params. */
const IDENTITY_SELECTOR_PARAMS = ["client_id", "principal_id", "mi_res_id"] as const;

/** A single token request the runtime made to the mock identity endpoint. */
export interface RecordedIdentityRequest {
  /** `resource` query parameter the runtime asked a token for. */
  resource: string | null;
  /** `api-version` query parameter. */
  apiVersion: string | null;
  /** Value of the `x-identity-header` secret header (undefined if absent). */
  identityHeader: string | undefined;
  /**
   * Identity selector params present on the request. System-assigned identities
   * send none; user-assigned identities send exactly one of `client_id`,
   * `principal_id`, or `mi_res_id`.
   */
  identityParams: Record<string, string>;
  /**
   * The exact `access_token` value returned for this request. With token
   * rotation enabled this changes per request, letting tests prove a refreshed
   * token (rather than a cached one) reached the model.
   */
  issuedToken: string;
}

/** Token lifetime / rotation behaviour of the mock identity endpoint. */
export interface MockIdentityConfig {
  /**
   * Lifetime to report via `expires_in` (seconds). Set this below the runtime's
   * 5-minute refresh buffer (e.g. `1`) to make every cached token immediately
   * eligible for refresh.
   */
  expiresInSeconds?: number;
  /**
   * When true, each token request returns a distinct token value
   * (`<token>-<n>`) so tests can observe refreshes. When false (default) the
   * fixed {@link MOCK_IDENTITY_TOKEN} is always returned.
   */
  rotateTokens?: boolean;
}

/** Connection details a test needs to exercise the mock identity endpoint. */
export interface MockIdentityEndpointInfo {
  /** URL to assign to the `IDENTITY_ENDPOINT` env var. */
  endpoint: string;
  /** Secret to assign to the `IDENTITY_HEADER` env var. */
  header: string;
  /** Fake bearer token the runtime injects (`Authorization: Bearer <token>`). */
  token: string;
  /** GET URL returning the recorded token requests as JSON. */
  recordedUrl: string;
  /** POST URL clearing the recorded token requests. */
  resetUrl: string;
  /** POST URL that closes the endpoint so its host process can exit. */
  stopUrl: string;
  /** POST URL that sets the token lifetime / rotation behaviour. */
  configureUrl: string;
}

/**
 * A mock of the Azure App Service / Functions managed identity token endpoint
 * (the `IDENTITY_ENDPOINT` + `IDENTITY_HEADER` contract). It mints a fixed fake
 * AAD token and records the resource + identity selector each token request
 * asked for, so e2e tests can assert the runtime resolved the managed identity
 * correctly.
 *
 * It lives in the shared harness and is exposed over HTTP (token + control
 * endpoints) so SDK e2e tests in any language can drive BYOK managed identity
 * without re-implementing the endpoint.
 */
export class MockIdentityEndpoint {
  private server: http.Server | undefined;
  private readonly requests: RecordedIdentityRequest[] = [];
  private baseUrl = "";
  private expiresInSeconds = DEFAULT_EXPIRES_IN_SECONDS;
  private rotateTokens = false;
  private issueCount = 0;

  /** Starts the endpoint on a random loopback port. */
  async start(): Promise<MockIdentityEndpointInfo> {
    const server = http.createServer((req, res) => this.handle(req, res));
    this.server = server;
    await new Promise<void>((resolve, reject) => {
      server.once("error", reject);
      server.listen(0, "127.0.0.1", () => resolve());
    });
    const { port } = server.address() as AddressInfo;
    this.baseUrl = `http://127.0.0.1:${port}`;
    return this.info();
  }

  /** Connection details for the running endpoint. */
  info(): MockIdentityEndpointInfo {
    return {
      endpoint: `${this.baseUrl}${TOKEN_PATH}`,
      header: MOCK_IDENTITY_HEADER_SECRET,
      token: MOCK_IDENTITY_TOKEN,
      recordedUrl: `${this.baseUrl}${RECORDED_PATH}`,
      resetUrl: `${this.baseUrl}${RESET_PATH}`,
      stopUrl: `${this.baseUrl}${STOP_PATH}`,
      configureUrl: `${this.baseUrl}${CONFIGURE_PATH}`,
    };
  }

  /** Sets the token lifetime / rotation behaviour. */
  configure(config: MockIdentityConfig): void {
    if (config.expiresInSeconds !== undefined) {
      this.expiresInSeconds = config.expiresInSeconds;
    }
    if (config.rotateTokens !== undefined) {
      this.rotateTokens = config.rotateTokens;
    }
  }

  /** Token requests recorded so far, in arrival order. */
  recordedRequests(): readonly RecordedIdentityRequest[] {
    return this.requests;
  }

  /** Clears recorded requests and restores the default token behaviour. */
  reset(): void {
    this.requests.length = 0;
    this.expiresInSeconds = DEFAULT_EXPIRES_IN_SECONDS;
    this.rotateTokens = false;
    this.issueCount = 0;
  }

  async stop(): Promise<void> {
    const server = this.server;
    if (!server) {
      return;
    }
    this.server = undefined;
    await new Promise<void>((resolve) => server.close(() => resolve()));
  }

  private handle(req: http.IncomingMessage, res: http.ServerResponse): void {
    const url = new URL(req.url ?? "/", this.baseUrl);

    if (req.method === "POST" && url.pathname === RESET_PATH) {
      this.reset();
      respondJson(res, 200, { ok: true });
      return;
    }

    if (req.method === "POST" && url.pathname === STOP_PATH) {
      respondJson(res, 200, { ok: true });
      res.on("finish", () => void this.stop());
      return;
    }

    if (req.method === "POST" && url.pathname === CONFIGURE_PATH) {
      void readBody(req).then((body) => {
        try {
          this.configure(body ? (JSON.parse(body) as MockIdentityConfig) : {});
          respondJson(res, 200, { ok: true });
        } catch {
          respondJson(res, 400, { error: "Invalid configure body" });
        }
      });
      return;
    }

    if (req.method === "GET" && url.pathname === RECORDED_PATH) {
      respondJson(res, 200, this.requests);
      return;
    }

    if (url.pathname !== TOKEN_PATH) {
      respondJson(res, 404, { error: "Not Found (mock identity endpoint)" });
      return;
    }

    const identityParams: Record<string, string> = {};
    for (const key of IDENTITY_SELECTOR_PARAMS) {
      const value = url.searchParams.get(key);
      if (value !== null) {
        identityParams[key] = value;
      }
    }
    const resource = url.searchParams.get("resource");
    const issuedToken = this.rotateTokens
      ? `${MOCK_IDENTITY_TOKEN}-${++this.issueCount}`
      : MOCK_IDENTITY_TOKEN;
    this.requests.push({
      resource,
      apiVersion: url.searchParams.get("api-version"),
      identityHeader: (req.headers["x-identity-header"] as string | undefined) ?? undefined,
      identityParams,
      issuedToken,
    });

    respondJson(res, 200, {
      access_token: issuedToken,
      expires_in: this.expiresInSeconds,
      token_type: "Bearer",
      resource,
    });
  }
}

/** Reads the full request body as a string. */
function readBody(req: http.IncomingMessage): Promise<string> {
  return new Promise((resolve, reject) => {
    const chunks: Buffer[] = [];
    req.on("data", (chunk: Buffer) => chunks.push(chunk));
    req.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
    req.on("error", reject);
  });
}

function respondJson(res: http.ServerResponse, statusCode: number, body: unknown): void {
  const data = JSON.stringify(body);
  res.writeHead(statusCode, {
    "content-type": "application/json",
    "content-length": Buffer.byteLength(data),
  });
  res.end(data);
}
