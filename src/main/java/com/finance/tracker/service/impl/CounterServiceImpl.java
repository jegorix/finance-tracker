package com.finance.tracker.service.impl;

import com.finance.tracker.service.CounterService;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class CounterServiceImpl implements CounterService {

    private final AtomicInteger atomicCounter = new AtomicInteger();
    private int unsafeCounter;
    private int synchronizedCounter;

    @Override
    public void incrementUnsafe() {
        unsafeCounter++;
    }

    @Override
    public synchronized void incrementSynchronized() {
        synchronizedCounter++;
    }

    @Override
    public void incrementAtomic() {
        atomicCounter.incrementAndGet();
    }

    @Override
    public int getUnsafeValue() {
        return unsafeCounter;
    }

    @Override
    public synchronized int getSynchronizedValue() {
        return synchronizedCounter;
    }

    @Override
    public int getAtomicValue() {
        return atomicCounter.get();
    }

    @Override
    public synchronized void reset() {
        unsafeCounter = 0;
        synchronizedCounter = 0;
        atomicCounter.set(0);
    }
}
