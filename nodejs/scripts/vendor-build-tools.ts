import { createHash } from "node:crypto";
import { once } from "node:events";
import {
    chmod,
    cp,
    mkdir,
    mkdtemp,
    open,
    readFile,
    readdir,
    realpath,
    rename,
    rm,
    rmdir,
    writeFile,
} from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import koffi from "koffi";
import { Parser } from "tar";
import ts from "typescript";

const root = fileURLToPath(new URL("../../../../", import.meta.url));
const outputDirectory = path.join(root, "third_party", "build-tools");
const sourceArtifacts = {
    "oxfmt@0.35.0":
        "sha512-QYeXWkP+aLt7utt5SLivNIk09glWx9QE235ODjgcEZ3sd1VMaUBSpLymh6ZRCA76gD2rMP4bXanUz/fx+nLM9Q==",
    "prettier@3.8.1":
        "sha512-UOnG6LftzbdaHZcKoPFtOcCKztrQ57WkHDeRD9t/PTQtmT0NHSeWWepj6pS0z/N7+08BHFDQVUrfmfMRcZwbMg==",
    "tsx@4.21.0":
        "sha512-5C1sg4USs1lfG0GFb2RLXsdpXqBSEhAaA/0kPL01wxzpMqLILNxIxIOKiILz+cdg/pLnOUxFYOR5yhHU666wbw==",
    "tsx@4.22.4":
        "sha512-X8EX+XV4QR5xCsrgxaED954zTDfY8KqlDtskKEL0cHhyS/P8b4IFOvGDQpsC9Q1XnLq915wEfwwY/zzskCtmhg==",
    "tsx@4.23.1":
        "sha512-GQHnkIfxyx1wYCOS/wonik5MVRZU9hi1TEZmzGZSCJB1y9YgoZ8H6itNE/u4suE+yLmOzuE4E5S4TZ/ZX2wcWQ==",
    "tsx@4.23.12":
        "sha512-FDf4L4sYzKtzWYhU/Xm0AQFdTjdIxNo9ElTf2mxXM6k8YMHXzYUe4yODVaXP4V9uMFbVg8c0qyBccK2OOxb45Q==",
    "tsx@4.23.13":
        "sha512-BL5MGkRln6aDYhb0xbQlEAGw743BaZYWdbWtdJOBriYJboKgUUYCadFp2/FpBBZquBC/ezNBn7wMMPx7FDZUDw==",
    "vite@8.3.4":
        "sha512-IftLE8phJhdNRIGNEV4JCCvfNScoZQVfcXaN8TQropWGJtOHcu+iAB9pHcuGvmEb/kddw0LBe/jS8ztdC6xdiQ==",
};
const retiredDistributions = new Set(["vite@8.2.2", "vite@8.3.3"]);
const originalImplementation =
    /node_modules\/braces\/|braces\/lib\/(?:compile|parse|expand)|expanded array length exceeds range limit|isInvalidBrace/;
const expectedModules = [
    "lib/utils.js",
    "lib/stringify.js",
    "lib/compile.js",
    "lib/expand.js",
    "lib/constants.js",
    "lib/parse.js",
    "index.js",
];

type Edit = { start: number; end: number; text: string };
type Module = { name: string; text: string; edit: Edit };
type Entry = { name: string; mode: number; bytes: Buffer };

function findNode<T extends ts.Node>(
    node: ts.Node,
    predicate: (candidate: ts.Node) => candidate is T
): T | undefined {
    if (predicate(node)) {
        return node;
    }
    return ts.forEachChild(node, (child) => findNode(child, predicate));
}

function numericValue(node: ts.Expression): number {
    if (ts.isNumericLiteral(node)) {
        return Number(node.text);
    }
    if (ts.isBinaryExpression(node) && node.operatorToken.kind === ts.SyntaxKind.AsteriskToken) {
        return numericValue(node.left) * numericValue(node.right);
    }
    throw new Error(`Unsupported maximum-length expression: ${node.getText()}`);
}

function maximumLength(source: string): number {
    const ast = ts.createSourceFile(
        "constants.js",
        source,
        ts.ScriptTarget.Latest,
        true,
        ts.ScriptKind.JS
    );
    const property = findNode(
        ast,
        (node): node is ts.PropertyAssignment =>
            ts.isPropertyAssignment(node) && node.name.getText(ast) === "MAX_LENGTH"
    );
    if (!property) {
        throw new Error("The original parser's maximum-length constant is missing");
    }
    return numericValue(property.initializer);
}

