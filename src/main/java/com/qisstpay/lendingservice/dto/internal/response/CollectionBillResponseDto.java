package com.qisstpay.lendingservice.dto.internal.response;

import com.qisstpay.lendingservice.enums.BillStatusType;
import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CollectionBillResponseDto {
    private Long           billId;
    private Double         amount;
    private Double         amountAfterDueDate;
    private String         userName;
    private String         serviceTransactionId;
    private Timestamp      dueDate;
    private BillStatusType billStatus;
    private String         consumerId;
    private String         consumerEmail;
}
