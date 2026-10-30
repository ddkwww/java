package com.fenglin.springboottest;

import com.fenglin.springboottest.entity.Manual;
import com.fenglin.springboottest.entity.Treasure;
import com.fenglin.springboottest.repository.ManualRepository;
import com.fenglin.springboottest.repository.TreasureRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 物品价格回填器（经济系统）。
 * 在 ItemStatSeeder（@Order 20）之后运行，按「物品名称」为每件功法/法宝写入灵石售价。
 *
 * 价格规则（见《数值系统设计文档》第十三节「经济系统」）：
 *   价格 = 品阶价格池 × 适用境界系数 × 类别价格系数
 *   品阶价格池：黄 100 / 玄 300 / 地 800 / 天 2000
 *   境界系数：练气×1 → 筑基×1.5 → 金丹×2.5 → 元婴×4 → 化神×6 → 炼虚×8 → 合体×10 → 大乘×12 → 渡劫×15
 *
 * 幂等：仅当某物品 price 为 null 时才回填，重复启动不覆盖手工调整的价格。
 */
@Component
@Order(30)
public class PriceSeeder implements CommandLineRunner {

    private final ManualRepository manualRepository;
    private final TreasureRepository treasureRepository;

    public PriceSeeder(ManualRepository manualRepository, TreasureRepository treasureRepository) {
        this.manualRepository = manualRepository;
        this.treasureRepository = treasureRepository;
    }

    /** 功法售价表：名称 -> 灵石 */
    private static final Map<String, Integer> MANUAL_PRICE = new LinkedHashMap<>();
    /** 法宝售价表：名称 -> 灵石 */
    private static final Map<String, Integer> TREASURE_PRICE = new LinkedHashMap<>();

    static {
        // ===== 主修功法 =====
        MANUAL_PRICE.put("长春功",       300);
        MANUAL_PRICE.put("青元剑诀",     1200);
        MANUAL_PRICE.put("大衍诀",       1200);
        MANUAL_PRICE.put("梵圣真魔功",   8000);
        MANUAL_PRICE.put("五雷正法",     750);
        MANUAL_PRICE.put("三阳决",       450);
        MANUAL_PRICE.put("明王诀",       3200);
        MANUAL_PRICE.put("素女轮回功",   3200);
        MANUAL_PRICE.put("托天魔功",     750);
        MANUAL_PRICE.put("渡劫秘要",     30000);

        // ===== 法术 =====
        MANUAL_PRICE.put("驱物术",       60);
        MANUAL_PRICE.put("火弹术",       60);
        MANUAL_PRICE.put("雷闪术",       60);
        MANUAL_PRICE.put("冰针术",       180);
        MANUAL_PRICE.put("土甲术",       60);
        MANUAL_PRICE.put("火蟒术",       270);
        MANUAL_PRICE.put("清风诀",       60);
        MANUAL_PRICE.put("匿风遁",       270);

        // ===== 秘术 =====
        MANUAL_PRICE.put("血影遁",       270);
        MANUAL_PRICE.put("惊神刺",       450);
        MANUAL_PRICE.put("血炼神光",     1920);
        MANUAL_PRICE.put("煞丹术",       450);
        MANUAL_PRICE.put("融灵之术",     1920);

        // ===== 神通 =====
        MANUAL_PRICE.put("春黎剑阵",     9600);
        MANUAL_PRICE.put("青蟠剑阵",     9600);
        MANUAL_PRICE.put("疾风九变",     14400);
        MANUAL_PRICE.put("百脉炼宝诀",   3840);
        MANUAL_PRICE.put("缩地成寸",     14400);
        MANUAL_PRICE.put("三头六臂",     28800);
        MANUAL_PRICE.put("玄天斩灵术",   19200);

        // ===== 炼丹 =====
        MANUAL_PRICE.put("筑基丹丹方",   240);
        MANUAL_PRICE.put("凝魂丹丹方",   1600);
        MANUAL_PRICE.put("大衍丹丹方",   2560);
        MANUAL_PRICE.put("天火液炼方",   960);
        MANUAL_PRICE.put("长春丹方",     80);

        // ===== 炼器 =====
        MANUAL_PRICE.put("炼器总纲",     360);
        MANUAL_PRICE.put("青竹蜂云剑炼制术", 1600);
        MANUAL_PRICE.put("虚空炼器法",   16000);

        // ===== 傀儡 =====
        MANUAL_PRICE.put("傀儡术总诀",   600);
        MANUAL_PRICE.put("千竹教傀儡术", 600);

        // ===== 符箓 =====
        MANUAL_PRICE.put("符箓基础",     80);
        MANUAL_PRICE.put("雷符术",       360);
        MANUAL_PRICE.put("五行灵符",     1600);

        // ===== 阵法 =====
        MANUAL_PRICE.put("阵法初解",     80);
        MANUAL_PRICE.put("幻阵术",       360);
        MANUAL_PRICE.put("困龙阵",       1600);

        // ===== 法宝 · 飞剑/攻伐 =====
        TREASURE_PRICE.put("青竹蜂云剑", 2000);
        TREASURE_PRICE.put("玄天斩灵剑", 16000);
        TREASURE_PRICE.put("乾蓝冰焰",   3200);
        TREASURE_PRICE.put("风雷旗",     750);
        TREASURE_PRICE.put("七焰扇",     12000);
        TREASURE_PRICE.put("墨蛟剑",     450);
        TREASURE_PRICE.put("雷珠",       450);

        // ===== 法宝 · 护身 =====
        TREASURE_PRICE.put("圭灵盾",     3200);
        TREASURE_PRICE.put("天罗真甲",   16000);

        // ===== 法宝 · 遁法 =====
        TREASURE_PRICE.put("风雷翅",     7200);
        TREASURE_PRICE.put("黑风旗",     90);

        // ===== 法宝 · 奇物 =====
        TREASURE_PRICE.put("聚宝盆",     330);
        TREASURE_PRICE.put("虚天鼎",     13200);
        TREASURE_PRICE.put("五行环",     2200);
        TREASURE_PRICE.put("化龙玺",     22000);

        // ===== 法宝 · 储物 =====
        TREASURE_PRICE.put("纳物袋",     50);
    }

    @Override
    public void run(String... args) {
        int manualCount = 0;
        int treasureCount = 0;

        for (Map.Entry<String, Integer> e : MANUAL_PRICE.entrySet()) {
            Optional<Manual> opt = manualRepository.findFirstByName(e.getKey());
            if (opt.isEmpty()) continue;
            Manual m = opt.get();
            if (m.getPrice() != null) continue; // 已回填，跳过
            m.setPrice(e.getValue());
            manualRepository.save(m);
            manualCount++;
        }

        for (Map.Entry<String, Integer> e : TREASURE_PRICE.entrySet()) {
            Optional<Treasure> opt = treasureRepository.findFirstByName(e.getKey());
            if (opt.isEmpty()) continue;
            Treasure t = opt.get();
            if (t.getPrice() != null) continue;
            t.setPrice(e.getValue());
            treasureRepository.save(t);
            treasureCount++;
        }

        System.out.println("[Price] 价格回填完成：功法 " + manualCount + " 卷，法宝 " + treasureCount + " 件。");
    }
}
