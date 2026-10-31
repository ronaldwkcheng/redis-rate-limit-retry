package com.ronald.rest.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ronald.rest.client.exception.RetryableServerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryLoggingListenerTest {

    private final RetryLoggingListener listener = new RetryLoggingListener();

    @Mock
    private RetryContext context;

    @Mock
    private RetryCallback<String, Exception> callback;

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachLogAppender() {
        logAppender = new ListAppender<>();
        logAppender.start();
        listenerLogger().addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        listenerLogger().detachAppender(logAppender);
    }

    @Test
    void logsSuccessAfterAtLeastOneFailedAttempt() {
        when(context.getRetryCount()).thenReturn(2);
        when(context.getAttribute(RetryContext.NAME)).thenReturn("com.ronald.rest.client.UserClient.findById");

        listener.onSuccess(context, callback, "result");

        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent event = logAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage())
                .isEqualTo("Retry succeeded for com.ronald.rest.client.UserClient.findById after 2 failed attempt(s)");
    }

    @Test
    void doesNotLogWhenTheCallSucceededOnTheFirstAttempt() {
        when(context.getRetryCount()).thenReturn(0);

        listener.onSuccess(context, callback, "result");

        assertThat(logAppender.list).isEmpty();
    }

    @Test
    void logsEachFailedAttempt() {
        when(context.getRetryCount()).thenReturn(1);
        when(context.getAttribute(RetryContext.NAME)).thenReturn("com.ronald.rest.client.UserClient.findById");
        RetryableServerException exception = new RetryableServerException(HttpStatus.BAD_GATEWAY, "Received retryable status 502 BAD_GATEWAY");

        listener.onError(context, callback, exception);

        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent event = logAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).isEqualTo(
                "Retry attempt 1 for com.ronald.rest.client.UserClient.findById failed with "
                        + "RetryableServerException: Received retryable status 502 BAD_GATEWAY");
    }

    private Logger listenerLogger() {
        return (Logger) LoggerFactory.getLogger(RetryLoggingListener.class);
    }

}
