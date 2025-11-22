package com.jigmjugm.home.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter @Builder
public class HomeRedisResponse {
    private HomeSection all;
    private HomeSection saving;
    private HomeSection installment;
    private HomeSection other;
    private List<HomeResponse.MyUpcomingItem> myUpcoming;
}
