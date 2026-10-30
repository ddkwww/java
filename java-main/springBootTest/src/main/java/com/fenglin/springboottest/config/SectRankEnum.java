package com.fenglin.springboottest.config;

import java.util.Arrays;

/**
 * 门派弟子级别（按修为境界自动判定）与月俸定义。
 * 弟子级别由「修为总层数」对应的大境界决定：
 *   练气期 / 筑基期          -> 外门弟子
 *   金丹期 / 元婴期          -> 内门弟子
 *   化神期及以上（炼虚/合体/大乘/渡劫）-> 真传弟子
 * 月俸（灵石 + 灵气）随级别递增，领取后进入 30 天冷却。
 */
public enum SectRankEnum {

    OUTER("外门弟子", 1, 100, 50),
    INNER("内门弟子", 2, 500, 200),
    TRUE("真传弟子", 3, 2000, 800);

    /** 级别名称 */
    private final String name;
    /** 级别序号（越高越尊） */
    private final int rank;
    /** 月俸灵石 */
    private final int stoneSalary;
    /** 月俸灵气 */
    private final int qiSalary;

    SectRankEnum(String name, int rank, int stoneSalary, int qiSalary) {
        this.name = name;
        this.rank = rank;
        this.stoneSalary = stoneSalary;
        this.qiSalary = qiSalary;
    }

    public String getName() { return name; }
    public int getRank() { return rank; }
    public int getStoneSalary() { return stoneSalary; }
    public int getQiSalary() { return qiSalary; }

    /** 月俸领取冷却秒数（30 天） */
    public static final long SALARY_COOLDOWN_SEC = 30L * 24 * 60 * 60;

    /**
     * 根据修为总层数判定弟子级别。
     * 练气(境界1)/筑基(境界2)=外门；金丹(3)/元婴(4)=内门；化神(5)及以上=真传。
     *
     * @param totalLevel 修为总层数（1-81）
     */
    public static SectRankEnum ofTotalLevel(int totalLevel) {
        int realmLevel = RealmEnum.realmLevelOfTotal(totalLevel);
        if (realmLevel <= 2) return OUTER;
        if (realmLevel <= 4) return INNER;
        return TRUE;
    }

    /** 根据级别名称取枚举，未匹配返回 null */
    public static SectRankEnum ofName(String name) {
        if (name == null) return null;
        return Arrays.stream(values())
                .filter(r -> r.name.equals(name))
                .findFirst().orElse(null);
    }
}
