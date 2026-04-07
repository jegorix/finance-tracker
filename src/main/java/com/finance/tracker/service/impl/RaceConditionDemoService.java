package com.finance.tracker.service.impl;

import com.finance.tracker.dto.response.RaceConditionDemoResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RaceConditionDemoService {

    private static final int THREAD_COUNT = 50;
    private static final int INCREMENTS_PER_THREAD = 1000;
    private static final int EXPECTED_VALUE = THREAD_COUNT * INCREMENTS_PER_THREAD;
    private static final int UNSAFE_ATTEMPTS = 3;
    private static final int FORCED_COLLISION_INTERVAL = 25;
    private static final int WORKER_READY_TIMEOUT_SECONDS = 10;

    public RaceConditionDemoResponse.CounterResult demonstrateRaceCondition() throws InterruptedException {
        List<RaceConditionDemoResponse.UnsafeAttempt> attempts = runUnsafeAttempts();
        return summarizeUnsafeAttempts(attempts);
    }

    public RaceConditionDemoResponse.CounterResult demonstrateSynchronizedSolution() throws InterruptedException {
        DemoCounters counters = new DemoCounters();
        executeConcurrentIncrements(iteration -> counters.incrementSynchronized());
        return buildCounterResult("Synchronized counter", counters.getSynchronizedValue());
    }

    public RaceConditionDemoResponse.CounterResult demonstrateAtomicSolution() throws InterruptedException {
        DemoCounters counters = new DemoCounters();
        executeConcurrentIncrements(iteration -> counters.incrementAtomic());
        return buildCounterResult("Atomic counter", counters.getAtomicValue());
    }

    public RaceConditionDemoResponse runAllDemos() throws InterruptedException {
        log.info("Starting race condition demonstration with {} threads", THREAD_COUNT);
        log.info("Each thread performs {} increments", INCREMENTS_PER_THREAD);
        log.info("Expected value: {}", EXPECTED_VALUE);

        List<RaceConditionDemoResponse.UnsafeAttempt> unsafeAttempts = runUnsafeAttempts();
        RaceConditionDemoResponse.CounterResult unsafeCounter = summarizeUnsafeAttempts(unsafeAttempts);
        RaceConditionDemoResponse.CounterResult synchronizedCounter = demonstrateSynchronizedSolution();
        RaceConditionDemoResponse.CounterResult atomicCounter = demonstrateAtomicSolution();
        String takeaway = buildTakeaway(unsafeCounter, synchronizedCounter, atomicCounter);

        return new RaceConditionDemoResponse(
                THREAD_COUNT,
                INCREMENTS_PER_THREAD,
                EXPECTED_VALUE,
                UNSAFE_ATTEMPTS,
                FORCED_COLLISION_INTERVAL,
                unsafeCounter,
                unsafeAttempts,
                synchronizedCounter,
                atomicCounter,
                takeaway);
    }

    private List<RaceConditionDemoResponse.UnsafeAttempt> runUnsafeAttempts() throws InterruptedException {
        List<RaceConditionDemoResponse.UnsafeAttempt> attempts = new ArrayList<>();

        for (int attempt = 1; attempt <= UNSAFE_ATTEMPTS; attempt++) {
            DemoCounters counters = new DemoCounters();
            executeUnsafeConcurrentIncrements(counters);
            attempts.add(buildUnsafeAttempt(attempt, counters.getUnsafeValue()));
        }

        return attempts;
    }

    private RaceConditionDemoResponse.CounterResult summarizeUnsafeAttempts(
            List<RaceConditionDemoResponse.UnsafeAttempt> attempts) {
        RaceConditionDemoResponse.UnsafeAttempt worstAttempt = attempts.stream()
                .max(Comparator.comparingInt(RaceConditionDemoResponse.UnsafeAttempt::getLostUpdates))
                .orElse(new RaceConditionDemoResponse.UnsafeAttempt(1, EXPECTED_VALUE, 0, false));

        String verdict = worstAttempt.isRaceConditionPresent()
                ? "RACE CONDITION OBSERVED"
                : "NO LOST UPDATES OBSERVED";

        return new RaceConditionDemoResponse.CounterResult(
                "Unsafe counter",
                worstAttempt.getActualValue(),
                worstAttempt.getLostUpdates(),
                !worstAttempt.isRaceConditionPresent(),
                verdict);
    }

    private RaceConditionDemoResponse.CounterResult buildCounterResult(String counterName, int actualValue) {
        int lostUpdates = EXPECTED_VALUE - actualValue;
        boolean matchesExpected = actualValue == EXPECTED_VALUE;
        String verdict = matchesExpected ? "SUCCESS" : "FAILURE";

        return new RaceConditionDemoResponse.CounterResult(
                counterName,
                actualValue,
                lostUpdates,
                matchesExpected,
                verdict);
    }

    private RaceConditionDemoResponse.UnsafeAttempt buildUnsafeAttempt(int attempt, int actualValue) {
        int lostUpdates = EXPECTED_VALUE - actualValue;
        return new RaceConditionDemoResponse.UnsafeAttempt(
                attempt,
                actualValue,
                lostUpdates,
                lostUpdates > 0);
    }

    private String buildTakeaway(
            RaceConditionDemoResponse.CounterResult unsafeCounter,
            RaceConditionDemoResponse.CounterResult synchronizedCounter,
            RaceConditionDemoResponse.CounterResult atomicCounter) {
        if (!unsafeCounter.isMatchesExpected()
                && synchronizedCounter.isMatchesExpected()
                && atomicCounter.isMatchesExpected()) {
            return "Unsafe increments lose updates under contention, "
                    + "while synchronized and atomic increments stay correct.";
        }
        return "Unsafe counter matched the expected value in this run, rerun the demo or increase contention.";
    }

    private void executeUnsafeConcurrentIncrements(DemoCounters counters) throws InterruptedException {
        Phaser forcedCollisionPhaser = new Phaser(THREAD_COUNT);
        executeConcurrentIncrements(iteration -> {
            if ((iteration + 1) % FORCED_COLLISION_INTERVAL == 0) {
                counters.incrementUnsafeWithForcedCollision(forcedCollisionPhaser);
                return;
            }
            counters.incrementUnsafe();
        });
    }

    private void executeConcurrentIncrements(IntConsumer incrementAction) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch readyGate = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);

        try {
            for (int worker = 0; worker < THREAD_COUNT; worker++) {
                executor.submit(() -> runWorker(incrementAction, readyGate, startGate));
            }

            releaseWorkers(readyGate, startGate);
            awaitTermination(executor);
        } catch (InterruptedException | RuntimeException exception) {
            executor.shutdownNow();
            throw exception;
        }
    }

    private void runWorker(IntConsumer incrementAction, CountDownLatch readyGate, CountDownLatch startGate) {
        readyGate.countDown();

        try {
            startGate.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return;
        }

        for (int iteration = 0; iteration < INCREMENTS_PER_THREAD; iteration++) {
            incrementAction.accept(iteration);
        }
    }

    private void releaseWorkers(CountDownLatch readyGate, CountDownLatch startGate) throws InterruptedException {
        if (!readyGate.await(WORKER_READY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            startGate.countDown();
            throw new IllegalStateException("Failed to prepare race-condition demo workers within 10 seconds");
        }
        startGate.countDown();
    }

    private void awaitTermination(ExecutorService executor) throws InterruptedException {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
                throw new IllegalStateException("Failed to complete race-condition demo within 1 minute");
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw exception;
        }
    }

    private static final class DemoCounters {

        private final AtomicInteger atomicCounter = new AtomicInteger();
        private int unsafeCounter;
        private int synchronizedCounter;

        private void incrementUnsafe() {
            unsafeCounter++;
        }

        private void incrementUnsafeWithForcedCollision(Phaser phaser) {
            int snapshot = unsafeCounter;
            phaser.arriveAndAwaitAdvance();
            unsafeCounter = snapshot + 1;
        }

        private int getUnsafeValue() {
            return unsafeCounter;
        }

        private synchronized void incrementSynchronized() {
            synchronizedCounter++;
        }

        private synchronized int getSynchronizedValue() {
            return synchronizedCounter;
        }

        private void incrementAtomic() {
            atomicCounter.incrementAndGet();
        }

        private int getAtomicValue() {
            return atomicCounter.get();
        }
    }
}
