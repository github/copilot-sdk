package fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.CompatibilityCallParams;

public final class BuilderRpcAddedPropertyCaller {
    public static void main(String[] args) throws Exception {
        var mapper = new ObjectMapper();
        var set = CompatibilityCallParams.builder()
                .requestId("request-42")
                .insertedOptional("added")
                .build();
        var unset = CompatibilityCallParams.builder().requestId("request-42").build();
        System.out.println(mapper.writeValueAsString(set));
        System.out.println(mapper.writeValueAsString(unset));
    }
}
