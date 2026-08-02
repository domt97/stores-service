package com.dotran.example.store.infrastructure.rest.exception;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ErrorResponse {

    private Instant timestamp;
    private Integer status;
    private String error;
    private List<String> errors;
    private String path;
}
