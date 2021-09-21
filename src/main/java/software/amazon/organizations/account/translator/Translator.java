package software.amazon.organizations.account.translator;

import com.google.common.collect.Lists;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.iam.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.organizations.model.*;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.organizations.account.DeploymentAccountConfiguration;
import software.amazon.organizations.account.ResourceModel;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static software.amazon.organizations.account.translator.PropertyTranslator.translateToSdkTags;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  public static AssumeRoleRequest createAssumeRoleRequest(final String roleArn) {
    return AssumeRoleRequest.builder()
            .roleArn(roleArn)
            .roleSessionName("proserve-organizations-account")
            .build();
  }

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  public static DescribeAccountRequest createDescribeAccountRequest(final ResourceModel model) {
    return DescribeAccountRequest
            .builder()
            .accountId(model.getAccountId())
            .build();
  }

  public static ListParentsRequest createListParentsRequest(final String childId) {
    return ListParentsRequest
            .builder()
            .childId(childId)
            .build();
  }

  public static ListAccountsRequest createListAccountsRequest() {
    return ListAccountsRequest
            .builder()
            .build();
  }

  public static ListRootsRequest createListRootsRequest() {
    return ListRootsRequest
            .builder()
            .build();
  }

  public static ListOrganizationalUnitsForParentRequest crateListOrganizationalUnitsForParentRequest(final ResourceModel model, final String parentId) {
    return ListOrganizationalUnitsForParentRequest
            .builder()
            .parentId(parentId)
            .build();
  }

  public static MoveAccountRequest createMoveAccountRequest(final ResourceModel model, String currentParentId, String destinationId) {
    return MoveAccountRequest
            .builder()
            .accountId(model.getAccountId())
            .sourceParentId(currentParentId)
            .destinationParentId(destinationId)
            .build();
  }

  public static TagResourceRequest createTagAccountRequest(final ResourceModel model) {
    return TagResourceRequest
            .builder()
            .resourceId(model.getAccountId())
            .tags(translateToSdkTags(model.getTags()))
            .build();
  }

  public static ListTagsForResourceRequest createListAccountTagsRequest(final ResourceModel model) {
    return ListTagsForResourceRequest
            .builder()
            .resourceId(model.getAccountId())
            .build();
  }

  public static CreateAccountRequest createCreateAccountRequest(final ResourceModel model) {
    return CreateAccountRequest
            .builder()
            .accountName(model.getAccountName())
            .email(model.getAccountEmail())
            .roleName(model.getOrganizationAccountAccessRoleName())
            .tags(translateToSdkTags(model.getTags()))
            .build();
  }

  public static DescribeCreateAccountStatusRequest createCreateAccountStatusRequest(final String resourceId) {
    return DescribeCreateAccountStatusRequest
            .builder()
            .createAccountRequestId(resourceId)
            .build();
  }

  public static CreateRoleRequest createCreateRoleRequest(final ResourceModel model) {
    DeploymentAccountConfiguration config = model.getDeploymentAccountConfiguration();
    return CreateRoleRequest
            .builder()
            .roleName(config.getRoleName())
            .assumeRolePolicyDocument(String.format("{\"Statement\":[{\"Action\":\"sts:AssumeRole\",\"Condition\":{},\"Principal\":{\"AWS\":\"arn:aws:iam::%s:root\"},\"Effect\":\"Allow\"}],\"Version\":\"2012-10-17\"}", config.getAccountId()))
            .build();
  }

  public static AttachRolePolicyRequest createAttachPolicyRequest(final ResourceModel model, final String policyArn) {
    DeploymentAccountConfiguration config = model.getDeploymentAccountConfiguration();
    return AttachRolePolicyRequest
            .builder()
            .roleName(config.getRoleName())
            .policyArn(policyArn)
            .build();
  }

  public static PublishRequest createPublishRequest(final ResourceModel model, final String msg) {
    return PublishRequest
            .builder()
            .message(msg)
            .topicArn(model.getNotificationTopicArn())
            .subject("Account Vending Operation Failure")
            .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L75-L82
    return streamOfOrEmpty(Lists.newArrayList())
        .map(resource -> ResourceModel.builder()
            // include only primary identifier
            .build())
        .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  public static Account findFirstAccount(final Iterable<ListAccountsResponse> iterable, final ResourceModel model) {
    for (ListAccountsResponse listAccountsResponse : iterable) {
      Optional<Account> account = listAccountsResponse
              .accounts()
              .stream()
              .filter(_account -> _account.email().compareTo(model.getAccountEmail()) == 0)
              .findFirst();
      if (account.isPresent()) return account.get();
    }
    return null;
  }

  public static ListAccountsResponse flattenAccountsIterator(final Iterable<ListAccountsResponse> iterable) {
    Iterator<ListAccountsResponse> iterator = iterable.iterator();
    Set<Account> accounts = new HashSet<>();
    while (iterator.hasNext()) {
      accounts.addAll(iterator.next().accounts());
    }
    return ListAccountsResponse
            .builder()
            .accounts(accounts)
            .build();
  }

  public static ListOrganizationalUnitsForParentResponse flattenOrganizationalUnitsIterator(final Iterable<ListOrganizationalUnitsForParentResponse> iterable) {
    Iterator<ListOrganizationalUnitsForParentResponse> iterator = iterable.iterator();
    Set<OrganizationalUnit> ous = new HashSet<>();
    while (iterator.hasNext()) {
      ous.addAll(iterator.next().organizationalUnits());
    }
    return ListOrganizationalUnitsForParentResponse
            .builder()
            .organizationalUnits(ous)
            .build();
  }
}
