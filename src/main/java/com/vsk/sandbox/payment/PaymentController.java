package com.vsk.sandbox.payment;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    private final AtomicLong artificialDelayMs = new AtomicLong(20);
    private final AtomicBoolean failMode = new AtomicBoolean(false);
    private final List<byte[]> memoryHog = new CopyOnWriteArrayList<>();

    @PostMapping("/payments/charge")
    public ResponseEntity<Map<String, Object>> charge() throws InterruptedException {
        long start = System.nanoTime();
        Thread.sleep(artificialDelayMs.get());
        long latencyMs = (System.nanoTime() - start) / 1_000_000;

        if (failMode.get()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "FAILED",
                            "reason", "Database connection pool exhausted"));
        }

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "transactionId", UUID.randomUUID().toString(),
                "latencyMs", latencyMs));
    }

    @PostMapping("/chaos/latency")
    public Map<String, Object> setLatency(@RequestParam(defaultValue = "20") long ms) {
        if (ms < 0 || ms > 60_000) {
            throw new IllegalArgumentException("ms must be between 0 and 60000");
        }
        artificialDelayMs.set(ms);
        return Map.of("artificialDelayMs", ms);
    }

    @PostMapping("/chaos/fail")
    public Map<String, Object> setFailureMode(@RequestParam(defaultValue = "true") boolean enabled) {
        failMode.set(enabled);
        return Map.of("failMode", enabled);
    }

    @PostMapping("/chaos/memory")
    public Map<String, Object> setMemory(@RequestParam(defaultValue = "250") int mb) {
        if (mb < 0 || mb > 512) {
            throw new IllegalArgumentException("mb must be between 0 and 512");
        }
        memoryHog.clear();
        for (int i = 0; i < mb; i++) {
            memoryHog.add(new byte[1024 * 1024]);
        }
        return Map.of("allocatedMb", memoryHog.size());
    }

    @PostMapping("/chaos/reset")
    public Map<String, Object> reset() {
        artificialDelayMs.set(20);
        failMode.set(false);
        memoryHog.clear();
        System.gc();
        return Map.of("status", "reset");
    }
}
