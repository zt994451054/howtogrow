# 后端 API 文档（以此文件为准）

> 基础前缀：`/api/v1`  
> 通用响应：`ApiResponse<T>`（详见文末 **数据结构（DTO）**）  
> 鉴权：HTTP Header `Authorization: Bearer <jwt>`

## 通用约定

### 错误码（常用）
- `OK`
- `INVALID_REQUEST`：参数不合法
- `UNAUTHORIZED`：未登录/Token 无效或过期
- `FORBIDDEN_RESOURCE`：资源越权
- `SUBSCRIPTION_REQUIRED`：需要订阅权益
- `FREE_TRIAL_ALREADY_USED`：免费体验已用完（按用户维度一次）
- `DAILY_ASSESSMENT_ALREADY_SUBMITTED`：当日自测已提交
- `DAILY_ASSESSMENT_INCOMPLETE`：提交不完整
- `QUESTION_POOL_EXHAUSTED`：题库不足/耗尽
- `AI_SUMMARY_ALREADY_GENERATED`：本次自测 AI 总结已生成
- `RATE_LIMITED`：限流（HTTP 429）

### CORS
后端已开启 CORS；预检请求 `OPTIONS` 放行，并会暴露响应头 `X-Trace-Id`。

### 字段与枚举约定（高频）
- `status`：0禁用 1启用
- `gender`：0未知 1男 2女
- `questionType`：`SINGLE`/`MULTI`
- 分页：`page` 从 1 开始；`pageSize` 默认 20，最大 200
- 年龄：均为**周岁（整数，含边界）**，中国时区口径

## 接口一览（按端）

### 小程序端（Miniprogram）
- `POST /miniprogram/auth/wechat-login`（登录）
- `GET /miniprogram/me`（当前用户）
- `GET /miniprogram/children`（孩子列表）
- `POST /miniprogram/children`（新增孩子）
- `PUT /miniprogram/children/{childId}`（更新孩子）
- `DELETE /miniprogram/children/{childId}`（删除孩子）
- `GET /miniprogram/banners`（Banner 列表：最多 5 个上架，按 sort 升序）
- `GET /miniprogram/banners/{id}`（Banner 详情：富文本 HTML）
- `GET /miniprogram/awareness/monthly?childId=...&month=YYYY-MM`（每日觉察月度概览：该月每日育儿状态/烦恼场景/自测/育儿日记）
- `GET /miniprogram/trouble-scenes`（烦恼场景列表：仅未删除）
- `GET /miniprogram/troubles/daily?childId=...&recordDate=YYYY-MM-DD`（查询每日烦恼记录；recordDate 不传默认今天）
- `PUT /miniprogram/troubles/daily`（提交/更新每日烦恼记录：可补录，仅修改不删除，至少选 1 个场景）
- `GET /miniprogram/parenting-status/daily?childId=...&recordDate=YYYY-MM-DD`（查询每日育儿状态；recordDate 不传默认今天）
- `PUT /miniprogram/parenting-status/daily`（提交/更新每日育儿状态：可补录，仅修改不删除）
- `POST /miniprogram/assessments/daily/begin`（开始每日自测：不落库，返回会话ID+题目）
- `POST /miniprogram/assessments/daily/sessions/{sessionId}/replace`（换题：基于会话）
- `POST /miniprogram/assessments/daily/sessions/{sessionId}/submit`（提交作答：基于会话，落库一次性提交）
- `POST /miniprogram/assessments/daily/{assessmentId}/ai-summary`（AI 总结）
- `POST /miniprogram/ai/chat/sessions`（创建会话）
- `GET /miniprogram/ai/chat/sessions?limit=20`（会话列表）
- `POST /miniprogram/ai/chat/sessions/{sessionId}/messages`（发送消息）
- `GET /miniprogram/ai/chat/sessions/{sessionId}/stream`（SSE 流式回复）
- `GET /miniprogram/reports/growth?childId=1&from=2025-12-01&to=2025-12-28`（成长报告）
- `GET /miniprogram/reports/persistence?childId=1`（坚持进步：从首次记录到今日的天数，含首日/今日，最小为1）
- `GET /miniprogram/quotes/random?childId=...&scene=...`（按场景+孩子周岁随机鸡汤语；无数据返回 `data=[]`）
- `GET /miniprogram/subscriptions/plans`（订阅套餐列表）
- `POST /miniprogram/subscriptions/orders`（创建订阅订单）

