package com.fenglin.springboottest.service;

import com.fenglin.springboottest.config.*;
import com.fenglin.springboottest.entity.Manual;
import com.fenglin.springboottest.entity.Treasure;
import com.fenglin.springboottest.entity.User;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数值系统核心计算服务。
 * 依据《数值系统设计文档》计算角色五维属性（生命/寿元/攻击/防御/速度）。
 *
 * 最终属性 = 境界成长 + 灵根加成 + 体质加成 + 功法加成 + 法宝加成。
 * 所有公式与设计文档保持一致。
 */
public class CharacterStatsService {

    // ===== 基础值（练气一层） =====
    private static final int BASE_HP = 100;
    private static final int BASE_ATK = 10;
    private static final int BASE_DEF = 10;
    private static final int BASE_SPD = 10;

    // ===== 单层成长基数（再乘以境界序号） =====
    private static final int HP_GROWTH = 50;
    private static final int ATK_GROWTH = 8;
    private static final int DEF_GROWTH = 8;
    private static final int SPD_GROWTH = 5;

    // ===== 功法品阶基数（黄阶基准，再乘品阶倍率×适用境界序号） =====
    private static final int MANUAL_HP_BASE = 20;
    private static final int MANUAL_ATK_BASE = 6;
    private static final int MANUAL_LIFE_BASE = 10;

    // ===== 法宝品阶基数（黄阶基准，再乘品阶倍率×适用境界序号） =====
    private static final int TREASURE_ATK_BASE = 8;
    private static final int TREASURE_DEF_BASE = 6;
    private static final int TREASURE_SPD_BASE = 4;

    /**
     * 计算某修为总层数下的境界成长值（不含任何装备/体质/灵根加成）。
     *
     * @param totalLevel 修为总层数 1-81
     * @return 五维境界成长，key：hp/lifespan/atk/def/spd
     */
    public static Map<String, Integer> realmGrowth(int totalLevel) {
        int t = clamp(totalLevel);
        int realmIdx = RealmEnum.realmLevelOfTotal(t);
        int subLevel = RealmEnum.subLevelOfTotal(t);
        RealmEnum realm = RealmEnum.of(realmIdx);

        // 生命/攻击/防御/速度：每层成长 = 基数 × 境界序号，逐层累加（当前层已计入）
        int hp = BASE_HP + HP_GROWTH * realmIdx * t;
        int atk = BASE_ATK + ATK_GROWTH * realmIdx * t;
        int def = BASE_DEF + DEF_GROWTH * realmIdx * t;
        int spd = BASE_SPD + SPD_GROWTH * realmIdx * t;

        // 寿元：境界寿元上限 × (当前小境界层数 / 9)，保证练气一层为下限、九层为上限
        int lifespan = 0;
        if (realm != null) {
            lifespan = (int) Math.round(realm.getLifespan() * (subLevel / 9.0));
        }

        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("hp", hp);
        map.put("lifespan", lifespan);
        map.put("atk", atk);
        map.put("def", def);
        map.put("spd", spd);
        return map;
    }

    /**
     * 计算功法（主修功法）对五维的加成。
     * 若实体显式配置了 bonus_* 字段则优先使用；否则按 品阶基数 × 品阶倍率 × 适用境界序号 自动计算。
     *
     * @param manual 功法实体（可为 null，表示无功法加成）
     */
    public static Map<String, Integer> manualBonus(Manual manual) {
        Map<String, Integer> map = zero();
        if (manual == null) return map;
        boolean explicit = hasAnyBonus(manual.getBonusHp(), manual.getBonusLifespan(),
                manual.getBonusAtk(), manual.getBonusDef(), manual.getBonusSpd());
        if (explicit) {
            map.put("hp", nz(manual.getBonusHp()));
            map.put("lifespan", nz(manual.getBonusLifespan()));
            map.put("atk", nz(manual.getBonusAtk()));
            map.put("def", nz(manual.getBonusDef()));
            map.put("spd", nz(manual.getBonusSpd()));
            return map;
        }
        double mult = GradeEnum.multiplierOf(manual.getGrade());
        int realmIdx = realmIndexOf(manual.getRealm());
        map.put("hp", (int) Math.round(MANUAL_HP_BASE * mult * realmIdx));
        map.put("atk", (int) Math.round(MANUAL_ATK_BASE * mult * realmIdx));
        map.put("lifespan", (int) Math.round(MANUAL_LIFE_BASE * mult * realmIdx));
        return map;
    }

