/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Downloads the `runtime.node` native binary for a single platform classifier
 * and stages it for packaging into a classifier JAR.
 *
 * Steps:
 *   1. Read the pinned version and the SHA-512 `integrity` value for
 *      `@github/copilot-<classifier>` from `nodejs/package-lock.json`.
 *   2. `npm pack` that exact version into the staging directory.
 *   3. Verify the downloaded tarball against the `integrity` value.
 *   4. Extract `package/prebuilds/<classifier>/runtime.node` to
 *      `<staging>/<classifier>/native/<classifier>/runtime.node`.
 *   5. Extract `package/copilot` (or `package/copilot.exe` on Windows) to
 *      `<staging>/<classifier>/native/<classifier>/copilot`.
 *   6. Write `<staging>/<classifier>/native/<classifier>/platform.properties`.
 *
 * Usage: node fetch-native.mjs <repoRoot> <stagingDir> <classifier>
 */

import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const [repoRoot, stagingDir, classifier] = process.argv.slice(2);

if (!repoRoot || !stagingDir || !classifier) {
  console.error('Usage: node fetch-native.mjs <repoRoot> <stagingDir> <classifier>');
  process.exit(1);
}

const lockPath = path.join(repoRoot, 'nodejs', 'package-lock.json');
const packageName = `@github/copilot-${classifier}`;
const lock = JSON.parse(fs.readFileSync(lockPath, 'utf8'));
const entry = lock.packages?.[`node_modules/${packageName}`];

if (!entry?.version || !entry?.integrity) {
  console.error(`Could not find version/integrity for ${packageName} in ${lockPath}`);
  process.exit(1);
}

const { version, integrity } = entry;
if (!integrity.startsWith('sha512-')) {
  console.error(`Unsupported integrity algorithm for ${packageName}: ${integrity}`);
  process.exit(1);
}

const outDir = path.join(stagingDir, classifier);
const resourceDir = path.join(outDir, 'native', classifier);
const runtimePath = path.join(resourceDir, 'runtime.node');
const isWindows = classifier.startsWith('win32');
const cliTarballMember = isWindows ? 'package/copilot.exe' : 'package/copilot';
const cliFilename = isWindows ? 'copilot.exe' : 'copilot';
const cliPath = path.join(resourceDir, cliFilename);
const platformPropertiesPath = path.join(resourceDir, 'platform.properties');
const expectedPlatformProperties = `classifier=${classifier}\nversion=${version}\n`;
const stampPath = path.join(outDir, '.version');

// Idempotence: skip the download only when every required staged artifact
// matches the package identity recorded in the stamp.
if (
  fs.existsSync(runtimePath) &&
  fs.existsSync(cliPath) &&
  fs.existsSync(platformPropertiesPath) &&
  fs.existsSync(stampPath)
) {
  const stampLines = fs.readFileSync(stampPath, 'utf8').trim().split('\n');
  const stampVersion = stampLines[0] || '';
  const stampIntegrity = stampLines[1] || '';
  const stampRuntimeDigest = stampLines[2] || '';
  const stampCliDigest = stampLines[3] || '';
  const currentRuntimeDigest = digestFile(runtimePath);
  const currentCliDigest = digestFile(cliPath);
  const currentPlatformProperties = fs.readFileSync(platformPropertiesPath, 'utf8');
  if (
    stampVersion === version &&
    stampIntegrity === integrity &&
    stampRuntimeDigest === currentRuntimeDigest &&
    stampCliDigest === currentCliDigest &&
    currentPlatformProperties === expectedPlatformProperties
  ) {
    console.log(`${packageName}@${version} already staged at ${runtimePath}`);
    process.exit(0);
  }
}

fs.rmSync(outDir, { recursive: true, force: true });
fs.mkdirSync(resourceDir, { recursive: true });

console.log(`Downloading ${packageName}@${version} ...`);
const packOutput = execFileSync('npm', ['pack', `${packageName}@${version}`, '--pack-destination', outDir], {
  encoding: 'utf8',
  shell: process.platform === 'win32',
});
const tarballName = packOutput.trim().split('\n').pop().trim();
const tarballPath = path.join(outDir, tarballName);

const actual = `sha512-${createHash('sha512').update(fs.readFileSync(tarballPath)).digest('base64')}`;
if (actual !== integrity) {
  console.error(`Integrity verification failed for ${tarballPath}`);
  console.error(`  expected: ${integrity}`);
  console.error(`  actual:   ${actual}`);
  process.exit(1);
}
console.log(`Integrity verified (${integrity.slice(0, 20)}...).`);

const memberPath = `package/prebuilds/${classifier}/runtime.node`;
execFileSync('tar', ['-xzf', tarballPath, '-C', outDir, memberPath], { stdio: 'inherit' });
fs.renameSync(path.join(outDir, memberPath), runtimePath);

// Extract the copilot CLI executable (necessary-and-sufficient runtime artifact invariant:
// host_start needs both runtime.node and the copilot CLI from the same package version).
execFileSync('tar', ['-xzf', tarballPath, '-C', outDir, cliTarballMember], { stdio: 'inherit' });
fs.renameSync(path.join(outDir, cliTarballMember), cliPath);
if (!isWindows) {
  fs.chmodSync(cliPath, 0o755);
}

fs.rmSync(path.join(outDir, 'package'), { recursive: true, force: true });
fs.rmSync(tarballPath, { force: true });

fs.writeFileSync(platformPropertiesPath, expectedPlatformProperties);
const runtimeDigest = digestFile(runtimePath);
const cliDigest = digestFile(cliPath);
fs.writeFileSync(stampPath, `${version}\n${integrity}\n${runtimeDigest}\n${cliDigest}\n`);

console.log(`Staged ${runtimePath}`);

function digestFile(filePath) {
  return `sha512-${createHash('sha512').update(fs.readFileSync(filePath)).digest('base64')}`;
}
