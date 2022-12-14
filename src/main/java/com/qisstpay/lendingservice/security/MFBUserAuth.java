package com.qisstpay.lendingservice.security;

import com.qisstpay.commons.exception.CustomException;
import com.qisstpay.lendingservice.dto.easypaisa.response.EPCollectionInquiryResponse;
import com.qisstpay.lendingservice.dto.internal.request.VerifyMFBRequestDto;
import com.qisstpay.lendingservice.dto.tasdeeq.request.TasdeeqAuthRequestDto;
import com.qisstpay.lendingservice.entity.User;
import com.qisstpay.lendingservice.enums.TransferState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
@Slf4j
public class MFBUserAuth {

    @Value("${security.jwt.token.verify-mfb}")
    String verifyMFBt;

    @Autowired
    RestTemplate restTemplate;

    public void verifyUser(String userName, String password) {
//
//        if (!checkUserCredentials(VerifyMFBRequestDto.builder().username(userName).password(password).build())) {
//            log.info("Token verification: {}", HttpStatus.UNAUTHORIZED);
//            throw new CustomException(HttpStatus.UNAUTHORIZED.toString(), String.format("Token verification: %s", HttpStatus.UNAUTHORIZED));
//        }
    }

    public boolean isUserVerified(String userName, String password, EPCollectionInquiryResponse epCollectionInquiryResponse) {

        if (!checkUserCredentials(VerifyMFBRequestDto.builder().username(userName).password(password).build(), epCollectionInquiryResponse)) {
            log.info("Token verification: {}", HttpStatus.UNAUTHORIZED);
            return false;
        }
        return true;
    }

    private Boolean checkUserCredentials(final VerifyMFBRequestDto verifyMFBRequestDto, EPCollectionInquiryResponse epCollectionInquiryResponse) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<VerifyMFBRequestDto> requestEntity = new HttpEntity<>(verifyMFBRequestDto, headers);
            ResponseEntity<Object> responseEntity = restTemplate.exchange(
                    verifyMFBt, HttpMethod.POST, requestEntity, Object.class);
            if (responseEntity.getStatusCode().equals(HttpStatus.OK)) {
                log.info("MFB verification: {}", HttpStatus.OK);
                return Boolean.TRUE;
            }
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                log.info("Token verification: {}", HttpStatus.UNAUTHORIZED);
                epCollectionInquiryResponse.setResponseCode(TransferState.UNKNOWN_ERROR.getCode());
                epCollectionInquiryResponse.setResponseMessage(TransferState.UNKNOWN_ERROR.getState());
                epCollectionInquiryResponse.setStatus(TransferState.GATEWAY_TRANSFER_PENDING.getDescription());
            } else if (ex.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
                log.info("Token verification: {}", HttpStatus.FORBIDDEN);
                epCollectionInquiryResponse.setResponseCode(TransferState.UNKNOWN_ERROR.getCode());
                epCollectionInquiryResponse.setResponseMessage(TransferState.UNKNOWN_ERROR.getState());
                epCollectionInquiryResponse.setStatus(TransferState.GATEWAY_TRANSFER_PENDING.getDescription());            }
        }
        return Boolean.FALSE;
    }

}
