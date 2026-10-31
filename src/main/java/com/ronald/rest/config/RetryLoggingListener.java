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

}
