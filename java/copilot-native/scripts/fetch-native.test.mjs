/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const version = '1.0.79';
const integrity = 'sha512-test-integrity';
const runtimeContent = 'runtime content';
const cliContent = 'cli content';
const scriptPath = fileURLToPath(new URL('./fetch-native.mjs', import.meta.url));

for (const classifier of ['linux-x64', 'win32-x64']) {
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

  test(`${classifier}: missing platform metadata does not use incremental fast path`, (t) => {
    const fixture = createFixture(t, classifier);
    fs.rmSync(fixture.platformPropertiesPath);

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
  const platformPropertiesPath = path.join(resourceDir, 'platform.properties');
  fs.writeFileSync(runtimePath, runtimeContent);
  fs.writeFileSync(cliPath, cliContent);
  fs.writeFileSync(platformPropertiesPath, `classifier=${classifier}\nversion=${version}\n`);
  fs.writeFileSync(
    path.join(stagingDir, classifier, '.version'),
    `${version}\n${integrity}\n${digest(runtimeContent)}\n${digest(cliContent)}\n`,
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
    platformPropertiesPath,
  };
}

function runScript(fixture) {
  return spawnSync(process.execPath, [scriptPath, fixture.repoRoot, fixture.stagingDir, fixture.classifier], {
    encoding: 'utf8',
    env: {
      ...process.env,
      PATH: `${fixture.fakeBinDir}${path.delimiter}${process.env.PATH}`,
      FETCH_NATIVE_NPM_MARKER: fixture.npmMarkerPath,
    },
  });
}

function assertRestagingAttempted(fixture, result) {
  assert.notEqual(result.status, 0, 'The fake npm command should make restaging fail');
  assert.equal(fs.readFileSync(fixture.npmMarkerPath, 'utf8').trim(), 'invoked');
}

function digest(content) {
  return `sha512-${createHash('sha512').update(content).digest('base64')}`;
}
