package com.jigmjugm.home;

import com.jigmjugm.config.RedisContainerConfig;
import com.jigmjugm.config.TestCacheConfig;
import com.jigmjugm.home.dto.HomeRedisResponse;
import com.jigmjugm.home.dto.HomeResponse;
import com.jigmjugm.home.dto.HomeSection;
import com.jigmjugm.home.service.HomeService;
import com.jigmjugm.testsupport.Fixtures;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestCacheConfig.class)
class HomeControllerCache extends RedisContainerConfig {

    @LocalServerPort int port;

    @Resource
    TestRestTemplate rest;

    @MockitoSpyBean
    HomeService homeService;

    private HomeSection makeSection(int allSize, int newestSize, int activeSize) {
        var monkey = Fixtures.monkey();
        return HomeSection.builder()
                .startSoon(monkey.giveMeBuilder(HomeResponse.ChallengeListItem.class).sampleList(allSize))
                .newest(monkey.giveMeBuilder(HomeResponse.ChallengeListItem.class).sampleList(newestSize))
                .active(monkey.giveMeBuilder(HomeResponse.ChallengeListItem.class).sampleList(activeSize))
                .build();
    }

    @BeforeEach
    void stubMyUpcoming() {
        var monkey = Fixtures.monkey();
        Mockito.doReturn(monkey.giveMeBuilder(HomeResponse.MyUpcomingItem.class).sampleList(2))
                .when(homeService).getMyUpcoming(Mockito.anyLong(), Mockito.anyInt());
    }

    @Test
    void 섹션은_캐시_myUpcoming은_계산() throws Exception {
        String url = "http://localhost:" + port + "/api/v1/home-redis";

        ResponseEntity<HomeRedisResponse> r1 = rest.getForEntity(url, HomeRedisResponse.class);
        assertThat(r1.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<HomeRedisResponse> r2 = rest.getForEntity(url, HomeRedisResponse.class);
        assertThat(r2.getStatusCode().is2xxSuccessful()).isTrue();

        Mockito.verify(homeService, times(1))
                .buildSectionCached("ALL", 3, 28, 5);
        Mockito.verify(homeService, times(1))
                .buildSectionCached("SAVING", 3, 28, 5);
        Mockito.verify(homeService, times(1))
                .buildSectionCached("INSTALLMENT", 3, 28, 5);
        Mockito.verify(homeService, times(1))
                .buildSectionCached("OTHER", 3, 28, 5);

        Thread.sleep(2200);

        ResponseEntity<HomeRedisResponse> r3 = rest.getForEntity(url, HomeRedisResponse.class);
        assertThat(r3.getStatusCode().is2xxSuccessful()).isTrue();

        Mockito.verify(homeService, times(2))
                .buildSectionCached("ALL", 3, 28, 5);
        Mockito.verify(homeService, times(2))
                .buildSectionCached("SAVING", 3, 28, 5);
        Mockito.verify(homeService, times(2))
                .buildSectionCached("INSTALLMENT", 3, 28, 5);
        Mockito.verify(homeService, times(2))
                .buildSectionCached("OTHER", 3, 28, 5);
    }
}
