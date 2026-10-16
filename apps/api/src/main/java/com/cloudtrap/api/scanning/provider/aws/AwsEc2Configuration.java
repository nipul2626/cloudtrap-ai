
package com.cloudtrap.api.scanning.provider.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

@Configuration
public class AwsEc2Configuration {

    @Bean
    public Ec2Client ec2Client(
            @Value("${cloudtrap.aws.ec2.region:us-east-1}")
            String region) {

        return Ec2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        DefaultCredentialsProvider.create())
                .build();
    }
}
