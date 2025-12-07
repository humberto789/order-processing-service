package br.com.loomi.orders.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for the application.
 * Configures CORS settings for cross-origin requests.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configures CORS mappings to allow all origins and methods.
     *
     * @param registry the CORS registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("*");
    }
}
