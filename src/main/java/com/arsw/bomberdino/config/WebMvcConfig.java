package com.arsw.bomberdino.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * Configuración de WebMVC para registrar interceptores
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LatencyInterceptor latencyInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Registrar interceptor de latencia para todos los endpoints
        registry.addInterceptor(latencyInterceptor)
                .addPathPatterns("/api/**"); // Solo medir APIs, no statics
    }
}
