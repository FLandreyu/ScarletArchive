package cn.scarlet.mapper;

import cn.scarlet.domain.Models.*;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface Store {
  @Select("SELECT * FROM admin_user WHERE username=#{username}")
  Admin adminByName(String username);

  @Select("SELECT * FROM admin_user WHERE id=#{id}")
  Admin admin(Long id);

  @Insert(
    "INSERT INTO admin_user(username,password_hash,token_version) VALUES(#{username},#{passwordHash},0)"
  )
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertAdmin(Admin a);

  @Update("UPDATE admin_user SET token_version=token_version+1 WHERE id=#{id}")
  int revoke(Long id);

  List<Article> articles(
    @Param("q") String q,
    @Param("category") String category,
    @Param("tag") String tag,
    @Param("sort") String sort,
    @Param("admin") boolean admin,
    @Param("status") String status,
    @Param("featured") Boolean featured,
    @Param("offset") int offset,
    @Param("size") int size
  );
  long articleCount(
    @Param("q") String q,
    @Param("category") String category,
    @Param("tag") String tag,
    @Param("admin") boolean admin,
    @Param("status") String status,
    @Param("featured") Boolean featured
  );

  @Select(
    "SELECT a.*,c.name category_name,c.slug category_slug FROM article a JOIN category c ON a.category_id=c.id WHERE a.id=#{id}"
  )
  Article article(Long id);

  @Select(
    "SELECT a.*,c.name category_name,c.slug category_slug FROM article a JOIN category c ON a.category_id=c.id WHERE a.slug=#{slug} AND a.status='PUBLISHED'"
  )
  Article published(String slug);

  @Insert(
    "INSERT INTO article(title,slug,excerpt,body,cover,category_id,status,featured,version,published_at,created_at,updated_at) VALUES(#{title},#{slug},#{excerpt},#{body},#{cover},#{categoryId},#{status},#{featured},0,#{publishedAt},#{createdAt},#{updatedAt})"
  )
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertArticle(Article a);

  @Update(
    "UPDATE article SET title=#{title},slug=#{slug},excerpt=#{excerpt},body=#{body},cover=#{cover},category_id=#{categoryId},status=#{status},featured=#{featured},published_at=#{publishedAt},updated_at=#{updatedAt},version=version+1 WHERE id=#{id} AND version=#{version}"
  )
  int updateArticle(Article a);

  @Delete("DELETE FROM article WHERE id=#{id}")
  int deleteArticle(Long id);

  @Delete("DELETE FROM article_tag WHERE article_id=#{id}")
  int clearArticleTags(Long id);

  @Insert("INSERT INTO article_tag(article_id,tag_id) VALUES(#{articleId},#{tagId})")
  int linkTag(@Param("articleId") Long a, @Param("tagId") Long t);

  @Select(
    "SELECT t.* FROM tag t JOIN article_tag atg ON t.id=atg.tag_id WHERE atg.article_id=#{id} ORDER BY t.id"
  )
  List<Tag> articleTags(Long id);

  @Select(
    "SELECT c.*,COUNT(a.id) article_count FROM category c LEFT JOIN article a ON a.category_id=c.id AND a.status='PUBLISHED' GROUP BY c.id ORDER BY c.id"
  )
  List<Category> categories();

  @Select("SELECT * FROM category WHERE id=#{id}")
  Category category(Long id);

  @Insert("INSERT INTO category(name,slug,description) VALUES(#{name},#{slug},#{description})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertCategory(Category c);

  @Update("UPDATE category SET name=#{name},slug=#{slug},description=#{description} WHERE id=#{id}")
  int updateCategory(Category c);

  @Delete("DELETE FROM category WHERE id=#{id}")
  int deleteCategory(Long id);

  @Select(
    "SELECT t.*,COUNT(a.id) article_count FROM tag t LEFT JOIN article_tag atg ON atg.tag_id=t.id LEFT JOIN article a ON a.id=atg.article_id AND a.status='PUBLISHED' GROUP BY t.id ORDER BY t.id"
  )
  List<Tag> tags();

  @Select("SELECT * FROM tag WHERE id=#{id}")
  Tag tag(Long id);

  @Insert("INSERT INTO tag(name,slug) VALUES(#{name},#{slug})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertTag(Tag t);

  @Update("UPDATE tag SET name=#{name},slug=#{slug} WHERE id=#{id}")
  int updateTag(Tag t);

  @Delete("DELETE FROM article_tag WHERE tag_id=#{id}")
  int unlinkTag(Long id);

  @Delete("DELETE FROM tag WHERE id=#{id}")
  int deleteTag(Long id);

  @Insert(
    "INSERT INTO guest_message(nickname,content,status,created_at) VALUES(#{nickname},#{content},#{status},#{createdAt})"
  )
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertMessage(Message m);

  @Select(
    "SELECT * FROM guest_message WHERE (#{admin}=TRUE OR status='APPROVED') ORDER BY created_at DESC,id DESC LIMIT #{size} OFFSET #{offset}"
  )
  List<Message> messages(@Param("admin") boolean admin, @Param("offset") int offset, @Param("size") int size);

  @Select("SELECT COUNT(*) FROM guest_message WHERE (#{admin}=TRUE OR status='APPROVED')")
  long messageCount(boolean admin);

  @Update("UPDATE guest_message SET status=#{status} WHERE id=#{id}")
  int messageStatus(@Param("id") Long id, @Param("status") String status);

  @Delete("DELETE FROM guest_message WHERE id=#{id}")
  int deleteMessage(Long id);

  @Insert(
    "INSERT INTO friend_link(name,url,description,status,created_at) VALUES(#{name},#{url},#{description},#{status},#{createdAt})"
  )
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertFriend(Friend f);

  @Select("SELECT * FROM friend_link WHERE (#{admin}=TRUE OR status='APPROVED') ORDER BY id")
  List<Friend> friends(boolean admin);

  @Update("UPDATE friend_link SET name=#{name},url=#{url},description=#{description} WHERE id=#{id}")
  int updateFriend(Friend f);

  @Update("UPDATE friend_link SET status=#{status} WHERE id=#{id}")
  int friendStatus(@Param("id") Long id, @Param("status") String status);

  @Delete("DELETE FROM friend_link WHERE id=#{id}")
  int deleteFriend(Long id);

  @Select("SELECT content FROM site_config WHERE id=1")
  String site();

  @Insert("INSERT INTO site_config(id,content) VALUES(1,#{content})")
  int insertSite(String content);

  @Update("UPDATE site_config SET content=#{content} WHERE id=1")
  int updateSite(String content);
}
