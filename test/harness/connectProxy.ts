/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import fs from "fs";
import http from "http";
import net from "net";
import os from "os";
import path from "path";
import tls from "tls";
import {
  type CaData,
  createSecureContextForHost,
  generateCA,
} from "./certUtils";

const debugLogPath = process.env.E2E_PROXY_DEBUG
  ? path.join(os.tmpdir(), `e2e-proxy-debug-${process.pid}.log`)
  : undefined;

function debugLog(msg: string): void {
  if (debugLogPath) {
    fs.appendFileSync(
      debugLogPath,
      `[${new Date().toISOString()}] [connect] ${msg}\n`,
    );
  }
}

export type RequestHandler = (
  req: http.IncomingMessage,
  res: http.ServerResponse,
  targetHost: string,
) => boolean | Promise<boolean>;

export class ConnectProxy {
  private proxyServer?: http.Server;
  private internalServer?: http.Server;
  private ca?: CaData;
  private certCache = new Map<string, tls.SecureContext>();
  private _caFilePath?: string;
  private _proxyUrl?: string;
  private _connectLog: Array<{
    host: string;
    port: string;
    timestamp: number;
  }> = [];
  private interceptDomains: Set<string>;
  private passthroughDomains: Set<string>;
  private onBlockedConnection?: (host: string, port: string) => void;
  private openSockets = new Set<net.Socket>();

  constructor(
    private handler: RequestHandler,
    options?: {
      interceptDomains?: string[];
      passthroughDomains?: string[];
      onBlockedConnection?: (host: string, port: string) => void;
    },
  ) {
    this.interceptDomains = new Set(options?.interceptDomains ?? []);
    this.passthroughDomains = new Set(options?.passthroughDomains ?? []);
    this.onBlockedConnection = options?.onBlockedConnection;
  }

  get proxyUrl(): string {
    if (!this._proxyUrl) {
      throw new Error("ConnectProxy not started");
    }
    return this._proxyUrl;
  }

  get caFilePath(): string {
    if (!this._caFilePath) {
      throw new Error("ConnectProxy not started");
    }
    return this._caFilePath;
  }

  get connectLog(): ReadonlyArray<{
    host: string;
    port: string;
    timestamp: number;
  }> {
    return this._connectLog;
  }

