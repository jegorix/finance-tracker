package com.finance.tracker.controller.api;

import com.finance.tracker.dto.response.RaceConditionDemoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Concurrency Demo", description = "Laboratory 6 race condition demonstration")
public interface RaceConditionControllerApi {

    @Operation(summary = "Run race condition demo with unsafe, synchronized and atomic counters")
    ResponseEntity<RaceConditionDemoResponse> runRaceConditionDemo() throws InterruptedException;
}
