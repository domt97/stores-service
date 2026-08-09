package com.dotran.example.store.infrastructure.rest.mapper;

import com.dotran.example.store.application.command.CreateStoreProductCmd;
import com.dotran.example.store.infrastructure.rest.dto.request.CreateStoreProductRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StoreProductRestMapper {

    CreateStoreProductCmd fromCreateRequestToCmd(CreateStoreProductRequest request);
}
