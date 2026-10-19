package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.entity.Manual;
import com.fenglin.springboottest.repository.ManualRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 藏经阁接口（契约见前端 src/api/sutra.js）：
 *   GET /api/manuals?category=&realm=&grade=&q=
 *       -> { items: [{ id, name, category, grade, realm, desc, origin }] }
 *       无参：返回全部秘籍；q 非空：关键字搜索，并可叠加 分类/境界/品阶 筛选
 *   GET /api/manuals/facets
 *       -> { categories:[...], realms:[...], grades:[...] }（供前端构建筛选器）
 *   GET /api/manuals/{id}
 *       -> { id, name, category, grade, realm, desc, origin, image }（单卷详情）
 *          未找到返回 404
 */
@RestController
@RequestMapping("/api/manuals")
public class SutraController {

    /** 修炼境界固定顺序（与小说体系一致） */
    private static final List<String> REALM_ORDER = Arrays.asList(
            "练气期", "筑基期", "金丹期", "元婴期", "化神期",
            "炼虚期", "合体期", "大乘期", "渡劫期");
    /** 品阶固定顺序：天 > 地 > 玄 > 黄 */
    private static final List<String> GRADE_ORDER = Arrays.asList("天", "地", "玄", "黄");

    private final ManualRepository manualRepository;

    public SutraController(ManualRepository manualRepository) {
        this.manualRepository = manualRepository;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String category,
                                    @RequestParam(required = false) String realm,
                                    @RequestParam(required = false) String grade,
                                    @RequestParam(required = false) String q) {
        String key = (q == null) ? "" : q.trim();
        List<Manual> source = key.isEmpty() ? manualRepository.findAll() : manualRepository.search(key);

        List<Manual> result = source.stream()
                .filter(m -> isBlank(category) || category.equals(m.getCategory()))
                .filter(m -> isBlank(realm) || realm.equals(m.getRealm()))
                .filter(m -> isBlank(grade) || grade.equals(m.getGrade()))
                .sorted(Comparator
                        .comparingInt((Manual m) -> gradeRank(m.getGrade()))
                        .thenComparing(Manual::getCategory)
                        .thenComparing(Manual::getName))
                .collect(Collectors.toList());

        Map<String, Object> resp = new LinkedHashMap<>(2);
        resp.put("items", result);
        return resp;
    }

    @GetMapping("/facets")
    public Map<String, Object> facets() {
        List<Manual> all = manualRepository.findAll();
        Map<String, Object> resp = new LinkedHashMap<>(3);
        resp.put("categories", distinctSorted(all, Manual::getCategory));
        resp.put("realms", distinctOrdered(all, Manual::getRealm, REALM_ORDER));
        resp.put("grades", distinctOrdered(all, Manual::getGrade, GRADE_ORDER));
        return resp;
    }

    @GetMapping("/{id}")
    public Manual detail(@PathVariable Long id) {
        return manualRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该卷秘籍：id=" + id));
    }

    private boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    private List<String> distinctSorted(List<Manual> all, Function<Manual, String> getter) {
        return all.stream().map(getter).filter(Objects::nonNull)
                .distinct().sorted().collect(Collectors.toList());
    }

    private List<String> distinctOrdered(List<Manual> all, Function<Manual, String> getter, List<String> order) {
        return all.stream().map(getter).filter(Objects::nonNull).distinct()
                .sorted(Comparator.comparingInt(r -> {
                    int i = order.indexOf(r);
                    return i < 0 ? 999 : i;
                })).collect(Collectors.toList());
    }

    private int gradeRank(String g) {
        int i = GRADE_ORDER.indexOf(g);
        return i < 0 ? 9 : i;
    }
}
