package com.indotaichen.laporan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressEvent {
    private String stage;
    private int current;
    private int total;
    private int percent;
    private String message;
    private String detail;
    private Object data;
    private long elapsedMs;
}
