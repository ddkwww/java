package com.fenglin.springboottest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 门派（对应库 fanren_login 的 t_sect）。
 * 《凡人修仙传》修仙门派/势力一览。
 */
@Entity
@Table(name = "t_sect")
public class Sect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 门派名称 */
    @Column(nullable = false, length = 64)
    private String name;

    /** 势力阵营：正道 / 魔道 / 中立 / 散修 */
    @Column(nullable = false, length = 16)
    private String camp;

    /** 所属地域（如：越国、天南、大晋、灵界……） */
    @Column(length = 64)
    private String region;

    /** 门派简介 */
    @Column(name = "description", length = 512)
    private String desc;

    /** 镇派功法 / 招牌绝学 */
    @Column(length = 128)
    private String signature;

    /** 代表人物 */
    @Column(length = 128)
    private String representative;

    /** 门派标识插画路径（后端静态资源，可空） */
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

    public String getCamp() {
        return camp;
    }

    public void setCamp(String camp) {
        this.camp = camp;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getRepresentative() {
        return representative;
    }

    public void setRepresentative(String representative) {
        this.representative = representative;
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
