package com.cypcode.transfer_service.repository;

import com.cypcode.transfer_service.entity.Idempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface IIdempotencyRepository extends JpaRepository<Idempotency, String> {

    Idempotency findIdempotencyById(String id);
    Idempotency findIdempotencyByTransferId(long transferId);

}