### 运营端（Admin）
- `POST /admin/auth/login`（登录）
- `GET /admin/auth/me`（当前管理员）
- `GET /admin/rbac/permissions`（权限列表）
- `GET /admin/rbac/roles`（角色列表）
- `POST /admin/rbac/roles`（创建角色）
- `PUT /admin/rbac/roles/{roleId}/permissions`（更新角色权限）
- `GET /admin/rbac/admin-users`（管理员列表）
- `POST /admin/rbac/admin-users`（创建管理员）
- `PUT /admin/rbac/admin-users/{adminUserId}/roles`（更新管理员角色）
- 能力维度（只读）：`GET /admin/dimensions`
- 套餐：`GET/POST /admin/plans`，`PUT/DELETE /admin/plans/{id}`
- 鸡汤语：`GET /admin/quotes`（分页 `page/pageSize`；筛选：`scene/status/keyword`）、`POST /admin/quotes`、`PUT/DELETE /admin/quotes/{id}`
- Banner：`GET /admin/banners`（分页 `page/pageSize`；筛选：`status/keyword`）、`POST /admin/banners`、`PUT/DELETE /admin/banners/{id}`（上架≤5）
- 烦恼场景：`GET /admin/trouble-scenes`（分页 `page/pageSize`；筛选：`keyword/ageYear`）、`POST /admin/trouble-scenes`、`PUT/DELETE /admin/trouble-scenes/{id}`（名称未删除唯一；需填写最小/最大年龄；删除会解除题目关联）
- AI 快捷问题：`GET /admin/ai-quick-questions`（分页 `page/pageSize`；筛选：`status/keyword`）、`POST /admin/ai-quick-questions`、`PUT/DELETE /admin/ai-quick-questions/{id}`
- 烦恼场景导入：`POST /admin/trouble-scenes/import-excel`（`multipart/form-data`，字段名 `file`；任一错误整体失败）
- 上传：`POST /admin/uploads/public`（`multipart/form-data`，字段名 `file`，后端转存 OSS 并返回 URL）
- 题库：`GET /admin/questions`，`GET /admin/questions/{questionId}`，`POST /admin/questions`，`PUT/DELETE /admin/questions/{questionId}`
- 题库导入：`POST /admin/questions/import-excel`（`multipart/form-data`，字段名 `file`；包含烦恼场景名称时需完全匹配，任一错误整体失败）
- 查询：`GET /admin/users` / `GET /admin/orders` / `GET /admin/assessments`（均为分页）

### 支付回调
- `POST /pay/wechat/notify`

## 接口详情（关键参数）

> 说明：除少数 `text/event-stream`/支付回调外，接口均返回 `ApiResponse<T>`；`T` 的字段见文末 **数据结构（DTO）**。

### 小程序端（Miniprogram）

#### `POST /api/v1/miniprogram/auth/wechat-login`（无需鉴权）
- Body(JSON)：`WechatLoginRequest`
- 响应 data：`WechatLoginResponse`

#### `GET /api/v1/miniprogram/me`
- 响应 data：`MiniprogramMeResponse`

#### `POST /api/v1/miniprogram/me/profile`
- Body(JSON)：`UpdateProfileRequest`
- 响应 data：null

#### `POST /api/v1/miniprogram/uploads/avatar`
- `multipart/form-data`：
  - 字段名：`file`（头像文件，`image/*`）
- 响应 data：`UploadAvatarResponse`

#### `POST /api/v1/miniprogram/uploads/diary-image`
- `multipart/form-data`：
  - 字段名：`file`（育儿日记配图，`image/*`）
- 响应 data：`UploadDiaryImageResponse`

#### `PUT /api/v1/miniprogram/children/{childId}` / `DELETE /api/v1/miniprogram/children/{childId}`
- Path：
  - `childId`：孩子ID
> 新增/更新孩子时需填写 `parentIdentity`（爸爸/妈妈/奶奶/爷爷/外公/外婆）。

#### `GET /api/v1/miniprogram/banners` / `GET /api/v1/miniprogram/banners/{id}`
- `GET /.../banners` 响应 data：`BannerListItemView[]`
- `GET /.../banners/{id}` 响应 data：`BannerDetailView`

#### `GET /api/v1/miniprogram/awareness/monthly`
- Query：
  - `childId`：孩子ID（必填）
  - `month`：月份（必填，格式 `YYYY-MM`，且不早于 `2025-06`）
- 规则：
  - 若 `month` 为当前月份（中国时区口径），返回日期范围为 `YYYY-MM-01..今天`，不包含未来日期。
- 响应 data：`MonthlyAwarenessResponse`

#### `GET /api/v1/miniprogram/trouble-scenes`
- 响应 data：`TroubleSceneView[]`

