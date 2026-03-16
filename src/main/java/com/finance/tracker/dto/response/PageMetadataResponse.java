package com.finance.tracker.dto.response;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PageMetadataResponse {

    private final int size;
    private final int number;
    private final long totalElements;
    private final int totalPages;

    public static PageMetadataResponse from(Page<?> page) {
        return new PageMetadataResponse(
                page.getSize(),
                page.getNumber(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
