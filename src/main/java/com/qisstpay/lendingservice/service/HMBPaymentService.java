package com.qisstpay.lendingservice.service;

import com.qisstpay.lendingservice.dto.hmb.request.SubmitTransactionRequestDto;
import com.qisstpay.lendingservice.dto.hmb.response.GetTokenResponseDto;
import com.qisstpay.lendingservice.dto.hmb.response.SubmitTransactionResponseDto;

public interface HMBPaymentService {

    GetTokenResponseDto getToken();
    SubmitTransactionResponseDto submitIFTTransaction(String token, SubmitTransactionRequestDto submitTransactionRequestDto);
    SubmitTransactionResponseDto submitIBFTTransaction(String token, SubmitTransactionRequestDto submitTransactionRequestDto);

}