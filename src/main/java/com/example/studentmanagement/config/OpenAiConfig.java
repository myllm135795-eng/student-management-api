package com.example.studentmanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OkHttpClient;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Bean
    public OpenAIClient openAIClient() {
        return OpenAIClient.builder()
                .apiKey(apiKey)
                .httpClient(OkHttpClient.builder().build())
                .build();
    }
}
