package com.fenglin.springboottest.config;

import java.util.Arrays;

/**
 * 灵根定义与加成（攻/防/速倾向），与设计文档「七、灵根加成」一致。
 */
public enum SpiritRootEnum {

    JIN("金", 10, 5, 0),
    MU("木", 0, 10, 5),
    SHUI("水", 5, 10, 0),
    HUO("火", 15, 0, 0),
    TU("土", 5, 15, 0),
    LEI("雷·变异", 20, 5, 10),
    BING("冰·变异", 10, 15, 0),
    FENG("风·变异", 5, 0, 20);

    private final String name;
    private final int atk;
    private final int def;
    private final int spd;

    SpiritRootEnum(String name, int atk, int def, int spd) {
        this.name = name;
        this.atk = atk;
        this.def = def;
        this.spd = spd;
    }

    public String getName() { return name; }
    public int getAtk() { return atk; }
    public int getDef() { return def; }
    public int getSpd() { return spd; }

    /** 根据灵根名称取枚举，未匹配返回 null（无灵根）。
     *  兼容两种命名：「金」与「金灵根」均可匹配（前端注册传「金灵根」）。 */
    public static SpiritRootEnum ofName(String name) {
        if (name == null) return null;
        String target = name.trim();
        return Arrays.stream(values())
                .filter(s -> s.name.equals(target)
                        || (target.endsWith("灵根") && s.name.equals(target.substring(0, target.length() - 2))))
                .findFirst().orElse(null);
    }
}
