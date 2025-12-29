package com.howtogrow.backend.auth;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
  private final JwtProperties props;
  private final Clock clock;

  public JwtService(JwtProperties props, Clock clock) {
    this.props = props;
    this.clock = clock;
  }

  public String issue(Audience audience, long subjectId) {
    var now = Instant.now(clock);
    var expiresAt = now.plus(Duration.ofSeconds(props.ttlSeconds()));
    var claims =
        new JWTClaimsSet.Builder()
            .subject(Long.toString(subjectId))
            .audience(audience.name().toLowerCase())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiresAt))
            .build();

    var header =
        new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(audience.name().toLowerCase()).build();
    var jwt = new SignedJWT(header, claims);
    try {
      jwt.sign(new MACSigner(secretFor(audience)));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "jwt sign failed");
    }
  }

  public AuthUser verify(String token) {
    SignedJWT jwt;
    try {
      jwt = SignedJWT.parse(token);
    } catch (ParseException e) {
      throw new AppException(ErrorCode.UNAUTHORIZED, "invalid token");
    }

    var keyId = jwt.getHeader().getKeyID();
    Audience preferred = null;
    if ("miniprogram".equalsIgnoreCase(keyId)) {
      preferred = Audience.MINIPROGRAM;
    } else if ("admin".equalsIgnoreCase(keyId)) {
      preferred = Audience.ADMIN;
    }

    if (preferred != null && verifyWith(jwt, preferred)) {
      return toAuthUser(jwt);
    }
    if (verifyWith(jwt, Audience.MINIPROGRAM) || verifyWith(jwt, Audience.ADMIN)) {
      return toAuthUser(jwt);
    }
    throw new AppException(ErrorCode.UNAUTHORIZED, "invalid token");
  }

  private boolean verifyWith(SignedJWT jwt, Audience audience) {
    try {
      return jwt.verify(new MACVerifier(secretFor(audience)));
    } catch (JOSEException e) {
      return false;
    }
  }

  private byte[] secretFor(Audience audience) {
    var secret =
        audience == Audience.MINIPROGRAM ? props.miniprogramSecret() : props.adminSecret();
    return secret.getBytes(StandardCharsets.UTF_8);
  }

  private AuthUser toAuthUser(SignedJWT jwt) {
    JWTClaimsSet claims;
    try {
      claims = jwt.getJWTClaimsSet();
    } catch (ParseException e) {
      throw new AppException(ErrorCode.UNAUTHORIZED, "invalid token");
    }

    var exp = claims.getExpirationTime();
    if (exp == null || exp.toInstant().isBefore(Instant.now(clock))) {
      throw new AppException(ErrorCode.UNAUTHORIZED, "token expired");
    }

    var sub = claims.getSubject();
    if (sub == null || sub.isBlank()) {
      throw new AppException(ErrorCode.UNAUTHORIZED, "invalid token");
    }

    var audClaim = claims.getAudience().stream().findFirst().orElse("");
    var audience =
        "admin".equalsIgnoreCase(audClaim) ? Audience.ADMIN : Audience.MINIPROGRAM;
    return new AuthUser(Long.parseLong(sub), audience);
  }
}

