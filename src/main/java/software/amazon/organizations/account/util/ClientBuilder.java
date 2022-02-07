package software.amazon.organizations.account.util;

import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.account.AccountClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.organizations.OrganizationsClient;
import com.amazonaws.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.OrRetryCondition;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.services.organizations.model.Account;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {
    public static OrganizationsClient getClient() {
        return LazyHolder.SERVICE_CLIENT;
    }

    public static StsClient getStsClient() {
        return LazyHolder.STS_CLIENT;
    }

    public static IamClient getIamClient() {
        return LazyHolder.IAM_CLIENT;
    }

    public static SnsClient getSnsClient() {
        return LazyHolder.SNS_CLIENT;
    }

    public static AccountClient getAccountClient() {
        return LazyHolder.ACCOUNT_CLIENT;
    }

    /**
     * Get OrganizationsClient for requests to interact with SC client
     *
     * @return {@link OrganizationsClient}
     */
    private static class LazyHolder {

        private static final Integer MAX_RETRIES = 10;

        public static OrganizationsClient SERVICE_CLIENT = OrganizationsClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .region(Region.US_EAST_1)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryPolicy(RetryPolicy.builder()
                                .backoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                                .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                                .numRetries(MAX_RETRIES)
                                .retryCondition(OrRetryCondition.create(RetryCondition.defaultRetryCondition(),
                                        OrganizationsClientRetryCondition.create()))
                                .build())
                        .build())
                .build();

        public static StsClient STS_CLIENT = StsClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryPolicy(RetryPolicy.builder()
                                .backoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                                .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                                .numRetries(MAX_RETRIES)
                                .retryCondition(OrRetryCondition.create(RetryCondition.defaultRetryCondition(),
                                        OrganizationsClientRetryCondition.create()))
                                .build())
                        .build())
                .build();

        public static IamClient IAM_CLIENT = IamClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .region(Region.AWS_GLOBAL)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryPolicy(RetryPolicy.builder()
                                .backoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                                .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                                .numRetries(MAX_RETRIES)
                                .retryCondition(OrRetryCondition.create(RetryCondition.defaultRetryCondition(),
                                        OrganizationsClientRetryCondition.create()))
                                .build())
                        .build())
                .build();

        public static SnsClient SNS_CLIENT = SnsClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryPolicy(RetryPolicy.builder()
                                .backoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                                .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                                .numRetries(MAX_RETRIES)
                                .retryCondition(OrRetryCondition.create(RetryCondition.defaultRetryCondition(),
                                        OrganizationsClientRetryCondition.create()))
                                .build())
                        .build())
                .build();

        public static AccountClient ACCOUNT_CLIENT = AccountClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .retryPolicy(RetryPolicy.builder()
                                .backoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                                .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy())
                                .numRetries(MAX_RETRIES)
                                .retryCondition(OrRetryCondition.create(RetryCondition.defaultRetryCondition(),
                                        OrganizationsClientRetryCondition.create()))
                                .build())
                        .build())
                .build();

        /**
         * OrganizationsClient Throttling Exception StatusCode is 400 while default throttling code is 429
         * https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/exception/SdkServiceException.java#L91
         * which means we would need to customize a RetryCondition
         */
        @ToString
        @EqualsAndHashCode
        @NoArgsConstructor
        public static class OrganizationsClientRetryCondition implements RetryCondition {

            public static OrganizationsClientRetryCondition create() {
                return new OrganizationsClientRetryCondition();
            }

            @Override
            public boolean shouldRetry(RetryPolicyContext context) {
                final String errorMessage = context.exception().getMessage();
                if (StringUtils.isNullOrEmpty(errorMessage)) return false;
                if (context.exception() instanceof RetryableException) return true;
                return errorMessage.contains("Rate exceeded");
            }
        }
    }
}
