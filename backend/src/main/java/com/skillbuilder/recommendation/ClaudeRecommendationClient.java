package com.skillbuilder.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.skillbuilder.config.AnthropicProperties;
import com.skillbuilder.recommendation.RecommendationDtos.GeneratedResource;
import com.skillbuilder.recommendation.RecommendationDtos.GeneratedResources;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Asks Claude for the best books, courses, and YouTube videos to learn a skill.
 * Uses structured JSON output so the response always parses, and effort HIGH.
 */
@Component
public class ClaudeRecommendationClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AnthropicProperties properties;
    private volatile AnthropicClient client;

    public ClaudeRecommendationClient(AnthropicProperties properties) {
        this.properties = properties;
    }

    public List<GeneratedResource> recommendResources(String skillName, String skillDescription) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(8000L)
                .outputConfig(OutputConfig.builder()
                        .effort(OutputConfig.Effort.HIGH)
                        .format(JsonOutputFormat.builder().schema(schema()).build())
                        .build())
                .addUserMessage(prompt(skillName, skillDescription))
                .build();

        Message message;
        try {
            message = client().messages().create(params);
        } catch (AnthropicException e) {
            throw new RecommendationUnavailableException(
                    "Claude could not generate recommendations right now: " + e.getMessage(), e);
        }

        if (message.stopReason().isPresent() && message.stopReason().get().equals(StopReason.REFUSAL)) {
            throw new RecommendationUnavailableException(
                    "Claude declined to generate recommendations for this skill.");
        }

        String json = message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .findFirst()
                .orElseThrow(() -> new RecommendationUnavailableException(
                        "Claude returned an empty response; please try again."));

        try {
            GeneratedResources parsed = MAPPER.readValue(json, GeneratedResources.class);
            if (parsed.resources() == null || parsed.resources().isEmpty()) {
                throw new RecommendationUnavailableException(
                        "Claude returned no recommendations; please try again.");
            }
            return parsed.resources();
        } catch (com.fasterxml.jackson.core.JacksonException e) {
            throw new RecommendationUnavailableException(
                    "Claude returned an unreadable response; please try again.", e);
        }
    }

    private AnthropicClient client() {
        if (!StringUtils.hasText(System.getenv("ANTHROPIC_API_KEY"))) {
            throw new RecommendationUnavailableException(
                    "ANTHROPIC_API_KEY is not configured. Set it in the backend environment to "
                            + "enable Claude-powered recommendations.");
        }
        AnthropicClient local = client;
        if (local == null) {
            synchronized (this) {
                if (client == null) {
                    client = AnthropicOkHttpClient.fromEnv();
                }
                local = client;
            }
        }
        return local;
    }

    private static String prompt(String skillName, String skillDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append("I want to learn the skill: \"").append(skillName).append("\".\n");
        if (StringUtils.hasText(skillDescription)) {
            sb.append("Context about my goal: ").append(skillDescription).append("\n");
        }
        sb.append("""

                Recommend the best learning resources for this skill:
                - about 4 books (type BOOK)
                - about 4 online courses (type COURSE)
                - about 4 YouTube videos or channels (type YOUTUBE_VIDEO)

                For each resource provide:
                - title: the exact, real title
                - creator: the author, instructor/platform, or channel
                - reason: one sentence on why it is worth my time
                - stars: an integer 1-5 rating of its value for learning this skill
                  (5 = essential, widely regarded as the best; 1 = marginal)
                - searchQuery: a short search string that reliably finds the resource
                  (title plus creator; do NOT include a URL)

                Only recommend real, well-regarded resources you are confident exist.
                Rate honestly - not everything deserves 5 stars.
                """);
        return sb.toString();
    }

    private static JsonOutputFormat.Schema schema() {
        Map<String, Object> resource = Map.of(
                "type", "object",
                "properties", Map.of(
                        "type", Map.of("type", "string", "enum", List.of("BOOK", "COURSE", "YOUTUBE_VIDEO")),
                        "title", Map.of("type", "string"),
                        "creator", Map.of("type", "string"),
                        "reason", Map.of("type", "string"),
                        "stars", Map.of("type", "integer", "enum", List.of(1, 2, 3, 4, 5)),
                        "searchQuery", Map.of("type", "string")),
                "required", List.of("type", "title", "creator", "reason", "stars", "searchQuery"),
                "additionalProperties", false);
        return JsonOutputFormat.Schema.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(Map.of(
                        "resources", Map.of("type", "array", "items", resource))))
                .putAdditionalProperty("required", JsonValue.from(List.of("resources")))
                .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                .build();
    }
}