#### `GET /api/v1/miniprogram/troubles/daily` / `PUT /api/v1/miniprogram/troubles/daily`
- `GET` Query：
  - `childId`：孩子ID
  - `recordDate`：可选，默认今天
- `PUT` Body(JSON)：`DailyTroubleRecordUpsertRequest`（`sceneIds` 至少 1 个，可补录、可修改、不可删除）
- 响应 data：`DailyTroubleRecordView`（`GET` 无记录时为 `null`）

#### `GET /api/v1/miniprogram/parenting-status/daily` / `PUT /api/v1/miniprogram/parenting-status/daily`
- `GET` Query：
  - `childId`：孩子ID
  - `recordDate`：可选，默认今天
- `PUT` Body(JSON)：`DailyParentingStatusUpsertRequest`（10 种状态单选，可补录、可修改、不可删除）
- 响应 data：`DailyParentingStatusView`（`GET` 无记录时为 `null`）

#### `GET /api/v1/miniprogram/diary/daily` / `PUT /api/v1/miniprogram/diary/daily`
- `GET` Query：
  - `childId`：孩子ID
  - `recordDate`：可选，默认今天
- `PUT` Body(JSON)：`DailyParentingDiaryUpsertRequest`（可补录、可修改、不可删除）
- 响应 data：`DailyParentingDiaryView`（`GET` 无记录时为 `null`）

#### `POST /api/v1/miniprogram/assessments/daily/sessions/{sessionId}/replace` / `POST /api/v1/miniprogram/assessments/daily/sessions/{sessionId}/submit`
- Path：
  - `sessionId`：自测会话ID
> 每日自测：出题优先匹配“今日烦恼记录 + 年龄”，不足再补全其他题；题数 5-10，做满 5 题后可随时提交（最多 10 题）。

#### `POST /api/v1/miniprogram/assessments/daily/{assessmentId}/ai-summary`
- Path：
  - `assessmentId`：自测ID（提交后返回）

#### `GET /api/v1/miniprogram/assessments/daily/records`
- Query：
  - `limit`：返回条数（默认 20，最大 100）
  - `offset`：偏移量（默认 0）
- 响应 data：`DailyAssessmentRecordView[]`

#### `GET /api/v1/miniprogram/assessments/daily/records/{assessmentId}`
- Path：
  - `assessmentId`：自测记录ID
- 响应 data：`DailyAssessmentRecordDetailResponse`

#### `GET /api/v1/miniprogram/ai/chat/sessions?limit=20`
- Query：
  - `limit`：返回会话数上限（默认 20）

#### `GET /api/v1/miniprogram/ai/chat/sessions/{sessionId}/messages?limit=20&beforeMessageId=...`
- Path：
  - `sessionId`：会话ID
- Query：
  - `limit`：返回消息数上限（默认 20，最大 100）
  - `beforeMessageId`：仅返回 `messageId < beforeMessageId` 的消息（用于分页；可选）
- 响应 data：`AiChatMessageView[]`（按消息时间倒序）

#### `POST /api/v1/miniprogram/ai/chat/sessions/{sessionId}/messages` / `GET /api/v1/miniprogram/ai/chat/sessions/{sessionId}/stream`
- Path：
  - `sessionId`：会话ID
- 订阅与体验：
  - 未订阅/已过期用户：允许发送 1 条消息并获得 1 次 AI 回复；从第 2 条开始返回 `SUBSCRIPTION_REQUIRED`
- `GET .../stream`：
  - 返回 `text/event-stream`
  - 事件：
    - `event: delta`：`data: <增量内容>`（Markdown）
    - `event: done`：`data: [DONE]`
    - `event: error`：`data: <error>`

#### `GET /api/v1/miniprogram/reports/growth`
- Query：
  - `childId`：孩子ID
  - `from`：开始日期（`yyyy-MM-dd`）
  - `to`：结束日期（`yyyy-MM-dd`）
- 响应 data：`GrowthReportResponse`
- 说明：
  - 在 `[from,to]` 内，按 **5 个有数据的业务日** 为一组（不要求连续），每组输出 1 个点。
  - 每个维度得分为组内日得分的平均值；若某天缺少某维度，按 0 计入平均。
  - 每个点的 `bizDate` 为该组内最大的业务日；不足 5 天也直接计算；超过则按每 5 天统计。

#### `GET /api/v1/miniprogram/reports/persistence`
- Query：
  - `childId`：孩子ID
- 响应 data：`AwarenessPersistenceResponse`

### 运营端（Admin）

#### `POST /api/v1/admin/auth/login`（无需鉴权）
- Body(JSON)：`AdminLoginRequest`
- 响应 data：`AdminLoginResponse`

