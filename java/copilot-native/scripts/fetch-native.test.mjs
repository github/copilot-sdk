/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { execFileSync, spawn } from 'node:child_process';
import fs from 'node:fs';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test, { after, before } from 'node:test';

const version = '1.0.79';
const checksum = '0'.repeat(64);
const runtimeContent = 'runtime content';
const wrapperContent = 'wrapper content';
const stagingSchema = 'hostless-runtime-v3';
const scriptPath = fileURLToPath(new URL('./fetch-native.mjs', import.meta.url));
const testRoot = fileURLToPath(new URL('../target/fetch-native-tests/', import.meta.url));
const releaseFiles = new Map();
const releaseRequests = [];
let releaseBase;
let releaseServer;

before(async () => {
  releaseServer = http.createServer((request, response) => {
    releaseRequests.push(request.url);
    const content = releaseFiles.get(request.url);
    if (content === undefined) {
      response.writeHead(404).end();
    } else {
      response.writeHead(200, { 'Content-Length': content.length }).end(content);
    }
  });
  await new Promise((resolve, reject) => {
    releaseServer.once('error', reject);
    releaseServer.listen(0, '127.0.0.1', resolve);
  });
  const address = releaseServer.address();
  releaseBase = `http://127.0.0.1:${address.port}`;
});

after(async () => {
  await new Promise((resolve, reject) => {
    releaseServer.close((error) => (error ? reject(error) : resolve()));
  });
});

for (const classifier of ['linux-x64', 'linux-arm64', 'win32-x64', 'win32-arm64', 'darwin-arm64']) {
  test(`${classifier}: complete hostless artifacts use incremental fast path without a CLI`, async (t) => {
    const fixture = createFixture(t, classifier);

    const result = await runScript(fixture);

    assert.equal(result.status, 0, result.stderr);
    assert.match(result.stdout, /already staged/);
  });

  test(`${classifier}: missing runtime wrapper does not use incremental fast path`, async (t) => {
    const fixture = createFixture(t, classifier);
    fs.rmSync(fixture.wrapperPath);

    const result = await runScript(fixture);

    assertRestagingAttempted(fixture, result);
  });

  test(`${classifier}: v2 staging schema does not use incremental fast path`, async (t) => {
    const fixture = createFixture(t, classifier);
    const stampPath = path.join(fixture.stagingDir, classifier, '.version');
    fs.writeFileSync(stampPath, fs.readFileSync(stampPath, 'utf8').replace(stagingSchema, 'hostless-runtime-v2'));

    const result = await runScript(fixture);

    assertRestagingAttempted(fixture, result);
  });

  test(`${classifier}: missing platform metadata does not use incremental fast path`, async (t) => {
    const fixture = createFixture(t, classifier);
    fs.rmSync(fixture.platformPropertiesPath);

    const result = await runScript(fixture);

    assertRestagingAttempted(fixture, result);
  });

  test(`${classifier}: missing retained runtime asset does not use incremental fast path`, async (t) => {
    const fixture = createFixture(t, classifier);
    fs.rmSync(fixture.ripgrepPath);

    const result = await runScript(fixture);

    assertRestagingAttempted(fixture, result);
  });

  test(`${classifier}: complete matching artifacts use incremental fast path`, async (t) => {
    const fixture = createFixture(t, classifier);

    const result = await runScript(fixture);

    assert.equal(result.status, 0, result.stderr);
    assert.match(result.stdout, /already staged/);
  });
}

