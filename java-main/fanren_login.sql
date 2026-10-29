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

 Date: 29/10/2026 15:09:37
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
  `image` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 94 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_manual
-- ----------------------------
INSERT INTO `t_manual` VALUES (46, '主修功法', '2026-10-12 11:37:12.826715', '七玄门基础功法，韩立初踏仙途所得，进境虽缓，根基却极稳，可延寿培元。', '玄', '长春功', '七玄门', '练气期', '/images/manuals/m01.jpg');
INSERT INTO `t_manual` VALUES (47, '主修功法', '2026-10-12 11:37:12.862718', '青元观镇派剑诀，韩立主修功法，剑气化丝、以剑御敌，至元婴可布剑阵。', '地', '青元剑诀', '青元观', '筑基期', '/images/manuals/m02.jpg');
INSERT INTO `t_manual` VALUES (48, '主修功法', '2026-10-12 11:37:12.865711', '大衍神君所创神识功法，可凝神识化身、参悟傀儡阵法，与青元剑诀相辅。', '地', '大衍诀', '大衍神君', '筑基期', '/images/manuals/m03.jpg');
INSERT INTO `t_manual` VALUES (49, '主修功法', '2026-10-12 11:37:12.866711', '魔道无上真魔功，修成三头六臂法相，力大无穷，韩立后期主修。', '天', '梵圣真魔功xxxxx', '真魔功', '元婴期', '/images/manuals/m04.jpg');
INSERT INTO `t_manual` VALUES (50, '主修功法', '2026-10-12 11:37:12.868711', '正道雷法总纲，引动天雷攻伐凌厉，破邪驱魔有奇效。', '玄', '五雷正法', '正道', '金丹期', '/images/manuals/m05.jpg');
INSERT INTO `t_manual` VALUES (51, '主修功法', '2026-10-12 11:37:12.869711', '三阳宗火属性功法，进境迅捷，利于火系法术。', '玄', '三阳决', '三阳宗', '筑基期', '/images/manuals/m06.jpg');
INSERT INTO `t_manual` VALUES (52, '主修功法', '2026-10-12 11:37:12.870712', '佛门护法功法，凝金身不坏，外力难伤。', '地', '明王诀', '佛门', '元婴期', '/images/manuals/m07.jpg');
INSERT INTO `t_manual` VALUES (53, '主修功法', '2026-10-12 11:37:12.871711', '阴罗宗魔道双修功法，采补夺元，歹毒非常。', '地', '素女轮回功', '阴罗宗', '元婴期', '/images/manuals/m08.jpg');
INSERT INTO `t_manual` VALUES (54, '主修功法', '2026-10-12 11:37:12.873714', '魔道托天魔功，炼体凝煞，力大无穷。', '玄', '托天魔功', '魔道', '金丹期', '/images/manuals/m09.jpg');
INSERT INTO `t_manual` VALUES (55, '法术', '2026-10-12 11:37:12.876711', '基础控物之术，以灵力摄物移形，入门必学。', '黄', '驱物术', '通用', '练气期', '/images/manuals/m10.jpg');
INSERT INTO `t_manual` VALUES (56, '法术', '2026-10-12 11:37:12.879713', '最常见火系攻击法术，凝火成弹，击中爆裂。', '黄', '火弹术', '通用', '练气期', '/images/manuals/m11.jpg');
INSERT INTO `t_manual` VALUES (57, '法术', '2026-10-12 11:37:12.882710', '雷系小法术，倏忽突袭，扰敌心神。', '黄', '雷闪术', '通用', '练气期', '/images/manuals/m12.jpg');
INSERT INTO `t_manual` VALUES (58, '法术', '2026-10-12 11:37:12.884712', '冰系法术，凝水成针，绵密难防。', '玄', '冰针术', '通用', '练气期', '/images/manuals/m13.jpg');
INSERT INTO `t_manual` VALUES (59, '法术', '2026-10-12 11:37:12.885711', '土系防御法术，凝甲护身，化解寻常攻势。', '黄', '土甲术', '通用', '练气期', '/images/manuals/m14.jpg');
INSERT INTO `t_manual` VALUES (60, '法术', '2026-10-12 11:37:12.886713', '进阶火系法术，化火为蟒，缠绞撕裂。', '玄', '火蟒术', '通用', '筑基期', '/images/manuals/m15.jpg');
INSERT INTO `t_manual` VALUES (61, '法术', '2026-10-12 11:37:12.888711', '轻身御风之术，身形飘忽，闪避迅捷。', '黄', '清风诀', '通用', '练气期', '/images/manuals/m16.jpg');
INSERT INTO `t_manual` VALUES (62, '法术', '2026-10-12 11:37:12.889712', '风系遁术，隐去声息身形，潜行无踪。', '玄', '匿风遁', '通用', '筑基期', '/images/manuals/m17.jpg');
INSERT INTO `t_manual` VALUES (63, '秘术', '2026-10-12 11:37:12.890715', '血道逃命秘术，燃精血极速遁逃，反噬极重。', '玄', '血影遁', '魔道', '筑基期', '/images/manuals/m18.jpg');
INSERT INTO `t_manual` VALUES (64, '秘术', '2026-10-12 11:37:12.893710', '神识攻击秘术，凝念成刺直击神魂，无形难防。', '玄', '惊神刺', '通用', '金丹期', '/images/manuals/m19.jpg');
INSERT INTO `t_manual` VALUES (65, '秘术', '2026-10-12 11:37:12.896716', '魔道血道秘术，可祭炼法宝器灵，凶险异常。', '地', '血炼神光', '魔道', '元婴期', '/images/manuals/m20.jpg');
INSERT INTO `t_manual` VALUES (66, '秘术', '2026-10-12 11:37:12.899712', '魔道凝煞成丹，借煞力增修为，戾气缠身。', '玄', '煞丹术', '魔道', '金丹期', '/images/manuals/m21.jpg');
INSERT INTO `t_manual` VALUES (67, '秘术', '2026-10-12 11:37:12.900711', '将器灵融于己身，人宝合一，威能大涨。', '地', '融灵之术x', '通用', '元婴期', '/images/manuals/m22.jpg');
INSERT INTO `t_manual` VALUES (68, '神通', '2026-10-12 11:37:12.901710', '青元剑诀配套剑阵，七十二口飞剑化春黎之景，困杀强敌。', '天', '春黎剑阵', '青元剑诀', '元婴期', '/images/manuals/m23.jpg');
INSERT INTO `t_manual` VALUES (69, '神通', '2026-10-12 11:37:12.903717', '另一配套剑阵，剑蟠盘绕如龙，攻守兼备。', '天', '青蟠剑阵', '青元剑诀', '元婴期', '/images/manuals/m24.jpg');
INSERT INTO `t_manual` VALUES (70, '神通', '2026-10-12 11:37:12.904718', '风系遁法神通，瞬息九变，难锁定难追及。', '天', '疾风九变', '风道', '化神期', '/images/manuals/m25.jpg');
INSERT INTO `t_manual` VALUES (71, '神通', '2026-10-12 11:37:12.906712', '以全身经脉温养本命法宝，宝随人长。', '地', '百脉炼宝诀', '通用', '元婴期', '/images/manuals/m26.jpg');
INSERT INTO `t_manual` VALUES (72, '神通', '2026-10-12 11:37:12.909712', '空间神通，一步千里，瞬息无踪。', '天', '缩地成寸', '空间', '化神期', '/images/manuals/m27.jpg');
INSERT INTO `t_manual` VALUES (73, '神通', '2026-10-12 11:37:12.912712', '梵圣真魔功法相神通，三头六臂各持神兵。', '天', '三头六臂', '梵圣真魔功', '大乘期', '/images/manuals/m28.jpg');
INSERT INTO `t_manual` VALUES (74, '炼丹', '2026-10-12 11:37:12.914710', '筑基必用丹方，引灵气冲关，破境筑基。', '玄', '筑基丹丹方', '炼丹', '练气期', '/images/manuals/m29.jpg');
INSERT INTO `t_manual` VALUES (75, '炼丹', '2026-10-12 11:37:12.916713', '凝练神魂、滋补识海，结丹前后修行必备。', '地', '凝魂丹丹方', '炼丹', '金丹期', '/images/manuals/m30.jpg');
INSERT INTO `t_manual` VALUES (76, '炼丹', '2026-10-12 11:37:12.918711', '大衍神君所传，增识海神识，助参悟大道。', '地', '大衍丹丹方', '大衍神君', '元婴期', '/images/manuals/m31.jpg');
INSERT INTO `t_manual` VALUES (77, '炼丹', '2026-10-12 11:37:12.919713', '以地火灵焰炼体之灵液，淬体坚骨。', '玄', '天火液炼方', '炼丹', '元婴期', '/images/manuals/m32.jpg');
INSERT INTO `t_manual` VALUES (78, '炼丹', '2026-10-12 11:37:12.920711', '疗伤培元之丹，气血双补，行走江湖常备。', '黄', '长春丹方', '炼丹', '练气期', '/images/manuals/m33.jpg');
INSERT INTO `t_manual` VALUES (79, '炼器', '2026-10-12 11:37:12.921710', '炼器入门总纲，识灵材、掌火候、祭器灵。', '玄', '炼器总纲', '炼器', '筑基期', '/images/manuals/m34.jpg');
INSERT INTO `t_manual` VALUES (80, '炼器', '2026-10-12 11:37:12.921710', '韩立本命飞剑炼制之术，七十二剑合一化云。', '地', '青竹蜂云剑炼制术', '韩立', '金丹期', '/images/manuals/m35.jpg');
INSERT INTO `t_manual` VALUES (81, '傀儡', '2026-10-12 11:37:12.922710', '机关傀儡之术，以灵石驱傀儡，代劳御敌、布阵探查。', '玄', '傀儡术总诀', '傀儡', '金丹期', '/images/manuals/m36.jpg');
INSERT INTO `t_manual` VALUES (82, '傀儡', '2026-10-12 11:37:12.924721', '千竹教镇派傀儡绝学，灵石驱动机关傀儡替身御敌，精巧诡变。', '玄', '千竹教傀儡术', '千竹教', '金丹期', '/images/manuals/m37.jpg');
INSERT INTO `t_manual` VALUES (83, '符箓', '2026-10-12 11:37:12.925713', '制符入门，引灵力入符，留法术于纸。', '黄', '符箓基础', '符箓', '练气期', '/images/manuals/m38.jpg');
INSERT INTO `t_manual` VALUES (84, '符箓', '2026-10-12 11:37:12.927713', '雷系符箓，掷出引天雷，一次性重创。', '玄', '雷符术', '符箓', '筑基期', '/images/manuals/m39.jpg');
INSERT INTO `t_manual` VALUES (85, '符箓', '2026-10-12 11:37:12.930713', '金木水火土五系高阶符箓，攻防一体。', '地', '五行灵符', '符箓', '金丹期', '/images/manuals/m40.jpg');
INSERT INTO `t_manual` VALUES (86, '阵法', '2026-10-12 11:37:12.932710', '阵法入门，布迷踪小阵，困敌隐匿。', '黄', '阵法初解', '阵法', '练气期', '/images/manuals/m41.jpg');
INSERT INTO `t_manual` VALUES (87, '阵法', '2026-10-12 11:37:12.934712', '幻术大阵，乱敌五感，困于虚境。', '玄', '幻阵术', '阵法', '筑基期', '/images/manuals/m42.jpg');
INSERT INTO `t_manual` VALUES (88, '阵法', '2026-10-12 11:37:12.935711', '困敌杀阵，引灵气成锁，锁龙困仙。', '地', '困龙阵', '阵法', '金丹期', '/images/manuals/m43.jpg');
INSERT INTO `t_manual` VALUES (89, '神通', '2026-10-12 11:37:12.936712', '借玄天之宝之威，斩灵断魂，无物不破。', '天', '玄天斩灵术', '玄天', '炼虚期', '/images/manuals/m44.jpg');
INSERT INTO `t_manual` VALUES (90, '炼器', '2026-10-12 11:37:12.937712', '于虚空中祭炼法宝，纳空间之威。', '天', '虚空炼器法', '炼器', '合体期', '/images/manuals/m45.jpg');
INSERT INTO `t_manual` VALUES (91, '主修功法', '2026-10-12 11:37:12.938710', '渡劫飞升之秘要，引雷劫淬身，九死一生。', '天', '渡劫秘要', '飞升', '渡劫期', '/images/manuals/m46.jpg');

