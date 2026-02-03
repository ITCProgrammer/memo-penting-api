package com.indotaichen.laporan.service;

import com.indotaichen.laporan.dto.MemoPentingResponse;
import com.indotaichen.laporan.dto.PpcFilterRequest;
import com.indotaichen.laporan.repository.PpcFilterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PpcFilterService {

    private final PpcFilterRepository repository;

    private static final DecimalFormat DF_0 = new DecimalFormat("#,##0");
    private static final DecimalFormat DF_2 = new DecimalFormat("#,##0.00");
    private static final DecimalFormat DF_3 = new DecimalFormat("#,##0.000");

    public List<MemoPentingResponse> getFilteredData(PpcFilterRequest request) {
        List<MemoPentingResponse> results = new ArrayList<>();

        // Get main data from ITXVIEW_MEMOPENTINGPPC
        List<Map<String, Object>> mainDataList = repository.getMainData(request);

        for (Map<String, Object> rowdb2 : mainDataList) {
            String noKk = getStringValue(rowdb2, "NO_KK");
            String demand = getStringValue(rowdb2, "DEMAND");
            String noOrder = getStringValue(rowdb2, "NO_ORDER");
            String orderLine = getStringValue(rowdb2, "ORDERLINE");
            String progressStatus = getStringValue(rowdb2, "PROGRESSSTATUS");
            String progressStatusDemand = getStringValue(rowdb2, "PROGRESSSTATUS_DEMAND");

            // Initialize status variables
            String kodeDept = "";
            String statusTerakhir = "";
            String statusOperation = "";
            String jamStatusTerakhir = "";
            String delayProgressStatus = "";
            String groupstepOption = "";

            // Check Production Demand Closed or not (PROGRESSSTATUS_DEMAND == 6)
            if ("6".equals(progressStatusDemand)) {
                kodeDept = "-";
                statusTerakhir = "-";
                statusOperation = "KK Oke";
            } else {
                // Check Production Order Closed (PROGRESSSTATUS == 6)
                if ("6".equals(progressStatus)) {
                    kodeDept = "-";
                    statusTerakhir = "-";
                    statusOperation = "KK Oke";
                } else {
                    // Get status close
                    Map<String, Object> rowStatusClose = repository.getStatusClose(noKk);
                    String statusCloseProgressStatus = getStringValue(rowStatusClose, "PROGRESSSTATUS");
                    String groupstepnumber = getStringValue(rowStatusClose, "GROUPSTEPNUMBER");
                    
                    if (groupstepnumber == null || groupstepnumber.isEmpty()) {
                        groupstepnumber = "0";
                    }

                    // For DELAY PROGRESS STATUS (MS. AMY Request)
                    if ("2".equals(statusCloseProgressStatus)) { // ENTERED
                        Map<String, Object> delayProgressSelesai = repository.getDelayProgressSelesai(noKk);
                        jamStatusTerakhir = getStringValue(delayProgressSelesai, "MULAI");
                        Object delayPs = delayProgressSelesai.get("DELAY_PROGRESSSTATUS");
                        delayProgressStatus = (delayPs != null ? delayPs.toString() : "") + " Hari";
                    } else if ("3".equals(statusCloseProgressStatus)) { // PROGRESS
                        Map<String, Object> delayProgressMulai = repository.getDelayProgressMulai(noKk);
                        jamStatusTerakhir = getStringValue(delayProgressMulai, "SELESAI");
                        Object delayPs = delayProgressMulai.get("DELAY_PROGRESSSTATUS");
                        delayProgressStatus = (delayPs != null ? delayPs.toString() : "") + " Hari";
                    }

                    // Get CNP Close
                    Map<String, Object> cnpClose = repository.getCnpClose(noKk);
                    Integer cnpProgressStatus = getIntValue(cnpClose, "PROGRESSSTATUS");
                    String cnpOperationCode = getStringValue(cnpClose, "OPERATIONCODE");

                    if (cnpProgressStatus != null && cnpProgressStatus == 3) { // 3 is Closed
                        if ("PPC4".equals(cnpOperationCode)) {
                            if ("6".equals(progressStatus)) {
                                kodeDept = "-";
                                statusTerakhir = "-";
                                statusOperation = "KK Oke";
                            } else {
                                kodeDept = "-";
                                statusTerakhir = "-";
                                statusOperation = "KK Oke | Segera Closed Production Order!";
                            }
                        } else {
                            if ("2".equals(statusCloseProgressStatus)) {
                                groupstepOption = "= '" + groupstepnumber + "'";
                            } else {
                                // Check if all steps are closed
                                Map<String, Object> totalStep = repository.getTotalStep(noKk);
                                Map<String, Object> totalClose = repository.getTotalClose(noKk);
                                
                                Object ts = totalStep.get("TOTALSTEP");
                                Object tc = totalClose.get("TOTALCLOSE");
                                
                                if (ts != null && tc != null && ts.toString().equals(tc.toString())) {
                                    groupstepOption = "= '" + groupstepnumber + "'";
                                } else {
                                    groupstepOption = "> '" + groupstepnumber + "'";
                                }
                            }

                            // Get not CNP close (next step)
                            Map<String, Object> notCnpClose = repository.getNotCnpClose(noKk, groupstepOption);
                            if (!notCnpClose.isEmpty()) {
                                kodeDept = getStringValue(notCnpClose, "OPERATIONGROUPCODE");
                                statusTerakhir = getStringValue(notCnpClose, "LONGDESCRIPTION");
                                statusOperation = getStringValue(notCnpClose, "STATUS_OPERATION");
                            } else {
                                // Retry with = groupstepnumber
                                groupstepOption = "= '" + groupstepnumber + "'";
                                notCnpClose = repository.getNotCnpClose(noKk, groupstepOption);
                                kodeDept = getStringValue(notCnpClose, "OPERATIONGROUPCODE");
                                statusTerakhir = getStringValue(notCnpClose, "LONGDESCRIPTION");
                                statusOperation = getStringValue(notCnpClose, "STATUS_OPERATION");
                            }
                        }
                    } else {
                        if ("2".equals(statusCloseProgressStatus)) {
                            groupstepOption = "= '" + groupstepnumber + "'";
                        } else {
                            groupstepOption = "> '" + groupstepnumber + "'";
                        }
                        
                        Map<String, Object> statusTerakhirData = repository.getStatusTerakhir(noKk, groupstepOption);
                        kodeDept = getStringValue(statusTerakhirData, "OPERATIONGROUPCODE");
                        statusTerakhir = getStringValue(statusTerakhirData, "LONGDESCRIPTION");
                        statusOperation = getStringValue(statusTerakhirData, "STATUS_OPERATION");
                    }
                }
            }

            // Check BKR1 status (Tiket no: BDIT250000492, BDIT250001161)
            Map<String, Object> statusTerakhirBKR1 = repository.getStatusTerakhirBKR1(noKk, demand);
            String stepNumberBKR1 = getStringValue(statusTerakhirBKR1, "STEPNUMBER");
            
            boolean shouldInclude = true;
            if (!statusTerakhirBKR1.isEmpty() && stepNumberBKR1 != null && !stepNumberBKR1.isEmpty()) {
                Map<String, Object> statusTerakhirClosed = repository.getStatusTerakhirClosed(noKk, demand, stepNumberBKR1);
                if (statusTerakhirClosed.isEmpty()) {
                    shouldInclude = false;
                }
            }

            if (!shouldInclude) {
                continue;
            }

            // Get LEBAR
            Map<String, Object> lebarData = repository.getLebar(noOrder, orderLine);
            String lebar = "";
            Object lebarVal = lebarData.get("LEBAR");
            if (lebarVal != null) {
                lebar = DF_0.format(((Number) lebarVal).doubleValue());
            }

            // Get GRAMASI
            Map<String, Object> gramasiData = repository.getGramasi(noOrder, orderLine);
            String gramasi = "-";
            Object gramasiKff = gramasiData.get("GRAMASI_KFF");
            Object gramasiFkf = gramasiData.get("GRAMASI_FKF");
            if (gramasiKff != null) {
                gramasi = DF_0.format(((Number) gramasiKff).doubleValue());
            } else if (gramasiFkf != null) {
                gramasi = DF_0.format(((Number) gramasiFkf).doubleValue());
            }

            // Get Actual Delivery
            String actualDelivery = repository.getActualDelivery(noOrder, orderLine);

            // Get QTY Salinan
            Map<String, Object> qtySalinanData = repository.getQtySalinan(demand);
            String subcode01 = getStringValue(qtySalinanData, "SUBCODE01");
            String subcode02 = getStringValue(qtySalinanData, "SUBCODE02");
            String subcode03 = getStringValue(qtySalinanData, "SUBCODE03");
            String subcode04 = getStringValue(qtySalinanData, "SUBCODE04");

            // Get Benang Booking New
            Map<String, Object> benangBookingNew = repository.getBenangBookingNew(noOrder, orderLine);
            String dBenangBookingNew = getStringValue(benangBookingNew, "SALESORDERCODE");

            // Get Benang Rajut
            Map<String, Object> benangRajut = repository.getBenangRajut(
                    subcode01 != null ? subcode01.trim() : "",
                    subcode02 != null ? subcode02.trim() : "",
                    subcode03 != null ? subcode03.trim() : "",
                    subcode04 != null ? subcode04.trim() : "",
                    noOrder);
            String dBenangRajut = getStringValue(benangRajut, "CODE");
            String tglPoGreige = getStringValue(benangRajut, "TGLPOGREIGE");

            // Get Tanggal Greige
            Map<String, Object> tglGreige = repository.getTglGreige(dBenangRajut != null ? dBenangRajut : "");
            Map<String, Object> tglAwalGreigeRmp = repository.getTglAwalGreigeRmp(demand);

            String greigeAwal = "";
            String greigeAkhir = "";
            
            if (dBenangRajut != null && !dBenangRajut.isEmpty()) {
                Object awalVal = tglGreige.get("AWAL");
                if (awalVal != null && !awalVal.toString().isEmpty()) {
                    greigeAwal = formatDate(awalVal);
                } else {
                    Object awalRmp = tglAwalGreigeRmp.get("AWAL");
                    greigeAwal = awalRmp != null ? formatDate(awalRmp) : "";
                }
                Object akhirVal = tglGreige.get("AKHIR");
                greigeAkhir = akhirVal != null ? formatDate(akhirVal) : "";
            } else {
                Object awalRmp = tglAwalGreigeRmp.get("AWAL");
                greigeAwal = awalRmp != null ? formatDate(awalRmp) : "";
                greigeAkhir = tglPoGreige != null ? tglPoGreige : "";
            }

            // Get Tanggal Bagi Kain
            Map<String, Object> tglBagiKain = repository.getTglBagiKain(noKk);
            String bagiKainTgl = getStringValue(tglBagiKain, "TRANSACTIONDATE");

            // Get Roll
            Map<String, Object> rollData = repository.getRoll(noKk);
            String roll = getStringValue(rollData, "ROLL");

            // Get Original PD Code
            Map<String, Object> origPdCode = repository.getOriginalPdCode(demand);
            String originalPdCode = getStringValue(origPdCode, "ORIGINALPDCODE");

            // Get Cek Salinan
            Map<String, Object> cekSalinan = repository.getCekSalinan(demand);
            String salinan058 = getStringValue(cekSalinan, "SALINAN_058");

            // Calculate Bruto/Bagi Kain
            String brutoBagiKain = "0";
            Object qtyBagiKainVal = rowdb2.get("QTY_BAGIKAIN");
            if (originalPdCode != null && !originalPdCode.isEmpty()) {
                if ("058".equals(salinan058)) {
                    brutoBagiKain = qtyBagiKainVal != null ? DF_2.format(((Number) qtyBagiKainVal).doubleValue()) : "0";
                } else {
                    brutoBagiKain = "0";
                }
            } else {
                brutoBagiKain = qtyBagiKainVal != null ? DF_2.format(((Number) qtyBagiKainVal).doubleValue()) : "0";
            }

            // Calculate QTY Salinan
            String qtySalinanStr = "0";
            if (originalPdCode != null && !originalPdCode.isEmpty()) {
                if (!"058".equals(salinan058)) {
                    Object userPrimaryQty = qtySalinanData.get("USERPRIMARYQUANTITY");
                    qtySalinanStr = userPrimaryQty != null ? DF_3.format(((Number) userPrimaryQty).doubleValue()) : "0";
                }
            }

            // Get QTY Packing
            Map<String, Object> qtyPacking = repository.getQtyPacking(demand);
            String qtyPackingStr = "0";
            Object mutasi = qtyPacking.get("mutasi");
            if (mutasi != null) {
                qtyPackingStr = mutasi.toString();
            }

            // Get Netto YD
            Map<String, Object> nettoYd = repository.getNettoYd(demand);
            String nettoYdMtr = "0";
            String priceUnitOfMeasureCode = getStringValue(nettoYd, "PRICEUNITOFMEASURECODE");
            if (priceUnitOfMeasureCode != null && "m".equals(priceUnitOfMeasureCode.trim())) {
                Object userSecQty = nettoYd.get("USERSECONDARYQUANTITY");
                nettoYdMtr = userSecQty != null ? DF_0.format(((Number) userSecQty).doubleValue()) : "0";
            } else {
                Object baseSecQty = nettoYd.get("BASESECONDARYQUANTITY");
                nettoYdMtr = baseSecQty != null ? DF_0.format(((Number) baseSecQty).doubleValue()) : "0";
            }

            // Get QTY Kurang
            String qtyKurangKg = "0.00";
            String qtyKurangYdMtr = "0.00";
            if (noOrder != null && !noOrder.isEmpty() && orderLine != null && !orderLine.isEmpty()) {
                Map<String, Object> qtyKurang = repository.getQtyKurang(noOrder, orderLine);
                Map<String, Object> lotCode = repository.getLotCode(noOrder, orderLine);
                
                String prodOrderCodes = getStringValue(lotCode, "PRODUCTIONORDERCODE");
                String prodDemandCodes = getStringValue(lotCode, "PRODUCTIONDEMANDCODE");
                
                Map<String, Object> qtyReady = new HashMap<>();
                if (prodOrderCodes != null && !prodOrderCodes.isEmpty()) {
                    qtyReady = repository.getQtyReady(prodOrderCodes, prodDemandCodes, noOrder);
                }

                Object konversi = qtyKurang.get("KONVERSI");
                Object netto2 = qtyKurang.get("NETTO_2");
                Object nettoM = qtyKurang.get("NETTO_M");
                Object qtySudahKirim2 = qtyKurang.get("QTY_SUDAH_KIRIM_2");
                Object qtyReady2 = qtyReady.get("QTY_READY_2");
                String priceUnit = getStringValue(qtyKurang, "PRICEUNITOFMEASURECODE");

                double konversiVal = konversi != null ? ((Number) konversi).doubleValue() : 0;
                double netto2Val = netto2 != null ? ((Number) netto2).doubleValue() : 0;
                double nettoMVal = nettoM != null ? ((Number) nettoM).doubleValue() : 0;
                double qtySudahKirim2Val = qtySudahKirim2 != null ? ((Number) qtySudahKirim2).doubleValue() : 0;
                double qtyReady2Val = qtyReady2 != null ? ((Number) qtyReady2).doubleValue() : 0;

                double nettoUsed = priceUnit != null && "m".equals(priceUnit.trim()) ? nettoMVal : netto2Val;
                double kurangYdMtr = nettoUsed - qtySudahKirim2Val - qtyReady2Val;
                
                qtyKurangYdMtr = DF_2.format(kurangYdMtr);
                
                if (konversiVal != 0) {
                    qtyKurangKg = DF_2.format(kurangYdMtr / konversiVal);
                }
            }

            // Get Schedule info
            String nomesin = "";
            String nourut = "";
            if ("DYE".equals(kodeDept)) {
                Map<String, Object> scheduleDye = repository.getScheduleDye(noKk);
                nomesin = getStringValue(scheduleDye, "no_mesin");
                nourut = getStringValue(scheduleDye, "no_urut");
            } else if ("FIN".equals(kodeDept)) {
                Map<String, Object> scheduleFin = repository.getScheduleFin(noKk, demand);
                String noMesinFin = getStringValue(scheduleFin, "no_mesin");
                if (noMesinFin != null && !noMesinFin.isEmpty()) {
                    String trimmed = noMesinFin.trim();
                    if (trimmed.length() >= 5) {
                        nomesin = noMesinFin + "-" + trimmed.substring(trimmed.length() - 5, trimmed.length() - 3) 
                                + trimmed.substring(trimmed.length() - 2);
                    } else {
                        nomesin = noMesinFin;
                    }
                }
                nourut = getStringValue(scheduleFin, "nourut");
            }

            // Calculate Total Hari
            String totalHari = "";
            if (bagiKainTgl != null && !bagiKainTgl.isEmpty()) {
                try {
                    LocalDate tglBagiKainDate = LocalDate.parse(bagiKainTgl.substring(0, 10));
                    LocalDate now = LocalDate.now();
                    long days = ChronoUnit.DAYS.between(tglBagiKainDate, now);
                    totalHari = days + " Hari";
                } catch (Exception e) {
                    totalHari = "";
                }
            } else {
                // Use ORDERDATE if no bagi kain date
                Object orderDateObj = rowdb2.get("ORDERDATE");
                if (orderDateObj != null) {
                    try {
                        String orderDateStr = formatDateTime(orderDateObj);
                        LocalDate orderDate = LocalDate.parse(orderDateStr.substring(0, 10));
                        LocalDate now = LocalDate.now();
                        long days = ChronoUnit.DAYS.between(orderDate, now);
                        totalHari = days + " Hari";
                    } catch (Exception e) {
                        totalHari = "";
                    }
                }
            }

            // Get Additional
            Map<String, Object> additional = repository.getAdditional(noKk, demand);
            String reProsesAdditional = getStringValue(additional, "TOTAL_ADDITIONAL");

            // Build catatan PO Greige
            String catatanPoGreige = "";
            if (dBenangBookingNew != null && !dBenangBookingNew.isEmpty()) {
                catatanPoGreige += dBenangBookingNew + ". Greige Ready";
            }
            if (dBenangRajut != null && !dBenangRajut.isEmpty()) {
                if (!catatanPoGreige.isEmpty()) catatanPoGreige += " ";
                catatanPoGreige += dBenangRajut + ". Rajut";
            }

            // Format status terakhir with jam
            String fullStatusTerakhir = statusTerakhir;
            if (!"KK Oke".equals(statusOperation) && jamStatusTerakhir != null && !jamStatusTerakhir.isEmpty()) {
                fullStatusTerakhir = statusTerakhir + " (" + jamStatusTerakhir + ")";
            }

            // Format delay progress status
            String finalDelayProgressStatus = "";
            if (!"KK Oke".equals(statusOperation)) {
                finalDelayProgressStatus = delayProgressStatus;
            }

            // Format Netto KG
            Object nettoVal = rowdb2.get("NETTO");
            String nettoKg = nettoVal != null ? DF_0.format(((Number) nettoVal).doubleValue()) : "0";

            // Format LOT
            String lotValue = getStringValue(rowdb2, "LOT");
            String formattedLot = "";
            if (lotValue != null && !lotValue.isEmpty()) {
                formattedLot = lotValue.substring(0, Math.min(7, lotValue.length()));
            }

            // Build response
            MemoPentingResponse response = MemoPentingResponse.builder()
                    .tglBukaKartu(formatDateTime(rowdb2.get("ORDERDATE")))
                    .pelanggan(getStringValue(rowdb2, "PELANGGAN"))
                    .noOrder(noOrder)
                    .noPo(getStringValue(rowdb2, "NO_PO"))
                    .keteranganProduct(getStringValue(rowdb2, "KETERANGAN_PRODUCT"))
                    .lebar(lebar)
                    .gramasi(gramasi)
                    .warna(getStringValue(rowdb2, "WARNA"))
                    .noWarna(getStringValue(rowdb2, "NO_WARNA"))
                    .delivery(formatDate(rowdb2.get("DELIVERY")))
                    .deliveryActual(actualDelivery)
                    .greigeAwal(greigeAwal)
                    .greigeAkhir(greigeAkhir)
                    .bagiKainTgl(bagiKainTgl != null ? bagiKainTgl : "")
                    .roll(roll != null ? roll : "")
                    .brutoBagiKain(brutoBagiKain)
                    .qtySalinan(qtySalinanStr)
                    .qtyPacking(qtyPackingStr)
                    .nettoKg(nettoKg)
                    .nettoYdMtr(nettoYdMtr)
                    .qtyKurangKg(qtyKurangKg)
                    .qtyKurangYdMtr(qtyKurangYdMtr)
                    .delay(getStringValue(rowdb2, "DELAY"))
                    .targetSelesai("")
                    .kodeDept(kodeDept != null ? kodeDept : "")
                    .statusTerakhir(fullStatusTerakhir != null ? fullStatusTerakhir : "")
                    .nomorMesinSchedule(nomesin != null ? nomesin : "")
                    .nomorUrutSchedule(nourut != null ? nourut : "")
                    .delayProgressStatus(finalDelayProgressStatus != null ? finalDelayProgressStatus : "")
                    .progressStatus(statusOperation != null ? statusOperation : "")
                    .totalHari(totalHari)
                    .lot(formattedLot)
                    .noDemand(demand)
                    .noKartuKerja(noKk)
                    .originalPdCode(originalPdCode != null ? originalPdCode : "")
                    .catatanPoGreige(catatanPoGreige)
                    .keterangan(getStringValue(rowdb2, "KETERANGAN"))
                    .reProsesAdditional(reProsesAdditional != null ? reProsesAdditional : "0")
                    .build();

            results.add(response);
        }

        return results;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatDateTime(Object dateObj) {
        if (dateObj == null) return "";
        if (dateObj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) dateObj).toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (dateObj instanceof java.sql.Date) {
            return ((java.sql.Date) dateObj).toLocalDate()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        if (dateObj instanceof LocalDateTime) {
            return ((LocalDateTime) dateObj).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (dateObj instanceof LocalDate) {
            return ((LocalDate) dateObj).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return dateObj.toString();
    }

    private String formatDate(Object dateObj) {
        if (dateObj == null) return "";
        if (dateObj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) dateObj).toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        if (dateObj instanceof java.sql.Date) {
            return ((java.sql.Date) dateObj).toLocalDate()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        if (dateObj instanceof LocalDateTime) {
            return ((LocalDateTime) dateObj).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        if (dateObj instanceof LocalDate) {
            return ((LocalDate) dateObj).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        String str = dateObj.toString();
        if (str.length() >= 10) {
            return str.substring(0, 10);
        }
        return str;
    }
}
