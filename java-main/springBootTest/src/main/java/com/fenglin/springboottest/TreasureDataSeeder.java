package com.fenglin.springboottest;

import com.fenglin.springboottest.entity.Treasure;
import com.fenglin.springboottest.repository.TreasureRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 法宝阁数据灌入：应用启动时若 t_treasure 为空，则录入《凡人修仙传》法宝。
 * 仅灌一次（以表内是否有数据判定），便于后续在库中增删而不被覆盖。
 * image 复用静态图池（static/images/manuals/），保证卡片与详情页有插画兜底。
 * @Order(10)：先于 ItemStatSeeder(20) 灌入数据，保证数值回填时物品已存在。
 */
@Component
@Order(10)
public class TreasureDataSeeder implements CommandLineRunner {

    private final TreasureRepository treasureRepository;

    public TreasureDataSeeder(TreasureRepository treasureRepository) {
        this.treasureRepository = treasureRepository;
    }

    @Override
    public void run(String... args) {
        if (treasureRepository.count() > 0) {
            return; // 已录入，跳过
        }
        List<Treasure> seed = List.of(
                mk("青竹蜂云剑", "飞剑", "地", "金丹期", "韩立本命飞剑，七十二口青竹小剑合一化云，剑随心动，群攻无双。", "韩立", "/images/manuals/m35.jpg"),
                mk("风雷翅", "遁法", "天", "元婴期", "以风雷之力凝成双翅，振翅瞬息千里，遁速冠绝同阶。", "韩立", "/images/manuals/m25.jpg"),
                mk("玄天斩灵剑", "飞剑", "天", "炼虚期", "玄天之宝所化剑器，斩灵断魂，无物不破，魔道至宝。", "玄天", "/images/manuals/m44.jpg"),
                mk("聚宝盆", "奇物", "玄", "练气期", "纳灵聚宝，置入灵石可缓慢滋生利息，散修攒家底之宝。", "散修", "/images/manuals/m33.jpg"),
                mk("虚天鼎", "奇物", "天", "化神期", "虚天殿秘宝，内蕴须弥空间，曾封印古魔，威能莫测。", "虚天殿", "/images/manuals/m27.jpg"),
                mk("乾蓝冰焰", "攻伐", "地", "元婴期", "极寒本源灵焰，触物成冰，焚敌于无形。", "极寒之地", "/images/manuals/m13.jpg"),
                mk("风雷旗", "攻伐", "玄", "金丹期", "引动风雷的阵旗，布下可困敌削肉，声势骇人。", "阵法", "/images/manuals/m41.jpg"),
                mk("圭灵盾", "护身", "地", "元婴期", "上古大能圭灵所遗护身盾，水火不侵，硬抗法宝。", "圭灵", "/images/manuals/m14.jpg"),
                mk("七焰扇", "攻伐", "天", "化神期", "融七种灵焰于一扇，扇出焚天之火，威能惊世。", "火道", "/images/manuals/m15.jpg"),
                mk("墨蛟剑", "飞剑", "玄", "筑基期", "以墨蛟之骨炼制的飞剑，剑出带腥风，凌厉非常。", "炼器", "/images/manuals/m34.jpg"),
                mk("五行环", "奇物", "地", "金丹期", "金木水火土五环相生，可攻可守，变化无穷。", "五行", "/images/manuals/m05.jpg"),
                mk("天罗真甲", "护身", "天", "炼虚期", "天罗秘银织就的真甲，灵力流转间化解诸般攻伐。", "天罗", "/images/manuals/m07.jpg"),
                mk("雷珠", "攻伐", "玄", "筑基期", "封印雷力于宝珠，掷出引动天雷，一次性重创强敌。", "雷道", "/images/manuals/m12.jpg"),
                mk("黑风旗", "遁法", "黄", "练气期", "卷起黑风护身遁逃，价廉物美，江湖常备。", "散修", "/images/manuals/m10.jpg"),
                mk("纳物袋", "储物", "黄", "练气期", "须弥芥子之术，方寸之间藏百物，出行必备。", "通用", "/images/manuals/m36.jpg"),
                mk("化龙玺", "奇物", "天", "合体期", "上古龙族传承之玺，镇运纳福，亦可引动龙威压敌。", "龙族", "/images/manuals/m28.jpg")
        );
        treasureRepository.saveAll(seed);
        System.out.println("[Treasure] 法宝阁已录入 " + seed.size() + " 件法宝。");
    }

    private Treasure mk(String name, String category, String grade, String realm, String desc, String origin, String image) {
        Treasure t = new Treasure();
        t.setName(name);
        t.setCategory(category);
        t.setGrade(grade);
        t.setRealm(realm);
        t.setDesc(desc);
        t.setOrigin(origin);
        t.setImage(image);
        return t;
    }
}
