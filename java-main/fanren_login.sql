/*
 Navicat Premium Dump SQL

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : fanren_login

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 22/10/2026 14:13:02
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_manual
-- ----------------------------
DROP TABLE IF EXISTS `t_manual`;
CREATE TABLE `t_manual`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `grade` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `origin` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `realm` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 92 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_manual
-- ----------------------------
INSERT INTO `t_manual` VALUES (46, '主修功法', '2026-10-12 11:37:12.826715', '七玄门基础功法，韩立初踏仙途所得，进境虽缓，根基却极稳，可延寿培元。', '玄', '长春功', '七玄门', '练气期');
INSERT INTO `t_manual` VALUES (47, '主修功法', '2026-10-12 11:37:12.862718', '青元观镇派剑诀，韩立主修功法，剑气化丝、以剑御敌，至元婴可布剑阵。', '地', '青元剑诀', '青元观', '筑基期');
INSERT INTO `t_manual` VALUES (48, '主修功法', '2026-10-12 11:37:12.865711', '大衍神君所创神识功法，可凝神识化身、参悟傀儡阵法，与青元剑诀相辅。', '地', '大衍诀', '大衍神君', '筑基期');
INSERT INTO `t_manual` VALUES (49, '主修功法', '2026-10-12 11:37:12.866711', '魔道无上真魔功，修成三头六臂法相，力大无穷，韩立后期主修。', '天', '梵圣真魔功', '真魔功', '元婴期');
INSERT INTO `t_manual` VALUES (50, '主修功法', '2026-10-12 11:37:12.868711', '正道雷法总纲，引动天雷攻伐凌厉，破邪驱魔有奇效。', '玄', '五雷正法', '正道', '金丹期');
INSERT INTO `t_manual` VALUES (51, '主修功法', '2026-10-12 11:37:12.869711', '三阳宗火属性功法，进境迅捷，利于火系法术。', '玄', '三阳决', '三阳宗', '筑基期');
INSERT INTO `t_manual` VALUES (52, '主修功法', '2026-10-12 11:37:12.870712', '佛门护法功法，凝金身不坏，外力难伤。', '地', '明王诀', '佛门', '元婴期');
INSERT INTO `t_manual` VALUES (53, '主修功法', '2026-10-12 11:37:12.871711', '阴罗宗魔道双修功法，采补夺元，歹毒非常。', '地', '素女轮回功', '阴罗宗', '元婴期');
INSERT INTO `t_manual` VALUES (54, '主修功法', '2026-10-12 11:37:12.873714', '魔道托天魔功，炼体凝煞，力大无穷。', '玄', '托天魔功', '魔道', '金丹期');
INSERT INTO `t_manual` VALUES (55, '法术', '2026-10-12 11:37:12.876711', '基础控物之术，以灵力摄物移形，入门必学。', '黄', '驱物术', '通用', '练气期');
INSERT INTO `t_manual` VALUES (56, '法术', '2026-10-12 11:37:12.879713', '最常见火系攻击法术，凝火成弹，击中爆裂。', '黄', '火弹术', '通用', '练气期');
INSERT INTO `t_manual` VALUES (57, '法术', '2026-10-12 11:37:12.882710', '雷系小法术，倏忽突袭，扰敌心神。', '黄', '雷闪术', '通用', '练气期');
INSERT INTO `t_manual` VALUES (58, '法术', '2026-10-12 11:37:12.884712', '冰系法术，凝水成针，绵密难防。', '玄', '冰针术', '通用', '练气期');
INSERT INTO `t_manual` VALUES (59, '法术', '2026-10-12 11:37:12.885711', '土系防御法术，凝甲护身，化解寻常攻势。', '黄', '土甲术', '通用', '练气期');
INSERT INTO `t_manual` VALUES (60, '法术', '2026-10-12 11:37:12.886713', '进阶火系法术，化火为蟒，缠绞撕裂。', '玄', '火蟒术', '通用', '筑基期');
INSERT INTO `t_manual` VALUES (61, '法术', '2026-10-12 11:37:12.888711', '轻身御风之术，身形飘忽，闪避迅捷。', '黄', '清风诀', '通用', '练气期');
INSERT INTO `t_manual` VALUES (62, '法术', '2026-10-12 11:37:12.889712', '风系遁术，隐去声息身形，潜行无踪。', '玄', '匿风遁', '通用', '筑基期');
INSERT INTO `t_manual` VALUES (63, '秘术', '2026-10-12 11:37:12.890715', '血道逃命秘术，燃精血极速遁逃，反噬极重。', '玄', '血影遁', '魔道', '筑基期');
INSERT INTO `t_manual` VALUES (64, '秘术', '2026-10-12 11:37:12.893710', '神识攻击秘术，凝念成刺直击神魂，无形难防。', '玄', '惊神刺', '通用', '金丹期');
INSERT INTO `t_manual` VALUES (65, '秘术', '2026-10-12 11:37:12.896716', '魔道血道秘术，可祭炼法宝器灵，凶险异常。', '地', '血炼神光', '魔道', '元婴期');
INSERT INTO `t_manual` VALUES (66, '秘术', '2026-10-12 11:37:12.899712', '魔道凝煞成丹，借煞力增修为，戾气缠身。', '玄', '煞丹术', '魔道', '金丹期');
INSERT INTO `t_manual` VALUES (67, '秘术', '2026-10-12 11:37:12.900711', '将器灵融于己身，人宝合一，威能大涨。', '地', '融灵之术', '通用', '元婴期');
INSERT INTO `t_manual` VALUES (68, '神通', '2026-10-12 11:37:12.901710', '青元剑诀配套剑阵，七十二口飞剑化春黎之景，困杀强敌。', '天', '春黎剑阵', '青元剑诀', '元婴期');
INSERT INTO `t_manual` VALUES (69, '神通', '2026-10-12 11:37:12.903717', '另一配套剑阵，剑蟠盘绕如龙，攻守兼备。', '天', '青蟠剑阵', '青元剑诀', '元婴期');
INSERT INTO `t_manual` VALUES (70, '神通', '2026-10-12 11:37:12.904718', '风系遁法神通，瞬息九变，难锁定难追及。', '天', '疾风九变', '风道', '化神期');
INSERT INTO `t_manual` VALUES (71, '神通', '2026-10-12 11:37:12.906712', '以全身经脉温养本命法宝，宝随人长。', '地', '百脉炼宝诀', '通用', '元婴期');
INSERT INTO `t_manual` VALUES (72, '神通', '2026-10-12 11:37:12.909712', '空间神通，一步千里，瞬息无踪。', '天', '缩地成寸', '空间', '化神期');
INSERT INTO `t_manual` VALUES (73, '神通', '2026-10-12 11:37:12.912712', '梵圣真魔功法相神通，三头六臂各持神兵。', '天', '三头六臂', '梵圣真魔功', '大乘期');
INSERT INTO `t_manual` VALUES (74, '炼丹', '2026-10-12 11:37:12.914710', '筑基必用丹方，引灵气冲关，破境筑基。', '玄', '筑基丹丹方', '炼丹', '练气期');
INSERT INTO `t_manual` VALUES (75, '炼丹', '2026-10-12 11:37:12.916713', '凝练神魂、滋补识海，结丹前后修行必备。', '地', '凝魂丹丹方', '炼丹', '金丹期');
INSERT INTO `t_manual` VALUES (76, '炼丹', '2026-10-12 11:37:12.918711', '大衍神君所传，增识海神识，助参悟大道。', '地', '大衍丹丹方', '大衍神君', '元婴期');
INSERT INTO `t_manual` VALUES (77, '炼丹', '2026-10-12 11:37:12.919713', '以地火灵焰炼体之灵液，淬体坚骨。', '玄', '天火液炼方', '炼丹', '元婴期');
INSERT INTO `t_manual` VALUES (78, '炼丹', '2026-10-12 11:37:12.920711', '疗伤培元之丹，气血双补，行走江湖常备。', '黄', '长春丹方', '炼丹', '练气期');
INSERT INTO `t_manual` VALUES (79, '炼器', '2026-10-12 11:37:12.921710', '炼器入门总纲，识灵材、掌火候、祭器灵。', '玄', '炼器总纲', '炼器', '筑基期');
INSERT INTO `t_manual` VALUES (80, '炼器', '2026-10-12 11:37:12.921710', '韩立本命飞剑炼制之术，七十二剑合一化云。', '地', '青竹蜂云剑炼制术', '韩立', '金丹期');
INSERT INTO `t_manual` VALUES (81, '傀儡', '2026-10-12 11:37:12.922710', '机关傀儡之术，以灵石驱傀儡，代劳御敌、布阵探查。', '玄', '傀儡术总诀', '傀儡', '金丹期');
INSERT INTO `t_manual` VALUES (82, '傀儡', '2026-10-12 11:37:12.924721', '千竹教镇派傀儡绝学，灵石驱动机关傀儡替身御敌，精巧诡变。', '玄', '千竹教傀儡术', '千竹教', '金丹期');
INSERT INTO `t_manual` VALUES (83, '符箓', '2026-10-12 11:37:12.925713', '制符入门，引灵力入符，留法术于纸。', '黄', '符箓基础', '符箓', '练气期');
INSERT INTO `t_manual` VALUES (84, '符箓', '2026-10-12 11:37:12.927713', '雷系符箓，掷出引天雷，一次性重创。', '玄', '雷符术', '符箓', '筑基期');
INSERT INTO `t_manual` VALUES (85, '符箓', '2026-10-12 11:37:12.930713', '金木水火土五系高阶符箓，攻防一体。', '地', '五行灵符', '符箓', '金丹期');
INSERT INTO `t_manual` VALUES (86, '阵法', '2026-10-12 11:37:12.932710', '阵法入门，布迷踪小阵，困敌隐匿。', '黄', '阵法初解', '阵法', '练气期');
INSERT INTO `t_manual` VALUES (87, '阵法', '2026-10-12 11:37:12.934712', '幻术大阵，乱敌五感，困于虚境。', '玄', '幻阵术', '阵法', '筑基期');
INSERT INTO `t_manual` VALUES (88, '阵法', '2026-10-12 11:37:12.935711', '困敌杀阵，引灵气成锁，锁龙困仙。', '地', '困龙阵', '阵法', '金丹期');
INSERT INTO `t_manual` VALUES (89, '神通', '2026-10-12 11:37:12.936712', '借玄天之宝之威，斩灵断魂，无物不破。', '天', '玄天斩灵术', '玄天', '炼虚期');
INSERT INTO `t_manual` VALUES (90, '炼器', '2026-10-12 11:37:12.937712', '于虚空中祭炼法宝，纳空间之威。', '天', '虚空炼器法', '炼器', '合体期');
INSERT INTO `t_manual` VALUES (91, '主修功法', '2026-10-12 11:37:12.938710', '渡劫飞升之秘要，引雷劫淬身，九死一生。', '天', '渡劫秘要', '飞升', '渡劫期');

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKjhib4legehrm4yscx9t3lirqi`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` VALUES (1, '2026-10-09 13:50:45.734471', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '123456');
INSERT INTO `t_user` VALUES (2, '2026-10-09 13:59:09.624941', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '1234567');

SET FOREIGN_KEY_CHECKS = 1;