    /**
     * 计算法宝对五维的加成。
     * 若实体显式配置了 bonus_* 字段则优先使用；否则按 品阶基数 × 品阶倍率 × 适用境界序号 自动计算。
     *
     * @param treasure 法宝实体（可为 null，表示无法宝加成）
     */
    public static Map<String, Integer> treasureBonus(Treasure treasure) {
        Map<String, Integer> map = zero();
        if (treasure == null) return map;
        boolean explicit = hasAnyBonus(treasure.getBonusHp(), treasure.getBonusLifespan(),
                treasure.getBonusAtk(), treasure.getBonusDef(), treasure.getBonusSpd());
        if (explicit) {
            map.put("hp", nz(treasure.getBonusHp()));
            map.put("lifespan", nz(treasure.getBonusLifespan()));
            map.put("atk", nz(treasure.getBonusAtk()));
            map.put("def", nz(treasure.getBonusDef()));
            map.put("spd", nz(treasure.getBonusSpd()));
            return map;
        }
        double mult = GradeEnum.multiplierOf(treasure.getGrade());
        int realmIdx = realmIndexOf(treasure.getRealm());
        map.put("atk", (int) Math.round(TREASURE_ATK_BASE * mult * realmIdx));
        map.put("def", (int) Math.round(TREASURE_DEF_BASE * mult * realmIdx));
        map.put("spd", (int) Math.round(TREASURE_SPD_BASE * mult * realmIdx));
        return map;
    }

    /**
     * 体质固定加成（五维）。
     */
    public static Map<String, Integer> physiqueBonus(String physiqueName) {
        PhysiqueEnum p = PhysiqueEnum.ofName(physiqueName);
        Map<String, Integer> map = zero();
        map.put("hp", p.getHp());
        map.put("lifespan", p.getLifespan());
        map.put("atk", p.getAtk());
        map.put("def", p.getDef());
        map.put("spd", p.getSpd());
        return map;
    }

    /**
     * 灵根固定加成（攻/防/速）。
     */
    public static Map<String, Integer> spiritRootBonus(String spiritRootName) {
        SpiritRootEnum s = SpiritRootEnum.ofName(spiritRootName);
        Map<String, Integer> map = zero();
        if (s == null) return map;
        map.put("atk", s.getAtk());
        map.put("def", s.getDef());
        map.put("spd", s.getSpd());
        return map;
    }

    /**
     * 汇总计算角色最终五维属性，并返回明细（各来源加成）。
     *
     * @param user       角色（含修为总层数、体质、灵根）
     * @param manual     当前主修功法（可为 null）
     * @param treasure   当前法宝（可为 null）
     * @return 结构化结果：realm 信息 + 各来源加成 + 最终五维
     */
    public static Map<String, Object> compute(User user, Manual manual, Treasure treasure) {
        int totalLevel = user.getTotalLevel() != null ? user.getTotalLevel() : 1;

        Map<String, Integer> realm = realmGrowth(totalLevel);
        Map<String, Integer> root = spiritRootBonus(user.getSpiritRoot());
        Map<String, Integer> phys = physiqueBonus(user.getPhysique());
        Map<String, Integer> manu = manualBonus(manual);
        Map<String, Integer> treas = treasureBonus(treasure);

        Map<String, Integer> total = new LinkedHashMap<>();
        total.put("hp", realm.get("hp") + phys.get("hp") + manu.get("hp") + treas.get("hp"));
        // 寿元：直接使用数据库持久化的 lifespan 字段，与剩余寿元完全同步
        total.put("lifespan", remainingLifespan(user));
        total.put("atk", realm.get("atk") + root.get("atk") + phys.get("atk") + manu.get("atk") + treas.get("atk"));
        total.put("def", realm.get("def") + root.get("def") + phys.get("def") + manu.get("def") + treas.get("def"));
        total.put("spd", realm.get("spd") + root.get("spd") + phys.get("spd") + manu.get("spd") + treas.get("spd"));

        int realmIdx = RealmEnum.realmLevelOfTotal(totalLevel);
        RealmEnum realmEnum = RealmEnum.of(realmIdx);
        int subLevel = RealmEnum.subLevelOfTotal(totalLevel);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalLevel", totalLevel);
        result.put("realm", realmEnum != null ? realmEnum.getName() : "未知");
        result.put("realmLevel", realmIdx);
        result.put("subLevel", subLevel);
        result.put("stage", RealmEnum.stageOfSubLevel(subLevel));
        result.put("fullRealm", RealmEnum.fullName(totalLevel));
        result.put("lifespanCap", realmEnum != null ? realmEnum.getLifespan() : 0);

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("realm", realm);
        breakdown.put("spiritRoot", root);
        breakdown.put("physique", phys);
        breakdown.put("manual", manu);
        breakdown.put("treasure", treas);
        result.put("breakdown", breakdown);
        result.put("total", total);
        return result;
    }

