package com.wallet.service;

import com.wallet.entity.TxnHistory;
import com.wallet.entity.Wallet;
import com.wallet.enums.TxnStatus;
import com.wallet.enums.TxnType;
import com.wallet.repository.TxnHistoryRepository;
import com.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Persists txn rows in an independent transaction so FAILURE records
 * survive when the outer money-movement transaction rolls back.
 */
@Service
@RequiredArgsConstructor
public class TxnHistoryService {

    private final TxnHistoryRepository txnHistoryRepository;
    private final WalletRepository walletRepository;

    /** Joins the caller's transaction (used for SUCCESS rows). */
    @Transactional(propagation = Propagation.REQUIRED)
    public TxnHistory recordSuccess(
            Long fromId,
            Long toId,
            BigDecimal amount,
            TxnType type,
            Long walletId
    ) {
        return recordSuccess(fromId, toId, amount, type, walletId, null);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public TxnHistory recordSuccess(
            Long fromId,
            Long toId,
            BigDecimal amount,
            TxnType type,
            Long walletId,
            String note
    ) {
        return persist(fromId, toId, amount, type, TxnStatus.SUCCESS, note, walletId);
    }

    /** Independent transaction so FAILURE rows survive outer rollback. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TxnHistory recordFailure(
            Long fromId,
            Long toId,
            BigDecimal amount,
            TxnType type,
            String failureReason,
            Long walletId
    ) {
        return persist(fromId, toId, amount, type, TxnStatus.FAILURE, failureReason, walletId);
    }

    private TxnHistory persist(
            Long fromId,
            Long toId,
            BigDecimal amount,
            TxnType type,
            TxnStatus status,
            String failureReason,
            Long walletId
    ) {
        Wallet walletRef = null;
        if (walletId != null) {
            walletRef = walletRepository.findById(walletId).orElse(null);
        }

        TxnHistory txn = TxnHistory.builder()
                .fromId(fromId)
                .toId(toId)
                .amount(amount)
                .txnType(type)
                .status(status)
                .failureReason(failureReason)
                .wallet(walletRef)
                .build();
        return txnHistoryRepository.saveAndFlush(txn);
    }
}
