package com.inhatc.auction.domain.auction.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import com.inhatc.auction.domain.transaction.entity.Transaction;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

class AuctionRepositoryTests {

    @Test
    void findByIdForUpdate_declaresPessimisticWriteLockWithoutRequiringMariaDb() throws NoSuchMethodException {
        Method method = AuctionRepository.class.getMethod("findByIdForUpdate", Long.class);

        Lock lock = method.getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(method.getReturnType()).isEqualTo(Optional.class);
    }

    @Test
    void transactionAuctionJoin_isOneToOneAndHasDatabaseUniqueConstraintWithoutRequiringMariaDb()
            throws NoSuchFieldException {
        Field auction = Transaction.class.getDeclaredField("auction");
        JoinColumn joinColumn = auction.getAnnotation(JoinColumn.class);
        Table table = Transaction.class.getAnnotation(Table.class);

        assertThat(auction.getAnnotation(OneToOne.class)).isNotNull();
        assertThat(joinColumn).isNotNull();
        assertThat(joinColumn.name()).isEqualTo("auction_id");
        assertThat(joinColumn.unique()).isTrue();
        assertThat(Arrays.stream(table.uniqueConstraints())
                .map(UniqueConstraint::columnNames)
                .anyMatch(columns -> Arrays.equals(columns, new String[] { "auction_id" })))
                .isTrue();
    }
}
