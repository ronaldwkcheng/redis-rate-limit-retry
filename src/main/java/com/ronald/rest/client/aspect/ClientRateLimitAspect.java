package com.ronald.rest.client.aspect;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
@Slf4j
public class ClientRateLimitAspect {

    private final Bucket userClientRateLimiterBucket;

    @Around("execution(* com.ronald.rest.client..*(..))")
    public Object throttle(ProceedingJoinPoint joinPoint) throws Throwable {
        ConsumptionProbe probe = userClientRateLimiterBucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long waitMillis = Duration.ofNanos(probe.getNanosToWaitForRefill()).toMillis();
            log.warn("Rate limit exceeded for {}; waiting {} ms for the next refill window",
                    joinPoint.getSignature(), waitMillis);
            userClientRateLimiterBucket.asBlocking().consume(1);
        }
        return joinPoint.proceed();
    }

}
