package com.trainbooking.notification.template;

public sealed interface ProviderTemplateRef
        permits ProviderTemplateRef.SesTemplateRef,
                ProviderTemplateRef.SendGridTemplateRef,
                ProviderTemplateRef.TwilioContentRef,
                ProviderTemplateRef.SnsApprovedBody,
                ProviderTemplateRef.MetaTemplateRef {

    record SesTemplateRef(String templateName) implements ProviderTemplateRef {}

    record SendGridTemplateRef(String templateId) implements ProviderTemplateRef {}

    record TwilioContentRef(
            String contentSid,
            String dltTemplateId,
            String dltEntityId,
            String senderId) implements ProviderTemplateRef {}

    record SnsApprovedBody(
            String approvedBody,
            String senderId,
            String dltTemplateId,
            String dltEntityId) implements ProviderTemplateRef {}

    record MetaTemplateRef(
            String templateName,
            String language) implements ProviderTemplateRef {}
}
