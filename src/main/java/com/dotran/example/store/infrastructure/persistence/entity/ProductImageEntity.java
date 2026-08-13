package com.dotran.example.store.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"product"})
@NoArgsConstructor
public class ProductImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_image_seq")
    @SequenceGenerator(
            name = "product_image_seq",
            sequenceName = "product_image_seq",
            allocationSize = 1
    )
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    private Integer displayOrder;

    @ManyToOne
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_images_product")
    )
    private StoreProductEntity product;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
