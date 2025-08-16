package com.cypcode.transfer_service.service.implementation;

import com.cypcode.transfer_service.common.exception.AccountNotFoundException;
import com.cypcode.transfer_service.common.exception.InsufficienetFundsException;
import com.cypcode.transfer_service.configuration.LedgerFeignClient;
import com.cypcode.transfer_service.entity.Idempotency;
import com.cypcode.transfer_service.entity.dto.IdempotencyDTO;
import com.cypcode.transfer_service.entity.dto.TransferDTO;
import com.cypcode.transfer_service.repository.IIdempotencyRepository;
import com.cypcode.transfer_service.service.TransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@EnableAsync
@Service
public class TransferServiceImpl implements TransferService {

    public static final int IDEMPOTENCY_KEY_EXPIRATION_HOURS = 2;

    @Autowired
    private LedgerFeignClient ledgerFeignClient;

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
            ResponseEntity<String> response = ledgerFeignClient.createTransfer(transferDTO);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }else if(response.getStatusCode().value() == HttpStatus.PRECONDITION_FAILED.value()){
                throw new InsufficienetFundsException(response.getBody());
            }else if(response.getStatusCode().value() == HttpStatus.NOT_FOUND.value()){
                throw new AccountNotFoundException(response.getBody());
            }else{
              throw new ResponseStatusException(response.getStatusCode(), response.getBody());
            }
        }catch (Exception e){
            throw e;
        }
    }

    @Override
    public IdempotencyDTO createTransferWithIndempotency(TransferDTO transferDTO, String id) {
        Idempotency idempotency = idempotencyRepository.findIdempotencyById(id);
        if (idempotency != null && idempotency.getExpiryDate().isAfter(LocalDateTime.now())) {
            return mapToIdempotencyDTO(idempotency);
        }else{
            long transferId = atomicTransferId.incrementAndGet();
            transferDTO.setTransferId(transferId);
            String response = createTransfer(transferDTO);
            Idempotency idempotencyEntry = addIdempotencyEntry(id, response, transferId);
            return mapToIdempotencyDTO(idempotencyEntry);
        }

    }

    @Override
    public void batchTransfer(List<TransferDTO> transferDTOList) throws ExecutionException, InterruptedException {
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (TransferDTO transferDTO : transferDTOList) {
            CompletableFuture<String> result = processItem(transferDTO);
            futures.add(result);
        }

        CompletableFuture<Void> allOfFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        CompletableFuture<List<String>> allResultsFuture = allOfFutures.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));

        List<String> results = allResultsFuture.get();
        log.info("Batch transfer results: {}", results);
    }

    @Async
    public CompletableFuture<String> processItem(TransferDTO item) {
        Idempotency idempotency = idempotencyRepository.findIdempotencyByTransferId(item.getTransferId());
        if (idempotency != null && idempotency.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.info("Idempotency: {}, status: {}", idempotency.getTransferId(), idempotency.getResponse());
            return CompletableFuture.completedFuture(String.format("Processed: %s, %s", item.getTransferId(), idempotency.getResponse()));
        }else{
            ResponseEntity<String> result = ledgerFeignClient.createTransfer(item);
            if(result.getStatusCode().is2xxSuccessful()){
                addIdempotencyEntry(String.valueOf(item.getTransferId()), result.getBody(), item.getTransferId());
            }
            log.info("Transfer: {}, status: {}", item.getTransferId(), result.getBody());
            return CompletableFuture.completedFuture(String.format("Processed: %s, %s", item.getTransferId(), result.getBody()));
        }
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
