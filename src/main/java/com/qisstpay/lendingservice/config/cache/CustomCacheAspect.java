package com.qisstpay.lendingservice.config.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Aspect
@Component
public class CustomCacheAspect {

    @Autowired
    public BeanFactory beanFactory;

    private HashMap<String, Long> entryTime = new HashMap<>();

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private ExpressionParser expressionParser;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    @Qualifier("redisCacheManager")
    CacheManager cacheManager;

    @Around("@annotation(customCache)")
    public Object customcache(ProceedingJoinPoint proceedingJoinPoint, CustomCache customCache) throws Throwable {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver(new BeanFactoryResolver(this.beanFactory));

        CacheManager cacheManagerToUse = cacheManager;
        if (!customCache.cacheManager().equals("")) {
            Expression cacheManagerExpression = expressionParser.parseExpression(customCache.cacheManager());
            cacheManager = (CacheManager) cacheManagerExpression.getValue(context);
        }

        String prefix = customCache.prefix();
        prefix = customCache.prefix().equals("") ? proceedingJoinPoint.getSignature().getName() : prefix;

        StringBuilder stringBuilder = new StringBuilder(prefix);

        for (Object part : proceedingJoinPoint.getArgs()) {
            //stringBuilder.append(":").append((String) expressionParser.parseExpression((String) part).getValue());
            stringBuilder.append(":").append(part);
        }
        String key = stringBuilder.toString();

        CacheEntry cacheEntry = objectMapper.convertValue(cacheManager.getCache(prefix).get(key) != null ? cacheManager.getCache(prefix).get(key).get() : null, new TypeReference<CacheEntry>() {
        });
        if (cacheEntry != null && !cacheEntry.getExpiresAt().isBefore(LocalDateTime.now())) {

            if (cacheEntry.getObject() instanceof List<?>) {
                return ((ArrayList<?>) cacheEntry.getObject()).stream().map(item -> (objectMapper).convertValue(item, cacheEntry.getListObjectClass())).collect(Collectors.toList());
            } else {
                Signature signature =  proceedingJoinPoint.getSignature();
                Class returnType = ((MethodSignature) signature).getReturnType();
               return  objectMapper.convertValue(cacheEntry.getObject(),returnType);
            }
        }


        Object object = proceedingJoinPoint.proceed(proceedingJoinPoint.getArgs());

        Long timeout = Long.valueOf(60000);
        try {
            Expression timeoutExpression = expressionParser.parseExpression(customCache.expiration());
            timeoutExpression = expressionParser.parseExpression((String) timeoutExpression.getValue(context));
            timeout = Long.valueOf(((Integer) timeoutExpression.getValue(context)).longValue());

        } catch (Exception e) {

        }

        Class c = null;

        try {
            if (object instanceof List<?>) {
                if (((List) object).size() > 0)
                    c = ((List) object).get(0).getClass();
            }
        } catch (Exception e) {

        }

        cacheManager.getCache(prefix).put(key, CacheEntry.builder().object(object).listObjectClass(c).expiresAt(LocalDateTime.now().plus(timeout, ChronoUnit.MILLIS)).build());

        return object;
    }

}