package com.marcos.codereviewagent.infraestructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.github")
public record GithubProperties(
        String token,
        String apiUrl
) {
    public GithubProperties {
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://api.github.com";
        }
    }
}
