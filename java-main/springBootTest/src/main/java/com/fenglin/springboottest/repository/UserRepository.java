package com.fenglin.springboottest.repository;

import com.fenglin.springboottest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    /** 天骄榜：按修为总层数(totalLevel)降序取前 100 名（境界相同按注册时间早者优先，排序稳定） */
    List<User> findTop100ByOrderByTotalLevelDescCreatedAtAsc();

    /** 好友搜索：按用户名模糊匹配（忽略大小写） */
    List<User> findByUsernameContainingIgnoreCase(String keyword);

    boolean existsByUsername(String username);

    boolean existsByPermission(Integer permission);

    List<User> findByPermissionIsNull();
}
