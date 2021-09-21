package software.amazon.organizations.account;

import java.util.Map;
import java.util.stream.Collectors;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("proserve-organizations-account.json");
    }

    @Override
    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {
        return resourceModel.getTags() == null ?
                null :
                resourceModel.getTags().stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue));
    }
}
