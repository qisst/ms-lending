package com.qisstpay.lendingservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qisstpay.commons.exception.CustomException;
import com.qisstpay.lendingservice.dto.easypaisa.request.EPLoginRequestDto;
import com.qisstpay.lendingservice.dto.easypaisa.request.EPRequestDto;
import com.qisstpay.lendingservice.dto.easypaisa.response.EPInquiryResponseDto;
import com.qisstpay.lendingservice.dto.easypaisa.response.EPLoginResponseDto;
import com.qisstpay.lendingservice.dto.easypaisa.response.EPTransferResposneDto;
import com.qisstpay.lendingservice.dto.internal.request.TransferRequestDto;
import com.qisstpay.lendingservice.dto.internal.response.TransactionStateResponse;
import com.qisstpay.lendingservice.dto.internal.response.TransferResponseDto;
import com.qisstpay.lendingservice.encryption.EncryptionUtil;
import com.qisstpay.lendingservice.entity.Consumer;
import com.qisstpay.lendingservice.entity.LendingTransaction;
import com.qisstpay.lendingservice.enums.QPResponseCode;
import com.qisstpay.lendingservice.enums.TransactionState;
import com.qisstpay.lendingservice.repository.ConsumerRepository;
import com.qisstpay.lendingservice.repository.LendingTransactionRepository;
import com.qisstpay.lendingservice.service.LendingService;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class LendingServiceImpl implements LendingService {


    @Value("${ep.endpoints.login}")
    private String epLoginUrl;

    @Value("${ep.endpoints.inquiry}")
    private String epInquiryUrl;

    @Value("${ep.endpoints.transfer}")
    private String epTransferUrl;

    @Value("${ep.config.msisdn}")
    private String msisdn;

    @Value("${ep.config.pin}")
    private String pin;

    private static final String SUCCESS_STATUS_CODE = "0";

    private final String xChanelHeaderKey = "X-Channel";
    private final String xChanelHeaderVal = "subgateway";
    private final String xClientIdHeaderKey = "X-IBM-Client-Id";
    private final String xClientIdHeaderVal = "0d9fe5ca-8147-4b05-a9af-c7ef2e0df3af";
    private final String xClientSecretHeaderKey = "X-IBM-Client-Secret";
    private final String xClientSecretHeaderVal = "I4lR4yW0uP4yW3eQ7rR4vL0bK0pX6mV5cS7cN4iL7rC6pG2cA1";
    private final String xHashValueKey = "X-Hash-Value";

    @Autowired
    private LendingTransactionRepository lendingTransactionRepository;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired
    private ConsumerRepository consumerRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public TransferResponseDto transfer(TransferRequestDto transferRequestDto) throws JsonProcessingException {

        if ( StringUtils.isBlank(transferRequestDto.getPhoneNumber()) ) {
            throw new CustomException(HttpStatus.BAD_REQUEST.toString(), "phone number is missing.");
        }

        // Consumer sign-up, if not already
        Consumer savedConsumer = null;
        Optional<Consumer> existingConsumer = consumerRepository.findByPhoneNumber(transferRequestDto.getPhoneNumber());
        if (!existingConsumer.isPresent()) {
            Consumer newConsumer = new Consumer();
            newConsumer.setPhoneNumber(transferRequestDto.getPhoneNumber());
            savedConsumer = consumerRepository.saveAndFlush(newConsumer);
        }

        //  persist lending transaction
        LendingTransaction lendingTransaction = new LendingTransaction();
        lendingTransaction.setAmount(transferRequestDto.getAmount());
        lendingTransaction.setIdentityNumber(transferRequestDto.getIdentityNumber());
        if (savedConsumer == null) {
            lendingTransaction.setConsumer(existingConsumer.get());
        } else {
            lendingTransaction.setConsumer(savedConsumer);
        }
        lendingTransaction.setUserName(transferRequestDto.getUserName());
        lendingTransaction.setTransactionState(TransactionState.RECEIVED);
        LendingTransaction savedLendingTransaction = lendingTransactionRepository.saveAndFlush(lendingTransaction);

        //  ep login call
        EPLoginRequestDto epLoginRequestDto = new EPLoginRequestDto();
        epLoginRequestDto.setLoginPayload(encryptionUtil.getEncryptedPayload( msisdn + ":" + pin ) );
        EPLoginResponseDto epLoginResponse = epLogin(epLoginRequestDto);
        if (!epLoginResponse.getResponseCode().equals(SUCCESS_STATUS_CODE)) {

            return TransferResponseDto
                    .builder()
                    .qpResponseCode(QPResponseCode.EP_LOGIN_FAILED.getCode())
                    .result(QPResponseCode.EP_LOGIN_FAILED.getDescription())
//                    .epResult(epLoginResponse)
                    .build();
        }

        // ep inquiry call
        final String xHashValueVal = encryptionUtil.getEncryptedPayload( msisdn + "~" + epLoginResponse.getTimestamp() + "~" + pin );
        EPRequestDto epRequestDto = new EPRequestDto();
        epRequestDto.setAmount(transferRequestDto.getAmount());
        epRequestDto.setSubscriberMSISDN(msisdn);
        epRequestDto.setReceiverMSISDN(transferRequestDto.getPhoneNumber());
        EPInquiryResponseDto epInquiryResponse = epInquiry(epRequestDto, xHashValueVal);

        // update lending transaction
        savedLendingTransaction.setEpInquiryResponse(new ObjectMapper().writeValueAsString(epInquiryResponse));
        savedLendingTransaction.setTransactionState(TransactionState.IN_PROGRESS);
        lendingTransactionRepository.saveAndFlush(savedLendingTransaction);

        if (!epInquiryResponse.getResponseCode().equals(SUCCESS_STATUS_CODE)) {

            return TransferResponseDto
                    .builder()
                    .qpResponseCode(QPResponseCode.EP_INQUIRY_FAILED.getCode())
                    .result(QPResponseCode.EP_INQUIRY_FAILED.getDescription())
//                    .epResult(epInquiryResponse)
                    .build();
        }

        // ep transfer call
        EPTransferResposneDto epTransferResponse = epTransfer(epRequestDto, xHashValueVal);

        // update lending transaction
        savedLendingTransaction.setEpTransferResponse((new ObjectMapper().writeValueAsString(epTransferResponse)));
        lendingTransactionRepository.saveAndFlush(savedLendingTransaction);

        if (epTransferResponse.getResponseCode().equals(SUCCESS_STATUS_CODE)) {

            savedLendingTransaction.setEpTransactionId(epTransferResponse.getTransactionReference());
            savedLendingTransaction.setTransactionState(TransactionState.COMPLETED);
            LendingTransaction finalSavedLendingTransaction = lendingTransactionRepository.saveAndFlush(savedLendingTransaction);

            return TransferResponseDto
                    .builder()
                    .qpResponseCode(QPResponseCode.SUCCESSFUL_EXECUTION.getCode())
                    .result(QPResponseCode.SUCCESSFUL_EXECUTION.getDescription())
//                    .epResult(epTransferResponse)
                    .transactionId(finalSavedLendingTransaction.getId().toString())
                    .build();
        }
        else {
            return TransferResponseDto
                    .builder()
                    .qpResponseCode(QPResponseCode.EP_TRANSFER_FAILED.getCode())
                    .result(QPResponseCode.EP_INQUIRY_FAILED.getDescription())
                    .epResult(epTransferResponse)
                    .build();
        }

    }

    @Override
    public TransactionStateResponse checkStatus(String transactionId) {
        Optional<LendingTransaction> lendingTransaction = lendingTransactionRepository.findById(Long.valueOf(transactionId));
        if (lendingTransaction.isPresent()) {
            TransactionStateResponse transactionStateResponse = new TransactionStateResponse();
            transactionStateResponse.setState(lendingTransaction.get().getTransactionState().toString());
            transactionStateResponse.setAmount(lendingTransaction.get().getAmount());
            transactionStateResponse.setIdentityNumber(lendingTransaction.get().getIdentityNumber());
            transactionStateResponse.setPhoneNumber(lendingTransaction.get().getConsumer().getPhoneNumber());
            transactionStateResponse.setTransactionId(lendingTransaction.get().getId().toString());
            transactionStateResponse.setUserName(lendingTransaction.get().getUserName());
            return transactionStateResponse;
        }
        throw new CustomException(HttpStatus.BAD_REQUEST.toString(), "transaction not found.");
    }

    @Override
    public TransferResponseDto checkCredirScore(TransferRequestDto transferRequestDto) {
        return null;
    }



    public EPLoginResponseDto epLogin(EPLoginRequestDto epLoginResponseDto){
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(xChanelHeaderKey, xChanelHeaderVal);
        headers.add(xClientIdHeaderKey, xClientIdHeaderVal);
        headers.add(xClientSecretHeaderKey, xClientSecretHeaderVal);
        HttpEntity request = new HttpEntity(epLoginResponseDto, headers);

        ResponseEntity<EPLoginResponseDto> epLoginResponse =
                restTemplate.exchange(epLoginUrl, HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                });
        return epLoginResponse.getBody();
    }

    public EPInquiryResponseDto epInquiry(EPRequestDto epRequestDto, String xHashValueVal){
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(xChanelHeaderKey, xChanelHeaderVal);
        headers.add(xClientIdHeaderKey, xClientIdHeaderVal);
        headers.add(xClientSecretHeaderKey, xClientSecretHeaderVal);
        headers.add(xHashValueKey, xHashValueVal);
        HttpEntity request = new HttpEntity(epRequestDto, headers);

        ResponseEntity<EPInquiryResponseDto> epInquiryResponse =
                restTemplate.exchange(epInquiryUrl, HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                });
        return epInquiryResponse.getBody();
    }

    public EPTransferResposneDto epTransfer(EPRequestDto epRequestDto, String xHashValueVal){
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.ALL));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(xChanelHeaderKey, xChanelHeaderVal);
        headers.add(xClientIdHeaderKey, xClientIdHeaderVal);
        headers.add(xClientSecretHeaderKey, xClientSecretHeaderVal);
        headers.add(xHashValueKey, xHashValueVal);
        HttpEntity request = new HttpEntity(epRequestDto, headers);
//        try {
//            String json = new ObjectMapper().writeValueAsString(epRequestDto);
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }
        ResponseEntity<EPTransferResposneDto> epTransferResponse =
                restTemplate.exchange(epTransferUrl, HttpMethod.POST, request, new ParameterizedTypeReference<>() {
                });
        return epTransferResponse.getBody();
    }
}