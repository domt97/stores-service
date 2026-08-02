package com.dotran.example.store.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "store_configs")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class StoreConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "store_config_seq")
    @SequenceGenerator(
            name = "store_config_seq",
            sequenceName = "store_config_seq",
            allocationSize = 1
    )
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne
    @JoinColumn(name = "store_id")
    private StoreEntity store;

    private boolean autoAcceptOrder;

    @Column(name = "allow_preorder")
    private boolean allowPreOrder;

    private LocalTime openingTime;
    private LocalTime closingTime;

    @Column(name = "timezone")
    private String timeZone;

    private String currency;
    private Integer maxOrdersPerDay;
    private Integer preparationTimeMinutes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