function applyEdits(source: string, edits: Edit[]): string {
    let previous = source.length;
    for (const edit of edits.sort((left, right) => right.start - left.start)) {
        if (edit.end > previous || edit.start > edit.end) {
            throw new Error("Overlapping embedded-module replacements");
        }
        source = source.slice(0, edit.start) + edit.text + source.slice(edit.end);
        previous = edit.start;
    }
    return source;
}

function replaceOnce(source: string, original: string, replacement: string): string {
    const index = source.indexOf(original);
    if (index < 0 || source.indexOf(original, index + original.length) >= 0) {
        throw new Error(`Unexpected reviewed security-fix target: ${original}`);
    }
    return source.slice(0, index) + replacement + source.slice(index + original.length);
}

function replaceJsoncParser(source: string, implementation: string): string {
    const ast = ts.createSourceFile(
        "migrate-biome.js",
        source,
        ts.ScriptTarget.Latest,
        true,
        ts.ScriptKind.JS
    );
    const edits: Edit[] = [];
    for (const statement of ast.statements) {
        if (ts.isFunctionDeclaration(statement) && statement.name?.text === "parseJSONC") {
            edits.push({
                start: statement.getStart(ast),
                end: statement.end,
                text: implementation,
            });
        } else if (
            ts.isVariableStatement(statement) &&
            statement.declarationList.declarations.length === 1 &&
            ["stringOrCommentRe", "stringOrTrailingCommaRe"].includes(
                statement.declarationList.declarations[0].name.getText(ast)
            )
        ) {
            edits.push({ start: statement.getStart(ast), end: statement.end, text: "" });
        }
    }
    if (edits.length !== 3) {
        throw new Error("Unexpected reviewed JSONC parser layout");
    }
    return applyEdits(source, edits);
}

function reviewedSecurityFixes(
    id: string,
    relative: string,
    source: string,
    jsoncImplementation: string
): { source: string; rules: string[] } {
    const rules: string[] = [];
    if (id.startsWith("vite@") && relative === "dist/node/module-runner.js") {
        source = replaceOnce(
            source,
            'return url.replace(postfixRE, "");',
            "const index = url.search(/[?#]/);\n\treturn index < 0 ? url : url.slice(0, index);"
        );
        source = replaceOnce(source, "const postfixRE = /[?#].*$/;\n", "");
        source = replaceOnce(
            source,
            "functionName.indexOf(`.${methodName}`) !== functionName.length - methodName.length - 1",
            "!functionName.endsWith(`.${methodName}`)"
        );
        rules.push("js/polynomial-redos", "js/incorrect-suffix-check");
    }
    if (id.startsWith("vite@") && relative === "dist/node/chunks/node.js") {
        const guard =
            'if (key === "__proto__" || key === "constructor" || key === "prototype") continue;';
        source = replaceOnce(
            source,
            "for (const key in value) cloned[key] = deepClone(value[key]);",
            `for (const key in value) {\n\t\t\t${guard}\n\t\t\tcloned[key] = deepClone(value[key]);\n\t\t}`
        );
        source = replaceOnce(
            source,
            "for (const key in values) {\n\t\tconst value = values[key];",
            `for (const key in values) {\n\t\t${guard}\n\t\tconst value = values[key];`
        );
        rules.push("js/prototype-pollution-utility");
    }
    if (id === "oxfmt@0.35.0" && relative === "dist/migrate-biome-B4fq26fI.js") {
        source = replaceJsoncParser(source, jsoncImplementation);
        rules.push("js/redos");
    }
    if (
        (id === "prettier@3.8.1" && relative === "index.mjs") ||
        (id === "oxfmt@0.35.0" && relative === "dist/prettier-DIVejRqd.js")
    ) {
        // Use the disjoint, optional-closing-class form already bundled by Vite.
        source = replaceOnce(
            source,
            String.raw`\[(?:[^\]\\\n\r\u2028\u2029]+|\\.)*\]|[^\/\\\n\r\u2028\u2029]+`,
            String.raw`\[(?:[^\]\\\n\r\u2028\u2029]+|\\.)*\]?|[^\/[\\\n\r\u2028\u2029]+`
        );
        rules.push("js/redos");
    }
    if (id === "oxfmt@0.35.0" && relative === "dist/v3-D-mr2VVh-DAVD4LOh.js") {
        source = replaceOnce(
            source,
            String.raw`^[+-]?[0-9]*\.?[0-9]+`,
            String.raw`^[+-]?[0-9]*\\.?[0-9]+`
        );
        rules.push("js/useless-regexp-character-escape");
    }
    return { source, rules };
}

