package com.wallet.exception;

import org.springframework.http.HttpStatus;

public final class BusinessExceptions {

    private BusinessExceptions() {
    }

    public static class BusinessException extends RuntimeException {
        private final String errorCode;
        private final HttpStatus status;

        public BusinessException(String errorCode, String message, HttpStatus status) {
            super(message);
            this.errorCode = errorCode;
            this.status = status;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }

    public static class UserAlreadyRegisteredException extends BusinessException {
        public UserAlreadyRegisteredException(String email) {
            super("USER_ALREADY_REGISTERED",
                    "User is already registered with email: " + email,
                    HttpStatus.CONFLICT);
        }
    }

    public static class WalletNotFoundException extends BusinessException {
        public WalletNotFoundException(Long walletId) {
            super("WALLET_NOT_FOUND",
                    "Wallet not found: " + walletId,
                    HttpStatus.NOT_FOUND);
        }
    }

    public static class InsufficientBalanceException extends BusinessException {
        public InsufficientBalanceException() {
            super("INSUFFICIENT_BALANCE",
                    "Insufficient wallet balance for this operation",
                    HttpStatus.BAD_REQUEST);
        }
    }

    public static class MaxBalanceExceededException extends BusinessException {
        public MaxBalanceExceededException() {
            super("MAX_BALANCE_EXCEEDED",
                    "Operation would exceed wallet maximum balance",
                    HttpStatus.BAD_REQUEST);
        }
    }

    public static class PaymentFailedException extends BusinessException {
        public PaymentFailedException(String reason) {
            super("PAYMENT_FAILED",
                    reason == null ? "Payment failed" : reason,
                    HttpStatus.BAD_REQUEST);
        }
    }

    public static class InvalidTransferException extends BusinessException {
        public InvalidTransferException(String message) {
            super("INVALID_TRANSFER", message, HttpStatus.BAD_REQUEST);
        }
    }
}
