package cn.scarlet.service;

import cn.scarlet.api.*;
import cn.scarlet.mapper.Store;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final Store store;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final Clock clock;

  public AuthService(Store store, PasswordEncoder passwords, JwtEncoder encoder, Clock clock) {
    this.store = store;
    this.passwords = passwords;
    this.encoder = encoder;
    this.clock = clock;
  }

  public Map<String, Object> login(Api.Login input) {
    var a = store.adminByName(input.username());
    if (
      input.password().getBytes(StandardCharsets.UTF_8).length > 72 ||
      a == null ||
      !passwords.matches(input.password(), a.passwordHash)
    ) throw new Problem(401, "BAD_CREDENTIALS", "账号或密码不正确");
    Instant now = clock.instant();
    var claims = JwtClaimsSet.builder()
      .issuer("scarlet-archive")
      .subject(a.id.toString())
      .issuedAt(now)
      .expiresAt(now.plusSeconds(7200))
      .claim("ver", a.tokenVersion)
      .build();
    var token = encoder
      .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
      .getTokenValue();
    return Map.of("token", token, "username", a.username, "expiresIn", 7200);
  }

  public Map<String, Object> me(Long id) {
    var a = store.admin(id);
    if (a == null) throw new Problem(401, "UNAUTHORIZED", "请重新登录");
    return Map.of("id", a.id, "username", a.username);
  }

  public void logout(Long id) {
    store.revoke(id);
  }
}
