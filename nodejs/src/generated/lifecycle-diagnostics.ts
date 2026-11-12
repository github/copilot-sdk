/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: lifecycle-diagnostics.schema.json
 */

/**
 * Union of experimental lifecycle diagnostic event variants emitted by the Copilot CLI runtime.
 */
export type LifecycleDiagnosticEvent =
  | RuntimeLifecycleSystemStateDiagnosticEvent
  | RuntimeLifecycleProgressStateDiagnosticEvent;
/**
 * How a settled lifecycle operation or progress record ended.
 */
export type RuntimeLifecycleDisposition =
  | "succeeded"
  | "failed"
  | "cancelled"
  | "timed_out"
  | "superseded"
  | "abandoned";

/**
 * Lifecycle diagnostic event "runtime.lifecycle.system_state". Experimental, ephemeral, observational-only state of one runtime system after an operation transition. Observation and delivery never control runtime work. Process death, diagnostic projection loss, transport loss, or serialization failure can prevent external delivery.
 */
export interface RuntimeLifecycleSystemStateDiagnosticEvent {
  data: RuntimeLifecycleSystemStateData;
  /**
   * ISO 8601 timestamp when the lifecycle diagnostic event was projected.
   */
  timestamp: string;
  /**
   * Type discriminator. Always "runtime.lifecycle.system_state".
   */
  type: "runtime.lifecycle.system_state";
}
/**
 * Experimental, observational-only state of one runtime system after an operation transition. Observation and delivery never control runtime work. Process death, diagnostic projection loss, transport loss, or serialization failure can prevent external delivery.
 */
export interface RuntimeLifecycleSystemStateData {
  /**
   * Complete active operation set after applying the change.
   */
  activeOperations: RuntimeLifecycleOperation[];
  changedOperation: RuntimeLifecycleOperation;
  /**
   * Authoritative condition retained after this change. Initial shared values are `current`, `disabled`, `degraded`, and `quiescent`; integrations can add bounded values as the contract matures.
   */
  condition?: string;
  /**
   * Opaque identity for one session registry generation.
   */
  incarnationId: string;
  /**
   * Session whose lifecycle owns this system.
   */
  sessionId: string;
  /**
   * Stable subsystem identifier.
   */
  system: string;
  /**
   * Orders every observed change to this system in the incarnation.
   */
  systemRevision: number;
}
/**
 * Observed state of one readiness or convergence operation.
 */
export interface RuntimeLifecycleOperation {
  disposition?: RuntimeLifecycleDisposition;
  /**
   * Opaque operation identifier, unique within the lifecycle incarnation and system.
   */
  operationId: string;
  /**
   * Orders transitions of this operation.
   */
  operationRevision: number;
  /**
   * System-owned lifecycle stage.
   */
  phase: string;
  /**
   * System-owned production boundary within the phase.
   */
  step: string;
  timing: RuntimeLifecycleOperationTiming;
  /**
   * System-owned reason the operation started.
   */
  trigger: string;
}
/**
 * Fixed timestamps for one observed operation revision.
 */
export interface RuntimeLifecycleOperationTiming {
  /**
   * ISO 8601 timestamp when the current phase began.
   */
  phaseStartedAt: string;
  /**
   * ISO 8601 timestamp when the operation began.
   */
  startedAt: string;
  /**
   * ISO 8601 timestamp when this operation revision was recorded.
   */
  transitionedAt: string;
}
/**
 * Lifecycle diagnostic event "runtime.lifecycle.progress_state". Experimental, ephemeral, observational-only state of runtime progress and the lifecycle operations that block it. Observation and delivery never control runtime or model behavior. Process death, diagnostic projection loss, transport loss, or serialization failure can prevent external delivery.
 */
export interface RuntimeLifecycleProgressStateDiagnosticEvent {
  data: RuntimeLifecycleProgressStateData;
  /**
   * ISO 8601 timestamp when the lifecycle diagnostic event was projected.
   */
  timestamp: string;
  /**
   * Type discriminator. Always "runtime.lifecycle.progress_state".
   */
  type: "runtime.lifecycle.progress_state";
}
/**
 * Experimental, observational-only state of runtime progress and the lifecycle operations that block it. Observation and delivery never control runtime or model behavior. Process death, diagnostic projection loss, transport loss, or serialization failure can prevent external delivery.
 */
export interface RuntimeLifecycleProgressStateData {
  /**
   * Complete dependency blocker set after this progress change.
   */
  blockers: RuntimeLifecycleBlocker[];
  disposition?: RuntimeLifecycleDisposition;
  /**
   * Opaque identity for one session registry generation.
   */
  incarnationId: string;
  /**
   * Progress-owner-defined kind.
   */
  kind: string;
  origin?: RuntimeLifecycleProgressOrigin;
  /**
   * Progress-owner-defined phase.
   */
  phase: string;
  /**
   * Opaque progress identifier, unique within the incarnation and kind.
   */
  progressId: string;
  /**
   * Orders changes to this progress record.
   */
  revision: number;
  /**
   * Session whose lifecycle owns this progress.
   */
  sessionId: string;
  timing: RuntimeLifecycleProgressTiming;
}
/**
 * Exact lifecycle operation awaited by a real dependency wait.
 */
export interface RuntimeLifecycleBlocker {
  /**
   * Current phase of the awaited operation.
   */
  currentPhase: string;
  /**
   * Current production boundary of the awaited operation.
   */
  currentStep: string;
  operationDisposition?: RuntimeLifecycleDisposition;
  /**
   * Opaque awaited operation identifier.
   */
  operationId: string;
  /**
   * Awaited operation revision observed by this blocker.
   */
  operationRevision: number;
  /**
   * System-owned capability required by the progress owner.
   */
  requirement: string;
  /**
   * System that owns the awaited operation.
   */
  system: string;
  timing: RuntimeLifecycleBlockerTiming;
}
/**
 * Timing of one real dependency wait.
 */
export interface RuntimeLifecycleBlockerTiming {
  /**
   * ISO 8601 timestamp when this dependency began blocking progress.
   */
  blockingSince: string;
}
/**
 * Immutable causal origin of a progress record with independent identity and settlement.
 */
export interface RuntimeLifecycleProgressOrigin {
  /**
   * Kind of the originating progress record.
   */
  kind: string;
  /**
   * Identifier of the originating progress record.
   */
  progressId: string;
}
/**
 * Fixed timestamps for one observed progress revision.
 */
export interface RuntimeLifecycleProgressTiming {
  /**
   * ISO 8601 timestamp when the current phase began.
   */
  phaseStartedAt: string;
  /**
   * ISO 8601 timestamp when the progress record began.
   */
  startedAt: string;
  /**
   * ISO 8601 timestamp when this progress revision was recorded.
   */
  transitionedAt: string;
}
