package com.dotran.oms.store.infrastructure.rest.api;

import com.dotran.oms.core.annotation.WebAdapter;
import com.dotran.oms.core.domain.id.MerchantId;
import com.dotran.oms.store.application.usecase.merchant.CreateMerchantUseCase;
import com.dotran.oms.store.application.usecase.merchant.GetMerchantUseCase;
import com.dotran.oms.store.infrastructure.rest.dto.request.CreateMerchantRequest;
import com.dotran.oms.store.infrastructure.rest.dto.response.MerchantResponse;
import com.dotran.oms.store.infrastructure.rest.mapper.MerchantRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Merchant Management", description = "Seller self-registration and merchant profile")
@WebAdapter
@RestController
@RequestMapping(value = "/v1/store")
@RequiredArgsConstructor
public class MerchantController {

    private final CreateMerchantUseCase createMerchantUseCase;
    private final GetMerchantUseCase getMerchantUseCase;
    private final MerchantRestMapper merchantRestMapper;

    @Operation(summary = "Register a merchant", description = "A seller registers its company; the merchant is active immediately")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Merchant registered",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MerchantResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
            @ApiResponse(responseCode = "409", description = "Merchant code already exists", content = @Content)
    })
    @PostMapping("/merchants")
    @ResponseStatus(HttpStatus.CREATED)
    public MerchantResponse registerMerchant(@RequestBody @Valid CreateMerchantRequest request) {
        return merchantRestMapper.toMerchantResponse(
                createMerchantUseCase.create(merchantRestMapper.fromRequestToCmd(request)));
    }

    @Operation(summary = "Get merchant", description = "Retrieves the merchant profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Merchant found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MerchantResponse.class))),
            @ApiResponse(responseCode = "404", description = "Merchant not found", content = @Content)
    })
    @GetMapping("/merchants/{merchantId}")
    @ResponseStatus(HttpStatus.OK)
    public MerchantResponse getMerchant(
            @Parameter(description = "Merchant ID", required = true) @PathVariable UUID merchantId) {
        return merchantRestMapper.toMerchantResponse(getMerchantUseCase.getById(MerchantId.of(merchantId)));
    }
}
