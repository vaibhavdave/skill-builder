package com.skillbuilder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.anthropic")
public record AnthropicProperties(String model) {
}
