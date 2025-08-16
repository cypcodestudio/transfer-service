package com.cypcode.transfer_service.service.implementation;

import com.cypcode.transfer_service.configuration.LedgerFeignClient;
import com.cypcode.transfer_service.entity.Idempotency;
import com.cypcode.transfer_service.entity.dto.IdempotencyDTO;
import com.cypcode.transfer_service.entity.dto.TransferDTO;
import com.cypcode.transfer_service.repository.IIdempotencyRepository;
import com.cypcode.transfer_service.service.TransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TransferServiceImpl implements TransferService {

    public static final int IDEMPOTENCY_KEY_EXPIRATION_HOURS = 2;

    @Autowired
    private static LedgerFeignClient ledgerFeignClient;

    @Autowired
    private IIdempotencyRepository idempotencyRepository;

    private AtomicInteger atomicTransferId = new AtomicInteger(1000000);

    @Override
    public IdempotencyDTO getTransferById(long id) {
        try {
            Idempotency response = idempotencyRepository.findIdempotencyByTransferId(id);
           if (response != null) {
               return mapToIdempotencyDTO(response);
           }
            return null;
        }catch (Exception e){
            throw e;
        }
    }

    @Override
    public String createTransfer(TransferDTO transferDTO) {
        try {
            return ledgerFeignClient.createTransfer(transferDTO);
        }catch (Exception e){
            throw e;
        }
    }

    @Override
    public IdempotencyDTO createTransferWithIndempotency(TransferDTO transferDTO, String id) {
        Idempotency idempotency = idempotencyRepository.findIdempotencyById(id);
        if (idempotency != null && idempotency.getExpiryDate().isBefore(LocalDateTime.now())) {
            return mapToIdempotencyDTO(idempotency);
        }

        long transferId = atomicTransferId.incrementAndGet();
        transferDTO.setTransferId(transferId);
        String response = createTransfer(transferDTO);
        Idempotency idempotencyEntry = addIdempotencyEntry(id, response, transferId);
        return mapToIdempotencyDTO(idempotencyEntry);

    }

    @Override
    public void batchTransfer(List<TransferDTO> transferDTOList) throws ExecutionException, InterruptedException {
        List<CompletableFuture<String>> futures = transferDTOList.stream()
                .map(TransferServiceImpl::processItem)
                .collect(Collectors.toList());

        CompletableFuture<Void> allOfFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        CompletableFuture<List<String>> allResultsFuture = allOfFutures.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));

        List<String> results = allResultsFuture.get();
        log.info("Batch transfer results: {}", results);
    }

    public static CompletableFuture<String> processItem(TransferDTO item) {
        return CompletableFuture.supplyAsync(() -> {
            ledgerFeignClient.createTransfer(item);
            return "Processed: " + item;
        });
    }

    private Idempotency addIdempotencyEntry(String id, String response, long transferId) {
        return idempotencyRepository.save(Idempotency.builder()
                .id(id)
                .transferId(transferId)
                .response(response)
                .expiryDate(LocalDateTime.now().plusHours(IDEMPOTENCY_KEY_EXPIRATION_HOURS))
                .build());
    }

    private IdempotencyDTO mapToIdempotencyDTO(Idempotency idempotency) {
        log.info("Idempotency: Key: {}, Transfer Id: {}", idempotency.getId(), idempotency.getTransferId());
        return IdempotencyDTO.builder()
                .id(idempotency.getId())
                .response(idempotency.getResponse())
                .expiryDate(idempotency.getExpiryDate())
                .transferId(idempotency.getTransferId())
                .build();
    }
}
