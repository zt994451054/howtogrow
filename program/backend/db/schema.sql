-- MySQL 8.0 / utf8mb4
-- 说明：该 DDL 作为“业务模型优先”的初版表设计，后续建议用 Flyway/Liquibase 管理迁移。

SET NAMES utf8mb4;

-- =========
-- 年龄段（题库直接使用“整数年龄”，不再单独维护年龄段配置表）
-- =========

-- =========
-- 用户（家长）
-- =========
CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  wechat_openid VARCHAR(64) NOT NULL COMMENT '微信openid（小程序）',
  wechat_unionid VARCHAR(64) NULL COMMENT '微信unionid（如可获取）',
  nickname VARCHAR(64) NULL COMMENT '昵称',
  avatar_url VARCHAR(512) NULL COMMENT '头像URL',
  birth_date DATE NULL COMMENT '出生日期（可选）',
  phone VARCHAR(32) NULL COMMENT '手机号（如采集；注意脱敏与合规）',
  subscription_end_at DATETIME(3) NULL COMMENT '订阅到期时间（为空表示未订阅）',
  free_trial_used TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已使用免费体验：0否 1是',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删除时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_wechat_openid (wechat_openid),
  KEY idx_user_subscription_end_at (subscription_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家长用户账户';

-- =========
-- 孩子
-- =========
CREATE TABLE IF NOT EXISTS child (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '所属用户ID',
  nickname VARCHAR(64) NOT NULL COMMENT '孩子昵称',
  gender TINYINT NOT NULL DEFAULT 0 COMMENT '性别：0未知 1男 2女',
  birth_date DATE NOT NULL COMMENT '出生日期',
  parent_identity VARCHAR(16) NOT NULL COMMENT '家长身份：爸爸/妈妈/奶奶/爷爷/外公/外婆/其他监护人',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0删除',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删除时间',
  PRIMARY KEY (id),
  KEY idx_child_user_id (user_id),
  CONSTRAINT fk_child_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT ck_child_parent_identity CHECK (parent_identity IN ('爸爸','妈妈','奶奶','爷爷','外公','外婆','其他监护人'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='孩子信息';

-- =========
-- 能力维度（代码枚举常量固定，不使用数据库管理）
-- =========

-- =========
-- Banner（小程序轮播）
-- =========
CREATE TABLE IF NOT EXISTS banner (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  title VARCHAR(64) NOT NULL COMMENT '标题',
  image_url VARCHAR(512) NOT NULL COMMENT '封面图URL（点击进入富文本渲染页）',
  html_content MEDIUMTEXT NOT NULL COMMENT '富文本HTML（支持图/视/音标签；小程序端渲染）',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：1上架 0下架',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '轮播顺序（越小越靠前）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删除时间',
  PRIMARY KEY (id),
  KEY idx_banner_status_sort (status, sort_no, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Banner（小程序轮播）';

-- =========
-- 烦恼场景
-- =========
CREATE TABLE IF NOT EXISTS trouble_scene (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  name VARCHAR(64) NOT NULL COMMENT '名称（未删除唯一；删除时建议改名以释放唯一约束）',
  logo_url VARCHAR(512) NULL COMMENT 'logo图片URL',
  min_age INT NOT NULL COMMENT '适用最小年龄（整数，单位：岁，含边界）',
  max_age INT NOT NULL COMMENT '适用最大年龄（整数，单位：岁，含边界）',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0删除',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删除时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_trouble_scene_name (name),
  KEY idx_trouble_scene_status (status),
  KEY idx_trouble_scene_age_range (min_age, max_age),
  CONSTRAINT ck_trouble_scene_age_range CHECK (min_age >= 0 AND max_age <= 18 AND min_age <= max_age)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烦恼场景';

-- =========
-- 每日育儿状态（按天记录；用户+孩子+日期唯一）
-- =========
CREATE TABLE IF NOT EXISTS daily_parenting_status (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  child_id BIGINT UNSIGNED NOT NULL COMMENT '孩子ID',
  record_date DATE NOT NULL COMMENT '记录日期（中国时区）',
  status_code VARCHAR(16) NOT NULL COMMENT '育儿状态：失望/平静/乐观/难过/无奈/愤怒/欣慰/担忧/开心/绝望',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_parenting_status_user_child_day (user_id, child_id, record_date),
  KEY idx_parenting_status_child_day (child_id, record_date),
  CONSTRAINT fk_parenting_status_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_parenting_status_child FOREIGN KEY (child_id) REFERENCES child(id),
  CONSTRAINT ck_parenting_status_code CHECK (status_code IN ('失望','平静','乐观','难过','无奈','愤怒','欣慰','担忧','开心','绝望'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日育儿状态（每人每孩每天最多1条）';

-- =========
-- 每日烦恼记录（按天记录；用户+孩子+日期唯一；可多选烦恼场景）
-- =========
CREATE TABLE IF NOT EXISTS daily_trouble_record (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  child_id BIGINT UNSIGNED NOT NULL COMMENT '孩子ID',
  record_date DATE NOT NULL COMMENT '记录日期（中国时区）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_trouble_record_user_child_day (user_id, child_id, record_date),
  KEY idx_trouble_record_child_day (child_id, record_date),
  CONSTRAINT fk_trouble_record_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_trouble_record_child FOREIGN KEY (child_id) REFERENCES child(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日烦恼记录';

CREATE TABLE IF NOT EXISTS daily_trouble_record_scene (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  record_id BIGINT UNSIGNED NOT NULL COMMENT '每日烦恼记录ID',
  scene_id BIGINT UNSIGNED NOT NULL COMMENT '烦恼场景ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_trouble_record_scene_unique (record_id, scene_id),
  KEY idx_trouble_scene_id (scene_id),
  CONSTRAINT fk_trouble_record_scene_record FOREIGN KEY (record_id) REFERENCES daily_trouble_record(id),
  CONSTRAINT fk_trouble_record_scene_scene FOREIGN KEY (scene_id) REFERENCES trouble_scene(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日烦恼记录-场景明细（多选）';

-- =========
-- 育儿日记（按天记录；用户+孩子+日期唯一）
-- =========
CREATE TABLE IF NOT EXISTS daily_parenting_diary (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  child_id BIGINT UNSIGNED NOT NULL COMMENT '孩子ID',
  record_date DATE NOT NULL COMMENT '记录日期（中国时区）',
  content TEXT NOT NULL COMMENT '日记内容',
  image_url VARCHAR(512) NULL COMMENT '配图 URL（可选）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_diary_user_child_day (user_id, child_id, record_date),
  KEY idx_diary_child_day (child_id, record_date),
  CONSTRAINT fk_diary_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_diary_child FOREIGN KEY (child_id) REFERENCES child(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='育儿日记（每人每孩每天最多1条）';

-- =========
-- 题库：问题/选项/选项-维度分值
-- =========
CREATE TABLE IF NOT EXISTS question (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  content TEXT NOT NULL COMMENT '题目内容',
  min_age INT NOT NULL COMMENT '适用最小年龄（整数，单位：岁，含边界）',
  max_age INT NOT NULL COMMENT '适用最大年龄（整数，单位：岁，含边界）',
  question_type VARCHAR(16) NOT NULL DEFAULT 'MULTI' COMMENT '题型：SINGLE单选 MULTI多选',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删除时间',
  PRIMARY KEY (id),
  KEY idx_question_age_range (min_age, max_age),
  KEY idx_question_status (status),
  CONSTRAINT ck_question_age_range CHECK (min_age >= 0 AND max_age <= 18 AND min_age <= max_age)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库-问题';

CREATE TABLE IF NOT EXISTS question_trouble_scene (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  question_id BIGINT UNSIGNED NOT NULL COMMENT '问题ID',
  scene_id BIGINT UNSIGNED NOT NULL COMMENT '烦恼场景ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_question_scene_unique (question_id, scene_id),
  KEY idx_qts_scene_id (scene_id),
  CONSTRAINT fk_qts_question FOREIGN KEY (question_id) REFERENCES question(id),
  CONSTRAINT fk_qts_scene FOREIGN KEY (scene_id) REFERENCES trouble_scene(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库-问题关联烦恼场景（可为空；弱关系）';

CREATE TABLE IF NOT EXISTS question_option (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  question_id BIGINT UNSIGNED NOT NULL COMMENT '所属问题ID',
  content VARCHAR(512) NOT NULL COMMENT '选项内容',
  suggest_flag TINYINT NOT NULL DEFAULT 1 COMMENT '建议属性：1建议 0不建议',
  improvement_tip VARCHAR(1024) NULL COMMENT '改进建议文案（可选）',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号（升序）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删除时间',
  PRIMARY KEY (id),
  KEY idx_option_question_id (question_id),
  CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES question(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库-问题选项';

CREATE TABLE IF NOT EXISTS option_dimension_score (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  option_id BIGINT UNSIGNED NOT NULL COMMENT '选项ID',
  dimension_code VARCHAR(64) NOT NULL COMMENT '能力维度编码（枚举常量）',
  score INT NOT NULL COMMENT '分值（正整数）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_option_dimension (option_id, dimension_code),
  KEY idx_ods_dimension_code (dimension_code),
  CONSTRAINT fk_ods_option FOREIGN KEY (option_id) REFERENCES question_option(id),
  CONSTRAINT ck_ods_score CHECK (score > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库-选项在各维度的分值映射';

-- =========
-- 每日自测：记录/题目/作答/维度得分
-- =========
CREATE TABLE IF NOT EXISTS daily_assessment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  child_id BIGINT UNSIGNED NOT NULL COMMENT '孩子ID',
  submitted_at DATETIME(3) NOT NULL COMMENT '提交时间（北京时间口径）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_daily_assessment_user_child_submitted (user_id, child_id, submitted_at),
  KEY idx_daily_assessment_child_submitted (child_id, submitted_at),
  CONSTRAINT fk_assessment_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_assessment_child FOREIGN KEY (child_id) REFERENCES child(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日自测（仅记录已提交）';

CREATE TABLE IF NOT EXISTS daily_assessment_item (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  assessment_id BIGINT UNSIGNED NOT NULL COMMENT '自测记录ID',
  question_id BIGINT UNSIGNED NOT NULL COMMENT '问题ID',
  display_order INT NOT NULL COMMENT '题目展示顺序（1..10）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_assessment_display_order_unique (assessment_id, display_order),
  UNIQUE KEY uk_assessment_question_unique (assessment_id, question_id),
  KEY idx_item_assessment_id (assessment_id),
  CONSTRAINT fk_item_assessment FOREIGN KEY (assessment_id) REFERENCES daily_assessment(id),
  CONSTRAINT fk_item_question FOREIGN KEY (question_id) REFERENCES question(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日自测题目明细（5-10题；提交时写入已作答题目）';

CREATE TABLE IF NOT EXISTS daily_assessment_answer (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  assessment_id BIGINT UNSIGNED NOT NULL COMMENT '自测记录ID',
  assessment_item_id BIGINT UNSIGNED NOT NULL COMMENT '自测题目明细ID',
  option_id BIGINT UNSIGNED NOT NULL COMMENT '选中的选项ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_answer_unique (assessment_item_id, option_id),
  KEY idx_answer_assessment_id (assessment_id),
  CONSTRAINT fk_answer_assessment FOREIGN KEY (assessment_id) REFERENCES daily_assessment(id),
  CONSTRAINT fk_answer_item FOREIGN KEY (assessment_item_id) REFERENCES daily_assessment_item(id),
  CONSTRAINT fk_answer_option FOREIGN KEY (option_id) REFERENCES question_option(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日自测作答（多选为多行）';

CREATE TABLE IF NOT EXISTS daily_assessment_dimension_score (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  assessment_id BIGINT UNSIGNED NOT NULL COMMENT '自测记录ID',
  assessment_answer_id BIGINT UNSIGNED NOT NULL COMMENT '作答记录ID（daily_assessment_answer）',
  dimension_code VARCHAR(64) NOT NULL COMMENT '能力维度编码（枚举常量）',
  score INT NOT NULL COMMENT '该维度得分明细（来自选项映射，正整数）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_answer_dimension_unique (assessment_answer_id, dimension_code),
  KEY idx_dimension_score_assessment_dimension (assessment_id, dimension_code),
  KEY idx_dimension_score_dimension_code (dimension_code),
  CONSTRAINT fk_ds_assessment FOREIGN KEY (assessment_id) REFERENCES daily_assessment(id),
  CONSTRAINT fk_ds_answer FOREIGN KEY (assessment_answer_id) REFERENCES daily_assessment_answer(id),
  CONSTRAINT ck_ds_score CHECK (score > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日自测-维度得分明细（每个作答选项在各维度的分值）';

-- =========
-- AI：自测总结（与 assessment 1:1）
-- =========
CREATE TABLE IF NOT EXISTS ai_assessment_summary (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  assessment_id BIGINT UNSIGNED NOT NULL COMMENT '自测记录ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  content TEXT NOT NULL COMMENT 'AI 总结内容（≤70字，存储留冗余）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_summary_assessment (assessment_id),
  KEY idx_ai_summary_user_id (user_id),
  CONSTRAINT fk_ai_summary_assessment FOREIGN KEY (assessment_id) REFERENCES daily_assessment(id),
  CONSTRAINT fk_ai_summary_user FOREIGN KEY (user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 自测总结（每条自测最多一条）';

-- =========
-- AI：实时对话（会话/消息）
-- =========
CREATE TABLE IF NOT EXISTS ai_chat_session (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  child_id BIGINT UNSIGNED NULL COMMENT '关联孩子ID（可选）',
  title VARCHAR(128) NULL COMMENT '会话标题（可选）',
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE进行中 CLOSED已关闭',
  last_active_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最后活跃时间',
  expires_at DATETIME(3) NULL COMMENT '过期时间（用于会话级上下文保留策略）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_session_user_id (user_id),
  KEY idx_session_last_active_at (last_active_at),
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_session_child FOREIGN KEY (child_id) REFERENCES child(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话会话（会话级上下文）';

CREATE TABLE IF NOT EXISTS ai_chat_message (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_id BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  role VARCHAR(16) NOT NULL COMMENT '角色：user/assistant/system',
  content TEXT NOT NULL COMMENT '消息内容',
  token_usage INT NULL COMMENT 'token 消耗（可选）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_message_session_id (session_id),
  KEY idx_message_user_id (user_id),
  CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES ai_chat_session(id),
  CONSTRAINT fk_message_user FOREIGN KEY (user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话消息';

-- =========
-- AI：会话式 Agent 快捷问题（运营端维护，小程序端展示）
-- =========
CREATE TABLE IF NOT EXISTS ai_agent_quick_question (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  prompt VARCHAR(512) NOT NULL COMMENT '快捷问题内容（同时用于展示与发送给 Agent）',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号（升序）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删除时间',
  PRIMARY KEY (id),
  KEY idx_ai_qq_status_sort (status, sort_no, id),
  CONSTRAINT ck_ai_qq_status CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话式 Agent 快捷问题（运营端配置）';

-- =========
-- 鸡汤语
-- =========
CREATE TABLE IF NOT EXISTS quote (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  content VARCHAR(256) NOT NULL COMMENT '鸡汤语内容',
  scene VARCHAR(16) NOT NULL COMMENT '场景：每日觉察/育儿状态/烦恼档案/育儿日记',
  min_age INT NOT NULL COMMENT '适用最小年龄（整数，单位：岁，含边界）',
  max_age INT NOT NULL COMMENT '适用最大年龄（整数，单位：岁，含边界）',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删除时间',
  PRIMARY KEY (id),
  KEY idx_quote_status (status),
  KEY idx_quote_scene_age (scene, min_age, max_age),
  CONSTRAINT ck_quote_scene CHECK (scene IN ('每日觉察','育儿状态','烦恼档案','育儿日记')),
  CONSTRAINT ck_quote_age_range CHECK (min_age >= 0 AND max_age <= 18 AND min_age <= max_age)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='鸡汤语';

-- =========
-- 订阅：套餐/订单（支付字段按接入方式补齐）
-- =========
CREATE TABLE IF NOT EXISTS subscription_plan (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  name VARCHAR(64) NOT NULL COMMENT '套餐名称',
  days INT NOT NULL COMMENT '订阅天数',
  original_price_cent INT NOT NULL COMMENT '原价（分）',
  price_cent INT NOT NULL COMMENT '现价（分）',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号（升序）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删除时间',
  PRIMARY KEY (id),
  KEY idx_plan_status (status),
  CONSTRAINT ck_plan_days CHECK (days > 0),
  CONSTRAINT ck_plan_original_price CHECK (original_price_cent >= 0),
  CONSTRAINT ck_plan_price CHECK (price_cent >= 0),
  CONSTRAINT ck_plan_original_ge_price CHECK (original_price_cent >= price_cent)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅套餐';

CREATE TABLE IF NOT EXISTS purchase_order (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_no VARCHAR(64) NOT NULL COMMENT '业务订单号（商户侧）',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  plan_id BIGINT UNSIGNED NOT NULL COMMENT '套餐ID',
  amount_cent INT NOT NULL COMMENT '订单金额（分）',
  status VARCHAR(16) NOT NULL DEFAULT 'CREATED' COMMENT '状态：CREATED待支付 PAID已支付 CANCELED已取消',
  pay_channel VARCHAR(32) NULL COMMENT '支付渠道（固定微信可为空或写WECHAT）',
  pay_trade_no VARCHAR(128) NULL COMMENT '微信交易号transaction_id（支付成功后写入）',
  prepay_id VARCHAR(128) NULL COMMENT '微信预支付ID（下单返回）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  paid_at DATETIME(3) NULL COMMENT '支付成功时间（success_time）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_user_id (user_id),
  KEY idx_order_status (status),
  CONSTRAINT ck_order_status CHECK (status IN ('CREATED','PAID','CANCELED')),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_order_plan FOREIGN KEY (plan_id) REFERENCES subscription_plan(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购买订单（不支持退款）';

CREATE TABLE IF NOT EXISTS subscription_grant (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  order_id BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  plan_id BIGINT UNSIGNED NOT NULL COMMENT '套餐ID',
  days_granted INT NOT NULL COMMENT '发放天数',
  granted_from DATETIME(3) NOT NULL COMMENT '生效起始时间（支付成功时间）',
  granted_to DATETIME(3) NOT NULL COMMENT '生效截止时间（含/不含由业务口径统一）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_grant_order_id (order_id),
  KEY idx_grant_user_id (user_id),
  CONSTRAINT fk_grant_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_grant_order FOREIGN KEY (order_id) REFERENCES purchase_order(id),
  CONSTRAINT fk_grant_plan FOREIGN KEY (plan_id) REFERENCES subscription_plan(id),
  CONSTRAINT ck_grant_days CHECK (days_granted > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅发放记录（用于幂等与审计）';

-- =========
-- 微信支付回调事件（幂等与审计）
-- =========
CREATE TABLE IF NOT EXISTS wechat_pay_notify_event (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  event_id VARCHAR(128) NOT NULL COMMENT '微信回调事件ID（用于幂等）',
  event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
  resource_type VARCHAR(64) NULL COMMENT '资源类型',
  summary VARCHAR(256) NULL COMMENT '事件摘要（可选）',
  raw_body MEDIUMTEXT NOT NULL COMMENT '原始回调请求体（审计用，注意访问控制）',
  received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '接收时间',
  processed_at DATETIME(3) NULL COMMENT '处理完成时间',
  process_status VARCHAR(16) NOT NULL DEFAULT 'RECEIVED' COMMENT '处理状态：RECEIVED/PROCESSED/FAILED',
  fail_reason VARCHAR(256) NULL COMMENT '失败原因（可选）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wechat_event_id (event_id),
  KEY idx_wechat_event_status (process_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信支付回调事件流水（幂等与审计）';

-- =========
-- 微信支付交易流水（结构化，用于查询/对账/运营展示）
-- =========
CREATE TABLE IF NOT EXISTS wechat_pay_transaction (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_id BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  order_no VARCHAR(64) NOT NULL COMMENT '业务订单号（冗余便于查询）',
  mch_id VARCHAR(32) NOT NULL COMMENT '商户号',
  appid VARCHAR(32) NOT NULL COMMENT 'AppID（小程序）',
  transaction_id VARCHAR(64) NOT NULL COMMENT '微信支付交易号',
  trade_type VARCHAR(32) NULL COMMENT '交易类型（JSAPI等）',
  trade_state VARCHAR(32) NOT NULL COMMENT '交易状态（如SUCCESS）',
  trade_state_desc VARCHAR(256) NULL COMMENT '交易状态描述（可选）',
  bank_type VARCHAR(32) NULL COMMENT '付款银行（可选）',
  payer_openid VARCHAR(64) NULL COMMENT '支付者openid（可选）',
  total_amount_cent INT NOT NULL COMMENT '支付金额（分）',
  currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  success_time DATETIME(3) NULL COMMENT '支付成功时间（微信回调success_time）',
  latest_event_id VARCHAR(128) NULL COMMENT '最近一次回调事件ID（可选）',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_wpt_transaction_id (transaction_id),
  UNIQUE KEY uk_wpt_order_id (order_id),
  KEY idx_wpt_order_no (order_no),
  KEY idx_wpt_success_time (success_time),
  CONSTRAINT fk_wpt_order FOREIGN KEY (order_id) REFERENCES purchase_order(id),
  CONSTRAINT ck_wpt_amount CHECK (total_amount_cent >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='微信支付交易流水（结构化字段）';

-- =========
-- 运营端：管理员/RBAC
-- =========
CREATE TABLE IF NOT EXISTS admin_user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  username VARCHAR(64) NOT NULL COMMENT '登录用户名',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希（不可逆）',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  deleted_at DATETIME(3) NULL COMMENT '软删除时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营端管理员账号';

CREATE TABLE IF NOT EXISTS admin_role (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  code VARCHAR(64) NOT NULL COMMENT '角色编码（唯一）',
  name VARCHAR(64) NOT NULL COMMENT '角色名称',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营端角色';

CREATE TABLE IF NOT EXISTS admin_permission (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  code VARCHAR(128) NOT NULL COMMENT '权限码（唯一）',
  name VARCHAR(128) NOT NULL COMMENT '权限名称',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运营端权限';

CREATE TABLE IF NOT EXISTS admin_user_role (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  admin_user_id BIGINT UNSIGNED NOT NULL COMMENT '管理员ID',
  role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (admin_user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (admin_user_id) REFERENCES admin_user(id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES admin_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员-角色关联';

CREATE TABLE IF NOT EXISTS admin_role_permission (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  permission_id BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_permission (role_id, permission_id),
  CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES admin_role(id),
  CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES admin_permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联';

-- =========
-- Seed 数据（仅开发/演示）
-- =========
-- 默认超级管理员：账号 admin / 密码 admin
-- ⚠️ 生产环境必须删除或覆盖该账号，并使用强密码与最小权限。
INSERT INTO admin_user (username, password_hash, status, created_at, updated_at, deleted_at)
VALUES ('admin', '$2a$10$OfYXs9DsbFfFfM2NojppBOIs0iZ9DtEwWH2pLcHCkkatk.UkvF12K', 1, NOW(3), NOW(3), NULL)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  status = 1,
  deleted_at = NULL,
  updated_at = NOW(3);

-- 默认超级管理员角色与权限（建议所有环境初始化；权限码用于后续接口级 RBAC 校验）
INSERT INTO admin_role (code, name, created_at, updated_at)
VALUES ('SUPER_ADMIN', '超级管理员', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  updated_at = NOW(3);

INSERT INTO admin_permission (code, name, created_at, updated_at)
VALUES
  ('QUESTION:MANAGE', '题库管理', NOW(3), NOW(3)),
  ('QUESTION:IMPORT', '题库导入', NOW(3), NOW(3)),
  ('USER:READ', '用户查询', NOW(3), NOW(3)),
  ('ORDER:READ', '订单查询', NOW(3), NOW(3)),
  ('ASSESSMENT:READ', '自测查询', NOW(3), NOW(3)),
  ('PLAN:MANAGE', '套餐管理', NOW(3), NOW(3)),
  ('QUOTE:MANAGE', '鸡汤语管理', NOW(3), NOW(3)),
  ('AI_QUICK_QUESTION:MANAGE', '快捷问题管理', NOW(3), NOW(3)),
  ('RBAC:MANAGE', '权限管理', NOW(3), NOW(3))
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  updated_at = NOW(3);

-- 绑定 admin -> SUPER_ADMIN
INSERT IGNORE INTO admin_user_role (admin_user_id, role_id, created_at)
SELECT u.id, r.id, NOW(3)
FROM admin_user u
JOIN admin_role r ON r.code = 'SUPER_ADMIN'
WHERE u.username = 'admin' AND u.deleted_at IS NULL;

-- 绑定 SUPER_ADMIN -> 所有权限
INSERT IGNORE INTO admin_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, NOW(3)
FROM admin_role r
JOIN admin_permission p
  ON p.code IN (
    'QUESTION:MANAGE',
    'QUESTION:IMPORT',
    'USER:READ',
    'ORDER:READ',
    'ASSESSMENT:READ',
    'PLAN:MANAGE',
    'QUOTE:MANAGE',
    'AI_QUICK_QUESTION:MANAGE',
    'RBAC:MANAGE'
  )
WHERE r.code = 'SUPER_ADMIN';
