/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/** Attempt every cleanup without hiding the test-body failure or accepting failed teardown. */
export async function withTestCleanup<T>(
    run: () => Promise<T>,
    ...cleanups: Array<() => void | Promise<void>>
): Promise<T> {
    const outcome = await Promise.resolve()
        .then(run)
        .then(
            (value) => ({ value }),
            (error: unknown) => ({ error })
        );
    const cleanupErrors: unknown[] = [];
    for (const cleanup of cleanups) {
        try {
            await cleanup();
        } catch (error) {
            cleanupErrors.push(error);
        }
    }
    if ("error" in outcome) {
        if (cleanupErrors.length > 0) {
            throw new AggregateError(
                [outcome.error, ...cleanupErrors],
                "Test body and cleanup failed",
                {
                    cause: outcome.error,
                }
            );
        }
        throw outcome.error;
    }
    if (cleanupErrors.length === 1) {
        throw cleanupErrors[0];
    }
    if (cleanupErrors.length > 1) {
        throw new AggregateError(cleanupErrors, "Test cleanup failed");
    }
    return outcome.value;
}
