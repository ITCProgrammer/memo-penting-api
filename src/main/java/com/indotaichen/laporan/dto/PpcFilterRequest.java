package com.indotaichen.laporan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PpcFilterRequest {
    private String noOrder;         // Bon Order
    private String prodDemand;      // Production Demand
    private String prodOrder;       // Production Order
    private String tgl1;            // Dari Tanggal (format: yyyy-MM-dd)
    private String tgl2;            // Sampai Tanggal (format: yyyy-MM-dd)
    private String noPo;            // Nomor PO
    private String articleGroup;    // Article Group (SUBCODE02)
    private String articleCode;     // Article Code (SUBCODE03)
    private String namaWarna;       // Nama Warna
    private String kkoke;           // KK OKE filter: "ya", "tidak", "sertakan"
    private String orderline;       // Orderline (optional)
}
