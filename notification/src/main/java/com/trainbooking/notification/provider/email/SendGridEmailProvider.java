package com.trainbooking.notification.provider.email;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.trainbooking.notification.domain.enums.ProviderName;
import com.trainbooking.notification.provider.ProviderResult;
import com.trainbooking.notification.template.ProviderTemplateRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@ConditionalOnBean(SendGrid.class)
public class SendGridEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailProvider.class);

    private final SendGrid sendGrid;

    public SendGridEmailProvider(SendGrid sendGrid) {
        this.sendGrid = sendGrid;
    }

    @Override
    public ProviderName name() {
        return ProviderName.SENDGRID;
    }

    @Override
    public ProviderResult send(TemplatedEmail payload) {
        if (!(payload.templateRef() instanceof ProviderTemplateRef.SendGridTemplateRef sgRef)) {
            return new ProviderResult.PermanentFailure(
                    "SendGrid provider received a non-SendGrid template ref: " + payload.templateRef());
        }
        if (sgRef.templateId() == null || sgRef.templateId().isBlank()) {
            return new ProviderResult.PermanentFailure("SendGrid template_id is missing in catalog");
        }

        Mail mail = new Mail();
        if (payload.from() != null) {
            mail.setFrom(new Email(payload.from().email(), payload.from().name()));
        }
        mail.setTemplateId(sgRef.templateId());

        Personalization personalization = new Personalization();
        personalization.addTo(new Email(payload.to().email(), payload.to().name()));
        if (payload.variables() != null) {
            for (Map.Entry<String, Object> entry : payload.variables().entrySet()) {
                personalization.addDynamicTemplateData(entry.getKey(), entry.getValue());
            }
        }
        mail.addPersonalization(personalization);

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        try {
            request.setBody(mail.build());
            Response response = sendGrid.api(request);
            int status = response.getStatusCode();
            if (status >= 200 && status < 300) {
                String messageId = extractMessageId(response);
                return new ProviderResult.Success(messageId);
            }
            String body = response.getBody();
            if (status >= 400 && status < 500 && status != 429) {
                log.warn("SendGrid permanent failure (status={}): {}", status, body);
                return new ProviderResult.PermanentFailure("HTTP " + status + ": " + body);
            }
            log.warn("SendGrid retryable failure (status={}): {}", status, body);
            return new ProviderResult.RetryableFailure("HTTP " + status + ": " + body);
        } catch (IOException e) {
            log.warn("SendGrid I/O exception: {}", e.getMessage());
            return new ProviderResult.RetryableFailure(e.getMessage(), e);
        }
    }

    private String extractMessageId(Response response) {
        if (response.getHeaders() == null) {
            return null;
        }
        for (Map.Entry<String, String> e : response.getHeaders().entrySet()) {
            if ("X-Message-Id".equalsIgnoreCase(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }
}
