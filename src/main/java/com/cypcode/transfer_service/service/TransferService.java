package com.cypcode.transfer_service.service;

import com.cypcode.transfer_service.entity.dto.IdempotencyDTO;
import com.cypcode.transfer_service.entity.dto.TransferDTO;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface TransferService {
    public IdempotencyDTO getTransferById(long id);
    public String createTransfer(TransferDTO transferDTO);
    public IdempotencyDTO createTransferWithIndempotency(TransferDTO transferDTO, String indempotencyId);
    public void batchTransfer(List<TransferDTO> transferDTOList) throws ExecutionException, InterruptedException;
}