#### `GET /api/v1/admin/auth/me`
- 响应 data：`AdminMeResponse`
- 说明：后端默认**不强制**校验权限码（仅供前端渲染）。如需开启校验：`ADMIN_ENFORCE_PERMISSION_CHECKS=true`。

#### `PUT /api/v1/admin/rbac/roles/{roleId}/permissions`
- Path：
  - `roleId`：角色ID
- Body(JSON)：`RolePermissionUpdateRequest`

#### `PUT /api/v1/admin/rbac/admin-users/{adminUserId}/roles`
- Path：
  - `adminUserId`：管理员ID
- Body(JSON)：`AdminUserRoleUpdateRequest`

#### `GET /api/v1/admin/questions`
- Query：
  - `ageYear`：筛选年龄（岁，可选，返回满足 `minAge<=ageYear<=maxAge` 的题目）
  - `page`：页码（从 1 开始）
  - `pageSize`：每页条数（1-200）
- 响应 data：`PageResponse<QuestionSummaryView>`

#### `GET /api/v1/admin/questions/{questionId}` / `PUT /api/v1/admin/questions/{questionId}` / `DELETE /api/v1/admin/questions/{questionId}`
- Path：
  - `questionId`：题目ID

#### `POST /api/v1/admin/questions/import-excel`
- Content-Type：`multipart/form-data`
- Body：
  - `file`：Excel 文件
- Excel 模板：`program/backend/db/question-import-template.xlsx`（一个 sheet，表头为中文；一行一个选项）
- 响应 data：`QuestionImportResponse`

#### `GET /api/v1/admin/users`
- Query：
  - `page`：页码（从 1 开始）
  - `pageSize`：每页条数（1-200）
  - `userId`：用户ID（可选）
  - `keyword`：昵称/openid 关键词（可选）
  - `freeTrialUsed`：是否已使用免费体验：true/false（可选）
  - `subscriptionStatus`：订阅状态：ACTIVE/EXPIRED/NONE（可选）
- 响应 data：`PageResponse<UserView>`

#### `POST /api/v1/admin/users/{userId}/subscription/extend`
- Path：
  - `userId`：用户ID
- Body(JSON)：`SubscriptionExtendRequest`
- 响应 data：`SubscriptionExtendResponse`

#### `GET /api/v1/admin/orders` / `GET /api/v1/admin/assessments`
- Query：
  - `page`：页码（从 1 开始）
  - `pageSize`：每页条数（1-200）
  - `/assessments` 额外筛选（可选）：
    - `bizDateFrom`：提交日期起（yyyy-MM-dd，北京时间口径）
    - `bizDateTo`：提交日期止（yyyy-MM-dd，北京时间口径）
    - `userId`：用户ID
    - `childId`：孩子ID
    - `keyword`：用户/孩子昵称关键词
- 响应 data：
  - `/orders`：`PageResponse<OrderView>`
  - `/assessments`：`PageResponse<AssessmentView>`

#### `GET /api/v1/admin/assessments/export-excel`
- 说明：按筛选条件导出自测记录 Excel（若数据量过大需缩小筛选范围）
- Query（均可选）：
  - `bizDateFrom`：提交日期起（yyyy-MM-dd，北京时间口径）
  - `bizDateTo`：提交日期止（yyyy-MM-dd，北京时间口径）
  - `userId`：用户ID
  - `childId`：孩子ID
  - `keyword`：用户/孩子昵称关键词
- 响应：Excel 文件（`.xlsx`，`Content-Disposition: attachment`）

#### `GET /api/v1/admin/children`
- Query：
  - `page`：页码（从 1 开始）
  - `pageSize`：每页条数（1-200）
  - `userId`：用户ID（可选）
  - `userNickname`：用户昵称关键词（可选）
  - `childId`：孩子ID（可选）
  - `childNickname`：孩子昵称关键词（可选）
  - `gender`：性别：0未知 1男 2女（可选）
  - `ageMin`：年龄下限（岁，含边界，可选）
  - `ageMax`：年龄上限（岁，含边界，可选）
  - `status`：状态：1启用 0删除（可选）
- 响应 data：`PageResponse<AdminChildView>`

### 支付回调

#### `POST /api/v1/pay/wechat/notify`
- Body：微信支付回调原始 JSON 字符串（事件落库 + 验签/解密 + 发放订阅）
- 响应：`NotifyAck`（注意：该接口不使用 `ApiResponse<T>`）

## 数据结构（DTO）

### `ApiResponse<T>`（通用响应外层）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | string | 业务码：OK/INVALID_REQUEST/... |
| `message` | string | 提示信息 |
| `data` | object | 响应数据（按接口而定） |
| `traceId` | string | 链路追踪ID（用于排查问题） |

