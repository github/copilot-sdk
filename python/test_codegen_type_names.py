import re
import types

from copilot import rpc as public_rpc
from copilot.generated import rpc


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


def test_login_account_enumeration_deserializes_concrete_identities():
    value = rpc._load_AuthEnumerateValue(
        {
            "kind": "loginAccounts",
            "items": [
                {
                    "accountId": "entra:opaque-account",
                    "active": True,
                    "label": "person@contoso.example",
                }
            ],
        }
    )

    assert isinstance(value, rpc.AuthEnumerateValueLoginAccounts)
    assert isinstance(value.items[0], rpc.AuthLoginIdentity)
    assert value.kind == rpc.AuthEnumerateQueryKind.LOGIN_ACCOUNTS
    assert value.items[0].account_id == "entra:opaque-account"


def test_catalog_candidate_provenance_serialises_every_catalog_media_type():
    assert not hasattr(rpc, "MediaType")
    for media_type in rpc.CatalogMediaType:
        provenance = rpc.CatalogCandidateProvenance(
            authority="agentfinder.github.com",
            media_type=media_type,
            observed_at="2026-10-13T00:00:00.000Z",
        )
        serialised = provenance.to_dict()
        assert serialised["mediaType"] == media_type.value
        assert rpc.CatalogCandidateProvenance.from_dict(serialised) == provenance


def test_image_availability_preserves_public_session_limit_discriminator():
    assert "SessionLimitPredictionResultKind" in rpc.__all__
    assert public_rpc.SessionLimitPredictionResultKind is rpc.SessionLimitPredictionResultKind
    assert public_rpc.SessionLimitPredictionResultKind is not public_rpc.ImageToolModelAvailability

    payload = {"kind": "unavailable", "reason": "no_model"}
    result = public_rpc.SessionLimitPredictionResult.from_dict(payload)
    assert result.kind is public_rpc.SessionLimitPredictionResultKind.UNAVAILABLE
    assert result.to_dict() == payload

    for value in ("available", "unavailable"):
        kind = public_rpc.SessionLimitPredictionResultKind(value)
        availability = public_rpc.ImageToolModelAvailability(value)
        assert kind is not availability
        assert kind.value == availability.value == value
        image_payload = {
            "availability": value,
            "id": "test-image",
            "isDefault": True,
            "name": "Test image",
            "supportedOperations": [],
        }
        image = public_rpc.ImageToolModel.from_dict(image_payload)
        assert image.availability is availability
        assert image.to_dict() == image_payload
