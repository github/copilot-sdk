# Phase One Lane — Validation

You are a grading agent. The implementer claims Phase "One Lane" is complete. Verify the work meets all requirements.

Run each check. Report PASS or FAIL. At the end, give an overall verdict: PASS, PARTIAL, or FAIL.

---

## Check 1: Workflow file contains Java setup step

```bash
grep -q "setup-java" .github/workflows/update-copilot-dependency.yml && echo "PASS" || echo "FAIL"
```

---

## Check 2: Workflow updates Java codegen package.json

```bash
grep -q "Update @github/copilot in Java codegen" .github/workflows/update-copilot-dependency.yml && echo "PASS" || echo "FAIL"
```

---

## Check 3: Workflow updates .lastmerge

```bash
grep -q "java/.lastmerge" .github/workflows/update-copilot-dependency.yml && echo "PASS" || echo "FAIL"
```

---

## Check 4: Workflow updates POM property

```bash
grep -q "readonly-copilot-sdk-ref-impl-version-from-lastmerge-file-updated-by-reference-impl-sync" .github/workflows/update-copilot-dependency.yml && echo "PASS" || echo "FAIL"
```

---

## Check 5: Workflow runs Java codegen

```bash
grep -q "mvn generate-sources -Pcodegen" .github/workflows/update-copilot-dependency.yml && echo "PASS" || echo "FAIL"
```

---

## Check 6: Workflow compiles Java (validates generated code)

```bash
grep -q "mvn compile" .github/workflows/update-copilot-dependency.yml && echo "PASS" || echo "FAIL"
```

---

## Check 7: Java steps appear AFTER "Format generated code" and BEFORE "Create pull request"

```bash
FORMAT_LINE=$(grep -n "Format generated code" .github/workflows/update-copilot-dependency.yml | head -1 | cut -d: -f1)
JAVA_LINE=$(grep -n "Update @github/copilot in Java codegen" .github/workflows/update-copilot-dependency.yml | head -1 | cut -d: -f1)
PR_LINE=$(grep -n "Create pull request" .github/workflows/update-copilot-dependency.yml | head -1 | cut -d: -f1)

if [[ -n "$FORMAT_LINE" && -n "$JAVA_LINE" && -n "$PR_LINE" ]] && (( FORMAT_LINE < JAVA_LINE && JAVA_LINE < PR_LINE )); then
  echo "PASS"
else
  echo "FAIL: Java steps not in correct position (format=$FORMAT_LINE, java=$JAVA_LINE, pr=$PR_LINE)"
fi
```

---

## Check 8: Workflow ran successfully on the topic branch

```bash
RESULT=$(gh run list --workflow="Update @github/copilot Dependency" \
  --branch=edburns/80-java-monorepo-iterating --limit=1 --json conclusion --jq '.[0].conclusion')
if [[ "$RESULT" == "success" ]]; then
  echo "PASS"
else
  echo "FAIL: Latest run conclusion was '$RESULT'"
fi
```

---

## Check 9: No `continue-on-error` on Java steps

Java codegen/compile failures must halt the workflow (no fallback).

```bash
# Extract lines between Java codegen step and Create pull request step
JAVA_START=$(grep -n "Update @github/copilot in Java codegen" .github/workflows/update-copilot-dependency.yml | head -1 | cut -d: -f1)
PR_START=$(grep -n "Create pull request" .github/workflows/update-copilot-dependency.yml | head -1 | cut -d: -f1)
if sed -n "${JAVA_START},${PR_START}p" .github/workflows/update-copilot-dependency.yml | grep -q "continue-on-error"; then
  echo "FAIL: continue-on-error found on Java steps"
else
  echo "PASS"
fi
```

---

## Overall Verdict

- If all checks PASS → **PASS**
- If Check 8 fails but all others pass → **PARTIAL** (workflow logic correct but runtime issue)
- If any of Checks 1-7 or 9 fail → **FAIL**
