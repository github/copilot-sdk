#!/usr/bin/env node

/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, '../../..');
const packagePath = path.join(repoRoot, 'nodejs', 'package.json');
const outputDir = path.resolve(
  process.env.COPILOT_CLI_SCHEMA_OUTPUT ?? path.join(scriptDir, 'target', 'schemas'),
);
// Schemas are platform-independent; use one asset consistently on every codegen host.
const platform = process.env.COPILOT_CLI_SCHEMA_PLATFORM ?? 'linux-x64';
const version =
  process.env.COPILOT_CLI_VERSION ??
  JSON.parse(fs.readFileSync(packagePath, 'utf8')).copilotCliVersion;

if (!version) {
  throw new Error(`Could not find copilotCliVersion in ${packagePath}`);
}

const assetName = `github-copilot-${version}-${platform}.tgz`;
const releaseBase = (
  process.env.COPILOT_CLI_DOWNLOAD_BASE_URL ??
  'https://github.com/github/copilot-cli/releases/download'
).replace(/\/+$/, '');

let archive;
let expectedHash;
if (process.env.COPILOT_CLI_RELEASE_TARBALL) {
  archive = fs.readFileSync(process.env.COPILOT_CLI_RELEASE_TARBALL);
  expectedHash = process.env.COPILOT_CLI_RELEASE_SHA256;
} else {
  const releaseUrl = `${releaseBase}/v${version}`;
  const checksums = (await download(`${releaseUrl}/SHA256SUMS.txt`)).toString('utf8');
  expectedHash = findChecksum(checksums, assetName);
  archive = await download(`${releaseUrl}/${assetName}`);
}

if (!expectedHash || !/^[a-fA-F0-9]{64}$/.test(expectedHash)) {
  throw new Error(`Missing or invalid SHA-256 for ${assetName}`);
}
const actualHash = createHash('sha256').update(archive).digest('hex');
if (actualHash !== expectedHash.toLowerCase()) {
  throw new Error(
    `Integrity verification failed for ${assetName}: expected ${expectedHash}, got ${actualHash}`,
  );
}

const schemaNames = ['api.schema.json', 'session-events.schema.json'];
const members = execFileSync('tar', ['-tzf', '-'], {
  encoding: 'utf8',
  input: archive,
  maxBuffer: 512 * 1024 * 1024,
})
  .split(/\r?\n/)
  .filter(Boolean);
const outputParent = path.dirname(outputDir);
fs.mkdirSync(outputParent, { recursive: true });
const stagingDir = fs.mkdtempSync(path.join(outputParent, '.schemas-'));

try {
  for (const schemaName of schemaNames) {
    const member = `package/schemas/${schemaName}`;
    if (members.filter((candidate) => candidate === member).length !== 1) {
      throw new Error(`${assetName} must contain exactly one ${member}`);
    }
    const contents = execFileSync('tar', ['-xOzf', '-', member], {
      encoding: null,
      input: archive,
      maxBuffer: 512 * 1024 * 1024,
    });
    JSON.parse(contents.toString('utf8'));
    fs.writeFileSync(path.join(stagingDir, schemaName), contents);
  }

  fs.rmSync(outputDir, { recursive: true, force: true });
  fs.renameSync(stagingDir, outputDir);
} finally {
  fs.rmSync(stagingDir, { recursive: true, force: true });
}

console.log(`Staged Copilot CLI ${version} schemas at ${outputDir}`);

async function download(url) {
  let lastError;
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      // lgtm[js/file-access-to-http] The repository-pinned CLI version selects the release asset.
      const response = await fetch(url, { signal: AbortSignal.timeout(600_000) });
      if (response.ok) {
        return Buffer.from(await response.arrayBuffer());
      }
      await response.body?.cancel();
      lastError = new Error(`${response.status} ${response.statusText}`);
      if (response.status >= 400 && response.status < 500 && response.status !== 408 && response.status !== 429) {
        break;
      }
    } catch (error) {
      lastError = error;
    }
    if (attempt < 2) {
      await new Promise((resolve) => setTimeout(resolve, 2 ** attempt * 1000));
    }
  }
  throw new Error(`Failed to download ${url}: ${lastError}`);
}

function findChecksum(checksums, expectedAssetName) {
  for (const line of checksums.split(/\r?\n/)) {
    const [hash, name] = line.trim().split(/\s+/, 2);
    if (
      name?.replace(/^\*/, '') === expectedAssetName &&
      /^[a-fA-F0-9]{64}$/.test(hash)
    ) {
      return hash.toLowerCase();
    }
  }
  throw new Error(`SHA256SUMS.txt does not contain ${expectedAssetName}`);
}
