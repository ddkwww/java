package com.fenglin.springboottest.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对战状态机（内存态，重启即失效，与 TokenStore 风格一致）。
 *
 * 状态流转：
 *   challenge -> PENDING（等待对方响应）
 *   respond(accept=true)  -> FIGHTING（同时按双方真实属性模拟整场回合制战斗，产出战报日志）
 *   respond(accept=false) -> REJECTED
 * 超时（10 分钟）自动清理，避免内存堆积与卡死。
 */
@Service
public class DuelService {

    /** 单回合战报 */
    public static class Round {
        public int index;
        public String attacker;
        public String defender;
        public int damage;
        public int defHpAfter;
        public int defHpMax;
        public String text;
        public boolean finalRound;
    }

    /** 一场战斗的结果（胜者 + 逐回合日志） */
    public static class BattleResult {
        public String winner;
        public List<Round> rounds = new ArrayList<>();
    }

    public static class Duel {
        public String id;
        public String from;   // 发起方
        public String to;     // 被挑战方
        public String status; // PENDING / FIGHTING / REJECTED
        public String winner; // 胜者用户名（FIGHTING 后非空）
        public long createdAt;
        public List<Round> log; // 回合制战报（FIGHTING 后非空）
    }

    private final ConcurrentHashMap<String, Duel> duels = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /** 发起对战；已存在未决的同一对则直接返回已有对战 */
    public synchronized Duel challenge(String from, String to) {
        evict();
        if (from == null || to == null || from.equals(to)) return null;
        for (Duel d : duels.values()) {
            if ("PENDING".equals(d.status) && d.from.equals(from) && d.to.equals(to)) {
                return d;
            }
        }
        Duel d = new Duel();
        d.id = UUID.randomUUID().toString();
        d.from = from;
        d.to = to;
        d.status = "PENDING";
        d.createdAt = System.currentTimeMillis();
        duels.put(d.id, d);
        return d;
    }

    /** 取被挑战方当前待处理、最早的一笔对战请求 */
    public Duel pendingFor(String username) {
        evict();
        Duel best = null;
        for (Duel d : duels.values()) {
            if ("PENDING".equals(d.status) && d.to.equals(username)) {
                if (best == null || d.createdAt < best.createdAt) best = d;
            }
        }
        return best;
    }

    /**
     * 被挑战方响应。仅 to 本人可对 PENDING 状态响应。
     * 同意：模拟整场回合制战斗并存储战报；拒绝：置 REJECTED。
     */
    public Duel applyResult(String duelId, boolean accept, String username, BattleResult result) {
        Duel d = duels.get(duelId);
        if (d == null || !d.to.equals(username) || !"PENDING".equals(d.status)) return null;
        if (accept) {
            d.status = "FIGHTING";
            d.winner = result != null ? result.winner : null;
            d.log = result != null ? result.rounds : null;
        } else {
            d.status = "REJECTED";
        }
        return d;
    }

    /** 查询对战状态（挑战方轮询用） */
    public Duel statusOf(String duelId) {
        return duels.get(duelId);
    }

    /** 对战最大回合数：超过则按剩余气血判定胜负（气血多者胜，相同随机） */
    private static final int MAX_ROUNDS = 60;

    /**
     * 回合制战斗模拟（确定性，双方读取一致）：
     *   1) 速度高者先手；速度相同则境界高者先手；仍相同随机。
     *   2) 每回合攻击方造成伤害 = max(1, 攻击力 - 防御力)，扣减防御方气血。
     *   3) 一方气血归零即结束，另一方获胜。
     *   4) 最多 MAX_ROUNDS 回合，超时按剩余气血判定胜负。
     */
    @SuppressWarnings("unchecked")
    public BattleResult simulate(String aName, Map<String, Object> aPanel, int aLevel,
                                 String bName, Map<String, Object> bPanel, int bLevel) {
        BattleResult res = new BattleResult();
        Map<String, Object> aTotal = aPanel != null ? (Map<String, Object>) aPanel.get("total") : null;
        Map<String, Object> bTotal = bPanel != null ? (Map<String, Object>) bPanel.get("total") : null;
        int aHp = num(aTotal, "hp", 100), aAtk = num(aTotal, "atk", 10),
            aDef = num(aTotal, "def", 10), aSpd = num(aTotal, "spd", 10);
        int bHp = num(bTotal, "hp", 100), bAtk = num(bTotal, "atk", 10),
            bDef = num(bTotal, "def", 10), bSpd = num(bTotal, "spd", 10);
        int aHpMax = aHp, bHpMax = bHp;

        boolean aFirst;
        if (aSpd != bSpd) aFirst = aSpd > bSpd;
        else if (aLevel != bLevel) aFirst = aLevel > bLevel;
        else aFirst = random.nextBoolean();

        int round = 0;
        while (aHp > 0 && bHp > 0 && round < MAX_ROUNDS) {
            round++;
            boolean aAttacks = (round % 2 == 1) ? aFirst : !aFirst;
            String atkName, defName;
            int atkPow, defPow, defSpd, defHpMaxRef;
            if (aAttacks) {
                atkName = aName; defName = bName;
                atkPow = aAtk; defPow = bDef; defSpd = bSpd; defHpMaxRef = bHpMax;
            } else {
                atkName = bName; defName = aName;
                atkPow = bAtk; defPow = aDef; defSpd = aSpd; defHpMaxRef = aHpMax;
            }
            int dmg = Math.max(1, atkPow - defPow);
            if (aAttacks) bHp = Math.max(0, bHp - dmg);
            else aHp = Math.max(0, aHp - dmg);
            int defHpAfter = aAttacks ? bHp : aHp;

            Round r = new Round();
            r.index = round;
            r.attacker = atkName;
            r.defender = defName;
            r.damage = dmg;
            r.defHpAfter = defHpAfter;
            r.defHpMax = defHpMaxRef;
            r.text = String.format("第%d回合：%s（速度%d）出手，对 %s 造成 %d 点伤害，%s 剩余气血 %d/%d。",
                    round, atkName, defSpd, defName, dmg, defName, defHpAfter, defHpMaxRef);
            res.rounds.add(r);
        }

        boolean timedOut = (aHp > 0 && bHp > 0); // 双方均存活 => 打到上限
        if (timedOut) {
            // 60 回合战平：按剩余气血判定，气血多者胜；相同随机
            if (aHp != bHp) res.winner = aHp > bHp ? aName : bName;
            else res.winner = random.nextBoolean() ? aName : bName;
        } else {
            res.winner = aHp > 0 ? aName : bName;
        }
        String loser = res.winner.equals(aName) ? bName : aName;
        Round fin = new Round();
        fin.index = round + 1;
        fin.attacker = res.winner;
        fin.defender = loser;
        fin.finalRound = true;
        if (timedOut) {
            fin.text = String.format("【终局】六十回合鏖战未分生死，判定 %s 力压群雄，赢下此战！", res.winner);
        } else {
            fin.text = String.format("【终局】%s 气血耗尽，%s 力压群雄，赢下此战！", loser, res.winner);
        }
        res.rounds.add(fin);
        return res;
    }

    private static int num(Map<String, Object> m, String k, int def) {
        if (m == null) return def;
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    /** 清理超过 10 分钟的对战记录 */
    private void evict() {
        long now = System.currentTimeMillis();
        duels.entrySet().removeIf(e -> now - e.getValue().createdAt > 10L * 60 * 1000);
    }
}
