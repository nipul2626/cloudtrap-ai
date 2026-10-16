
package com.cloudtrap.api.scanning.provider.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;

@Configuration
public class AwsStsConfiguration {

    @Bean(destroyMethod = "close")
    public StsClient awsStsClient(
            @Value("${cloudtrap.aws.sts.region:us-east-1}")
            String region) {

        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException(
                    "AWS STS region must not be blank");
        }

        return StsClient.builder()
                .region(Region.of(region.trim()))
                .build();
    }
}
