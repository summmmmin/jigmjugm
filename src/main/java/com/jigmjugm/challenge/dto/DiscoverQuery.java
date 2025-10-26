package com.jigmjugm.challenge.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiscoverQuery {
    private String categoryType = "ALL"; // ALL|SAVING|INSTALLMENT|OTHER
    private String status;               // PENDING|ACTIVE|COMPLETED|null
    private Integer page = 0;
    private Integer size = 20;
    private String sort = "startDate,asc";
}
