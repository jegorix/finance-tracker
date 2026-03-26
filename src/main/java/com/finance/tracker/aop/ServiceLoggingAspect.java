package com.finance.tracker.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ServiceLoggingAspect {

    @Around("within(com.finance.tracker.service.impl..*)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime();
        String method = joinPoint.getSignature().toShortString();

        try {
            Object result = joinPoint.proceed();
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            log.info("Service method {} completed in {} ms", method, executionTimeMs);
            return result;
        } catch (Throwable exception) {
            long executionTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            log.warn(
                    "Service method {} failed in {} ms: {}",
                    method,
                    executionTimeMs,
                    exception.getMessage());
            throw exception;
        }
    }
}
