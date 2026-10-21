/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { CopilotClient } from "../src/index.js";
import type { ModelsListRequest } from "../src/generated/rpc.js";

/** Pass the same account selection to both catalog operations. */
export async function formatModelTokenPrices(
    client: CopilotClient,
    selection: ModelsListRequest
): Promise<string> {
    const { models } = await client.rpc.models.list(selection);
    const { units } = await client.rpc.models.listTokenPriceUnits(selection);
    const unitsByModel = new Map(units.map(({ modelId, unit }) => [modelId, unit]));
    const prices = models.flatMap((model) => {
        const unit = unitsByModel.get(model.id);
        if (!model.billing?.tokenPrices || !unit) {
            return [];
        }
        return [{ modelId: model.id, unit, prices: model.billing.tokenPrices }];
    });
    return JSON.stringify(prices, null, 2);
}
