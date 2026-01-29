# SDK Consistency Review Agent - Workflow Failure Analysis

**Workflow Run:** [#21486968231](https://github.com/github/copilot-sdk/actions/runs/21486968231)  
**Date:** January 29, 2026  
**Status:** Failed (Race Condition)

## Executive Summary

The SDK Consistency Review Agent workflow failed due to a race condition in the gh-aw infrastructure where the PR branch was deleted (after merge) before the workflow could checkout the code. **However, this failure prevented the workflow from catching a real documentation inconsistency** - the .NET SDK was missing important Azure provider configuration documentation that was added to the other three SDKs.

## What Happened

### Timeline
1. **16:49:13** - PR #260 opened (provider-info branch): "Update docs to reflect you need version for Azure Foundry"
2. **16:50:46** - SDK Consistency Review workflow triggered by PR event
3. **16:50:53** - **PR #260 merged** (branch deleted)
4. **16:51:08** - Workflow step "Checkout PR branch" failed: `fatal: couldn't find remote ref provider-info`

### The Failure
```
fatal: couldn't find remote ref provider-info
##[error]Failed to checkout PR branch: The process '/usr/bin/git' failed with exit code 128
```

The workflow's checkout step tried to `git fetch origin provider-info` but the branch no longer existed because it was deleted as part of the merge process.

## Root Cause Analysis

### Infrastructure Issue
The gh-aw workflow engine's `checkout_pr_branch.cjs` script uses the branch name for checkout instead of the commit SHA. This creates a race condition:

- **Branch names** are ephemeral - they get deleted after PR merge
- **Commit SHAs** persist indefinitely - they remain accessible even after branch deletion

**Solution:** The gh-aw infrastructure should use `github.event.pull_request.head.sha` for checkout, which is always available and never gets deleted. This has been logged as a separate work item for the gh-aw team.

### What the Workflow Missed

PR #260 modified README documentation for three SDKs:
- ✅ **Node.js SDK** - Added Azure provider configuration notes
- ✅ **Python SDK** - Added Azure provider configuration notes  
- ✅ **Go SDK** - Added Azure provider configuration notes
- ❌ **.NET SDK** - **Not updated** (inconsistency)

The changes included critical information:
- Azure OpenAI endpoints must use `type: "azure"`, not `type: "openai"`
- BaseURL should be just the host, not include `/openai/v1` path
- Model parameter is required when using custom providers
- Examples for Ollama, custom OpenAI-compatible APIs, and Azure OpenAI

This is **exactly the kind of cross-SDK inconsistency** the workflow is designed to catch.

## Resolution

### Immediate Fix
✅ **Added comprehensive Azure provider documentation to .NET SDK README** (commit: 9778e86)

The .NET SDK now has:
- Complete ProviderConfig field documentation
- Ollama example with local provider usage
- Custom OpenAI-compatible API example
- Azure OpenAI example with important notes
- All the same warnings and best practices as other SDKs

### Future Prevention
📋 **Proposed work item:** Fix gh-aw race condition by using commit SHA instead of branch name for checkout

This will prevent future workflow failures when PRs are merged quickly, ensuring reviews can complete even for fast-moving PRs.

## Key Takeaways

1. **The workflow failure was a symptom, not the disease** - The real issue was the documentation gap across SDKs
2. **Fast merges expose infrastructure limitations** - The gh-aw checkout logic needs to be more resilient
3. **Consistency reviews are valuable** - Even though the workflow failed, manual analysis found the exact kind of issue it was designed to catch

## Recommendations

1. **For gh-aw team:** Implement SHA-based checkout with fallback to branch name
2. **For SDK maintainers:** When updating documentation across SDKs, always check all four implementations (Node.js, Python, Go, .NET)
3. **For this repository:** Consider adding a docs linting check that validates critical sections exist in all SDK READMEs
