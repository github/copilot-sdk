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
  if (title.length <= MAX_TITLE_LENGTH) return title;
  return title.substring(0, MAX_TITLE_LENGTH - 3).trimEnd() + "...";
}

/**
 * Sanitizes text for use inside a markdown table cell by normalizing
 * newlines, collapsing whitespace, and trimming.
 * @param {string} text
 * @returns {string}
 */
function sanitizeText(text) {
  return text
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
  return text.replace(/\\/g, "\\\\").replace(/\|/g, "\\|");
}

/**
 * Resolves the feedback context from either a slash command or manual CLI dispatch.
 * @param {any} payload
 * @param {string} sender
 * @returns {{ issueNumber: number, feedback: string, sender: string }}
 */
function resolveContext(payload, sender) {
  const issueNumber =
    payload.command?.resource?.number ?? payload.issue_number;
  const feedback = payload.data?.Feedback ?? payload.feedback;

  if (!issueNumber) {
    throw new Error("Missing issue_number in payload");
  }
  if (!feedback) {
    throw new Error("Missing feedback in payload");
  }

  return { issueNumber: Number(issueNumber), feedback, sender };
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
  });

  const available = issues.find((issue) => (issue.assignees ?? []).length === 0);

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

  const body = trackingIssue.body || "";
  const tableHeader = "|-------|----------|--------------|------|";
  const tableStart = body.indexOf(tableHeader);
  const existingRows =
    tableStart === -1
      ? 0
      : body
          .slice(tableStart)
          .split("\n")
          .filter((line) => line.startsWith("| ")).length;
  const correctionCount = existingRows + 1;
  const today = new Date().toISOString().split("T")[0];

  const cleanTitle = sanitizeText(issue.title);
  const displayTitle = escapeForTable(truncateTitle(cleanTitle));
  const safeFeedback = escapeForTable(sanitizeText(feedback));

  const issueUrl = `https://github.com/${owner}/${repo}/issues/${issueNumber}`;
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
  const payload = context.payload.client_payload ?? context.payload.inputs ?? {};
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
