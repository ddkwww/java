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
 * 功法/法宝数值回填器。
 * 在数据 seeder 之后运行（@Order 保证顺序），按「物品名称」为每件物品写入五维属性加成。
 *
 * 数值规则（见《数值系统设计文档》第十二节「物品数值表」）：
 *   品阶池：黄 40 / 玄 120 / 地 320 / 天 800，再乘适用境界系数；
 *   类别按比例分配到 生命/寿元/攻击/防御/速度 五维。
 *
 * 幂等：仅当某物品 bonus_* 全为 null 时才回填，重复启动不覆盖手工调整的数值。
 */
@Component
@Order(20)
public class ItemStatSeeder implements CommandLineRunner {

    private final ManualRepository manualRepository;
    private final TreasureRepository treasureRepository;

    public ItemStatSeeder(ManualRepository manualRepository, TreasureRepository treasureRepository) {
        this.manualRepository = manualRepository;
        this.treasureRepository = treasureRepository;
    }

    /** 功法加成表：名称 -> [生命, 寿元, 攻击, 防御, 速度] */
    private static final Map<String, int[]> MANUAL_STATS = new LinkedHashMap<>();
    /** 法宝加成表：名称 -> [生命, 寿元, 攻击, 防御, 速度] */
    private static final Map<String, int[]> TREASURE_STATS = new LinkedHashMap<>();

    static {
        // ===== 主修功法（生命/寿元/攻击） =====
        MANUAL_STATS.put("长春功",       s(48, 36, 36, 0, 0));
        MANUAL_STATS.put("青元剑诀",     s(192, 144, 144, 0, 0));
        MANUAL_STATS.put("大衍诀",       s(192, 144, 144, 0, 0));
        MANUAL_STATS.put("梵圣真魔功",   s(1280, 960, 960, 0, 0));
        MANUAL_STATS.put("五雷正法",     s(120, 90, 90, 0, 0));
        MANUAL_STATS.put("三阳决",       s(72, 54, 54, 0, 0));
        MANUAL_STATS.put("明王诀",       s(512, 384, 384, 0, 0));
        MANUAL_STATS.put("素女轮回功",   s(512, 384, 384, 0, 0));
        MANUAL_STATS.put("托天魔功",     s(120, 90, 90, 0, 0));
        MANUAL_STATS.put("渡劫秘要",     s(4800, 3600, 3600, 0, 0));

        // ===== 法术（攻击/速度） =====
        MANUAL_STATS.put("驱物术",       s(0, 0, 28, 0, 12));
        MANUAL_STATS.put("火弹术",       s(0, 0, 28, 0, 12));
        MANUAL_STATS.put("雷闪术",       s(0, 0, 28, 0, 12));
        MANUAL_STATS.put("冰针术",       s(0, 0, 84, 0, 36));
        MANUAL_STATS.put("土甲术",       s(0, 0, 28, 0, 12));
        MANUAL_STATS.put("火蟒术",       s(0, 0, 126, 0, 54));
        MANUAL_STATS.put("清风诀",       s(0, 0, 28, 0, 12));
        MANUAL_STATS.put("匿风遁",       s(0, 0, 126, 0, 54));

        // ===== 秘术（生命/攻击/速度） =====
        MANUAL_STATS.put("血影遁",       s(36, 0, 90, 0, 54));
        MANUAL_STATS.put("惊神刺",       s(60, 0, 150, 0, 90));
        MANUAL_STATS.put("血炼神光",     s(256, 0, 640, 0, 384));
        MANUAL_STATS.put("煞丹术",       s(60, 0, 150, 0, 90));
        MANUAL_STATS.put("融灵之术",     s(256, 0, 640, 0, 384));

        // ===== 神通（生命/攻击/速度） =====
        MANUAL_STATS.put("春黎剑阵",     s(640, 0, 1920, 0, 640));
        MANUAL_STATS.put("青蟠剑阵",     s(640, 0, 1920, 0, 640));
        MANUAL_STATS.put("疾风九变",     s(960, 0, 2880, 0, 960));
        MANUAL_STATS.put("百脉炼宝诀",   s(256, 0, 768, 0, 256));
        MANUAL_STATS.put("缩地成寸",     s(960, 0, 2880, 0, 960));
        MANUAL_STATS.put("三头六臂",     s(1920, 0, 5760, 0, 1920));
        MANUAL_STATS.put("玄天斩灵术",   s(1280, 0, 3840, 0, 1280));

        // ===== 炼丹（生命/寿元） =====
        MANUAL_STATS.put("筑基丹丹方",   s(60, 60, 0, 0, 0));
        MANUAL_STATS.put("凝魂丹丹方",   s(400, 400, 0, 0, 0));
        MANUAL_STATS.put("大衍丹丹方",   s(640, 640, 0, 0, 0));
        MANUAL_STATS.put("天火液炼方",   s(240, 240, 0, 0, 0));
        MANUAL_STATS.put("长春丹方",     s(20, 20, 0, 0, 0));

        // ===== 炼器（生命/寿元/防御） =====
        MANUAL_STATS.put("炼器总纲",     s(72, 36, 0, 72, 0));
        MANUAL_STATS.put("青竹蜂云剑炼制术", s(320, 160, 0, 320, 0));
        MANUAL_STATS.put("虚空炼器法",   s(3200, 1600, 0, 3200, 0));

        // ===== 傀儡（生命/寿元/防御） =====
        MANUAL_STATS.put("傀儡术总诀",   s(120, 60, 0, 120, 0));
        MANUAL_STATS.put("千竹教傀儡术", s(120, 60, 0, 120, 0));

        // ===== 符箓（生命/寿元） =====
        MANUAL_STATS.put("符箓基础",     s(20, 20, 0, 0, 0));
        MANUAL_STATS.put("雷符术",       s(90, 90, 0, 0, 0));
        MANUAL_STATS.put("五行灵符",     s(400, 400, 0, 0, 0));

        // ===== 阵法（生命/寿元/防御） =====
        MANUAL_STATS.put("阵法初解",     s(12, 8, 0, 20, 0));
        MANUAL_STATS.put("幻阵术",       s(54, 36, 0, 90, 0));
        MANUAL_STATS.put("困龙阵",       s(240, 160, 0, 400, 0));

        // ===== 法宝 · 飞剑/攻伐（纯攻击） =====
        TREASURE_STATS.put("青竹蜂云剑", s(0, 0, 800, 0, 0));
        TREASURE_STATS.put("玄天斩灵剑", s(0, 0, 6400, 0, 0));
        TREASURE_STATS.put("乾蓝冰焰",   s(0, 0, 1280, 0, 0));
        TREASURE_STATS.put("风雷旗",     s(0, 0, 300, 0, 0));
        TREASURE_STATS.put("七焰扇",     s(0, 0, 4800, 0, 0));
        TREASURE_STATS.put("墨蛟剑",     s(0, 0, 180, 0, 0));
        TREASURE_STATS.put("雷珠",       s(0, 0, 180, 0, 0));

        // ===== 法宝 · 护身（纯防御） =====
        TREASURE_STATS.put("圭灵盾",     s(0, 0, 0, 1280, 0));
        TREASURE_STATS.put("天罗真甲",   s(0, 0, 0, 6400, 0));

        // ===== 法宝 · 遁法（纯速度） =====
        TREASURE_STATS.put("风雷翅",     s(0, 0, 0, 0, 3200));
        TREASURE_STATS.put("黑风旗",     s(0, 0, 0, 0, 40));

        // ===== 法宝 · 奇物（生命/寿元） =====
        TREASURE_STATS.put("聚宝盆",     s(48, 72, 0, 0, 0));
        TREASURE_STATS.put("虚天鼎",     s(1920, 2880, 0, 0, 0));
        TREASURE_STATS.put("五行环",     s(320, 480, 0, 0, 0));
        TREASURE_STATS.put("化龙玺",     s(3200, 4800, 0, 0, 0));

        // ===== 法宝 · 储物（生命） =====
        TREASURE_STATS.put("纳物袋",     s(40, 0, 0, 0, 0));
    }

