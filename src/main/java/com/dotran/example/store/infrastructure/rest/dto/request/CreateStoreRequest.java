package com.dotran.example.store.infrastructure.rest.dto.request;

import com.dotran.example.store.common.dto.AddressDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoreRequest {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private UUID tenantId;

    @NotNull
    private UUID ownerId;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String phone;

    @Valid
    @NotNull
    private AddressDto address;

    @Valid
    @NotNull
    private StoreConfigRequest config;

    @Valid
    @NotEmpty
    @Size(min = 7, max = 7)
    private List<BusinessHourRequest> businessHours;
}
