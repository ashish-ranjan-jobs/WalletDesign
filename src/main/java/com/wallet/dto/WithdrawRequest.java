package com.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawRequest {

    /**
     * Wallet id to withdraw from (maps to external_id intent in the design:
     * funds leave this wallet toward an external destination).
     */
    @NotNull(message = "walletId is required")
    private Long walletId;

    /**
     * Optional external destination reference (bank account / payout id).
     */
    private String externalId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be greater than zero")
    private BigDecimal amount;
}
