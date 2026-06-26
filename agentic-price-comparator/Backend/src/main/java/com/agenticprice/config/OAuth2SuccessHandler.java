package com.agenticprice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        if (email == null) {
            // GitHub doesn't always expose email — fall back to login name
            String login = oauthUser.getAttribute("login");
            email = login != null ? login + "@github" : "unknown@oauth";
        }

        // Redirect to frontend with email as query param so it can be stored in localStorage
        String redirectUrl = frontendUrl + "/dashboard?oauth_email=" + email;
        response.sendRedirect(redirectUrl);
    }
}
