package com.vipul.claimq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "claimq")
public record ClaimQProperties(@DefaultValue Worker worker, @DefaultValue Retry retry, @DefaultValue Shutdown shutdown) {

    public record Worker(
            @DefaultValue("3") int count,
            @DefaultValue("1s") Duration pollInterval,
            @DefaultValue("60s") Duration leaseTimeout,
            @DefaultValue("true") boolean autoStart
    ) {
        public Worker {
            if (count < 1) {
                throw new IllegalArgumentException("claimq.worker.count must be at least 1");
            }
            if (leaseTimeout.isZero() || leaseTimeout.isNegative()) {
                throw new IllegalArgumentException("claimq.worker.lease-timeout must be positive");
            }
        }
    }

    public record Retry(
            @DefaultValue("5s") Duration baseDelay,
            @DefaultValue("60s") Duration maxDelay
    ) {
        public Retry {
            if (baseDelay.isZero() || baseDelay.isNegative()) {
                throw new IllegalArgumentException("claimq.retry.base-delay must be positive");
            }
            if (maxDelay.compareTo(baseDelay) < 0) {
                throw new IllegalArgumentException("claimq.retry.max-delay must not be below base-delay");
            }
        }
    }

    public record Shutdown(@DefaultValue("30s") Duration gracePeriod) {
        public Shutdown {
            if (gracePeriod.isNegative()) {
                throw new IllegalArgumentException("claimq.shutdown.grace-period must not be negative");
            }
        }
    }

}