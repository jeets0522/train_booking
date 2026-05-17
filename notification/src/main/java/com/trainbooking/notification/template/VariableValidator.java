package com.trainbooking.notification.template;

import com.trainbooking.notification.exception.TemplateException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class VariableValidator {

    private VariableValidator() {}

    public static void validate(TemplateDescriptor descriptor, Map<String, Object> variables) {
        if (descriptor.variables() == null || descriptor.variables().isEmpty()) {
            return;
        }
        Map<String, Object> vars = variables == null ? Map.of() : variables;
        List<String> missing = new ArrayList<>();
        for (String required : descriptor.variables()) {
            if (!vars.containsKey(required) || vars.get(required) == null) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new TemplateException(
                    "Template '" + descriptor.key() + "' missing variables: " + missing);
        }
    }
}
