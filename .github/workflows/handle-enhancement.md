---
description: Handles issues classified as enhancements by the triage classifier
concurrency:
  job-discriminator: ${{ inputs.issue_number }}
on:
  workflow_call:
    inputs:
      payload:
        type: string
        required: false
      issue_number:
        type: string
        required: true
  roles: all
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
    allowed: [enhancement]
    max: 1
    target: "*"
  add-comment:
    max: 1
    target: "*"
timeout-minutes: 5
---

# Enhancement Handler

Add the `enhancement` label to issue #${{ inputs.issue_number }}.
