package com.dotran.example.store.application.impex.template;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ImportResult {

    private ImportStatus status;
    private Integer totalRecords;
    private Instant startedAt;
    private Instant completedAt;
    private Long executionTimeInMillis;
    private String errorMessage;


    public static ImportResult start() {
        return ImportResult.builder()
                .startedAt(Instant.now())
                .build();
    }

    public void calculateExecutionTime() {
        if (this.startedAt != null && this.completedAt != null) {
            this.executionTimeInMillis = this.completedAt.toEpochMilli() - this.startedAt.toEpochMilli();
        }
    }

    public enum ImportStatus {
        SUCCESS,
        FAILED
    }
}
