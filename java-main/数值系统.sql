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

-- =============================================================
-- 说明：物品的具体五维数值由后端 ItemStatSeeder 在应用启动时
--       按「物品名称」自动回填，无需手动 UPDATE。
--       物品售价（灵石）由后端 PriceSeeder 在应用启动时按名称回填。
--       如需为某件物品单独定制数值/价格，可直接改 ItemStatSeeder
--       中的 MANUAL_STATS / TREASURE_STATS 映射表，或 PriceSeeder。
-- =============================================================
