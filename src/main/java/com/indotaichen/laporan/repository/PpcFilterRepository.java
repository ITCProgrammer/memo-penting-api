package com.indotaichen.laporan.repository;

import com.indotaichen.laporan.dto.PpcFilterRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Slf4j
public class PpcFilterRepository {

    private final JdbcTemplate db2JdbcTemplate;        // DB2 NOWPRD - conn1 in PHP
    private final JdbcTemplate nowprdJdbcTemplate;     // SQL Server nowprd - con_nowprd in PHP
    private final JdbcTemplate qcJdbcTemplate;         // MySQL db_qc - con_db_qc in PHP
    private final JdbcTemplate dyeingJdbcTemplate;     // SQL Server db_dying - con_db_dyeing in PHP
    private final JdbcTemplate finishingJdbcTemplate;  // SQL Server db_finishing - con_db_finishing in PHP

    public PpcFilterRepository(
            @Qualifier("db2JdbcTemplate") JdbcTemplate db2JdbcTemplate,
            @Qualifier("nowprdJdbcTemplate") JdbcTemplate nowprdJdbcTemplate,
            @Qualifier("qcJdbcTemplate") JdbcTemplate qcJdbcTemplate,
            @Qualifier("dyeingJdbcTemplate") JdbcTemplate dyeingJdbcTemplate,
            @Qualifier("finishingJdbcTemplate") JdbcTemplate finishingJdbcTemplate) {
        this.db2JdbcTemplate = db2JdbcTemplate;
        this.nowprdJdbcTemplate = nowprdJdbcTemplate;
        this.qcJdbcTemplate = qcJdbcTemplate;
        this.dyeingJdbcTemplate = dyeingJdbcTemplate;
        this.finishingJdbcTemplate = finishingJdbcTemplate;
    }

    /**
     * Get main data from ITXVIEW_MEMOPENTINGPPC (DB2)
     * PHP: db2_exec($conn1, $query)
     * Query: SELECT * FROM (SELECT * FROM ITXVIEW_MEMOPENTINGPPC WHERE conditions) WHERE kkoke_conditions
     */
    public List<Map<String, Object>> getMainData(PpcFilterRequest request) {
        List<String> conditions = new ArrayList<>();
        List<String> conditions2 = new ArrayList<>();

        // Build conditions based on filter - exactly like PHP native
        if (request.getNamaWarna() != null && !request.getNamaWarna().isEmpty()) {
            conditions.add("WARNA LIKE '%" + request.getNamaWarna() + "%'");
        }
        if (request.getProdOrder() != null && !request.getProdOrder().isEmpty()) {
            conditions.add("NO_KK = '" + request.getProdOrder() + "'");
        }
        if (request.getProdDemand() != null && !request.getProdDemand().isEmpty()) {
            conditions.add("DEMAND = '" + request.getProdDemand() + "'");
        }
        if (request.getNoOrder() != null && !request.getNoOrder().isEmpty()) {
            conditions.add("NO_ORDER = '" + request.getNoOrder() + "'");
        }
        if (request.getOrderline() != null && !request.getOrderline().isEmpty()) {
            conditions.add("ORDERLINE = '" + request.getOrderline() + "'");
        }
        if (request.getTgl1() != null && !request.getTgl1().isEmpty() 
                && request.getTgl2() != null && !request.getTgl2().isEmpty()) {
            conditions.add("DELIVERY BETWEEN '" + request.getTgl1() + "' AND '" + request.getTgl2() + "'");
        }
        if (request.getNoPo() != null && !request.getNoPo().isEmpty()) {
            conditions.add("NO_PO = '" + request.getNoPo() + "'");
        }
        if (request.getArticleGroup() != null && !request.getArticleGroup().isEmpty()
                && request.getArticleCode() != null && !request.getArticleCode().isEmpty()) {
            conditions.add("SUBCODE02 = '" + request.getArticleGroup() + "' AND SUBCODE03 = '" + request.getArticleCode() + "'");
        }
        
        // KK OKE filter - PHP: if ($kkoke === 'tidak')
        if ("tidak".equals(request.getKkoke())) {
            conditions2.add("NOT PROGRESSSTATUS = '6' AND NOT PROGRESSSTATUS_DEMAND = '6'");
        }

        String conditionsString = conditions.isEmpty() ? "1=1" : String.join(" AND ", conditions);
        
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM (SELECT * FROM ITXVIEW_MEMOPENTINGPPC WHERE ").append(conditionsString).append(")");
        
        if (!conditions2.isEmpty()) {
            query.append(" WHERE ").append(String.join(" AND ", conditions2));
        }

        log.debug("Main Query (DB2): {}", query.toString());
        
        return db2JdbcTemplate.queryForList(query.toString());
    }

