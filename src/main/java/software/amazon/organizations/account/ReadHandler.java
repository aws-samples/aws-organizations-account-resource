package software.amazon.organizations.account;

import software.amazon.awssdk.services.organizations.OrganizationsClient;
import software.amazon.cloudformation.proxy.*;
import software.amazon.organizations.account.util.ClientBuilder;

public class ReadHandler extends BaseHandlerStd {
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OrganizationsClient> proxyClient,
            final Logger logger, TypeConfigurationModel typeConfiguration) {
        AmazonWebServicesClientProxy _proxy = typeConfiguration != null && typeConfiguration.getRoleArn() != null ? retrieveCrossAccountProxy(
                proxy,
                (LoggerProxy) logger,
                typeConfiguration.getRoleArn()
        ) : proxy;
        ProxyClient<OrganizationsClient> _proxyClient = _proxy.newProxy(ClientBuilder::getClient);
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> describeAccount(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext))
                .then(progress -> listAccountTags(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext))
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }
}