### 小程序端（Miniprogram）

#### `WechatLoginRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | string | `wx.login()` 返回的 code |

#### `WechatLoginResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `token` | string | JWT Token |
| `expiresIn` | number | Token 有效期（秒） |
| `user` | object | `MiniprogramUserView` |

#### `MiniprogramMeResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `user` | object | `MiniprogramUserView` |

#### `MiniprogramUserView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 用户ID |
| `nickname` | string/null | 昵称（可为空） |
| `avatarUrl` | string/null | 头像URL（可为空） |
| `birthDate` | string/null | 出生日期（YYYY-MM-DD，可为空） |
| `subscriptionEndAt` | string/null | 订阅到期时间（可为空，ISO-8601） |
| `freeTrialUsed` | boolean | 是否已使用免费体验 |

#### `UpdateProfileRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nickname` | string | 昵称 |
| `avatarUrl` | string | 头像URL |
| `birthDate` | string/null | 出生日期（YYYY-MM-DD，可选） |

#### `UploadAvatarResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `url` | string | 上传后的可访问 URL（用于后续保存到用户资料） |

#### `ChildCreateRequest` / `ChildUpdateRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `nickname` | string | 孩子昵称 |
| `gender` | number | 性别：0未知 1男 2女 |
| `birthDate` | string | 出生日期（YYYY-MM-DD） |

#### `ChildCreateResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `childId` | number | 新建孩子ID |

#### `ChildView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 孩子ID |
| `nickname` | string | 孩子昵称 |
| `gender` | number | 性别：0未知 1男 2女 |
| `birthDate` | string | 出生日期（YYYY-MM-DD） |

#### `DailyAssessmentBeginRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `childId` | number | 孩子ID |

#### `DailyAssessmentBeginResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sessionId` | string | 自测会话ID（后续换题/提交使用） |
| `items` | array | `DailyAssessmentItemView[]`（固定 5 题） |

#### `DailyAssessmentItemView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `displayOrder` | number | 题目展示顺序（1..5） |
| `questionId` | number | 题目ID |
| `content` | string | 题目内容 |
| `questionType` | string | 题型：SINGLE/MULTI |
| `options` | array | `QuestionOptionView[]` |

#### `QuestionOptionView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `optionId` | number | 选项ID |
| `content` | string | 选项内容 |
| `sortNo` | number | 排序号（升序） |
| `suggestFlag` | number | 建议属性：1建议 0不建议 |
| `improvementTip` | string/null | 改进建议文案（可选） |

#### `DailyAssessmentReplaceRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `childId` | number | 孩子ID |
| `displayOrder` | number | 要替换的题目顺序（1..5） |

#### `DailyAssessmentReplaceResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `displayOrder` | number | 被替换的题目顺序（1..5） |
| `newItem` | object | `DailyAssessmentItemView` |

#### `DailyAssessmentSubmitRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `childId` | number | 孩子ID |
| `answers` | array | `DailyAssessmentAnswerRequest[]`（必须覆盖 5 题） |

#### `DailyAssessmentAnswerRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `questionId` | number | 题目ID |
| `optionIds` | array | 选中选项ID列表（单选题必须且只能 1 个） |

#### `DailyAssessmentSubmitResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `assessmentId` | number | 自测记录ID（用于 AI 总结等后续接口） |
| `dimensionScores` | array | `DimensionScoreView[]` |

#### `DailyAssessmentRecordView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `assessmentId` | number | 自测记录ID |
| `childId` | number | 孩子ID |
| `childName` | string | 孩子昵称 |
| `submittedAt` | string | 提交时间（ISO-8601） |
| `aiSummary` | string/null | AI 总结内容（已生成则返回） |

#### `DailyAssessmentRecordDetailResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `assessmentId` | number | 自测记录ID |
| `childId` | number | 孩子ID |
| `childName` | string | 孩子昵称 |
| `submittedAt` | string | 提交时间（ISO-8601） |
| `aiSummary` | string/null | AI 总结内容（已生成则返回） |
| `items` | array | `DailyAssessmentItemView[]` |
| `answers` | array | `DailyAssessmentAnswerView[]` |

#### `DailyAssessmentAnswerView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `questionId` | number | 题目ID |
| `optionIds` | array | 选中选项ID列表 |

#### `DimensionScoreView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `dimensionCode` | string | 维度编码 |
| `dimensionName` | string | 维度名称 |
| `score` | number | 分值 |

#### `AiSummaryResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `content` | string | 总结内容（<=70字） |