function wrappedModules(source: string): Map<string, Module> {
    const modules = new Map<string, Module>();
    const regions = [
        ...source.matchAll(
            /\/\/#region [^\n]*\/braces\/((?:lib\/)?\w+\.js)\n[\s\S]*?\/\/#endregion/g
        ),
    ];
    if (regions.length > 0) {
        for (const region of regions) {
            modules.set(region[1], {
                name: region[1],
                text: region[0],
                edit: { start: region.index, end: region.index + region[0].length, text: "" },
            });
        }
        return modules;
    }
    const ast = ts.createSourceFile(
        "bundle.js",
        source,
        ts.ScriptTarget.Latest,
        true,
        ts.ScriptKind.JS
    );
    for (const statement of ast.statements) {
        if (!ts.isVariableStatement(statement)) {
            continue;
        }
        const match = /["']node_modules\/braces\/((?:lib\/)?\w+\.js)["']/.exec(
            statement.getText(ast)
        );
        if (match) {
            modules.set(match[1], {
                name: match[1],
                text: statement.getText(ast),
                edit: { start: statement.getFullStart(), end: statement.end, text: "" },
            });
        }
    }
    return modules;
}

function replaceWrappedModules(
    source: string,
    modules: Map<string, Module>,
    factory: string
): string {
    if (
        modules.size !== expectedModules.length ||
        expectedModules.some((name) => !modules.has(name))
    ) {
        throw new Error(`Unexpected embedded module inventory: ${[...modules.keys()].join(", ")}`);
    }
    const entry = modules.get("index.js")!;
    const compile = ts.createSourceFile(
        "compile.js",
        modules.get("lib/compile.js")!.text,
        ts.ScriptTarget.Latest,
        true,
        ts.ScriptKind.JS
    );
    const range = findNode(
        compile,
        (node): node is ts.VariableDeclaration =>
            ts.isVariableDeclaration(node) &&
            node.name.getText(compile) === "fill" &&
            node.initializer !== undefined &&
            ts.isCallExpression(node.initializer)
    );
    if (!range?.initializer) {
        throw new Error("Cannot locate the existing bundled range implementation");
    }
    const entryAst = ts.createSourceFile(
        "entry.js",
        entry.text,
        ts.ScriptTarget.Latest,
        true,
        ts.ScriptKind.JS
    );
    const declaration = entryAst.statements.find(ts.isVariableStatement)?.declarationList
        .declarations[0];
    if (!declaration || !ts.isIdentifier(declaration.name)) {
        throw new Error("Cannot locate the embedded entry point");
    }
    const length = maximumLength(modules.get("lib/constants.js")!.text);
    entry.edit.text = `\nvar ${declaration.name.text} = (() => { let implementation; return () => implementation ??= (${factory})(${range.initializer.getText(compile)}, ${length}); })();\n`;
    return applyEdits(
        source,
        [...modules.values()].map((module) => module.edit)
    );
}

