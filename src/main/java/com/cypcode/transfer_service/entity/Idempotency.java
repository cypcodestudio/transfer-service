package com.cypcode.transfer_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "idempotency")
public class Idempotency implements Serializable {
    @Id
    private String id;
    private Long transferId;
    private String response;
    private LocalDateTime expiryDate;
}
