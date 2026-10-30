package com.fenglin.springboottest.repository;

import com.fenglin.springboottest.entity.Sect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SectRepository extends JpaRepository<Sect, Long> {

    /**
     * 关键字搜索：名称 / 阵营 / 地域 / 简介 / 镇派功法 / 代表人物 任一字段命中即返回（忽略大小写）。
     */
    @Query("SELECT s FROM Sect s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.camp) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.region) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.desc) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.signature) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.representative) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Sect> search(@Param("q") String q);
}
