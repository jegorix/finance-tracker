package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.finance.tracker.dto.response.RaceConditionDemoResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RaceConditionDemoServiceTest {

    private final RaceConditionDemoService service = new RaceConditionDemoService();

    @Test
    void runAllDemosShouldReturnUnsafeFailureAndSafeSuccess() throws InterruptedException {
        RaceConditionDemoResponse response = service.runAllDemos();

        assertEquals(50, response.getThreadCount());
        assertEquals(1000, response.getIncrementsPerThread());
        assertEquals(50_000, response.getExpectedValue());
        assertEquals(3, response.getUnsafeAttemptsCount());
        assertEquals(25, response.getForcedCollisionInterval());
        assertEquals(3, response.getUnsafeAttempts().size());
        assertFalse(response.getUnsafeCounter().isMatchesExpected());
        assertTrue(response.getUnsafeCounter().getLostUpdates() > 0);
        assertTrue(response.getSynchronizedCounter().isMatchesExpected());
        assertTrue(response.getAtomicCounter().isMatchesExpected());
        assertTrue(response.getTakeaway().contains("Unsafe increments lose updates"));
    }

    @Test
    void demonstrateRaceConditionShouldClearlyLoseUpdates() throws InterruptedException {
        RaceConditionDemoResponse.CounterResult result = service.demonstrateRaceCondition();

        assertFalse(result.isMatchesExpected());
        assertTrue(result.getLostUpdates() > 0);
        assertEquals("RACE CONDITION OBSERVED", result.getVerdict());
    }

    @Test
    void demonstrateSafeCountersShouldReachExpectedValue() throws InterruptedException {
        RaceConditionDemoResponse.CounterResult synchronizedResult = service.demonstrateSynchronizedSolution();
        RaceConditionDemoResponse.CounterResult atomicResult = service.demonstrateAtomicSolution();

        assertEquals(50_000, synchronizedResult.getActualValue());
        assertEquals(0, synchronizedResult.getLostUpdates());
        assertTrue(synchronizedResult.isMatchesExpected());
        assertEquals("SUCCESS", synchronizedResult.getVerdict());

        assertEquals(50_000, atomicResult.getActualValue());
        assertEquals(0, atomicResult.getLostUpdates());
        assertTrue(atomicResult.isMatchesExpected());
        assertEquals("SUCCESS", atomicResult.getVerdict());
    }

    @Test
    void awaitTerminationShouldThrowWhenExecutorDoesNotFinishInTime() throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        when(executor.awaitTermination(1, TimeUnit.MINUTES)).thenReturn(false);

        Method awaitTerminationMethod = privateMethod("awaitTermination", ExecutorService.class);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> awaitTerminationMethod.invoke(service, executor));

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals(
                "Failed to complete race-condition demo within 1 minute",
                exception.getCause().getMessage());
        verify(executor).shutdown();
        verify(executor).shutdownNow();
    }

    @Test
    void awaitTerminationShouldShutdownAndRethrowInterruptedException() throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        InterruptedException interruptedException = new InterruptedException("interrupted");
        when(executor.awaitTermination(1, TimeUnit.MINUTES)).thenThrow(interruptedException);

        Method awaitTerminationMethod = privateMethod("awaitTermination", ExecutorService.class);

        try {
            InvocationTargetException exception = assertThrows(
                    InvocationTargetException.class,
                    () -> awaitTerminationMethod.invoke(service, executor));

            assertEquals(interruptedException, exception.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(executor).shutdown();
            verify(executor).shutdownNow();
        } finally {
            Thread.interrupted();
        }
    }

    private Method privateMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = RaceConditionDemoService.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }
}
