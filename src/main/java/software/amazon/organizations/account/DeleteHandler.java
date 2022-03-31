package software.amazon.organizations.account;

import software.amazon.awssdk.services.organizations.OrganizationsClient;
import software.amazon.cloudformation.proxy.*;
import software.amazon.organizations.account.util.ClientBuilder;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OrganizationsClient> proxyClient,
            final Logger logger, TypeConfigurationModel typeConfiguration) {

        this.logger = logger;
        AmazonWebServicesClientProxy _proxy = typeConfiguration != null && typeConfiguration.getRoleArn() != null ? retrieveCrossAccountProxy(
                proxy,
                (LoggerProxy) logger,
                typeConfiguration.getRoleArn()
        ) : proxy;
        ProxyClient<OrganizationsClient> _proxyClient = _proxy.newProxy(ClientBuilder::getClient);
        callbackContext.setRequestType("DELETE");
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> describeAccount(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext))
                .then(progress -> getParentId(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext))
                .then(progress -> getRootId(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext))
                .then(progress -> moveAccount(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext, true))
                .then(progress -> progress.getResourceModel().getCloseAccountOnDeletion() != null && progress.getResourceModel().getCloseAccountOnDeletion() ? closeAccount(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext) : progress)
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }
}
