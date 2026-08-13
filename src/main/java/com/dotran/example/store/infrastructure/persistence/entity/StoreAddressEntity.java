package com.dotran.example.store.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "store_addresses")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"store"})
@NoArgsConstructor
public class StoreAddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "store_address_seq")
    @SequenceGenerator(
            name = "store_address_seq",
            sequenceName = "store_address_seq",
            allocationSize = 1
    )
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String ward;
    private String district;
    private String province;
    private String city;
    private String country;
    private String postalCode;
}
