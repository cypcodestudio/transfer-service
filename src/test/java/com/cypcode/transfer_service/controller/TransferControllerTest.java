package com.cypcode.transfer_service.controller;

import com.cypcode.transfer_service.configuration.LedgerFeignClient;
import com.cypcode.transfer_service.entity.dto.IdempotencyDTO;
import com.cypcode.transfer_service.entity.dto.TransferDTO;
import com.cypcode.transfer_service.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class TransferControllerTest {
    @Mock
    private LedgerFeignClient ledgerFeignClient;

    @Mock
    private TransferService transferService;

    @InjectMocks
    private TransferController transferController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private final String url = "http://localhost:8080/transfers";

    @BeforeEach
    public void init(){
        mockMvc = MockMvcBuilders.standaloneSetup(transferController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testCreateTransfer() throws Exception {
        String idempotencyKey = "18ad19fa-c93a-484c-a964-e46a0fb7f3c6";
        TransferDTO payload = TransferDTO.builder()
                .transferId(123456789)
                .fromAccountId(12345)
                .toAccountId(56789)
                .amount(BigDecimal.valueOf(200))
                .build();
        IdempotencyDTO response = IdempotencyDTO.builder()
                .id(idempotencyKey)
                .transferId(123456789l)
                .response("SUCCESS")
                .expiryDate(LocalDateTime.now().plusHours(2))
                .build();
        lenient().when(ledgerFeignClient.createTransfer(payload)).thenReturn(ResponseEntity.ok("SUCCESS"));
        lenient().when(transferService.createTransferWithIndempotency(payload, idempotencyKey)).thenReturn(response);
        mockMvc.perform(post(url)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().is2xxSuccessful());

        verify(transferService, times(1)).createTransferWithIndempotency(payload, idempotencyKey);
    }

    @Test
    public void testGetTransfer() throws Exception {
        String idempotencyKey = "18ad19fa-c93a-484c-a964-e46a0fb7f3c6";
        IdempotencyDTO response = IdempotencyDTO.builder()
                .id(idempotencyKey)
                .transferId(123456789l)
                .response("SUCCESS")
                .expiryDate(LocalDateTime.now().plusHours(2))
                .build();
        lenient().when(transferService.getTransferById(response.getTransferId())).thenReturn(response);
        mockMvc.perform(get(url + "/{id}", response.getTransferId()))
                .andExpect(status().is2xxSuccessful());

        verify(transferService, times(1)).getTransferById(response.getTransferId());
    }

    @Test
    public void testCreateBatchTransfer() throws Exception {
        TransferDTO payload = TransferDTO.builder()
                .transferId(123456789)
                .fromAccountId(12345)
                .toAccountId(56789)
                .amount(BigDecimal.valueOf(200))
                .build();
        List<TransferDTO> payloads = List.of(payload);
        lenient().when(ledgerFeignClient.createTransfer(payload)).thenReturn(ResponseEntity.ok("SUCCESS"));
        mockMvc.perform(post(url + "/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payloads)))
                .andExpect(status().is2xxSuccessful());

        verify(transferService, times(1)).batchTransfer(payloads);
    }

}
