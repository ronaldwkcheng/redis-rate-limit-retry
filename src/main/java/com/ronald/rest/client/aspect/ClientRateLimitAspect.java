package com.ronald.rest.client.aspect;

import com.ronald.rest.client.exception.RetryableServerException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
@Slf4j
public class ClientRateLimitAspect {

    private final Bucket userClientRateLimiterBucket;

    @Value("${client.rate-limiter.max-wait-ms}")
    private long maxWaitMillis;

    @Around("execution(* com.ronald.rest.client..*(..))")
    public Object throttle(ProceedingJoinPoint joinPoint) throws Throwable {
        ConsumptionProbe probe = userClientRateLimiterBucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long waitMillis = Duration.ofNanos(probe.getNanosToWaitForRefill()).toMillis();
            log.warn("Rate limit exceeded for {}; waiting up to {} ms (refill in {} ms)",
                    joinPoint.getSignature(), maxWaitMillis, waitMillis);
            boolean consumed = userClientRateLimiterBucket.asBlocking()
                    .tryConsume(1, Duration.ofMillis(maxWaitMillis));
            if (!consumed) {
                throw new RetryableServerException(HttpStatus.TOO_MANY_REQUESTS,
                        "Timed out after %d ms waiting for rate limiter refill for %s"
                                .formatted(maxWaitMillis, joinPoint.getSignature()));
            }
        }
        return joinPoint.proceed();
    }

}
