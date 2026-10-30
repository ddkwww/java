package com.fenglin.springboottest.repository;

import com.fenglin.springboottest.entity.Treasure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TreasureRepository extends JpaRepository<Treasure, Long> {

    Optional<Treasure> findFirstByName(String name);

    /**
     * 关键字搜索：名称 / 简介 / 类别 / 境界 / 出处 任一字段命中即返回（忽略大小写）。
     */
    @Query("SELECT t FROM Treasure t WHERE " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(t.desc) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(t.category) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(t.realm) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(t.origin) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Treasure> search(@Param("q") String q);
}
