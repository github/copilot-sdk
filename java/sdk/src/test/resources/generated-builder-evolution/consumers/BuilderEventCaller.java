package fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.SampleUpdatedEvent;
import java.util.List;

public final class BuilderEventCaller {
    public static void main(String[] args) throws Exception {
        try {
            var data = SampleUpdatedEvent.SampleUpdatedEventData.builder()
                    .message("baseline-message")
                    .sequence(7L)
                    .active(true)
                    .tags(List.of("tag"))
                    .build();
            System.out.println(new ObjectMapper().writeValueAsString(data));
        } catch (IllegalStateException error) {
            System.out.println(error.getMessage());
        }
    }
}
