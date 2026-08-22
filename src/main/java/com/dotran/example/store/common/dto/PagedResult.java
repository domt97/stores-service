package com.dotran.example.store.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResult<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean isLast;

    /**
     * Converts a Spring Data Page to PagedResult with content transformation.
     *
     * @param page      the source page
     * @param converter the function to convert each element from type S to type T
     * @param <S>       source type
     * @param <T>       target type
     * @return PagedResult with converted content
     */
    public static <S, T> PagedResult<T> of(Page<S> page, Function<S, T> converter) {
        List<T> convertedContent = page.getContent().stream()
                .map(converter)
                .toList();

        return PagedResult.<T>builder()
                .content(convertedContent)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    /**
     * Converts a Spring Data Page to PagedResult without transformation.
     *
     * @param page the source page
     * @param <T>  content type
     * @return PagedResult with same content type
     */
    public static <T> PagedResult<T> of(Page<T> page) {
        return PagedResult.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    /**
     * Converts this PagedResult's content to a different type using the provided converter function.
     * Pagination metadata (pageNumber, pageSize, totalElements, etc.) is preserved.
     *
     * @param converter the function to convert each element from type T to type U
     * @param <U>       target content type
     * @return new PagedResult with converted content
     */
    public <U> PagedResult<U> map(Function<T, U> converter) {
        List<U> convertedContent = this.content.stream()
                .map(converter)
                .toList();

        return PagedResult.<U>builder()
                .content(convertedContent)
                .pageNumber(this.pageNumber)
                .pageSize(this.pageSize)
                .totalElements(this.totalElements)
                .totalPages(this.totalPages)
                .isLast(this.isLast)
                .build();
    }
}
