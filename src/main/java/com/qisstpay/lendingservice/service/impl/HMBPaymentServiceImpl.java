package com.qisstpay.lendingservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qisstpay.lendingservice.dto.hmb.request.SubmitTransactionRequestDto;
import com.qisstpay.lendingservice.dto.hmb.response.GetTokenResponseDto;
import com.qisstpay.lendingservice.dto.hmb.response.SubmitTransactionResponseDto;
import com.qisstpay.lendingservice.service.HMBPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.logging.Logger;

@Service
@Slf4j
public class HMBPaymentServiceImpl implements HMBPaymentService {

    private String hmbserviceBaseUrl = "http://172.27.81.112";
    
    private String getTokenAPIBasePath = "/TransPaymentAPI/Transaction/GetToken";
    private String submitIFTTransactionBasePath = "/TransPaymentAPI/Transaction/TransSubmit";
    private String submitIBFTTransactionBasePath = "/TransPaymentAPI/Transaction/TransSubmit";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public GetTokenResponseDto getToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("UserId", "EFAPI");
        headers.add("Password", "CRA");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        GetTokenResponseDto getTokenResponseDto = null;

        try{
            String response = restTemplate.exchange(hmbserviceBaseUrl + getTokenAPIBasePath, HttpMethod.GET, requestEntity, String.class).getBody();
            getTokenResponseDto = objectMapper.readValue(response, GetTokenResponseDto.class);
        }catch (Exception e){

        }
        return getTokenResponseDto;
    }

    @Override
    public SubmitTransactionResponseDto submitIFTTransaction(String authToken, SubmitTransactionRequestDto submitTransactionRequestDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("UserId", "EFAPI");
        headers.add("Password", "CRA");
        headers.add("Authorization", "Bearer "+authToken);
        HttpEntity<SubmitTransactionRequestDto> requestEntity = new HttpEntity<SubmitTransactionRequestDto>(submitTransactionRequestDto, headers);

        SubmitTransactionResponseDto submitTransactionResponseDto = null;

        try{
            String response = restTemplate.exchange(hmbserviceBaseUrl + submitIFTTransactionBasePath, HttpMethod.POST, requestEntity, String.class).getBody();
            submitTransactionResponseDto =  objectMapper.readValue(response, SubmitTransactionResponseDto.class);
        }catch (Exception e){

        }

        return submitTransactionResponseDto;
    }

    @Override
    public SubmitTransactionResponseDto submitIBFTTransaction(String authToken, SubmitTransactionRequestDto submitTransactionRequestDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("UserId", "EFAPI");
        headers.add("Password", "CRA");
        headers.add("Authorization", "Bearer "+authToken);
        HttpEntity<SubmitTransactionRequestDto> requestEntity = new HttpEntity<>(submitTransactionRequestDto, headers);

        SubmitTransactionResponseDto submitTransactionResponseDto = null;

        try{
            String response = restTemplate.exchange(hmbserviceBaseUrl + submitIFTTransactionBasePath, HttpMethod.POST, requestEntity, String.class).getBody();
            log.info("HMB IBFT Response: "+response);
            submitTransactionResponseDto =  objectMapper.readValue(response, SubmitTransactionResponseDto.class);
        }catch (Exception e){
            throw e;
        }

        return submitTransactionResponseDto;
    }
}