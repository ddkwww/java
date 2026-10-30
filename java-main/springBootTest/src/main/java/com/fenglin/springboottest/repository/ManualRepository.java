package com.fenglin.springboottest.repository;

import com.fenglin.springboottest.entity.Manual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ManualRepository extends JpaRepository<Manual, Long> {

    Optional<Manual> findFirstByName(String name);

    /**
     * 关键字搜索：名称 / 简介 / 分类 / 境界 / 出处 任一字段命中即返回（忽略大小写）。
     */
    @Query("SELECT m FROM Manual m WHERE " +
           "LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(m.desc) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(m.category) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(m.realm) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(m.origin) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Manual> search(@Param("q") String q);
}
