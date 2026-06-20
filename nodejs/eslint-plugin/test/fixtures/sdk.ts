/** A perfectly stable API. */
export function stableGreeting(name: string): string {
    return `Hello, ${name}`;
}

/**
 * Start an experimental canvas session.
 * @experimental
 */
export function startCanvas(): string {
    return "canvas";
}

/**
 * Options bag for an experimental feature.
 * @experimental
 */
export interface CanvasOptions {
    title: string;
}

export class StableClient {
    greet(): string {
        return "hi";
    }

    /**
     * Enable experimental MCP apps support.
     * @experimental
     */
    enableMcpApps(): void {
        // ...
    }
}
