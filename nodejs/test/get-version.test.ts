import { describe, expect, it } from "vitest";
import { calculateVersion } from "../scripts/calculate-version.js";

describe("get-version", () => {
    it("increments stable latest versions by patch", () => {
        expect(calculateVersion("latest", { latest: "1.0.1" })).toBe("1.0.2");
    });

    it("promotes a higher prerelease to stable for latest releases", () => {
        expect(calculateVersion("latest", { latest: "0.3.0", prerelease: "1.0.0-beta.1" })).toBe(
            "1.0.0"
        );
    });

    it("starts preview prereleases when incrementing from a stable release", () => {
        expect(calculateVersion("prerelease", { latest: "0.3.0" })).toBe("0.3.1-preview.0");
    });

    it("preserves custom prerelease identifiers when incrementing prereleases", () => {
        expect(
            calculateVersion("prerelease", { latest: "0.3.0", prerelease: "0.4.0-chicken.2" })
        ).toBe("0.4.0-chicken.3");
    });

    it("preserves beta prerelease identifiers when incrementing prereleases", () => {
        expect(
            calculateVersion("prerelease", { latest: "0.3.0", prerelease: "1.0.0-beta.1" })
        ).toBe("1.0.0-beta.2");
    });

    it("increments unstable releases with the unstable identifier", () => {
        expect(
            calculateVersion("unstable", {
                latest: "0.3.0",
                prerelease: "0.4.0-chicken.2",
                unstable: "0.5.0-unstable.2",
            })
        ).toBe("0.5.0-unstable.3");
    });
});
