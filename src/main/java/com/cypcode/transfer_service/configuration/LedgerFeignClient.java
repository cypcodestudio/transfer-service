package com.cypcode.transfer_service.configuration;

import com.cypcode.transfer_service.entity.dto.TransferDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "ledger-feign-client", url = "${cypcode.service.ledger.base-url}")
public interface LedgerFeignClient {
    @PostMapping(path = "${cypcode.service.ledger.transfer}")
    @CircuitBreaker(name = "ledger-cb-transfer", fallbackMethod = "createTransferFallback")
    String createTransfer(@RequestBody TransferDTO payload);

    default String createTransferFallback(@RequestBody TransferDTO payload, Throwable throwable) {
        System.out.println("Create Transfer Fallback");
        System.out.println(String.format("Create Transfer Exception: %s", throwable.getMessage()));
        return "FAILURE";
    }


    @GetMapping(path = "${cypcode.service.ledger.transfer}/{id}")
    @CircuitBreaker(name = "ledger-cb-transfer", fallbackMethod = "getTransferFallback")
    TransferDTO getTransfer(@PathVariable("id") long id);

    default TransferDTO getTransferFallback(@PathVariable("id") long id, Throwable throwable) {
        System.out.println("Get Transfer Fallback");
        System.out.println(String.format("Get Transfer Exception: %s", throwable.getMessage()));
        return new TransferDTO();
    }
}
