import type { CopilotSession } from "@github/copilot-sdk";
import type { joinSession } from "@github/copilot-sdk/extension";
import type { session } from "./declarations/extension.mjs";

type Equal<A, B> =
    (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2 ? true : false;
type Assert<T extends true> = T;
export type ExactSession = Assert<Equal<typeof session, CopilotSession>>;
export type ExactReturn = Assert<Equal<typeof session, Awaited<ReturnType<typeof joinSession>>>>;
export type ExactPromise = Assert<Equal<ReturnType<typeof joinSession>, Promise<CopilotSession>>>;
