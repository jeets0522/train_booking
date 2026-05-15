package com.trainbooking.accounts.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WelcomeEmailEvent {

    private Long userId;
    private String email;
    private String firstName;
}
