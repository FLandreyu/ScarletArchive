package cn.scarlet.service;

import cn.scarlet.api.*;
import cn.scarlet.domain.Models.*;
import cn.scarlet.mapper.Store;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlogService {

  private final Store store;
  private final Clock clock;
  private final ObjectMapper json;

  public BlogService(Store store, Clock clock, ObjectMapper json) {
    this.store = store;
    this.clock = clock;
    this.json = json;
  }

  public Api.Page<Article> list(
    String q,
    String category,
    String tag,
    String sort,
    boolean admin,
    String status,
    Boolean featured,
    int page,
    int size
  ) {
    if (
      !Set.of("newest", "oldest").contains(sort) ||
      q.length() > 100 ||
      category.length() > 60 ||
      tag.length() > 60 ||
      (!status.isBlank() && !Set.of("DRAFT", "PUBLISHED").contains(status))
    ) throw new Problem(400, "VALIDATION", "筛选参数无效");
    var items = store.articles(q, category, tag, sort, admin, status, featured, (page - 1) * size, size);
    items.forEach(this::attach);
    return new Api.Page<>(items, store.articleCount(q, category, tag, admin, status, featured), page, size);
  }

  public Article detail(String slug) {
    Article a = store.published(slug);
    if (a == null) throw Problem.missing();
    attach(a);
    return a;
  }

  public Article edit(Long id) {
    Article a = store.article(id);
    if (a == null) throw Problem.missing();
    attach(a);
    return a;
  }

  private void attach(Article a) {
    a.tags = store.articleTags(a.id);
  }

  @Transactional
  public Article save(Long id, Api.ArticleInput input) {
    if (store.category(input.categoryId()) == null) throw new Problem(400, "CATEGORY", "请选择有效分类");
    for (Long tag : new HashSet<>(input.tagIds()))
      if (store.tag(tag) == null) throw new Problem(400, "TAG", "标签不存在");
    safeImage(input.cover());
    Article a = id == null ? new Article() : edit(id);
    if (id != null && a.version != input.version()) throw new Problem(
      409,
      "EDIT_CONFLICT",
      "这篇文章已被其他编辑更新，请重新载入"
    );
    a.title = input.title().trim();
    a.slug = input.slug();
    a.excerpt = input.excerpt().trim();
    a.body = input.body();
    a.cover = input.cover();
    a.categoryId = input.categoryId();
    a.status = input.status();
    a.featured = input.featured();
    var now = LocalDateTime.now(clock);
    a.updatedAt = now;
    if (a.publishedAt == null && "PUBLISHED".equals(a.status)) a.publishedAt = now;
    if (id == null) {
      a.createdAt = now;
      store.insertArticle(a);
    } else if (store.updateArticle(a) != 1) throw new Problem(
      409,
      "EDIT_CONFLICT",
      "文章版本发生变化，请重新载入"
    );
    store.clearArticleTags(a.id);
    for (Long tag : new HashSet<>(input.tagIds())) store.linkTag(a.id, tag);
    return edit(a.id);
  }

  @Transactional
  public void delete(Long id) {
    edit(id);
    store.clearArticleTags(id);
    store.deleteArticle(id);
  }

  @Transactional
  public Object taxonomy(String type, Long id, Api.Taxonomy input) {
    if ("categories".equals(type)) {
      Category c = new Category();
      c.id = id;
      c.name = input.name().trim();
      c.slug = input.slug();
      c.description = input.description() == null ? "" : input.description();
      if (id == null) store.insertCategory(c);
      else if (store.updateCategory(c) != 1) throw Problem.missing();
      return c;
    }
    if (input.name().length() > 30) throw new Problem(400, "VALIDATION", "标签名称最长30字符");
    Tag t = new Tag();
    t.id = id;
    t.name = input.name().trim();
    t.slug = input.slug();
    if (id == null) store.insertTag(t);
    else if (store.updateTag(t) != 1) throw Problem.missing();
    return t;
  }

  @Transactional
  public void deleteTaxonomy(String type, Long id) {
    if ("categories".equals(type)) {
      if (store.deleteCategory(id) != 1) throw Problem.missing();
    } else {
      store.unlinkTag(id);
      if (store.deleteTag(id) != 1) throw Problem.missing();
    }
  }

  public Api.SiteInput site() {
    try {
      return json.readValue(store.site(), Api.SiteInput.class);
    } catch (Exception e) {
      throw new IllegalStateException("Site settings unavailable", e);
    }
  }

  @Transactional
  public Api.SiteInput site(Api.SiteInput input) {
    if (!input.github().isBlank()) safeUrl(input.github());
    try {
      store.updateSite(json.writeValueAsString(input));
      return input;
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static void safeUrl(String value) {
    try {
      URI u = URI.create(value);
      if (
        !Set.of("http", "https").contains(u.getScheme()) || u.getHost() == null || u.getUserInfo() != null
      ) throw new IllegalArgumentException();
    } catch (Exception e) {
      throw new Problem(400, "URL", "链接必须为有效的 http 或 https 地址");
    }
  }

  public static void safeImage(String value) {
    if (value.isEmpty() || value.matches("/(art|uploads)/[a-zA-Z0-9._-]+")) return;
    safeUrl(value);
    if (!value.startsWith("https://")) throw new Problem(400, "IMAGE_URL", "外部图片必须使用 HTTPS");
  }

  public Map<String, Object> stats() {
    return Map.of(
      "published",
      store.articleCount("", "", "", false, "", null),
      "drafts",
      store.articleCount("", "", "", true, "DRAFT", null),
      "messages",
      store.messageCount(true),
      "pendingMessages",
      store
        .messages(true, 0, 10000)
        .stream()
        .filter(m -> "PENDING".equals(m.status))
        .count()
    );
  }
}
