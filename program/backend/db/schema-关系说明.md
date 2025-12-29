# 数据库 Schema 表关系说明

> 适用范围：`program/backend/db/schema.sql`（MySQL 8.0）。
> 目标：用“可读的方式”解释各表的职责与关系（1:1 / 1:N / N:N），方便研发、运营、数据排查与审计对齐口径。

## 1. 领域划分（按业务模块）
- 用户与孩子：`user_account`、`child`
- 题库：`question`、`question_option`、`option_dimension_score`
- 每日自测：`daily_assessment`、`daily_assessment_item`、`daily_assessment_answer`、`daily_assessment_dimension_score`
- AI 能力：`ai_assessment_summary`、`ai_chat_session`、`ai_chat_message`
- 订阅与支付：`subscription_plan`、`purchase_order`、`subscription_grant`、`wechat_pay_transaction`、`wechat_pay_notify_event`
- 运营端权限：`admin_user`、`admin_role`、`admin_permission`、`admin_user_role`、`admin_role_permission`
- 内容：`quote`

## 2. 核心实体与关系总览（文字版 ER）

### 2.1 用户与孩子
- `user_account (1) -> (N) child`
  - 关系字段：`child.user_id -> user_account.id`
  - 说明：一个家长可维护多个孩子；孩子软删除不影响历史自测记录的归档与统计。

### 2.2 题库（按整数年龄范围）
- `question`
  - 年龄字段：`question.min_age` / `question.max_age`（整数，单位：岁，含边界）
  - 说明：题库不再单独维护“年龄段配置表”，出题时按孩子的整数年龄匹配题库（`min_age <= age <= max_age`）。

- `question (1) -> (N) question_option`
  - 关系字段：`question_option.question_id -> question.id`
  - 说明：题目可单选或多选（`question.question_type`），但选项结构一致。

- `question_option (1) -> (N) option_dimension_score`
  - 关系字段：`option_dimension_score.option_id -> question_option.id`
  - 维度字段：`option_dimension_score.dimension_code`（能力维度编码，枚举常量）
  - 说明：
    - 能力维度不再使用数据库管理，后端代码以枚举常量维护固定维度列表与展示名称/排序。
    - 同一个选项在同一维度上只能有一条分值映射（`uk_option_dimension`）。

### 2.3 每日自测（强一致链路）
- `user_account (1) -> (N) daily_assessment`
  - 关系字段：`daily_assessment.user_id -> user_account.id`

- `child (1) -> (N) daily_assessment`
  - 关系字段：`daily_assessment.child_id -> child.id`
  - 关键约束：北京时间口径，`(user_id, child_id, 当天)` 只能有一条“已提交”记录（提交前查询 `submitted_at` 拦截）。

- `daily_assessment (1) -> (N) daily_assessment_item`
  - 关系字段：`daily_assessment_item.assessment_id -> daily_assessment.id`
  - 说明：每次自测固定 5 题；自测题目在“提交时一次性写入”（换题仅在会话缓存中记录，不在 DB 记录换题链路）。

- `daily_assessment_item (1) -> (N) daily_assessment_answer`
  - 关系字段：`daily_assessment_answer.assessment_item_id -> daily_assessment_item.id`
  - 说明：多选题会产生多行作答；用 `uk_answer_unique (assessment_item_id, option_id)` 防重复。

- `daily_assessment_answer (1) -> (N) daily_assessment_dimension_score`
  - 关系字段：
    - `daily_assessment_dimension_score.assessment_answer_id -> daily_assessment_answer.id`
    - `daily_assessment_dimension_score.assessment_id -> daily_assessment.id`（冗余便于按自测聚合查询）
  - 说明：
    - `daily_assessment_dimension_score` 存的是“维度得分明细”（每个作答选项在各维度的分值）
    - 查询/报告时再按 `assessment_id + dimension_code` 聚合求和，便于后续调整计分口径而不丢细节

### 2.4 AI 自测总结（每次自测最多一条）
- `daily_assessment (1) -> (0..1) ai_assessment_summary`
  - 关系字段：`ai_assessment_summary.assessment_id -> daily_assessment.id`
  - 关键约束：`ai_assessment_summary` 对 `assessment_id` 唯一，保证“最多生成 1 次”。

### 2.5 AI 实时对话（会话级上下文）
- `user_account (1) -> (N) ai_chat_session`
  - 关系字段：`ai_chat_session.user_id -> user_account.id`

- `ai_chat_session (1) -> (N) ai_chat_message`
  - 关系字段：`ai_chat_message.session_id -> ai_chat_session.id`
  - 说明：按会话保留上下文（不跨会话/跨天）；后端通常取“最近 N 条消息”拼接上下文。

- `child (0..1) <- ai_chat_session.child_id`
  - 说明：会话可选关联某个孩子，便于对话上下文带上年龄/性别等信息（仍需在服务端做用户归属校验）。

### 2.6 订阅与支付（微信支付 v3，小程序 JSAPI）
- `subscription_plan (1) -> (N) purchase_order`
  - 关系字段：`purchase_order.plan_id -> subscription_plan.id`

- `user_account (1) -> (N) purchase_order`
  - 关系字段：`purchase_order.user_id -> user_account.id`

- `purchase_order (1) -> (0..1) subscription_grant`
  - 关系字段：`subscription_grant.order_id -> purchase_order.id`
  - 关键约束：`subscription_grant` 对 `order_id` 唯一，用作“订阅发放幂等锚点”（避免回调重放导致重复加天数）。
  - 口径：`granted_from` 以微信支付 `success_time` 为准（与 `purchase_order.paid_at` 对齐）。

- `purchase_order (1) -> (0..1) wechat_pay_transaction`
  - 关系字段：`wechat_pay_transaction.order_id -> purchase_order.id`
  - 说明：结构化交易流水（`transaction_id/success_time/amount/bank_type/payer_openid` 等），方便运营查询与对账。

- `wechat_pay_notify_event`
  - 说明：微信回调事件原文审计与幂等（以 `event_id` 唯一）。
  - 关系：与 `wechat_pay_transaction.latest_event_id` 为“弱关联”（非外键），避免强依赖导致回调落库顺序耦合；业务层仍需校验一致性。

### 2.7 运营端 RBAC
- `admin_user (N) <-> (N) admin_role`（通过 `admin_user_role`）
  - 关系字段：
    - `admin_user_role.admin_user_id -> admin_user.id`
    - `admin_user_role.role_id -> admin_role.id`

- `admin_role (N) <-> (N) admin_permission`（通过 `admin_role_permission`）
  - 关系字段：
    - `admin_role_permission.role_id -> admin_role.id`
    - `admin_role_permission.permission_id -> admin_permission.id`

## 3. 典型查询路径（用于接口设计/索引检查）
- 获取某用户孩子列表：`user_account -> child (idx_child_user_id)`
- 开始每日自测：`child -> question(idx_question_age_range)`（题目下发与换题判重通过 Redis 会话缓存实现）
- 查看一次自测详情：`daily_assessment -> daily_assessment_item -> daily_assessment_answer` + `daily_assessment_dimension_score`
- 成长报告趋势：按 `child_id + submitted_at(按天)` 查询 `daily_assessment`，再对 `daily_assessment_dimension_score` 按天聚合（通过 `assessment_id` 关联）
- 订单与支付对账：`purchase_order` + `wechat_pay_transaction` + `subscription_grant`（按 `order_no/transaction_id/success_time`）
