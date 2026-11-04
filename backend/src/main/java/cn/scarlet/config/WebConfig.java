package cn.scarlet.config;

import cn.scarlet.api.Problem;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final String uploads;

  public WebConfig(@Value("${scarlet.uploads}") String uploads) {
    this.uploads = uploads;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry r) {
    r.addResourceHandler("/uploads/**")
      .addResourceLocations(Path.of(uploads).toAbsolutePath().toUri().toString())
      .setCachePeriod(31536000);
  }

  @Controller
  static class Spa {

    @GetMapping({
      "/articles",
      "/articles/{slug}",
      "/archive",
      "/categories",
      "/tags",
      "/about",
      "/guestbook",
      "/friends",
      "/secret",
      "/admin",
      "/admin/{section}",
      "/lost",
    })
    String app() {
      return "forward:/index.html";
    }

    @GetMapping("/{single:[a-zA-Z0-9_-]+}")
    String unknown(@PathVariable String single) {
      if (java.util.Set.of("api", "art", "uploads", "assets").contains(single)) throw Problem.missing();
      return "forward:/index.html";
    }
  }
}
