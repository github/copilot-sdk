/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Downloads the native runtime artifacts for one platform classifier.
 *
 * Steps:
 *   1. Read the pinned version from `nodejs/package.json`.
 *   2. Download the platform npm tarball and checksums from the matching
 *      `github/copilot-cli` release.
 *   3. Verify the downloaded tarball against the release SHA-256.
 *   4. Stage the hostless runtime tree, flattening the selected prebuild directory
 *      beside the package's retained top-level runtime assets.
 *   5. Write an inventory consumed by the SDK's generic classpath extractor.
 *   6. Write `<staging>/<classifier>/native/<classifier>/platform.properties`.
 *
 * Usage: node fetch-native.mjs <repoRoot> <stagingDir> <classifier>
 */

import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const excludedTopLevel = new Set([
  'app.js',
  'assets',
  'changelog.json',
  'copilot',
  'copilot.exe',
  'copilot-sdk',
  'foundry-local-sdk',
  'index.js',
  'LICENSE.md',
  'napi-oop-runtime',
  'npm-loader.js',
  'package.json',
  'preloads',
  'pvrecorder',
  'queries',
  'README.md',
  'sdk',
  'sea-loader.js',
  'webview',
]);

const [repoRoot, stagingDir, classifier] = process.argv.slice(2);

if (!repoRoot || !stagingDir || !classifier) {
  console.error('Usage: node fetch-native.mjs <repoRoot> <stagingDir> <classifier>');
  process.exit(1);
}

const packagePath = path.join(repoRoot, 'nodejs', 'package.json');
const packageName = `@github/copilot-${classifier}`;
const packageJson = JSON.parse(fs.readFileSync(packagePath, 'utf8'));
const version = packageJson.copilotCliVersion;
if (!version) {
  console.error(`Could not find copilotCliVersion in ${packagePath}`);
  process.exit(1);
}

const outDir = path.join(stagingDir, classifier);
const resourceDir = path.join(outDir, 'native', classifier);
const runtimePath = path.join(resourceDir, 'runtime.node');
const isWindows = classifier.startsWith('win32');
const wrapperFilename = isWindows ? 'copilot-runtime.exe' : 'copilot-runtime';
const wrapperPath = path.join(resourceDir, wrapperFilename);
const inventoryPath = path.join(resourceDir, 'runtime-assets.list');
const platformPropertiesPath = path.join(resourceDir, 'platform.properties');
const expectedPlatformProperties = `classifier=${classifier}\nversion=${version}\n`;
const stagingSchema = 'hostless-runtime-v2';
const stampPath = path.join(outDir, '.version');

// Idempotence: skip the download only when every required staged artifact
// matches the package identity recorded in the stamp.
if (
  fs.existsSync(runtimePath) &&
  fs.existsSync(wrapperPath) &&
  fs.existsSync(inventoryPath) &&
  fs.existsSync(platformPropertiesPath) &&
  fs.existsSync(stampPath)
) {
  const stampLines = fs.readFileSync(stampPath, 'utf8').trim().split('\n');
  const stampSchema = stampLines[0] || '';
  const stampVersion = stampLines[1] || '';
  const stampTreeDigest = stampLines[3] || '';
  const currentTreeDigest = digestTree(resourceDir);
  const currentPlatformProperties = fs.readFileSync(platformPropertiesPath, 'utf8');
  if (
    stampSchema === stagingSchema &&
    stampVersion === version &&
    stampTreeDigest === currentTreeDigest &&
    currentPlatformProperties === expectedPlatformProperties
  ) {
    console.log(`${packageName}@${version} already staged at ${runtimePath}`);
    process.exit(0);
  }
}

fs.rmSync(outDir, { recursive: true, force: true });
fs.mkdirSync(resourceDir, { recursive: true });

console.log(`Downloading ${packageName}@${version} ...`);
const assetName = `github-copilot-${version}-${classifier}.tgz`;
const tarballName = assetName;
const tarballPath = path.join(outDir, tarballName);
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
  const [checksums, downloadedArchive] = await Promise.all([
    download(`${releaseUrl}/SHA256SUMS.txt`).then((data) => data.toString('utf8')),
    download(`${releaseUrl}/${assetName}`),
  ]);
  expectedHash = checksumForAsset(checksums, assetName);
  archive = downloadedArchive;
}
if (!expectedHash || !/^[a-fA-F0-9]{64}$/.test(expectedHash)) {
  throw new Error(`Missing or invalid SHA-256 for ${assetName}`);
}
fs.writeFileSync(tarballPath, archive);
const actual = createHash('sha256').update(archive).digest('hex');
if (actual !== expectedHash.toLowerCase()) {
  console.error(`Integrity verification failed for ${tarballPath}`);
  console.error(`  expected: ${expectedHash}`);
  console.error(`  actual:   ${actual}`);
  process.exit(1);
}
console.log(`Integrity verified (${expectedHash.slice(0, 20)}...).`);

