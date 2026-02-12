package com.alang.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration.
 *
 * Additional Spring MVC configuration if needed.
 *
 * TODO: Add custom argument resolvers if needed
 * TODO: Add interceptors for logging, rate limiting
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // TODO: Add custom configuration

    /**
     * Example: Add custom argument resolver to extract user ID from JWT
     * (Alternative to @AuthenticationPrincipal if you implement custom auth)
     */
    // @Override
    // public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    //     resolvers.add(new CurrentUserArgumentResolver());
    // }

    /**
     * Example: Add interceptor for request logging
     */
    // @Override
    // public void addInterceptors(InterceptorRegistry registry) {
    //     registry.addInterceptor(new RequestLoggingInterceptor());
    // }
}
