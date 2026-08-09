package com.codestorykh.account.client.screening;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds the screening {@link RestClient} from Boot's observation-enabled
 * {@link RestClient.Builder} so W3C {@code traceparent} is propagated and both
 * services share one trace id.
 */
@Configuration
public class ScreeningClientConfig {

    @Bean
    RestClient screeningRestClient(
            RestClient.Builder restClientBuilder,
            JsonMapper jsonMapper,
            @Value("${screening.service.base-url:http://localhost:8082}") String baseUrl
    ) {
        JacksonJsonHttpMessageConverter jackson = new JacksonJsonHttpMessageConverter(jsonMapper);
        return restClientBuilder
                .baseUrl(baseUrl)
                .messageConverters(converters -> {
                    converters.removeIf(JacksonJsonHttpMessageConverter.class::isInstance);
                    converters.add(jackson);
                })
                .build();
    }
}
