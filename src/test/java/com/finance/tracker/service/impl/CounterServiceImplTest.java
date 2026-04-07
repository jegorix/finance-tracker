package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CounterServiceImplTest {

    private CounterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CounterServiceImpl();
    }

    @Test
    void incrementMethodsShouldIncreaseDedicatedCounters() {
        service.incrementUnsafe();
        service.incrementUnsafe();
        service.incrementSynchronized();
        service.incrementAtomic();
        service.incrementAtomic();

        assertEquals(2, service.getUnsafeValue());
        assertEquals(1, service.getSynchronizedValue());
        assertEquals(2, service.getAtomicValue());
    }

    @Test
    void resetShouldClearAllCounters() {
        service.incrementUnsafe();
        service.incrementSynchronized();
        service.incrementAtomic();

        service.reset();

        assertEquals(0, service.getUnsafeValue());
        assertEquals(0, service.getSynchronizedValue());
        assertEquals(0, service.getAtomicValue());
    }
}
