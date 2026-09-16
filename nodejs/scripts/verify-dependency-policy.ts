import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import {
    assertExactProductionDependencies,
    assertMinimumPublicationAge,
    loadNpmPublicationTimes,
    readProductionDependencyManifest,
    requiresPublicationCooldown,
} from "./dependency-policy.js";

async function main(): Promise<void> {
    const nodeRoot = join(dirname(fileURLToPath(import.meta.url)), "..");
    const manifest = readProductionDependencyManifest(join(nodeRoot, "package.json"));
    const dependencies = assertExactProductionDependencies(manifest);
    const publicationTimes = await loadNpmPublicationTimes(dependencies);
    assertMinimumPublicationAge(dependencies, publicationTimes);
    const externalCount = dependencies.filter((dependency) =>
        requiresPublicationCooldown(dependency.name)
    ).length;
    console.log(
        `Verified ${dependencies.length} exact production dependencies; ${externalCount} meet the seven-day npm publication age requirement.`
    );
}

main().catch((error) => {
    console.error(`::error::${error instanceof Error ? error.message : String(error)}`);
    process.exitCode = 1;
});