#### `AiChatCreateSessionRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `childId` | number/null | 关联孩子ID（可选） |

#### `AiChatCreateSessionResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sessionId` | number | 会话ID |

#### `AiChatSessionView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sessionId` | number | 会话ID |
| `childId` | number/null | 关联孩子ID（可选） |
| `title` | string/null | 会话标题（可选；默认取用户首条提问） |
| `status` | string | 状态：ACTIVE/CLOSED |
| `lastActiveAt` | string | 最后活跃时间（ISO-8601） |

#### `AiChatMessageCreateRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `content` | string | 消息内容（Markdown/纯文本） |

#### `AiChatMessageCreateResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `messageId` | number | 消息ID |

#### `AiChatMessageView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `messageId` | number | 消息ID |
| `role` | string | 角色：user/assistant/system |
| `content` | string | 消息内容（Markdown/纯文本） |
| `createdAt` | string | 消息时间（ISO-8601） |

#### `GrowthReportResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `childId` | number | 孩子ID |
| `from` | string | 起始日期（YYYY-MM-DD） |
| `to` | string | 结束日期（YYYY-MM-DD） |
| `days` | array | `GrowthDayView[]` |

#### `GrowthDayView`（`GrowthReportResponse.days[]`）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `bizDate` | string | 业务日期（YYYY-MM-DD） |
| `dimensionScores` | array | `DimensionScoreView[]` |

#### `AwarenessPersistenceResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `childId` | number | 孩子ID |
| `firstRecordDate` | string | 首次记录日期（YYYY-MM-DD；若无任何记录则为今日） |
| `today` | string | 今日（YYYY-MM-DD，中国时区口径） |
| `persistenceDays` | number | 坚持进步天数（含首日/今日，最小为1） |

#### `QuoteResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `content` | string | 鸡汤语内容 |

#### `MonthlyAwarenessResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `childId` | number | 孩子ID |
| `month` | string | 月份（YYYY-MM） |
| `days` | array | `MonthlyAwarenessDayView[]` |

#### `MonthlyAwarenessDayView`（`MonthlyAwarenessResponse.days[]`）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `recordDate` | string | 日期（YYYY-MM-DD） |
| `parentingStatusCode` | string | 育儿状态（失望/平静/乐观/难过/无奈/愤怒/欣慰/担忧/开心/绝望；为空表示未记录） |
| `parentingStatusMoodId` | string | 育儿状态表情ID（disappointed/calm/optimistic/happy/sad/worried/helpless/angry/relieved/desperate；为空表示未记录） |
| `troubleScenes` | array | `MonthlyAwarenessTroubleSceneView[]` |
| `assessment` | object | `MonthlyAwarenessAssessmentView`（为空表示未完成） |
| `diary` | object | `MonthlyAwarenessDiaryView`（为空表示未记录） |

#### `MonthlyAwarenessTroubleSceneView`（`MonthlyAwarenessDayView.troubleScenes[]`）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 场景ID |
| `name` | string | 场景名称 |
| `logoUrl` | string | 场景 Logo URL |

#### `MonthlyAwarenessAssessmentView`（`MonthlyAwarenessDayView.assessment`）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `assessmentId` | number | 自测记录ID |
| `submittedAt` | string | 提交时间（ISO-8601，+08:00） |
| `aiSummary` | string | AI 总结（可选） |

#### `MonthlyAwarenessDiaryView`（`MonthlyAwarenessDayView.diary`）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `content` | string | 日记内容 |
| `imageUrl` | string | 配图 URL（可选） |

#### `DailyParentingDiaryView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `childId` | number | 孩子ID |
| `recordDate` | string | 记录日期（YYYY-MM-DD） |
| `content` | string | 日记内容 |
| `imageUrl` | string | 配图 URL（可选） |

#### `DailyParentingDiaryUpsertRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `childId` | number | 孩子ID |
| `recordDate` | string | 记录日期（YYYY-MM-DD；可选，默认今天） |
| `content` | string | 日记内容（可为空；但 content/imageUrl 至少 1 个不为空） |
| `imageUrl` | string | 配图 URL（可选） |

#### `UploadDiaryImageResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `url` | string | 图片 URL |

#### `SubscriptionPlanView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `planId` | number | 套餐ID |
| `name` | string | 套餐名称 |
| `days` | number | 套餐天数 |
| `priceCent` | number | 价格（分） |

#### `SubscriptionOrderCreateRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `planId` | number | 套餐ID |

#### `SubscriptionOrderCreateResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `orderNo` | string | 业务订单号 |
| `payParams` | object | `PayParams`（用于 `wx.requestPayment`） |