function replaceMinifiedModules(source: string, factory: string): string {
    const ast = ts.createSourceFile(
        "cli.js",
        source,
        ts.ScriptTarget.Latest,
        true,
        ts.ScriptKind.JS
    );
    const declarations = ast.statements
        .filter(ts.isVariableStatement)
        .flatMap((statement) =>
            [...statement.declarationList.declarations].map((node) => ({ node, statement }))
        );
    const core = declarations.find(({ node }) =>
        /["']braces\$\d+["']/.test(node.initializer?.getText(ast) ?? "")
    );
    const compiler = declarations.find(({ node }) => {
        const text = node.initializer?.getText(ast) ?? "";
        return text.includes(".isInvalidBrace(") && text.includes("toRegex:");
    });
    const stringify = declarations.find(({ node }) => {
        const text = node.initializer?.getText(ast) ?? "";
        return text.includes(".isInvalidBrace(") && !text.includes("toRegex:");
    });
    const utilities = ast.statements.find(
        (statement) =>
            ts.isExpressionStatement(statement) &&
            statement.getText(ast).includes(".isInvalidBrace=")
    );
    if (!core || !compiler || !stringify || !utilities || !ts.isIdentifier(core.node.name)) {
        throw new Error("Unrecognized minified embedded-module layout");
    }
    const methods = ast.statements.find(
        (statement) =>
            ts.isExpressionStatement(statement) &&
            statement.getText(ast).startsWith(`${core.node.name.getText(ast)}.parse=`)
    );
    if (!methods) {
        throw new Error("Cannot locate the original entry-point methods");
    }
    const compileCall = findNode(
        compiler.node,
        (node): node is ts.CallExpression =>
            ts.isCallExpression(node) &&
            node.arguments.some(
                (argument) =>
                    ts.isObjectLiteralExpression(argument) &&
                    argument.properties.some(
                        (property) =>
                            ts.isPropertyAssignment(property) &&
                            property.name.getText(ast) === "toRegex"
                    )
            )
    );
    const rangeAlias = declarations.find(
        ({ node }) => node.name.getText(ast) === compileCall?.expression.getText(ast)
    );
    if (!rangeAlias?.node.initializer || !stringify.node.initializer) {
        throw new Error("Cannot locate the retained minified range implementation");
    }
    const utilityProperty = findNode(
        stringify.node,
        (node): node is ts.PropertyAccessExpression =>
            ts.isPropertyAccessExpression(node) && node.name.text === "isInvalidBrace"
    );
    const utilityAlias = declarations.find(
        ({ node }) => node.name.getText(ast) === utilityProperty?.expression.getText(ast)
    );
    if (!utilityAlias?.node.initializer || !ts.isIdentifier(utilityAlias.node.initializer)) {
        throw new Error("Cannot locate the original utility namespace");
    }
    const removed = new Set([stringify.node, utilityAlias.node]);
    const namespace = declarations.find(
        ({ node }) => node.name.getText(ast) === utilityAlias.node.initializer!.getText(ast)
    );
    if (!namespace) {
        throw new Error("Cannot locate the original utility initializer");
    }
    removed.add(namespace.node);
    const edits: Edit[] = [{ start: utilities.getStart(ast), end: utilities.end, text: "" }];
    for (const statement of new Set(
        [...removed].map(
            (node) => declarations.find((declaration) => declaration.node === node)!.statement
        )
    )) {
        const retained = statement.declarationList.declarations.filter(
            (node) => !removed.has(node)
        );
        const keyword =
            statement.declarationList.flags & ts.NodeFlags.Const
                ? "const"
                : statement.declarationList.flags & ts.NodeFlags.Let
                  ? "let"
                  : "var";
        edits.push({
            start: statement.getStart(ast),
            end: statement.end,
            text:
                retained.length === 0
                    ? ""
                    : `${keyword} ${retained.map((node) => node.getText(ast)).join(",")};`,
        });
    }
    edits.push({
        start: compiler.statement.getStart(ast),
        end: methods.end,
        text: `var ${core.node.name.text} = (${factory})(${rangeAlias.node.initializer.getText(ast)}, ${maximumLength(source.slice(compiler.statement.pos, methods.end))});`,
    });
    return applyEdits(source, edits);
}

export function replaceBundledBracePatterns(source: string, factory: string): string {
    const modules = wrappedModules(source);
    const result =
        modules.size > 0
            ? replaceWrappedModules(source, modules, factory)
            : replaceMinifiedModules(source, factory);
    if (originalImplementation.test(result)) {
        throw new Error("The original embedded implementation remains after replacement");
    }
    const diagnostics = ts.transpileModule(result, {
        compilerOptions: { target: ts.ScriptTarget.ESNext, module: ts.ModuleKind.ESNext },
        reportDiagnostics: true,
    }).diagnostics;
    if (diagnostics?.some((diagnostic) => diagnostic.category === ts.DiagnosticCategory.Error)) {
        throw new Error(
            ts.formatDiagnosticsWithColorAndContext(diagnostics, {
                getCanonicalFileName: (name) => name,
                getCurrentDirectory: () => root,
                getNewLine: () => "\n",
            })
        );
    }
    return result;
}

async function readEntries(bytes: Buffer): Promise<Entry[]> {
    const entries: Entry[] = [];
    const parser = new Parser({
        onReadEntry(entry) {
            const chunks: Buffer[] = [];
            entry.on("data", (chunk: Buffer) => chunks.push(chunk));
            entry.on("end", () => {
                if (entry.type === "File") {
                    entries.push({
                        name: entry.path,
                        mode: entry.mode ?? 0o644,
                        bytes: Buffer.concat(chunks),
                    });
                } else if (entry.type !== "Directory") {
                    parser.emit(
                        "error",
                        new Error(`Unsupported archive entry: ${entry.type} ${entry.path}`)
                    );
                }
            });
        },
    });
    const complete = once(parser, "end");
    parser.end(bytes);
    await complete;
    for (const entry of entries) {
        if (
            !entry.name.startsWith("package/") ||
            entry.name.split("/").some((part) => part === ".." || part === "") ||
            entry.name.includes("\\")
        ) {
            throw new Error(`Unsafe archive path: ${entry.name}`);
        }
    }
    return entries;
}

async function vendor(
    id: string,
    integrity: string,
    factory: string,
    jsoncImplementation: string,
    destination: string
) {
    const [name, version] = id.split("@");
    const url = `https://registry.npmjs.org/${name}/-/${name}-${version}.tgz`;
    const response = await fetch(url);
    if (!response.ok) {
        throw new Error(`${response.status}: ${url}`);
    }
    const original = Buffer.from(await response.arrayBuffer());
    if (`sha512-${createHash("sha512").update(original).digest("base64")}` !== integrity) {
        throw new Error(`Upstream archive integrity mismatch: ${id}`);
    }
    const entries = await readEntries(original);
    const manifest = JSON.parse(
        entries.find((entry) => entry.name === "package/package.json")!.bytes.toString("utf8")
    );
    if (manifest.name !== name || manifest.version !== version) {
        throw new Error(`Upstream identity mismatch: ${id}`);
    }
    const changed: string[] = [];
    const braceReplacements: string[] = [];
    const securityFixes: { file: string; rules: string[] }[] = [];
    if (name === "oxfmt") {
        if (manifest.dependencies?.tinypool !== "2.1.0") {
            throw new Error("The original formatter's worker dependency changed");
        }
        // Retain the repository's existing worker override in every installation graph.
        const entry = entries.find((entry) => entry.name === "package/package.json")!;
        entry.bytes = Buffer.from(
            entry.bytes.toString("utf8").replace('"tinypool": "2.1.0"', '"tinypool": "2.2.0"')
        );
        changed.push(entry.name);
    }
    for (const entry of entries) {
        if (
            /\.(?:[cm]?js|map)$/.test(entry.name) &&
            originalImplementation.test(entry.bytes.toString("utf8"))
        ) {
            if (entry.name.endsWith(".map")) {
                throw new Error(`A bundled source map needs explicit regeneration: ${entry.name}`);
            }
            entry.bytes = Buffer.from(
                replaceBundledBracePatterns(entry.bytes.toString("utf8"), factory)
            );
            changed.push(entry.name);
            braceReplacements.push(entry.name);
        }
        if (/\.[cm]?js$/.test(entry.name)) {
            const fixed = reviewedSecurityFixes(
                id,
                entry.name.slice("package/".length),
                entry.bytes.toString("utf8"),
                jsoncImplementation
            );
            if (fixed.rules.length > 0) {
                entry.bytes = Buffer.from(fixed.source);
                if (!changed.includes(entry.name)) {
                    changed.push(entry.name);
                }
                securityFixes.push({ file: entry.name, rules: fixed.rules });
            }
        }
    }
    if (braceReplacements.length === 0) {
        throw new Error(`No embedded implementation found: ${id}`);
    }
    const expectedSecurityFiles =
        name === "vite" ? 2 : name === "oxfmt" ? 3 : name === "prettier" ? 1 : 0;
    if (securityFixes.length !== expectedSecurityFiles) {
        throw new Error(`Unexpected reviewed security-fix inventory: ${id}`);
    }
    const directory = `${name}-${version}`;
    const files: Record<string, { mode: number; integrity: string }> = {};
    for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name, "en"))) {
        const relative = entry.name.slice("package/".length);
        const file = path.join(destination, directory, ...relative.split("/"));
        await mkdir(path.dirname(file), { recursive: true });
        await writeFile(file, entry.bytes);
        await chmod(file, entry.mode);
        files[relative] = {
            mode: entry.mode,
            integrity: `sha512-${createHash("sha512").update(entry.bytes).digest("base64")}`,
        };
    }
    return {
        directory,
        source: url,
        sourceIntegrity: integrity,
        changed,
        braceReplacements,
        securityFixes,
        files,
    };
}

