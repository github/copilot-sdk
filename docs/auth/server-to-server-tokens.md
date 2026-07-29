# Server-to-server authentication

Use a short-lived installation access token when a service needs to make Copilot requests on behalf of an organization without a user's credentials. In GitHub Actions, use the built-in `GITHUB_TOKEN` instead.

## GitHub Actions

For workflows in an organization-owned repository, grant the built-in token permission to make Copilot requests:

```yaml
permissions:
  contents: read
  copilot-requests: write

jobs:
  copilot:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - run: your-application
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

The organization's **Allow use of Copilot CLI billed to the organization** policy must be enabled. This approach needs no GitHub App or stored authentication secret. For details, see [Using Copilot CLI in GitHub Actions with GITHUB_TOKEN](https://docs.github.com/en/copilot/how-tos/copilot-cli/use-copilot-cli-in-actions).

## Other services and CI systems

For services outside GitHub Actions:

1. Create a GitHub App with the **Copilot Requests** repository permission set to **Read & write**.
1. Install it on the organization that should be billed. The current Copilot permission check requires **All repositories** access.
1. [Create an installation access token](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-an-installation-access-token-for-a-github-app) with a repository ID and the Copilot permission:

   ```json
   {
     "repository_ids": [123456789],
     "permissions": {
       "copilot_requests": "write"
     }
   }
   ```

1. Pass the resulting `ghs_` token to the runtime as `COPILOT_GITHUB_TOKEN`.

The organization must be enabled for Copilot requests from GitHub App installations. Installation tokens expire after one hour.

> [!WARNING]
> Do not pass an installation token through the SDK's `gitHubToken`, `github_token`, or equivalent option. That option is for user tokens. Installation tokens must use the runtime environment authentication path.

## Configure the runtime

Set the token in the environment before starting the application:

```shell
COPILOT_GITHUB_TOKEN=ghs_your_token your-application
```

All six SDKs inherit the host environment by default. When spawning a runtime with a per-client environment, use the following option:

| SDK | Child-process environment | Disable user fallback |
|---|---|---|
| TypeScript | `env` | `useLoggedInUser: false` |
| Python | `env` | `use_logged_in_user=False` |
| Go | `Env` | `UseLoggedInUser: copilot.Bool(false)` |
| Rust | `ClientOptions::with_env` | `.with_use_logged_in_user(false)` |
| .NET | `Environment` | `UseLoggedInUser = false` |
| Java | `setEnvironment` | `.setUseLoggedInUser(false)` |

Environment configuration depends on how the SDK reaches the runtime:

| Runtime connection | Where to set `COPILOT_GITHUB_TOKEN` |
|---|---|
| Child process | Host environment or the SDK's child-process environment option |
| In-process FFI | Host process environment before loading the runtime |
| Existing runtime URI | Environment of the existing runtime process |

Disable logged-in-user fallback when the host could also contain stored user credentials.

## Refresh tokens

Mint a new installation token before the current token expires. For a child process, restart the SDK client with the new environment. For an in-process or existing runtime, restart the host runtime with the new token.

## Troubleshooting

| Symptom | Check |
|---|---|
| `401 Unauthorized` | Confirm the organization supports GitHub App installation authentication for Copilot. |
| Error mentioning user information | Confirm the installation token is in `COPILOT_GITHUB_TOKEN`, not the SDK's explicit token option. |
| `403 Forbidden` | Confirm the app has **Copilot Requests: Read & write**, the installation covers all repositories, and the token request contains `repository_ids` and `copilot_requests: write`. |
| Wrong account billed | Confirm the installation belongs to the intended organization. |

## Further reading

* [Authenticate Copilot SDK](./authenticate.md): other authentication methods and priority
* [Generating an installation access token](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-an-installation-access-token-for-a-github-app): GitHub App token creation
