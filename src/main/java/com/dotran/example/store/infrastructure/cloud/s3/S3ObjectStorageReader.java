package com.dotran.example.store.infrastructure.cloud.s3;

import com.dotran.example.store.application.files.ObjectStorageReader;
import com.dotran.example.store.common.annotation.FileAdapter;
import com.dotran.example.store.infrastructure.cloud.AWSException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

@FileAdapter
@RequiredArgsConstructor
@Slf4j
public class S3ObjectStorageReader implements ObjectStorageReader {

    private final S3Client s3Client;

    @Override
    public InputStream read(String bucket, String key) {
        if (!isBucketExist(bucket)) {
            throw new AWSException("Bucket " + bucket + " does not exist");
        }

        GetObjectRequest request =
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();

        return s3Client.getObject(request);
    }

    private HeadBucketResponse headBucket(String bucket) {
        log.info("Checking if bucket {} exists", bucket);
        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucket)
                .build();

        HeadBucketResponse headBucketResponse = s3Client.headBucket(headBucketRequest);
        log.info("Bucket {} exists, status code: {}", bucket, headBucketResponse.sdkHttpResponse().statusCode());

        return headBucketResponse;
    }

    private boolean isBucketExist(String bucket) {
        try {
            this.headBucket(bucket);
            return true;
        } catch (NoSuchBucketException noSuchBucket) {
            log.warn("Bucket {} does not exist", bucket);
            return false;
        } catch (S3Exception e) {
            log.error("Error checking if bucket {} exists, error: {}", bucket, e.getMessage(), e);
            throw e;
        }
    }
}
