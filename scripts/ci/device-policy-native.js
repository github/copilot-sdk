/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import("./device-policy-fixture.mjs")
    .then(({ launchRuntime }) => launchRuntime("native"))
    .catch((error) => {
        console.error(error.message);
        process.exitCode = 1;
    });
