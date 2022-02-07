package software.amazon.organizations.account;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.services.account.AccountClient;
import software.amazon.awssdk.services.account.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.CreateRoleResponse;
import software.amazon.awssdk.services.iam.model.EntityAlreadyExistsException;
import software.amazon.awssdk.services.organizations.OrganizationsClient;
import software.amazon.awssdk.services.organizations.model.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.StsException;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.cloudformation.proxy.*;
import software.amazon.cloudformation.proxy.delay.MultipleOf;
import software.amazon.organizations.account.translator.Translator;
import software.amazon.organizations.account.util.ClientBuilder;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static software.amazon.organizations.account.translator.PropertyTranslator.translateFromSdkTags;
import static software.amazon.organizations.account.translator.Translator.*;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext, TypeConfigurationModel> {

    protected static final MultipleOf MULTIPLE_OF = MultipleOf.multipleOf()
            .multiple(2)
            .timeout(Duration.ofHours(24L))
            .delay(Duration.ofSeconds(2L))
            .build();

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger,
            final TypeConfigurationModel typeConfiguration) {

        logger.log(request.getDesiredResourceState().toString());
        ProxyClient<SnsClient> snsClient = proxy.newProxy(ClientBuilder::getSnsClient);
        ProgressEvent<ResourceModel, CallbackContext> progress;
        try {
            progress = handleRequest(
                    proxy,
                    request,
                    callbackContext != null ? callbackContext : new CallbackContext(),
                    proxy.newProxy(ClientBuilder::getClient),
                    logger,
                    typeConfiguration
            );
        } catch (Exception e) {
            String modelAsText = request.getDesiredResourceState() != null ? request.getDesiredResourceState().toString() : "";
            String msg = String.format("[%s] - %s \n\n %s", e.getClass().toString(), e.getMessage(), modelAsText);
            publishNotification(proxy, request.getDesiredResourceState(), snsClient, msg);
            throw e;
        }
        if (progress.isFailed()) {
            String msg = String.format("[%s] - %s \n\n %s", progress.getErrorCode(), progress.getMessage(), request.getDesiredResourceState().toString());
            publishNotification(proxy, request.getDesiredResourceState(), snsClient, msg);
        }
        return progress;
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<OrganizationsClient> proxyClient,
            final Logger logger, TypeConfigurationModel typeConfiguration);


    protected static AmazonWebServicesClientProxy retrieveCrossAccountProxy(AmazonWebServicesClientProxy proxy, LoggerProxy loggerProxy, String roleArn) {
        ProxyClient<StsClient> proxyClient = proxy.newProxy(ClientBuilder::getStsClient);
        AssumeRoleResponse assumeRoleResponse = proxyClient.injectCredentialsAndInvokeV2(
                createAssumeRoleRequest(roleArn),
                proxyClient.client()::assumeRole
        );

        software.amazon.awssdk.services.sts.model.Credentials credentials = assumeRoleResponse.credentials();
        Credentials cfnCredentials = new Credentials(credentials.accessKeyId(), credentials.secretAccessKey(), credentials.sessionToken());
        return new AmazonWebServicesClientProxy(
                loggerProxy,
                cfnCredentials,
                DelayFactory.CONSTANT_DEFAULT_DELAY_FACTORY,
                WaitStrategy.scheduleForCallbackStrategy()
        );
    }

    protected ProgressEvent<ResourceModel, CallbackContext> listAccount(
          final AmazonWebServicesClientProxy proxy,
          final ProxyClient<OrganizationsClient> proxyClient,
          final ProgressEvent<ResourceModel, CallbackContext> progress,
          final ResourceModel model,
          final Logger logger
    ) {
    return proxy
            .initiate("ProServe-Organizations-Account::Create::ListAccounts", proxyClient, model, progress.getCallbackContext())
            .translateToServiceRequest(modelRequest -> createListAccountsRequest())
            .backoffDelay(MULTIPLE_OF)
            .makeServiceCall((modelRequest, proxyInvocation) -> {
              final Iterable<ListAccountsResponse> iterable = proxyInvocation.injectCredentialsAndInvokeIterableV2(modelRequest, proxyInvocation.client()::listAccountsPaginator);
              return flattenAccountsIterator(iterable);
            })
            .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> moveAccount(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<OrganizationsClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceModel model,
            final Logger logger,
            final CallbackContext ctx,
            final boolean moveToRoot
    ) {
        return proxy
                .initiate("ProServe-Organizations-Account::MoveAccount", proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest(modelRequest ->  createMoveAccountRequest(modelRequest, ctx.getCurrentParent(), moveToRoot ? ctx.getRootId() : modelRequest.getOrganizationalUnitId()))
                .backoffDelay(MULTIPLE_OF)
                .makeServiceCall((modelRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::moveAccount))
                .handleError((_request, e, _proxyClient, _model, context) -> {
                    if (e instanceof DuplicateAccountException) {
                        if (moveToRoot) {
                            return ProgressEvent.failed(_model, context, HandlerErrorCode.NotFound, "Account already moved to root.");
                        } else {
                            return ProgressEvent.progress(_model, context);
                        }

                    }
                    if (e instanceof ConcurrentModificationException) {
                        throw RetryableException.builder().message(e.getMessage()).build();
                    }
                    throw e;
                })
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> tagAccount(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<OrganizationsClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceModel model,
            final Logger logger,
            final CallbackContext ctx
    ) {
        return proxy
                .initiate("ProServe-Organizations-Account::TagAccount", proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest(Translator::createTagAccountRequest)
                .backoffDelay(MULTIPLE_OF)
                .makeServiceCall((modelRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::tagResource))
                .retryErrorFilter(this::filterException)
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> putAlternateContact(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<AccountClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceModel model,
            final String type,
            final Logger logger,
            final CallbackContext ctx
    ) {
        return proxy
                .initiate("ProServe-Organizations-Account::PutContact-"+type, proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest(_model -> createPutAlternateContactRequest(_model, type))
                .backoffDelay(MULTIPLE_OF)
                .makeServiceCall((modelRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::putAlternateContact))
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> deleteAlternateContact(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<AccountClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceModel model,
            final String type,
            final Logger logger,
            final CallbackContext ctx
    ) {
        return proxy
                .initiate("ProServe-Organizations-Account::DeleteContact-"+type, proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest(_model -> createDeleteAlternateContactRequest(_model, type))
                .backoffDelay(MULTIPLE_OF)
                .makeServiceCall((modelRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::deleteAlternateContact))
                .handleError((_request, e, _proxyClient, _model, context) -> {
                    if (e instanceof ResourceNotFoundException) {
                            return ProgressEvent.progress(_model, context);
                    }
                    throw e;
                })
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> findAccount(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<OrganizationsClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceModel model,
            final Logger logger
    ) {
        return proxy
                .initiate("ProServe-Organizations-Account::Create::FindAccount", proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest(modelRequest -> createListAccountsRequest())
                .backoffDelay(MULTIPLE_OF)
                .makeServiceCall((modelRequest, proxyInvocation) -> {
                    final Iterable<ListAccountsResponse> iterable = proxyInvocation.injectCredentialsAndInvokeIterableV2(modelRequest, proxyInvocation.client()::listAccountsPaginator);
                    return findFirstAccount(iterable, model);
                })
                .done((request, response, client, _model, context) -> {
                        if (response == null) {
                            return ProgressEvent.progress(_model, context);
                        } else {
                            if (_model.getAccountEmail().compareTo(response.email()) == 0 && _model.getAccountName().compareTo(response.name()) == 0) {
                                _model.setAccountId(response.id());
                                return ProgressEvent.progress(_model, context);
                            } else {
                                return ProgressEvent.failed(_model, context, HandlerErrorCode.AlreadyExists, "Account with this email already exists, account name does not match");
                            }
                            // return ProgressEvent.failed(_model, context, HandlerErrorCode.AlreadyExists, "Account with this email already exists"));
                        }
                });
    }

    protected ProgressEvent<ResourceModel, CallbackContext> describeAccount(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<OrganizationsClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceModel model,
            final Logger logger,
            final CallbackContext ctx
    ) {
        if (ctx.getRequestType() != null && ctx.getRequestType().equals("DELETE") && model.getAccountId() == null) {
            return ProgressEvent.defaultSuccessHandler(model);
        }
        return proxy
                .initiate("ProServe-Organizations-Account::DescribeAccount", proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest(Translator::createDescribeAccountRequest)
                .backoffDelay(MULTIPLE_OF)
                .makeServiceCall((modelRequest, proxyInvocation) -> {
                    final DescribeAccountResponse response = proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::describeAccount);
                    model.setAccountEmail(response.account().email());
                    ctx.setAccount(response.account());
                    return response;
                })
                .retryErrorFilter(this::filterException)
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> listAccountTags(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<OrganizationsClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceModel model,
            final Logger logger,
            final CallbackContext ctx
    ) {
        return proxy
                .initiate("ProServe-Organizations-Account::ListAccountTags", proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest(Translator::createListAccountTagsRequest)
                .backoffDelay(MULTIPLE_OF)
                .makeServiceCall((modelRequest, proxyInvocation) -> {
                    final ListTagsForResourceResponse response = proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::listTagsForResource);
                    model.setTags(translateFromSdkTags(response.tags()));
                    return response;
                })
                .retryErrorFilter(this::filterException)
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> getParentId(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<OrganizationsClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceModel model,
            final Logger logger,
            final CallbackContext ctx
    ) {
        return proxy
                .initiate("ProServe-Organizations-Account::GetParentId", proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest(modelRequest -> createListParentsRequest(model.getAccountId()))
                .backoffDelay(MULTIPLE_OF)
                .makeServiceCall((modelRequest, proxyInvocation) -> {
                    final ListParentsResponse response = proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::listParents);
                    ctx.setCurrentParent(response.parents().get(0).id());
                    return response;
                })
                .retryErrorFilter(this::filterException)
                .progress();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> getRootId(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<OrganizationsClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceModel model,
            final Logger logger,
            final CallbackContext ctx
    ) {
        return proxy
                .initiate("ProServe-Organizations-Account::GetRootId", proxyClient, model, progress.getCallbackContext())
                .translateToServiceRequest(modelRequest -> createListRootsRequest())
                .backoffDelay(MULTIPLE_OF)
                .makeServiceCall((modelRequest, proxyInvocation) -> {
                    final ListRootsResponse response = proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::listRoots);
                    ctx.setRootId(response.roots().get(0).id());
                    return response;
                })
                .retryErrorFilter(this::filterException)
                .progress();
    }

    protected static ProgressEvent<ResourceModel, CallbackContext> createDeploymentAccountRole(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<OrganizationsClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceModel model,
            final Logger logger
    ) {
        String accountId = progress.getCallbackContext().account.id();
        String roleName = model.getOrganizationAccountAccessRoleName() == null ? "OrganizationAccountAccessRole" : model.getOrganizationAccountAccessRoleName();
        AmazonWebServicesClientProxy _proxy_attempt;
        while (true) {
            try {
                _proxy_attempt = retrieveCrossAccountProxy(
                        proxy,
                        (LoggerProxy) logger,
                        String.format("arn:aws:iam::%s:role/%s", accountId, roleName)
                );
                break;
            } catch (StsException e) {
                logger.log(String.format("Retry: %s", e.getMessage()));
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ex) {
                    logger.log("Thread interrupted.");
                    ex.printStackTrace();
                }
            }
        }
        AmazonWebServicesClientProxy _proxy = _proxy_attempt;
        ProxyClient<IamClient> _proxyClient = _proxy.newProxy(ClientBuilder::getIamClient);
        return ProgressEvent.progress(model, progress.getCallbackContext())
                .then(_progress ->
                        _proxy
                                .initiate("ProServe-Organizations-Account::Create::CreateRole", _proxyClient, _progress.getResourceModel(), _progress.getCallbackContext())
                                .translateToServiceRequest(Translator::createCreateRoleRequest)
                                .backoffDelay(MULTIPLE_OF)
                                .makeServiceCall((modelRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::createRole))
                                .handleError((_request, e, _client, _model, context) -> {
                                    if (e instanceof EntityAlreadyExistsException) {
                                        return ProgressEvent.progress(_model, context);
                                    }
                                    throw e;
                                })
                                .progress()
                ).then(_progress -> {
                    for (String policyArn : _progress.getResourceModel().getDeploymentAccountConfiguration().getAWSManagedPolicyArns()) {
                        final ProgressEvent<ResourceModel, CallbackContext> progressEvent = _proxy
                                .initiate("ProServe-Organizations-Account::Create::AttachPolicy" + policyArn, _proxyClient, _progress.getResourceModel(), _progress.getCallbackContext())
                                .translateToServiceRequest(_model -> createAttachPolicyRequest(_model, policyArn))
                                .backoffDelay(MULTIPLE_OF)
                                .makeServiceCall((modelRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::attachRolePolicy))
                                .progress();
                        if (!progressEvent.isSuccess()) {
                            return progressEvent;
                        }
                    }
                    return ProgressEvent.progress(model, _progress.getCallbackContext());
                });
    }

    protected ProgressEvent<ResourceModel, CallbackContext> listOrganizationalUnitsForParent(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<OrganizationsClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceModel model,
            final Logger logger
    ) {
    return proxy
            .initiate("ProServe-Organizations-Account::Create::ListOrganizationalUnits", proxyClient, model, progress.getCallbackContext())
            .translateToServiceRequest(modelRequest -> crateListOrganizationalUnitsForParentRequest(modelRequest, ""))
            .backoffDelay(MULTIPLE_OF)
            .makeServiceCall((modelRequest, proxyInvocation) -> {
                final Iterable<ListOrganizationalUnitsForParentResponse> iterable = proxyInvocation.injectCredentialsAndInvokeIterableV2(modelRequest, proxyInvocation.client()::listOrganizationalUnitsForParentPaginator);
                return flattenOrganizationalUnitsIterator(iterable);
            })
            .progress();
    }

    private void publishNotification(
            final AmazonWebServicesClientProxy proxy,
            final ResourceModel model,
            final ProxyClient<SnsClient> proxyClient,
            final String msg
    ) {
        if (model.getNotificationTopicArn() != null) {
            proxyClient.injectCredentialsAndInvokeV2(
                    createPublishRequest(model, msg),
                    proxyClient.client()::publish
            );
        }
    }

    protected boolean isCreateAccountOperationStabilized(final ProxyClient<OrganizationsClient> proxyClient,
                                                         final String requestId,
                                                         final Logger logger,
                                                         final ResourceModel model,
                                                         final CallbackContext ctx) throws InterruptedException {
        int backoffDelay = 2;
        int backoffCount = 0;
        int backoffLimit = 10;
        while (true) {
            final CreateAccountStatus status = getAccountCreationStatus(proxyClient, requestId);
            switch (status.state()) {
                case SUCCEEDED:
                    model.setAccountId(status.accountId());
                    return true;
                case IN_PROGRESS:
                    model.setAccountId(status.accountId());
                    if (backoffLimit >= backoffCount) {
                        TimeUnit.SECONDS.sleep(backoffDelay);
                        backoffDelay += 2;
                        backoffCount++;
                        continue;
                    }
                default:
                    ctx.setCreateAccountFailureReason(status.failureReason());
                    String errMsg = String.format("account creating failed, reason: %s", status.failureReasonAsString());
                    logger.log(errMsg);
                    throw new TerminalException(errMsg);
            }
        }
    }

    private static CreateAccountStatus getAccountCreationStatus(
            final ProxyClient<OrganizationsClient> proxyClient,
            final String requestId) {

        final DescribeCreateAccountStatusResponse response = proxyClient.injectCredentialsAndInvokeV2(
                createCreateAccountStatusRequest(requestId),
                proxyClient.client()::describeCreateAccountStatus);
        return response.createAccountStatus();
    }

    protected boolean filterException(AwsRequest request, Exception e, ProxyClient<OrganizationsClient> client, ResourceModel model, CallbackContext context) {
        return e instanceof ConcurrentModificationException;
    }

}
