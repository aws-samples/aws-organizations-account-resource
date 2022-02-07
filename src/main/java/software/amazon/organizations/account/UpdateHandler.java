package software.amazon.organizations.account;

import software.amazon.awssdk.services.account.AccountClient;
import software.amazon.awssdk.services.organizations.OrganizationsClient;
import software.amazon.cloudformation.proxy.*;
import software.amazon.organizations.account.util.ClientBuilder;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OrganizationsClient> proxyClient,
            final Logger logger, TypeConfigurationModel typeConfiguration) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        AmazonWebServicesClientProxy _proxy = typeConfiguration != null && typeConfiguration.getRoleArn() != null ? retrieveCrossAccountProxy(
                proxy,
                (LoggerProxy) logger,
                typeConfiguration.getRoleArn()
        ) : proxy;
        ProxyClient<OrganizationsClient> _proxyClient = _proxy.newProxy(ClientBuilder::getClient);
        ProxyClient<AccountClient> _proxyAccountClient = _proxy.newProxy(ClientBuilder::getAccountClient);
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> describeAccount(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext))
                .then(progress -> getParentId(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext))
                .then(progress -> moveAccount(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext, false))
                .then(progress -> tagAccount(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext))
                .then(progress -> model.getAlternateContacts() != null && model.getAlternateContacts().getBilling() != null ?
                        putAlternateContact(_proxy, _proxyAccountClient, progress, progress.getResourceModel(), "BILLING", logger, callbackContext) :
                        deleteAlternateContact(_proxy, _proxyAccountClient, progress, progress.getResourceModel(), "BILLING", logger, callbackContext)
                )
                .then(progress -> model.getAlternateContacts() != null && model.getAlternateContacts().getOperations() != null ?
                        putAlternateContact(_proxy, _proxyAccountClient, progress, progress.getResourceModel(), "OPERATIONS", logger, callbackContext) :
                        deleteAlternateContact(_proxy, _proxyAccountClient, progress, progress.getResourceModel(), "OPERATIONS", logger, callbackContext)
                )
                .then(progress -> model.getAlternateContacts() != null && model.getAlternateContacts().getSecurity() != null ?
                        putAlternateContact(_proxy, _proxyAccountClient, progress, progress.getResourceModel(), "SECURITY", logger, callbackContext) :
                        deleteAlternateContact(_proxy, _proxyAccountClient, progress, progress.getResourceModel(), "SECURITY", logger, callbackContext)
                )
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }
}
