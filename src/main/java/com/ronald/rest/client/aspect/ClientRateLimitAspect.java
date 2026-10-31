package com.ronald.rest.client.aspect;

import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class ClientRateLimitAspect {

    private final Bucket userClientRateLimiterBucket;

    @Around("execution(* com.ronald.rest.client..*(..))")
    public Object throttle(ProceedingJoinPoint joinPoint) throws Throwable {
        userClientRateLimiterBucket.asBlocking().consume(1);
        return joinPoint.proceed();
    }

}