export async function writeDistributionProvenance(
    directory: string,
    provenance: Record<string, { directory: string; files: Record<string, unknown> }>
) {
    const file = path.join(directory, "provenance.json");
    let previous: unknown = {};
    try {
        previous = JSON.parse(await readFile(file, "utf8"));
    } catch (error) {
        if (!(error instanceof Error && "code" in error && error.code === "ENOENT")) {
            throw error;
        }
    }
    if (!previous || typeof previous !== "object" || Array.isArray(previous)) {
        throw new Error("Invalid previous tool-distribution provenance");
    }
    const obsolete: string[] = [];
    const obsoleteDirectories: string[] = [];
    const entries: [string, unknown][] = Object.entries(previous);
    for (const [id, entry] of entries) {
        const [name, version] = id.split("@");
        if (
            (!Object.hasOwn(sourceArtifacts, id) && !retiredDistributions.has(id)) ||
            !entry ||
            typeof entry !== "object" ||
            !("directory" in entry) ||
            entry.directory !== `${name}-${version}` ||
            !("files" in entry) ||
            !entry.files ||
            typeof entry.files !== "object" ||
            Array.isArray(entry.files)
        ) {
            throw new Error(`Unsafe previous tool distribution: ${id}`);
        }
        for (const relative of Object.keys(entry.files)) {
            if (
                relative.includes("\\") ||
                relative.split("/").some((part) => part === "" || part === "." || part === "..") ||
                path.isAbsolute(relative)
            ) {
                throw new Error(`Unsafe previous tool distribution file: ${id}/${relative}`);
            }
            if (!Object.hasOwn(provenance[id]?.files ?? {}, relative)) {
                obsolete.push(path.join(entry.directory, relative));
            }
        }
        if (retiredDistributions.has(id)) {
            if (Object.hasOwn(provenance, id)) {
                throw new Error(`Cannot regenerate retired tool distribution: ${id}`);
            }
            const retiredDirectory = entry.directory;
            const inventory = await toolInventory(path.join(directory, retiredDirectory));
            for (const file of inventory.files) {
                if (!Object.hasOwn(entry.files, file.split(path.sep).join("/"))) {
                    throw new Error(`Unrecorded file in retired tool distribution: ${id}/${file}`);
                }
            }
            obsoleteDirectories.push(
                retiredDirectory,
                ...inventory.directories.map((name) => path.join(retiredDirectory, name))
            );
        }
    }
    await writeFile(file, `${JSON.stringify(provenance, null, 2)}\n`);
    await Promise.all(obsolete.map((name) => rm(path.join(directory, name), { force: true })));
    await removeToolDirectories(directory, obsoleteDirectories);
}

