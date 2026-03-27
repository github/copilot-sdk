import { describe, it, expect, vi } from "vitest";

const mod = await import("../collect-corrections.js");
const {
  truncateTitle,
  sanitizeText,
  escapeForTable,
  resolveContext,
  findOrCreateTrackingIssue,
  appendCorrection,
  maybeAssignCCA,
} = mod;

// ---------------------------------------------------------------------------
// Pure functions
// ---------------------------------------------------------------------------

describe("truncateTitle", () => {
  it("returns short titles unchanged", () => {
    expect(truncateTitle("Short title")).toBe("Short title");
  });

  it("returns titles at exactly the max length unchanged", () => {
    const title = "a".repeat(50);
    expect(truncateTitle(title)).toBe(title);
  });

  it("truncates long titles with ellipsis", () => {
    const title = "a".repeat(60);
    const result = truncateTitle(title);
    expect(result.length).toBeLessThanOrEqual(50);
    expect(result).toMatch(/\.\.\.$/);
  });

  it("trims trailing whitespace before ellipsis", () => {
    const title = "a".repeat(44) + "   " + "b".repeat(10);
    const result = truncateTitle(title);
    expect(result).not.toMatch(/\s\.\.\.$/);
    expect(result).toMatch(/\.\.\.$/);
  });
});

describe("sanitizeText", () => {
  it("collapses newlines into spaces", () => {
    expect(sanitizeText("line1\nline2\r\nline3\rline4")).toBe(
      "line1 line2 line3 line4",
    );
  });

  it("replaces <br> tags with spaces", () => {
    expect(sanitizeText("hello<br>world<br />there")).toBe(
      "hello world there",
    );
  });

  it("collapses multiple spaces", () => {
    expect(sanitizeText("too   many    spaces")).toBe("too many spaces");
  });

  it("trims leading and trailing whitespace", () => {
    expect(sanitizeText("  padded  ")).toBe("padded");
  });

  it("handles empty string", () => {
    expect(sanitizeText("")).toBe("");
  });
});

describe("escapeForTable", () => {
  it("escapes pipe characters", () => {
    expect(escapeForTable("a | b")).toBe("a \\| b");
  });

  it("escapes backslashes", () => {
    expect(escapeForTable("path\\to\\file")).toBe("path\\\\to\\\\file");
  });

  it("escapes both pipes and backslashes", () => {
    expect(escapeForTable("a\\|b")).toBe("a\\\\\\|b");
  });

  it("returns clean text unchanged", () => {
    expect(escapeForTable("no special chars")).toBe("no special chars");
  });
});

describe("resolveContext", () => {
  it("resolves from slash command payload", () => {
    const payload = {
      command: { resource: { number: 42 } },
      data: { Feedback: "Wrong label" },
    };
    const result = resolveContext(payload, "testuser");
    expect(result).toEqual({
      issueNumber: 42,
      feedback: "Wrong label",
      sender: "testuser",
    });
  });

  it("resolves from manual dispatch payload", () => {
    const payload = {
      issue_number: "7",
      feedback: "Should be enhancement",
    };
    const result = resolveContext(payload, "admin");
    expect(result).toEqual({
      issueNumber: 7,
      feedback: "Should be enhancement",
      sender: "admin",
    });
  });

  it("prefers slash command fields over dispatch fields", () => {
    const payload = {
      command: { resource: { number: 10 } },
      data: { Feedback: "From slash" },
      issue_number: "99",
      feedback: "From dispatch",
    };
    const result = resolveContext(payload, "user");
    expect(result.issueNumber).toBe(10);
    expect(result.feedback).toBe("From slash");
  });

  it("throws on missing issue number", () => {
    expect(() => resolveContext({ feedback: "oops" }, "u")).toThrow(
      "Missing issue_number",
    );
  });

  it("throws on missing feedback", () => {
    expect(() =>
      resolveContext({ issue_number: "1" }, "u"),
    ).toThrow("Missing feedback");
  });
});

// ---------------------------------------------------------------------------
// Octokit-dependent functions
// ---------------------------------------------------------------------------

function mockGitHub(overrides: Record<string, any> = {}) {
  return {
    rest: {
      issues: {
        listForRepo: vi.fn().mockResolvedValue({ data: [] }),
        create: vi.fn().mockResolvedValue({
          data: { number: 100, body: "" },
        }),
        get: vi.fn().mockResolvedValue({
          data: { title: "Test issue title", number: 1 },
        }),
        update: vi.fn().mockResolvedValue({}),
        addAssignees: vi.fn().mockResolvedValue({}),
        ...overrides,
      },
    },
  } as any;
}

const OWNER = "test-owner";
const REPO = "test-repo";

