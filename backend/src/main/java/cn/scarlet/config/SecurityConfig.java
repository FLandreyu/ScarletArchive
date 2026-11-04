package cn.scarlet.config;

import cn.scarlet.api.Api;
import cn.scarlet.mapper.Store;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  Clock clock() {
    return Clock.system(ZoneId.of("Asia/Shanghai"));
  }

  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecretKeySpec key(@Value("${scarlet.jwt-secret:}") String configured) throws Exception {
    byte[] bytes;
    if (!configured.isBlank()) bytes = Base64.getDecoder().decode(configured);
    else {
      Path p = Path.of("data/jwt.key");
      Files.createDirectories(p.getParent());
      if (Files.exists(p)) bytes = Base64.getDecoder().decode(Files.readString(p).trim());
      else {
        bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        Files.writeString(p, Base64.getEncoder().encodeToString(bytes), StandardOpenOption.CREATE_NEW);
      }
    }
    if (bytes.length < 32) throw new IllegalStateException(
      "JWT_SECRET must contain 32 random bytes in Base64"
    );
    return new SecretKeySpec(bytes, "HmacSHA256");
  }

  @Bean
  JwtEncoder encoder(SecretKeySpec key) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  @Bean
  JwtDecoder decoder(SecretKeySpec key, Store store) {
    var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    OAuth2TokenValidator<Jwt> version = jwt -> {
      try {
        var a = store.admin(Long.valueOf(jwt.getSubject()));
        if (
          a != null && a.tokenVersion == ((Number) jwt.getClaim("ver")).intValue()
        ) return OAuth2TokenValidatorResult.success();
      } catch (Exception ignored) {}
      return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "已退出或令牌无效", null));
    };
    decoder.setJwtValidator(
      new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("scarlet-archive"), version)
    );
    return decoder;
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http, ObjectMapper json) throws Exception {
    var entry = new org.springframework.security.web.AuthenticationEntryPoint() {
      public void commence(
        jakarta.servlet.http.HttpServletRequest req,
        jakarta.servlet.http.HttpServletResponse res,
        org.springframework.security.core.AuthenticationException e
      ) throws java.io.IOException {
        res.setStatus(401);
        res.setContentType("application/json;charset=UTF-8");
        json.writeValue(res.getWriter(), new Api.Result<>("UNAUTHORIZED", "请重新登录管理后台", null));
      }
    };
    http
      .csrf(c -> c.disable())
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a ->
        a
          .requestMatchers("/api/auth/login")
          .permitAll()
          .requestMatchers("/api/admin/**", "/api/auth/me", "/api/auth/logout")
          .authenticated()
          .anyRequest()
          .permitAll()
      )
      .oauth2ResourceServer(o -> o.jwt(j -> {}).authenticationEntryPoint(entry))
      .exceptionHandling(e -> e.authenticationEntryPoint(entry))
      .headers(h ->
        h.contentSecurityPolicy(c ->
          c.policyDirectives(
            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; object-src 'none'"
          )
        )
      );
    return http.build();
  }
}
