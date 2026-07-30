# Agent Factory patterns

Composable orchestration patterns built on the factory context. Read [factories.md](./factories.md) first for the API and its semantics. The API is experimental.

Every snippet below assumes the surrounding `async (ctx) => { ... }` run body and destructures the hooks it uses. Three rules apply throughout, because breaking them fails silently:

- **Give every independent subagent a unique `label`.** Identical prompt-and-options pairs memoize into a single shared subagent.
- **Guard every `agent()` result.** An ordinary failure resolves to `null` rather than throwing.
- **Filter with `v => v !== null`,** not `Boolean`, which also discards a valid `false`, `0`, or `""`.

## Multi-stage review

The default shape: fan out across dimensions, and let each dimension verify as soon as its own review lands. No barrier, so a slow dimension never holds up a fast one.

```js
async ({ pipeline, parallel, agent, phase, log }) => {
    const FINDINGS = {
        type: "object",
        properties: {
            findings: {
                type: "array",
                items: {
                    type: "object",
                    properties: { title: { type: "string" } },
                    required: ["title"],
                },
            },
        },
        required: ["findings"],
    };
    const VERDICT = {
        type: "object",
        properties: { isReal: { type: "boolean" } },
        required: ["isReal"],
    };
    const DIMENSIONS = [
        { key: "bugs", prompt: "Review the diff for correctness bugs. Return JSON {findings:[{title}]}." },
        { key: "perf", prompt: "Review the diff for performance issues. Return JSON {findings:[{title}]}." },
    ];

    phase("Review"); // Run-global: set it before the fan-out, never inside a stage.
    const perDimension = await pipeline(
        DIMENSIONS,
        (d) => agent(d.prompt, { label: `review:${d.key}`, schema: FINDINGS }),
        (review, d) => {
            if (!review) {
                log(`review:${d.key} produced nothing`);
                return [];
            }
            return parallel(
                (review.findings ?? []).map((f, i) => () =>
                    agent(`Adversarially verify this finding is real: ${f.title}`, {
                        label: `verify:${d.key}:${i}`,
                        schema: VERDICT,
                    }).then((v) => (v && v.isReal ? f : null))
                )
            );
        }
    );

    return { confirmed: perDimension.flat().filter((v) => v !== null) };
};
```

## When a barrier is correct

Deduplicating across every finding needs the whole set in hand, so the barrier earns its cost here. Dedup itself is plain JavaScript, done in the body between the two fan-outs. This excerpt reuses `FINDINGS`, `VERDICT`, and `DIMENSIONS` from the previous example — define them inside your own function.

```js
const all = await parallel(
    DIMENSIONS.map((d) => () => agent(d.prompt, { label: `find:${d.key}`, schema: FINDINGS }))
);
const findings = all.filter((v) => v !== null).flatMap((r) => r.findings ?? []);
const deduped = [...new Map(findings.map((f) => [f.title, f])).values()]; // Needs all of them.
const verified = await parallel(
    deduped.map((f, i) => () => agent(`Verify: ${f.title}`, { label: `verify:${i}`, schema: VERDICT }))
);
```

## Loop until count

Accumulate toward a target. Each iteration needs a unique identity — a unique label plus a prompt that excludes what has already been found — a bounded attempt count, and a null guard.

```js
const BUG = {
    type: "object",
    properties: { title: { type: "string" } },
    required: ["title"],
};

const bugs = [];
let attempt = 0;
while (bugs.length < 10 && attempt < 30) {
    const r = await agent(
        `Find ONE distinct bug NOT already listed: ${JSON.stringify(bugs.map((b) => b.title))}. Return JSON {title}.`,
        { label: `finder:${attempt}`, schema: BUG }
    );
    attempt++;
    if (r && r.title) bugs.push(r);
    log(`${bugs.length}/10 found`);
}
```

## Loop until dry

Keep spawning finders until some number of consecutive rounds surface nothing new. Deduplicate against everything *seen*, not just what was kept, or discarded findings resurface every round.

```js
const BUGS = {
    type: "object",
    properties: {
        bugs: {
            type: "array",
            items: { type: "object", properties: { title: { type: "string" } }, required: ["title"] },
        },
    },
    required: ["bugs"],
};
const VERDICT = {
    type: "object",
    properties: { real: { type: "boolean" } },
    required: ["real"],
};

const seen = new Set();
const confirmed = [];
const keyOf = (b) => b.title.toLowerCase();
let dry = 0;
let round = 0;

while (dry < 2 && round < 20) {
    const found = (
        await parallel(
            [0, 1, 2].map((i) => () =>
                agent(`Find bugs (finder ${i}, round ${round}). Return JSON {bugs:[{title}]}.`, {
                    label: `find:${round}:${i}`,
                    schema: BUGS,
                })
            )
        )
    )
        .filter((v) => v !== null)
        .flatMap((r) => r.bugs ?? []);

    const fresh = found.filter((b) => {
        const k = keyOf(b);
        if (seen.has(k)) return false;
        seen.add(k);
        return true;
    });

    if (!fresh.length) {
        dry++;
        round++;
        continue;
    }
    dry = 0;

    const judged = await parallel(
        fresh.map((b, i) => () =>
            parallel(
                ["correctness", "security", "repro"].map((lens) => () =>
                    agent(`Judge via ${lens}: is "${b.title}" real? Return JSON {real}.`, {
                        label: `judge:${round}:${i}:${lens}`,
                        schema: VERDICT,
                    })
                )
            ).then((vs) => ({ b, real: vs.filter((v) => v !== null).filter((v) => v.real).length >= 2 }))
        )
    );

    confirmed.push(...judged.filter((v) => v !== null && v.real).map((v) => v.b));
    round++;
}
```

## Quality patterns

Compose these freely.

- **Adversarial verify.** Spawn several independent skeptics per finding, each prompted to *refute* it and to default to refuted when uncertain. Keep only what a majority fails to refute.
- **Perspective-diverse verify.** Give each verifier a distinct lens — correctness, security, performance, does-it-reproduce — instead of several identical skeptics. The distinct prompts also stop them memoizing into one subagent.
- **Judge panel.** Generate several independent attempts from different angles, score them with parallel judges, then synthesize from the winner while grafting the best ideas from the runners-up.
- **Multi-modal sweep.** Run parallel searchers that each look a different way: by container, by content, by entity, by time.
- **Completeness critic.** End with an agent asking what is missing — an angle not run, a claim unverified, a source unread — and use its answer to seed the next round.
- **No silent caps.** When the factory bounds its own coverage with a top-N, a sampling step, or a no-retry rule, `log()` what was dropped.

## Scaling

Match the orchestration to what was asked. A quick check wants a couple of subagents and single-vote verification; a request to be thorough or comprehensive wants a larger finder pool, a three-to-five vote adversarial pass, and a synthesis stage.

There is no in-script budget object. Scale with your own counters, as in the loop patterns above, and treat the declared limits as the safety ceiling rather than the control mechanism. Only `agent()` spawns are throttled, by `maxConcurrentSubagents` falling back to `maxTotalSubagents`; with neither declared there is no built-in concurrency cap, so declare one before fanning out widely. `parallel` itself is `Promise.all`, so non-agent work in a thunk runs fully concurrently regardless.

These patterns are not exhaustive. Compose novel harnesses — tournament brackets, self-repair loops, staged escalation — when the task calls for it.