#### `PayParams`（`SubscriptionOrderCreateResponse.payParams`）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `timeStamp` | string | 时间戳（秒） |
| `nonceStr` | string | 随机串 |
| `package` | string | `package` 字段（通常为 `prepay_id=...`） |
| `signType` | string | 签名类型（通常 RSA） |
| `paySign` | string | 签名 |

### 运营端（Admin）

#### `AdminLoginRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `username` | string | 用户名 |
| `password` | string | 密码（明文） |

#### `AdminLoginResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `token` | string | JWT Token |
| `expiresIn` | number | Token 有效期（秒） |

#### `AdminMeResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `adminUserId` | number | 管理员ID |
| `username` | string | 用户名 |
| `permissionCodes` | array | 权限码列表（用于前端渲染） |

#### `PermissionView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `permissionId` | number | 权限ID |
| `code` | string | 权限码 |
| `name` | string | 权限名称 |

#### `RoleView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `roleId` | number | 角色ID |
| `code` | string | 角色编码 |
| `name` | string | 角色名称 |
| `permissionCodes` | array | 权限码列表 |

#### `RoleCreateRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | string | 角色编码（唯一） |
| `name` | string | 角色名称 |

#### `RolePermissionUpdateRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `permissionCodes` | array | 权限码列表 |

#### `AdminUserView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `adminUserId` | number | 管理员ID |
| `username` | string | 用户名 |
| `status` | number | 状态：0禁用 1启用 |
| `createdAt` | string | 创建时间（ISO-8601） |
| `roleCodes` | array | 角色编码列表 |

#### `AdminUserCreateRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `username` | string | 用户名 |
| `password` | string | 初始密码（明文） |
| `roleCodes` | array | 角色编码列表 |

#### `AdminUserRoleUpdateRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `roleCodes` | array | 角色编码列表 |

#### `DimensionView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | string | 维度编码（唯一） |
| `name` | string | 维度名称 |
| `sortNo` | number | 排序号（越小越靠前） |

> 说明：能力维度为固定枚举（不提供增删改），当前固定 5 个编码：
> `EMOTION_MANAGEMENT`、`COMMUNICATION_EXPRESSION`、`RULE_GUIDANCE`、`RELATIONSHIP_BUILDING`、`LEARNING_SUPPORT`

#### `PlanCreateRequest` / `PlanUpdateRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `name` | string | 套餐名称 |
| `days` | number | 套餐天数 |
| `priceCent` | number | 价格（分） |
| `status` | number | 状态：0禁用 1启用 |

#### `PlanView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `planId` | number | 套餐ID |
| `name` | string | 套餐名称 |
| `days` | number | 套餐天数 |
| `priceCent` | number | 价格（分） |
| `status` | number | 状态：0禁用 1启用 |

#### `QuoteCreateRequest` / `QuoteUpdateRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `content` | string | 内容 |
| `status` | number | 状态：0禁用 1启用 |

#### `QuoteView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 鸡汤语ID |
| `content` | string | 内容 |
| `status` | number | 状态：0禁用 1启用 |

#### `PageResponse<T>`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `page` | number | 页码（从1开始） |
| `pageSize` | number | 每页条数 |
| `total` | number | 总数 |
| `items` | array | 数据列表 |

#### `QuestionSummaryView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `questionId` | number | 题目ID |
| `minAge` | number | 适用最小年龄（整数，单位：岁，含边界） |
| `maxAge` | number | 适用最大年龄（整数，单位：岁，含边界） |
| `questionType` | string | 题型：SINGLE/MULTI |
| `status` | number | 状态：0禁用 1启用 |
| `content` | string | 题干内容 |

#### `QuestionDetailView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `questionId` | number | 题目ID |
| `minAge` | number | 适用最小年龄（整数，单位：岁，含边界） |
| `maxAge` | number | 适用最大年龄（整数，单位：岁，含边界） |
| `questionType` | string | 题型：SINGLE/MULTI |
| `content` | string | 题干内容 |
| `options` | array | `OptionView[]` |

#### `OptionView`（`QuestionDetailView.options[]`）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `optionId` | number | 选项ID |
| `content` | string | 选项内容 |
| `suggestFlag` | number | 是否建议：0否 1是 |
| `improvementTip` | string/null | 改进建议（可为空） |
| `sortNo` | number | 排序号（越小越靠前） |
| `dimensionScores` | array | `DimensionScore[]` |

#### `DimensionScore`（`QuestionDetailView.options[].dimensionScores[]`）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `dimensionCode` | string | 维度编码 |
| `dimensionName` | string | 维度名称 |
| `score` | number | 分值 |

