package com.cypcode.transfer_service.controller;

import com.cypcode.transfer_service.entity.dto.IdempotencyDTO;
import com.cypcode.transfer_service.entity.dto.TransferDTO;
import com.cypcode.transfer_service.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("transfers")
@Tag(name = "Transfers", description = "APIs for managing transfers")
public class TransferController {

    @Autowired
    private TransferService transferService;

    @Operation(summary = "Create a new transfers", description = "Add a new transfers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "transfer completed successfully",
                    content = @Content(schema = @Schema(implementation = IdempotencyDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "500", description = "internal server error",
                    content = @Content(schema = @Schema()))
    })
    @PostMapping()
    public ResponseEntity<?> createTransfer(@RequestHeader(name = "Idempotency-Key") String idempotencyKey,@RequestBody TransferDTO payload) {
        try {
            IdempotencyDTO response = transferService.createTransferWithIndempotency(payload, idempotencyKey);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }catch (Exception e){
            log.error("Transfer request failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Operation(summary = "Get transfer by id", description = "Retrieve transfer status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "transfer status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "internal server error",
                    content = @Content(schema = @Schema()))
    })
    @GetMapping("{id}")
    public ResponseEntity<?> getTransferStatus(@PathVariable long id){
        try {
            return ResponseEntity.ok().body(transferService.getTransferById(id));
        }catch (Exception e){
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @Operation(summary = "Create a new transfers", description = "Add a new transfers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "transfer completed successfully",
                    content = @Content(schema = @Schema(implementation = TransferDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data",
                    content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "500", description = "internal server error",
                    content = @Content(schema = @Schema()))
    })
    @PostMapping("batch")
    public ResponseEntity<?> createBatchTransfer(@RequestBody
                                                 @NotEmpty(message = "Item list cannot be empty")
                                                 @Size(min = 1, max = 20, message = "Item list size must be between 1 and 20")
                                                 List<@Valid TransferDTO> payload) {
        try {
            transferService.batchTransfer(payload);
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }catch (Exception e){
            log.error("Transfer request failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
