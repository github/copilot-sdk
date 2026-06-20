// @ts-check
const { ESLintUtils } = require("@typescript-eslint/utils");
const ts = require("typescript");

const DOCS_URL = "https://github.com/github/copilot-sdk/tree/main/nodejs/eslint-plugin";
const EXPERIMENTAL_TAG = "experimental";

const createRule = ESLintUtils.RuleCreator((name) => `${DOCS_URL}#${name}`);

function resolveSymbol(symbol, checker) {
    if (!symbol) {
        return undefined;
    }

    let resolvedSymbol = symbol;
    const seenSymbols = new Set();

    while ((resolvedSymbol.flags & ts.SymbolFlags.Alias) !== 0) {
        if (seenSymbols.has(resolvedSymbol)) {
            break;
        }

        seenSymbols.add(resolvedSymbol);

        try {
            resolvedSymbol = checker.getAliasedSymbol(resolvedSymbol);
        } catch {
            break;
        }
    }

    return resolvedSymbol;
}

function getExperimentalTag(symbol, checker) {
    const resolvedSymbol = resolveSymbol(symbol, checker);
    return resolvedSymbol
        ?.getJsDocTags(checker)
        .find((tag) => tag.name === EXPERIMENTAL_TAG);
}

function isDeclarationSite(symbol, tsNode) {
    return (symbol.declarations ?? []).some((declaration) => declaration.name === tsNode);
}

module.exports = createRule({
    name: "no-experimental-api",
    meta: {
        type: "problem",
        docs: {
            description:
                "Disallow referencing @experimental Copilot SDK APIs without an explicit opt-in.",
            recommended: "recommended",
            requiresTypeChecking: true,
        },
        messages: {
            experimental:
                "'{{name}}' is an experimental Copilot SDK API and may change or be removed without notice. Opt in explicitly with `// eslint-disable-next-line @github/copilot-sdk/no-experimental-api`.",
        },
        schema: [],
    },
    defaultOptions: [],
    create(context) {
        const services = ESLintUtils.getParserServices(context);
        const checker = services.program.getTypeChecker();

        function check(node) {
            const tsNode = services.esTreeNodeToTSNodeMap.get(node);
            if (!tsNode) {
                return;
            }

            const symbol = checker.getSymbolAtLocation(tsNode);
            if (!symbol || isDeclarationSite(symbol, tsNode)) {
                return;
            }

            if (!getExperimentalTag(symbol, checker)) {
                return;
            }

            context.report({
                node,
                messageId: "experimental",
                data: {
                    name: node.name,
                },
            });
        }

        return {
            Identifier: check,
            JSXIdentifier: check,
        };
    },
});
