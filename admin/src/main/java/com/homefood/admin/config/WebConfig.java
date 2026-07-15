package com.homefood.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.Locale;

/** RU/EN toggle for the public shop page (see shop/index.html "lang" links + messages*.properties).
 * Remembered per-visitor via a cookie so it sticks across visits, not just the one session. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("shop_lang");
        resolver.setDefaultLocaleFunction(request -> new Locale("ru"));
        resolver.setCookieMaxAge(Duration.ofDays(365));
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Scoped to the public shop - the admin UI has no message-driven text, no need for it there.
        registry.addInterceptor(localeChangeInterceptor()).addPathPatterns("/shop/**");
    }
}
