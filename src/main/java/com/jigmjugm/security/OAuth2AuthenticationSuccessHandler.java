package com.jigmjugm.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, 
                                       Authentication authentication) {
        // React 앱으로 리다이렉트 (Vite 개발 서버)
        String targetUrl = "http://localhost:5173/";
        
        // 프로덕션 환경에서는 환경변수로 관리
        // String targetUrl = System.getenv("FRONTEND_URL") != null 
        //     ? System.getenv("FRONTEND_URL") 
        //     : "http://localhost:5173/";
        
        return UriComponentsBuilder.fromUriString(targetUrl)
                .build()
                .toUriString();
    }
}