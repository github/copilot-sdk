# Startup policy example

This source example uses an isolated configuration directory and a local runtime.
It makes no model request and installs no certificate.
It does not read or write your settings or keychain.

Prepare the same-checkout runtime and generated Node.js client with the
[SDK contributor guide](../../CONTRIBUTING.md#testing-an-unreleased-runtime-api).
From the SDK directory, run:

```sh
npx --no-install tsx samples/startup-policy/read-settings.mts /absolute/path/to/runtime.node
```

The output shows `user-model`, then `repo-model`, and a `changed` repository edit.
The obsolete value in `config.json` does not override `settings.json`.
Managed plugin controls override the user value.
The startup override disables native web search.
The permission query reports the managed bypass restriction.
The ingestion request disables policy discovery and network access.
The managed sandbox floor stays enabled.
`proxyCaRelevant` is `true`; installation support depends on the host platform.

The example imports internal host helpers directly from the source checkout.
Those helpers are not public SDK exports.
Applications can use `client.rpc.sandbox.proxyCa.checkRelevance` and
`client.rpc.sandbox.proxyCa.checkInstallationSupport` through the public client.
Neither query installs a certificate or requests elevation.
