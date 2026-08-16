package com.dotran.example.store.common.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;

@Getter
@Setter
@Builder
public class DomainPageRequest {

    private int page;
    private int size;
    private String sortBy;
    private String direction;

    public PageRequest toPageRequest() {
        if (sortBy == null || sortBy.isEmpty()) {
            return PageRequest.of(page, size);
        }

        Sort.Direction sortDirection = Optional.ofNullable(direction)
                .map(String::toUpperCase)
                .map(Sort.Direction::valueOf)
                .orElse(Sort.Direction.ASC);

        return PageRequest.of(page, size, sortDirection, sortBy);
    }
}
