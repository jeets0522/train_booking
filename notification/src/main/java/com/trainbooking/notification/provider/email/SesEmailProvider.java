package com.trainbooking.notification.provider.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trainbooking.notification.domain.enums.ProviderName;
import com.trainbooking.notification.provider.ProviderResult;
import com.trainbooking.notification.template.ProviderTemplateRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailRequest;
import software.amazon.awssdk.services.ses.model.SendTemplatedEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

import java.util.Map;

@Component
@ConditionalOnBean(SesClient.class)
public class SesEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SesEmailProvider.class);

    private final SesClient sesClient;
    private final ObjectMapper objectMapper;
    private final String configurationSet;

    public SesEmailProvider(
            SesClient sesClient,
            ObjectMapper objectMapper,
            @Value("${aws.ses.configuration-set:}") String configurationSet) {
        this.sesClient = sesClient;
        this.objectMapper = objectMapper;
        this.configurationSet = configurationSet;
    }

    @Override
    public ProviderName name() {
        return ProviderName.SES;
    }

    @Override
    public ProviderResult send(TemplatedEmail payload) {
        if (!(payload.templateRef() instanceof ProviderTemplateRef.SesTemplateRef sesRef)) {
            return new ProviderResult.PermanentFailure(
                    "SES provider received a non-SES template ref: " + payload.templateRef());
        }
        if (sesRef.templateName() == null || sesRef.templateName().isBlank()) {
            return new ProviderResult.PermanentFailure("SES template_name is missing in catalog");
        }

        String templateData;
        try {
            Map<String, Object> vars = payload.variables() == null ? Map.of() : payload.variables();
            templateData = objectMapper.writeValueAsString(vars);
        } catch (JsonProcessingException e) {
            return new ProviderResult.PermanentFailure("Failed to serialize template variables", e);
        }

        SendTemplatedEmailRequest.Builder request = SendTemplatedEmailRequest.builder()
                .source(formatAddress(payload.from()))
                .destination(Destination.builder().toAddresses(payload.to().email()).build())
                .template(sesRef.templateName())
                .templateData(templateData);
        if (configurationSet != null && !configurationSet.isBlank()) {
            request.configurationSetName(configurationSet);
        }

        try {
            SendTemplatedEmailResponse response = sesClient.sendTemplatedEmail(request.build());
            return new ProviderResult.Success(response.messageId());
        } catch (SesException e) {
            int status = e.statusCode();
            String msg = e.awsErrorDetails() != null && e.awsErrorDetails().errorMessage() != null
                    ? e.awsErrorDetails().errorMessage()
                    : e.getMessage();
            if (status >= 400 && status < 500) {
                log.warn("SES permanent failure (status={}): {}", status, msg);
                return new ProviderResult.PermanentFailure(msg, e);
            }
            log.warn("SES retryable failure (status={}): {}", status, msg);
            return new ProviderResult.RetryableFailure(msg, e);
        } catch (SdkException e) {
            log.warn("SES SDK exception: {}", e.getMessage());
            return new ProviderResult.RetryableFailure(e.getMessage(), e);
        }
    }

    private String formatAddress(EmailRecipient from) {
        if (from == null) {
            throw new IllegalStateException("Email 'from' address must be configured");
        }
        if (from.name() == null || from.name().isBlank()) {
            return from.email();
        }
        return from.name() + " <" + from.email() + ">";
    }
}
