package com.howtogrow.backend.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpAiClient implements AiClient {
  private final AiProperties props;
  private final RestClient restClient;

  public HttpAiClient(AiProperties props) {
    this.props = props;
    this.restClient = RestClient.builder().baseUrl(props.baseUrl()).build();
  }

  @Override
  public AiTextResponse generateShortSummary(String prompt) {
    if (props.apiKey() == null || props.apiKey().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "AI_API_KEY missing");
    }
    var req =
        new ChatCompletionsRequest(
            props.model(),
            List.of(
                new ChatMessage(
                    "system",
                    "你是正向育儿陪伴助手。请基于给定信息生成<=70字的共情、正向引导总结，不做诊断、不贴标签。"),
                new ChatMessage("user", prompt)),
            120);

    var resp =
        restClient
            .post()
            .uri(props.chatCompletionsPath())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
            .body(req)
            .retrieve()
            .onStatus(HttpStatusCode::isError, (request, response) -> {
              throw new AppException(ErrorCode.INTERNAL_ERROR, "AI request failed");
            })
            .body(ChatCompletionsResponse.class);

    if (resp == null || resp.choices == null || resp.choices.isEmpty()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "AI response invalid");
    }
    var first = resp.choices.get(0);
    var content = first.message == null ? "" : first.message.content;
    if (content == null) {
      content = "";
    }
    content = content.trim();
    if (content.length() > 120) {
      content = content.substring(0, 120);
    }
    Integer tokenUsage = resp.usage == null ? null : resp.usage.totalTokens;
    return new AiTextResponse(content, resp.model, tokenUsage);
  }

  record ChatCompletionsRequest(
      String model, List<ChatMessage> messages, @JsonProperty("max_tokens") int maxTokens) {}

  record ChatMessage(String role, String content) {}

  static final class ChatCompletionsResponse {
    @JsonProperty("model")
    private String model;

    @JsonProperty("choices")
    private List<Choice> choices;

    @JsonProperty("usage")
    private Usage usage;
  }

  static final class Choice {
    @JsonProperty("message")
    private ChatMessage message;
  }

  static final class Usage {
    @JsonProperty("total_tokens")
    private Integer totalTokens;
  }
}
