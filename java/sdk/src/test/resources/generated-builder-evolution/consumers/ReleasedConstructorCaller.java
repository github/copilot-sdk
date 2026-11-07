/* Copyright (c) Microsoft Corporation. All rights reserved. */
package fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.CommandQueuedEvent.CommandQueuedEventData;
import com.github.copilot.generated.rpc.MarketplaceInfo;
import com.github.copilot.generated.rpc.SandboxConfigUserPolicyFilesystem;
import com.github.copilot.generated.rpc.LocalSessionMetadataValue;
import com.github.copilot.generated.rpc.SessionAccountsSetParams;
import com.github.copilot.generated.rpc.SkillsInvokedSkill;
import com.github.copilot.generated.rpc.TaskBlocker;
import com.github.copilot.generated.rpc.TaskBlockerKind;
import com.github.copilot.generated.rpc.PermissionRecoveryReason;
import java.util.List;
import java.util.Map;

public final class ReleasedConstructorCaller {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var skill = new SkillsInvokedSkill("example", "/example", "content", List.of("tool"), false, 5L);
        var blocker = new TaskBlocker(TaskBlockerKind.PERMISSION_RECOVERY,
                PermissionRecoveryReason.PERMISSION_REQUIRED, true, null);
        var filesystem = new SandboxConfigUserPolicyFilesystem(List.of("/write"), List.of("/read"),
                List.of("/deny"), true);
        var metadata = new LocalSessionMetadataValue("session", "start", "modified", "summary", "name",
                "client", false, true, null, "task");
        var account = new SessionAccountsSetParams("session", Map.of("kind", "switchActive", "selectionId", "selected"));
        System.out.println(mapper.writeValueAsString(skill));
        System.out.println(mapper.writeValueAsString(blocker));
        System.out.println(mapper.writeValueAsString(filesystem));
        System.out.println(mapper.writeValueAsString(metadata));
        System.out.println(mapper.writeValueAsString(account));
        System.out.println(mapper.writeValueAsString(new CommandQueuedEventData("legacy-request", "/help")));
        System.out.println(mapper.writeValueAsString(new MarketplaceInfo("market", "source", false, true, false)));
    }
}
