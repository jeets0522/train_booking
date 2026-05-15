package com.trainbooking.accounts.kafka;

import com.trainbooking.accounts.kafka.events.EmailVerificationEvent;
import com.trainbooking.accounts.kafka.events.PhoneOtpEvent;
import com.trainbooking.accounts.kafka.events.WelcomeEmailEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.email-verification}")
    private String emailVerificationTopic;

    @Value("${kafka.topics.welcome-email}")
    private String welcomeEmailTopic;

    @Value("${kafka.topics.sms-otp}")
    private String smsOtpTopic;

    public NotificationProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEmailVerification(EmailVerificationEvent event) {
        kafkaTemplate.send(emailVerificationTopic, event.getUserId().toString(), event);
    }

    public void sendWelcomeEmail(WelcomeEmailEvent event) {
        kafkaTemplate.send(welcomeEmailTopic, event.getUserId().toString(), event);
    }

    public void sendPhoneOtp(PhoneOtpEvent event) {
        kafkaTemplate.send(smsOtpTopic, event.getUserId().toString(), event);
    }
}
