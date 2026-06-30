from copilot.generated import rpc


def test_python_codegen_does_not_export_quicktype_synthetic_permission_approval_name():
    assert not hasattr(rpc, "PermissionDecisionApproveForIonApproval")
    assert "PermissionDecisionApproveForIonApproval" not in rpc.__all__

    assert hasattr(rpc, "PermissionDecisionApproveForSessionApproval")
    assert hasattr(rpc, "PermissionDecisionApproveForLocationApproval")
    assert "PermissionDecisionApproveForSessionApproval" in rpc.__all__
    assert "PermissionDecisionApproveForLocationApproval" in rpc.__all__
