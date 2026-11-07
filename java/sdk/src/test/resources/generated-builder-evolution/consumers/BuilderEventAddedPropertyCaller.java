package fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.SampleUpdatedEvent;
import java.util.List;

public final class BuilderEventAddedPropertyCaller {
    public static void main(String[] args) throws Exception {
        var mapper = new ObjectMapper();
        var set = SampleUpdatedEvent.SampleUpdatedEventData.builder()
                .message("baseline-message")
                .insertedOptional("added")
                .sequence(7L)
                .active(true)
                .tags(List.of("tag"))
                .build();
        var unset = SampleUpdatedEvent.SampleUpdatedEventData.builder()
                .message("baseline-message")
                .sequence(7L)
                .active(true)
                .tags(List.of("tag"))
                .build();
        System.out.println(mapper.writeValueAsString(set));
        System.out.println(mapper.writeValueAsString(unset));
    }
}
