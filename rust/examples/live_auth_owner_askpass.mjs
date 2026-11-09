#!/usr/bin/env node
/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFile } from "node:fs/promises";

process.stdout.write(await readFile(process.env.COPILOT_AUTH_OWNER_TOKEN_FILE, "utf8"));
