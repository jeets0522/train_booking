package com.trainbooking.accounts.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyPhoneRequest {

    @NotBlank(message = "OTP is required")
    private String otp;
}
