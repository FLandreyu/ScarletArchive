package cn.scarlet.api;

import jakarta.validation.constraints.*;
import java.util.*;

public final class Api {

  private Api() {}

  public record Result<T>(String code, String message, T data) {
    public static <T> Result<T> ok(T data) {
      return new Result<>("OK", "操作成功", data);
    }
  }

  public record Page<T>(List<T> items, long total, int page, int size) {}

  public record Login(@NotBlank @Size(max = 32) String username, @NotBlank @Size(max = 64) String password) {}

  public record ArticleInput(
    @NotBlank @Size(max = 160) String title,
    @NotNull @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 180) String slug,
    @NotBlank @Size(max = 400) String excerpt,
    @NotBlank @Size(max = 100000) String body,
    @NotNull @Size(max = 500) String cover,
    @NotNull @Positive Long categoryId,
    @NotNull @Size(max = 12) List<@NotNull @Positive Long> tagIds,
    @NotNull @Pattern(regexp = "DRAFT|PUBLISHED") String status,
    boolean featured,
    @Min(0) int version
  ) {}

  public record Taxonomy(
    @NotBlank @Size(max = 40) String name,
    @NotNull @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max = 60) String slug,
    @Size(max = 200) String description
  ) {}

  public record GuestInput(
    @NotBlank @Size(max = 30) String nickname,
    @NotBlank @Size(min = 3, max = 800) String content,
    @Size(max = 100) String website
  ) {}

  public record FriendInput(
    @NotBlank @Size(max = 60) String name,
    @NotBlank @Size(max = 300) String url,
    @NotBlank @Size(max = 200) String description
  ) {}

  public record Moderation(@NotNull @Pattern(regexp = "PENDING|APPROVED") String status) {}

  public record SiteInput(
    @NotBlank @Size(max = 60) String title,
    @NotBlank @Size(max = 120) String subtitle,
    @NotBlank @Size(max = 40) String owner,
    @NotBlank @Size(max = 5000) String about,
    @NotNull @Size(max = 300) String github,
    @NotNull @Size(max = 120) String email,
    @NotNull @Size(max = 500) String interests,
    @NotNull @Size(max = 3000) String timeline,
    @NotNull @Size(max = 3000) String projects
  ) {}
}
