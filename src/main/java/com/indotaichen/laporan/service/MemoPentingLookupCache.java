package com.indotaichen.laporan.service;

import com.indotaichen.laporan.repository.PpcFilterRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Request-scoped cache for the many per-row lookups required by the memo penting report.
 */
public class MemoPentingLookupCache {

    private final PpcFilterRepository repository;

    private final Map<String, Map<String, Object>> lebarCache = new HashMap<>();
    private final Map<String, Map<String, Object>> gramasiCache = new HashMap<>();
    private final Map<String, String> actualDeliveryCache = new HashMap<>();
    private final Map<String, Map<String, Object>> qtySalinanCache = new HashMap<>();
    private final Map<String, Map<String, Object>> benangBookingCache = new HashMap<>();
    private final Map<String, Map<String, Object>> benangRajutCache = new HashMap<>();
    private final Map<String, Map<String, Object>> tglGreigeCache = new HashMap<>();
    private final Map<String, Map<String, Object>> tglAwalGreigeCache = new HashMap<>();
    private final Map<String, Map<String, Object>> tglBagiKainCache = new HashMap<>();
    private final Map<String, Map<String, Object>> rollCache = new HashMap<>();
    private final Map<String, Map<String, Object>> originalPdCache = new HashMap<>();
    private final Map<String, Map<String, Object>> cekSalinanCache = new HashMap<>();
    private final Map<String, Map<String, Object>> qtyPackingCache = new HashMap<>();
    private final Map<String, Map<String, Object>> nettoYdCache = new HashMap<>();
    private final Map<String, Map<String, Object>> qtyKurangCache = new HashMap<>();
    private final Map<String, Map<String, Object>> lotCodeCache = new HashMap<>();
    private final Map<String, Map<String, Object>> qtyReadyCache = new HashMap<>();
    private final Map<String, Map<String, Object>> scheduleDyeCache = new HashMap<>();
    private final Map<String, Map<String, Object>> scheduleFinCache = new HashMap<>();
    private final Map<String, Map<String, Object>> additionalCache = new HashMap<>();
    private final Map<String, Map<String, Object>> jamCache = new HashMap<>();

    private final Map<String, Map<String, Object>> statusCloseCache = new HashMap<>();
    private final Map<String, Map<String, Object>> delayProgressSelesaiCache = new HashMap<>();
    private final Map<String, Map<String, Object>> delayProgressMulaiCache = new HashMap<>();
    private final Map<String, Map<String, Object>> cnpCloseCache = new HashMap<>();
    private final Map<String, Map<String, Object>> totalStepCache = new HashMap<>();
    private final Map<String, Map<String, Object>> totalCloseCache = new HashMap<>();
    private final Map<String, Map<String, Object>> notCnpCloseCache = new HashMap<>();
    private final Map<String, Map<String, Object>> statusTerakhirCache = new HashMap<>();
    private final Map<String, Map<String, Object>> statusTerakhirBKR1Cache = new HashMap<>();
    private final Map<String, Map<String, Object>> statusTerakhirClosedCache = new HashMap<>();

