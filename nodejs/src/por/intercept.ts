export type PorDecision = "PROCEED" | "ABSTAIN";

export function porCheck(args: {
  coherence: number;
  drift: number;
  thresholds?: { coherence: number; drift: number };
}): { decision: PorDecision; reason?: string } {
  const thresholds = args.thresholds ?? { coherence: 0.72, drift: 0.18 };

  if (args.drift > thresholds.drift) {
    return { decision: "ABSTAIN", reason: "drift_gt_tolerance" };
  }
  if (args.coherence < thresholds.coherence) {
    return { decision: "ABSTAIN", reason: "coherence_lt_threshold" };
  }
  return { decision: "PROCEED" };
}