  async start(): Promise<void> {
    this.ca = generateCA();
    const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "e2e-proxy-ca-"));
    fs.writeFileSync(path.join(tmpDir, "test-ca.pem"), this.ca.certPem);
    this._caFilePath = path.join(tmpDir, "test-ca-bundle.pem");
    fs.writeFileSync(
      this._caFilePath,
      [...tls.rootCertificates, this.ca.certPem].join("\n"),
    );

    this.internalServer = http.createServer((req, res) => {
      const socket = req.socket as tls.TLSSocket & { _connectTarget?: string };
      const targetHost = socket._connectTarget ?? req.headers.host ?? "unknown";

      void Promise.resolve(this.handler(req, res, targetHost))
        .then((handled) => {
          if (!handled && !res.headersSent) {
            res.writeHead(502, { "content-type": "text/plain" });
            res.end(
              `E2E proxy: no handler for ${req.method} ${targetHost}${req.url}`,
            );
          }
        })
        .catch((err) => {
          console.warn(
            `[E2E proxy] handler error for ${req.method} ${targetHost}${req.url}: ${err}`,
          );
          if (!res.headersSent) {
            res.writeHead(502, { "content-type": "text/plain" });
            res.end("E2E proxy: handler error");
          }
        });
    });

    this.proxyServer = http.createServer((req, res) => {
      this.handleForwardProxy(req, res);
    });

    this.proxyServer.on("connect", (req, clientSocket, head) => {
      this.handleConnect(req, clientSocket as net.Socket, head);
    });

    await new Promise<void>((resolve, reject) => {
      this.proxyServer!.on("error", reject);
      this.proxyServer!.listen(0, "127.0.0.1", () => resolve());
    });

    const addr = this.proxyServer.address() as net.AddressInfo;
    this._proxyUrl = `http://${addr.address}:${addr.port}`;
  }

  async stop(): Promise<void> {
    for (const socket of this.openSockets) {
      socket.destroy();
    }
    this.openSockets.clear();

    const closeServer = (server?: http.Server) =>
      new Promise<void>((resolve) => {
        if (!server) {
          resolve();
          return;
        }
        server.close(() => resolve());
      });

    await Promise.all([
      closeServer(this.proxyServer),
      closeServer(this.internalServer),
    ]);

    if (this._caFilePath) {
      try {
        fs.rmSync(path.dirname(this._caFilePath), {
          recursive: true,
          force: true,
        });
      } catch {
        // Best-effort cleanup.
      }
    }
  }

  private handleConnect(
    req: http.IncomingMessage,
    clientSocket: net.Socket,
    head: Buffer,
  ) {
    const { host, port } = parseConnectTarget(req.url ?? "");
    debugLog(`CONNECT ${host}:${port}`);
    if (!host) {
      clientSocket.write("HTTP/1.1 400 Bad Request\r\n\r\n");
      clientSocket.destroy();
      return;
    }

    this._connectLog.push({ host, port, timestamp: Date.now() });

    if (this.passthroughDomains.has(host)) {
      this.pipeToRealTarget(clientSocket, head, host, port);
      return;
    }

    if (!this.interceptDomains.has(host)) {
      this.onBlockedConnection?.(host, port);
      clientSocket.write("HTTP/1.1 502 Blocked by E2E proxy\r\n\r\n");
      clientSocket.destroy();
      return;
    }

    clientSocket.write("HTTP/1.1 200 Connection Established\r\n\r\n");

    const tlsSocket = new tls.TLSSocket(clientSocket, {
      isServer: true,
      secureContext: this.getOrCreateSecureContext(host),
      ALPNProtocols: ["http/1.1"],
    });

    this.openSockets.add(clientSocket);
    this.openSockets.add(tlsSocket);
    let cleaned = false;
    const cleanup = () => {
      if (cleaned) {
        return;
      }
      cleaned = true;
      tlsSocket.off("close", cleanup);
      clientSocket.off("close", cleanup);
      tlsSocket.off("error", onTlsError);
      clientSocket.off("error", onClientError);
      this.openSockets.delete(clientSocket);
      this.openSockets.delete(tlsSocket);
    };
    const onTlsError = (err: Error) => {
      debugLog(`TLS error for ${host}: ${err.message}`);
      cleanup();
      clientSocket.destroy();
    };
    const onClientError = () => {
      cleanup();
      tlsSocket.destroy();
    };
    tlsSocket.on("close", cleanup);
    clientSocket.on("close", cleanup);
    tlsSocket.on("error", onTlsError);
    clientSocket.on("error", onClientError);

    (tlsSocket as tls.TLSSocket & { _connectTarget?: string })._connectTarget =
      host;
    if (head.length > 0) {
      tlsSocket.unshift(head);
    }
    this.internalServer!.emit("connection", tlsSocket);
  }

  private handleForwardProxy(
    req: http.IncomingMessage,
    res: http.ServerResponse,
  ) {
    let targetHost: string;
    try {
      const url = new URL(req.url ?? "");
      targetHost = url.hostname;
      req.url = url.pathname + url.search;
    } catch {
      targetHost = req.headers.host ?? "unknown";
    }

    void Promise.resolve(this.handler(req, res, targetHost))
      .then((handled) => {
        if (!handled && !res.headersSent) {
          res.writeHead(502, { "content-type": "text/plain" });
          res.end(
            `E2E proxy: no handler for HTTP ${req.method} ${targetHost}${req.url}`,
          );
        }
      })
      .catch(() => {
        if (!res.headersSent) {
          res.writeHead(502, { "content-type": "text/plain" });
          res.end("E2E proxy: handler error");
        }
      });
  }

  private getOrCreateSecureContext(hostname: string): tls.SecureContext {
    let context = this.certCache.get(hostname);
    if (!context) {
      context = createSecureContextForHost(hostname, this.ca!);
      this.certCache.set(hostname, context);
    }
    return context;
  }

  private pipeToRealTarget(
    clientSocket: net.Socket,
    head: Buffer,
    host: string,
    port: string,
  ) {
    const targetSocket = net.connect(Number.parseInt(port, 10), host, () => {
      if (clientSocket.destroyed || targetSocket.destroyed) {
        return;
      }
      clientSocket.write("HTTP/1.1 200 Connection Established\r\n\r\n");
      if (head.length > 0) {
        targetSocket.write(head);
      }
      clientSocket.pipe(targetSocket);
      targetSocket.pipe(clientSocket);
    });

    this.openSockets.add(clientSocket);
    this.openSockets.add(targetSocket);

    let cleaned = false;
    const cleanup = () => {
      if (cleaned) {
        return;
      }
      cleaned = true;
      clientSocket.off("error", cleanup);
      clientSocket.off("close", cleanup);
      targetSocket.off("error", cleanup);
      targetSocket.off("close", cleanup);
      clientSocket.destroy();
      targetSocket.destroy();
      this.openSockets.delete(clientSocket);
      this.openSockets.delete(targetSocket);
    };
    clientSocket.on("error", cleanup);
    clientSocket.on("close", cleanup);
    targetSocket.on("error", cleanup);
    targetSocket.on("close", cleanup);
  }
}

export function parseConnectTarget(authority: string): {
  host: string;
  port: string;
} {
  if (!authority) {
    return { host: "", port: "" };
  }

  if (authority.startsWith("[")) {
    const closeBracket = authority.indexOf("]");
    if (closeBracket === -1) {
      return { host: "", port: "" };
    }
    const host = authority.slice(1, closeBracket);
    const afterBracket = authority.slice(closeBracket + 1);
    if (afterBracket === "" || afterBracket === ":") {
      return { host, port: "443" };
    }
    if (afterBracket[0] !== ":") {
      return { host: "", port: "" };
    }
    return { host, port: afterBracket.slice(1) || "443" };
  }

  const lastColon = authority.lastIndexOf(":");
  if (lastColon === -1) {
    return { host: authority, port: "443" };
  }

  const host = authority.slice(0, lastColon);
  const port = authority.slice(lastColon + 1) || "443";
  return { host, port };
}