    @Override
    public void run(String... args) {
        int manualCount = 0;
        int treasureCount = 0;

        for (Map.Entry<String, int[]> e : MANUAL_STATS.entrySet()) {
            Optional<Manual> opt = manualRepository.findFirstByName(e.getKey());
            if (opt.isEmpty()) continue;
            Manual m = opt.get();
            if (m.getBonusHp() != null) continue; // 已回填，跳过
            int[] v = e.getValue();
            m.setBonusHp(v[0]);
            m.setBonusLifespan(v[1]);
            m.setBonusAtk(v[2]);
            m.setBonusDef(v[3]);
            m.setBonusSpd(v[4]);
            manualRepository.save(m);
            manualCount++;
        }

        for (Map.Entry<String, int[]> e : TREASURE_STATS.entrySet()) {
            Optional<Treasure> opt = treasureRepository.findFirstByName(e.getKey());
            if (opt.isEmpty()) continue;
            Treasure t = opt.get();
            if (t.getBonusHp() != null) continue;
            int[] v = e.getValue();
            t.setBonusHp(v[0]);
            t.setBonusLifespan(v[1]);
            t.setBonusAtk(v[2]);
            t.setBonusDef(v[3]);
            t.setBonusSpd(v[4]);
            treasureRepository.save(t);
            treasureCount++;
        }

        System.out.println("[ItemStat] 数值回填完成：功法 " + manualCount + " 卷，法宝 " + treasureCount + " 件。");
    }

    /** 便捷构造：生命/寿元/攻击/防御/速度 */
    private static int[] s(int hp, int life, int atk, int def, int spd) {
        return new int[]{hp, life, atk, def, spd};
    }
}
