"""Internal diagnostics helpers shared by SDK modules."""

from __future__ import annotations

import logging
import time
from typing import Any


def elapsed_ms(start: float) -> float:
    return (time.perf_counter() - start) * 1000


def log_timing(
    logger: logging.Logger,
    level: int,
    message: str,
    start: float,
    *,
    exc_info: bool = False,
    **fields: Any,
) -> None:
    if logger.isEnabledFor(level):
        logger.log(
            level,
            message,
            extra={"elapsed_ms": elapsed_ms(start), **fields},
            exc_info=exc_info,
        )
