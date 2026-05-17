package com.trainbooking.notification.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.domain.enums.ProviderName;
import com.trainbooking.notification.domain.enums.SmsMode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TemplateCatalog {

    private static final Logger log = LoggerFactory.getLogger(TemplateCatalog.class);

    private final ResourceLoader resourceLoader;
    private final String catalogLocation;
    private final Map<String, TemplateDescriptor> templates = new HashMap<>();

    public TemplateCatalog(
            ResourceLoader resourceLoader,
            @Value("${notification.template-catalog.location:classpath:template-catalog.yml}") String catalogLocation) {
        this.resourceLoader = resourceLoader;
        this.catalogLocation = catalogLocation;
    }

    @PostConstruct
    void load() throws IOException {
        Resource resource = resourceLoader.getResource(catalogLocation);
        if (!resource.exists()) {
            throw new IllegalStateException("Template catalog not found at: " + catalogLocation);
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream is = resource.getInputStream()) {
            JsonNode root = mapper.readTree(is);
            JsonNode templateNodes = root.path("templates");
            if (!templateNodes.isObject()) {
                throw new IllegalStateException("template-catalog.yml is missing the 'templates:' root");
            }
            templateNodes.properties().forEach(entry -> {
                String key = entry.getKey();
                templates.put(key, parseDescriptor(key, entry.getValue()));
            });
        }
        log.info("Loaded {} notification templates from {}", templates.size(), catalogLocation);
    }

    public TemplateDescriptor lookup(String key) {
        TemplateDescriptor t = templates.get(key);
        if (t == null) {
            throw new IllegalArgumentException("Unknown template key: " + key);
        }
        return t;
    }

    public boolean contains(String key) {
        return templates.containsKey(key);
    }

    private TemplateDescriptor parseDescriptor(String key, JsonNode node) {
        Channel channel = Channel.valueOf(node.path("channel").asText().toUpperCase());
        SmsMode mode = node.has("mode") && !node.get("mode").isNull()
                ? SmsMode.valueOf(node.get("mode").asText().toUpperCase())
                : null;

        List<String> variables = new ArrayList<>();
        JsonNode varsNode = node.path("variables");
        if (varsNode.isArray()) {
            varsNode.forEach(v -> variables.add(v.asText()));
        }

        String body = textOrNull(node, "body");
        String titleKey = textOrNull(node, "title_key");
        String bodyKey = textOrNull(node, "body_key");

        Map<ProviderName, ProviderTemplateRef> providers = parseProviders(node.path("providers"));

        return new TemplateDescriptor(
                key,
                channel,
                mode,
                providers,
                Collections.unmodifiableList(variables),
                body,
                titleKey,
                bodyKey);
    }

    private Map<ProviderName, ProviderTemplateRef> parseProviders(JsonNode providersNode) {
        if (!providersNode.isObject()) {
            return Map.of();
        }
        Map<ProviderName, ProviderTemplateRef> map = new EnumMap<>(ProviderName.class);
        providersNode.properties().forEach(entry -> {
            ProviderName provider = ProviderName.fromWireName(entry.getKey());
            map.put(provider, parseRef(provider, entry.getValue()));
        });
        return Collections.unmodifiableMap(map);
    }

    private ProviderTemplateRef parseRef(ProviderName provider, JsonNode node) {
        return switch (provider) {
            case SES -> new ProviderTemplateRef.SesTemplateRef(
                    textOrNull(node, "template_name"));
            case SENDGRID -> new ProviderTemplateRef.SendGridTemplateRef(
                    textOrNull(node, "template_id"));
            case TWILIO -> new ProviderTemplateRef.TwilioContentRef(
                    textOrNull(node, "content_sid"),
                    textOrNull(node, "dlt_template_id"),
                    textOrNull(node, "dlt_entity_id"),
                    textOrNull(node, "sender_id"));
            case SNS -> new ProviderTemplateRef.SnsApprovedBody(
                    textOrNull(node, "approved_body"),
                    textOrNull(node, "sender_id"),
                    textOrNull(node, "dlt_template_id"),
                    textOrNull(node, "dlt_entity_id"));
            case META -> new ProviderTemplateRef.MetaTemplateRef(
                    textOrNull(node, "template_name"),
                    textOrNull(node, "language"));
            case FCM -> throw new IllegalStateException(
                    "FCM is not a templated provider; push templates use title_key/body_key");
        };
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? null : child.asText();
    }
}
