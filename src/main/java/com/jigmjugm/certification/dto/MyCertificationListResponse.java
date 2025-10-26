package com.jigmjugm.certification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyCertificationListResponse {
    private java.util.List<MyCertificationRow> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Summary summary;

    @Getter @Builder
    public static class Summary { private Long accumulatedAmount; }
}
