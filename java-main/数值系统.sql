-- =============================================================
-- 《凡人修仙传》数值系统 数据库变更脚本
-- 适用库：fanren_login
-- 说明：为角色修为、功法、法宝补充数值系统所需字段。
--       Hibernate ddl-auto=update 也会自动建列，本脚本用于手动/幂等执行。
--       MySQL 8.0+ 支持 ADD COLUMN IF NOT EXISTS；低版本可去掉该子句。
-- 执行：mysql -uroot -p123456 fanren_login < 数值系统.sql
-- =============================================================

-- ---------- 1) t_user：修为总层数（1-81） ----------
ALTER TABLE `t_user`
  ADD COLUMN IF NOT EXISTS `total_level` int NULL DEFAULT 1 COMMENT '修为总层数1-81' AFTER `realm`;

-- 已有账号修为总层数兜底为 1（练气初期一层）
UPDATE `t_user` SET `total_level` = 1 WHERE `total_level` IS NULL;

-- ---------- 1.1) t_user：灵石余额（经济系统） ----------
ALTER TABLE `t_user`
  ADD COLUMN IF NOT EXISTS `spirit_stone` int NULL DEFAULT 1000 COMMENT '灵石余额' AFTER `total_level`;

-- 已有账号灵石兜底为 1000
UPDATE `t_user` SET `spirit_stone` = 1000 WHERE `spirit_stone` IS NULL;

-- ---------- 1.2) t_user：灵石获取冷却时间戳 ----------
ALTER TABLE `t_user`
  ADD COLUMN IF NOT EXISTS `last_meditate_at` datetime NULL DEFAULT NULL COMMENT '打坐修炼最后获取时间' AFTER `spirit_stone`,
  ADD COLUMN IF NOT EXISTS `last_hunt_at`     datetime NULL DEFAULT NULL COMMENT '斩妖除魔最后获取时间' AFTER `last_meditate_at`;

-- ---------- 1.3) t_user：剩余寿元（持久化字段，五维寿元与顶部剩余寿元均读此字段） ----------
ALTER TABLE `t_user`
  ADD COLUMN IF NOT EXISTS `lifespan` int NULL DEFAULT 100 COMMENT '剩余寿元（年，持久化）' AFTER `age_used`;

-- 已有账号 lifespan 初始化（按当前境界寿元上限）
UPDATE `t_user` SET `lifespan` = 100  WHERE `lifespan` IS NULL AND `total_level` <= 9;
UPDATE `t_user` SET `lifespan` = 200  WHERE `lifespan` IS NULL AND `total_level` BETWEEN 10 AND 18;
UPDATE `t_user` SET `lifespan` = 300  WHERE `lifespan` IS NULL AND `total_level` BETWEEN 19 AND 27;
UPDATE `t_user` SET `lifespan` = 500  WHERE `lifespan` IS NULL AND `total_level` BETWEEN 28 AND 36;
UPDATE `t_user` SET `lifespan` = 800  WHERE `lifespan` IS NULL AND `total_level` BETWEEN 37 AND 45;
UPDATE `t_user` SET `lifespan` = 1200 WHERE `lifespan` IS NULL AND `total_level` BETWEEN 46 AND 54;
UPDATE `t_user` SET `lifespan` = 2000 WHERE `lifespan` IS NULL AND `total_level` BETWEEN 55 AND 63;
UPDATE `t_user` SET `lifespan` = 4000 WHERE `lifespan` IS NULL AND `total_level` BETWEEN 64 AND 72;
UPDATE `t_user` SET `lifespan` = 10000 WHERE `lifespan` IS NULL AND `total_level` BETWEEN 73 AND 81;

-- ---------- 2) t_manual：功法属性加成（可空，空则按品阶×境界自动计算） ----------
ALTER TABLE `t_manual`
  ADD COLUMN IF NOT EXISTS `bonus_hp`       int NULL DEFAULT NULL COMMENT '生命加成' AFTER `image`,
  ADD COLUMN IF NOT EXISTS `bonus_lifespan` int NULL DEFAULT NULL COMMENT '寿元加成' AFTER `bonus_hp`,
  ADD COLUMN IF NOT EXISTS `bonus_atk`      int NULL DEFAULT NULL COMMENT '攻击加成' AFTER `bonus_lifespan`,
  ADD COLUMN IF NOT EXISTS `bonus_def`      int NULL DEFAULT NULL COMMENT '防御加成' AFTER `bonus_atk`,
  ADD COLUMN IF NOT EXISTS `bonus_spd`      int NULL DEFAULT NULL COMMENT '速度加成' AFTER `bonus_def`,
  ADD COLUMN IF NOT EXISTS `price`          int NULL DEFAULT NULL COMMENT '售价（灵石）' AFTER `bonus_spd`;

