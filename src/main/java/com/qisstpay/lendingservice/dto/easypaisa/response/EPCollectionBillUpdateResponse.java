
package com.qisstpay.lendingservice.dto.easypaisa.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EPCollectionBillUpdateResponse {

    @JsonProperty("Identification_parameter")
    private String identificationParameter;
    @JsonProperty("response_Code")
    private String responseCode;
    @JsonProperty("reserved")
    private String reserved;
}
