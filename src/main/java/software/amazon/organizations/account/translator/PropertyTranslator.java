package software.amazon.organizations.account.translator;

import com.amazonaws.util.CollectionUtils;
import software.amazon.awssdk.services.organizations.model.Tag;

import java.util.*;
import java.util.stream.Collectors;

public class PropertyTranslator {
    /**
     * Converts tags (from CFN resource model) to Organization set (from Organization SDK)
     *
     * @param tags Tags CFN resource model.
     * @return SDK Tags.
     */
    public static Set<Tag> translateToSdkTags(final List<software.amazon.organizations.account.Tag> tags) {
        if (tags == null) {
            return Collections.emptySet();
        }
        return Optional.of(tags).orElse(Collections.emptyList())
                .stream()
                .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }

    /**
     * Converts a list of tags (from Organization SDK) to Tag set (from CFN resource model)
     *
     * @param tags Tags from SC SDK.
     * @return A set of CFN Tag.
     */
    public static List<software.amazon.organizations.account.Tag> translateFromSdkTags(final List<Tag> tags) {
        if (CollectionUtils.isNullOrEmpty(tags)) return null;
        return tags.stream().map(tag -> software.amazon.organizations.account.Tag.builder()
                        .key(tag.key())
                        .value(tag.value())
                        .build())
                .collect(Collectors.toList());
    }
}