test('downloads the exact release asset, verifies its manifest checksum, and stages runtime assets', async (t) => {
  const classifier = 'linux-x64';
  const fixture = createFixture(t, classifier);
  const packageRoot = path.join(fixture.repoRoot, 'package-root', 'package');
  fs.mkdirSync(path.join(packageRoot, 'prebuilds', classifier), { recursive: true });
  fs.mkdirSync(path.join(packageRoot, 'ripgrep', 'bin', classifier), { recursive: true });
  fs.mkdirSync(path.join(packageRoot, 'definitions'), { recursive: true });
  fs.writeFileSync(path.join(packageRoot, 'copilot'), 'excluded');
  fs.writeFileSync(path.join(packageRoot, 'prebuilds', classifier, 'runtime.node'), runtimeContent);
  fs.writeFileSync(path.join(packageRoot, 'prebuilds', classifier, 'copilot-runtime'), wrapperContent);
  fs.chmodSync(path.join(packageRoot, 'prebuilds', classifier, 'copilot-runtime'), 0o755);
  fs.writeFileSync(path.join(packageRoot, 'ripgrep', 'bin', classifier, 'rg'), 'ripgrep content');
  fs.chmodSync(path.join(packageRoot, 'ripgrep', 'bin', classifier, 'rg'), 0o755);
  fs.writeFileSync(path.join(packageRoot, 'definitions', 'future.json'), '{}');
  fs.writeFileSync(path.join(packageRoot, 'app.js'), 'excluded');
  fs.writeFileSync(path.join(packageRoot, 'LICENSE.md'), 'excluded');
  fs.writeFileSync(path.join(packageRoot, 'README.md'), 'excluded');
  const tarball = path.join(fixture.repoRoot, 'fixture.tgz');
  execFileSync('tar', ['-czf', tarball, '-C', path.dirname(packageRoot), 'package']);
  const packageChecksum = createHash('sha256').update(fs.readFileSync(tarball)).digest('hex');
  fs.writeFileSync(
    path.join(fixture.repoRoot, 'nodejs', 'package.json'),
    JSON.stringify({ copilotCliVersion: version }),
  );
  fs.rmSync(path.join(fixture.stagingDir, classifier), { recursive: true, force: true });

  const assetName = `github-copilot-${version}-${classifier}.tgz`;
  const releasePrefix = `/v${version}`;
  releaseFiles.set(`${releasePrefix}/SHA256SUMS.txt`, Buffer.from(`${packageChecksum}  ${assetName}\n`));
  releaseFiles.set(`${releasePrefix}/${assetName}`, fs.readFileSync(tarball));
  const requestOffset = releaseRequests.length;
  t.after(() => {
    releaseFiles.delete(`${releasePrefix}/SHA256SUMS.txt`);
    releaseFiles.delete(`${releasePrefix}/${assetName}`);
  });

  const result = await runScript(fixture);

  assert.equal(result.status, 0, result.stderr);
  assert.deepEqual(releaseRequests.slice(requestOffset), [
    `${releasePrefix}/SHA256SUMS.txt`,
    `${releasePrefix}/${assetName}`,
  ]);
  const resourceDir = path.join(fixture.stagingDir, classifier, 'native', classifier);
  assert.equal(fs.readFileSync(path.join(resourceDir, 'ripgrep', 'bin', classifier, 'rg'), 'utf8'), 'ripgrep content');
  assert.equal(fs.readFileSync(path.join(resourceDir, 'definitions', 'future.json'), 'utf8'), '{}');
  assert.equal(fs.existsSync(path.join(resourceDir, 'app.js')), false);
  assert.equal(fs.existsSync(path.join(resourceDir, 'copilot')), false);
  assert.equal(fs.existsSync(path.join(resourceDir, 'LICENSE.md')), false);
  assert.equal(fs.existsSync(path.join(resourceDir, 'README.md')), false);
  const inventory = fs.readFileSync(path.join(resourceDir, 'runtime-assets.list'), 'utf8');
  assert.match(inventory, /^644\truntime\.node$/m);
  assert.match(inventory, /^755\tcopilot-runtime$/m);
  assert.match(inventory, /^755\tripgrep\/bin\/linux-x64\/rg$/m);
});

test('stages a pre-downloaded release asset with its supplied SHA-256 without contacting a release server', async (t) => {
  const classifier = 'linux-x64';
  const fixture = createFixture(t, classifier);
  const packageRoot = path.join(fixture.repoRoot, 'local-package', 'package');
  const prebuildRoot = path.join(packageRoot, 'prebuilds', classifier);
  fs.mkdirSync(prebuildRoot, { recursive: true });
  fs.writeFileSync(path.join(prebuildRoot, 'runtime.node'), runtimeContent);
  fs.writeFileSync(path.join(prebuildRoot, 'copilot-runtime'), wrapperContent);
  fs.chmodSync(path.join(prebuildRoot, 'copilot-runtime'), 0o755);
  const tarball = path.join(fixture.repoRoot, 'pre-downloaded-release.tgz');
  execFileSync('tar', ['-czf', tarball, '-C', path.dirname(packageRoot), 'package']);
  const expectedHash = createHash('sha256').update(fs.readFileSync(tarball)).digest('hex');
  fs.rmSync(path.join(fixture.stagingDir, classifier), { recursive: true, force: true });
  const requestOffset = releaseRequests.length;

  const result = await runScript(fixture, {
    COPILOT_CLI_RELEASE_TARBALL: tarball,
    COPILOT_CLI_RELEASE_SHA256: expectedHash,
  });

  assert.equal(result.status, 0, result.stderr);
  assert.deepEqual(releaseRequests.slice(requestOffset), []);
  const resourceDir = path.join(fixture.stagingDir, classifier, 'native', classifier);
  assert.equal(fs.readFileSync(path.join(resourceDir, 'runtime.node'), 'utf8'), runtimeContent);
  assert.equal(fs.readFileSync(path.join(resourceDir, 'copilot-runtime'), 'utf8'), wrapperContent);
});

