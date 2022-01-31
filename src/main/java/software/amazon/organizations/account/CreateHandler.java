package software.amazon.organizations.account;


import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.services.organizations.OrganizationsClient;
import software.amazon.awssdk.services.organizations.model.ConcurrentModificationException;
import software.amazon.awssdk.services.organizations.model.CreateAccountFailureReason;
import software.amazon.awssdk.services.organizations.model.CreateAccountResponse;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.organizations.account.translator.Translator;
import software.amazon.organizations.account.util.ClientBuilder;

import java.util.concurrent.TimeUnit;


public class CreateHandler extends BaseHandlerStd {
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
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> findAccount(_proxy, proxyClient, progress, progress.getResourceModel(), logger))
                .then(progress ->
                        _proxy
                            .initiate("ProServe-Organizations-Account::CreateAccount", _proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                            .translateToServiceRequest(Translator::createCreateAccountRequest)
                            .makeServiceCall((modelRequest, proxyInvocation) -> {
                                CreateAccountResponse response = null;
                                while (true) {
                                    try {
                                        response = proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::createAccount);
                                        break;
                                    } catch (ConcurrentModificationException e) {
                                        logger.log(String.format("Retry: %s", e.getMessage()));
                                        try {
                                            TimeUnit.SECONDS.sleep(10);
                                        } catch (InterruptedException ex) {
                                            logger.log("Thread interrupted.");
                                            ex.printStackTrace();
                                        }
                                    }
                                }
                                model.setAccountId(response.createAccountStatus().accountId());
                                model.setAccountRequestId(response.createAccountStatus().id());
                                return response;
                            })
                            .stabilize((_request, response, proxyInvocation, resourceModel, context) ->
                            {
                                try {
                                    return isCreateAccountOperationStabilized(proxyInvocation, response.createAccountStatus().id(), logger, resourceModel, context);
                                } catch (InterruptedException e) {
                                    return false;
                                }
                            })
                            .handleError((_request, e, _client, _model, context) -> {
                                if (e instanceof TerminalException) {
                                        if (context.getCreateAccountFailureReason() != null && context.getCreateAccountFailureReason() == CreateAccountFailureReason.EMAIL_ALREADY_EXISTS) {
                                            return ProgressEvent.failed(_model, context, HandlerErrorCode.AlreadyExists, e.getMessage());
                                        } else {
                                            return ProgressEvent.failed(_model, context, HandlerErrorCode.GeneralServiceException, e.getMessage());
                                        }
                                    }
                                if (e instanceof ConcurrentModificationException) {
                                    throw RetryableException.builder().message(e.getMessage()).build();
                                }
                                throw e;
                            })
                            .progress()
                )
                .then(progress -> describeAccount(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext))
                .then(progress -> getParentId(_proxy, _proxyClient, progress, progress.getResourceModel(), logger, callbackContext))
                .then(progress -> moveAccount(_proxy, _proxyClient, progress, progress.getResourceModel(), logger,callbackContext, false))
                .then(progress ->
                        model.getDeploymentAccountConfiguration() != null ?
                                createDeploymentAccountRole(_proxy, _proxyClient, progress, progress.getResourceModel(), logger) :
                                ProgressEvent.defaultInProgressHandler(progress.getCallbackContext(), 0, progress.getResourceModel())
                )
                .then(progress -> {
                    CallbackContext ctx = progress.getCallbackContext();
                    int creationCompleteBackoff = ctx.getCreationCompleteBackoff();
                    ctx.setCreationCompleteBackoff(0);
                    return ProgressEvent.defaultInProgressHandler(ctx, creationCompleteBackoff, progress.getResourceModel());
                })
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }
}
