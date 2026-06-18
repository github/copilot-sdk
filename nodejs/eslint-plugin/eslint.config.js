const parser = require("@typescript-eslint/parser");
const copilotSdk = require("./index.js");

module.exports = [
    {
        files: ["test/fixtures/consumer-*.ts"],
        languageOptions: {
            parser,
            parserOptions: {
                project: "./test/fixtures/tsconfig.json",
                tsconfigRootDir: __dirname,
            },
        },
        plugins: {
            "@github/copilot-sdk": copilotSdk,
        },
        rules: {
            "@github/copilot-sdk/no-experimental-api": "error",
        },
    },
];