const inventory = [];
const members = execFileSync('tar', ['-tzf', tarballPath], { encoding: 'utf8' })
  .split(/\r?\n/)
  .filter(Boolean);
for (const member of members) {
  const destinationRelative = hostlessRuntimePath(member, classifier);
  if (destinationRelative === null) {
    continue;
  }
  const listing = execFileSync('tar', ['-tvzf', tarballPath, member], { encoding: 'utf8' }).trim();
  if (listing.startsWith('d')) {
    continue;
  }
  if (!listing.startsWith('-')) {
    throw new Error(`Unsupported runtime package entry: ${member}`);
  }
  const content = execFileSync('tar', ['-xOzf', tarballPath, member], {
    encoding: null,
    maxBuffer: 512 * 1024 * 1024,
  });
  const destination = path.resolve(resourceDir, destinationRelative);
  const resourceRoot = `${path.resolve(resourceDir)}${path.sep}`;
  if (!destination.startsWith(resourceRoot)) {
    throw new Error(`Runtime package entry escapes staging directory: ${member}`);
  }
  fs.mkdirSync(path.dirname(destination), { recursive: true });
  fs.writeFileSync(destination, content);
  const mode = listing.slice(0, 10).includes('x') ? 0o755 : 0o644;
  fs.chmodSync(destination, mode);
  inventory.push(`${mode.toString(8)}\t${destinationRelative.split(path.sep).join('/')}`);
}
inventory.sort();
fs.writeFileSync(inventoryPath, `${inventory.join('\n')}\n`);

fs.rmSync(tarballPath, { force: true });

if (!fs.existsSync(runtimePath) || !fs.existsSync(wrapperPath)) {
  throw new Error(`Package ${packageName}@${version} is missing the runtime wrapper pair`);
}
fs.writeFileSync(platformPropertiesPath, expectedPlatformProperties);
const treeDigest = digestTree(resourceDir);
fs.writeFileSync(stampPath, `${stagingSchema}\n${version}\n${expectedHash}\n${treeDigest}\n`);

console.log(`Staged ${runtimePath}`);

function hostlessRuntimePath(packageRelative, platform) {
  if (packageRelative.includes('\\')) {
    return null;
  }
  const parts = packageRelative.split('/');
  if (parts[0] !== 'package' || parts.some((part) => !part || part === '..')) {
    return null;
  }
  parts.shift();
  const topLevel = parts[0];
  const fileName = parts.at(-1);
  if (
    excludedTopLevel.has(topLevel) ||
    (topLevel.startsWith('tree-sitter') && topLevel.endsWith('.wasm')) ||
    (topLevel.startsWith('voice-') && topLevel.endsWith('.js')) ||
    fileName === 'cli-native.node' ||
    parts.includes('mediaremote-adapter') ||
    fileName.startsWith('copilot-runtime-bin')
  ) {
    return null;
  }
  if (topLevel === 'prebuilds') {
    if (parts[1] !== platform || parts.length < 3) {
      return null;
    }
    return path.join(...parts.slice(2));
  }
  return path.join(...parts);
}

function walkFiles(directory) {
  const files = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const entryPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...walkFiles(entryPath));
    } else if (entry.isFile()) {
      files.push(entryPath);
    }
  }
  return files;
}

function digestTree(directory) {
  const hash = createHash('sha512');
  for (const file of walkFiles(directory).sort()) {
    const relative = path.relative(directory, file).split(path.sep).join('/');
    hash.update(relative).update('\0').update(fs.readFileSync(file)).update('\0');
  }
  return `sha512-${hash.digest('base64')}`;
}

function checksumForAsset(checksums, assetName) {
  for (const line of checksums.split(/\r?\n/)) {
    const [hash, name] = line.trim().split(/\s+/, 2);
    if (name?.replace(/^\*/, '') === assetName && /^[a-fA-F0-9]{64}$/.test(hash)) {
      return hash;
    }
  }
  throw new Error(`SHA256SUMS.txt does not contain ${assetName}`);
}

async function download(url) {
  let lastError;
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const response = await fetch(url);
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