    /**
     * Get LEBAR from ITXVIEWLEBAR (DB2)
     * PHP: $q_lebar = db2_exec($conn1, "SELECT * FROM ITXVIEWLEBAR WHERE SALESORDERCODE = '$rowdb2[NO_ORDER]' AND ORDERLINE = '$rowdb2[ORDERLINE]'");
     */
    public Map<String, Object> getLebar(String salesOrderCode, String orderLine) {
        String query = "SELECT * FROM ITXVIEWLEBAR WHERE SALESORDERCODE = ? AND ORDERLINE = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, salesOrderCode, orderLine);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get GRAMASI from ITXVIEWGRAMASI (DB2)
     * PHP: $q_gramasi = db2_exec($conn1, "SELECT * FROM ITXVIEWGRAMASI WHERE SALESORDERCODE = '$rowdb2[NO_ORDER]' AND ORDERLINE = '$rowdb2[ORDERLINE]'");
     */
    public Map<String, Object> getGramasi(String salesOrderCode, String orderLine) {
        String query = "SELECT * FROM ITXVIEWGRAMASI WHERE SALESORDERCODE = ? AND ORDERLINE = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, salesOrderCode, orderLine);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Actual Delivery from SALESORDER (DB2)
     * PHP: $q_actual_delivery = db2_exec($conn1, "SELECT COALESCE(s2.CONFIRMEDDELIVERYDATE, s.CONFIRMEDDUEDATE) AS ACTUAL_DELIVERY ...")
     */
    public String getActualDelivery(String noOrder, String orderLine) {
        String query = "SELECT " +
                "COALESCE(s2.CONFIRMEDDELIVERYDATE, s.CONFIRMEDDUEDATE) AS ACTUAL_DELIVERY " +
                "FROM SALESORDER s " +
                "LEFT JOIN SALESORDERDELIVERY s2 ON s2.SALESORDERLINESALESORDERCODE = s.CODE " +
                "AND s2.SALORDLINESALORDERCOMPANYCODE = s.COMPANYCODE " +
                "AND s2.SALORDLINESALORDERCOUNTERCODE = s.COUNTERCODE " +
                "WHERE s2.SALESORDERLINESALESORDERCODE = ? " +
                "AND s2.SALESORDERLINEORDERLINE = ?";
        try {
            Map<String, Object> result = db2JdbcTemplate.queryForMap(query, noOrder, orderLine);
            Object actualDelivery = result.get("ACTUAL_DELIVERY");
            return actualDelivery != null ? actualDelivery.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get QTY Salinan from PRODUCTIONDEMAND (DB2)
     * PHP: $q_qtysalinan = db2_exec($conn1, "SELECT * FROM PRODUCTIONDEMAND WHERE CODE = '$rowdb2[DEMAND]'");
     */
    public Map<String, Object> getQtySalinan(String demandCode) {
        String query = "SELECT * FROM PRODUCTIONDEMAND WHERE CODE = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, demandCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Booking New from ITXVIEW_BOOKING_NEW (DB2)
     * PHP: $sql_benang_booking_new = db2_exec($conn1, "SELECT * FROM ITXVIEW_BOOKING_NEW WHERE SALESORDERCODE = '$rowdb2[NO_ORDER]' AND ORDERLINE = '$rowdb2[ORDERLINE]'");
     */
    public Map<String, Object> getBenangBookingNew(String salesOrderCode, String orderLine) {
        String query = "SELECT * FROM ITXVIEW_BOOKING_NEW WHERE SALESORDERCODE = ? AND ORDERLINE = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, salesOrderCode, orderLine);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Benang Rajut from ITXVIEW_RAJUT (DB2)
     * PHP: $sql_benang_rajut = db2_exec($conn1, "SELECT * FROM ITXVIEW_RAJUT WHERE (ITEMTYPEAFICODE ='KGF' OR ITEMTYPEAFICODE ='FKG') ...")
     */
    public Map<String, Object> getBenangRajut(String subcode01, String subcode02, String subcode03, 
                                               String subcode04, String noOrder) {
        String query = "SELECT * FROM ITXVIEW_RAJUT WHERE (ITEMTYPEAFICODE ='KGF' OR ITEMTYPEAFICODE ='FKG') " +
                "AND TRIM(SUBCODE01) = ? AND TRIM(SUBCODE02) = ? AND TRIM(SUBCODE03) = ? " +
                "AND TRIM(SUBCODE04) = ? AND TRIM(ORIGDLVSALORDLINESALORDERCODE) = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, subcode01, subcode02, subcode03, subcode04, noOrder);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Tanggal Greige from PRODUCTIONDEMAND with ADSTORAGE (DB2)
     * PHP: $q_tgl_greige = db2_exec($conn1, "SELECT a2.VALUEDATE AS AWAL, a.VALUEDATE AS AKHIR FROM PRODUCTIONDEMAND p ...")
     */
    public Map<String, Object> getTglGreige(String benangRajutCode) {
        String query = "SELECT " +
                "a2.VALUEDATE AS AWAL, " +
                "a.VALUEDATE AS AKHIR " +
                "FROM PRODUCTIONDEMAND p " +
                "LEFT JOIN ADSTORAGE a ON a.UNIQUEID = p.ABSUNIQUEID AND a.FIELDNAME = 'RMPGreigeReqDateTo' " +
                "LEFT JOIN ADSTORAGE a2 ON a2.UNIQUEID = p.ABSUNIQUEID AND a2.FIELDNAME = 'RMPReqDate' " +
                "WHERE CODE = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, benangRajutCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Tanggal Awal Greige RMP from PRODUCTIONDEMAND with ADSTORAGE (DB2)
     * PHP: $tgl_awal_greige_rmp = db2_exec($conn1, "SELECT a.VALUESTRING, a2.VALUEDATE AS AWAL FROM PRODUCTIONDEMAND p ...")
     */
    public Map<String, Object> getTglAwalGreigeRmp(String demandCode) {
        String query = "SELECT " +
                "a.VALUESTRING, " +
                "a2.VALUEDATE AS AWAL " +
                "FROM PRODUCTIONDEMAND p " +
                "LEFT JOIN ADSTORAGE a ON a.UNIQUEID = p.ABSUNIQUEID AND a.FIELDNAME = 'ProAllow' " +
                "LEFT JOIN ADSTORAGE a2 ON a2.UNIQUEID = p.ABSUNIQUEID AND a2.FIELDNAME = 'ProAllowDate' " +
                "WHERE CODE = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, demandCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Tanggal Bagi Kain from ITXVIEW_TGLBAGIKAIN (DB2)
     * PHP: $q_tglbagikain = db2_exec($conn1, "SELECT * FROM ITXVIEW_TGLBAGIKAIN WHERE PRODUCTIONORDERCODE = '$rowdb2[NO_KK]'");
     */
    public Map<String, Object> getTglBagiKain(String productionOrderCode) {
        String query = "SELECT * FROM ITXVIEW_TGLBAGIKAIN WHERE PRODUCTIONORDERCODE = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, productionOrderCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Roll from STOCKTRANSACTION (DB2)
     * PHP: $q_roll_tdk_gabung = db2_exec($conn1, "SELECT count(*) AS ROLL, s2.PRODUCTIONORDERCODE FROM STOCKTRANSACTION s2 WHERE s2.ITEMTYPECODE ='KGF' AND s2.PRODUCTIONORDERCODE = '$rowdb2[NO_KK]' GROUP BY s2.PRODUCTIONORDERCODE");
     */
    public Map<String, Object> getRoll(String productionOrderCode) {
        String query = "SELECT count(*) AS ROLL, s2.PRODUCTIONORDERCODE " +
                "FROM STOCKTRANSACTION s2 " +
                "WHERE s2.ITEMTYPECODE ='KGF' AND s2.PRODUCTIONORDERCODE = ? " +
                "GROUP BY s2.PRODUCTIONORDERCODE";
        try {
            return db2JdbcTemplate.queryForMap(query, productionOrderCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Original PD Code from PRODUCTIONDEMAND with ADSTORAGE (DB2)
     * PHP: $q_orig_pd_code = db2_exec($conn1, "SELECT *, a.VALUESTRING AS ORIGINALPDCODE FROM PRODUCTIONDEMAND p LEFT JOIN ADSTORAGE a ON a.UNIQUEID = p.ABSUNIQUEID AND a.FIELDNAME = 'OriginalPDCode' WHERE p.CODE = '$rowdb2[DEMAND]'");
     */
    public Map<String, Object> getOriginalPdCode(String demandCode) {
        String query = "SELECT *, a.VALUESTRING AS ORIGINALPDCODE " +
                "FROM PRODUCTIONDEMAND p " +
                "LEFT JOIN ADSTORAGE a ON a.UNIQUEID = p.ABSUNIQUEID AND a.FIELDNAME = 'OriginalPDCode' " +
                "WHERE p.CODE = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, demandCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Cek Salinan from PRODUCTIONDEMAND with ADSTORAGE (DB2)
     * PHP: $q_cek_salinan = db2_exec($conn1, "SELECT a2.VALUESTRING AS SALINAN_058 FROM PRODUCTIONDEMAND p LEFT JOIN ADSTORAGE a2 ... WHERE p.CODE = '$rowdb2[DEMAND]'");
     */
    public Map<String, Object> getCekSalinan(String demandCode) {
        String query = "SELECT " +
                "a2.VALUESTRING AS SALINAN_058 " +
                "FROM PRODUCTIONDEMAND p " +
                "LEFT JOIN ADSTORAGE a2 ON a2.UNIQUEID = p.ABSUNIQUEID AND a2.FIELDNAME = 'DefectTypeCode' " +
                "LEFT JOIN USERGENERICGROUP u ON u.CODE = a2.VALUESTRING AND u.USERGENERICGROUPTYPECODE = '006' " +
                "WHERE p.CODE = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, demandCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get QTY Packing from db_qc (MySQL - con_db_qc)
     * PHP: mysqli_query($con_db_qc, $mysql_qtypacking) or sqlsrv_query($con_db_qc, ...)
     * Note: In PHP it checks if mysqli or sqlsrv. Based on koneksi.php, con_db_qc is MySQL
     */
    public Map<String, Object> getQtyPacking(String demandCode) {
        // MySQL query - using TRIM() instead of LTRIM(RTRIM())
        String query = "SELECT nodemand, SUM(jml_mutasi) AS roll, SUM(mutasi) AS mutasi " +
                "FROM tbl_lap_inspeksi " +
                "WHERE TRIM(nodemand) = ? AND dept = 'PACKING' " +
                "GROUP BY TRIM(nodemand)";
        try {
            return qcJdbcTemplate.queryForMap(query, demandCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Netto YD from ITXVIEW_NETTO (DB2)
     * PHP: $sql_netto_yd = db2_exec($conn1, "SELECT * FROM ITXVIEW_NETTO WHERE CODE = '$rowdb2[DEMAND]'");
     */
    public Map<String, Object> getNettoYd(String demandCode) {
        String query = "SELECT * FROM ITXVIEW_NETTO WHERE CODE = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, demandCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get QTY Kurang data from ITXVIEW_SUMMARY_QTY_DELIVERY (DB2)
     * PHP: $sqlQtyKurang = db2_exec($conn1, "SELECT isqd.ORDERLINE, isqd.PELANGGAN, ...")
     */
    public Map<String, Object> getQtyKurang(String noOrder, String orderLine) {
        String query = "SELECT " +
                "isqd.ORDERLINE, " +
                "isqd.PELANGGAN, " +
                "TRIM(isqd.NO_ORDER) AS NO_ORDER, " +
                "isqd.NO_PO, " +
                "isqd.KET_PRODUCT, " +
                "isqd.STYLE, " +
                "isqd.LEBAR, " +
                "isqd.GRAMASI, " +
                "isqd.WARNA, " +
                "isqd.NO_WARNA, " +
                "isqd.PRICEUNITOFMEASURECODE, " +
                "isqd.NETTO, " +
                "isqd.NETTO_2, " +
                "isqd.NETTO_M, " +
                "isqd.KONVERSI, " +
                "isqd.ACTUAL_DELIVERY, " +
                "SUM(isqd.QTY_SUDAH_KIRIM) AS QTY_SUDAH_KIRIM, " +
                "SUM(isqd.QTY_SUDAH_KIRIM_2) AS QTY_SUDAH_KIRIM_2, " +
                "CASE " +
                "    WHEN DAYS(CURRENT DATE) - DAYS(Timestamp_Format(isqd.ACTUAL_DELIVERY, 'YYYY-MM-DD')) < 0 THEN 0 " +
                "    ELSE DAYS(CURRENT DATE) - DAYS(Timestamp_Format(isqd.ACTUAL_DELIVERY, 'YYYY-MM-DD')) " +
                "END AS DELAY, " +
                "isqd.SUBCODE01, " +
                "isqd.SUBCODE02, " +
                "isqd.SUBCODE03, " +
                "isqd.SUBCODE04, " +
                "isqd.SUBCODE05, " +
                "isqd.SUBCODE06, " +
                "isqd.SUBCODE07, " +
                "isqd.SUBCODE08, " +
                "s.STATISTICALGROUPCODE, " +
                "ip.BUYER " +
                "FROM ITXVIEW_SUMMARY_QTY_DELIVERY isqd " +
                "LEFT JOIN SALESORDER s ON s.CODE = isqd.NO_ORDER " +
                "LEFT JOIN ITXVIEW_PELANGGAN ip ON ip.ORDPRNCUSTOMERSUPPLIERCODE = s.ORDPRNCUSTOMERSUPPLIERCODE AND ip.CODE = s.CODE " +
                "WHERE isqd.NO_ORDER = ? AND isqd.ORDERLINE = ? " +
                "GROUP BY " +
                "isqd.ORDERLINE, isqd.PELANGGAN, isqd.NO_ORDER, isqd.NO_PO, isqd.KET_PRODUCT, " +
                "isqd.STYLE, isqd.LEBAR, isqd.GRAMASI, isqd.WARNA, isqd.NO_WARNA, " +
                "isqd.PRICEUNITOFMEASURECODE, isqd.NETTO, isqd.NETTO_2, isqd.NETTO_M, isqd.KONVERSI, " +
                "isqd.ACTUAL_DELIVERY, isqd.SUBCODE01, isqd.SUBCODE02, isqd.SUBCODE03, isqd.SUBCODE04, " +
                "isqd.SUBCODE05, isqd.SUBCODE06, isqd.SUBCODE07, isqd.SUBCODE08, " +
                "s.STATISTICALGROUPCODE, ip.BUYER " +
                "ORDER BY isqd.ORDERLINE ASC";
        try {
            return db2JdbcTemplate.queryForMap(query, noOrder, orderLine);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Lot Code data from ITXVIEWKK (DB2)
     * PHP: $ResultLotCode = "SELECT LISTAGG('''' || TRIM(PRODUCTIONORDERCODE) || '''', ', ' ) AS PRODUCTIONORDERCODE, ..."
     */
    public Map<String, Object> getLotCode(String noOrder, String orderLine) {
        String query = "SELECT " +
                "LISTAGG('''' || TRIM(PRODUCTIONORDERCODE) || '''', ', ' ) AS PRODUCTIONORDERCODE, " +
                "LISTAGG('''' || TRIM(PRODUCTIONDEMANDCODE) || '''', ', ' ) AS PRODUCTIONDEMANDCODE " +
                "FROM ITXVIEWKK " +
                "WHERE PROJECTCODE = ? AND ITEMTYPEAFICODE = 'KFF' AND ORIGDLVSALORDERLINEORDERLINE = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, noOrder, orderLine);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get QTY Ready from BALANCE (DB2)
     * PHP: $sqlQtyReady = "SELECT SUM(BASEPRIMARYQUANTITYUNIT) AS QTY_READY, SUM(BASESECONDARYQUANTITYUNIT) AS QTY_READY_2 FROM BALANCE b WHERE LOTCODE IN (...) ..."
     */
    public Map<String, Object> getQtyReady(String productionOrderCodes, String productionDemandCodes, String noOrder) {
        if (productionOrderCodes == null || productionOrderCodes.isEmpty()) {
            return new HashMap<>();
        }
        String query = "SELECT " +
                "SUM(BASEPRIMARYQUANTITYUNIT) AS QTY_READY, " +
                "SUM(BASESECONDARYQUANTITYUNIT) AS QTY_READY_2 " +
                "FROM BALANCE b " +
                "WHERE LOTCODE IN (" + productionOrderCodes + ") " +
                "AND LEFT(ELEMENTSCODE, 8) IN (" + productionDemandCodes + ") " +
                "AND LOGICALWAREHOUSECODE = 'M031' " +
                "AND PROJECTCODE = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, noOrder);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Status Close from VIEWPRODUCTIONDEMANDSTEP (DB2)
     * PHP: $q_deteksi_status_close = db2_exec($conn1, "SELECT ... FROM VIEWPRODUCTIONDEMANDSTEP p WHERE p.PRODUCTIONORDERCODE = '$rowdb2[NO_KK]' AND (p.PROGRESSSTATUS = '3' OR p.PROGRESSSTATUS = '2') ORDER BY p.GROUPSTEPNUMBER DESC LIMIT 1");
     */
    public Map<String, Object> getStatusClose(String productionOrderCode) {
        String query = "SELECT " +
                "p.PRODUCTIONORDERCODE AS PRODUCTIONORDERCODE, " +
                "TRIM(p.GROUPSTEPNUMBER) AS GROUPSTEPNUMBER, " +
                "TRIM(p.PROGRESSSTATUS) AS PROGRESSSTATUS " +
                "FROM VIEWPRODUCTIONDEMANDSTEP p " +
                "WHERE p.PRODUCTIONORDERCODE = ? AND (p.PROGRESSSTATUS = '3' OR p.PROGRESSSTATUS = '2') " +
                "ORDER BY p.GROUPSTEPNUMBER DESC FETCH FIRST 1 ROWS ONLY";
        try {
            return db2JdbcTemplate.queryForMap(query, productionOrderCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Delay Progress Selesai for PROGRESSSTATUS = '2' (Entered) (DB2)
     * PHP: $q_delay_progress_selesai = db2_exec($conn1, "SELECT ... WHERE p.PRODUCTIONORDERCODE = '$rowdb2[NO_KK]' AND p.PROGRESSSTATUS = '2' ORDER BY p.GROUPSTEPNUMBER DESC LIMIT 1");
     */
    public Map<String, Object> getDelayProgressSelesai(String productionOrderCode) {
        String query = "SELECT " +
                "p.PRODUCTIONORDERCODE AS PRODUCTIONORDERCODE, " +
                "CASE " +
                "    WHEN TRIM(p.STEPTYPE) = '0' THEN p.GROUPSTEPNUMBER " +
                "    WHEN TRIM(p.STEPTYPE) = '3' THEN p2.STEPNUMBER " +
                "    WHEN TRIM(p.STEPTYPE) = '1' THEN p2.STEPNUMBER " +
                "    ELSE p.GROUPSTEPNUMBER " +
                "END AS GROUPSTEPNUMBER, " +
                "iptip.MULAI, " +
                "DAYS(CURRENT DATE) - DAYS(iptip.MULAI) AS DELAY_PROGRESSSTATUS, " +
                "p.PROGRESSSTATUS AS PROGRESSSTATUS " +
                "FROM PRODUCTIONDEMANDSTEP p " +
                "LEFT JOIN PRODUCTIONDEMANDSTEP p2 ON p2.PRODUCTIONORDERCODE = p.PRODUCTIONORDERCODE " +
                "AND p2.STEPTYPE = p.STEPTYPE AND p2.OPERATIONCODE = p.OPERATIONCODE " +
                "LEFT JOIN ITXVIEW_POSISIKK_TGL_IN_PRODORDER iptip ON iptip.PRODUCTIONORDERCODE = p.PRODUCTIONORDERCODE " +
                "AND iptip.DEMANDSTEPSTEPNUMBER = p.STEPNUMBER " +
                "WHERE p.PRODUCTIONORDERCODE = ? AND p.PROGRESSSTATUS = '2' " +
                "ORDER BY p.GROUPSTEPNUMBER DESC FETCH FIRST 1 ROWS ONLY";
        try {
            return db2JdbcTemplate.queryForMap(query, productionOrderCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Delay Progress Mulai for PROGRESSSTATUS = '3' (Progress) (DB2)
     * PHP: $q_delay_progress_mulai = db2_exec($conn1, "SELECT ... WHERE p.PRODUCTIONORDERCODE = '$rowdb2[NO_KK]' AND p.PROGRESSSTATUS = '3' ... ORDER BY ... DESC LIMIT 1");
     */
    public Map<String, Object> getDelayProgressMulai(String productionOrderCode) {
        String query = "SELECT " +
                "p.PRODUCTIONORDERCODE AS PRODUCTIONORDERCODE, " +
                "CASE " +
                "    WHEN TRIM(p.STEPTYPE) = '0' THEN p.GROUPSTEPNUMBER " +
                "    WHEN TRIM(p.STEPTYPE) = '3' THEN p2.STEPNUMBER " +
                "    WHEN TRIM(p.STEPTYPE) = '1' THEN p2.STEPNUMBER " +
                "    ELSE p.GROUPSTEPNUMBER " +
                "END AS GROUPSTEPNUMBER, " +
                "COALESCE(iptop.SELESAI, SUBSTRING(p2.LASTUPDATEDATETIME, 1, 19)) AS SELESAI, " +
                "DAYS(CURRENT DATE) - COALESCE(DAYS(iptop.SELESAI), DAYS(p2.LASTUPDATEDATETIME)) AS DELAY_PROGRESSSTATUS, " +
                "p.PROGRESSSTATUS AS PROGRESSSTATUS " +
                "FROM VIEWPRODUCTIONDEMANDSTEP p " +
                "LEFT JOIN PRODUCTIONDEMANDSTEP p2 ON p2.PRODUCTIONORDERCODE = p.PRODUCTIONORDERCODE " +
                "AND p2.STEPTYPE = p.STEPTYPE AND p2.OPERATIONCODE = p.OPERATIONCODE " +
                "LEFT JOIN ITXVIEW_POSISIKK_TGL_OUT_PRODORDER iptop ON iptop.PRODUCTIONORDERCODE = p.PRODUCTIONORDERCODE " +
                "AND iptop.DEMANDSTEPSTEPNUMBER = " +
                "    CASE " +
                "        WHEN TRIM(p.STEPTYPE) = '0' THEN p.GROUPSTEPNUMBER " +
                "        WHEN TRIM(p.STEPTYPE) = '3' THEN p2.STEPNUMBER " +
                "        WHEN TRIM(p.STEPTYPE) = '1' THEN p2.STEPNUMBER " +
                "        ELSE p.GROUPSTEPNUMBER " +
                "    END " +
                "WHERE p.PRODUCTIONORDERCODE = ? AND p.PROGRESSSTATUS = '3' " +
                "AND (NOT iptop.SELESAI IS NULL OR NOT p2.LASTUPDATEDATETIME IS NULL) " +
                "ORDER BY " +
                "    CASE " +
                "        WHEN TRIM(p.STEPTYPE) = '0' THEN p.GROUPSTEPNUMBER " +
                "        WHEN TRIM(p.STEPTYPE) = '3' THEN p2.STEPNUMBER " +
                "        WHEN TRIM(p.STEPTYPE) = '1' THEN p2.STEPNUMBER " +
                "        ELSE p.GROUPSTEPNUMBER " +
                "    END DESC " +
                "FETCH FIRST 1 ROWS ONLY";
        try {
            return db2JdbcTemplate.queryForMap(query, productionOrderCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get CNP Close data from VIEWPRODUCTIONDEMANDSTEP (DB2)
     * PHP: $q_cnp1 = db2_exec($conn1, "SELECT GROUPSTEPNUMBER, TRIM(OPERATIONCODE) AS OPERATIONCODE, ... WHERE PRODUCTIONORDERCODE = '$rowdb2[NO_KK]' AND PROGRESSSTATUS = 3 ORDER BY GROUPSTEPNUMBER DESC LIMIT 1");
     */
    public Map<String, Object> getCnpClose(String productionOrderCode) {
        String query = "SELECT " +
                "GROUPSTEPNUMBER, " +
                "TRIM(OPERATIONCODE) AS OPERATIONCODE, " +
                "o.LONGDESCRIPTION AS LONGDESCRIPTION, " +
                "PROGRESSSTATUS, " +
                "CASE " +
                "    WHEN PROGRESSSTATUS = 0 THEN 'Entered' " +
                "    WHEN PROGRESSSTATUS = 1 THEN 'Planned' " +
                "    WHEN PROGRESSSTATUS = 2 THEN 'Progress' " +
                "    WHEN PROGRESSSTATUS = 3 THEN 'Closed' " +
                "END AS STATUS_OPERATION " +
                "FROM VIEWPRODUCTIONDEMANDSTEP v " +
                "LEFT JOIN OPERATION o ON o.CODE = v.OPERATIONCODE " +
                "WHERE PRODUCTIONORDERCODE = ? AND PROGRESSSTATUS = 3 " +
                "ORDER BY GROUPSTEPNUMBER DESC FETCH FIRST 1 ROWS ONLY";
        try {
            return db2JdbcTemplate.queryForMap(query, productionOrderCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Total Step from VIEWPRODUCTIONDEMANDSTEP (DB2)
     * PHP: $q_deteksi_total_step = db2_exec($conn1, "SELECT COUNT(*) AS TOTALSTEP FROM VIEWPRODUCTIONDEMANDSTEP WHERE PRODUCTIONORDERCODE = '$rowdb2[NO_KK]'");
     */
    public Map<String, Object> getTotalStep(String productionOrderCode) {
        String query = "SELECT COUNT(*) AS TOTALSTEP FROM VIEWPRODUCTIONDEMANDSTEP WHERE PRODUCTIONORDERCODE = ?";
        try {
            return db2JdbcTemplate.queryForMap(query, productionOrderCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Total Close from VIEWPRODUCTIONDEMANDSTEP (DB2)
     * PHP: $q_deteksi_total_close = db2_exec($conn1, "SELECT COUNT(*) AS TOTALCLOSE FROM VIEWPRODUCTIONDEMANDSTEP WHERE PRODUCTIONORDERCODE = '$rowdb2[NO_KK]' AND PROGRESSSTATUS = 3");
     */
    public Map<String, Object> getTotalClose(String productionOrderCode) {
        String query = "SELECT COUNT(*) AS TOTALCLOSE FROM VIEWPRODUCTIONDEMANDSTEP " +
                "WHERE PRODUCTIONORDERCODE = ? AND PROGRESSSTATUS = 3";
        try {
            return db2JdbcTemplate.queryForMap(query, productionOrderCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Not CNP Close (next step after closed) (DB2)
     * PHP: $q_not_cnp1 = db2_exec($conn1, "SELECT ... WHERE PRODUCTIONORDERCODE = '$rowdb2[NO_KK]' AND GROUPSTEPNUMBER $groupstep_option ORDER BY GROUPSTEPNUMBER ASC LIMIT 1");
     */
    public Map<String, Object> getNotCnpClose(String productionOrderCode, String groupstepOption) {
        String query = "SELECT " +
                "GROUPSTEPNUMBER, " +
                "TRIM(OPERATIONCODE) AS OPERATIONCODE, " +
                "TRIM(o.OPERATIONGROUPCODE) AS OPERATIONGROUPCODE, " +
                "o.LONGDESCRIPTION AS LONGDESCRIPTION, " +
                "PROGRESSSTATUS, " +
                "CASE " +
                "    WHEN PROGRESSSTATUS = 0 THEN 'Entered' " +
                "    WHEN PROGRESSSTATUS = 1 THEN 'Planned' " +
                "    WHEN PROGRESSSTATUS = 2 THEN 'Progress' " +
                "    WHEN PROGRESSSTATUS = 3 THEN 'Closed' " +
                "END AS STATUS_OPERATION " +
                "FROM VIEWPRODUCTIONDEMANDSTEP v " +
                "LEFT JOIN OPERATION o ON o.CODE = v.OPERATIONCODE " +
                "WHERE PRODUCTIONORDERCODE = ? AND GROUPSTEPNUMBER " + groupstepOption + " " +
                "ORDER BY GROUPSTEPNUMBER ASC FETCH FIRST 1 ROWS ONLY";
        try {
            return db2JdbcTemplate.queryForMap(query, productionOrderCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Status Terakhir from VIEWPRODUCTIONDEMANDSTEP (DB2)
     * PHP: $q_StatusTerakhir = db2_exec($conn1, "SELECT p.PRODUCTIONORDERCODE, p.GROUPSTEPNUMBER, ... WHERE p.PRODUCTIONORDERCODE = '$rowdb2[NO_KK]' AND (p.PROGRESSSTATUS = '0' OR p.PROGRESSSTATUS = '1' OR p.PROGRESSSTATUS ='2') AND p.GROUPSTEPNUMBER $groupstep_option ORDER BY p.GROUPSTEPNUMBER ASC LIMIT 1");
     */
    public Map<String, Object> getStatusTerakhir(String productionOrderCode, String groupstepOption) {
        String query = "SELECT " +
                "p.PRODUCTIONORDERCODE, " +
                "p.GROUPSTEPNUMBER, " +
                "p.OPERATIONCODE, " +
                "TRIM(o.OPERATIONGROUPCODE) AS OPERATIONGROUPCODE, " +
                "o.LONGDESCRIPTION AS LONGDESCRIPTION, " +
                "CASE " +
                "    WHEN p.PROGRESSSTATUS = 0 THEN 'Entered' " +
                "    WHEN p.PROGRESSSTATUS = 1 THEN 'Planned' " +
                "    WHEN p.PROGRESSSTATUS = 2 THEN 'Progress' " +
                "    WHEN p.PROGRESSSTATUS = 3 THEN 'Closed' " +
                "END AS STATUS_OPERATION, " +
                "wc.LONGDESCRIPTION AS DEPT, " +
                "p.WORKCENTERCODE " +
                "FROM VIEWPRODUCTIONDEMANDSTEP p " +
                "LEFT JOIN WORKCENTER wc ON wc.CODE = p.WORKCENTERCODE " +
                "LEFT JOIN OPERATION o ON o.CODE = p.OPERATIONCODE " +
                "WHERE p.PRODUCTIONORDERCODE = ? " +
                "AND (p.PROGRESSSTATUS = '0' OR p.PROGRESSSTATUS = '1' OR p.PROGRESSSTATUS ='2') " +
                "AND p.GROUPSTEPNUMBER " + groupstepOption + " " +
                "ORDER BY p.GROUPSTEPNUMBER ASC FETCH FIRST 1 ROWS ONLY";
        try {
            return db2JdbcTemplate.queryForMap(query, productionOrderCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Status Terakhir BKR1 closed from ITXVIEW_POSISI_KARTU_KERJA (DB2)
     * PHP: $q_deteksi_status_terakhir_BKR1 = db2_exec($conn1, "SELECT DISTINCT STEPNUMBER FROM ITXVIEW_POSISI_KARTU_KERJA WHERE PRODUCTIONORDERCODE = '$rowdb2[NO_KK]' AND PRODUCTIONDEMANDCODE = '$rowdb2[DEMAND]' AND NOT STATUS_OPERATION IN ('Entered', 'Progress') AND (OPERATIONCODE = 'BKR1' OR ...) ORDER BY STEPNUMBER DESC FETCH FIRST 1 ROWS ONLY");
     */
    public Map<String, Object> getStatusTerakhirBKR1(String productionOrderCode, String demandCode) {
        String query = "SELECT DISTINCT STEPNUMBER " +
                "FROM ITXVIEW_POSISI_KARTU_KERJA " +
                "WHERE PRODUCTIONORDERCODE = ? " +
                "AND PRODUCTIONDEMANDCODE = ? " +
                "AND NOT STATUS_OPERATION IN ('Entered', 'Progress') " +
                "AND (OPERATIONCODE = 'BKR1' OR OPERATIONCODE = 'MAT1' OR OPERATIONCODE = 'BKN1' " +
                "OR OPERATIONCODE = 'BAT1' OR OPERATIONCODE = 'BAT2' " +
                "OR OPERATIONCODE = 'WAIT35' OR OPERATIONCODE = 'WAIT40' OR OPERATIONCODE = 'WAIT37' " +
                "OR OPERATIONCODE = 'PRE1' OR OPERATIONCODE = 'SUE1' " +
                "OR OPERATIONCODE = 'WAIT33' OR OPERATIONCODE = 'WAIT36') " +
                "ORDER BY STEPNUMBER DESC " +
                "FETCH FIRST 1 ROWS ONLY";
        try {
            return db2JdbcTemplate.queryForMap(query, productionOrderCode, demandCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Status Terakhir closed from ITXVIEW_POSISI_KARTU_KERJA (DB2)
     * PHP: $q_deteksi_status_terakhir = db2_exec($conn1, "SELECT DISTINCT * FROM ITXVIEW_POSISI_KARTU_KERJA WHERE PRODUCTIONORDERCODE = '$rowdb2[NO_KK]' AND PRODUCTIONDEMANDCODE = '$rowdb2[DEMAND]' AND STEPNUMBER > '$row_status_terakhir_closed_BKR1[STEPNUMBER]' ORDER BY STEPNUMBER DESC FETCH FIRST 1 ROWS ONLY;");
     */
    public Map<String, Object> getStatusTerakhirClosed(String productionOrderCode, String demandCode, String stepNumber) {
        String query = "SELECT DISTINCT * " +
                "FROM ITXVIEW_POSISI_KARTU_KERJA " +
                "WHERE PRODUCTIONORDERCODE = ? " +
                "AND PRODUCTIONDEMANDCODE = ? " +
                "AND STEPNUMBER > ? " +
                "ORDER BY STEPNUMBER DESC " +
                "FETCH FIRST 1 ROWS ONLY";
        try {
            return db2JdbcTemplate.queryForMap(query, productionOrderCode, demandCode, stepNumber);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Schedule DYE from SQL Server db_dying (con_db_dyeing)
     * PHP: $sql_schedule_dye = "SELECT TOP 1 * FROM db_dying.tbl_schedule WHERE nokk = ? AND status <> 'selesai' ORDER BY id DESC";
     *      $stmt_schedule_dye = sqlsrv_query($con_db_dyeing, $sql_schedule_dye, [$rowdb2['NO_KK']], ["Scrollable" => SQLSRV_CURSOR_STATIC]);
     */
    public Map<String, Object> getScheduleDye(String nokk) {
        // SQL Server syntax - using TOP 1 instead of LIMIT 1
        String query = "SELECT TOP 1 * FROM tbl_schedule WHERE nokk = ? AND status <> 'selesai' ORDER BY id DESC";
        try {
            return dyeingJdbcTemplate.queryForMap(query, nokk);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Schedule FIN from SQL Server db_finishing (con_db_finishing)
     * PHP: $sql_schedule_fin = "SELECT TOP 1 * FROM db_finishing.tbl_schedule_new WHERE nokk = ? AND nodemand = ? ORDER BY id DESC";
     *      $stmt_schedule_fin = sqlsrv_query($con_db_finishing, $sql_schedule_fin, [$rowdb2['NO_KK'], $rowdb2['DEMAND']], ["Scrollable" => SQLSRV_CURSOR_STATIC]);
     */
    public Map<String, Object> getScheduleFin(String nokk, String nodemand) {
        // SQL Server syntax - using TOP 1 instead of LIMIT 1
        String query = "SELECT TOP 1 * FROM tbl_schedule_new WHERE nokk = ? AND nodemand = ? ORDER BY id DESC";
        try {
            return finishingJdbcTemplate.queryForMap(query, nokk, nodemand);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Get Additional count from PRODUCTIONDEMANDSTEP (DB2)
     * PHP: $additional = db2_exec($conn1, "SELECT COUNT(*) AS TOTAL_ADDITIONAL FROM PRODUCTIONDEMANDSTEP p WHERE p.PRODUCTIONORDERCODE = $pr_order AND p.PRODUCTIONDEMANDCODE = $dmand AND (p.STEPTYPE = 1)");
     */
    public Map<String, Object> getAdditional(String productionOrderCode, String demandCode) {
        String query = "SELECT COUNT(*) AS TOTAL_ADDITIONAL " +
                "FROM PRODUCTIONDEMANDSTEP p " +
                "WHERE p.PRODUCTIONORDERCODE = ? " +
                "AND p.PRODUCTIONDEMANDCODE = ? " +
                "AND (p.STEPTYPE = 1)";
        try {
            return db2JdbcTemplate.queryForMap(query, productionOrderCode, demandCode);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
