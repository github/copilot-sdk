type PatternOptions = {
    expand?: boolean;
    noempty?: boolean;
    nodupes?: boolean;
    keepEscaping?: boolean;
    keepQuotes?: boolean;
    escapeInvalid?: boolean;
    maxLength?: number;
    rangeLimit?: number | false;
    step?: number | string;
};

type FillRange = (
    start: string,
    end: string,
    step: string | number | undefined,
    options: PatternOptions & { toRegex?: boolean; wrap?: boolean; strictZeros?: boolean }
) => string | (number | string)[];

export function createBracePatterns(fillRange: FillRange, maximumLength = 10_000) {
    type Group = {
        kind: "root" | "brace" | "paren";
        branches: (string | number)[][];
        closed: boolean;
        dollar: boolean;
    };
    type Result = { literal: string; values: string[] };

    const evaluate = (pattern: string, options: PatternOptions): string[] => {
        const maximum = Math.min(maximumLength, options.maxLength ?? maximumLength);
        if (pattern.length > maximum) {
            throw new SyntaxError(
                `Input length (${pattern.length}), exceeds max characters (${maximum})`
            );
        }
        const groups: Group[] = [{ kind: "root", branches: [[]], closed: true, dollar: false }];
        const stack = [0];
        let braceDepth = 0;
        const append = (value: string | number) => {
            const branches = groups[stack[stack.length - 1]].branches;
            const parts = branches[branches.length - 1];
            if (typeof value === "string" && typeof parts[parts.length - 1] === "string") {
                parts[parts.length - 1] += value;
            } else {
                parts.push(value);
            }
        };

        for (let index = 0; index < pattern.length; index++) {
            const character = pattern[index];
            const current = groups[stack[stack.length - 1]];
            if (character === "\u00a0" || character === "\ufeff") {
                continue;
            }
            if (character === "\\") {
                append((options.keepEscaping ? "\\" : "") + (pattern[++index] ?? ""));
            } else if (character === "[") {
                const start = index;
                let brackets = 1;
                while (++index < pattern.length && brackets > 0) {
                    if (pattern[index] === "\\") {
                        index++;
                    } else if (pattern[index] === "[") {
                        brackets++;
                    } else if (pattern[index] === "]") {
                        brackets--;
                    }
                }
                append(pattern.slice(start, index));
                index--;
            } else if (character === "]") {
                append("\\]");
            } else if (character === '"' || character === "'" || character === "`") {
                let text = options.keepQuotes ? character : "";
                while (++index < pattern.length) {
                    if (pattern[index] === "\\") {
                        text += pattern[index] + (pattern[++index] ?? "");
                    } else if (pattern[index] === character) {
                        if (options.keepQuotes) {
                            text += character;
                        }
                        break;
                    } else {
                        text += pattern[index];
                    }
                }
                append(text);
            } else if (character === "{" || character === "(") {
                const previous = current.branches[current.branches.length - 1].at(-1);
                const group: Group = {
                    kind: character === "{" ? "brace" : "paren",
                    branches: [[]],
                    closed: false,
                    dollar:
                        current.dollar ||
                        (character === "{" &&
                            typeof previous === "string" &&
                            previous.endsWith("$")),
                };
                const child = groups.length;
                groups.push(group);
                append(child);
                stack.push(child);
                if (character === "{") {
                    braceDepth++;
                }
            } else if (
                (character === "}" && current.kind === "brace") ||
                (character === ")" && current.kind === "paren")
            ) {
                current.closed = true;
                stack.pop();
                if (character === "}") {
                    braceDepth--;
                }
            } else if (character === "," && braceDepth > 0) {
                current.branches.push([]);
            } else {
                append(character);
            }
        }

        // Children always have larger indices, so deeply nested patterns need no recursive walk.
        const results: Result[] = [];
        for (let index = groups.length - 1; index >= 0; index--) {
            const group = groups[index];
            const open = group.kind === "brace" ? "{" : group.kind === "paren" ? "(" : "";
            const close = !group.closed
                ? ""
                : group.kind === "brace"
                  ? "}"
                  : group.kind === "paren"
                    ? ")"
                    : "";
            const escapeLiteral =
                options.escapeInvalid && group.kind === "brace" && (group.dollar || !group.closed);
            const literal =
                (escapeLiteral ? `\\${open}` : open) +
                group.branches
                    .map((parts) =>
                        parts
                            .map((part) =>
                                typeof part === "string" ? part : results[part].literal
                            )
                            .join("")
                    )
                    .join(",") +
                (escapeLiteral && close ? `\\${close}` : close);
            const branches = group.branches.map((parts) => {
                let values = [""];
                for (const part of parts) {
                    const suffixes = typeof part === "string" ? [part] : results[part].values;
                    values = values.flatMap((prefix) => suffixes.map((suffix) => prefix + suffix));
                }
                return values;
            });
            let values: string[];
            if (group.dollar) {
                values = [literal];
            } else if (group.kind === "brace" && group.closed && branches.length > 1) {
                values =
                    options.expand === true
                        ? branches.flat()
                        : [
                              `(${branches
                                  .filter(
                                      (branch, branchIndex) =>
                                          branchIndex === 0 ||
                                          branchIndex === branches.length - 1 ||
                                          branch.join("") !== ""
                                  )
                                  .map((branch) => branch.join(""))
                                  .join("|")})`,
                          ];
            } else {
                const separators = options.expand === true || group.kind !== "paren" ? "," : "|";
                values = [""];
                for (const [branchIndex, branch] of branches.entries()) {
                    values = values.flatMap((prefix) =>
                        branch.map(
                            (suffix) => prefix + (branchIndex === 0 ? "" : separators) + suffix
                        )
                    );
                }
                values = values.map((value) => open + value + close);
                if (
                    group.kind === "brace" &&
                    group.closed &&
                    group.branches[0].every((part) => typeof part === "string")
                ) {
                    const range = literal.slice(1, -1).split("..");
                    if ((range.length === 2 || range.length === 3) && range.every(Boolean)) {
                        const limit = options.rangeLimit ?? 1_000;
                        const step = Number(range[2] || options.step || 1);
                        if (
                            options.expand === true &&
                            limit !== false &&
                            Number.isInteger(Number(range[0])) &&
                            Number.isInteger(Number(range[1])) &&
                            Number.isInteger(step) &&
                            Math.abs(Number(range[1]) - Number(range[0])) /
                                Math.max(Math.abs(step), 1) >=
                                limit
                        ) {
                            throw new RangeError("Brace range exceeds options.rangeLimit");
                        }
                        const filled = fillRange(
                            range[0],
                            range[1],
                            range[2],
                            options.expand === true
                                ? options
                                : { ...options, toRegex: true, wrap: false, strictZeros: true }
                        );
                        if (filled.length > 0) {
                            values = Array.isArray(filled)
                                ? filled.map(String)
                                : [range.length > 1 && filled.length > 1 ? `(${filled})` : filled];
                        }
                    }
                }
                if (
                    options.escapeInvalid &&
                    group.kind === "brace" &&
                    (branches.length === 1 || !group.closed)
                ) {
                    values = values.map((value) =>
                        value.replace(/^\{/, "\\{").replace(/\}$/, "\\}")
                    );
                }
            }
            results[index] = { literal, values };
        }
        return results[0].values;
    };

    const patterns = (input: string | string[], options: PatternOptions = {}): string[] => {
        let values = (Array.isArray(input) ? input : [input]).flatMap((pattern) =>
            evaluate(pattern, options)
        );
        if (options.expand === true && options.noempty) {
            values = values.filter(Boolean);
        }
        if (options.expand === true && options.nodupes) {
            values = [...new Set(values)];
        }
        return values;
    };
    return Object.assign(patterns, {
        expand(input: string | string[], options: PatternOptions = {}) {
            return patterns(input, { ...options, expand: true });
        },
    });
}
