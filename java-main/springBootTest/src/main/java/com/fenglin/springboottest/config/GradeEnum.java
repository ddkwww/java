package com.fenglin.springboottest.config;

/**
 * 品阶加成定义（天/地/玄/黄），与设计文档「五、品阶加成」一致。
 * 品阶倍率：黄×1 / 玄×2 / 地×4 / 天×8。
 */
public enum GradeEnum {

    HUANG("黄", 1.0),
    XUAN("玄", 2.0),
    DI("地", 4.0),
    TIAN("天", 8.0);

    private final String name;
    private final double multiplier;

    GradeEnum(String name, double multiplier) {
        this.name = name;
        this.multiplier = multiplier;
    }

    public String getName() { return name; }
    public double getMultiplier() { return multiplier; }

    /** 根据品阶名称取倍率，未匹配默认黄阶×1 */
    public static double multiplierOf(String name) {
        if (name == null) return HUANG.multiplier;
        for (GradeEnum g : values()) {
            if (g.name.equals(name)) return g.multiplier;
        }
        return HUANG.multiplier;
    }
}
