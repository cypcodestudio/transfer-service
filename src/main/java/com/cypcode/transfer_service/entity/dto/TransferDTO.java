package com.cypcode.transfer_service.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferDTO implements Serializable {
    private long transferId;
    @NotNull(message = "From account id is required")
    private long fromAccountId;
    @NotNull(message = "To account id is required")
    private long toAccountId;
    @NotNull(message = "amount is required")
    private BigDecimal amount;
}
