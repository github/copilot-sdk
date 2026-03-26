---
description: Handles issues classified as bugs by the triage classifier
on:
  workflow_call:
    inputs:
      payload:
        type: string
        required: false
      issue_number:
        type: string
        required: true
permissions:
  contents: read
  issues: read
  pull-requests: read
tools:
  github:
    toolsets: [default]
    min-integrity: none
safe-outputs:
  add-labels:
    allowed: [bug]
    max: 1
    target: "*"
  add-comment:
    max: 1
    target: "*"
timeout-minutes: 5
---

# Bug Handler

Add the `bug` label to issue #${{ inputs.issue_number }} and leave a comment. The comment should say the issue was classified as a bug, and must end with this HTML comment (include it verbatim):

<!-- triage-agent: [insert your feedback here] -->
