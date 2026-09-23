import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import {
    assertExactProductionDependencies,
    assertMinimumPublicationAge,
    loadNpmPublicationTimes,
    readProductionDependencyManifest,
    readResolvedProductionDependencies,
    requiresPublicationCooldown,
} from "./dependency-policy.js";

async function main(): Promise<void> {
    const nodeRoot = join(dirname(fileURLToPath(import.meta.url)), "..");
    const manifest = readProductionDependencyManifest(join(nodeRoot, "package.json"));
    const directDependencies = assertExactProductionDependencies(manifest);
    const resolvedDependencies = readResolvedProductionDependencies(
        join(nodeRoot, "package-lock.json")
    );
    const publicationTimes = await loadNpmPublicationTimes(resolvedDependencies);
    assertMinimumPublicationAge(resolvedDependencies, publicationTimes);
    const externalCount = resolvedDependencies.filter((dependency) =>
        requiresPublicationCooldown(dependency.name)
    ).length;
    console.log(
        `Verified ${directDependencies.length} exact direct production dependencies; ${externalCount} resolved production package versions meet the seven-day npm publication age requirement.`
    );
}

main().catch((error) => {
    console.error(`::error::${error instanceof Error ? error.message : String(error)}`);
    process.exitCode = 1;
});
