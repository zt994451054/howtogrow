package com.howtogrow.backend.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class OpenAiStreamClient {
  private final AiProperties props;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public OpenAiStreamClient(AiProperties props, ObjectMapper objectMapper) {
    this.props = props;
    this.objectMapper = objectMapper;
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
  }

  public void streamChatCompletions(
      List<AiChatClient.ChatMessage> messages,
      Consumer<String> onDelta,
      Consumer<StreamDone> onDone) {
    if (props.apiKey() == null || props.apiKey().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "OPENAI_API_KEY missing");
    }
    var url = joinUrl(props.baseUrl(), props.chatCompletionsPath());
    var requestBody = buildRequestBody(messages);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(120))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build();

    HttpResponse<java.io.InputStream> response;
    try {
      response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
    } catch (Exception e) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "AI request failed");
    }

    if (response.statusCode() / 100 != 2) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "AI request failed");
    }

    String modelName = props.model();
    Integer totalTokens = null;

    try (var reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }
        if (!line.startsWith("data:")) {
          continue;
        }
        var data = line.substring("data:".length()).trim();
        if ("[DONE]".equals(data)) {
          break;
        }
        var node = safeReadTree(data);
        if (node == null) {
          continue;
        }
        var maybeModel = node.path("model").asText(null);
        if (maybeModel != null && !maybeModel.isBlank()) {
          modelName = maybeModel;
        }
        var usage = node.path("usage");
        if (!usage.isMissingNode()) {
          var tt = usage.path("total_tokens").asInt(-1);
          if (tt >= 0) {
            totalTokens = tt;
          }
        }
        var delta =
            node.path("choices").path(0).path("delta").path("content").asText(null);
        if (delta != null && !delta.isBlank()) {
          onDelta.accept(delta);
        }
      }
    } catch (Exception e) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "AI stream failed");
    }

    onDone.accept(new StreamDone(modelName, totalTokens));
  }

  private String buildRequestBody(List<AiChatClient.ChatMessage> messages) {
    var payload =
        objectMapper.createObjectNode()
            .put("model", props.model())
            .put("stream", true);
    var arr = payload.putArray("messages");
    for (var m : messages) {
      var obj = arr.addObject();
      obj.put("role", m.role());
      obj.put("content", m.content());
    }
    return payload.toString();
  }

  private JsonNode safeReadTree(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (Exception e) {
      return null;
    }
  }

  private static String joinUrl(String baseUrl, String path) {
    if (baseUrl == null) {
      baseUrl = "";
    }
    if (path == null) {
      path = "";
    }
    var b = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    var p = path.startsWith("/") ? path : "/" + path;
    return b + p;
  }

  public record StreamDone(String modelName, Integer tokenUsage) {}
}

