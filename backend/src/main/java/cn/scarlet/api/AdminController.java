package cn.scarlet.api;

import cn.scarlet.mapper.Store;
import cn.scarlet.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Validated
public class AdminController {

  private final Store store;
  private final AuthService auth;
  private final BlogService blog;
  private final CommunityService community;
  private final AssetService assets;
  private final RateLimiter limits;

  public AdminController(
    Store store,
    AuthService auth,
    BlogService blog,
    CommunityService community,
    AssetService assets,
    RateLimiter limits
  ) {
    this.store = store;
    this.auth = auth;
    this.blog = blog;
    this.community = community;
    this.assets = assets;
    this.limits = limits;
  }

  @PostMapping("/auth/login")
  public Object login(@Valid @RequestBody Api.Login input, HttpServletRequest request) {
    limits.check("login:" + request.getRemoteAddr(), 5, 60000);
    return Api.Result.ok(auth.login(input));
  }

  @GetMapping("/auth/me")
  public Object me(@AuthenticationPrincipal Jwt jwt) {
    return Api.Result.ok(auth.me(Long.valueOf(jwt.getSubject())));
  }

  @PostMapping("/auth/logout")
  public Object logout(@AuthenticationPrincipal Jwt jwt) {
    auth.logout(Long.valueOf(jwt.getSubject()));
    return Api.Result.ok(null);
  }

  @GetMapping("/admin/stats")
  public Object stats() {
    return Api.Result.ok(blog.stats());
  }

  @GetMapping("/admin/articles")
  public Object list(
    @RequestParam(defaultValue = "") String q,
    @RequestParam(defaultValue = "") String status,
    @RequestParam(defaultValue = "1") @Min(1) @Max(100000) int page,
    @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
  ) {
    return Api.Result.ok(blog.list(q, "", "", "newest", true, status, null, page, size));
  }

  @GetMapping("/admin/articles/{id}")
  public Object article(@PathVariable Long id) {
    return Api.Result.ok(blog.edit(id));
  }

  @PostMapping("/admin/articles")
  public Object create(@Valid @RequestBody Api.ArticleInput input) {
    return Api.Result.ok(blog.save(null, input));
  }

  @PutMapping("/admin/articles/{id}")
  public Object save(@PathVariable Long id, @Valid @RequestBody Api.ArticleInput input) {
    return Api.Result.ok(blog.save(id, input));
  }

  @DeleteMapping("/admin/articles/{id}")
  public Object delete(@PathVariable Long id) {
    blog.delete(id);
    return Api.Result.ok(null);
  }

  @PostMapping("/admin/{type:categories|tags}")
  public Object taxonomy(@PathVariable String type, @Valid @RequestBody Api.Taxonomy input) {
    return Api.Result.ok(blog.taxonomy(type, null, input));
  }

  @PutMapping("/admin/{type:categories|tags}/{id}")
  public Object taxonomyEdit(
    @PathVariable String type,
    @PathVariable Long id,
    @Valid @RequestBody Api.Taxonomy input
  ) {
    return Api.Result.ok(blog.taxonomy(type, id, input));
  }

  @DeleteMapping("/admin/{type:categories|tags}/{id}")
  public Object taxonomyDelete(@PathVariable String type, @PathVariable Long id) {
    blog.deleteTaxonomy(type, id);
    return Api.Result.ok(null);
  }

  @PostMapping("/admin/uploads")
  public Object upload(@RequestParam("file") MultipartFile file) throws Exception {
    return Api.Result.ok(assets.upload(file));
  }

  @GetMapping("/admin/guestbook")
  public Object guestbook(
    @RequestParam(defaultValue = "1") @Min(1) @Max(100000) int page,
    @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
  ) {
    return Api.Result.ok(
      new Api.Page<>(store.messages(true, (page - 1) * size, size), store.messageCount(true), page, size)
    );
  }

  @PatchMapping("/admin/guestbook/{id}")
  public Object approve(@PathVariable Long id, @Valid @RequestBody Api.Moderation input) {
    if (store.messageStatus(id, input.status()) != 1) throw Problem.missing();
    return Api.Result.ok(null);
  }

  @DeleteMapping("/admin/guestbook/{id}")
  public Object deleteMessage(@PathVariable Long id) {
    if (store.deleteMessage(id) != 1) throw Problem.missing();
    return Api.Result.ok(null);
  }

  @GetMapping("/admin/friends")
  public Object friends() {
    return Api.Result.ok(store.friends(true));
  }

  @PostMapping("/admin/friends")
  public Object friend(@Valid @RequestBody Api.FriendInput input) {
    return Api.Result.ok(community.friend(null, input, true));
  }

  @PutMapping("/admin/friends/{id}")
  public Object updateFriend(@PathVariable Long id, @Valid @RequestBody Api.FriendInput input) {
    return Api.Result.ok(community.friend(id, input, true));
  }

  @PatchMapping("/admin/friends/{id}")
  public Object approveFriend(@PathVariable Long id, @Valid @RequestBody Api.Moderation input) {
    if (store.friendStatus(id, input.status()) != 1) throw Problem.missing();
    return Api.Result.ok(null);
  }

  @DeleteMapping("/admin/friends/{id}")
  public Object deleteFriend(@PathVariable Long id) {
    if (store.deleteFriend(id) != 1) throw Problem.missing();
    return Api.Result.ok(null);
  }

  @PutMapping("/admin/site")
  public Object site(@Valid @RequestBody Api.SiteInput input) {
    return Api.Result.ok(blog.site(input));
  }
}
