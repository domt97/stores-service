package com.dotran.example.store.common.utils;

import com.dotran.example.store.common.dto.DomainPageRequest;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;

@UtilityClass
public class PageUtils {

    public static PageRequest toPageRequest(DomainPageRequest domainPageRequest) {
        if (StringUtils.isNullOrEmpty(domainPageRequest.getSortBy())) {
            return PageRequest.of(domainPageRequest.getPageNumber(), domainPageRequest.getPageSize());
        }

        Sort.Direction sortDirection = Optional.ofNullable(domainPageRequest.getDirection())
                .map(String::toUpperCase)
                .map(Sort.Direction::valueOf)
                .orElse(Sort.Direction.ASC);

        return PageRequest.of(
                domainPageRequest.getPageNumber(),
                domainPageRequest.getPageSize(),
                sortDirection,
                domainPageRequest.getSortBy());
    }
}
