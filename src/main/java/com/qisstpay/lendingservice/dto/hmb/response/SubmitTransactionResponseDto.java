package com.qisstpay.lendingservice.dto.hmb.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitTransactionResponseDto {
    private String channelID;
    private String productCode;
}