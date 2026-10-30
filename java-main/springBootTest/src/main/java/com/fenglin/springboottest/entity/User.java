package com.fenglin.springboottest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户表（对应库 fanren_login 的 t_user）。
 * 表名用 t_user 以避开 MySQL 保留字 user。
 */
@Entity
@Table(name = "t_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "permission", columnDefinition = "int default 1")
    private Integer permission; // 0=管理员, 1=默认(普通用户)

    // ===== 人物初始化资料（注册页「游戏捏人」模块）=====
    /** 性别：男 / 女 */
    @Column(length = 8)
    private String gender;

    /** 年龄：10–99 */
    @Column
    private Integer age;

    /** 灵根：金/木/水/火/土/雷·变异/冰·变异/风·变异 */
    @Column(length = 32)
    private String spiritRoot;

    /** 体质：凡人之躯/先天道体/太阴之体/纯阳之体/九幽玄阴体/风灵体质 */
    @Column(length = 32)
    private String physique;

    /** 头像：默认头像键名(avatar:male-1 等)或上传的 dataURL(base64) */
    @Column(columnDefinition = "TEXT")
    private String avatar;

    /** 初始法宝 */
    @Column(length = 64)
    private String initialTreasure;

    /** 初始功法 */
    @Column(length = 64)
    private String initialGongfa;

    /** 修为：注册固定为「练气一层」，只读不可改 */
    @Column(length = 32)
    private String realm;

    /** 修为总层数（1-81）：境界序号×9 + 小境界层数，数值系统核心字段，默认 1（练气初期一层） */
    @Column(name = "total_level", columnDefinition = "int default 1")
    private Integer totalLevel = 1;

    /** 灵石余额（经济系统），默认 0 灵石 */
    @Column(name = "spirit_stone", columnDefinition = "int default 0")
    private Integer spiritStone = 0;

    /** 灵气余额（修炼突破体系第二资源），默认 0 点 */
    @Column(name = "spirit_qi", columnDefinition = "int default 0")
    private Integer spiritQi = 0;

    /** 已消耗天数（岁，寿元流逝累计）。打坐/除妖/挖矿/突破都会消耗天数，与寿元挂钩 */
    @Column(name = "age_used", columnDefinition = "int default 0")
    private Integer ageUsed = 0;

    /** 打坐修炼最后获取时间（冷却用） */
    @Column(name = "last_meditate_at")
    private LocalDateTime lastMeditateAt;

    /** 斩妖除魔最后获取时间（冷却用） */
    @Column(name = "last_hunt_at")
    private LocalDateTime lastHuntAt;

    /** 挖矿最后获取灵石时间（冷却用） */
    @Column(name = "last_mine_at")
    private LocalDateTime lastMineAt;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPermission() {
        return permission;
    }

    public void setPermission(Integer permission) {
        this.permission = permission;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getSpiritRoot() {
        return spiritRoot;
    }

    public void setSpiritRoot(String spiritRoot) {
        this.spiritRoot = spiritRoot;
    }

    public String getPhysique() {
        return physique;
    }

    public void setPhysique(String physique) {
        this.physique = physique;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getInitialTreasure() {
        return initialTreasure;
    }

    public void setInitialTreasure(String initialTreasure) {
        this.initialTreasure = initialTreasure;
    }

    public String getInitialGongfa() {
        return initialGongfa;
    }

    public void setInitialGongfa(String initialGongfa) {
        this.initialGongfa = initialGongfa;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Integer getTotalLevel() {
        return totalLevel;
    }

    public void setTotalLevel(Integer totalLevel) {
        this.totalLevel = totalLevel;
    }

    public Integer getSpiritStone() {
        return spiritStone;
    }

    public void setSpiritStone(Integer spiritStone) {
        this.spiritStone = spiritStone;
    }

    public Integer getSpiritQi() {
        return spiritQi;
    }

    public void setSpiritQi(Integer spiritQi) {
        this.spiritQi = spiritQi;
    }

    public Integer getAgeUsed() {
        return ageUsed;
    }

    public void setAgeUsed(Integer ageUsed) {
        this.ageUsed = ageUsed;
    }

    public LocalDateTime getLastMineAt() {
        return lastMineAt;
    }

    public void setLastMineAt(LocalDateTime lastMineAt) {
        this.lastMineAt = lastMineAt;
    }

    public LocalDateTime getLastMeditateAt() {
        return lastMeditateAt;
    }

    public void setLastMeditateAt(LocalDateTime lastMeditateAt) {
        this.lastMeditateAt = lastMeditateAt;
    }

    public LocalDateTime getLastHuntAt() {
        return lastHuntAt;
    }

    public void setLastHuntAt(LocalDateTime lastHuntAt) {
        this.lastHuntAt = lastHuntAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