    /**
     * 计算角色「剩余寿元」（年），用于修炼操作与寿元挂钩。
     * 优先返回数据库持久化的 lifespan 字段；若为 null（老数据），动态计算（不修改 user 对象）。
     *
     * @param user 角色（含修为总层数、体质、已消耗天数、lifespan）
     * @return 剩余寿元（年，向下取整，最少 0）
     */
    public static int remainingLifespan(User user) {
        if (user.getLifespan() != null) return user.getLifespan();
        // 兼容老数据：动态计算（只读，不修改 user 对象）
        int totalLevel = user.getTotalLevel() != null ? user.getTotalLevel() : 1;
        int realmIdx = RealmEnum.realmLevelOfTotal(totalLevel);
        RealmEnum realm = RealmEnum.of(realmIdx);
        int cap = realm != null ? realm.getLifespan() : 0;
        int physBonus = physiqueBonus(user.getPhysique()).get("lifespan");
        int ageUsed = user.getAgeUsed() != null ? user.getAgeUsed() : 0;
        int years = (int) Math.floor(ageUsed / 365.0);
        return Math.max(0, cap + physBonus - years);
    }

    /**
     * 根据当前境界、体质、已消耗天数，重新计算并设置用户的 lifespan 字段。
     * 公式：lifespan = 境界寿元上限 + 体质寿元加成 - floor(ageUsed / 365)。
     * 在每次消耗天数（打坐/除妖/挖矿/突破）后调用，保持寿元与消耗同步。
     *
     * @param user 角色（将被修改 lifespan 字段）
     */
    public static void refreshLifespan(User user) {
        int totalLevel = user.getTotalLevel() != null ? user.getTotalLevel() : 1;
        int realmIdx = RealmEnum.realmLevelOfTotal(totalLevel);
        RealmEnum realm = RealmEnum.of(realmIdx);
        int cap = realm != null ? realm.getLifespan() : 0;
        int physBonus = physiqueBonus(user.getPhysique()).get("lifespan");
        int ageUsed = user.getAgeUsed() != null ? user.getAgeUsed() : 0;
        int years = (int) Math.floor(ageUsed / 365.0);
        user.setLifespan(Math.max(0, cap + physBonus - years));
    }

    /** 根据境界名称取序号，未知回退 1 */
    private static int realmIndexOf(String realmName) {
        RealmEnum r = RealmEnum.ofName(realmName);
        return r != null ? r.getLevel() : 1;
    }

    private static int clamp(int totalLevel) {
        return Math.max(1, Math.min(RealmEnum.MAX_TOTAL_LEVEL, totalLevel));
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static boolean hasAnyBonus(Integer... vals) {
        for (Integer v : vals) {
            if (v != null && v != 0) return true;
        }
        return false;
    }

    private static Map<String, Integer> zero() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("hp", 0);
        map.put("lifespan", 0);
        map.put("atk", 0);
        map.put("def", 0);
        map.put("spd", 0);
        return map;
    }
}
