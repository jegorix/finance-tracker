package com.finance.tracker.service;

public interface CounterService {

    void incrementUnsafe();

    void incrementSynchronized();

    void incrementAtomic();

    int getUnsafeValue();

    int getSynchronizedValue();

    int getAtomicValue();

    void reset();
}
