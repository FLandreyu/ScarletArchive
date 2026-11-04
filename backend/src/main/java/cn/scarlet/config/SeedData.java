package cn.scarlet.config;

import cn.scarlet.api.Api;
import cn.scarlet.domain.Models.*;
import cn.scarlet.mapper.Store;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeedData implements ApplicationRunner {

  private final Store store;
  private final PasswordEncoder passwords;
  private final ObjectMapper json;
  private final Clock clock;
  private final boolean seed;
  private final String password;

  public SeedData(
    Store store,
    PasswordEncoder passwords,
    ObjectMapper json,
    Clock clock,
    @Value("${scarlet.seed}") boolean seed,
    @Value("${scarlet.admin-password}") String password
  ) {
    this.store = store;
    this.passwords = passwords;
    this.json = json;
    this.clock = clock;
    this.seed = seed;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    if (store.adminByName("admin") == null) {
      if (
        password.length() < 10 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
      ) throw new IllegalStateException(
        "ADMIN_PASSWORD must be at least 10 characters and at most 72 UTF-8 bytes"
      );
      Admin a = new Admin();
      a.username = "admin";
      a.passwordHash = passwords.encode(password);
      store.insertAdmin(a);
    }
    if (store.site() == null) store.insertSite(
      json.writeValueAsString(
        new Api.SiteInput(
          "红魔馆手记",
          "在永夜与晨曦之间，收藏一点微小的光。",
          "芙兰朵露yu",
          "你好，欢迎来到我的红魔馆。\n\n我是一名学习 Java 后端与前端开发的学生，也喜欢东方 Project、安静的音乐和有故事的设计。这里记录代码、阅读，以及日常生活里值得留住的小事。\n\n希望每一份笔记都能成为架子上的一本书，等到未来的某一天，再被认真翻开。",
          "https://github.com/FLandreyu",
          "",
          "Java / Vue / 东方 Project / 音乐 / 阅读",
          "2024|开始计算机科学的学习旅程\n2026|用真实项目串联前后端与工程实践\n现在|在这座小小的馆邸继续收藏与记录",
          "SeatFlow|校园自习室预约与运营平台|https://github.com/FLandreyu/SeatFlow"
        )
      )
    );
    if (!store.categories().isEmpty()) return;
    String[][] cats = {
      { "开发札记", "engineering", "把学习过程写成可以重访的路径" },
      { "幻想乡见闻", "gensokyo", "幻想世界与创作灵感" },
      { "生活随笔", "life", "在平凡日常里拾起微光" },
      { "书与思考", "reading", "那些读过之后留下回声的文字" },
    };
    for (var c : cats) {
      Category v = new Category();
      v.name = c[0];
      v.slug = c[1];
      v.description = c[2];
      store.insertCategory(v);
    }
    for (String[] t : new String[][] {
      { "Java", "java" },
      { "Vue", "vue" },
      { "东方 Project", "touhou" },
      { "设计", "design" },
      { "随笔", "notes" },
    }) {
      Tag v = new Tag();
      v.name = t[0];
      v.slug = t[1];
      store.insertTag(v);
    }
    if (!seed) return;
    var categories = store.categories();
    var tags = store.tags();
    String[][] posts = {
      {
        "在红魔馆，给文字留一盏灯",
        "a-light-for-every-story",
        "推开那扇深红色的门，在魔法图书馆里，为代码、故事与日常留一个位置。",
        "1",
        "/art/library.svg",
      },
      {
        "从一张座位开始：理解事务与并发",
        "transactions-and-a-seat",
        "一次简单预约背后，是资源、时间区间与一致性的细小约定。",
        "0",
        "/art/clock.svg",
      },
      {
        "用 Vue 构建一个可以慢慢阅读的地方",
        "building-a-reading-space",
        "界面可以富有氛围，也可以安静地退到内容身后。记录这次组件与排版的实践。",
        "0",
        "/art/library.svg",
      },
      {
        "帕秋莉的书签：关于知识的整理",
        "patchoulis-bookmarks",
        "分类不是为了把所有事物放进抽屉，而是让下一次相遇更容易发生。",
        "3",
        "/art/crystals.svg",
      },
      {
        "一杯红茶，和没有截止日期的下午",
        "an-afternoon-with-tea",
        "暂停一会儿，听听窗外的风。偶尔不追赶什么，也是一件值得认真做的事。",
        "2",
        "/art/roses.svg",
      },
      {
        "月光落在雾之湖上",
        "moonlight-on-the-lake",
        "夜色让边界变得柔软，也让一些白天没注意到的心情逐渐清晰。",
        "1",
        "/art/mansion-moonlight.png",
      },
      {
        "让动画懂得适时停下",
        "motion-that-knows-when-to-rest",
        "好的动效应当回应操作，而非争夺注意力。关于减少动态偏好与动画清理的笔记。",
        "0",
        "/art/clock.svg",
      },
    };
    for (int i = 0; i < posts.length; i++) {
      var p = posts[i];
      Article a = new Article();
      a.title = p[0];
      a.slug = p[1];
      a.excerpt = p[2];
      a.categoryId = categories.get(Integer.parseInt(p[3])).id;
      a.cover = p[4];
      a.status = "PUBLISHED";
      a.featured = i == 0 || i == 3;
      a.createdAt = LocalDateTime.now(clock).minusDays(i * 3 + 1);
      a.updatedAt = a.createdAt;
      a.publishedAt = a.createdAt;
      a.body = i == 1 ? engineering() : story(a.title, a.excerpt);
      store.insertArticle(a);
      store.linkTag(a.id, tags.get(i == 1 ? 0 : i == 2 ? 1 : i == 0 || i == 5 ? 2 : i == 6 ? 3 : 4).id);
    }
    Article draft = new Article();
    draft.title = "尚未寄出的信";
    draft.slug = "an-unpublished-letter";
    draft.excerpt = "这是一篇仅后台可见的示例草稿。";
    draft.body = "## 写作中的想法\n\n草稿不会出现在公开文章、归档、分类计数或站点地图里。";
    draft.cover = "/art/roses.svg";
    draft.categoryId = categories.get(2).id;
    draft.status = "DRAFT";
    draft.createdAt = LocalDateTime.now(clock);
    draft.updatedAt = draft.createdAt;
    store.insertArticle(draft);
    for (String[] m : new String[][] {
      { "旅人", "书页和月色都很温柔。愿这里留下许多值得重读的文字。" },
      { "庭院里的风", "路过庭院，留下一点好心情。明天也要记得好好休息。" },
    }) {
      Message v = new Message();
      v.nickname = m[0];
      v.content = m[1];
      v.status = "APPROVED";
      v.createdAt = LocalDateTime.now(clock).minusDays(1);
      store.insertMessage(v);
    }
    Friend f = new Friend();
    f.name = "东方 Project よもやまニュース";
    f.url = "https://touhou-project.news/";
    f.description = "东方Project相关资讯与二次创作指南";
    f.status = "APPROVED";
    f.createdAt = LocalDateTime.now(clock);
    store.insertFriend(f);
  }

  private String story(String title, String intro) {
    return (
      "# " +
      title +
      "\n\n" +
      intro +
      "\n\n## 推开那扇门\n\n夜晚的馆邸没有想象中遥远。走过庭院时，玫瑰在风里轻轻摇晃，钟楼的指针仍按自己的节奏前行。我们可以暂时放慢脚步，把没写完的想法留在这里。\n\n> 记录不是为了证明自己一直向前，而是为了记得：原来那时，我曾这样认真地看过世界。\n\n## 把片段收进书页\n\n一本笔记可以容纳很多不同的事：第一次理解的算法、一段终于跑通的代码、一本喜欢的书，或者只是一个安静的下午。它们不必在刚开始时就完美。\n\n- 先写下那个最具体的问题。\n- 保留尝试、推敲和失败的过程。\n- 在未来的某一天，回来补上一句新的理解。\n\n![月下的红魔馆](/art/mansion-moonlight.png)\n\n## 让阅读成为一件轻松的事\n\n这里的文章有目录、可复制的代码和清楚的标题。背景与装饰会在阅读时退后一点，让文字保有足够的留白。你也可以在右上角关闭动态效果，安静地读完这一页。\n\n| 留下什么 | 为什么记录 |\n| --- | --- |\n| 学习札记 | 让理解可以复现 |\n| 日常片段 | 给生活留下一点温度 |\n| 设计与灵感 | 让想法在未来重新发芽 |\n\n## 下一次再见\n\n如果愿意，可以在庭院留一句话。美铃会先替大家检查来信，再把它放到访客留言簿中。\n\n月光还在，灯也没有熄灭。愿你总能找到值得停留的一页。"
    );
  }

  private String engineering() {
    return "# 从一张座位开始：理解事务与并发\n\n当两个请求同时预约同一张座位，普通的先查询、再插入可能让它们同时成功。这篇笔记用一个具体场景梳理约束。\n\n## 先定义不变量\n\n同一个座位的有效预约不能重叠；同一个用户也不能同时占用两个座位。时间区间采用半开区间 `[start, end)`，相邻预约允许在同一整点交接。\n\n## 在事务内锁定资源\n\n稳定存在的用户行和座位行可以作为互斥资源。所有写入路径遵循相同锁定顺序。\n\n```java\n@Transactional\npublic Booking reserve(long userId, long seatId) {\n    users.lockForUpdate(userId);\n    seats.lockForUpdate(seatId);\n    verifyTimeConflict(userId, seatId);\n    return bookings.insert(userId, seatId);\n}\n```\n\n## 用 SQL 表达重叠\n\n```sql\nSELECT COUNT(*) FROM booking\nWHERE seat_id = #{seatId}\n  AND start_at < #{end}\n  AND end_at > #{start}\n  AND status IN ('RESERVED', 'CHECKED_IN');\n```\n\n这里的参数需要预编译绑定。前端展示空闲只是观察，提交事务的判断才是最终结果。\n\n## 重复请求与幂等\n\n用户双击或网络重试并不代表希望建立两条预约。可以使用用户级请求键，并核对相同键的请求载荷。如果载荷不同，应明确报冲突，而非静默覆盖。\n\n## 让测试回答问题\n\n并发测试需要同时放行多个真实事务，再检查成功数、冲突数和数据库记录数。测试通过能支持正确性的结论，但不能直接推导出线上吞吐量。\n\n> 先解释清楚系统承诺什么，再讨论它能跑多快。\n\n## 延伸阅读\n\n完整示例见 [SeatFlow 项目](https://github.com/FLandreyu/SeatFlow)。";
  }
}
