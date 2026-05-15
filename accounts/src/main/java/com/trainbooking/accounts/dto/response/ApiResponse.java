package com.trainbooking.accounts.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse {

    private String message;

    public static ApiResponse success(String message) {
        return new ApiResponse(message);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(message);
    }
}
