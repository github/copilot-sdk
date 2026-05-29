# Phase Two Lanes — Manual Fix Validation

You are a grading agent. The implementer claims Phase "Two Lanes — Lane 01" (manual fix plan in PR body) is complete. Verify the work.

Run each check. Report PASS or FAIL. At the end, give an overall verdict: PASS, PARTIAL, or FAIL.

---

## Check 1: PR body template contains adaptation plan header

```bash
grep -q "Java Handwritten Code Adaptation Plan" .github/workflows/update-copilot-dependency.yml && echo "PASS" || echo "FAIL"
```

---

## Check 2: PR body template mentions mvn verify

```bash
grep -q "mvn verify" .github/workflows/update-copilot-dependency.yml && echo "PASS" || echo "FAIL"
```

---

## Check 3: PR body template mentions constructor signature changes

```bash
grep -q "Constructor signature" .github/workflows/update-copilot-dependency.yml || \
grep -q "constructor" .github/workflows/update-copilot-dependency.yml && echo "PASS" || echo "FAIL"
```

---

## Check 4: PR body template mentions the agentic workflow as alternative

```bash
grep -q "java-adapt-handwritten-code-to-accept-upgrade-changes" .github/workflows/update-copilot-dependency.yml && echo "PASS" || echo "FAIL"
```

---

## Check 5: PR body template mentions spotless

```bash
grep -q "spotless" .github/workflows/update-copilot-dependency.yml && echo "PASS" || echo "FAIL"
```

---

## Check 6: If a PR was created, verify its body contains the plan

```bash
# Try to find the most recent update-copilot-* PR on the topic branch
PR_BODY=$(gh pr list --head "update-copilot-" --state open --json body --jq '.[0].body' 2>/dev/null || echo "")
if [[ -z "$PR_BODY" ]]; then
  echo "SKIP: No open update-copilot PR found (acceptable if workflow produced 'no changes')"
else
  if echo "$PR_BODY" | grep -q "Java Handwritten Code Adaptation Plan"; then
    echo "PASS"
  else
    echo "FAIL: PR body does not contain adaptation plan"
  fi
fi
```

---

## Overall Verdict

- If Checks 1-5 all PASS → **PASS** (Check 6 SKIP is acceptable)
- If Check 6 is FAIL when a PR exists → **PARTIAL**
- If any of Checks 1-5 fail → **FAIL**
