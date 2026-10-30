package com.fenglin.springboottest.config;

import java.util.Arrays;
import java.util.List;

/**
 * 九大修炼境界定义（由低到高），与设计文档一致。
 * 每个境界包含：序号、名称、寿元上限（年）。
 * 每个大境界内分 9 个小境界（初期1-3层 / 中期4-6层 / 后期7-9层）。
 */
public enum RealmEnum {

    LIANQI(1, "练气期", 100),
    ZHUJI(2, "筑基期", 200),
    JINDAN(3, "金丹期", 500),
    YUANYING(4, "元婴期", 1000),
    HUASHEN(5, "化神期", 2000),
    LIANXU(6, "炼虚期", 4000),
    HETI(7, "合体期", 8000),
    DACHENG(8, "大乘期", 15000),
    DUJIE(9, "渡劫期", 30000);

    /** 境界序号 1-9 */
    private final int level;
    /** 境界名称 */
    private final String name;
    /** 寿元上限（年） */
    private final int lifespan;

    RealmEnum(int level, String name, int lifespan) {
        this.level = level;
        this.name = name;
        this.lifespan = lifespan;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public int getLifespan() {
        return lifespan;
    }

    /** 每大境界的小境界层数 */
    public static final int SUB_LEVELS_PER_REALM = 9;

    /** 修为总层数上限：9 境界 × 9 层 = 81 */
    public static final int MAX_TOTAL_LEVEL = 9 * SUB_LEVELS_PER_REALM;

    /** 根据境界序号取枚举，越界返回 null */
    public static RealmEnum of(int level) {
        return Arrays.stream(values())
                .filter(r -> r.level == level)
                .findFirst().orElse(null);
    }

    /** 根据境界名称取枚举，未匹配返回 null */
    public static RealmEnum ofName(String name) {
        if (name == null) return null;
        return Arrays.stream(values())
                .filter(r -> r.name.equals(name))
                .findFirst().orElse(null);
    }

    /** 根据修为总层数（1-81）反推境界序号（1-9） */
    public static int realmLevelOfTotal(int totalLevel) {
        int t = Math.max(1, Math.min(MAX_TOTAL_LEVEL, totalLevel));
        return (t - 1) / SUB_LEVELS_PER_REALM + 1;
    }

    /** 根据修为总层数反推小境界层数（1-9） */
    public static int subLevelOfTotal(int totalLevel) {
        int t = Math.max(1, Math.min(MAX_TOTAL_LEVEL, totalLevel));
        return (t - 1) % SUB_LEVELS_PER_REALM + 1;
    }

    /** 小境界层数 -> 阶段名称（初期/中期/后期） */
    public static String stageOfSubLevel(int subLevel) {
        int s = Math.max(1, Math.min(SUB_LEVELS_PER_REALM, subLevel));
        if (s <= 3) return "初期";
        if (s <= 6) return "中期";
        return "后期";
    }

    /** 可读境界全称，如「练气期 初期 一层」 */
    public static String fullName(int totalLevel) {
        int rl = realmLevelOfTotal(totalLevel);
        int sl = subLevelOfTotal(totalLevel);
        RealmEnum realm = of(rl);
        String rn = realm != null ? realm.getName() : "未知";
        return rn + " " + stageOfSubLevel(sl) + " " + sl + "层";
    }

    /** 所有境界名称列表（顺序） */
    public static List<String> allNames() {
        return Arrays.stream(values()).map(RealmEnum::getName).toList();
    }

    // ===== 修炼突破体系：突破费用（灵石 + 灵气 + 天数） =====

    /** 每个大境界「每提升一层」所需灵石（下标 0 对应练气期，8 对应渡劫期） */
    private static final int[] BREAKTHROUGH_COST = {20, 50, 120, 300, 800, 2000, 5000, 12000, 30000};

    /** 每个大境界「每提升一层」所需灵气（与灵石同梯度，约为灵石的 1/2） */
    private static final int[] BREAKTHROUGH_QI_COST = {10, 25, 60, 150, 400, 1000, 2500, 6000, 15000};

    /** 每个大境界「每提升一层」所需闭关天数（境界越高，一次闭关越久） */
    private static final int[] BREAKTHROUGH_DAY_COST = {1, 2, 3, 5, 8, 13, 21, 34, 55};

    /**
     * 计算从「当前修为总层数」突破到下一层所需的灵石。
     * 费用 = 目标层所在境界的每层突破费（练气期 20 … 渡劫期 30000）。
     *
     * @param currentTotalLevel 当前修为总层数（1-81）
     * @return 下一层突破所需灵石；已满 81 层返回 -1
     */
    public static int breakthroughCost(int currentTotalLevel) {
        int idx = breakthroughIdx(currentTotalLevel);
        if (idx < 0) return -1;
        return BREAKTHROUGH_COST[idx];
    }

    /**
     * 计算突破到下一层所需灵气。
     *
     * @param currentTotalLevel 当前修为总层数（1-81）
     * @return 下一层突破所需灵气；已满 81 层返回 -1
     */
    public static int breakthroughQiCost(int currentTotalLevel) {
        int idx = breakthroughIdx(currentTotalLevel);
        if (idx < 0) return -1;
        return BREAKTHROUGH_QI_COST[idx];
    }

    /**
     * 计算突破到下一层所需闭关天数。
     *
     * @param currentTotalLevel 当前修为总层数（1-81）
     * @return 下一层突破所需天数；已满 81 层返回 -1
     */
    public static int breakthroughDayCost(int currentTotalLevel) {
        int idx = breakthroughIdx(currentTotalLevel);
        if (idx < 0) return -1;
        return BREAKTHROUGH_DAY_COST[idx];
    }

    /** 解析「下一层所属境界」的下标（0-based）；已满 81 层返回 -1 */
    private static int breakthroughIdx(int currentTotalLevel) {
        int t = Math.max(1, Math.min(MAX_TOTAL_LEVEL, currentTotalLevel));
        if (t >= MAX_TOTAL_LEVEL) return -1; // 已圆满
        int next = t + 1;
        int realmIdx = realmLevelOfTotal(next) - 1; // 0-based
        if (realmIdx < 0 || realmIdx >= BREAKTHROUGH_COST.length) return -1;
        return realmIdx;
    }
}
