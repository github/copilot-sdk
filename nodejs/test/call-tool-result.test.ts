import { describe, expect, it } from "vitest";
import { convertMcpCallToolResult } from "../src/types.js";

type McpCallToolResult = Parameters<typeof convertMcpCallToolResult>[0];

describe("convertMcpCallToolResult", () => {
    it("extracts text from text content blocks", () => {
        const input: McpCallToolResult = {
            content: [
                { type: "text", text: "line 1" },
                { type: "text", text: "line 2" },
            ],
        };

        const result = convertMcpCallToolResult(input);

        expect(result.textResultForLlm).toBe("line 1\nline 2");
        expect(result.resultType).toBe("success");
        expect(result.binaryResultsForLlm).toBeUndefined();
    });

    it("maps isError to failure resultType", () => {
        const input: McpCallToolResult = {
            content: [{ type: "text", text: "error occurred" }],
            isError: true,
        };

        const result = convertMcpCallToolResult(input);

        expect(result.textResultForLlm).toBe("error occurred");
        expect(result.resultType).toBe("failure");
    });

    it("maps isError: false to success", () => {
        const input: McpCallToolResult = {
            content: [{ type: "text", text: "ok" }],
            isError: false,
        };

        expect(convertMcpCallToolResult(input).resultType).toBe("success");
    });

    it("converts image content to binaryResultsForLlm", () => {
        const input: McpCallToolResult = {
            content: [{ type: "image", data: "base64data", mimeType: "image/png" }],
        };

        const result = convertMcpCallToolResult(input);

        expect(result.textResultForLlm).toBe("");
        expect(result.binaryResultsForLlm).toHaveLength(1);
        expect(result.binaryResultsForLlm![0]).toEqual({
            data: "base64data",
            mimeType: "image/png",
            type: "image",
        });
    });

    it("converts resource with text to textResultForLlm", () => {
        const input: McpCallToolResult = {
            content: [
                {
                    type: "resource",
                    resource: { uri: "file:///tmp/data.txt", text: "file contents" },
                },
            ],
        };

        const result = convertMcpCallToolResult(input);

        expect(result.textResultForLlm).toBe("file contents");
    });

    it("converts resource with blob to binaryResultsForLlm", () => {
        const input: McpCallToolResult = {
            content: [
                {
                    type: "resource",
                    resource: {
                        uri: "file:///tmp/image.png",
                        mimeType: "image/png",
                        blob: "blobdata",
                    },
                },
            ],
        };

        const result = convertMcpCallToolResult(input);

        expect(result.binaryResultsForLlm).toHaveLength(1);
        expect(result.binaryResultsForLlm![0]).toEqual({
            data: "blobdata",
            mimeType: "image/png",
            type: "resource",
            description: "file:///tmp/image.png",
        });
    });

    it("handles mixed content types", () => {
        const input: McpCallToolResult = {
            content: [
                { type: "text", text: "Analysis complete" },
                { type: "image", data: "chartdata", mimeType: "image/svg+xml" },
                {
                    type: "resource",
                    resource: { uri: "file:///report.txt", text: "Report details" },
                },
            ],
        };

        const result = convertMcpCallToolResult(input);

        expect(result.textResultForLlm).toBe("Analysis complete\nReport details");
        expect(result.binaryResultsForLlm).toHaveLength(1);
        expect(result.binaryResultsForLlm![0]!.mimeType).toBe("image/svg+xml");
    });

    it("handles empty content array", () => {
        const result = convertMcpCallToolResult({ content: [] });

        expect(result.textResultForLlm).toBe("");
        expect(result.resultType).toBe("success");
        expect(result.binaryResultsForLlm).toBeUndefined();
    });

    it("defaults resource blob mimeType to application/octet-stream", () => {
        const input: McpCallToolResult = {
            content: [
                {
                    type: "resource",
                    resource: { uri: "file:///data.bin", blob: "binarydata" },
                },
            ],
        };

        const result = convertMcpCallToolResult(input);

        expect(result.binaryResultsForLlm![0]!.mimeType).toBe("application/octet-stream");
    });

    it("handles text block with missing text field without corrupting output", () => {
        // The input type uses structural typing, so type-specific fields might be absent
        // at runtime. convertMcpCallToolResult must be defensive.
        const input = { content: [{ type: "text" }] } as unknown as McpCallToolResult;

        const result = convertMcpCallToolResult(input);

        expect(result.textResultForLlm).toBe("");
        expect(result.textResultForLlm).not.toBe("undefined");
    });

    it("handles resource block with missing resource field without crashing", () => {
        // A resource content item missing the resource field would crash with an
        // unguarded block.resource.text access. Optional chaining must be used.
        const input = { content: [{ type: "resource" }] } as unknown as McpCallToolResult;

        expect(() => convertMcpCallToolResult(input)).not.toThrow();
        const result = convertMcpCallToolResult(input);
        expect(result.textResultForLlm).toBe("");
    });
});
