package cn.scarlet.api;

import cn.scarlet.mapper.Store;
import cn.scarlet.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Validated
public class PublicController {

  private final Store store;
  private final BlogService blog;
  private final CommunityService community;
  private final RateLimiter limits;

  public PublicController(Store store, BlogService blog, CommunityService community, RateLimiter limits) {
    this.store = store;
    this.blog = blog;
    this.community = community;
    this.limits = limits;
  }

  @GetMapping("/health")
  public Object health() {
    return Api.Result.ok(java.util.Map.of("application", "ScarletArchive", "status", "UP"));
  }

  @GetMapping("/site")
  public Object site() {
    return Api.Result.ok(blog.site());
  }

  @GetMapping("/articles")
  public Object articles(
    @RequestParam(defaultValue = "") String q,
    @RequestParam(defaultValue = "") String category,
    @RequestParam(defaultValue = "") String tag,
    @RequestParam(defaultValue = "newest") String sort,
    @RequestParam(required = false) Boolean featured,
    @RequestParam(defaultValue = "1") @Min(1) @Max(100000) int page,
    @RequestParam(defaultValue = "9") @Min(1) @Max(50) int size
  ) {
    return Api.Result.ok(blog.list(q, category, tag, sort, false, "", featured, page, size));
  }

  @GetMapping("/articles/{slug}")
  public Object article(@PathVariable String slug) {
    return Api.Result.ok(blog.detail(slug));
  }

  @GetMapping("/categories")
  public Object categories() {
    return Api.Result.ok(store.categories());
  }

  @GetMapping("/tags")
  public Object tags() {
    return Api.Result.ok(store.tags());
  }

  @GetMapping("/archive")
  public Object archive() {
    return Api.Result.ok(blog.list("", "", "", "newest", false, "", null, 1, 10000).items());
  }

  @GetMapping("/guestbook")
  public Object messages(
    @RequestParam(defaultValue = "1") @Min(1) @Max(100000) int page,
    @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
  ) {
    return Api.Result.ok(
      new Api.Page<>(store.messages(false, (page - 1) * size, size), store.messageCount(false), page, size)
    );
  }

  @PostMapping("/guestbook")
  public Object leave(@Valid @RequestBody Api.GuestInput input, HttpServletRequest request) {
    limits.check("guest:" + request.getRemoteAddr(), 3, 300000);
    return Api.Result.ok(community.leave(input));
  }

  @GetMapping("/friends")
  public Object friends() {
    return Api.Result.ok(store.friends(false));
  }

  @PostMapping("/friends/apply")
  public Object apply(@Valid @RequestBody Api.FriendInput input, HttpServletRequest request) {
    limits.check("friend:" + request.getRemoteAddr(), 3, 600000);
    return Api.Result.ok(community.friend(null, input, false));
  }
}