    public MemoPentingLookupCache(PpcFilterRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> getLebar(String noOrder, String orderLine) {
        if (isBlank(noOrder) || isBlank(orderLine)) return Collections.emptyMap();
        String key = key(noOrder, orderLine);
        return lebarCache.computeIfAbsent(key, k -> repository.getLebar(noOrder, orderLine));
    }

    public Map<String, Object> getGramasi(String noOrder, String orderLine) {
        if (isBlank(noOrder) || isBlank(orderLine)) return Collections.emptyMap();
        String key = key(noOrder, orderLine);
        return gramasiCache.computeIfAbsent(key, k -> repository.getGramasi(noOrder, orderLine));
    }

    public String getActualDelivery(String noOrder, String orderLine) {
        if (isBlank(noOrder) || isBlank(orderLine)) return "";
        String key = key(noOrder, orderLine);
        return actualDeliveryCache.computeIfAbsent(key, k -> repository.getActualDelivery(noOrder, orderLine));
    }

    public Map<String, Object> getQtySalinan(String demand) {
        if (isBlank(demand)) return Collections.emptyMap();
        return qtySalinanCache.computeIfAbsent(demand, k -> repository.getQtySalinan(demand));
    }

    public Map<String, Object> getBenangBookingNew(String noOrder, String orderLine) {
        if (isBlank(noOrder) || isBlank(orderLine)) return Collections.emptyMap();
        String key = key(noOrder, orderLine);
        return benangBookingCache.computeIfAbsent(key, k -> repository.getBenangBookingNew(noOrder, orderLine));
    }

    public Map<String, Object> getBenangRajut(String subcode01, String subcode02, String subcode03,
                                               String subcode04, String noOrder) {
        if (isBlank(noOrder)) return Collections.emptyMap();
        String key = key(subcode01, subcode02, subcode03, subcode04, noOrder);
        return benangRajutCache.computeIfAbsent(key, k -> repository.getBenangRajut(
                safe(subcode01), safe(subcode02), safe(subcode03), safe(subcode04), noOrder));
    }

    public Map<String, Object> getTglGreige(String kode) {
        if (isBlank(kode)) return Collections.emptyMap();
        return tglGreigeCache.computeIfAbsent(kode, k -> repository.getTglGreige(kode));
    }

    public Map<String, Object> getTglAwalGreigeRmp(String demand) {
        if (isBlank(demand)) return Collections.emptyMap();
        return tglAwalGreigeCache.computeIfAbsent(demand, k -> repository.getTglAwalGreigeRmp(demand));
    }

    public Map<String, Object> getTglBagiKain(String noKk) {
        if (isBlank(noKk)) return Collections.emptyMap();
        return tglBagiKainCache.computeIfAbsent(noKk, k -> repository.getTglBagiKain(noKk));
    }

    public Map<String, Object> getRoll(String noKk) {
        if (isBlank(noKk)) return Collections.emptyMap();
        return rollCache.computeIfAbsent(noKk, k -> repository.getRoll(noKk));
    }

    public Map<String, Object> getOriginalPdCode(String demand) {
        if (isBlank(demand)) return Collections.emptyMap();
        return originalPdCache.computeIfAbsent(demand, k -> repository.getOriginalPdCode(demand));
    }

    public Map<String, Object> getCekSalinan(String demand) {
        if (isBlank(demand)) return Collections.emptyMap();
        return cekSalinanCache.computeIfAbsent(demand, k -> repository.getCekSalinan(demand));
    }

    public Map<String, Object> getQtyPacking(String demand) {
        if (isBlank(demand)) return Collections.emptyMap();
        return qtyPackingCache.computeIfAbsent(demand, k -> repository.getQtyPacking(demand));
    }

    public Map<String, Object> getNettoYd(String demand) {
        if (isBlank(demand)) return Collections.emptyMap();
        return nettoYdCache.computeIfAbsent(demand, k -> repository.getNettoYd(demand));
    }

    public Map<String, Object> getQtyKurang(String noOrder, String orderLine) {
        if (isBlank(noOrder) || isBlank(orderLine)) return Collections.emptyMap();
        String key = key(noOrder, orderLine);
        return qtyKurangCache.computeIfAbsent(key, k -> repository.getQtyKurang(noOrder, orderLine));
    }

    public Map<String, Object> getLotCode(String noOrder, String orderLine) {
        if (isBlank(noOrder) || isBlank(orderLine)) return Collections.emptyMap();
        String key = key(noOrder, orderLine);
        return lotCodeCache.computeIfAbsent(key, k -> repository.getLotCode(noOrder, orderLine));
    }

    public Map<String, Object> getQtyReady(String productionOrderCodes, String productionDemandCodes, String noOrder) {
        if (isBlank(productionOrderCodes) || isBlank(productionDemandCodes) || isBlank(noOrder)) {
            return Collections.emptyMap();
        }
        String key = key(productionOrderCodes, productionDemandCodes, noOrder);
        return qtyReadyCache.computeIfAbsent(key, k -> repository.getQtyReady(productionOrderCodes, productionDemandCodes, noOrder));
    }

    public Map<String, Object> getScheduleDye(String noKk) {
        if (isBlank(noKk)) return Collections.emptyMap();
        return scheduleDyeCache.computeIfAbsent(noKk, k -> repository.getScheduleDye(noKk));
    }

    public Map<String, Object> getScheduleFin(String noKk, String demand) {
        if (isBlank(noKk) || isBlank(demand)) return Collections.emptyMap();
        String key = key(noKk, demand);
        return scheduleFinCache.computeIfAbsent(key, k -> repository.getScheduleFin(noKk, demand));
    }

    public Map<String, Object> getAdditional(String noKk, String demand) {
        if (isBlank(noKk) || isBlank(demand)) return Collections.emptyMap();
        String key = key(noKk, demand);
        return additionalCache.computeIfAbsent(key, k -> repository.getAdditional(noKk, demand));
    }

    public Map<String, Object> getJamInOut(String noKk, String demand, String groupstepOption) {
        if (isBlank(noKk) || isBlank(demand) || isBlank(groupstepOption)) return Collections.emptyMap();
        String key = key(noKk, demand, groupstepOption);
        return jamCache.computeIfAbsent(key, k -> repository.getJamInOut(noKk, demand, groupstepOption));
    }

    public Map<String, Object> getStatusClose(String noKk) {
        if (isBlank(noKk)) return Collections.emptyMap();
        return statusCloseCache.computeIfAbsent(noKk, k -> repository.getStatusClose(noKk));
    }

    public Map<String, Object> getDelayProgressSelesai(String noKk) {
        if (isBlank(noKk)) return Collections.emptyMap();
        return delayProgressSelesaiCache.computeIfAbsent(noKk, k -> repository.getDelayProgressSelesai(noKk));
    }

    public Map<String, Object> getDelayProgressMulai(String noKk) {
        if (isBlank(noKk)) return Collections.emptyMap();
        return delayProgressMulaiCache.computeIfAbsent(noKk, k -> repository.getDelayProgressMulai(noKk));
    }

    public Map<String, Object> getCnpClose(String noKk) {
        if (isBlank(noKk)) return Collections.emptyMap();
        return cnpCloseCache.computeIfAbsent(noKk, k -> repository.getCnpClose(noKk));
    }

    public Map<String, Object> getTotalStep(String noKk) {
        if (isBlank(noKk)) return Collections.emptyMap();
        return totalStepCache.computeIfAbsent(noKk, k -> repository.getTotalStep(noKk));
    }

    public Map<String, Object> getTotalClose(String noKk) {
        if (isBlank(noKk)) return Collections.emptyMap();
        return totalCloseCache.computeIfAbsent(noKk, k -> repository.getTotalClose(noKk));
    }

    public Map<String, Object> getNotCnpClose(String noKk, String groupstepOption) {
        if (isBlank(noKk) || isBlank(groupstepOption)) return Collections.emptyMap();
        String key = key(noKk, groupstepOption);
        return notCnpCloseCache.computeIfAbsent(key, k -> repository.getNotCnpClose(noKk, groupstepOption));
    }

    public Map<String, Object> getStatusTerakhir(String noKk, String groupstepOption) {
        if (isBlank(noKk) || isBlank(groupstepOption)) return Collections.emptyMap();
        String key = key(noKk, groupstepOption);
        return statusTerakhirCache.computeIfAbsent(key, k -> repository.getStatusTerakhir(noKk, groupstepOption));
    }

    public Map<String, Object> getStatusTerakhirBKR1(String noKk, String demand) {
        if (isBlank(noKk) || isBlank(demand)) return Collections.emptyMap();
        String key = key(noKk, demand);
        return statusTerakhirBKR1Cache.computeIfAbsent(key, k -> repository.getStatusTerakhirBKR1(noKk, demand));
    }

    public Map<String, Object> getStatusTerakhirClosed(String noKk, String demand, String stepNumber) {
        if (isBlank(noKk) || isBlank(demand) || isBlank(stepNumber)) return Collections.emptyMap();
        String key = key(noKk, demand, stepNumber);
        return statusTerakhirClosedCache.computeIfAbsent(key, k -> repository.getStatusTerakhirClosed(noKk, demand, stepNumber));
    }

    private static String key(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            builder.append(part == null ? "" : part).append('#');
        }
        return builder.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