describe("findOrCreateTrackingIssue", () => {
  it("returns existing unassigned tracking issue", async () => {
    const existing = { number: 5, assignees: [], body: "..." };
    const github = mockGitHub({
      listForRepo: vi.fn().mockResolvedValue({ data: [existing] }),
    });

    const result = await findOrCreateTrackingIssue(github, OWNER, REPO);
    expect(result).toBe(existing);
    expect(github.rest.issues.create).not.toHaveBeenCalled();
  });

  it("skips issues with assignees and creates a new one", async () => {
    const assigned = {
      number: 5,
      assignees: [{ login: "copilot" }],
      body: "...",
    };
    const github = mockGitHub({
      listForRepo: vi.fn().mockResolvedValue({ data: [assigned] }),
    });

    const result = await findOrCreateTrackingIssue(github, OWNER, REPO);
    expect(result.number).toBe(100); // from create mock
    expect(github.rest.issues.create).toHaveBeenCalledWith(
      expect.objectContaining({
        owner: OWNER,
        repo: REPO,
        title: "Triage Agent Corrections",
      }),
    );
  });

  it("creates a new issue when none exist", async () => {
    const github = mockGitHub();

    const result = await findOrCreateTrackingIssue(github, OWNER, REPO);
    expect(result.number).toBe(100);
    expect(github.rest.issues.create).toHaveBeenCalled();
  });
});

describe("appendCorrection", () => {
  const trackingBody = [
    "# Triage Agent Corrections",
    "",
    "| Issue | Feedback | Submitted by | Date |",
    "|-------|----------|--------------|------|",
    "",
  ].join("\n");

  it("appends a row and returns correction count of 1", async () => {
    const github = mockGitHub();
    const trackingIssue = { number: 10, body: trackingBody } as any;
    const correction = {
      issueNumber: 3,
      feedback: "Wrong label",
      sender: "alice",
    };

    const count = await appendCorrection(
      github,
      OWNER,
      REPO,
      trackingIssue,
      correction,
    );

    expect(count).toBe(1);
    expect(github.rest.issues.update).toHaveBeenCalledWith(
      expect.objectContaining({
        issue_number: 10,
        body: expect.stringContaining("[#3]"),
      }),
    );
  });

  it("counts existing rows correctly", async () => {
    const bodyWithRows =
      trackingBody.trimEnd() +
      "\n| <a href=\"url\">[#1] Title</a> | feedback | @bob | 2026-01-01 |\n";
    const github = mockGitHub();
    const trackingIssue = { number: 10, body: bodyWithRows } as any;
    const correction = {
      issueNumber: 2,
      feedback: "Also wrong",
      sender: "carol",
    };

    const count = await appendCorrection(
      github,
      OWNER,
      REPO,
      trackingIssue,
      correction,
    );

    expect(count).toBe(2);
  });

  it("handles empty tracking issue body", async () => {
    const github = mockGitHub();
    const trackingIssue = { number: 10, body: "" } as any;
    const correction = {
      issueNumber: 1,
      feedback: "test",
      sender: "user",
    };

    const count = await appendCorrection(
      github,
      OWNER,
      REPO,
      trackingIssue,
      correction,
    );

    // No table header found → 0 existing rows + 1
    expect(count).toBe(1);
  });

  it("sanitizes and escapes feedback in the row", async () => {
    const github = mockGitHub();
    const trackingIssue = { number: 10, body: trackingBody } as any;
    const correction = {
      issueNumber: 1,
      feedback: "has | pipe\nand newline",
      sender: "user",
    };

    await appendCorrection(github, OWNER, REPO, trackingIssue, correction);

    const updatedBody =
      github.rest.issues.update.mock.calls[0][0].body as string;
    expect(updatedBody).toContain("has \\| pipe and newline");
    // Verify the feedback cell doesn't contain raw newlines
    const rows = updatedBody.split("\n").filter((l) => l.startsWith("| <a"));
    expect(rows).toHaveLength(1);
    expect(rows[0]).not.toContain("\n");
  });
});

describe("maybeAssignCCA", () => {
  it("assigns CCA when threshold is reached", async () => {
    const github = mockGitHub();
    const trackingIssue = { number: 10 } as any;

    await maybeAssignCCA(github, OWNER, REPO, trackingIssue, 10);

    expect(github.rest.issues.addAssignees).toHaveBeenCalledWith({
      owner: OWNER,
      repo: REPO,
      issue_number: 10,
      assignees: ["copilot"],
    });
  });

  it("assigns CCA when threshold is exceeded", async () => {
    const github = mockGitHub();
    const trackingIssue = { number: 10 } as any;

    await maybeAssignCCA(github, OWNER, REPO, trackingIssue, 15);

    expect(github.rest.issues.addAssignees).toHaveBeenCalled();
  });

  it("does not assign CCA below threshold", async () => {
    const github = mockGitHub();
    const trackingIssue = { number: 10 } as any;

    await maybeAssignCCA(github, OWNER, REPO, trackingIssue, 9);

    expect(github.rest.issues.addAssignees).not.toHaveBeenCalled();
  });
});
