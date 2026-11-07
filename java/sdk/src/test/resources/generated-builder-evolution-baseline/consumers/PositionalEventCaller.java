package fixture;

import com.github.copilot.generated.SampleUpdatedEvent;
import java.util.List;

public final class PositionalEventCaller {
    public static void main(String[] args) {
        SampleUpdatedEvent.SampleUpdatedEventData data =
                new SampleUpdatedEvent.SampleUpdatedEventData("baseline-message", 7L, true, List.of("tag"), "note");
        if (!"baseline-message".equals(data.message()) || data.sequence() != 7L || !data.active()
                || !List.of("tag").equals(data.tags()) || !"note".equals(data.note())) {
            throw new AssertionError(data);
        }
        System.out.println(data.message() + "|" + data.sequence() + "|" + data.active() + "|" + data.tags() + "|"
                + data.note());
    }
}
