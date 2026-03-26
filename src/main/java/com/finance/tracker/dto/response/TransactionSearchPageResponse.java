package com.finance.tracker.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Paged transaction search result")
public class TransactionSearchPageResponse {

    private final List<TransactionResponse> content;
    private final PageMetadataResponse page;

    public static TransactionSearchPageResponse from(Page<TransactionResponse> page) {
        return new TransactionSearchPageResponse(
                page.getContent(),
                PageMetadataResponse.from(page));
    }
}
