package com.dotran.example.store.infrastructure.cloud.s3;

import com.dotran.example.store.infrastructure.cloud.AwsProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class S3Properties extends AwsProperties {

    @Value("${aws.s3.store-product-bucket:localdev_store_products}")
    private String storeProductBucket;
}
