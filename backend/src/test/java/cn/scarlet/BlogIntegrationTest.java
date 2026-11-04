package cn.scarlet;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.scarlet.api.*;
import cn.scarlet.domain.Models.*;
import cn.scarlet.mapper.Store;
import cn.scarlet.service.*;
import com.fasterxml.jackson.databind.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
  properties = {
    "spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:scarlet-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${TEST_DB_USERNAME:sa}",
    "spring.datasource.password=${TEST_DB_PASSWORD:}",
    "scarlet.uploads=./target/test-uploads",
    "scarlet.jwt-secret=YL8vNSlPrPMAeEiZwbboNAmSjoV17Vb3AWWn3tx03xQ=",
    "debug=false",
  }
)
@AutoConfigureMockMvc
class BlogIntegrationTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  Store store;

  @Autowired
  BlogService blog;

  @Autowired
  CommunityService community;

  @Autowired
  RateLimiter limits;

  @Autowired
  ObjectMapper json;

  @Autowired
  AuthService auth;

  @Autowired
  JwtDecoder decoder;

  private String prefix;

  @BeforeEach
  void before() {
    limits.clear();
    prefix = "test-" + UUID.randomUUID().toString().substring(0, 8);
  }

  Api.ArticleInput input(String status, int version) {
    return new Api.ArticleInput(
      "测试文章",
      prefix,
      "一份真实测试摘要",
      "## 测试章节\n\n测试正文。",
      "/art/library.svg",
      store.categories().get(0).id,
      List.of(store.tags().get(0).id),
      status,
      false,
      version
    );
  }

  @Test
  void healthIsPublic() throws Exception {
    mvc
      .perform(get("/api/health"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.status").value("UP"));
  }

  @Test
  void unknownFrontRouteLoadsThemedPage() throws Exception {
    mvc
      .perform(get("/not-a-room").accept("text/html"))
      .andExpect(status().isOk())
      .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  void adminRequiresAuthentication() throws Exception {
    mvc.perform(get("/api/admin/articles")).andExpect(status().isUnauthorized());
  }

  @Test
  void anonymousCannotUpload() throws Exception {
    mvc
      .perform(
        multipart("/api/admin/uploads").file(
          new MockMultipartFile("file", "x.png", "image/png", new byte[] { 1 })
        )
      )
      .andExpect(status().isUnauthorized());
  }

  @Test
  void draftHiddenFromDetail() throws Exception {
    var a = blog.save(null, input("DRAFT", 0));
    mvc.perform(get("/api/articles/" + a.slug)).andExpect(status().isNotFound());
  }

  @Test
  void draftHiddenFromSearch() {
    blog.save(null, input("DRAFT", 0));
    assertEquals(0, blog.list(prefix, "", "", "newest", false, "", null, 1, 10).total());
  }

  @Test
  void draftHiddenFromArchive() throws Exception {
    var a = blog.save(null, input("DRAFT", 0));
    var out = mvc.perform(get("/api/archive")).andReturn().getResponse().getContentAsString();
    assertFalse(out.contains(a.slug));
  }

  @Test
  void draftHiddenFromSitemap() throws Exception {
    var a = blog.save(null, input("DRAFT", 0));
    assertFalse(
      mvc.perform(get("/sitemap.xml")).andReturn().getResponse().getContentAsString().contains(a.slug)
    );
  }

  @Test
  void publishMakesArticleReadable() throws Exception {
    var a = blog.save(null, input("PUBLISHED", 0));
    mvc
      .perform(get("/api/articles/" + a.slug))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.body").value(a.body));
  }

  @Test
  void unpublishRemovesPublicAccess() {
    var a = blog.save(null, input("PUBLISHED", 0));
    blog.save(a.id, input("DRAFT", a.version));
    assertThrows(Problem.class, () -> blog.detail(a.slug));
  }

  @Test
  void editIncrementsVersion() {
    var a = blog.save(null, input("DRAFT", 0));
    var b = blog.save(a.id, input("PUBLISHED", a.version));
    assertEquals(1, b.version);
    assertNotNull(b.publishedAt);
  }

  @Test
  void staleEditorRejected() {
    var a = blog.save(null, input("DRAFT", 0));
    blog.save(a.id, input("DRAFT", a.version));
    var e = assertThrows(Problem.class, () -> blog.save(a.id, input("PUBLISHED", a.version)));
    assertEquals("EDIT_CONFLICT", e.code);
  }

  @Test
  void parallelEditsOnlyOneWins() throws Exception {
    var a = blog.save(null, input("DRAFT", 0));
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Boolean>> results = new ArrayList<>();
    try {
      for (int i = 0; i < 2; i++) results.add(
        pool.submit(() -> {
          start.await();
          try {
            blog.save(a.id, input("PUBLISHED", a.version));
            return true;
          } catch (Problem e) {
            assertEquals("EDIT_CONFLICT", e.code);
            return false;
          }
        })
      );
      start.countDown();
      int wins = 0;
      for (var f : results) if (f.get(10, TimeUnit.SECONDS)) wins++;
      assertEquals(1, wins);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void deleteRemovesTagsAndArticle() {
    var a = blog.save(null, input("PUBLISHED", 0));
    assertFalse(store.articleTags(a.id).isEmpty());
    blog.delete(a.id);
    assertNull(store.article(a.id));
    assertTrue(store.articleTags(a.id).isEmpty());
  }

  @Test
  void invalidCategoryRejected() {
    var v = input("DRAFT", 0);
    var bad = new Api.ArticleInput(
      v.title(),
      v.slug(),
      v.excerpt(),
      v.body(),
      v.cover(),
      999999L,
      v.tagIds(),
      v.status(),
      false,
      0
    );
    assertThrows(Problem.class, () -> blog.save(null, bad));
  }

  @Test
  void invalidTagRollsBackArticle() {
    long before = store.articleCount("", "", "", true, "", null);
    var v = input("DRAFT", 0);
    var bad = new Api.ArticleInput(
      v.title(),
      v.slug(),
      v.excerpt(),
      v.body(),
      v.cover(),
      v.categoryId(),
      List.of(999999L),
      v.status(),
      false,
      0
    );
    assertThrows(Problem.class, () -> blog.save(null, bad));
    assertEquals(before, store.articleCount("", "", "", true, "", null));
  }

  @Test
  void scriptImageUrlRejected() {
    assertThrows(Problem.class, () -> BlogService.safeImage("javascript:alert(1)"));
  }

  @Test
  void invalidUploadPathRejected() {
    assertThrows(Problem.class, () -> BlogService.safeImage("/uploads/../../secret"));
  }

  @Test
  void externalHttpsImageAccepted() {
    assertDoesNotThrow(() -> BlogService.safeImage("https://example.com/image.png"));
  }

  @Test
  void linkCredentialsRejected() {
    assertThrows(Problem.class, () -> BlogService.safeUrl("https://username:password@example.com"));
  }

  @Test
  void sqlInjectionCannotBroadenResults() {
    assertEquals(0, blog.list("' OR 1=1 --", "", "", "newest", false, "", null, 1, 10).total());
  }

  @Test
  void invalidSortRejected() throws Exception {
    mvc.perform(get("/api/articles?sort=DROP")).andExpect(status().isBadRequest());
  }

  @Test
  void invalidPageRejected() throws Exception {
    mvc.perform(get("/api/articles?page=0")).andExpect(status().isBadRequest());
  }

  @Test
  void pageSizeBounded() throws Exception {
    mvc.perform(get("/api/articles?size=500")).andExpect(status().isBadRequest());
  }

  @Test
  void taxonomyCreationWorks() {
    Category c = (Category) blog.taxonomy("categories", null, new Api.Taxonomy(prefix, prefix, "测试分类"));
    assertNotNull(c.id);
    blog.deleteTaxonomy("categories", c.id);
    assertNull(store.category(c.id));
  }

  @Test
  void usedCategoryCannotBeDeleted() {
    var a = blog.save(null, input("DRAFT", 0));
    assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
      blog.deleteTaxonomy("categories", a.categoryId)
    );
  }

  @Test
  void newMessageIsPending() {
    var result = community.leave(new Api.GuestInput(prefix, "一封等待审核的来信", ""));
    assertEquals("PENDING", result.get("status"));
    assertTrue(
      store
        .messages(false, 0, 10000)
        .stream()
        .noneMatch(m -> m.id.equals(result.get("id")))
    );
  }

  @Test
  void approveMessageMakesItPublic() {
    var result = community.leave(new Api.GuestInput(prefix, "审核后才公开的来信", ""));
    Long id = ((Number) result.get("id")).longValue();
    store.messageStatus(id, "APPROVED");
    assertTrue(
      store
        .messages(false, 0, 10000)
        .stream()
        .anyMatch(m -> m.id.equals(id))
    );
  }

  @Test
  void honeypotRejected() {
    assertThrows(Problem.class, () ->
      community.leave(new Api.GuestInput("bot", "一条广告消息", "spam.example"))
    );
  }

  @Test
  void messageValidationWorks() throws Exception {
    mvc
      .perform(
        post("/api/guestbook")
          .contentType("application/json")
          .content("{\"nickname\":\"\",\"content\":\"x\"}")
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void guestRateLimitWorks() throws Exception {
    for (int i = 0; i < 3; i++) mvc
      .perform(
        post("/api/guestbook")
          .contentType("application/json")
          .content(json.writeValueAsString(new Api.GuestInput(prefix, "一封测试来信", "")))
      )
      .andExpect(status().isOk());
    mvc
      .perform(
        post("/api/guestbook")
          .contentType("application/json")
          .content(json.writeValueAsString(new Api.GuestInput(prefix, "第四封被限流", "")))
      )
      .andExpect(status().isTooManyRequests());
  }

  @Test
  void friendApplicationIsNotPublic() {
    var f = community.friend(null, new Api.FriendInput(prefix, "https://example.com", "等待审核"), false);
    assertEquals("PENDING", f.status);
    assertTrue(
      store
        .friends(false)
        .stream()
        .noneMatch(x -> x.id.equals(f.id))
    );
  }

  @Test
  void friendJavascriptRejected() {
    assertThrows(Problem.class, () ->
      community.friend(null, new Api.FriendInput("bad", "javascript:alert(1)", "bad"), false)
    );
  }

  @Test
  void wrongPasswordRejected() throws Exception {
    mvc
      .perform(
        post("/api/auth/login")
          .contentType("application/json")
          .content("{\"username\":\"admin\",\"password\":\"wrong\"}")
      )
      .andExpect(status().isUnauthorized());
  }

  @Test
  void loginRateLimitWorks() throws Exception {
    for (int i = 0; i < 5; i++) mvc
      .perform(
        post("/api/auth/login")
          .contentType("application/json")
          .content("{\"username\":\"admin\",\"password\":\"wrong\"}")
      )
      .andExpect(status().isUnauthorized());
    mvc
      .perform(
        post("/api/auth/login")
          .contentType("application/json")
          .content("{\"username\":\"admin\",\"password\":\"wrong\"}")
      )
      .andExpect(status().isTooManyRequests());
  }

  @Test
  void actualJwtAndLogoutRevocationWork() {
    String token = (String) auth.login(new Api.Login("admin", "Scarlet123!")).get("token");
    Jwt parsed = decoder.decode(token);
    assertNotNull(parsed);
    auth.logout(Long.valueOf(parsed.getSubject()));
    assertThrows(JwtValidationException.class, () -> decoder.decode(token));
  }

  @Test
  void passwordHashNeverInMe() throws Exception {
    Long id = store.adminByName("admin").id;
    mvc
      .perform(get("/api/auth/me").with(jwt().jwt(j -> j.subject(id.toString()))))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
  }

  @Test
  void fakePngIsRejected() throws Exception {
    mvc
      .perform(
        multipart("/api/admin/uploads")
          .file(new MockMultipartFile("file", "fake.png", "image/png", "<script>bad</script>".getBytes()))
          .with(jwt())
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void svgIsRejected() throws Exception {
    mvc
      .perform(
        multipart("/api/admin/uploads")
          .file(new MockMultipartFile("file", "bad.svg", "image/svg+xml", "<svg/>".getBytes()))
          .with(jwt())
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void realPngReencoded() throws Exception {
    BufferedImage image = new BufferedImage(12, 8, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);
    mvc
      .perform(
        multipart("/api/admin/uploads")
          .file(new MockMultipartFile("file", "../../unsafe.png", "image/png", bytes.toByteArray()))
          .with(jwt())
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.data.width").value(12))
      .andExpect(
        jsonPath("$.data.url").value(org.hamcrest.Matchers.matchesPattern("/uploads/[a-f0-9-]+\\.jpg"))
      );
  }

  @Test
  void publishedSitemapContainsArticle() throws Exception {
    var a = blog.save(null, input("PUBLISHED", 0));
    assertTrue(
      mvc.perform(get("/sitemap.xml")).andReturn().getResponse().getContentAsString().contains(a.slug)
    );
  }

  @Test
  void siteSettingsPersist() {
    var old = blog.site();
    var value = new Api.SiteInput(
      old.title(),
      prefix,
      old.owner(),
      old.about(),
      old.github(),
      old.email(),
      old.interests(),
      old.timeline(),
      old.projects()
    );
    blog.site(value);
    assertEquals(prefix, blog.site().subtitle());
    blog.site(old);
  }
}
