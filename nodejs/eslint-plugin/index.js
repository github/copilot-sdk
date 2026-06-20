const noExperimentalApi = require("./rules/no-experimental-api");

/** @type {import("eslint").ESLint.Plugin} */
const plugin = {
    meta: {
        name: "@github/eslint-plugin-copilot-sdk",
        version: "0.0.0",
    },
    rules: {
        "no-experimental-api": noExperimentalApi,
    },
    configs: {},
};

plugin.configs = {
    recommended: {
        name: "@github/copilot-sdk/recommended",
        plugins: {
            "@github/copilot-sdk": plugin,
        },
        rules: {
            "@github/copilot-sdk/no-experimental-api": "error",
        },
    },
    "recommended-legacy": {
        plugins: ["@github/copilot-sdk"],
        rules: {
            "@github/copilot-sdk/no-experimental-api": "error",
        },
    },
};

module.exports = plugin;
