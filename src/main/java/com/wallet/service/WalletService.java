package com.wallet.service;

import com.wallet.dto.ApiResponses.*;
import com.wallet.dto.CreateUserRequest;
import com.wallet.dto.TopUpRequest;
import com.wallet.dto.TransferRequest;
import com.wallet.dto.WithdrawRequest;
import com.wallet.entity.TxnHistory;
import com.wallet.entity.User;
import com.wallet.entity.Wallet;
import com.wallet.enums.TxnType;
import com.wallet.exception.BusinessExceptions.*;
import com.wallet.repository.TxnHistoryRepository;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TxnHistoryRepository txnHistoryRepository;
    private final TxnHistoryService txnHistoryService;

    @Value("${wallet.default-max-balance:100000.00}")
    private BigDecimal defaultMaxBalance;

    /**
     * Creates a user and their wallet (1:1). Returns the new wallet id.
     */
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new UserAlreadyRegisteredException(request.getEmail());
        }

        BigDecimal maxBalance = request.getMaxBalance() != null
                ? request.getMaxBalance()
                : defaultMaxBalance;

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .maxBalance(maxBalance)
                .build();

        user.setWallet(wallet);
        User saved = userRepository.save(user);

        log.info("Created user id={} wallet id={}", saved.getId(), saved.getWallet().getId());

        return CreateUserResponse.builder()
                .userId(saved.getId())
                .walletId(saved.getWallet().getId())
                .message("User and wallet created successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public WalletBalanceResponse getBalance(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        return WalletBalanceResponse.builder()
                .walletId(wallet.getId())
                .amount(wallet.getBalance())
                .maxAmount(wallet.getMaxBalance())
                .build();
    }

    /**
     * Credits wallet from an external source (top-up).
     * Records SUCCESS or FAILURE in txn_history.
     */
    @Transactional
    public MoneyOperationResponse topUp(TopUpRequest request) {
        Wallet wallet = lockWallet(request.getWalletId());
        BigDecimal amount = request.getAmount();

        try {
            wallet.credit(amount);
            walletRepository.save(wallet);

            TxnHistory txn = txnHistoryService.recordSuccess(
                    null, wallet.getId(), amount, TxnType.TOP_UP, wallet.getId());

            return MoneyOperationResponse.builder()
                    .success(true)
                    .walletId(wallet.getId())
                    .amount(amount)
                    .balance(wallet.getBalance())
                    .maxAmount(wallet.getMaxBalance())
                    .transactionId(txn.getId())
                    .message("Success")
                    .build();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            txnHistoryService.recordFailure(null, wallet.getId(), amount, TxnType.TOP_UP,
                    ex.getMessage(), wallet.getId());
            if (ex.getMessage() != null && ex.getMessage().contains("max balance")) {
                throw new MaxBalanceExceededException();
            }
            throw new PaymentFailedException(ex.getMessage());
        }
    }

    /**
     * Debits wallet toward an external destination (withdraw).
     */
    @Transactional
    public MoneyOperationResponse withdraw(WithdrawRequest request) {
        Wallet wallet = lockWallet(request.getWalletId());
        BigDecimal amount = request.getAmount();

        try {
            wallet.debit(amount);
            walletRepository.save(wallet);

            String reasonNote = request.getExternalId() == null
                    ? null
                    : "externalId=" + request.getExternalId();

            TxnHistory txn = txnHistoryService.recordSuccess(
                    wallet.getId(), null, amount, TxnType.WITHDRAW, wallet.getId(), reasonNote);

            return MoneyOperationResponse.builder()
                    .success(true)
                    .walletId(wallet.getId())
                    .amount(amount)
                    .balance(wallet.getBalance())
                    .maxAmount(wallet.getMaxBalance())
                    .transactionId(txn.getId())
                    .message("Success")
                    .build();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            txnHistoryService.recordFailure(wallet.getId(), null, amount, TxnType.WITHDRAW,
                    ex.getMessage(), wallet.getId());
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("insufficient")) {
                throw new InsufficientBalanceException();
            }
            throw new PaymentFailedException(ex.getMessage());
        }
    }

    /**
     * Transfers funds between two wallets.
     * Locks wallets in ascending id order to prevent deadlocks
     * (A→C locks A then C; C→A also locks A then C).
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        if (request.getFromId().equals(request.getToId())) {
            throw new InvalidTransferException("fromId and toId must be different");
        }

        Long firstId = request.getFromId() < request.getToId()
                ? request.getFromId()
                : request.getToId();
        Long secondId = request.getFromId() < request.getToId()
                ? request.getToId()
                : request.getFromId();

        Wallet first = lockWallet(firstId);
        Wallet second = lockWallet(secondId);

        Wallet from = request.getFromId().equals(first.getId()) ? first : second;
        Wallet to = request.getToId().equals(first.getId()) ? first : second;
        BigDecimal amount = request.getAmount();

        try {
            from.debit(amount);
            to.credit(amount);
            walletRepository.save(from);
            walletRepository.save(to);

            TxnHistory txn = txnHistoryService.recordSuccess(
                    from.getId(), to.getId(), amount, TxnType.TRANSFER, from.getId());

            return TransferResponse.builder()
                    .success(true)
                    .fromId(from.getId())
                    .toId(to.getId())
                    .amount(amount)
                    .fromBalance(from.getBalance())
                    .toBalance(to.getBalance())
                    .transactionId(txn.getId())
                    .message("Success")
                    .build();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            txnHistoryService.recordFailure(from.getId(), to.getId(), amount, TxnType.TRANSFER,
                    ex.getMessage(), from.getId());

            String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (msg.contains("insufficient")) {
                throw new InsufficientBalanceException();
            }
            if (msg.contains("max balance")) {
                throw new MaxBalanceExceededException();
            }
            throw new PaymentFailedException(ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public TxnHistoryResponse getTxnDetails(
            Long walletId,
            Instant startDate,
            Instant endDate,
            TxnType txnType,
            int page,
            int size
    ) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 20;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TxnHistory> result = txnHistoryRepository.findAll(
                TxnHistoryRepository.withFilters(walletId, txnType, startDate, endDate),
                pageable);

        List<TxnDetail> details = result.getContent().stream()
                .map(t -> TxnDetail.builder()
                        .id(t.getId())
                        .fromId(t.getFromId())
                        .toId(t.getToId())
                        .amount(t.getAmount())
                        .txnType(t.getTxnType())
                        .status(t.getStatus())
                        .failureReason(t.getFailureReason())
                        .createdAt(t.getCreatedAt())
                        .build())
                .toList();

        return TxnHistoryResponse.builder()
                .success(true)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .transactions(details)
                .build();
    }

    private Wallet lockWallet(Long walletId) {
        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }
}
