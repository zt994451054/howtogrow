package com.howtogrow.backend.infrastructure.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpWechatClient implements WechatClient {
  private final WechatProperties props;
  private final RestClient restClient;

  public HttpWechatClient(WechatProperties props) {
    this.props = props;
    this.restClient = RestClient.builder().baseUrl("https://api.weixin.qq.com").build();
  }

  @Override
  public WechatSession exchangeLoginCode(String code) {
    if (code == null || code.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "code is required");
    }
    if (props.appid() == null
        || props.appid().isBlank()
        || props.secret() == null
        || props.secret().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "wechat app config missing");
    }

    var resp =
        restClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/sns/jscode2session")
                        .queryParam("appid", props.appid())
                        .queryParam("secret", props.secret())
                        .queryParam("js_code", code)
                        .queryParam("grant_type", "authorization_code")
                        .build())
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, res) -> {
              throw new AppException(ErrorCode.UNAUTHORIZED, "wechat login failed");
            })
            .body(WechatSessionResponse.class);

    if (resp == null) {
      throw new AppException(ErrorCode.UNAUTHORIZED, "wechat login failed");
    }
    if (resp.errCode != null && resp.errCode != 0) {
      throw new AppException(ErrorCode.UNAUTHORIZED, "wechat login failed: " + resp.errMsg);
    }
    if (resp.openid == null || resp.openid.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHORIZED, "wechat login failed");
    }
    return new WechatSession(resp.openid, resp.unionid);
  }

  static final class WechatSessionResponse {
    @JsonProperty("openid")
    private String openid;

    @JsonProperty("unionid")
    private String unionid;

    @JsonProperty("errcode")
    private Integer errCode;

    @JsonProperty("errmsg")
    private String errMsg;
  }
}

