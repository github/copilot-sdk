/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { execFileSync, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const version = '1.0.79';
const integrity = 'sha512-test-integrity';
const runtimeContent = 'runtime content';
const cliContent = 'cli content';
const wrapperContent = 'wrapper content';
const scriptPath = fileURLToPath(new URL('./fetch-native.mjs', import.meta.url));

for (const classifier of ['linux-x64', 'linux-arm64', 'win32-x64', 'win32-arm64', 'darwin-arm64']) {
  test(`${classifier}: missing CLI does not use incremental fast path`, (t) => {
    const fixture = createFixture(t, classifier);
    fs.rmSync(fixture.cliPath);

    const result = runScript(fixture);

    assertRestagingAttempted(fixture, result);
  });

  test(`${classifier}: stale CLI does not use incremental fast path`, (t) => {
    const fixture = createFixture(t, classifier);
    fs.writeFileSync(fixture.cliPath, 'stale CLI content');

    const result = runScript(fixture);

    assertRestagingAttempted(fixture, result);
  });

  test(`${classifier}: missing runtime wrapper does not use incremental fast path`, (t) => {
    const fixture = createFixture(t, classifier);
    fs.rmSync(fixture.wrapperPath);

    const result = runScript(fixture);

    assertRestagingAttempted(fixture, result);
  });

  test(`${classifier}: missing platform metadata does not use incremental fast path`, (t) => {
    const fixture = createFixture(t, classifier);
    fs.rmSync(fixture.platformPropertiesPath);

    const result = runScript(fixture);

    assertRestagingAttempted(fixture, result);
  });

  test(`${classifier}: missing retained runtime asset does not use incremental fast path`, (t) => {
    const fixture = createFixture(t, classifier);
    fs.rmSync(fixture.ripgrepPath);

    const result = runScript(fixture);

    assertRestagingAttempted(fixture, result);
  });

  test(`${classifier}: complete matching artifacts use incremental fast path`, (t) => {
    const fixture = createFixture(t, classifier);

    const result = runScript(fixture);

    assert.equal(result.status, 0, result.stderr);
    assert.match(result.stdout, /already staged/);
    assert.equal(fs.existsSync(fixture.npmMarkerPath), false);
  });
}

test('stages retained package assets and excludes CLI-only content', (t) => {
  const classifier = 'linux-x64';
  const fixture = createFixture(t, classifier);
  const packageRoot = path.join(fixture.repoRoot, 'package-root', 'package');
  fs.mkdirSync(path.join(packageRoot, 'prebuilds', classifier), { recursive: true });
  fs.mkdirSync(path.join(packageRoot, 'ripgrep', 'bin', classifier), { recursive: true });
  fs.mkdirSync(path.join(packageRoot, 'definitions'), { recursive: true });
  fs.writeFileSync(path.join(packageRoot, 'copilot'), cliContent);
  fs.writeFileSync(path.join(packageRoot, 'prebuilds', classifier, 'runtime.node'), runtimeContent);
  fs.writeFileSync(path.join(packageRoot, 'prebuilds', classifier, 'copilot-runtime'), wrapperContent);
  fs.writeFileSync(path.join(packageRoot, 'ripgrep', 'bin', classifier, 'rg'), 'ripgrep content');
  fs.chmodSync(path.join(packageRoot, 'ripgrep', 'bin', classifier, 'rg'), 0o755);
  fs.writeFileSync(path.join(packageRoot, 'definitions', 'future.json'), '{}');
  fs.writeFileSync(path.join(packageRoot, 'app.js'), 'excluded');
  const tarball = path.join(fixture.repoRoot, 'fixture.tgz');
  execFileSync('tar', ['-czf', tarball, '-C', path.dirname(packageRoot), 'package']);
  const packageIntegrity = digest(fs.readFileSync(tarball));
  fs.writeFileSync(
    path.join(fixture.repoRoot, 'nodejs', 'package-lock.json'),
    JSON.stringify({
      packages: {
        [`node_modules/@github/copilot-${classifier}`]: { version, integrity: packageIntegrity },
      },
    }),
  );
  fs.writeFileSync(
    path.join(fixture.fakeBinDir, 'npm'),
    '#!/bin/sh\ncp "$FETCH_NATIVE_TARBALL" "$4/fixture.tgz"\nprintf "fixture.tgz\\n"\n',
  );
  fs.chmodSync(path.join(fixture.fakeBinDir, 'npm'), 0o755);
  fs.rmSync(path.join(fixture.stagingDir, classifier), { recursive: true, force: true });

  const result = runScript(fixture, { FETCH_NATIVE_TARBALL: tarball });

  assert.equal(result.status, 0, result.stderr);
  const resourceDir = path.join(fixture.stagingDir, classifier, 'native', classifier);
  assert.equal(fs.readFileSync(path.join(resourceDir, 'ripgrep', 'bin', classifier, 'rg'), 'utf8'), 'ripgrep content');
  assert.equal(fs.readFileSync(path.join(resourceDir, 'definitions', 'future.json'), 'utf8'), '{}');
  assert.equal(fs.existsSync(path.join(resourceDir, 'app.js')), false);
  assert.match(fs.readFileSync(path.join(resourceDir, 'runtime-assets.list'), 'utf8'), /ripgrep\/bin\/linux-x64\/rg/);
});

function createFixture(t, classifier) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'fetch-native-test-'));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));

  const repoRoot = path.join(root, 'repo');
  const stagingDir = path.join(root, 'staging');
  const resourceDir = path.join(stagingDir, classifier, 'native', classifier);
  const fakeBinDir = path.join(root, 'bin');
  const npmMarkerPath = path.join(root, 'npm-invoked');
  fs.mkdirSync(path.join(repoRoot, 'nodejs'), { recursive: true });
  fs.mkdirSync(resourceDir, { recursive: true });
  fs.mkdirSync(fakeBinDir);

  fs.writeFileSync(
    path.join(repoRoot, 'nodejs', 'package-lock.json'),
    JSON.stringify({
      packages: {
        [`node_modules/@github/copilot-${classifier}`]: { version, integrity },
      },
    }),
  );

  const runtimePath = path.join(resourceDir, 'runtime.node');
  const cliPath = path.join(resourceDir, classifier.startsWith('win32') ? 'copilot.exe' : 'copilot');
  const wrapperPath = path.join(
    resourceDir,
    classifier.startsWith('win32') ? 'copilot-runtime.exe' : 'copilot-runtime',
  );
  const platformPropertiesPath = path.join(resourceDir, 'platform.properties');
  const ripgrepPath = path.join(resourceDir, 'ripgrep', 'bin', classifier, 'rg');
  const inventoryPath = path.join(resourceDir, 'runtime-assets.list');
  fs.mkdirSync(path.dirname(ripgrepPath), { recursive: true });
  fs.writeFileSync(runtimePath, runtimeContent);
  fs.writeFileSync(cliPath, cliContent);
  fs.writeFileSync(wrapperPath, wrapperContent);
  fs.writeFileSync(ripgrepPath, 'ripgrep content');
  fs.writeFileSync(
    inventoryPath,
    `644\truntime.node\n755\tcopilot\n755\tcopilot-runtime\n755\tripgrep/bin/${classifier}/rg\n`,
  );
  fs.writeFileSync(platformPropertiesPath, `classifier=${classifier}\nversion=${version}\n`);
  fs.writeFileSync(
    path.join(stagingDir, classifier, '.version'),
    `${version}\n${integrity}\n${digestTree(resourceDir)}\n`,
  );

  const fakeNpmPath = path.join(fakeBinDir, process.platform === 'win32' ? 'npm.cmd' : 'npm');
  if (process.platform === 'win32') {
    fs.writeFileSync(fakeNpmPath, '@echo off\r\n> "%FETCH_NATIVE_NPM_MARKER%" echo invoked\r\nexit /b 42\r\n');
  } else {
    fs.writeFileSync(fakeNpmPath, '#!/bin/sh\nprintf invoked > "$FETCH_NATIVE_NPM_MARKER"\nexit 42\n');
    fs.chmodSync(fakeNpmPath, 0o755);
  }

  return {
    classifier,
    repoRoot,
    stagingDir,
    fakeBinDir,
    npmMarkerPath,
    runtimePath,
    cliPath,
    wrapperPath,
    ripgrepPath,
    platformPropertiesPath,
  };
}

function runScript(fixture, extraEnv = {}) {
  return spawnSync(process.execPath, [scriptPath, fixture.repoRoot, fixture.stagingDir, fixture.classifier], {
    encoding: 'utf8',
    env: {
      ...process.env,
      PATH: `${fixture.fakeBinDir}${path.delimiter}${process.env.PATH}`,
      FETCH_NATIVE_NPM_MARKER: fixture.npmMarkerPath,
      ...extraEnv,
    },
  });
}

function assertRestagingAttempted(fixture, result) {
  assert.notEqual(result.status, 0, 'The fake npm command should make restaging fail');
  assert.equal(fs.readFileSync(fixture.npmMarkerPath, 'utf8').trim(), 'invoked');
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
