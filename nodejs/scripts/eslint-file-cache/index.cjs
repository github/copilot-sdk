const { createHash } = require("node:crypto");
const { mkdirSync, readFileSync, statSync, writeFileSync } = require("node:fs");
const { dirname, resolve } = require("node:path");

function isMissing(error) {
    return error.code === "ENOENT" || error.code === "ENOTDIR";
}

function readEntries(cachePath) {
    let text;
    try {
        text = readFileSync(cachePath, "utf8");
    } catch (error) {
        if (isMissing(error)) {
            return new Map();
        }
        throw error;
    }

    let data;
    try {
        data = JSON.parse(text);
    } catch (error) {
        if (!(error instanceof SyntaxError)) {
            throw error;
        }
    }
    if (
        data?.version === 1 &&
        Array.isArray(data.entries) &&
        data.entries.every(
            (entry) =>
                Array.isArray(entry) &&
                entry.length === 2 &&
                typeof entry[0] === "string" &&
                entry[1] !== null &&
                typeof entry[1] === "object" &&
                !Array.isArray(entry[1])
        )
    ) {
        return new Map(data.entries);
    }

    process.emitWarning(`Ignoring obsolete or invalid ESLint cache: ${cachePath}`, {
        code: "COPILOT_ESLINT_CACHE",
    });
    return new Map();
}

// Only ESLint's create/getFileDescriptor/reconcile surface is needed. Keeping
// ESLint itself preserves its rules, inline suppressions, and --cache behavior.
exports.create = function create(cacheId, cacheDirectory, useChecksum) {
    const cachePath = resolve(cacheDirectory ?? ".", cacheId);
    const entries = readEntries(cachePath);
    const pending = new Map();

    function removeMissing(file) {
        try {
            statSync(file);
            return false;
        } catch (error) {
            if (!isMissing(error)) {
                if (pending.has(file)) {
                    throw error;
                }
                process.emitWarning(
                    `Unable to check unvisited ESLint cache entry ${file}: ${error.message}`,
                    { code: "COPILOT_ESLINT_CACHE" }
                );
                return false;
            }
            entries.delete(file);
            pending.delete(file);
            return true;
        }
    }

    return {
        getFileDescriptor(file) {
            let fingerprint;
            try {
                const stat = statSync(file);
                fingerprint = useChecksum
                    ? { hash: createHash("sha256").update(readFileSync(file)).digest("hex") }
                    : { size: stat.size, mtime: stat.mtimeMs };
            } catch (error) {
                if (!isMissing(error)) {
                    throw error;
                }
                entries.delete(file);
                pending.delete(file);
                return { key: file, notFound: true, err: error };
            }

            const previous = entries.get(file);
            const meta = pending.get(file)?.meta ?? { ...previous };
            const changed =
                !previous ||
                (useChecksum
                    ? previous.hash !== fingerprint.hash
                    : previous.size !== fingerprint.size || previous.mtime !== fingerprint.mtime);
            if (useChecksum) {
                delete meta.size;
                delete meta.mtime;
            } else {
                delete meta.hash;
            }
            Object.assign(meta, fingerprint);
            const descriptor = { key: file, changed, meta };
            pending.set(file, descriptor);
            return descriptor;
        },

        reconcile() {
            for (const [file, descriptor] of pending) {
                entries.set(file, descriptor.meta);
            }
            for (const file of entries.keys()) {
                removeMissing(file);
            }
            mkdirSync(dirname(cachePath), { recursive: true });
            writeFileSync(cachePath, JSON.stringify({ version: 1, entries: [...entries] }));
            pending.clear();
        },
    };
};
