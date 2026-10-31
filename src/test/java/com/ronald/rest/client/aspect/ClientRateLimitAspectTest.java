package com.ronald.rest.client.aspect;

import io.github.bucket4j.BlockingBucket;
import io.github.bucket4j.Bucket;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientRateLimitAspectTest {

    @Mock
    private Bucket bucket;

    @Mock
    private BlockingBucket blockingBucket;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Test
    void consumesATokenBeforeProceedingWithTheCall() throws Throwable {
        when(bucket.asBlocking()).thenReturn(blockingBucket);
        when(joinPoint.proceed()).thenReturn("result");

        Object result = new ClientRateLimitAspect(bucket).throttle(joinPoint);

        assertThat(result).isEqualTo("result");
        InOrder inOrder = inOrder(blockingBucket, joinPoint);
        inOrder.verify(blockingBucket).consume(1);
        inOrder.verify(joinPoint).proceed();
    }

    @Test
    void doesNotProceedWhenBlockedWaitForATokenIsInterrupted() throws Throwable {
        when(bucket.asBlocking()).thenReturn(blockingBucket);
        doThrow(new InterruptedException("interrupted while waiting for refill")).when(blockingBucket).consume(1);

        ClientRateLimitAspect aspect = new ClientRateLimitAspect(bucket);

        assertThatThrownBy(() -> aspect.throttle(joinPoint)).isInstanceOf(InterruptedException.class);
        verify(joinPoint, never()).proceed();
    }

}
