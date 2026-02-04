package com.indotaichen.laporan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indotaichen.laporan.dto.MemoPentingResponse;
import com.indotaichen.laporan.dto.PpcFilterRequest;
import com.indotaichen.laporan.dto.ProgressEvent;
import com.indotaichen.laporan.service.PpcFilterSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/ppc")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PpcFilterSseController {

    private final PpcFilterSseService sseService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * SSE Endpoint for streaming progress during data fetch
     * 
     * URL: GET /api/ppc/memo-penting-stream?noOrder=xxx&kkoke=tidak
     * 
     * Events sent:
     * - stage: "init" - Starting process
     * - stage: "fetch" - Fetching main data from DB
     * - stage: "process" - Processing each row (with current/total)
     * - stage: "complete" - Done, includes final data
     * - stage: "error" - Error occurred
     */
    @GetMapping(value = "/memo-penting-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMemoPenting(
            @RequestParam(required = false) String noOrder,
            @RequestParam(required = false) String prodDemand,
            @RequestParam(required = false) String prodOrder,
            @RequestParam(required = false) String tgl1,
            @RequestParam(required = false) String tgl2,
            @RequestParam(required = false) String noPo,
            @RequestParam(required = false) String articleGroup,
            @RequestParam(required = false) String articleCode,
            @RequestParam(required = false) String namaWarna,
            @RequestParam(required = false, defaultValue = "tidak") String kkoke,
            @RequestParam(required = false) String orderline) {

        // SSE timeout: 5 minutes
        SseEmitter emitter = new SseEmitter(300000L);

        PpcFilterRequest request = PpcFilterRequest.builder()
                .noOrder(noOrder)
                .prodDemand(prodDemand)
                .prodOrder(prodOrder)
                .tgl1(tgl1)
                .tgl2(tgl2)
                .noPo(noPo)
                .articleGroup(articleGroup)
                .articleCode(articleCode)
                .namaWarna(namaWarna)
                .kkoke(kkoke)
                .orderline(orderline)
                .build();

        log.info("SSE Stream started with request: noOrder={}, tgl1={}, tgl2={}, kkoke={}", 
                noOrder, tgl1, tgl2, kkoke);

        executor.execute(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // Send initial event
                sendEvent(emitter, ProgressEvent.builder()
                        .stage("init")
                        .percent(0)
                        .message("Memulai proses...")
                        .detail("Menghubungkan ke database")
                        .elapsedMs(0)
                        .build());

                // Process with progress callback
                List<MemoPentingResponse> results = sseService.getFilteredDataWithProgress(request, 
                    (stage, current, total, message, detail) -> {
                        int percent = total > 0 ? (int) ((current * 100.0) / total) : 0;
                        
                        // Adjust percent based on stage
                        if ("fetch".equals(stage)) {
                            percent = 10; // Fetching main data = 10%
                        } else if ("process".equals(stage)) {
                            percent = 10 + (int) ((current * 80.0) / Math.max(total, 1)); // Processing = 10-90%
                        } else if ("finalize".equals(stage)) {
                            percent = 95;
                        }
                        
                        try {
                            sendEvent(emitter, ProgressEvent.builder()
                                    .stage(stage)
                                    .current(current)
                                    .total(total)
                                    .percent(Math.min(percent, 99))
                                    .message(message)
                                    .detail(detail)
                                    .elapsedMs(System.currentTimeMillis() - startTime)
                                    .build());
                        } catch (IOException e) {
                            log.warn("Failed to send SSE event: {}", e.getMessage());
                        }
                    });

                // Send complete event with data
                long elapsed = System.currentTimeMillis() - startTime;
                sendEvent(emitter, ProgressEvent.builder()
                        .stage("complete")
                        .current(results.size())
                        .total(results.size())
                        .percent(100)
                        .message("Selesai!")
                        .detail("Berhasil memuat " + results.size() + " data dalam " + (elapsed / 1000.0) + " detik")
                        .data(results)
                        .elapsedMs(elapsed)
                        .build());

                emitter.complete();
                log.info("SSE stream completed. Total: {} records, Duration: {}ms", results.size(), elapsed);

            } catch (Exception e) {
                log.error("Error in SSE stream", e);
                try {
                    sendEvent(emitter, ProgressEvent.builder()
                            .stage("error")
                            .percent(0)
                            .message("Terjadi kesalahan")
                            .detail(e.getMessage())
                            .elapsedMs(System.currentTimeMillis() - startTime)
                            .build());
                } catch (IOException ex) {
                    log.error("Failed to send error event", ex);
                }
                emitter.completeWithError(e);
            }
        });

        // Handle client disconnect
        emitter.onCompletion(() -> log.debug("SSE connection completed"));
        emitter.onTimeout(() -> log.warn("SSE connection timed out"));
        emitter.onError(e -> log.error("SSE error: {}", e.getMessage()));

        return emitter;
    }

    private void sendEvent(SseEmitter emitter, ProgressEvent event) throws IOException {
        emitter.send(SseEmitter.event()
                .name("progress")
                .data(objectMapper.writeValueAsString(event)));
    }
}