-- ---------- 3) t_treasure：法宝属性加成（可空，空则按品阶×境界自动计算） ----------
ALTER TABLE `t_treasure`
  ADD COLUMN IF NOT EXISTS `bonus_hp`       int NULL DEFAULT NULL COMMENT '生命加成' AFTER `realm`,
  ADD COLUMN IF NOT EXISTS `bonus_lifespan` int NULL DEFAULT NULL COMMENT '寿元加成' AFTER `bonus_hp`,
  ADD COLUMN IF NOT EXISTS `bonus_atk`      int NULL DEFAULT NULL COMMENT '攻击加成' AFTER `bonus_lifespan`,
  ADD COLUMN IF NOT EXISTS `bonus_def`      int NULL DEFAULT NULL COMMENT '防御加成' AFTER `bonus_atk`,
  ADD COLUMN IF NOT EXISTS `bonus_spd`      int NULL DEFAULT NULL COMMENT '速度加成' AFTER `bonus_def`,
  ADD COLUMN IF NOT EXISTS `price`          int NULL DEFAULT NULL COMMENT '售价（灵石）' AFTER `bonus_spd`;

-- ---------- 4) 修正历史脏数据：功法名称规范化 ----------
-- 旧库存在「梵圣真魔功xxxxx」等带后缀的名称，导致 ItemStatSeeder 按名匹配失败，统一修正。
UPDATE `t_manual` SET `name` = '梵圣真魔功' WHERE `name` = '梵圣真魔功xxxxx';

-- ---------- 5) t_user：门派系统字段 ----------
-- 所属门派名（对应 t_sect.name，null 表示未加入）
ALTER TABLE `t_user` ADD COLUMN `sect` varchar(64) NULL COMMENT '所属门派名' AFTER `spirit_qi`;
-- 弟子级别（外门弟子/内门弟子/真传弟子，按修为境界自动判定）
ALTER TABLE `t_user` ADD COLUMN `sect_rank` varchar(16) NULL COMMENT '弟子级别' AFTER `sect`;
-- 最后领取月俸时间（30 天冷却用）
ALTER TABLE `t_user` ADD COLUMN `last_salary_at` datetime(6) NULL COMMENT '最后领取月俸时间' AFTER `sect_rank`;

-- ---------- 6) t_sect：门派谱表 ----------
CREATE TABLE IF NOT EXISTS `t_sect` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '门派名称',
  `camp` varchar(16) NOT NULL COMMENT '阵营：正道/魔道/中立/散修',
  `region` varchar(64) NULL COMMENT '所属地域',
  `description` varchar(512) NULL COMMENT '门派简介',
  `signature` varchar(128) NULL COMMENT '镇派功法/招牌绝学',
  `representative` varchar(128) NULL COMMENT '代表人物',
  `image` varchar(128) NULL COMMENT '插画路径',
  `created_at` datetime(6) NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 门派数据（幂等：仅当表为空时灌入）
INSERT INTO `t_sect` (`name`, `camp`, `region`, `description`, `signature`, `representative`, `created_at`)
SELECT * FROM (
  SELECT '七玄门' n,'正道' c,'越国' r,'越国七大修仙门派之一，门规森严，以剑修为长，韩立早年的宗门。' d,'七绝剑气' sg,'黄枫谷诸修' rp, NOW() ct
  UNION ALL SELECT '黄枫谷','正道','越国','越国知名正道宗门，谷中多灵植灵药，善炼丹制符，门风平和。','长春功','韩立', NOW()
  UNION ALL SELECT '掩月宗','魔道','越国','越国魔道巨擘，行事诡秘，善驱鬼御魔，门徒众多。','掩月魔功','掩月老祖', NOW()
  UNION ALL SELECT '巨剑门','正道','越国','越国以巨剑立派的正道宗门，力大势沉，门下多炼体剑修。','巨剑诀','巨剑门主', NOW()
  UNION ALL SELECT '天阙堡','正道','越国','越国正道堡垒，据守一方，堡中修士擅防御布阵。','天阙阵','天阙堡主', NOW()
  UNION ALL SELECT '落云宗','正道','天南','天南正道大宗，云霞缭绕，宗内剑修与法修并重。','落云剑经','南宫婉', NOW()
  UNION ALL SELECT '合欢宗','魔道','天南','天南魔道双修宗门，男女双修采补，行事放浪不羁。','合欢心法','合欢宗主', NOW()
  UNION ALL SELECT '极阴岛','魔道','天南','天南魔道势力，盘踞海外仙岛，阴气森森，善驭阴煞。','极阴煞功','极阴祖师', NOW()
  UNION ALL SELECT '星宫','中立','海外','海外散修汇聚之地，与魔道分庭抗礼，宫主神通广大。','星宿大法','星宫宫主', NOW()
  UNION ALL SELECT '魔道联盟','魔道','大晋','大晋魔道诸派的联合，势力庞大，行事无忌。','真魔功','古魔', NOW()
  UNION ALL SELECT '灵界宗门','中立','灵界','飞升灵界后诸仙门统称，传承古老，境界深不可测。','诸般神通','灵界大能', NOW()
  UNION ALL SELECT '散修','散修','各地','无门无派的修仙者，凭机缘与苦修自成一脉，如韩立早年。','各凭机缘','韩立', NOW()
) t WHERE NOT EXISTS (SELECT 1 FROM `t_sect`);

-- =============================================================
-- 说明：物品的具体五维数值由后端 ItemStatSeeder 在应用启动时
--       按「物品名称」自动回填，无需手动 UPDATE。
--       物品售价（灵石）由后端 PriceSeeder 在应用启动时按名称回填。
--       如需为某件物品单独定制数值/价格，可直接改 ItemStatSeeder
--       中的 MANUAL_STATS / TREASURE_STATS 映射表，或 PriceSeeder。
-- =============================================================
