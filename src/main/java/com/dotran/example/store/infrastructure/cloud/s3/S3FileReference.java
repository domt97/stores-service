package com.dotran.example.store.infrastructure.cloud.s3;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class S3FileReference {

    private final String bucket;
    private final String fileName;
}
