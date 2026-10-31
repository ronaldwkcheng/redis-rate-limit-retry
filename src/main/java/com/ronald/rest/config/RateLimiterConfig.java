package com.ronald.rest.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class RateLimiterConfig {

    private static final byte[] USER_CLIENT_BUCKET_KEY =
            "rate-limiter:jsonplaceholder-user-client".getBytes(StandardCharsets.UTF_8);

    @Value("${client.rate-limiter.capacity}")
    private long capacity;

    @Value("${client.rate-limiter.refill-tokens}")
    private long refillTokens;

    @Value("${client.rate-limiter.refill-duration-seconds}")
    private long refillDurationSeconds;

    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient(RedisProperties redisProperties) {
        RedisURI redisUri = RedisURI.builder()
                .withHost(redisProperties.getHost())
                .withPort(redisProperties.getPort())
                .build();
        return RedisClient.create(redisUri);
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<byte[], byte[]> redisBucketConnection(RedisClient redisClient) {
        return redisClient.connect(ByteArrayCodec.INSTANCE);
    }

    @Bean
    public ProxyManager<byte[]> bucketProxyManager(StatefulRedisConnection<byte[], byte[]> redisBucketConnection) {
        return Bucket4jLettuce.casBasedBuilder(redisBucketConnection)
                .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
                .build();
    }

    @Bean
    public Bucket userClientRateLimiterBucket(ProxyManager<byte[]> bucketProxyManager) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(capacity)
                        .refillGreedy(refillTokens, Duration.ofSeconds(refillDurationSeconds)))
                .build();
        return bucketProxyManager.getProxy(USER_CLIENT_BUCKET_KEY, () -> configuration);
    }

}