test('rejects a release asset that does not match SHA256SUMS.txt without npm fallback', async (t) => {
  const classifier = 'linux-x64';
  const fixture = createFixture(t, classifier);
  const assetName = `github-copilot-${version}-${classifier}.tgz`;
  const releasePrefix = `/v${version}`;
  releaseFiles.set(`${releasePrefix}/SHA256SUMS.txt`, Buffer.from(`${checksum}  ${assetName}\n`));
  releaseFiles.set(`${releasePrefix}/${assetName}`, Buffer.from('not the release archive'));
  t.after(() => {
    releaseFiles.delete(`${releasePrefix}/SHA256SUMS.txt`);
    releaseFiles.delete(`${releasePrefix}/${assetName}`);
  });
  fs.rmSync(path.join(fixture.stagingDir, classifier), { recursive: true, force: true });

  const result = await runScript(fixture);

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Integrity verification failed/);
  assert.doesNotMatch(result.stderr, /npm pack/);
});

test('fails when SHA256SUMS.txt does not list the exact release asset', async (t) => {
  const classifier = 'linux-x64';
  const fixture = createFixture(t, classifier);
  const releasePrefix = `/v${version}`;
  releaseFiles.set(`${releasePrefix}/SHA256SUMS.txt`, Buffer.from(`${checksum}  another-asset.tgz\n`));
  t.after(() => releaseFiles.delete(`${releasePrefix}/SHA256SUMS.txt`));
  fs.rmSync(path.join(fixture.stagingDir, classifier), { recursive: true, force: true });

  const result = await runScript(fixture);

  assert.notEqual(result.status, 0);
  assert.match(
    result.stderr,
    new RegExp(`SHA256SUMS\\.txt does not contain github-copilot-${version}-${classifier}\\.tgz`),
  );
});

function createFixture(t, classifier) {
  fs.mkdirSync(testRoot, { recursive: true });
  const root = fs.mkdtempSync(path.join(testRoot, 'fixture-'));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));

  const repoRoot = path.join(root, 'repo');
  const stagingDir = path.join(root, 'staging');
  const resourceDir = path.join(stagingDir, classifier, 'native', classifier);
  fs.mkdirSync(path.join(repoRoot, 'nodejs'), { recursive: true });
  fs.mkdirSync(resourceDir, { recursive: true });

  fs.writeFileSync(
    path.join(repoRoot, 'nodejs', 'package.json'),
    JSON.stringify({ copilotCliVersion: version }),
  );

  const runtimePath = path.join(resourceDir, 'runtime.node');
  const wrapperPath = path.join(
    resourceDir,
    classifier.startsWith('win32') ? 'copilot-runtime.exe' : 'copilot-runtime',
  );
  const platformPropertiesPath = path.join(resourceDir, 'platform.properties');
  const ripgrepPath = path.join(resourceDir, 'ripgrep', 'bin', classifier, 'rg');
  const inventoryPath = path.join(resourceDir, 'runtime-assets.list');
  fs.mkdirSync(path.dirname(ripgrepPath), { recursive: true });
  fs.writeFileSync(runtimePath, runtimeContent);
  fs.writeFileSync(wrapperPath, wrapperContent);
  fs.writeFileSync(ripgrepPath, 'ripgrep content');
  fs.writeFileSync(
    inventoryPath,
    `644\truntime.node\n755\tcopilot-runtime\n755\tripgrep/bin/${classifier}/rg\n`,
  );
  fs.writeFileSync(platformPropertiesPath, `classifier=${classifier}\nversion=${version}\n`);
  fs.writeFileSync(
    path.join(stagingDir, classifier, '.version'),
    `${stagingSchema}\n${version}\n${checksum}\n${digestTree(resourceDir)}\n`,
  );

  return {
    root,
    classifier,
    repoRoot,
    stagingDir,
    runtimePath,
    wrapperPath,
    ripgrepPath,
    platformPropertiesPath,
  };
}

function runScript(fixture, extraEnv = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [scriptPath, fixture.repoRoot, fixture.stagingDir, fixture.classifier], {
      env: {
        ...process.env,
        COPILOT_CLI_DOWNLOAD_BASE_URL: releaseBase,
        ...extraEnv,
      },
    });
    let stdout = '';
    let stderr = '';
    child.stdout.setEncoding('utf8');
    child.stderr.setEncoding('utf8');
    child.stdout.on('data', (chunk) => {
      stdout += chunk;
    });
    child.stderr.on('data', (chunk) => {
      stderr += chunk;
    });
    child.once('error', reject);
    child.once('close', (status, signal) => {
      resolve({ status, signal, stdout, stderr });
    });
  });
}

function assertRestagingAttempted(fixture, result) {
  assert.notEqual(result.status, 0, 'The unavailable release should make restaging fail');
}

function digestTree(directory) {
  const hash = createHash('sha512');
  for (const file of walkFiles(directory).sort()) {
    const relative = path.relative(directory, file).split(path.sep).join('/');
    hash.update(relative).update('\0').update(fs.readFileSync(file)).update('\0');
  }

  return `sha512-${hash.digest('base64')}`;
}

function digest(content) {
  return `sha512-${createHash('sha512').update(content).digest('base64')}`;
}

function walkFiles(directory) {
  const files = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const entryPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      files.push(...walkFiles(entryPath));
    } else {
      files.push(entryPath);
    }
  }
  return files;
}
