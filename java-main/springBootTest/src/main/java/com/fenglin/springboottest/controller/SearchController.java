package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.entity.Manual;
import com.fenglin.springboottest.entity.Treasure;
import com.fenglin.springboottest.repository.ManualRepository;
import com.fenglin.springboottest.repository.TreasureRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 万象搜索统一接口（契约见前端 src/api/home.js）：
 *   GET /api/search?q=<关键词>
 *     -> { items: [{ id, name, type, grade, desc }] }
 *       - type: 分类（功法 | 法宝 | 丹药 | 灵草）
 *               后端目前收录 功法（t_manual）与 法宝（t_treasure），丹药/灵草暂无数据，仅返回已有两类
 *       - grade: 品阶（天 | 地 | 玄 | 黄）
 *       - desc: 一句话简介
 *   q 为空时返回推荐列表（功法、法宝各取若干条混合）
 *   合并 ManualRepository 与 TreasureRepository 的 search 结果，统一映射为上述扁平结构。
 *   丹药/灵草为后续扩展预留（type 字段已兼容），当前搜索不到时自然不返回。
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    /** 品阶固定顺序：天 > 地 > 玄 > 黄（用于结果排序） */
    private static final List<String> GRADE_ORDER = Arrays.asList("天", "地", "玄", "黄");
    /** 推荐列表每类最多条数（功法/法宝各取此数混合） */
    private static final int RECOMMEND_PER_TYPE = 4;

    private final ManualRepository manualRepository;
    private final TreasureRepository treasureRepository;

    public SearchController(ManualRepository manualRepository, TreasureRepository treasureRepository) {
        this.manualRepository = manualRepository;
        this.treasureRepository = treasureRepository;
    }

    @GetMapping
    public Map<String, Object> search(@RequestParam(required = false) String q) {
        String key = (q == null) ? "" : q.trim();
        List<Map<String, Object>> items;

        if (key.isEmpty()) {
            // 无关键字：返回推荐列表（各取前若干条，按品阶/类型/名称排序）
            items = recommend();
        } else {
            // 有关键字：功法 + 法宝 两边各搜，合并后按品阶/类型/名称排序
            items = new ArrayList<>();
            manualRepository.search(key).forEach(m ->
                    items.add(toItem(m.getId(), m.getName(), "功法", m.getGrade(), m.getDesc())));
            treasureRepository.search(key).forEach(t ->
                    items.add(toItem(t.getId(), t.getName(), "法宝", t.getGrade(), t.getDesc())));
            items.sort(comparator());
        }

        Map<String, Object> resp = new LinkedHashMap<>(2);
        resp.put("items", items);
        return resp;
    }

    /** 推荐列表：功法、法宝各取前 RECOMMEND_PER_TYPE 条 */
    private List<Map<String, Object>> recommend() {
        List<Map<String, Object>> items = new ArrayList<>();
        manualRepository.findAll().stream()
                .limit(RECOMMEND_PER_TYPE)
                .forEach(m -> items.add(toItem(m.getId(), m.getName(), "功法", m.getGrade(), m.getDesc())));
        treasureRepository.findAll().stream()
                .limit(RECOMMEND_PER_TYPE)
                .forEach(t -> items.add(toItem(t.getId(), t.getName(), "法宝", t.getGrade(), t.getDesc())));
        items.sort(comparator());
        return items;
    }

    /** 统一映射为 { id, name, type, grade, desc } */
    private Map<String, Object> toItem(Long id, String name, String type, String grade, String desc) {
        Map<String, Object> m = new LinkedHashMap<>(5);
        m.put("id", id);
        m.put("name", name);
        m.put("type", type);
        m.put("grade", grade == null ? "" : grade);
        m.put("desc", desc == null ? "" : desc);
        return m;
    }

    /** 排序：品阶(天>地>玄>黄) → 类型(功法→法宝) → 名称 */
    private Comparator<Map<String, Object>> comparator() {
        return Comparator
                .comparingInt((Map<String, Object> it) -> gradeRank((String) it.get("grade")))
                .thenComparing((Map<String, Object> it) -> (String) it.get("type"))
                .thenComparing((Map<String, Object> it) -> (String) it.get("name"));
    }

    private int gradeRank(String g) {
        int i = GRADE_ORDER.indexOf(g);
        return i < 0 ? 9 : i;
    }
}
