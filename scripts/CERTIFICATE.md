name: PoR AutoResonance Certification

on:
  push:
    branches: [main]
  pull_request:

jobs:
  por-certify:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repo
        uses: actions/checkout@v4

      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: 20

      - name: Install dependencies
        run: npm ci

      - name: Run PoR invariant certification
        run: |
          node scripts/por_certify.js

      - name: Generate certificate
        if: success()
        run: |
          echo "# PoR AutoResonance Certificate" > CERTIFICATE.md
          echo "" >> CERTIFICATE.md
          echo "Commit: $GITHUB_SHA" >> CERTIFICATE.md
          echo "Date: $(date -u)" >> CERTIFICATE.md
          echo "Status: CERTIFIED" >> CERTIFICATE.md

      - name: Upload certificate artifact
        if: success()
        uses: actions/upload-artifact@v4
        with:
          name: por-autoresonance-certificate
          path: CERTIFICATE.md
