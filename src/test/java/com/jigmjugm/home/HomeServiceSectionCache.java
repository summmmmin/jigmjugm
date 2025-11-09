package com.jigmjugm.home;

import com.jigmjugm.certification.repo.CertificationRepository;
import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
import com.jigmjugm.challenge.repo.ChallengeRepository;
import com.jigmjugm.challenge.repo.ChallengeRoundRepository;
import com.jigmjugm.common.util.ChallengePolicy;
import com.jigmjugm.config.RedisContainerConfig;
import com.jigmjugm.config.TestCacheConfig;
import com.jigmjugm.home.repo.HomeChallengeItemView;
import com.jigmjugm.home.service.HomeService;
import com.jigmjugm.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;

@SpringBootTest
@Import(TestCacheConfig.class)          // TTL=2s
class HomeServiceSectionCacheIT extends RedisContainerConfig {

    @Autowired
    HomeService homeService;

    @MockitoBean
    ChallengeRepository challengeRepository;

    @MockitoBean
    ChallengeParticipationRepository participationRepository;

    @MockitoBean
    ChallengeRoundRepository roundRepository;

    @MockitoBean
    CertificationRepository certificationRepository;

    @MockitoBean
    ChallengePolicy policy;

    @BeforeEach
    void stubRepositories() {
        var m = Fixtures.monkey();

        List<HomeChallengeItemView> startSoon = m.giveMeBuilder(HomeChallengeItemView.class).sampleList(2);
        List<HomeChallengeItemView> newest    = m.giveMeBuilder(HomeChallengeItemView.class).sampleList(2);
        List<HomeChallengeItemView> active    = m.giveMeBuilder(HomeChallengeItemView.class).sampleList(2);

        Mockito.when(challengeRepository.homeStartSoon(anyString(), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(startSoon);
        Mockito.when(challengeRepository.homeNewest(anyString(), any(LocalDate.class), any(PageRequest.class)))
                .thenReturn(newest);
        Mockito.when(challengeRepository.homeActive(anyString(), any(LocalDate.class), anyInt(), anyInt(), any(PageRequest.class)))
                .thenReturn(active);
    }

    @Test
    void 동일파라미터_캐시만료후_재계산() throws Exception {
        var section1 = homeService.buildSectionCached("ALL", 3, 28, 5);
        assertAll(
                () -> assertEquals(2, section1.getStartSoon().size()),
                () -> assertEquals(2, section1.getNewest().size()),
                () -> assertEquals(2, section1.getActive().size())
        );

        var section2 = homeService.buildSectionCached("ALL", 3, 28, 5);
        assertEquals(2, section2.getStartSoon().size());

        Mockito.verify(challengeRepository, times(1))
                .homeStartSoon(eq("ALL"), any(LocalDate.class), any(PageRequest.class));
        Mockito.verify(challengeRepository, times(1))
                .homeNewest(eq("ALL"), any(LocalDate.class), any(PageRequest.class));
        Mockito.verify(challengeRepository, times(1))
                .homeActive(eq("ALL"), any(LocalDate.class), eq(28), eq(5), any(PageRequest.class));

        Thread.sleep(2200);

        var section3 = homeService.buildSectionCached("ALL", 3, 28, 5);
        assertEquals(2, section3.getActive());

        Mockito.verify(challengeRepository, times(2))
                .homeStartSoon(eq("ALL"), any(LocalDate.class), any(PageRequest.class));
        Mockito.verify(challengeRepository, times(2))
                .homeNewest(eq("ALL"), any(LocalDate.class), any(PageRequest.class));
        Mockito.verify(challengeRepository, times(2))
                .homeActive(eq("ALL"), any(LocalDate.class), eq(28), eq(5), any(PageRequest.class));
    }
}

