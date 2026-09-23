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

test('normal nested generation selects checked-out runtime schemas with a clean environment', (t) => {
  const fixture = createFixture(t);
  const outputDir = path.join(fixture.root, 'runtime-output');
  const result = runFetch(fixture, outputDir, {
    COPILOT_CLI_DOWNLOAD_BASE_URL: 'http://127.0.0.1:1/should-not-be-called',
    COPILOT_CLI_RELEASE_TARBALL: undefined,
    COPILOT_CLI_RELEASE_SHA256: undefined,
    COPILOT_RUNTIME_SOURCE: undefined,
  });

  assert.equal(result.status, 0, result.stderr);
  assert.deepEqual(
    JSON.parse(fs.readFileSync(path.join(outputDir, 'api.schema.json'), 'utf8')),
    JSON.parse(
      fs.readFileSync(
        path.resolve(path.dirname(scriptPath), '../../../../..', 'generated/api.schema.json'),
        'utf8',
      ),
    ),
  );
});

test('copied standalone generation retains pinned published acquisition', (t) => {
  const fixture = createFixture(t);
  const standaloneRoot = path.join(fixture.root, 'standalone');
  const standaloneScript = path.join(standaloneRoot, 'java/scripts/codegen/fetch-schemas.mjs');
  const standaloneLayoutHelper = path.join(standaloneRoot, 'scripts/runtime-layout.mjs');
  const outputDir = path.join(standaloneRoot, 'java/scripts/codegen/target/schemas');
  fs.mkdirSync(path.dirname(standaloneScript), { recursive: true });
  fs.mkdirSync(path.dirname(standaloneLayoutHelper), { recursive: true });
  fs.mkdirSync(path.join(standaloneRoot, 'nodejs'), { recursive: true });
  fs.copyFileSync(scriptPath, standaloneScript);
  fs.copyFileSync(
    path.resolve(path.dirname(scriptPath), '../../../scripts/runtime-layout.mjs'),
    standaloneLayoutHelper,
  );
  fs.writeFileSync(
    path.join(standaloneRoot, 'nodejs/package.json'),
    JSON.stringify({ copilotCliVersion: '1.0.83' }),
  );

  const result = runFetch(
    fixture,
    outputDir,
    {
      COPILOT_RUNTIME_SOURCE: undefined,
    },
    standaloneScript,
  );

  assert.equal(result.status, 0, result.stderr);
  assert.deepEqual(
    JSON.parse(fs.readFileSync(path.join(outputDir, 'api.schema.json'), 'utf8')),
    { title: 'API' },
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

test('stages explicit runtime schemas without acquiring a published archive', (t) => {
  const fixture = createFixture(t);
  const runtimeSchemas = path.join(fixture.root, 'runtime-schemas');
  const outputDir = path.join(fixture.root, 'output');
  fs.mkdirSync(runtimeSchemas, { recursive: true });
  fs.mkdirSync(outputDir, { recursive: true });
  fs.writeFileSync(path.join(runtimeSchemas, 'api.schema.json'), '{"title":"Runtime API"}\n');
  fs.writeFileSync(path.join(runtimeSchemas, 'session-events.schema.json'), '{"title":"Runtime Events"}\n');
  fs.writeFileSync(path.join(outputDir, 'api.schema.json'), '{"title":"Stale Published API"}\n');

  const result = runFetch(fixture, outputDir, {
    COPILOT_CLI_DOWNLOAD_BASE_URL: 'http://127.0.0.1:1/should-not-be-called',
    COPILOT_CLI_RELEASE_TARBALL: undefined,
    COPILOT_CLI_RELEASE_SHA256: undefined,
    COPILOT_CLI_SCHEMA_DIR: runtimeSchemas,
    COPILOT_RUNTIME_SOURCE: 'checkout',
  });

  assert.equal(result.status, 0, result.stderr);
  assert.deepEqual(
    JSON.parse(fs.readFileSync(path.join(outputDir, 'api.schema.json'), 'utf8')),
    { title: 'Runtime API' },
  );
  assert.deepEqual(
    JSON.parse(fs.readFileSync(path.join(outputDir, 'session-events.schema.json'), 'utf8')),
    { title: 'Runtime Events' },
  );
});

test('fails closed when runtime schemas are missing or invalid', (t) => {
  const fixture = createFixture(t);
  const runtimeSchemas = path.join(fixture.root, 'runtime-schemas');
  fs.mkdirSync(runtimeSchemas, { recursive: true });
  fs.writeFileSync(path.join(runtimeSchemas, 'api.schema.json'), '{}\n');

  let result = runFetch(fixture, path.join(fixture.root, 'missing-output'), {
    COPILOT_CLI_RELEASE_TARBALL: undefined,
    COPILOT_CLI_RELEASE_SHA256: undefined,
    COPILOT_CLI_SCHEMA_DIR: runtimeSchemas,
    COPILOT_RUNTIME_SOURCE: 'checkout',
  });
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /session-events\.schema\.json/);

  fs.writeFileSync(path.join(runtimeSchemas, 'session-events.schema.json'), 'invalid json');
  result = runFetch(fixture, path.join(fixture.root, 'invalid-output'), {
    COPILOT_CLI_RELEASE_TARBALL: undefined,
    COPILOT_CLI_RELEASE_SHA256: undefined,
    COPILOT_CLI_SCHEMA_DIR: runtimeSchemas,
    COPILOT_RUNTIME_SOURCE: 'checkout',
  });
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Invalid runtime schema/);
});

test('rejects a selected missing schema directory in runtime mode', (t) => {
  const fixture = createFixture(t);
  const result = runFetch(fixture, path.join(fixture.root, 'output'), {
    COPILOT_CLI_RELEASE_TARBALL: undefined,
    COPILOT_CLI_RELEASE_SHA256: undefined,
    COPILOT_CLI_SCHEMA_DIR: path.join(fixture.root, 'missing-schemas'),
    COPILOT_RUNTIME_SOURCE: 'checkout',
  });

  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /Selected Copilot schema not found/);
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

function runFetch(fixture, outputDir, environment = {}, executable = scriptPath) {
  const env = {
    ...process.env,
    COPILOT_CLI_SCHEMA_DIR: undefined,
    COPILOT_CLI_RELEASE_TARBALL: fixture.archivePath,
    COPILOT_CLI_RELEASE_SHA256: fixture.hash,
    COPILOT_CLI_SCHEMA_OUTPUT: outputDir,
    COPILOT_RUNTIME_SOURCE: 'published',
    ...environment,
  };
  return spawnSync(process.execPath, [executable], {
    encoding: 'utf8',
    env,
  });
}
