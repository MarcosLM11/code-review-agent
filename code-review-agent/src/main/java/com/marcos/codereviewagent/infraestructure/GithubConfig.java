package com.marcos.codereviewagent.infraestructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(GithubProperties.class)
class GithubConfig {

    @Bean
    WebClient gitHubWebClient(GithubProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.apiUrl())
                .defaultHeader("Authorization", "Bearer " + properties.token())
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }
}
