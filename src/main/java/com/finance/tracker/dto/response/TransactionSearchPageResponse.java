package com.finance.tracker.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TransactionSearchPageResponse {

    private final List<TransactionResponse> content;
    private final PageMetadataResponse page;

    public static TransactionSearchPageResponse from(Page<TransactionResponse> page) {
        return new TransactionSearchPageResponse(
                page.getContent(),
                PageMetadataResponse.from(page));
    }
}
