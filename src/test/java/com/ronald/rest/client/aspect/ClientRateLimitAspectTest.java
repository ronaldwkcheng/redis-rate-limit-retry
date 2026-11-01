package com.ronald.rest.client.aspect;

import com.ronald.rest.client.exception.RetryableServerException;
import io.github.bucket4j.BlockingBucket;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientRateLimitAspectTest {

    private static final long MAX_WAIT_MILLIS = 500;

    @Mock
    private Bucket bucket;

    @Mock
    private BlockingBucket blockingBucket;

    @Mock
    private ProceedingJoinPoint joinPoint;

    private ClientRateLimitAspect newAspect() {
        ClientRateLimitAspect aspect = new ClientRateLimitAspect(bucket);
        ReflectionTestUtils.setField(aspect, "maxWaitMillis", MAX_WAIT_MILLIS);
        return aspect;
    }

    @Test
    void proceedsImmediatelyWhenATokenIsAvailable() throws Throwable {
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(ConsumptionProbe.consumed(4, 0));
        when(joinPoint.proceed()).thenReturn("result");

        Object result = newAspect().throttle(joinPoint);

        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();
        verify(bucket, never()).asBlocking();
    }

    @Test
    void blocksForTheRefillWindowWhenRateLimitIsExceeded() throws Throwable {
        long nanosToWait = Duration.ofMillis(100).toNanos();
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(ConsumptionProbe.rejected(0, nanosToWait, nanosToWait));
        when(bucket.asBlocking()).thenReturn(blockingBucket);
        when(blockingBucket.tryConsume(1, Duration.ofMillis(MAX_WAIT_MILLIS))).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("result");

        Object result = newAspect().throttle(joinPoint);

        assertThat(result).isEqualTo("result");
        InOrder inOrder = inOrder(blockingBucket, joinPoint);
        inOrder.verify(blockingBucket).tryConsume(1, Duration.ofMillis(MAX_WAIT_MILLIS));
        inOrder.verify(joinPoint).proceed();
    }

    @Test
    void doesNotProceedWhenBlockedWaitForATokenIsInterrupted() throws Throwable {
        long nanosToWait = Duration.ofMillis(100).toNanos();
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(ConsumptionProbe.rejected(0, nanosToWait, nanosToWait));
        when(bucket.asBlocking()).thenReturn(blockingBucket);
        doThrow(new InterruptedException("interrupted while waiting for refill"))
                .when(blockingBucket).tryConsume(eq(1L), eq(Duration.ofMillis(MAX_WAIT_MILLIS)));

        ClientRateLimitAspect aspect = newAspect();

        assertThatThrownBy(() -> aspect.throttle(joinPoint)).isInstanceOf(InterruptedException.class);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void throwsRetryableServerExceptionWhenMaxWaitIsExceededWithoutAToken() throws Throwable {
        long nanosToWait = Duration.ofMillis(100).toNanos();
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(ConsumptionProbe.rejected(0, nanosToWait, nanosToWait));
        when(bucket.asBlocking()).thenReturn(blockingBucket);
        when(blockingBucket.tryConsume(1, Duration.ofMillis(MAX_WAIT_MILLIS))).thenReturn(false);

        ClientRateLimitAspect aspect = newAspect();

        assertThatThrownBy(() -> aspect.throttle(joinPoint))
                .isInstanceOf(RetryableServerException.class)
                .extracting(ex -> ((RetryableServerException) ex).getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        verify(joinPoint, never()).proceed();
    }

}