async function acquireToolPublicationLock(directory: string): Promise<() => Promise<void>> {
    const physical = await realpath(directory);
    // Never unlink this file: POSIX contenders must open the same inode.
    const file = await open(
        path.join(path.dirname(physical), `.${path.basename(physical)}.lock`),
        "a",
        0o600
    );
    let closeMutex: (() => void) | undefined;
    const release = async () => {
        const results = await Promise.allSettled([
            file.close(),
            Promise.resolve().then(() => closeMutex?.()),
        ]);
        const errors = results.flatMap((result) =>
            result.status === "rejected" ? [result.reason] : []
        );
        if (errors.length > 0) {
            throw new AggregateError(errors, "Cannot release tool-publication lock");
        }
    };
    try {
        if (process.platform === "win32") {
            const ERROR_ALREADY_EXISTS = 183;
            const library = koffi.load("kernel32.dll");
            const createMutex = library.func(
                "void *__stdcall CreateMutexW(void *attributes, int initialOwner, str16 name)"
            );
            const lastError = library.func("uint32_t __stdcall GetLastError()");
            const closeHandle = library.func("int __stdcall CloseHandle(void *handle)");
            const key = createHash("sha256").update(physical.toLowerCase()).digest("hex");
            const handle: unknown = createMutex(null, 0, `Global\\CopilotVendorTools-${key}`);
            const error: number = lastError();
            if (handle === null) {
                throw new Error(`Cannot create tool-publication mutex: Windows error ${error}`);
            }
            closeMutex = () => {
                if (closeHandle(handle) === 0) {
                    throw new Error(
                        `Cannot close tool-publication mutex: Windows error ${lastError()}`
                    );
                }
            };
            if (error === ERROR_ALREADY_EXISTS) {
                throw new Error("Tool publication is already running");
            }
        } else {
            const LOCK_EX = 2;
            const LOCK_NB = 4;
            const flock = koffi.load(null).func("int flock(int fd, int operation)");
            if (flock(file.fd, LOCK_EX | LOCK_NB) !== 0) {
                const error = koffi.errno();
                if (error === koffi.os.errno.EWOULDBLOCK || error === koffi.os.errno.EAGAIN) {
                    throw new Error("Tool publication is already running");
                }
                throw new Error(`Cannot acquire tool-publication flock: errno ${error}`);
            }
        }
        return release;
    } catch (error) {
        try {
            await release();
        } catch (cleanupError) {
            throw new AggregateError(
                [error, cleanupError],
                "Cannot acquire or clean up tool-publication lock"
            );
        }
        throw error;
    }
}

type ToolPublication = {
    directory: string;
    files: string[];
    originalFiles: string[];
    addedDirectories: string[];
    removedDirectories?: string[];
};

function isToolPublication(value: unknown): value is ToolPublication {
    const safeFiles = (files: unknown): files is string[] =>
        Array.isArray(files) &&
        files.every(
            (file: unknown) =>
                typeof file === "string" &&
                !path.isAbsolute(file) &&
                !file.split(/[\\/]/).some((part) => part === "" || part === "." || part === "..")
        );
    if (
        !(
            value !== null &&
            typeof value === "object" &&
            "directory" in value &&
            typeof value.directory === "string" &&
            "files" in value &&
            safeFiles(value.files) &&
            "originalFiles" in value &&
            safeFiles(value.originalFiles) &&
            "addedDirectories" in value &&
            safeFiles(value.addedDirectories) &&
            (!("removedDirectories" in value) || safeFiles(value.removedDirectories))
        )
    ) {
        return false;
    }
    const files = value.files;
    return value.originalFiles.every((file) => files.includes(file));
}

