package fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.ValidationSubmitParams;
import java.util.List;

public final class BuilderValidationCaller {
    public static void main(String[] args) throws Exception {
        try {
            ValidationSubmitParams.builder().build();
            throw new AssertionError("Missing required properties were accepted");
        } catch (IllegalStateException error) {
            if (!"Missing required property: enabled".equals(error.getMessage())) {
                throw error;
            }
        }

        try {
            ValidationSubmitParams.builder()
                    .enabled(false)
                    .count(0L)
                    .emptyString("")
                    .tags(List.of())
                    .build();
            throw new AssertionError("Unset required-nullable property was accepted");
        } catch (IllegalStateException error) {
            if (!"Missing required property: requiredNullable".equals(error.getMessage())) {
                throw error;
            }
        }

        var value = ValidationSubmitParams.builder()
                .enabled(false)
                .count(0L)
                .emptyString("")
                .tags(List.of())
                .requiredNullable(null)
                .build();
        if (value.requiredNullable() != null || value.optionalDefault() != null) {
            throw new AssertionError("Explicit null or omitted default changed the record value");
        }
        System.out.println(new ObjectMapper().writeValueAsString(value));
    }
}
