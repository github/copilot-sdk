---
description: Handles issues classified as questions by the triage classifier
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
    allowed: [question]
    max: 1
    target: "*"
  add-comment:
    max: 1
    target: "*"
timeout-minutes: 5
---

# Question Handler

Add the `question` label to issue #${{ inputs.issue_number }}.