async function toolInventory(
    directory: string
): Promise<{ files: string[]; directories: string[] }> {
    const entries = await readdir(directory, { recursive: true, withFileTypes: true });
    if (entries.some((entry) => !entry.isDirectory() && !entry.isFile())) {
        throw new Error(
            `Tool distributions must contain only regular files and directories: ${directory}`
        );
    }
    const relative = (entry: (typeof entries)[number]) =>
        path.relative(directory, path.join(entry.parentPath, entry.name));
    return {
        files: entries.filter((entry) => entry.isFile()).map(relative),
        directories: entries.filter((entry) => entry.isDirectory()).map(relative),
    };
}

async function publishToolFiles(
    source: string,
    publication: ToolPublication,
    presentFiles: string[],
    transaction: string,
    removedDirectories: string[] = []
): Promise<void> {
    const present = new Set(presentFiles);
    const publish = async (file: string) => {
        const destination = path.join(publication.directory, file);
        if (present.has(file)) {
            const pending = path.join(transaction, "current");
            await rm(pending, { force: true });
            await cp(path.join(source, file), pending);
            await mkdir(path.dirname(destination), { recursive: true });
            await rename(pending, destination);
        } else {
            await rm(destination, { force: true });
        }
    };
    // Keep the canonical tree in place; commit provenance after files and retired directories.
    for (const file of publication.files.filter((file) => file !== "provenance.json").sort()) {
        await publish(file);
    }
    await removeToolDirectories(publication.directory, removedDirectories);
    if (publication.files.includes("provenance.json")) {
        await publish("provenance.json");
    }
}

async function removeToolDirectories(root: string, directories: string[]): Promise<void> {
    for (const directory of [...directories].sort((left, right) => right.length - left.length)) {
        try {
            await rmdir(path.join(root, directory));
        } catch (error) {
            if (!(error instanceof Error && "code" in error && error.code === "ENOENT")) {
                throw error;
            }
        }
    }
}

async function restoreToolPublication(
    transaction: string,
    publication: ToolPublication
): Promise<void> {
    for (const directory of publication.removedDirectories ?? []) {
        await mkdir(path.join(publication.directory, directory), { recursive: true });
    }
    await publishToolFiles(
        path.join(transaction, "previous"),
        publication,
        publication.originalFiles,
        transaction,
        publication.addedDirectories
    );
}

async function recoverToolPublications(directory: string, prefix: string): Promise<void> {
    for (const entry of await readdir(path.dirname(directory), { withFileTypes: true })) {
        if (!entry.isDirectory() || !entry.name.startsWith(prefix)) {
            continue;
        }
        const transaction = path.join(path.dirname(directory), entry.name);
        let publication: unknown;
        try {
            publication = JSON.parse(
                await readFile(path.join(transaction, "journal.json"), "utf8")
            );
        } catch (error) {
            if (error instanceof Error && "code" in error && error.code === "ENOENT") {
                continue;
            }
            throw error;
        }
        if (!isToolPublication(publication)) {
            throw new Error(`Invalid tool-publication journal: ${transaction}`);
        }
        if (publication.directory !== directory) {
            continue;
        }
        let committed = false;
        try {
            await readFile(path.join(transaction, "committed"));
            committed = true;
        } catch (error) {
            if (!(error instanceof Error && "code" in error && error.code === "ENOENT")) {
                throw error;
            }
        }
        if (!committed) {
            await restoreToolPublication(transaction, publication);
        }
        await rm(transaction, { recursive: true, force: true });
    }
}

export async function replaceToolDistributions(
    directory: string,
    prepare: (staged: string) => Promise<void>
): Promise<void> {
    const release = await acquireToolPublicationLock(directory);
    try {
        await replaceLockedToolDistributions(directory, prepare);
    } catch (error) {
        try {
            await release();
        } catch (cleanupError) {
            throw new AggregateError(
                [error, cleanupError],
                "Tool publication and lock cleanup failed"
            );
        }
        throw error;
    }
    await release();
}

