package com.indotaichen.laporan.controller;

import com.indotaichen.laporan.dto.ApiResponse;
import com.indotaichen.laporan.dto.MemoPentingResponse;
import com.indotaichen.laporan.dto.PpcFilterRequest;
import com.indotaichen.laporan.service.PpcFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ppc")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PpcFilterController {

    private final PpcFilterService ppcFilterService;

    /**
     * Get filtered data for Memo Penting PPC
     * 
     * This endpoint replicates the PHP native ppc_filter.php functionality
     * 
     * Filter Parameters (all optional):
     * - noOrder: Bon Order (NO_ORDER)
     * - prodDemand: Production Demand (DEMAND)
     * - prodOrder: Production Order (NO_KK)
     * - tgl1: Dari Tanggal (format: yyyy-MM-dd)
     * - tgl2: Sampai Tanggal (format: yyyy-MM-dd)
     * - noPo: Nomor PO
     * - articleGroup: Article Group (SUBCODE02)
     * - articleCode: Article Code (SUBCODE03)
     * - namaWarna: Nama Warna
     * - kkoke: KK OKE filter - "ya", "tidak", "sertakan" (default: "tidak")
     * - orderline: Orderline
     *
     * @param noOrder Bon Order filter
     * @param prodDemand Production Demand filter
     * @param prodOrder Production Order filter
     * @param tgl1 From date filter (yyyy-MM-dd)
     * @param tgl2 To date filter (yyyy-MM-dd)
     * @param noPo PO Number filter
     * @param articleGroup Article Group filter
     * @param articleCode Article Code filter
     * @param namaWarna Color Name filter
     * @param kkoke KK OKE filter
     * @param orderline Orderline filter
     * @return List of MemoPentingResponse
     */
    @GetMapping("/memo-penting")
    public ResponseEntity<ApiResponse<List<MemoPentingResponse>>> getMemoPenting(
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

        long startTime = System.currentTimeMillis();

        try {
            log.info("Fetching Memo Penting data with filters - noOrder: {}, prodDemand: {}, prodOrder: {}, " +
                    "tgl1: {}, tgl2: {}, noPo: {}, articleGroup: {}, articleCode: {}, namaWarna: {}, kkoke: {}",
                    noOrder, prodDemand, prodOrder, tgl1, tgl2, noPo, articleGroup, articleCode, namaWarna, kkoke);

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

            List<MemoPentingResponse> results = ppcFilterService.getFilteredData(request);

            long loadDuration = System.currentTimeMillis() - startTime;
            log.info("Memo Penting data fetched successfully. Total records: {}, Duration: {} ms",
                    results.size(), loadDuration);

            return ResponseEntity.ok(ApiResponse.success(results, results.size(), loadDuration));

        } catch (Exception e) {
            log.error("Error fetching Memo Penting data", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching data: " + e.getMessage()));
        }
    }

    /**
     * POST endpoint for Memo Penting PPC (alternative to GET)
     * Same functionality as GET but accepts JSON body
     */
    @PostMapping("/memo-penting")
    public ResponseEntity<ApiResponse<List<MemoPentingResponse>>> getMemoPentingPost(
            @RequestBody PpcFilterRequest request) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Fetching Memo Penting data (POST) with request: {}", request);

            // Set default kkoke if not provided
            if (request.getKkoke() == null || request.getKkoke().isEmpty()) {
                request.setKkoke("tidak");
            }

            List<MemoPentingResponse> results = ppcFilterService.getFilteredData(request);

            long loadDuration = System.currentTimeMillis() - startTime;
            log.info("Memo Penting data fetched successfully. Total records: {}, Duration: {} ms",
                    results.size(), loadDuration);

            return ResponseEntity.ok(ApiResponse.success(results, results.size(), loadDuration));

        } catch (Exception e) {
            log.error("Error fetching Memo Penting data", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching data: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("PPC Filter API is running"));
    }
}
