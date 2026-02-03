package com.indotaichen.laporan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoPentingResponse {

    @JsonProperty("TGL_BUKA_KARTU")
    private String tglBukaKartu;                    // TGL BUKA KARTU (ORDERDATE)

    @JsonProperty("PELANGGAN")
    private String pelanggan;                        // PELANGGAN

    @JsonProperty("NO_ORDER")
    private String noOrder;                          // NO. ORDER

    @JsonProperty("NO_PO")
    private String noPo;                             // NO. PO

    @JsonProperty("KETERANGAN_PRODUCT")
    private String keteranganProduct;                // KETERANGAN PRODUCT

    @JsonProperty("LEBAR")
    private String lebar;                            // LEBAR

    @JsonProperty("GRAMASI")
    private String gramasi;                          // GRAMASI

    @JsonProperty("WARNA")
    private String warna;                            // WARNA

    @JsonProperty("NO_WARNA")
    private String noWarna;                          // NO WARNA

    @JsonProperty("DELIVERY")
    private String delivery;                         // DELIVERY

    @JsonProperty("DELIVERY_ACTUAL")
    private String deliveryActual;                   // DELIVERY ACTUAL

    @JsonProperty("GREIGE_AWAL")
    private String greigeAwal;                       // GREIGE AWAL

    @JsonProperty("GREIGE_AKHIR")
    private String greigeAkhir;                      // GREIGE AKHIR

    @JsonProperty("BAGI_KAIN_TGL")
    private String bagiKainTgl;                      // BAGI KAIN TGL

    @JsonProperty("ROLL")
    private String roll;                             // ROLL

    @JsonProperty("BRUTO_BAGI_KAIN")
    private String brutoBagiKain;                    // BRUTO/BAGI KAIN

    @JsonProperty("QTY_SALINAN")
    private String qtySalinan;                       // QTY SALINAN

    @JsonProperty("QTY_PACKING")
    private String qtyPacking;                       // QTY PACKING

    @JsonProperty("NETTO_KG")
    private String nettoKg;                          // NETTO(kg)

    @JsonProperty("NETTO_YD_MTR")
    private String nettoYdMtr;                       // NETTO(yd/mtr)

    @JsonProperty("QTY_KURANG_KG")
    private String qtyKurangKg;                      // QTY KURANG (KG)

    @JsonProperty("QTY_KURANG_YD_MTR")
    private String qtyKurangYdMtr;                   // QTY KURANG (YD/MTR)

    @JsonProperty("DELAY")
    private String delay;                            // DELAY

    @JsonProperty("TARGET_SELESAI")
    private String targetSelesai;                    // TARGET SELESAI

    @JsonProperty("KODE_DEPT")
    private String kodeDept;                         // KODE DEPT

    @JsonProperty("STATUS_TERAKHIR")
    private String statusTerakhir;                   // STATUS TERAKHIR

    @JsonProperty("NOMOR_MESIN_SCHEDULE")
    private String nomorMesinSchedule;               // NOMOR MESIN SCHEDULE

    @JsonProperty("NOMOR_URUT_SCHEDULE")
    private String nomorUrutSchedule;                // NOMOR URUT SCHEDULE

    @JsonProperty("DELAY_PROGRESS_STATUS")
    private String delayProgressStatus;              // DELAY PROGRESS STATUS

    @JsonProperty("PROGRESS_STATUS")
    private String progressStatus;                   // PROGRESS STATUS

    @JsonProperty("TOTAL_HARI")
    private String totalHari;                        // TOTAL HARI

    @JsonProperty("LOT")
    private String lot;                              // LOT

    @JsonProperty("NO_DEMAND")
    private String noDemand;                         // NO DEMAND

    @JsonProperty("NO_KARTU_KERJA")
    private String noKartuKerja;                     // NO KARTU KERJA

    @JsonProperty("ORIGINAL_PD_CODE")
    private String originalPdCode;                   // ORIGINAL PD CODE

    @JsonProperty("CATATAN_PO_GREIGE")
    private String catatanPoGreige;                  // CATATAN PO GREIGE

    @JsonProperty("KETERANGAN")
    private String keterangan;                       // KETERANGAN

    @JsonProperty("RE_PROSES_ADDITIONAL")
    private String reProsesAdditional;               // RE PROSES ADDITIONAL
}
