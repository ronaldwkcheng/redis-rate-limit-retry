package com.ronald.rest.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RetryLoggingListener implements RetryListener {

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
            Throwable throwable) {
        log.warn("Retry attempt {} for {} failed with {}: {}",
                context.getRetryCount(), context.getAttribute(RetryContext.NAME),
                throwable.getClass().getSimpleName(), throwable.getMessage());
    }

    @Override
    public <T, E extends Throwable> void onSuccess(RetryContext context, RetryCallback<T, E> callback, T result) {
        if (context.getRetryCount() > 0) {
            log.info("Retry succeeded for {} after {} failed attempt(s)",
                    context.getAttribute(RetryContext.NAME), context.getRetryCount());
        }
    }

}
