package com.wallet.repository;

import com.wallet.entity.TxnHistory;
import com.wallet.enums.TxnType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;

public interface TxnHistoryRepository
        extends JpaRepository<TxnHistory, Long>, JpaSpecificationExecutor<TxnHistory> {

    /**
     * Builds a dynamic predicate so optional filters never bind as untyped NULL
     * parameters (PostgreSQL error 42P18 / "could not determine data type").
     */
    static Specification<TxnHistory> withFilters(
            Long walletId,
            TxnType txnType,
            Instant startDate,
            Instant endDate
    ) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (walletId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("fromId"), walletId),
                        cb.equal(root.get("toId"), walletId),
                        cb.equal(root.get("wallet").get("id"), walletId)
                ));
            }
            if (txnType != null) {
                predicates.add(cb.equal(root.get("txnType"), txnType));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
