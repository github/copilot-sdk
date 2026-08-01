// @ts-check

/** @typedef {ReturnType<typeof import('@actions/github').getOctokit>} GitHub */
/** @typedef {typeof import('@actions/github').context} Context */
/** @typedef {{ number: number, body?: string | null, assignees?: Array<{login: string}> | null }} TrackingIssue */

const TRACKING_LABEL = "triage-agent-tracking";
const CCA_THRESHOLD = 10;
const MAX_TITLE_LENGTH = 50;

const TRACKING_ISSUE_BODY = `# Triage Agent Corrections

This issue tracks corrections to the triage agent system. When assigned to
Copilot, analyze the corrections and generate an improvement PR.

## Instructions for Copilot

When assigned:
1. Read each linked correction comment and the original issue for full context
2. Identify patterns (e.g., the classifier frequently confuses X with Y)
3. Determine which workflow file(s) need improvement
4. Use the \`agentic-workflows\` agent in this repo for guidance on workflow syntax and conventions
5. Open a PR with targeted changes to the relevant \`.md\` workflow files in \`.github/workflows/\`
6. **If you changed the YAML frontmatter** (between the \`---\` markers) of any workflow, run \`gh aw compile\` and commit the updated \`.lock.yml\` files. Changes to the markdown body (instructions) do NOT require recompilation.
7. Reference this issue in the PR description using \`Closes #<this issue number>\`
8. Include a summary of which corrections motivated each change

## Corrections

| Issue | Feedback | Submitted by | Date |
|-------|----------|--------------|------|
`;

/**
 * Truncates a title to the maximum length, adding ellipsis if needed.
 * @param {string} title
 * @returns {string}
 */
function truncateTitle(title) {
  const str = String(title || "");
  if (str.length <= MAX_TITLE_LENGTH) return str;
  return str.substring(0, MAX_TITLE_LENGTH - 3).trimEnd() + "...";
}

/**
 * Sanitizes text for use inside a markdown table cell by normalizing
 * newlines, collapsing whitespace, and trimming.
 * @param {string} text
 * @returns {string}
 */
