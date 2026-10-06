package com.wallet.entity;

import com.wallet.enums.TxnStatus;
import com.wallet.enums.TxnType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "txn_history", indexes = {
        @Index(name = "idx_txn_created_at", columnList = "created_at"),
        @Index(name = "idx_txn_type", columnList = "txn_type"),
        @Index(name = "idx_txn_from_id", columnList = "from_id"),
        @Index(name = "idx_txn_to_id", columnList = "to_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TxnHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Source wallet id. Null for TOP_UP (external source).
     */
    @Column(name = "from_id")
    private Long fromId;

    /**
     * Destination wallet id. Null for WITHDRAW (external destination).
     */
    @Column(name = "to_id")
    private Long toId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 20)
    private TxnType txnType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TxnStatus status;

    @Column(length = 512)
    private String failureReason;

    /**
     * Owning wallet for history queries (the wallet this row is primarily about).
     * For TRANSFER, two rows may be created or one shared; we attach to from-wallet
     * and also keep fromId/toId for filtering.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
