package com.fenglin.springboottest.game;

import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 大富翁多房间管理器（单例，线程安全）。
 *
 * 职责：
 *   - 同时维护多张牌桌（MonopolyRoom 实例，按 roomId 区分），支持「满 4 人开一局，其余人另开新局」。
 *   - 按用户名路由所有请求：每个玩家只属于一张牌桌；未报名者进入一张可加入的牌桌（没有则新建）。
 *   - 由本类的 @Scheduled 每秒驱动所有牌桌的 tick（超时代掷 / 自动跳过），替代原 MonopolyRoom 上的调度。
 *
 * 前端无需感知 roomId：所有接口仍按原路径，后端按登录用户自动定位其所在牌桌。
 */
@Component
public class MonopolyRoomManager {

    private final UserRepository userRepository;
    private final Map<String, MonopolyRoom> rooms = new ConcurrentHashMap<>();
    private final AtomicInteger seq = new AtomicInteger(0);

    public MonopolyRoomManager(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 返回某用户当前所在的牌桌（已报名），否则 null */
    public MonopolyRoom roomOf(String username) {
        for (MonopolyRoom r : rooms.values()) {
            if (r.isMember(username)) return r;
        }
        return null;
    }

    /** 选一张可加入的牌桌：等待阶段且未满（座位 < 4） */
    private MonopolyRoom pickJoinable(String username) {
        for (MonopolyRoom r : rooms.values()) {
            if (r.canJoin(username)) return r;
        }
        return null;
    }

    /** 调度：每秒驱动所有牌桌的 tick（超时自动代掷 / 自动跳过选择） */
    @Scheduled(fixedRate = 1000)
    public void tick() {
        long now = System.currentTimeMillis();
        for (MonopolyRoom r : rooms.values()) {
            r.tick();
        }
    }

    private long now() { return System.currentTimeMillis(); }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", msg);
        return m;
    }

    /** 报名：已在某桌则回到该桌；否则进入可加入的桌，没有则新建 */
    public Map<String, Object> join(String username, User u) {
        MonopolyRoom existing = roomOf(username);
        if (existing != null) {
            String err = existing.join(username, u);
            if (err != null) return error(err);
            return existing.snapshot(now(), username);
        }
        MonopolyRoom target = pickJoinable(username);
        if (target == null) {
            target = new MonopolyRoom("R" + seq.incrementAndGet(), userRepository);
            rooms.put(target.getRoomId(), target);
        }
        String err = target.join(username, u);
        if (err != null) return error(err);
        return target.snapshot(now(), username);
    }

    public Map<String, Object> leave(String username, User u) {
        MonopolyRoom r = roomOf(username);
        if (r == null) return error("您尚未报名");
        String err = r.leave(username, u);
        if (err != null) return error(err);
        return r.snapshot(now(), username);
    }

    public Map<String, Object> start(String username, User u) {
        MonopolyRoom r = roomOf(username);
        if (r == null) return error("您尚未报名");
        String err = r.start(u);
        if (err != null) return error(err);
        return r.snapshot(now(), username);
    }

    public Map<String, Object> roll(String username) {
        MonopolyRoom r = roomOf(username);
        if (r == null) return error("您尚未报名");
        String err = r.roll(username);
        if (err != null) return error(err);
        return r.snapshot(now(), username);
    }

    public Map<String, Object> decide(String username, String action) {
        MonopolyRoom r = roomOf(username);
        if (r == null) return error("您尚未报名");
        String err = r.decide(username, action);
        if (err != null) return error(err);
        return r.snapshot(now(), username);
    }

    public Map<String, Object> proposeEnd(String username) {
        MonopolyRoom r = roomOf(username);
        if (r == null) return error("您尚未报名");
        String err = r.proposeEnd(username);
        if (err != null) return error(err);
        return r.snapshot(now(), username);
    }

    public Map<String, Object> voteEnd(String username, boolean agree) {
        MonopolyRoom r = roomOf(username);
        if (r == null) return error("您尚未报名");
        String err = r.voteEnd(username, agree);
        if (err != null) return error(err);
        return r.snapshot(now(), username);
    }

    public Map<String, Object> reset(String username) {
        MonopolyRoom r = roomOf(username);
        if (r == null) return error("您尚未报名");
        String err = r.reset();
        if (err != null) return error(err);
        return r.snapshot(now(), username);
    }

    /** 牌桌状态：已报名者返回其所在桌；未报名者返回一张可加入桌（或新建空桌）的快照 */
    public Map<String, Object> state(String username) {
        MonopolyRoom r = roomOf(username);
        if (r != null) return r.snapshot(now(), username);
        MonopolyRoom candidate = pickJoinable(username);
        if (candidate == null) {
            candidate = new MonopolyRoom("R" + seq.incrementAndGet(), userRepository);
            rooms.put(candidate.getRoomId(), candidate);
        }
        return candidate.snapshot(now(), username);
    }
}
