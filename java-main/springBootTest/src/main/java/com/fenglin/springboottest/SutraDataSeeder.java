package com.fenglin.springboottest;

import com.fenglin.springboottest.entity.Manual;
import com.fenglin.springboottest.repository.ManualRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 藏经阁数据灌入：应用启动时若 t_manual 为空，则录入《凡人修仙传》功法秘籍。
 * 仅灌一次（以表内是否有数据判定），便于后续在库中增删而不被覆盖。
 * image 为后端静态资源路径（static/images/manuals/），历史数据用 SQL 按名称回填。
 * @Order(10)：先于 ItemStatSeeder(20) 灌入数据，保证数值回填时物品已存在。
 */
@Component
@Order(10)
public class SutraDataSeeder implements CommandLineRunner {

    private final ManualRepository manualRepository;

    public SutraDataSeeder(ManualRepository manualRepository) {
        this.manualRepository = manualRepository;
    }

    @Override
    public void run(String... args) {
        if (manualRepository.count() > 0) {
            return; // 已录入，跳过
        }
        List<Manual> seed = List.of(
                mk("长春功", "主修功法", "玄", "练气期", "七玄门基础功法，韩立初踏仙途所得，进境虽缓，根基却极稳，可延寿培元。", "七玄门", "/images/manuals/m01.jpg"),
                mk("青元剑诀", "主修功法", "地", "筑基期", "青元观镇派剑诀，韩立主修功法，剑气化丝、以剑御敌，至元婴可布剑阵。", "青元观", "/images/manuals/m02.jpg"),
                mk("大衍诀", "主修功法", "地", "筑基期", "大衍神君所创神识功法，可凝神识化身、参悟傀儡阵法，与青元剑诀相辅。", "大衍神君", "/images/manuals/m03.jpg"),
                mk("梵圣真魔功", "主修功法", "天", "元婴期", "魔道无上真魔功，修成三头六臂法相，力大无穷，韩立后期主修。", "真魔功", "/images/manuals/m04.jpg"),
                mk("五雷正法", "主修功法", "玄", "金丹期", "正道雷法总纲，引动天雷攻伐凌厉，破邪驱魔有奇效。", "正道", "/images/manuals/m05.jpg"),
                mk("三阳决", "主修功法", "玄", "筑基期", "三阳宗火属性功法，进境迅捷，利于火系法术。", "三阳宗", "/images/manuals/m06.jpg"),
                mk("明王诀", "主修功法", "地", "元婴期", "佛门护法功法，凝金身不坏，外力难伤。", "佛门", "/images/manuals/m07.jpg"),
                mk("素女轮回功", "主修功法", "地", "元婴期", "阴罗宗魔道双修功法，采补夺元，歹毒非常。", "阴罗宗", "/images/manuals/m08.jpg"),
                mk("托天魔功", "主修功法", "玄", "金丹期", "魔道托天魔功，炼体凝煞，力大无穷。", "魔道", "/images/manuals/m09.jpg"),
                mk("驱物术", "法术", "黄", "练气期", "基础控物之术，以灵力摄物移形，入门必学。", "通用", "/images/manuals/m10.jpg"),
                mk("火弹术", "法术", "黄", "练气期", "最常见火系攻击法术，凝火成弹，击中爆裂。", "通用", "/images/manuals/m11.jpg"),
                mk("雷闪术", "法术", "黄", "练气期", "雷系小法术，倏忽突袭，扰敌心神。", "通用", "/images/manuals/m12.jpg"),
                mk("冰针术", "法术", "玄", "练气期", "冰系法术，凝水成针，绵密难防。", "通用", "/images/manuals/m13.jpg"),
                mk("土甲术", "法术", "黄", "练气期", "土系防御法术，凝甲护身，化解寻常攻势。", "通用", "/images/manuals/m14.jpg"),
                mk("火蟒术", "法术", "玄", "筑基期", "进阶火系法术，化火为蟒，缠绞撕裂。", "通用", "/images/manuals/m15.jpg"),
                mk("清风诀", "法术", "黄", "练气期", "轻身御风之术，身形飘忽，闪避迅捷。", "通用", "/images/manuals/m16.jpg"),
                mk("匿风遁", "法术", "玄", "筑基期", "风系遁术，隐去声息身形，潜行无踪。", "通用", "/images/manuals/m17.jpg"),
                mk("血影遁", "秘术", "玄", "筑基期", "血道逃命秘术，燃精血极速遁逃，反噬极重。", "魔道", "/images/manuals/m18.jpg"),
                mk("惊神刺", "秘术", "玄", "金丹期", "神识攻击秘术，凝念成刺直击神魂，无形难防。", "通用", "/images/manuals/m19.jpg"),
                mk("血炼神光", "秘术", "地", "元婴期", "魔道血道秘术，可祭炼法宝器灵，凶险异常。", "魔道", "/images/manuals/m20.jpg"),
                mk("煞丹术", "秘术", "玄", "金丹期", "魔道凝煞成丹，借煞力增修为，戾气缠身。", "魔道", "/images/manuals/m21.jpg"),
                mk("融灵之术", "秘术", "地", "元婴期", "将器灵融于己身，人宝合一，威能大涨。", "通用", "/images/manuals/m22.jpg"),
                mk("春黎剑阵", "神通", "天", "元婴期", "青元剑诀配套剑阵，七十二口飞剑化春黎之景，困杀强敌。", "青元剑诀", "/images/manuals/m23.jpg"),
                mk("青蟠剑阵", "神通", "天", "元婴期", "另一配套剑阵，剑蟠盘绕如龙，攻守兼备。", "青元剑诀", "/images/manuals/m24.jpg"),
                mk("疾风九变", "神通", "天", "化神期", "风系遁法神通，瞬息九变，难锁定难追及。", "风道", "/images/manuals/m25.jpg"),
                mk("百脉炼宝诀", "神通", "地", "元婴期", "以全身经脉温养本命法宝，宝随人长。", "通用", "/images/manuals/m26.jpg"),
                mk("缩地成寸", "神通", "天", "化神期", "空间神通，一步千里，瞬息无踪。", "空间", "/images/manuals/m27.jpg"),
                mk("三头六臂", "神通", "天", "大乘期", "梵圣真魔功法相神通，三头六臂各持神兵。", "梵圣真魔功", "/images/manuals/m28.jpg"),
                mk("筑基丹丹方", "炼丹", "玄", "练气期", "筑基必用丹方，引灵气冲关，破境筑基。", "炼丹", "/images/manuals/m29.jpg"),
                mk("凝魂丹丹方", "炼丹", "地", "金丹期", "凝练神魂、滋补识海，结丹前后修行必备。", "炼丹", "/images/manuals/m30.jpg"),
                mk("大衍丹丹方", "炼丹", "地", "元婴期", "大衍神君所传，增识海神识，助参悟大道。", "大衍神君", "/images/manuals/m31.jpg"),
                mk("天火液炼方", "炼丹", "玄", "元婴期", "以地火灵焰炼体之灵液，淬体坚骨。", "炼丹", "/images/manuals/m32.jpg"),
                mk("长春丹方", "炼丹", "黄", "练气期", "疗伤培元之丹，气血双补，行走江湖常备。", "炼丹", "/images/manuals/m33.jpg"),
                mk("炼器总纲", "炼器", "玄", "筑基期", "炼器入门总纲，识灵材、掌火候、祭器灵。", "炼器", "/images/manuals/m34.jpg"),
                mk("青竹蜂云剑炼制术", "炼器", "地", "金丹期", "韩立本命飞剑炼制之术，七十二剑合一化云。", "韩立", "/images/manuals/m35.jpg"),
                mk("傀儡术总诀", "傀儡", "玄", "金丹期", "机关傀儡之术，以灵石驱傀儡，代劳御敌、布阵探查。", "傀儡", "/images/manuals/m36.jpg"),
                mk("千竹教傀儡术", "傀儡", "玄", "金丹期", "千竹教镇派傀儡绝学，灵石驱动机关傀儡替身御敌，精巧诡变。", "千竹教", "/images/manuals/m37.jpg"),
                mk("符箓基础", "符箓", "黄", "练气期", "制符入门，引灵力入符，留法术于纸。", "符箓", "/images/manuals/m38.jpg"),
                mk("雷符术", "符箓", "玄", "筑基期", "雷系符箓，掷出引天雷，一次性重创。", "符箓", "/images/manuals/m39.jpg"),
                mk("五行灵符", "符箓", "地", "金丹期", "金木水火土五系高阶符箓，攻防一体。", "符箓", "/images/manuals/m40.jpg"),
                mk("阵法初解", "阵法", "黄", "练气期", "阵法入门，布迷踪小阵，困敌隐匿。", "阵法", "/images/manuals/m41.jpg"),
                mk("幻阵术", "阵法", "玄", "筑基期", "幻术大阵，乱敌五感，困于虚境。", "阵法", "/images/manuals/m42.jpg"),
                mk("困龙阵", "阵法", "地", "金丹期", "困敌杀阵，引灵气成锁，锁龙困仙。", "阵法", "/images/manuals/m43.jpg"),
                mk("玄天斩灵术", "神通", "天", "炼虚期", "借玄天之宝之威，斩灵断魂，无物不破。", "玄天", "/images/manuals/m44.jpg"),
                mk("虚空炼器法", "炼器", "天", "合体期", "于虚空中祭炼法宝，纳空间之威。", "炼器", "/images/manuals/m45.jpg"),
                mk("渡劫秘要", "主修功法", "天", "渡劫期", "渡劫飞升之秘要，引雷劫淬身，九死一生。", "飞升", "/images/manuals/m46.jpg")
        );
        manualRepository.saveAll(seed);
        System.out.println("[Sutra] 藏经阁已录入 " + seed.size() + " 卷功法秘籍。");
    }

    private Manual mk(String name, String category, String grade, String realm, String desc, String origin, String image) {
        Manual m = new Manual();
        m.setName(name);
        m.setCategory(category);
        m.setGrade(grade);
        m.setRealm(realm);
        m.setDesc(desc);
        m.setOrigin(origin);
        m.setImage(image);
        return m;
    }
}
