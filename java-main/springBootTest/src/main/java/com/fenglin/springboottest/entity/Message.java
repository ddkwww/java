package com.fenglin.springboottest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 好友聊天消息表（对应库 fanren_login 的 t_message，由 ddl-auto=update 自动建表）。
 *
 * 每条消息一行：fromUser 发送方、toUser 接收方，均存用户名。
 * 会话 = 一对用户（双向），历史消息按时间正序读取。
 */
@Entity
@Table(name = "t_message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 发送方用户名 */
    @Column(nullable = false, length = 64, name = "from_user")
    private String fromUser;

    /** 接收方用户名 */
    @Column(nullable = false, length = 64, name = "to_user")
    private String toUser;

    /** 消息内容 */
    @Column(nullable = false, length = 1000)
    private String content;

    /** 接收方是否已读（列名用 is_read，避免 MySQL 保留字 read 导致建表/读写 500） */
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    /** 消息类型：TEXT 普通文字 / GIFT 赠送灵石（列名用 msg_type，避免关键字的歧义） */
    @Column(name = "msg_type", nullable = false, length = 8)
    private String type = "TEXT";

    /** 赠送灵石数量，仅 type=GIFT 时有效 */
    @Column(name = "stone_amount", nullable = false)
    private int stone = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFromUser() { return fromUser; }
    public void setFromUser(String fromUser) { this.fromUser = fromUser; }

    public String getToUser() { return toUser; }
    public void setToUser(String toUser) { this.toUser = toUser; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getStone() { return stone; }
    public void setStone(int stone) { this.stone = stone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
