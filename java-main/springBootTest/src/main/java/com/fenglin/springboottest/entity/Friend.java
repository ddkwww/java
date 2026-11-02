package com.fenglin.springboottest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 好友关系表（对应库 fanren_login 的 t_friend，由 ddl-auto=update 自动建表）。
 *
 * 关系采用「双向行」存储：
 *  - 发起方 A 向 B 发请求：写入 (owner=A, friend=B, status=PENDING)
 *  - B 同意：PENDING 行置 ACCEPTED，并补一条 (owner=B, friend=A, status=ACCEPTED)
 *  - 这样「我的好友」= owner=我 & status=ACCEPTED；「收到的请求」= friend=我 & status=PENDING
 */
@Entity
@Table(name = "t_friend")
public class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关系拥有者（列表归属者 / 发起方） */
    @Column(nullable = false, length = 64)
    private String owner;

    /** 好友用户名 */
    @Column(nullable = false, length = 64)
    private String friend;

    /** 状态：PENDING（待对方确认）/ ACCEPTED（已是好友） */
    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getFriend() { return friend; }
    public void setFriend(String friend) { this.friend = friend; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
