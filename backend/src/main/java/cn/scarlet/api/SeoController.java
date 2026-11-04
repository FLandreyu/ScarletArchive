package cn.scarlet.api;

import cn.scarlet.service.BlogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

@RestController
public class SeoController {

  private final BlogService blog;
  private final String url;

  public SeoController(BlogService blog, @Value("${scarlet.public-url}") String url) {
    this.blog = blog;
    this.url = url.replaceAll("/+$", "");
  }

  private String escape(String s) {
    return HtmlUtils.htmlEscape(s);
  }

  @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
  public String sitemap() {
    StringBuilder s = new StringBuilder(
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?><urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
    );
    for (String path : new String[] {
      "",
      "/articles",
      "/archive",
      "/categories",
      "/tags",
      "/about",
      "/guestbook",
      "/friends",
    })
      s.append("<url><loc>")
        .append(escape(url + path))
        .append("</loc></url>");
    for (var a : blog.list("", "", "", "newest", false, "", null, 1, 10000).items())
      s.append("<url><loc>")
        .append(escape(url + "/articles/" + a.slug))
        .append("</loc><lastmod>")
        .append(a.updatedAt.toLocalDate())
        .append("</lastmod></url>");
    return s.append("</urlset>").toString();
  }

  @GetMapping(value = "/robots.txt", produces = "text/plain")
  public String robots() {
    return "User-agent: *\nAllow: /\nDisallow: /admin\nSitemap: " + url + "/sitemap.xml\n";
  }
}
