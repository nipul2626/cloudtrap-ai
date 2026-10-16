
package com.cloudtrap.api.scanning.provider.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.time.Duration;

@Configuration
public class AwsEc2Configuration {

    @Bean(destroyMethod = "close")
    public Ec2Client ec2Client(
            @Value("${cloudtrap.aws.ec2.region:us-east-1}")
            String region,
            @Value("${cloudtrap.aws.ec2.api-call-timeout:20s}")
            Duration apiCallTimeout,
            @Value("${cloudtrap.aws.ec2.connection-timeout:3s}")
            Duration connectionTimeout) {

        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException(
                    "AWS EC2 region must not be blank");
        }

        if (apiCallTimeout.isZero() || apiCallTimeout.isNegative()
                || connectionTimeout.isZero()
                || connectionTimeout.isNegative()
                || connectionTimeout.compareTo(apiCallTimeout) > 0) {
            throw new IllegalArgumentException(
                    "AWS EC2 timeout configuration is invalid");
        }

        return Ec2Client.builder()
                .region(Region.of(region.trim()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .httpClientBuilder(ApacheHttpClient.builder()
                        .connectionTimeout(connectionTimeout))
                .overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                .apiCallTimeout(apiCallTimeout)
                                .apiCallAttemptTimeout(apiCallTimeout)
                                .build())
                .build();
    }
}
