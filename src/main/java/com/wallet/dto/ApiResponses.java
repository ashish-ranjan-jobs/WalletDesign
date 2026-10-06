package com.wallet.dto;

import com.wallet.enums.TxnStatus;
import com.wallet.enums.TxnType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApiResponses {

    private ApiResponses() {
    }

    @Value
    @Builder
    public static class CreateUserResponse {
        Long userId;
        Long walletId;
        String message;
    }

    @Value
    @Builder
    public static class WalletBalanceResponse {
        Long walletId;
        BigDecimal amount;
        BigDecimal maxAmount;
    }

    @Value
    @Builder
    public static class MoneyOperationResponse {
        boolean success;
        Long walletId;
        BigDecimal amount;
        BigDecimal balance;
        BigDecimal maxAmount;
        Long transactionId;
        String message;
    }

    @Value
    @Builder
    public static class TransferResponse {
        boolean success;
        Long fromId;
        Long toId;
        BigDecimal amount;
        BigDecimal fromBalance;
        BigDecimal toBalance;
        Long transactionId;
        String message;
    }

    @Value
    @Builder
    public static class TxnDetail {
        Long id;
        Long fromId;
        Long toId;
        BigDecimal amount;
        TxnType txnType;
        TxnStatus status;
        String failureReason;
        Instant createdAt;
    }

    @Value
    @Builder
    public static class TxnHistoryResponse {
        boolean success;
        int page;
        int size;
        long totalElements;
        int totalPages;
        List<TxnDetail> transactions;
    }

    @Value
    @Builder
    public static class ErrorResponse {
        boolean success;
        String errorCode;
        String message;
        Instant timestamp;
    }
}
