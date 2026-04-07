package com.finance.tracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Structured response for the race condition demonstration.")
public class RaceConditionDemoResponse {

    private int threadCount;
    private int incrementsPerThread;
    private int expectedValue;
    private int unsafeAttemptsCount;
    private int forcedCollisionInterval;
    private CounterResult unsafeCounter;
    private List<UnsafeAttempt> unsafeAttempts;
    private CounterResult synchronizedCounter;
    private CounterResult atomicCounter;
    private String takeaway;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Aggregated counter result for one concurrency strategy.")
    public static class CounterResult {

        private String name;
        private int actualValue;
        private int lostUpdates;
        private boolean matchesExpected;
        private String verdict;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "One unsafe counter attempt captured during the demo.")
    public static class UnsafeAttempt {

        private int attempt;
        private int actualValue;
        private int lostUpdates;
        private boolean raceConditionPresent;
    }
}
