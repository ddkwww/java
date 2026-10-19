package com.fenglin.springboottest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 藏经阁 · 功法秘籍（对应库 fanren_login 的 t_manual）。
 * 三维分类：
 *   category 分类（主修功法/法术/秘术/神通/炼丹/炼器/符箓/阵法/傀儡）
 *   grade    品阶（天 | 地 | 玄 | 黄）
 *   realm    适用境界（练气期 … 渡劫期）
 */
@Entity
@Table(name = "t_manual")
public class Manual {

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

    /** 插画路径（后端静态资源，如 /images/manuals/m01.jpg） */
    @Column(length = 128)
    private String image;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
