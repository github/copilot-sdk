# `@github/eslint-plugin-copilot-sdk`

Type-aware ESLint rules for the GitHub Copilot SDK.

## What it does

`@github/copilot-sdk/no-experimental-api` reports references to SDK symbols marked
with `/** @experimental */`. Consumers must explicitly suppress the diagnostic to
opt in at a call site.

## Rules

### `no-experimental-api`

Reports references to SDK symbols whose JSDoc is tagged `@experimental`. The rule is
type-aware: it resolves each referenced symbol back to its declaration and inspects the
declaration's JSDoc, so aliased imports and re-exports are flagged too. Suppress an
individual call site with an `eslint-disable-next-line` comment (see
[Suppressing a single use](#suppressing-a-single-use)).

## Install

```bash
npm install --save-dev @github/eslint-plugin-copilot-sdk @typescript-eslint/parser eslint typescript
```

## Flat config

```js
import tsParser from "@typescript-eslint/parser";
import copilotSdk from "@github/eslint-plugin-copilot-sdk";

export default [
    {
        files: ["src/**/*.ts"],
        languageOptions: {
            parser: tsParser,
            parserOptions: {
                project: "./tsconfig.json",
                tsconfigRootDir: import.meta.dirname,
            },
        },
        plugins: {
            "@github/copilot-sdk": copilotSdk,
        },
        rules: {
            ...copilotSdk.configs.recommended.rules,
        },
    },
];
```

This rule requires type-aware linting, so `parserOptions.project` must point to a
TypeScript project that includes the files being linted.

## Suppressing a single use

```ts
// eslint-disable-next-line @github/copilot-sdk/no-experimental-api
startCanvas();
```
