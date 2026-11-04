package cn.scarlet.service;

import cn.scarlet.api.*;
import cn.scarlet.domain.Models.*;
import cn.scarlet.mapper.Store;
import java.time.*;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CommunityService {

  private final Store store;
  private final Clock clock;

  public CommunityService(Store store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public Map<String, Object> leave(Api.GuestInput input) {
    if (input.website() != null && !input.website().isBlank()) throw new Problem(400, "SPAM", "留言未被接受");
    if (input.content().matches("(?is).*(https?://.*){3,}.*")) throw new Problem(
      400,
      "SPAM",
      "留言中的链接过多"
    );
    Message m = new Message();
    m.nickname = input.nickname().trim();
    m.content = input.content().trim();
    m.status = "PENDING";
    m.createdAt = LocalDateTime.now(clock);
    store.insertMessage(m);
    return Map.of("id", m.id, "status", m.status, "message", "留言已交给美铃，审核后会出现在庭院");
  }

  public Friend friend(Long id, Api.FriendInput input, boolean approved) {
    BlogService.safeUrl(input.url());
    Friend f = new Friend();
    f.id = id;
    f.name = input.name().trim();
    f.url = input.url();
    f.description = input.description().trim();
    f.status = approved ? "APPROVED" : "PENDING";
    f.createdAt = LocalDateTime.now(clock);
    if (id == null) store.insertFriend(f);
    else if (store.updateFriend(f) != 1) throw Problem.missing();
    return f;
  }
}
