package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.entity.Treasure;
import com.fenglin.springboottest.repository.TreasureRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 法宝阁接口（契约见前端 src/api/treasure.js）：
 *   GET  /api/treasures?category=&realm=&grade=&q=
 *       -> { items: [{ id, name, category, grade, realm, desc, origin, image }] }
 *       无参：返回全部法宝；q 非空：关键字搜索，并可叠加 类别/境界/品阶 筛选
 *   GET  /api/treasures/facets
 *       -> { categories:[...], realms:[...], grades:[...] }（供前端构建筛选器）
 *   GET  /api/treasures/{id}
 *       -> { id, name, category, grade, realm, desc, origin, image }（单件详情）
 *          未找到返回 404
 *   POST   /api/treasures          body: { name, category, grade, realm, desc, origin, image }
 *       -> 新建成功返回该件（含生成的主键 id）
 *   PUT    /api/treasures/{id}     body: 同上（仅覆盖传入字段）
 *       -> 更新成功返回该件；id 不存在返回 404
 *   DELETE /api/treasures/{id}
 *       -> { ok:true }；id 不存在返回 404
 *   注：写接口与 GET 保持一致，暂未强制鉴权（演示用）；接入正式管理后台时建议加角色校验。
 */
@RestController
@RequestMapping("/api/treasures")
public class TreasureController {

    /** 修炼境界固定顺序（与小说体系一致） */
    private static final List<String> REALM_ORDER = Arrays.asList(
            "练气期", "筑基期", "金丹期", "元婴期", "化神期",
            "炼虚期", "合体期", "大乘期", "渡劫期");
    /** 品阶固定顺序：天 > 地 > 玄 > 黄 */
    private static final List<String> GRADE_ORDER = Arrays.asList("天", "地", "玄", "黄");

    private final TreasureRepository treasureRepository;

    public TreasureController(TreasureRepository treasureRepository) {
        this.treasureRepository = treasureRepository;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String category,
                                    @RequestParam(required = false) String realm,
                                    @RequestParam(required = false) String grade,
                                    @RequestParam(required = false) String q) {
        String key = (q == null) ? "" : q.trim();
        List<Treasure> source = key.isEmpty() ? treasureRepository.findAll() : treasureRepository.search(key);

        List<Treasure> result = source.stream()
                .filter(m -> isBlank(category) || category.equals(m.getCategory()))
                .filter(m -> isBlank(realm) || realm.equals(m.getRealm()))
                .filter(m -> isBlank(grade) || grade.equals(m.getGrade()))
                .sorted(Comparator
                        .comparingInt((Treasure m) -> gradeRank(m.getGrade()))
                        .thenComparing(Treasure::getCategory)
                        .thenComparing(Treasure::getName))
                .collect(Collectors.toList());

        Map<String, Object> resp = new LinkedHashMap<>(2);
        resp.put("items", result);
        return resp;
    }

    @GetMapping("/facets")
    public Map<String, Object> facets() {
        List<Treasure> all = treasureRepository.findAll();
        Map<String, Object> resp = new LinkedHashMap<>(3);
        resp.put("categories", distinctSorted(all, Treasure::getCategory));
        resp.put("realms", distinctOrdered(all, Treasure::getRealm, REALM_ORDER));
        resp.put("grades", distinctOrdered(all, Treasure::getGrade, GRADE_ORDER));
        return resp;
    }

    @GetMapping("/{id}")
    public Treasure detail(@PathVariable Long id) {
        return treasureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该件法宝：id=" + id));
    }

    /** 新增法宝：body { name, category, grade, realm, desc, origin, image } */
    @PostMapping
    public Treasure create(@RequestBody Treasure m) {
        if (isBlank(m.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "名称不能为空");
        }
        Treasure entity = new Treasure();
        entity.setName(m.getName());
        entity.setCategory(m.getCategory());
        entity.setGrade(m.getGrade());
        entity.setRealm(m.getRealm());
        entity.setDesc(m.getDesc());
        entity.setOrigin(m.getOrigin());
        entity.setImage(m.getImage());
        return treasureRepository.save(entity);
    }

    /** 更新法宝：patch 语义，仅覆盖传入字段，保留 id 与 createdAt */
    @PutMapping("/{id}")
    public Treasure update(@PathVariable Long id, @RequestBody Treasure m) {
        Treasure existing = treasureRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该件法宝：id=" + id));
        if (!isBlank(m.getName())) existing.setName(m.getName());
        if (!isBlank(m.getCategory())) existing.setCategory(m.getCategory());
        if (!isBlank(m.getGrade())) existing.setGrade(m.getGrade());
        if (!isBlank(m.getRealm())) existing.setRealm(m.getRealm());
        if (m.getDesc() != null) existing.setDesc(m.getDesc());
        if (m.getOrigin() != null) existing.setOrigin(m.getOrigin());
        if (m.getImage() != null) existing.setImage(m.getImage());
        return treasureRepository.save(existing);
    }

    /** 删除法宝 */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        if (!treasureRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该件法宝：id=" + id);
        }
        treasureRepository.deleteById(id);
        Map<String, Object> resp = new LinkedHashMap<>(1);
        resp.put("ok", true);
        return resp;
    }

    private boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    private List<String> distinctSorted(List<Treasure> all, Function<Treasure, String> getter) {
        return all.stream().map(getter).filter(Objects::nonNull)
                .distinct().sorted().collect(Collectors.toList());
    }

    private List<String> distinctOrdered(List<Treasure> all, Function<Treasure, String> getter, List<String> order) {
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
