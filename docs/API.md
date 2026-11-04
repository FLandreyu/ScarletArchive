# REST API

前缀 `/api`，响应 `{code,message,data}`；失败使用400/401/404/409/413/429/500相应HTTP状态。日期时间按Asia/Shanghai解释。分页 `{items,total,page,size}`，page从1起；公开列表size最多50。

## 公开接口

| 方法 | 路径 | 参数 / 返回 |
| --- | --- | --- |
| GET | `/health` | 应用存活状态，不是完整数据库就绪探针 |
| GET | `/site` | 站点与馆主档案配置 |
| GET | `/articles` | `q,category,tag,sort=newest/oldest,featured,page,size`；仅已发布 |
| GET | `/articles/{slug}` | 公开详情，包含Markdown原文和标签；草稿404 |
| GET | `/categories`、`/tags` | 名称、slug、公开文章数 |
| GET | `/archive` | 全部公开文章，当前最多10000条 |
| GET | `/guestbook` | 审核通过的来信分页 |
| POST | `/guestbook` | `{nickname,content,website:""}`；默认PENDING |
| GET | `/friends` | 审核通过的友链 |
| POST | `/friends/apply` | `{name,url,description}`；默认PENDING |

留言昵称1-30字符，内容3-800字符；website是反机器人蜜罐，应为空；单IP5分钟最多3次留言。友链限HTTP/HTTPS地址，不允许嵌入用户名密码，10分钟最多3次申请。

站点地图位于 `/sitemap.xml`，机器人配置位于 `/robots.txt`，由后端依据已发布数据生成，不位于/api下。

## 认证

`POST /auth/login`：`{username,password}` → `{token,username,expiresIn:7200}`。登录每IP每分钟最多5次。后台接口携带 `Authorization: Bearer <token>`。凭证保存在当前标签页sessionStorage，未使用Cookie认证，因此采用无状态配置并关闭CSRF；生产同源代理不开放通配CORS。

`GET /auth/me`：只返回id和username，不返回密码哈希。

`POST /auth/logout`：提升管理员token_version，旧的所有登录令牌均在服务端失效。没有公众注册与角色提升接口。

## 后台接口

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | `/admin/stats` | 公开文章、草稿、总留言、待审核留言数量 |
| GET | `/admin/articles` | 所有文章分页；可选q/status |
| GET | `/admin/articles/{id}` | 编辑用完整内容和version |
| POST | `/admin/articles` | 创建文章 |
| PUT | `/admin/articles/{id}` | 按version条件更新 |
| DELETE | `/admin/articles/{id}` | 同事务删除文章与标签关联 |
| POST/PUT/DELETE | `/admin/categories`、`/admin/categories/{id}` | 分类管理；有引用的分类不能删除 |
| POST/PUT/DELETE | `/admin/tags`、`/admin/tags/{id}` | 标签管理；删除会移除标签关联 |
| POST | `/admin/uploads` | multipart字段file；PNG/JPEG，5MB内，1600万像素内 |
| GET | `/admin/guestbook` | 所有留言分页 |
| PATCH | `/admin/guestbook/{id}` | `{status:"APPROVED"}` 或PENDING |
| DELETE | `/admin/guestbook/{id}` | 删除来信 |
| GET/POST | `/admin/friends` | 列表/创建直接公开友链 |
| PUT/PATCH/DELETE | `/admin/friends/{id}` | 修改/审核状态/删除 |
| PUT | `/admin/site` | 更新完整站点与馆主配置 |

文章写入结构：

```json
{"title":"新的篇章","slug":"a-new-chapter","excerpt":"摘要","body":"## 标题\n\n正文","cover":"/art/library.svg","categoryId":1,"tagIds":[1,2],"status":"DRAFT","featured":false,"version":0}
```

title最长160，slug最长180并只能用小写字母、数字和分隔连字符，excerpt最长400，body最长100000，单篇最多12个标签。新增version为0，编辑必须用GET返回的version，冲突返回409 `EDIT_CONFLICT`。更新成功后version增加。cover允许项目内/art、/uploads文件名或HTTPS外部图片；禁止任意相对路径和javascript等协议。

分类/标签结构：`{name,slug,description}`，标签可省略description。站点配置字段：`title,subtitle,owner,about,github,email,interests,timeline,projects`，后台表单是完整示例。

图片上传解码后将透明背景铺白并重编码JPEG，最大宽度1920。返回 `{url,width,height}`。使用随机UUID文件名，原始文件名不会成为保存路径。上传图片为公开资源，请勿上传私密信息。

限流保存在单进程内存中，重启会重置；Nginx额外限制登录。多实例部署需重新设计共享限流与代理信任范围。新建友链与外部图片仅保存URL，后端不会请求这些外部地址。
