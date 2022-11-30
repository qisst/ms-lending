package com.qisstpay.lendingservice.config.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qisstpay.lendingservice.config.RedisConfig;
import com.qisstpay.lendingservice.service.TasdeeqService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CacheHelper {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    TasdeeqService tasdeeqService;

    @Autowired
    private RedisConfig redisConfig;

    private final String AUTH_TOKEN_ID = "getLastAuthTokenId";
    private final String AUTH          = "authentication";
    private final String KEY           = "%s:%s";


    public void removeAuthTokenAndIdFromCache(Long id) {
        cacheManager.getCache(AUTH).evictIfPresent(String.format(KEY, AUTH, id));
        cacheManager.getCache(AUTH_TOKEN_ID).evictIfPresent(AUTH_TOKEN_ID);
    }

}
