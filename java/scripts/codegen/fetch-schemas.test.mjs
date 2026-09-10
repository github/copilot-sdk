/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { execFileSync, spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const scriptPath = path.join(path.dirname(fileURLToPath(import.meta.url)), 'fetch-schemas.mjs');

test('extracts schemas from a verified release archive', (t) => {
  const fixture = createFixture(t);
  const outputDir = path.join(fixture.root, 'output');
  const result = runFetch(fixture, outputDir);

  assert.equal(result.status, 0, result.stderr);
  assert.deepEqual(
    JSON.parse(fs.readFileSync(path.join(outputDir, 'api.schema.json'), 'utf8')),
    { title: 'API' },
  );
  assert.deepEqual(
    JSON.parse(fs.readFileSync(path.join(outputDir, 'session-events.schema.json'), 'utf8')),
    { title: 'Events' },
  );
});

test('rejects an archive with the wrong checksum', (t) => {
  const fixture = createFixture(t);
  const result = runFetch(
    { ...fixture, hash: '0'.repeat(64) },
    path.join(fixture.root, 'output'),
  );

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Integrity verification failed/);
});

test('requires both schema files', (t) => {
  const fixture = createFixture(t, { includeEvents: false });
  const result = runFetch(fixture, path.join(fixture.root, 'output'));

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /must contain exactly one package\/schemas\/session-events\.schema\.json/);
});

test('fetches schemas from a pinned runtime commit', (t) => {
  const fixture = createFixture(t);
  const outputDir = path.join(fixture.root, 'output');
  const result = spawnSync(process.execPath, [scriptPath], {
    encoding: 'utf8',
    env: {
      ...process.env,
      COPILOT_RUNTIME_CONTRACT_COMMIT: '0123456789abcdef',
      COPILOT_RUNTIME_API_SCHEMA_URL:
        'data:application/json,%7B%22title%22%3A%22Commit%20API%22%7D',
      COPILOT_RUNTIME_SESSION_EVENTS_SCHEMA_URL:
        'data:application/json,%7B%22title%22%3A%22Commit%20Events%22%7D',
      COPILOT_CLI_SCHEMA_OUTPUT: outputDir,
      GH_TOKEN: 'test-token',
    },
  });

  assert.equal(result.status, 0, result.stderr);
  assert.deepEqual(
    JSON.parse(fs.readFileSync(path.join(outputDir, 'api.schema.json'), 'utf8')),
    { title: 'Commit API' },
  );
  assert.deepEqual(
    JSON.parse(fs.readFileSync(path.join(outputDir, 'session-events.schema.json'), 'utf8')),
    { title: 'Commit Events' },
  );
});

function createFixture(t, { includeEvents = true } = {}) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'copilot-java-schemas-'));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  const packageDir = path.join(root, 'package');
  const schemasDir = path.join(packageDir, 'schemas');
  fs.mkdirSync(schemasDir, { recursive: true });
  fs.writeFileSync(path.join(schemasDir, 'api.schema.json'), '{"title":"API"}\n');
  if (includeEvents) {
    fs.writeFileSync(path.join(schemasDir, 'session-events.schema.json'), '{"title":"Events"}\n');
  }
  const archivePath = path.join(root, 'release.tgz');
  execFileSync('tar', ['-czf', archivePath, '-C', root, 'package']);
  const hash = createHash('sha256').update(fs.readFileSync(archivePath)).digest('hex');
  return { root, archivePath, hash };
}

function runFetch(fixture, outputDir) {
  return spawnSync(process.execPath, [scriptPath], {
    encoding: 'utf8',
    env: {
      ...process.env,
      COPILOT_CLI_RELEASE_TARBALL: fixture.archivePath,
      COPILOT_CLI_RELEASE_SHA256: fixture.hash,
      COPILOT_CLI_SCHEMA_OUTPUT: outputDir,
      COPILOT_RUNTIME_CONTRACT_COMMIT: '',
    },
  });
}