async function replaceLockedToolDistributions(
    directory: string,
    prepare: (staged: string) => Promise<void>
): Promise<void> {
    directory = path.resolve(directory);
    const prefix = `.${path.basename(directory)}-`;
    await recoverToolPublications(directory, prefix);
    const transaction = await mkdtemp(path.join(path.dirname(directory), prefix));
    const staged = path.join(transaction, "staged");
    const previous = path.join(transaction, "previous");
    let publication: ToolPublication | undefined;
    try {
        await cp(directory, staged, { recursive: true });
        await prepare(staged);
        const original = await toolInventory(directory);
        const replacements = await toolInventory(staged);
        if (replacements.files.some((file) => original.directories.includes(file))) {
            throw new Error("A tool file cannot replace an existing directory");
        }
        if (replacements.directories.some((file) => original.files.includes(file))) {
            throw new Error("A tool directory cannot replace an existing file");
        }
        await cp(directory, previous, { recursive: true });
        const journal: ToolPublication = {
            directory,
            files: [...new Set([...original.files, ...replacements.files])],
            originalFiles: original.files,
            addedDirectories: replacements.directories.filter(
                (directory) => !original.directories.includes(directory)
            ),
            removedDirectories: original.directories.filter(
                (directory) => !replacements.directories.includes(directory)
            ),
        };
        await writeFile(path.join(transaction, "journal.pending"), JSON.stringify(journal));
        await rename(
            path.join(transaction, "journal.pending"),
            path.join(transaction, "journal.json")
        );
        publication = journal;
        for (const directory of publication.addedDirectories) {
            await mkdir(path.join(publication.directory, directory), { recursive: true });
        }
        await publishToolFiles(
            staged,
            publication,
            replacements.files,
            transaction,
            publication.removedDirectories
        );
        await writeFile(path.join(transaction, "committed"), "");
    } catch (error) {
        if (publication) {
            try {
                await restoreToolPublication(transaction, publication);
            } catch (rollbackError) {
                throw new AggregateError(
                    [error, rollbackError],
                    `Tool publication and rollback failed; original files remain at ${previous}`
                );
            }
        }
        try {
            await rm(transaction, { recursive: true, force: true });
        } catch (cleanupError) {
            throw new AggregateError(
                [error, cleanupError],
                `Tool preparation failed and staging cleanup failed at ${transaction}`
            );
        }
        throw error;
    }
    await rm(transaction, { recursive: true, force: true });
}

async function main() {
    const args = process.argv.slice(2);
    if (args.length === 0 || args.includes("--help")) {
        console.log(
            "Vendor existing tools without the embedded braces implementation.\nUsage from the Node SDK: npm run vendor:tools -- --run\nNo arguments makes no changes. --run downloads integrity-pinned upstream archives and writes reviewable package files to third_party/build-tools; licenses and bytes outside the recorded brace/security replacements are retained. Previously recorded obsolete files are removed only after every replacement succeeds. The formatter retains the repository's existing tinypool 2.2.0 override. This is a maintainer source update, not a live deployment: finish vendoring before running installs or builds. Generator processes are mutually exclusive; interrupted publication is recovered on the next run. Regenerate dependency locks after updating the vendored files."
        );
        return;
    }
    if (args.length !== 1 || args[0] !== "--run") {
        throw new Error(`Unknown arguments: ${args.join(" ")}`);
    }
    const source = ts
        .transpileModule(await readFile(new URL("./brace-patterns.ts", import.meta.url), "utf8"), {
            compilerOptions: {
                target: ts.ScriptTarget.ES2022,
                module: ts.ModuleKind.ESNext,
                newLine: ts.NewLineKind.LineFeed,
            },
        })
        .outputText.replace("export function", "function");
    const factory = `(() => { ${source}\nreturn createBracePatterns; })()`;
    const jsoncImplementation = ts
        .transpileModule(await readFile(new URL("./vendor-jsonc.ts", import.meta.url), "utf8"), {
            compilerOptions: {
                target: ts.ScriptTarget.ES2022,
                module: ts.ModuleKind.ESNext,
                newLine: ts.NewLineKind.LineFeed,
            },
        })
        .outputText.replace("export function", "function");
    await replaceToolDistributions(outputDirectory, async (directory) => {
        const provenance: Record<string, Awaited<ReturnType<typeof vendor>>> = {};
        for (const [id, integrity] of Object.entries(sourceArtifacts)) {
            provenance[id] = await vendor(id, integrity, factory, jsoncImplementation, directory);
            console.log(`Vendored ${id}: ${provenance[id].changed.join(", ")}`);
        }
        await writeDistributionProvenance(directory, provenance);
    });
}

if (
    process.argv[1] &&
    path.relative(fileURLToPath(import.meta.url), path.resolve(process.argv[1])) === ""
) {
    try {
        await main();
    } catch (error: unknown) {
        console.error(error);
        process.exitCode = 1;
    }
}
