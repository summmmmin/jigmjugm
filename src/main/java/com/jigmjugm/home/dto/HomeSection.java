package com.jigmjugm.home.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter @Builder
public class HomeSection {
    private List<HomeResponse.ChallengeListItem> startSoon;
    private List<HomeResponse.ChallengeListItem> newest;
    private List<HomeResponse.ChallengeListItem> active;
}
