package com.example.testgrpc.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AWSConfiguration {

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.EU_CENTRAL_1)
                .build();
    }
}
