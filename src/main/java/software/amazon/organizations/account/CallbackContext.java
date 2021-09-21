package software.amazon.organizations.account;

import software.amazon.awssdk.services.organizations.model.Account;
import software.amazon.awssdk.services.organizations.model.CreateAccountFailureReason;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    Account account = null;
    String currentParent = null;
    String rootId = null;
    CreateAccountFailureReason createAccountFailureReason = null;
    String requestType = null;
    int creationCompleteBackoff = 30;
}
