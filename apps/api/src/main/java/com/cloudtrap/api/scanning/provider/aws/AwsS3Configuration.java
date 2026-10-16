package com.cloudtrap.api.scanning.provider.aws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Configuration {

    @Bean(destroyMethod = "close")
    public S3Client awsS3Client() {
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .build();
    }
}