-- ----------------------------
-- Table structure for t_treasure
-- ----------------------------
DROP TABLE IF EXISTS `t_treasure`;
CREATE TABLE `t_treasure`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `description` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `grade` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `image` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `origin` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `realm` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_treasure
-- ----------------------------
INSERT INTO `t_treasure` VALUES (1, '飞剑', '2026-10-23 16:23:28.734698', '韩立本命飞剑，七十二口青竹小剑合一化云，剑随心动，群攻无双。', '地', '/images/manuals/m35.jpg', '青竹蜂云剑', '韩立', '金丹期');
INSERT INTO `t_treasure` VALUES (2, '遁法', '2026-10-23 16:23:28.786705', '以风雷之力凝成双翅，振翅瞬息千里，遁速冠绝同阶。', '天', '/images/manuals/m25.jpg', '风雷翅', '韩立', '元婴期');
INSERT INTO `t_treasure` VALUES (3, '飞剑', '2026-10-23 16:23:28.790699', '玄天之宝所化剑器，斩灵断魂，无物不破，魔道至宝。', '天', '/images/manuals/m44.jpg', '玄天斩灵剑', '玄天', '炼虚期');
INSERT INTO `t_treasure` VALUES (4, '奇物', '2026-10-23 16:23:28.796700', '纳灵聚宝，置入灵石可缓慢滋生利息，散修攒家底之宝。', '玄', '/images/manuals/m33.jpg', '聚宝盆', '散修', '练气期');
INSERT INTO `t_treasure` VALUES (5, '奇物', '2026-10-23 16:23:28.799698', '虚天殿秘宝，内蕴须弥空间，曾封印古魔，威能莫测。', '天', '/images/manuals/m27.jpg', '虚天鼎', '虚天殿', '化神期');
INSERT INTO `t_treasure` VALUES (6, '攻伐', '2026-10-23 16:23:28.801699', '极寒本源灵焰，触物成冰，焚敌于无形。', '地', '/images/manuals/m13.jpg', '乾蓝冰焰', '极寒之地', '元婴期');
INSERT INTO `t_treasure` VALUES (7, '攻伐', '2026-10-23 16:23:28.803700', '引动风雷的阵旗，布下可困敌削肉，声势骇人。', '玄', '/images/manuals/m41.jpg', '风雷旗', '阵法', '金丹期');
INSERT INTO `t_treasure` VALUES (8, '护身', '2026-10-23 16:23:28.804698', '上古大能圭灵所遗护身盾，水火不侵，硬抗法宝。', '地', '/images/manuals/m14.jpg', '圭灵盾', '圭灵', '元婴期');
INSERT INTO `t_treasure` VALUES (9, '攻伐', '2026-10-23 16:23:28.807701', '融七种灵焰于一扇，扇出焚天之火，威能惊世。', '天', '/images/manuals/m15.jpg', '七焰扇', '火道', '化神期');
INSERT INTO `t_treasure` VALUES (10, '飞剑', '2026-10-23 16:23:28.813698', '以墨蛟之骨炼制的飞剑，剑出带腥风，凌厉非常。', '玄', '/images/manuals/m34.jpg', '墨蛟剑', '炼器', '筑基期');
INSERT INTO `t_treasure` VALUES (11, '奇物', '2026-10-23 16:23:28.816700', '金木水火土五环相生，可攻可守，变化无穷。', '地', '/images/manuals/m05.jpg', '五行环', '五行', '金丹期');
INSERT INTO `t_treasure` VALUES (12, '护身', '2026-10-23 16:23:28.818705', '天罗秘银织就的真甲，灵力流转间化解诸般攻伐。', '天', '/images/manuals/m07.jpg', '天罗真甲', '天罗', '炼虚期');
INSERT INTO `t_treasure` VALUES (13, '攻伐', '2026-10-23 16:23:28.819699', '封印雷力于宝珠，掷出引动天雷，一次性重创强敌。', '玄', '/images/manuals/m12.jpg', '雷珠', '雷道', '筑基期');
INSERT INTO `t_treasure` VALUES (14, '遁法', '2026-10-23 16:23:28.821698', '卷起黑风护身遁逃，价廉物美，江湖常备。', '黄', '/images/manuals/m10.jpg', '黑风旗', '散修', '练气期');
INSERT INTO `t_treasure` VALUES (15, '储物', '2026-10-23 16:23:28.824700', '须弥芥子之术，方寸之间藏百物，出行必备。', '黄', '/images/manuals/m36.jpg', '纳物袋', '通用', '练气期');
INSERT INTO `t_treasure` VALUES (16, '奇物', '2026-10-23 16:23:28.830698', '上古龙族传承之玺，镇运纳福，亦可引动龙威压敌。', '天', '/images/manuals/m28.jpg', '化龙玺', '龙族', '合体期');

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `permission` int NULL DEFAULT 1,
  `age` int NULL DEFAULT NULL,
  `avatar` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  `gender` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `initial_gongfa` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `initial_treasure` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `physique` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `realm` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `spirit_root` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKjhib4legehrm4yscx9t3lirqi`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` VALUES (1, '2026-10-09 13:50:45.734471', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '123456', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_user` VALUES (2, '2026-10-09 13:59:09.624941', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '1234567', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_user` VALUES (3, '2026-10-26 09:21:42.563155', '8bb0cf6eb9b17d0f7d22b456f121257dc1254e1f01665370476383ea776df414', '123', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_user` VALUES (4, '2026-10-26 09:26:43.272581', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'xyy', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_user` VALUES (5, '2026-10-26 09:27:58.711622', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'ljq', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_user` VALUES (6, '2026-10-26 09:28:27.395646', '5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5', '1234', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_user` VALUES (7, '2026-10-26 09:28:39.451608', '5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5', '1', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_user` VALUES (8, '2026-10-26 09:31:52.923709', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 'xx', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_user` VALUES (9, '2026-10-26 09:33:55.903299', 'admin', 'admin', 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_user` VALUES (10, '2026-10-26 09:35:14.149937', '12345', 'xxs', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_user` VALUES (11, '2026-10-26 09:35:44.909584', '123456', '朱银智', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `t_user` VALUES (12, '2026-10-26 11:30:29.776444', '12345', '太上皇', 1, 77, 'avatar:female-1', '女', '大衍诀', '青竹蜂云剑', '凡人之躯', '练气一层', '风灵根·变异');
INSERT INTO `t_user` VALUES (13, '2026-10-26 11:30:34.792339', '123456', 'aaa', 1, 99, 'avatar:male-1', '男', '长春功', '青竹蜂云剑', '凡人之躯', '练气一层', '木灵根');
INSERT INTO `t_user` VALUES (14, '2026-10-26 11:31:03.624460', '123456', 'hhhh', 1, 56, 'avatar:male-2', '男', '清风诀', '储物袋', '凡人之躯', '练气一层', '风灵根·变异');
INSERT INTO `t_user` VALUES (15, '2026-10-26 11:33:20.922539', '12345678', '无敌', 1, 10, 'data:image/jpeg;base64,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', '男', '长春功', '青竹蜂云剑', '凡人之躯', '练气一层', '木灵根');
INSERT INTO `t_user` VALUES (16, '2026-10-26 11:35:15.419132', '123456', 'xxxxx', 1, 19, 'avatar:female-3', '女', '长春功', '青竹蜂云剑', '凡人之躯', '练气一层', '冰灵根·变异');

SET FOREIGN_KEY_CHECKS = 1;
