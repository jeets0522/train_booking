package com.trainbooking.notification.dto.request;

import com.trainbooking.notification.domain.enums.Channel;
import com.trainbooking.notification.domain.enums.SuppressionReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddSuppressionRequest {

    @NotNull
    private Channel channel;

    @NotBlank
    private String recipient;

    @NotNull
    private SuppressionReason reason;

    private String notes;
}
