// Reuse the Node E2E's candidate attestation and real runtime materializer.
import { candidateHostArtifacts } from "../../../nodejs/test/e2e/harness/runtimeHostCandidate.js";

console.log(JSON.stringify(candidateHostArtifacts(process.argv[2])));
