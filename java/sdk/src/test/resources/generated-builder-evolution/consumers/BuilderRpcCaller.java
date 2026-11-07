package fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.CompatibilityCallParams;

public final class BuilderRpcCaller {
    public static void main(String[] args) throws Exception {
        try {
            var params = CompatibilityCallParams.builder().requestId("request-42").build();
            System.out.println(new ObjectMapper().writeValueAsString(params));
        } catch (IllegalStateException error) {
            System.out.println(error.getMessage());
        }
    }
}