function sanitizeText(text) {
  return String(text || "")
    .replace(/\r\n|\Here are the bugs and potential edge cases identified in `collect-corrections.js`, along with the corrected code:

---

## Identified Issues & Fixes

1. **Payload Resolution Fallback**:
   * **Issue**: `context.payload.client_payload ?? context.payload.inputs ?? {}` falls back to `{}` if the trigger payload is directly at `context.payload` (e.g., slash command or issue comment events).
   * **Fix**: Include `context.payload` in the fallback chain: `context.payload.client_payload ?? context.payload.inputs ?? context.payload`.

2. **Type Safety in Text Sanitization**:
   * **Issue**: `sanitizeText` assumes `text` is always a `string`. If `feedback` is supplied as a non-string value (e.g., a number or boolean in JSON), `.replace()` throws a `TypeError`.
   * **Fix**: Ensure `text` is cast to a string (`String(text ?? "")`) before calling string methods.

3. **GitHub API Filtering & Pagination**:
   * **Issue**: `github.rest.issues.listForRepo` returns Pull Requests alongside Issues and defaults to `per_page: 30`. If the repository has many open items, an existing tracking issue beyond page 1 could be missed, leading to duplicate tracking issues.
   * **Fix**: Add `per_page: 100` and filter out PRs (`!issue.pull_request`).

4. **Missing Table Header Handling**:
   * **Issue**: If `trackingIssue.body` is modified or missing the Markdown table header (`tableStart === -1`), new rows are appended to the end of the body without creating a valid table format.
   * **Fix**: Re-append the table header if it is missing before adding new rows.

---

## Corrected Code

```javascript
// @ts-check

/** @typedef {ReturnType<typeof import('@actions/github').getOctokit>} GitHub */
/** @typedef {typeof import('@actions/github').context} Context */
/** @typedef {{ number: number, body?: string | null, assignees?: Array<{login: string}> | null }} TrackingIssue */

const TRACKING_LABEL = "triage-agent-tracking";
const CCA_THRESHOLD = 10;
const MAX_TITLE_LENGTH = 50;

const TABLE_HEADER = "|-------|----------|--------------|------|";

const TRACKING_ISSUE_BODY = `# Triage Agent Corrections

This issue tracks corrections to the triage agent system. When assigned to
Copilot, analyze the corrections and generate an improvement PR.

## Instructions for Copilot

When assigned:
1. Read each linked correction comment and the original issue for full context
2. Identify patterns (e.g., the classifier frequently confuses X with Y)
3. Determine which workflow file(s) need improvement
4. Use the \`agentic-workflows\` agent in this repo for guidance on workflow syntax and conventions
5. Open a PR with targeted changes to the relevant \`.md\` workflow files in \`.github/workflows/\`
6. **If you changed the YAML frontmatter** (between the \`---\` markers) of any workflow, run \`gh aw compile\` and commit the updated \`.lock.yml\` files. Changes to the markdown body (instructions) do NOT require recompilation.
7. Reference this issue in the PR description using \`Closes #<this issue number>\`
8. Include a summary of which corrections motivated each change

## Corrections

| Issue | Feedback | Submitted by | Date |
${TABLE_HEADER}
`;

/**
 * Truncates a title to the maximum length, adding ellipsis if needed.
 * @param {string} title
 * @returns {string}
 */
function truncateTitle(title) {
  const str = String(title ?? "");
  if (str.length <= MAX_TITLE_LENGTH) return str;
  return str.substring(0, MAX_TITLE_LENGTH - 3).trimEnd() + "...";
}

/**
 * Sanitizes text for use inside a markdown table cell by normalizing
 * newlines, collapsing whitespace, and trimming.
 * @param {string} text
 * @returns {string}
 */
function sanitizeText(text) {
  return String(text ?? "")
    .replace(/\r\n|\r|\n/g, " ")
    .replace(/<br\s*\/?>/gi, " ")
    .replace(/\s+/g, " ")
    .trim();
}

/**
 * Escapes backslash and pipe characters so they don't break markdown table columns.
 * @param {string} text
 * @returns {string}
 */
function escapeForTable(text) {
  return String(text ?? "").replace(/\\/g, "\\\\").replace(/\|/g, "\\|");
}

/**
 * Resolves the feedback context from either a slash command or manual CLI dispatch.
 * @param {any} payload
 * @param {string} sender
 * @returns {{ issueNumber: number, feedback: string, sender: string }}
 */
function resolveContext(payload, sender) {
  const issueNumber =
    payload?.command?.resource?.number ?? payload?.issue_number;
  const feedback = payload?.data?.Feedback ?? payload?.feedback;

  if (!issueNumber) {
    throw new Error("Missing issue_number in payload");
  }
  if (!feedback) {
    throw new Error("Missing feedback in payload");
  }

  const parsed = Number(issueNumber);
  if (!Number.isFinite(parsed) || parsed < 1 || !Number.isInteger(parsed)) {
    throw new Error(`Invalid issue_number: ${issueNumber}`);
  }

  return { issueNumber: parsed, feedback: String(feedback), sender };
}

/**
 * Finds an open tracking issue with no assignees, or creates a new one.
 * @param {GitHub} github - Octokit instance
 * @param {string} owner
 * @param {string} repo
 */
async function findOrCreateTrackingIssue(github, owner, repo) {
  const { data: issues } = await github.rest.issues.listForRepo({
    owner,
    repo,
    labels: TRACKING_LABEL,
    state: "open",
    per_page: 100,
  });

  const available = issues.find(
    (issue) => !issue.pull_request && (issue.assignees ?? []).length === 0
  );

  if (available) {
    console.log(`Found existing tracking issue #${available.number}`);
    return available;
  }

  console.log("No available tracking issue found, creating one...");
  const { data: created } = await github.rest.issues.create({
    owner,
    repo,
    title: "Triage Agent Corrections",
    labels: [TRACKING_LABEL],
    body: TRACKING_ISSUE_BODY,
  });
  console.log(`Created tracking issue #${created.number}`);
  return created;
}

/**
 * Appends a correction row to the tracking issue's markdown table.
 * Returns the new correction count.
 * @param {GitHub} github - Octokit instance
 * @param {string} owner
 * @param {string} repo
 * @param {TrackingIssue} trackingIssue
 * @param {{ issueNumber: number, feedback: string, sender: string }} correction
 * @returns {Promise<number>}
 */
async function appendCorrection(github, owner, repo, trackingIssue, correction) {
  const { issueNumber, feedback, sender } = correction;

  const { data: issue } = await github.rest.issues.get({
    owner,
    repo,
    issue_number: issueNumber,
  });

  let body = trackingIssue.body || "";
  let tableStart = body.indexOf(TABLE_HEADER);

  if (tableStart === -1) {
    body = body.trimEnd() + `\n\n| Issue | Feedback | Submitted by | Date |\n${TABLE_HEADER}\n`;
    tableStart = body.indexOf(TABLE_HEADER);
  }

  const existingRows = body
    .slice(tableStart)
    .split("\n")
    .filter((line) => line.startsWith("| ")).length;

  const correctionCount = existingRows + 1;
  const today = new Date().toISOString().split("T")[0];

  const cleanTitle = sanitizeText(issue.title);
  const displayTitle = escapeForTable(truncateTitle(cleanTitle));
  const safeFeedback = escapeForTable(sanitizeText(feedback));

  const issueUrl = `[https://github.com/$](https://github.com/$){owner}/${repo}/issues/${issueNumber}`;
  const newRow = `| <a href="${issueUrl}">[#${issueNumber}] ${displayTitle}</a> | ${safeFeedback} | @${sender} | ${today} |`;
  const updatedBody = body.trimEnd() + "\n" + newRow + "\n";

  await github.rest.issues.update({
    owner,
    repo,
    issue_number: trackingIssue.number,
    body: updatedBody,
  });

  console.log(
    `Appended correction #${correctionCount} to tracking issue #${trackingIssue.number}`,
  );
  return correctionCount;
}

/**
 * Auto-assigns CCA if the correction threshold is reached.
 * @param {GitHub} github - Octokit instance
 * @param {string} owner
 * @param {string} repo
 * @param {TrackingIssue} trackingIssue
 * @param {number} correctionCount
 */
async function maybeAssignCCA(github, owner, repo, trackingIssue, correctionCount) {
  if (correctionCount >= CCA_THRESHOLD) {
    console.log(
      `Threshold reached (${correctionCount} >= ${CCA_THRESHOLD}). Assigning CCA...`,
    );
    await github.rest.issues.addAssignees({
      owner,
      repo,
      issue_number: trackingIssue.number,
      assignees: ["copilot"],
    });
  } else {
    console.log(
      `Threshold not reached (${correctionCount}/${CCA_THRESHOLD}) or CCA already assigned.`,
    );
  }
}

/**
 * Main entrypoint for actions/github-script.
 * @param {{ github: GitHub, context: Context }} params
 */
module.exports = async ({ github, context }) => {
  const { owner, repo } = context.repo;
  const payload = context.payload.client_payload ?? context.payload.inputs ?? context.payload ?? {};
  const sender = context.payload.sender?.login ?? "unknown";

  const correction = resolveContext(payload, sender);
  console.log(
    `Processing feedback for issue #${correction.issueNumber} from @${correction.sender}`,
  );

  const trackingIssue = await findOrCreateTrackingIssue(github, owner, repo);
  const correctionCount = await appendCorrection(
    github,
    owner,
    repo,
    trackingIssue,
    correction,
  );
  await maybeAssignCCA(github, owner, repo, trackingIssue, correctionCount);
};

// Export internals for testing
module.exports.truncateTitle = truncateTitle;
module.exports.sanitizeText = sanitizeText;
module.exports.escapeForTable = escapeForTable;
module.exports.resolveContext = resolveContext;
module.exports.findOrCreateTrackingIssue = findOrCreateTrackingIssue;
module.exports.appendCorrection = appendCorrection;
module.exports.maybeAssignCCA = maybeAssignCCA;
