# Phase Two Lanes — Agentic Fix Validation

You are a grading agent. The implementer claims Phase "Two Lanes — Lane 02" (agentic fix workflow) is complete. Verify the work.

Run each check. Report PASS or FAIL. At the end, give an overall verdict: PASS, PARTIAL, or FAIL.

---

## Check 1: Agentic workflow .md file exists

```bash
test -f .github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md && echo "PASS" || echo "FAIL"
```

---

## Check 2: Lock file exists (compiled successfully)

```bash
test -f .github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.lock.yml && echo "PASS" || echo "FAIL"
```

---

## Check 3: Lock file contains content-hash

```bash
grep -q "content-hash" .github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.lock.yml && echo "PASS" || echo "FAIL"
```

---

## Check 4: Workflow does NOT edit generated code

```bash
if grep -q "java/src/generated" .github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md; then
  # It's referenced but should be as a boundary (DO NOT edit)
  if grep -B1 "java/src/generated" .github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md | grep -qi "NOT\|❌\|never\|boundary"; then
    echo "PASS: references generated dir only as boundary"
  else
    echo "FAIL: appears to instruct editing generated code"
  fi
else
  echo "PASS: no reference to generated dir"
fi
```

---

## Check 5: Workflow does NOT create tests in com.github.copilot.generated package

```bash
if grep "com.github.copilot.generated\|com/github/copilot/sdk/generated" .github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md | grep -qi "NOT\|❌\|exclud"; then
  echo "PASS"
elif ! grep -q "com.github.copilot.generated\|com/github/copilot/sdk/generated" .github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md; then
  echo "PASS: no mention of generated test package"
else
  echo "FAIL: may create tests in generated package without prohibition"
fi
```

---

## Check 6: Workflow uses mvn verify for validation

```bash
grep -q "mvn verify" .github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md && echo "PASS" || echo "FAIL"
```

---

## Check 7: Workflow uses spotless:apply for formatting

```bash
grep -q "spotless:apply" .github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md && echo "PASS" || echo "FAIL"
```

---

## Check 8: Workflow has workflow_dispatch trigger

```bash
grep -q "workflow_dispatch" .github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md && echo "PASS" || echo "FAIL"
```

---

## Check 9: Workflow accepts branch and pr_number inputs

```bash
grep -q "branch:" .github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md && \
grep -q "pr_number:" .github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.md && echo "PASS" || echo "FAIL"
```

---

## Check 10: Agentic workflow ran on the topic branch (or is dispatchable)

```bash
# Verify it's at least dispatchable — actual run is best-effort
if gh workflow list --all | grep -q "java-adapt-handwritten-code-to-accept-upgrade-changes"; then
  echo "PASS: workflow recognized by GitHub"
else
  # May not be on main yet; check lock file validity instead
  if grep -q "content-hash" .github/workflows/java-adapt-handwritten-code-to-accept-upgrade-changes.lock.yml 2>/dev/null; then
    echo "PASS: lock file valid (workflow not yet on main)"
  else
    echo "FAIL"
  fi
fi
```

---

## Overall Verdict

- If all checks PASS → **PASS**
- If Checks 1-3 pass but Check 10 is FAIL → **PARTIAL** (workflow correct but not yet runnable from branch)
- If any of Checks 1-9 fail → **FAIL**
