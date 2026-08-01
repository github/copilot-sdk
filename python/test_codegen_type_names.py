import re
import types

from copilot._generated import rpc


def test_permission_approval_exports_are_union_aliases():
    approval_exports = [
        name
        for name in rpc.__all__
        if re.fullmatch(r"PermissionDecisionApproveFor.*Approval", name)
    ]
    assert approval_exports

    for name in approval_exports:
        exported = getattr(rpc, name)
        assert isinstance(exported, types.UnionType), (
            f"{name} must be a union alias, not a synthetic dataclass"
        )


def test_permission_approval_union_loaders_deserialize_expected_variants():
    session = rpc._load_PermissionDecisionApproveForSessionApproval(
        {"kind": "commands", "commandIdentifiers": ["git status"]}
    )
    location = rpc._load_PermissionDecisionApproveForLocationApproval({"kind": "read"})

    assert isinstance(session, rpc.PermissionDecisionApproveForSessionApprovalCommands)
    assert isinstance(location, rpc.PermissionDecisionApproveForLocationApprovalRead)
