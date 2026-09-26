/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Stages the native runtime artifacts for one platform classifier.
 *
 * Steps:
 *   1. Read the pinned version from `nodejs/package.json`.
 *   2. Use the enclosing runtime checkout when present, or download the matching release.
 *   3. Verify downloaded tarballs against the release checksum.
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
import { findRuntimeRoot } from '../../../scripts/runtime-layout.mjs';

const excludedTopLevel = new Set([
  'app.js',
  'assets',
  'changelog.json',
  'copilot',
  'copilot.exe',
  'foundry-local-sdk',
  'index.js',
  'LICENSE.md',
  'napi-oop-runtime',
  'npm-loader.js',
  'package.json',
  'pvrecorder',
  'queries',
  'README.md',
  'sea-loader.js',
  'webview',
]);

const [repoRoot, stagingDir, classifier] = process.argv.slice(2);

if (!repoRoot || !stagingDir || !classifier) {
  console.error('Usage: node fetch-native.mjs <repoRoot> <stagingDir> <classifier>');
  process.exit(1);
}

const packagePath = path.join(repoRoot, 'nodejs', 'package.json');
const packageJson = JSON.parse(fs.readFileSync(packagePath, 'utf8'));
const version = packageJson.copilotCliVersion;
if (!version) {
  console.error(`Could not find copilotCliVersion in ${packagePath}`);
  process.exit(1);
}
const assetName = `github-copilot-${version}-${classifier}.tgz`;

const outDir = path.join(stagingDir, classifier);
const resourceDir = path.join(outDir, 'native', classifier);
const runtimePath = path.join(resourceDir, 'runtime.node');
const isWindows = classifier.startsWith('win32');
const wrapperFilename = isWindows ? 'copilot-runtime.exe' : 'copilot-runtime';
const wrapperPath = path.join(resourceDir, wrapperFilename);
const inventoryPath = path.join(resourceDir, 'runtime-assets.list');
const platformPropertiesPath = path.join(resourceDir, 'platform.properties');
const expectedPlatformProperties = `classifier=${classifier}\nversion=${version}\n`;
const stagingSchema = 'hostless-runtime-v3';
const stampPath = path.join(outDir, '.version');
const runtimeRoot = findRuntimeRoot(repoRoot);
const localPackageRoot =
  !process.env.COPILOT_CLI_RELEASE_TARBALL && runtimeRoot
    ? path.join(runtimeRoot, 'dist-cli')
    : undefined;
if (localPackageRoot && !fs.statSync(localPackageRoot, { throwIfNoEntry: false })?.isDirectory()) {
  throw new Error(`Same-checkout CLI not found at ${localPackageRoot}; run pnpm run build:cli first`);
}
const sourceIdentity = localPackageRoot
  ? fingerprintDirectory(localPackageRoot, classifier)
  : process.env.COPILOT_CLI_RELEASE_SHA256;

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
  const stampSourceIdentity = stampLines[2] || '';
  const stampTreeDigest = stampLines[3] || '';
  const currentTreeDigest = digestTree(resourceDir);
  const currentPlatformProperties = fs.readFileSync(platformPropertiesPath, 'utf8');
  if (
    stampSchema === stagingSchema &&
    stampVersion === version &&
    (!sourceIdentity || stampSourceIdentity === sourceIdentity) &&
    stampTreeDigest === currentTreeDigest &&
    currentPlatformProperties === expectedPlatformProperties
  ) {
    console.log(`${assetName} already staged at ${runtimePath}`);
    process.exit(0);
  }
}

fs.rmSync(outDir, { recursive: true, force: true });
fs.mkdirSync(resourceDir, { recursive: true });
const inventory = [];
let expectedHash;
if (process.env.COPILOT_CLI_RELEASE_TARBALL) {
  const archive = fs.readFileSync(process.env.COPILOT_CLI_RELEASE_TARBALL);
  expectedHash = process.env.COPILOT_CLI_RELEASE_SHA256;
  stageArchive(archive);
} else if (localPackageRoot) {
  console.log(`Staging same-checkout runtime from ${localPackageRoot} ...`);
  expectedHash = sourceIdentity;
  stageDirectory(localPackageRoot);
} else {
  console.log(`Downloading ${assetName} ...`);
  const releaseBase = (
    process.env.COPILOT_CLI_DOWNLOAD_BASE_URL ??
    'https://github.com/github/copilot-cli/releases/download'
  ).replace(/\/+$/, '');
  const releaseUrl = `${releaseBase}/v${version}`;
  const checksums = (await download(`${releaseUrl}/SHA256SUMS.txt`)).toString('utf8');
  expectedHash = findChecksum(checksums, assetName);
  const archive = await download(`${releaseUrl}/${assetName}`);
  stageArchive(archive);
}

function stageResourceFile(destinationRelative, source, mode) {
  const destination = path.resolve(resourceDir, destinationRelative);
  const resourceRoot = `${path.resolve(resourceDir)}${path.sep}`;
  if (!destination.startsWith(resourceRoot)) {
    throw new Error(`Runtime package entry escapes staging directory: ${destinationRelative}`);
  }
  fs.mkdirSync(path.dirname(destination), { recursive: true });
  fs.copyFileSync(source, destination);
  fs.chmodSync(destination, mode);
  inventory.push(`${mode.toString(8)}\t${destinationRelative.split(path.sep).join('/')}`);
}

