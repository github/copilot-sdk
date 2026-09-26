# Copyright (c) Microsoft Corporation. All rights reserved.

"""Optional diagnostic decoding; consumers still validate enclosing self identity."""

import json
import logging
import re
import unicodedata
from collections.abc import Callable
from typing import Any, TypeVar

T = TypeVar("T")
_LOG = logging.getLogger(__name__)
_UUID = re.compile(r"[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}")


def _uuid(value: Any) -> bool:
    return isinstance(value, str) and _UUID.fullmatch(value) is not None


def _text(value: Any) -> bool:
    return (
        isinstance(value, str)
        and bool(value)
        and len(value.encode("utf-8")) <= 256
        and all(unicodedata.category(char) != "Cc" for char in value)
    )


def _fields(value: dict[str, Any], allowed: set[str]) -> bool:
    return value.keys() <= allowed


def _reference(value: Any, event_type: str) -> bool:
    return (
        isinstance(value, dict)
        and _fields(value, {"sessionId", "eventId", "agentId", "eventType", "provenance"})
        and _text(value.get("sessionId"))
        and _uuid(value.get("eventId"))
        and ("agentId" not in value or _text(value["agentId"]))
        and value.get("eventType") == event_type
        and value.get("provenance") in ("native", "ahp_coordinator")
    )


def _source(value: Any) -> bool:
    if not isinstance(value, dict) or not isinstance(value.get("input"), dict):
        return False
    source_input = value["input"]
    if (
        not _fields(
            value,
            {
                "input",
                "admissions",
                "captureComplete",
                "completion",
                "notification",
                "admittedDuring",
            },
        )
        or not _fields(source_input, {"queueItemId", "agentId", "sender", "senderBridges"})
        or type(value.get("captureComplete")) is not bool
        or not _uuid(source_input.get("queueItemId"))
        or not _text(source_input.get("agentId"))
        or not isinstance(value.get("admissions"), list)
        or len(value["admissions"]) > 32
    ):
        return False
    if "sender" in source_input and not _reference(source_input["sender"], "tool.execution_start"):
        return False
    if "senderBridges" in source_input:
        edges = source_input["senderBridges"]
        if "sender" not in source_input or not isinstance(edges, list) or len(edges) > 32:
            return False
        if any(
            not isinstance(edge, dict)
            or not _fields(edge, {"source", "reported"})
            or not _reference(edge.get("source"), "tool.execution_start")
            or not _reference(edge.get("reported"), "tool.execution_start")
            for edge in edges
        ):
            return False
    for admission in value["admissions"]:
        if (
            not isinstance(admission, dict)
            or not _fields(admission, {"kind", "messageId", "event", "ahpTurnId"})
            or admission.get("kind") not in ("queued_input", "system_continuation")
            or not _text(admission.get("messageId"))
            or ("ahpTurnId" in admission and not _uuid(admission["ahpTurnId"]))
            or (
                "event" in admission
                and (
                    not _reference(admission["event"], "user.message")
                    or admission["event"].get("agentId") != source_input["agentId"]
                )
            )
        ):
            return False
    for field, event_type in [
        ("completion", "subagent.completed"),
        ("admittedDuring", "assistant.turn_start"),
    ]:
        if field in value and not _reference(value[field], event_type):
            return False
    if "notification" in value:
        notification = value["notification"]
        if (
            not isinstance(notification, dict)
            or not _fields(notification, {"deliveryId", "event", "mode"})
            or not _uuid(notification.get("deliveryId"))
            or notification.get("mode") not in ("queued", "immediate")
            or (
                "event" in notification
                and not _reference(notification["event"], "system.notification")
            )
        ):
            return False
    return True


def _supported(value: Any) -> bool:
    return (
        isinstance(value, dict)
        and _fields(value, {"version", "observationProvenance", "sources", "captureComplete"})
        and type(value.get("version")) is int
        and value["version"] == 1
        and value.get("observationProvenance") in ("native", "ahp_coordinator")
        and type(value.get("captureComplete")) is bool
        and isinstance(value.get("sources"), list)
        and len(value["sources"]) <= 32
        and all(
            _source(source) and (not value["captureComplete"] or source["captureComplete"])
            for source in value["sources"]
        )
    )


def _small(value: Any, remaining: list[int], depth: int = 0) -> bool:
    remaining[0] -= 1
    if remaining[0] < 0 or depth > 32:
        return False
    if isinstance(value, str):
        remaining[0] -= len(value)
    elif isinstance(value, dict):
        for key, item in value.items():
            if not _small(key, remaining, depth + 1) or not _small(item, remaining, depth + 1):
                return False
    elif isinstance(value, list):
        for item in value:
            if not _small(item, remaining, depth + 1):
                return False
    return remaining[0] >= 0


def optional_worker_causality(value: Any, load: Callable[[], T]) -> T | None:
    if value is None:
        return None
    try:
        if not _small(value, [4096]) or not _supported(value):
            raise ValueError("invalid optional worker metadata")
        size = 0
        encoder = json.JSONEncoder(ensure_ascii=False, separators=(",", ":"))
        for chunk in encoder.iterencode({"workerCausality": value}):
            size += len(chunk.encode("utf-8"))
            if size > 4096:
                raise ValueError("optional worker metadata budget")
        return load()
    except (AssertionError, AttributeError, KeyError, TypeError, ValueError, UnicodeError):
        _LOG.warning("Ignoring invalid, unsupported or oversized workerCausality metadata")
        return None
