package com.homefood.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * The session cookie's Max-Age is set once, at login, and the servlet container
 * never resends it just because the session is still active - so a session that's
 * genuinely alive for 24h of continued daily use would still get logged out once the
 * *cookie* (not the session) reaches its original 24h mark. Re-issuing the cookie with
 * a fresh Max-Age on every authenticated request makes "stay logged in as long as you
 * keep visiting at least once a day" actually true, not just true for one sitting.
 */
@Component
public class SlidingSessionCookieFilter extends OncePerRequestFilter {

    private static final int MAX_AGE_SECONDS = 24 * 60 * 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Must run before chain.doFilter(): once the downstream chain renders the response,
        // headers are committed and addCookie() silently does nothing.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
        HttpSession session = request.getSession(false);
        if (authenticated && session != null) {
            String path = request.getContextPath().isEmpty() ? "/" : request.getContextPath();
            Cookie cookie = new Cookie("JSESSIONID", session.getId());
            cookie.setHttpOnly(true);
            cookie.setSecure(request.isSecure());
            cookie.setPath(path);
            cookie.setMaxAge(MAX_AGE_SECONDS);
            response.addCookie(cookie);
        }

        chain.doFilter(request, response);
    }
}
