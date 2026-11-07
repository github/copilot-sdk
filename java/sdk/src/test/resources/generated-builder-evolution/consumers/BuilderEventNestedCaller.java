package fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.SampleNestedBuilderEvent;
import com.github.copilot.generated.SampleNestedBuilderEvent.SampleNestedBuilderEventData;
import com.github.copilot.generated.SampleNestedBuilderEvent.SampleNestedBuilderEventData.SampleNestedBuilderEventDataItemsItem;
import com.github.copilot.generated.SampleNestedBuilderEvent.SampleNestedBuilderEventData.SampleNestedBuilderEventDataLookupValue;
import com.github.copilot.generated.SampleNestedBuilderEvent.SampleNestedBuilderEventData.SampleNestedBuilderEventDataProfile;
import com.github.copilot.generated.SampleNestedBuilderEvent.SampleNestedBuilderEventData.SampleNestedBuilderEventDataProfile.SampleNestedBuilderEventDataProfileChild;
import java.util.List;
import java.util.Map;

public final class BuilderEventNestedCaller {
    public static void main(String[] args) throws Exception {
        var child = SampleNestedBuilderEventDataProfileChild.builder().value("leaf").build();
        var profile = SampleNestedBuilderEventDataProfile.builder().name("Ada").child(child).build();
        var item = SampleNestedBuilderEventDataItemsItem.builder().id("entry").build();
        var lookupValue = SampleNestedBuilderEventDataLookupValue.builder().count(3L).build();
        var data = SampleNestedBuilderEventData.builder()
                .profile(profile)
                .items(List.of(item))
                .lookup(Map.of("key", lookupValue))
                .build();
        System.out.println(new ObjectMapper().writeValueAsString(data));
    }
}
