package com.qisstpay.lendingservice.controller;

import com.qisstpay.commons.response.CustomResponse;
import com.qisstpay.lendingservice.dto.internal.request.TransferRequestDto;
import com.qisstpay.lendingservice.dto.internal.response.TransferResponseDto;
import com.qisstpay.lendingservice.service.LendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/lending/v1")
@RequiredArgsConstructor
public class LendingController {


    private static final String TRANSFER = "/transfer";

    private final LendingService lendingService;

    @PostMapping(TRANSFER)
    public CustomResponse<TransferResponseDto> transfer(@RequestBody TransferRequestDto transferRequestDto) {
        return CustomResponse.CustomResponseBuilder.<TransferResponseDto>builder()
                .body(lendingService.transfer(transferRequestDto)).build();
    }
}
