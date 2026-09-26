# Copyright (c) Microsoft Corporation. All rights reserved.

import copy
import json
from pathlib import Path

import pytest

from copilot.rpc import TasksSendMessageResult
from copilot.session_events import session_event_from_dict, session_event_to_dict

CORPUS = json.loads((Path(__file__).parent.parent / "test/worker-causality.json").read_text())


def decode(value, event):
    if event:
        return session_event_to_dict(session_event_from_dict(value))
    return TasksSendMessageResult.from_dict(value).to_dict()


def payload(value, event):
    return value["data"] if event else value


@pytest.mark.parametrize("case", CORPUS["valid"], ids=lambda case: case["name"])
def test_worker_causality_public_readers_preserve_source_and_product(case):
    event = "event" in case
    wire = case["event"] if event else case["result"]
    original = copy.deepcopy(wire)
    assert (
        payload(decode(wire, event), event)["workerCausality"]
        == payload(wire, event)["workerCausality"]
    )
    assert wire == original

    legacy = copy.deepcopy(wire)
    del payload(legacy, event)["workerCausality"]
    baseline = json.dumps(decode(legacy, event), sort_keys=True)
    for invalid in CORPUS["invalid"]:
        candidate = copy.deepcopy(legacy)
        payload(candidate, event)["workerCausality"] = invalid["value"]
        assert json.dumps(decode(candidate, event), sort_keys=True) == baseline, invalid["name"]


@pytest.mark.parametrize("case", CORPUS["boundaries"], ids=lambda case: case["name"])
def test_worker_causality_exact_utf8_budget_and_unknown_fields(case):
    wire = copy.deepcopy(CORPUS["valid"][0]["event"])
    wire["data"]["workerCausality"] = case["value"]
    result = decode(wire, True)
    assert ("workerCausality" in result["data"]) == case["accepted"]
    assert result["data"]["content"] == wire["data"]["content"]


def test_canonical_workflow_completion_decodes_typed_fields():
    event = session_event_from_dict(CORPUS["workflowCompleted"]["event"])
    assert event.type.value == "system.notification"
    assert event.data.kind.type == "workflow_completed"
    assert event.data.kind.workflow_name == "fix-ci"
    assert event.data.kind.run_id == "run-1"
    assert event.data.kind.status.value == "completed"
    assert event.data.kind.consumed_subagents == 1
