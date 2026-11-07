package fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.SampleValidationEvent;

public final class BuilderEventValidationCaller {
    public static void main(String[] args) throws Exception {
        try {
            SampleValidationEvent.SampleValidationEventData.builder().build();
        } catch (IllegalStateException error) {
            System.out.println(error.getMessage());
        }
        var withNull = SampleValidationEvent.SampleValidationEventData.builder()
                .message("present")
                .requiredNullable(null)
                .build();
        System.out.println(new ObjectMapper().writeValueAsString(withNull));
    }
}
