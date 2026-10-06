package com.wallet.controller;

import com.wallet.dto.ApiResponses.*;
import com.wallet.dto.CreateUserRequest;
import com.wallet.dto.TopUpRequest;
import com.wallet.dto.TransferRequest;
import com.wallet.dto.WithdrawRequest;
import com.wallet.enums.TxnType;
import com.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class WalletController {

    private final WalletService walletService;

    /**
     * POST /api/create_user
     * Creates a user and associated wallet; returns walletId.
     */
    @PostMapping("/create_user")
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse response = walletService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/wallet_balance/{walletId}
     */
    @GetMapping("/wallet_balance/{walletId}")
    public ResponseEntity<WalletBalanceResponse> getWalletBalance(@PathVariable Long walletId) {
        return ResponseEntity.ok(walletService.getBalance(walletId));
    }

    /**
     * POST /api/top_up
     */
    @PostMapping("/top_up")
    public ResponseEntity<MoneyOperationResponse> topUp(@Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.ok(walletService.topUp(request));
    }

    /**
     * POST /api/withdraw
     */
    @PostMapping("/withdraw")
    public ResponseEntity<MoneyOperationResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(walletService.withdraw(request));
    }

    /**
     * POST /api/transfer
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(walletService.transfer(request));
    }

    /**
     * GET /api/txn_details?walletId=&startDate=&endDate=&txnType=&page=&size=
     */
    @GetMapping("/txn_details")
    public ResponseEntity<TxnHistoryResponse> txnDetails(
            @RequestParam Long walletId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) TxnType txnType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                walletService.getTxnDetails(walletId, startDate, endDate, txnType, page, size)
        );
    }
}
