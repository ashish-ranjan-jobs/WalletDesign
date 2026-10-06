package com.wallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateUserRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;

    private String phone;

    private String address;

    /**
     * Optional custom max balance; falls back to configured default.
     */
    @Positive(message = "maxBalance must be > 0")
    private BigDecimal maxBalance;
}
