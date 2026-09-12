import assert from "node:assert/strict";
import test from "node:test";
import type { JSONSchema7 } from "json-schema";

import { generateApiTypesCode } from "./rust.js";

test("names an explicit unknown wire value separately from the catch-all", () => {
  const schema = {
    definitions: {
      CatalogTrustEligibility: {
        type: "string",
        enum: ["default", "expanded", "hidden", "unknown"],
      },
    },
  } satisfies JSONSchema7;

  const generated = generateApiTypesCode(schema);

  assert.match(
    generated,
    /    #\[serde\(rename = "unknown"\)\]\n    UnknownValue,\n    \/\/\/ Unknown variant for forward compatibility\.\n    #\[default\]\n    #\[serde\(other\)\]\n    Unknown,/,
  );
});
