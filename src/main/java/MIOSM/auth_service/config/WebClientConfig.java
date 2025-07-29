package MIOSM.auth_service.config;

import feign.okhttp.OkHttpClient;
import org.springframework.context.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    @Bean public OkHttpClient client() { return new OkHttpClient(); }
}