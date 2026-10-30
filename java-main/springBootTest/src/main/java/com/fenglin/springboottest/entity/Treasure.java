package com.fenglin.springboottest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 藏经阁 · 法宝（对应库 fanren_login 的 t_treasure）。
 * 三维分类：
 *   category 类别（飞剑 / 遁法 / 护身 / 攻伐 / 储物 / 奇物）
 *   grade    品阶（天 | 地 | 玄 | 黄）
 *   realm    适用境界（练气期 … 渡劫期）
 */
@Entity
@Table(name = "t_treasure")
public class Treasure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false, length = 8)
    private String grade;

    @Column(nullable = false, length = 16)
    private String realm;

    // desc 是 MySQL 保留字，映射为 description 列名，避免 CREATE TABLE 语法错误
    @Column(name = "description", length = 512)
    private String desc;

    @Column(length = 64)
    private String origin;

    /** 插画路径（后端静态资源，如 /images/manuals/mXX.jpg） */
    @Column(length = 128)
    private String image;

    // ===== 数值系统：属性加成（可空；为空时按 品阶×适用境界 自动计算） =====
    /** 生命加成 */
    @Column(name = "bonus_hp")
    private Integer bonusHp;
    /** 寿元加成 */
    @Column(name = "bonus_lifespan")
    private Integer bonusLifespan;
    /** 攻击加成 */
    @Column(name = "bonus_atk")
    private Integer bonusAtk;
    /** 防御加成 */
    @Column(name = "bonus_def")
    private Integer bonusDef;
    /** 速度加成 */
    @Column(name = "bonus_spd")
    private Integer bonusSpd;

    /** 售价（单位：灵石），经济系统字段 */
    @Column(name = "price")
    private Integer price;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getBonusHp() {
        return bonusHp;
    }

    public void setBonusHp(Integer bonusHp) {
        this.bonusHp = bonusHp;
    }

    public Integer getBonusLifespan() {
        return bonusLifespan;
    }

    public void setBonusLifespan(Integer bonusLifespan) {
        this.bonusLifespan = bonusLifespan;
    }

    public Integer getBonusAtk() {
        return bonusAtk;
    }

    public void setBonusAtk(Integer bonusAtk) {
        this.bonusAtk = bonusAtk;
    }

    public Integer getBonusDef() {
        return bonusDef;
    }

    public void setBonusDef(Integer bonusDef) {
        this.bonusDef = bonusDef;
    }

    public Integer getBonusSpd() {
        return bonusSpd;
    }

    public void setBonusSpd(Integer bonusSpd) {
        this.bonusSpd = bonusSpd;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
