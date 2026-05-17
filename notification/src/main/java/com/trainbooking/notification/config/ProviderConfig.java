package com.trainbooking.notification.config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class ProviderConfig {

    /**
     * SES client uses the DefaultCredentialsProvider chain (env vars, ~/.aws/credentials,
     * EC2 instance profile, etc.). If no credentials are available, calls will fail at
     * runtime with an AuthException and the EmailChannel will fail over to SendGrid.
     */
    @Bean
    public SesClient sesClient(@Value("${aws.ses.region}") String region) {
        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * SendGrid bean is created only when an API key is configured. If the key is unset
     * or blank, SendGridEmailProvider stays unregistered and the channel falls back to SES.
     */
    @Bean
    @ConditionalOnExpression("'${sendgrid.api-key:}'.length() > 0")
    public SendGrid sendGrid(@Value("${sendgrid.api-key}") String apiKey) {
        return new SendGrid(apiKey);
    }
}