function stageArchive(archive) {
  if (!expectedHash || !/^[a-fA-F0-9]{64}$/.test(expectedHash)) {
    throw new Error(`Missing or invalid SHA-256 for ${assetName}`);
  }
  const actual = createHash('sha256').update(archive).digest('hex');
  if (actual !== expectedHash.toLowerCase()) {
    throw new Error(`Integrity verification failed for ${assetName}: expected ${expectedHash}, received ${actual}`);
  }
  console.log(`Integrity verified (${expectedHash.slice(0, 20)}...).`);

  const members = execFileSync('tar', ['-tzf', '-'], { encoding: 'utf8', input: archive })
    .split(/\r?\n/)
    .filter(Boolean);
  const listings = execFileSync('tar', ['-tvzf', '-'], { encoding: 'utf8', input: archive })
    .split(/\r?\n/)
    .filter(Boolean);
  if (members.length !== listings.length) {
    throw new Error(`Inconsistent archive listings for ${assetName}`);
  }
  const files = [];
  for (const [index, member] of members.entries()) {
    const destinationRelative = hostlessRuntimePath(member, classifier);
    if (destinationRelative === null) {
      continue;
    }
    const listing = listings[index];
    if (listing.startsWith('d')) {
      continue;
    }
    // BusyBox renders hard links as regular files with an appended arrow.
    if (!listing.startsWith('-') || listing.includes(`${member} -> `)) {
      throw new Error(`Unsupported runtime package entry: ${member}`);
    }
    files.push({
      member,
      destinationRelative,
      mode: listing.slice(0, 10).includes('x') ? 0o755 : 0o644,
    });
  }

  const scratch = fs.mkdtempSync(path.resolve(outDir, '.archive-'));
  try {
    const extracted = path.join(scratch, 'files');
    const selection = path.join(scratch, 'members');
    fs.mkdirSync(extracted);
    // Newline-delimited lists work with BusyBox tar and avoid Windows' command-line limit.
    fs.writeFileSync(selection, files.map(({ member }) => `${member}\n`).join(''));
    if (files.length > 0) {
      // Pass native Windows paths through cwd rather than tar's argument parser.
      execFileSync('tar', ['-xzf', '-', '-T', '../members'], { input: archive, cwd: extracted });
    }
    for (const { member, destinationRelative, mode } of files) {
      stageResourceFile(destinationRelative, path.join(extracted, member), mode);
    }
  } finally {
    fs.rmSync(scratch, { recursive: true, force: true });
  }
}

function stageDirectory(packageRoot) {
  for (const source of walkFiles(packageRoot)) {
    const relative = path.relative(packageRoot, source).split(path.sep).join('/');
    const destinationRelative = hostlessRuntimePath(`package/${relative}`, classifier);
    if (destinationRelative === null) {
      continue;
    }
    const mode = fs.statSync(source).mode & 0o111 ? 0o755 : 0o644;
    stageResourceFile(destinationRelative, source, mode);
  }
}

inventory.sort();
fs.writeFileSync(inventoryPath, `${inventory.join('\n')}\n`);

if (!fs.existsSync(runtimePath) || !fs.existsSync(wrapperPath)) {
  if (localPackageRoot) {
    throw new Error(
      `Same-checkout CLI artifacts for ${classifier} not found under ${localPackageRoot}; run pnpm run build:cli first`,
    );
  }
  throw new Error(`${assetName} is missing the runtime wrapper pair`);
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

function fingerprintDirectory(directory, platform) {
  const hash = createHash('sha256');
  for (const file of walkFiles(directory).sort()) {
    const relative = path.relative(directory, file).split(path.sep).join('/');
    if (hostlessRuntimePath(`package/${relative}`, platform) === null) {
      continue;
    }
    const stat = fs.statSync(file);
    hash
      .update(relative)
      .update('\0')
      .update(`${stat.size}`)
      .update('\0')
      .update(`${stat.mtimeMs}`)
      .update('\0')
      .update(`${stat.mode & 0o777}`)
      .update('\0');
  }
  return hash.digest('hex');
}

function digestTree(directory) {
  const hash = createHash('sha512');
  for (const file of walkFiles(directory).sort()) {
    const relative = path.relative(directory, file).split(path.sep).join('/');
    hash.update(relative).update('\0').update(fs.readFileSync(file)).update('\0');
  }
  return `sha512-${hash.digest('base64')}`;
}

async function download(url) {
  let lastError;
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      // lgtm[js/file-access-to-http] The repository-pinned CLI version intentionally selects the release asset.
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

function findChecksum(checksums, assetName) {
  for (const line of checksums.split(/\r?\n/)) {
    const [hash, name] = line.trim().split(/\s+/, 2);
    if (name?.replace(/^\*/, '') === assetName && /^[a-fA-F0-9]{64}$/.test(hash)) {
      return hash.toLowerCase();
    }
  }
  throw new Error(`SHA256SUMS.txt does not contain ${assetName}`);
}