#### `QuestionUpsertRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `minAge` | number | 适用最小年龄（整数，单位：岁，含边界） |
| `maxAge` | number | 适用最大年龄（整数，单位：岁，含边界） |
| `questionType` | string | 题型：SINGLE/MULTI |
| `content` | string | 题干内容 |
| `status` | number | 状态：0禁用 1启用 |
| `options` | array | `OptionUpsert[]` |

#### `OptionUpsert`（`QuestionUpsertRequest.options[]`）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `content` | string | 选项内容 |
| `suggestFlag` | number | 是否建议：0否 1是 |
| `improvementTip` | string/null | 改进建议（可为空） |
| `sortNo` | number | 排序号（越小越靠前） |
| `dimensionScores` | array | `DimensionScoreUpsert[]` |

#### `DimensionScoreUpsert`（`QuestionUpsertRequest.options[].dimensionScores[]`）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `dimensionCode` | string | 维度编码 |
| `score` | number | 分值（>=1） |

#### `QuestionImportResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `total` | number | 总行数 |
| `success` | number | 成功数 |
| `failed` | number | 失败数 |
| `failures` | array | `Failure[]` |

#### `Failure`（`QuestionImportResponse.failures[]`）
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `row` | number | 行号（Excel 中的行号） |
| `reason` | string | 失败原因 |

#### `UserView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | number | 用户ID |
| `wechatOpenid` | string/null | 微信 openid |
| `nickname` | string/null | 昵称 |
| `avatarUrl` | string/null | 头像URL |
| `subscriptionEndAt` | string/null | 订阅到期时间（可为空，ISO-8601） |
| `freeTrialUsed` | boolean | 是否已使用免费体验 |
| `createdAt` | string | 创建时间（ISO-8601） |

#### `SubscriptionExtendRequest`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `days` | number | 延长天数（在当前订阅基础上增加） |

#### `SubscriptionExtendResponse`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | number | 用户ID |
| `subscriptionEndAt` | string/null | 订阅到期时间（可为空，ISO-8601） |

#### `OrderView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `orderId` | number | 订单ID |
| `orderNo` | string | 订单号 |
| `userId` | number | 用户ID |
| `userNickname` | string/null | 用户昵称 |
| `userAvatarUrl` | string/null | 用户头像URL |
| `planId` | number | 套餐ID |
| `planName` | string | 套餐名称 |
| `amountCent` | number | 订单金额（分） |
| `status` | string | 订单状态（`CREATED`待支付 / `PAID`已支付） |
| `payTradeNo` | string/null | 支付平台交易号 |
| `prepayId` | string/null | 微信预支付ID |
| `createdAt` | string | 创建时间（ISO-8601） |
| `paidAt` | string/null | 支付时间（可为空，ISO-8601） |

#### `AssessmentView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `assessmentId` | number | 自测ID |
| `userId` | number | 用户ID |
| `userNickname` | string/null | 用户昵称 |
| `userAvatarUrl` | string/null | 用户头像URL |
| `childId` | number | 孩子ID |
| `childNickname` | string/null | 孩子昵称 |
| `bizDate` | string | 业务日期（yyyy-MM-dd） |
| `submittedAt` | string/null | 提交时间（可为空，ISO-8601） |
| `emotionManagementScore` | number | 情绪管理力得分 |
| `communicationExpressionScore` | number | 沟通表达力得分 |
| `ruleGuidanceScore` | number | 规则引导力得分 |
| `relationshipBuildingScore` | number | 关系建设力得分 |
| `learningSupportScore` | number | 学习支持力得分 |
| `dimensionScores` | array | `AssessmentDimensionScoreView[]` |

#### `AssessmentDimensionScoreView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `dimensionCode` | string | 维度编码 |
| `dimensionName` | string | 维度名称 |
| `score` | number | 得分 |

#### `AdminChildView`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `childId` | number | 孩子ID |
| `userId` | number | 所属用户ID |
| `userNickname` | string/null | 用户昵称 |
| `userAvatarUrl` | string/null | 用户头像URL |
| `childNickname` | string | 孩子昵称 |
| `gender` | number | 性别：0未知 1男 2女 |
| `birthDate` | string | 出生日期（YYYY-MM-DD） |
| `ageYear` | number | 年龄（整数，单位：岁） |
| `status` | number | 状态：1启用 0删除 |
| `createdAt` | string | 创建时间（ISO-8601） |

### 支付回调（Pay）

#### `NotifyAck`
| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | string | 返回码 |
| `message` | string | 返回信息 |
