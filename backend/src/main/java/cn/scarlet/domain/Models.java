package cn.scarlet.domain;

import java.time.LocalDateTime;
import java.util.List;

public final class Models {

  private Models() {}

  public static class Admin {

    public Long id;
    public String username;
    public String passwordHash;
    public int tokenVersion;
  }

  public static class Category {

    public Long id;
    public String name;
    public String slug;
    public String description;
    public long articleCount;
  }

  public static class Tag {

    public Long id;
    public String name;
    public String slug;
    public long articleCount;
  }

  public static class Article {

    public Long id;
    public String title;
    public String slug;
    public String excerpt;
    public String body;
    public String cover;
    public Long categoryId;
    public String categoryName;
    public String categorySlug;
    public String status;
    public boolean featured;
    public int version;
    public LocalDateTime publishedAt;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public List<Tag> tags;
  }

  public static class Message {

    public Long id;
    public String nickname;
    public String content;
    public String status;
    public LocalDateTime createdAt;
  }

  public static class Friend {

    public Long id;
    public String name;
    public String url;
    public String description;
    public String status;
    public LocalDateTime createdAt;
  }
}
