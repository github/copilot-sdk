# Authentication

Choose the authentication method that best fits your deployment scenario for the GitHub Copilot SDK.

* [Authenticate Copilot SDK](authenticate.md): methods, priority order, and examples
* [Bring your own key (BYOK)](./byok.md): use your own API keys from OpenAI, Azure, Anthropic, and more

## Authentication priority

When multiple credentials are configured, an explicit SDK token takes priority, followed by HMAC or direct Copilot API environment authentication, environment variable GitHub tokens, stored Copilot CLI credentials, and then GitHub CLI credentials. See [Authenticate Copilot SDK](authenticate.md#authentication-priority) for details.

For multi-user server mode, pass a per-session `gitHubToken` so each session runs with the correct GitHub identity; see [Multi-user and server deployments](../setup/multi-tenancy.md).
