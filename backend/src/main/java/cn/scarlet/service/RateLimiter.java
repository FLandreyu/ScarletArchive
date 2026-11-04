package cn.scarlet.service;

import cn.scarlet.api.Problem;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

  private record Bucket(long start, int count) {}

  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  public void check(String key, int limit, long windowMs) {
    long now = System.currentTimeMillis();
    if (buckets.size() > 10000) buckets.entrySet().removeIf(e -> now - e.getValue().start > 600000);
    if (buckets.size() > 20000) throw new Problem(429, "RATE_LIMIT", "请稍后再试");
    buckets.compute(key, (k, b) -> {
      if (b == null || now - b.start >= windowMs) return new Bucket(now, 1);
      if (b.count >= limit) throw new Problem(429, "RATE_LIMIT", "来访有些频繁，请稍后再试");
      return new Bucket(b.start, b.count + 1);
    });
  }

  public void clear() {
    buckets.clear();
  }
}
