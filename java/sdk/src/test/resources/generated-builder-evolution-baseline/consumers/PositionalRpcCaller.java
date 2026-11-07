package fixture;

import com.github.copilot.generated.rpc.CompatibilityCallParams;

public final class PositionalRpcCaller {
    public static void main(String[] args) {
        CompatibilityCallParams params = new CompatibilityCallParams("request-42", "baseline-note");
        if (!"request-42".equals(params.requestId()) || !"baseline-note".equals(params.note())) {
            throw new AssertionError(params);
        }
        System.out.println(params.requestId() + "|" + params.note());
    }
}
