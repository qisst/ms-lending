package com.qisstpay.lendingservice.config.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomCache {

    String prefix() default "";

    //todo: implement custom key parts not all args @CustomCache(keyParts={"#pageNo", "#pageSize"},...)
    String[] keyParts() default {};

    String key() default "";

    String cacheManager() default "";

    String expiration() default "";
}