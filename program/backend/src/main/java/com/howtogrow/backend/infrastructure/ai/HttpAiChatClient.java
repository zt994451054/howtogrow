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
public class HttpAiChatClient implements AiChatClient {
  private final AiProperties props;
  private final RestClient restClient;

  public HttpAiChatClient(AiProperties props) {
    this.props = props;
    this.restClient = RestClient.builder().baseUrl(props.baseUrl()).build();
  }

  @Override
  public String chat(List<ChatMessage> messages) {
    if (props.apiKey() == null || props.apiKey().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "AI_API_KEY missing");
    }
    var req =
        new ChatCompletionsRequest(
            props.model(),
            prependSystem(messages),
            512);

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
    var content = first.message == null ? "" : first.message.content();
    if (content == null) {
      return "";
    }
    return content.trim();
  }

  private static List<ChatMessage> prependSystem(List<ChatMessage> messages) {
    return java.util.stream.Stream.concat(
            java.util.stream.Stream.of(
                new ChatMessage(
                    "system",
                    "你是正向育儿陪伴助手。请共情、具体、可执行，不做诊断、不贴标签。")),
            messages == null ? java.util.stream.Stream.empty() : messages.stream())
        .toList();
  }

  record ChatCompletionsRequest(
      String model, List<ChatMessage> messages, @JsonProperty("max_tokens") int maxTokens) {}

  static final class ChatCompletionsResponse {
    @JsonProperty("choices")
    private List<Choice> choices;
  }

  static final class Choice {
    @JsonProperty("message")
    private ChatMessage message;
  }
}
