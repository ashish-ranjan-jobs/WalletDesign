package com.wallet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wallets", indexes = {
        @Index(name = "idx_wallet_user_id", columnList = "user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "max_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxBalance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TxnHistory> transactions = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        modifiedAt = now;
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        validateInvariants();
    }

    @PreUpdate
    void onUpdate() {
        modifiedAt = Instant.now();
        validateInvariants();
    }

    public void credit(BigDecimal amount) {
        requirePositive(amount);
        BigDecimal newBalance = balance.add(amount);
        if (newBalance.compareTo(maxBalance) > 0) {
            throw new IllegalStateException(
                    "Top-up would exceed max balance of " + maxBalance);
        }
        this.balance = newBalance;
    }

    public void debit(BigDecimal amount) {
        requirePositive(amount);
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance = balance.subtract(amount);
    }

    private void validateInvariants() {
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Balance must be >= 0");
        }
        if (maxBalance == null || maxBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Max balance must be > 0");
        }
        if (balance.compareTo(maxBalance) > 0) {
            throw new IllegalStateException("Balance cannot exceed max balance");
        }
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
