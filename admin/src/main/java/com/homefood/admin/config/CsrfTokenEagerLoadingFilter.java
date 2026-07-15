package com.homefood.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 6 resolves the CSRF token lazily by default: nothing actually reads it (and
 * therefore nothing writes its cookie/session entry) until something calls CsrfToken.getToken() -
 * normally the first <form> the Thymeleaf CSRF dialect processes. On a long page rendered with
 * chunked transfer encoding (e.g. the public shop, with many product cards before its <form> at
 * the bottom), Tomcat can already have flushed and committed the response by the time rendering
 * reaches that form, so the late token resolution fails with "response is already committed" and
 * the page cuts off mid-render. Forcing resolution here, right after CsrfFilter sets the request
 * attribute and before any view rendering starts, avoids that entirely.
 */
@Component
public class CsrfTokenEagerLoadingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
