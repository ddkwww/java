package com.fenglin.springboottest.config;

import java.util.Arrays;

/**
 * 体质定义与加成（五维固定加成），与设计文档「六、体质加成」一致。
 * 加成字段：生命/寿元/攻击/防御/速度。
 */
public enum PhysiqueEnum {

    FANREN("凡人之躯", 0, 0, 0, 0, 0),
    XIANDAOTI("先天道体", 200, 50, 10, 10, 5),
    TAIYIN("太阴之体", 150, 30, 15, 5, 0),
    CHUNYANG("纯阳之体", 150, 30, 10, 10, 0),
    JIUYOU("九幽玄阴体", 250, 80, 20, 15, 5),
    FENGLING("风灵体质", 100, 20, 5, 5, 20);

    private final String name;
    private final int hp;
    private final int lifespan;
    private final int atk;
    private final int def;
    private final int spd;

    PhysiqueEnum(String name, int hp, int lifespan, int atk, int def, int spd) {
        this.name = name;
        this.hp = hp;
        this.lifespan = lifespan;
        this.atk = atk;
        this.def = def;
        this.spd = spd;
    }

    public String getName() { return name; }
    public int getHp() { return hp; }
    public int getLifespan() { return lifespan; }
    public int getAtk() { return atk; }
    public int getDef() { return def; }
    public int getSpd() { return spd; }

    /** 根据体质名称取枚举，未匹配回退为凡人之躯 */
    public static PhysiqueEnum ofName(String name) {
        if (name == null) return FANREN;
        return Arrays.stream(values())
                .filter(p -> p.name.equals(name))
                .findFirst().orElse(FANREN);
    }
}
