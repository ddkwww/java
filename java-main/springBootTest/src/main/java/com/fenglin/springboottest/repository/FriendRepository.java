package com.fenglin.springboottest.repository;

import com.fenglin.springboottest.entity.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    /** 我发起的、处于某状态的好友关系 */
    List<Friend> findByOwnerAndStatus(String owner, String status);

    /** 别人发给我、处于某状态的好友关系（用于「收到的请求」） */
    List<Friend> findByFriendAndStatus(String friend, String status);

    Optional<Friend> findByOwnerAndFriend(String owner, String friend);

    void deleteByOwnerAndFriend(String owner, String friend);

    boolean existsByOwnerAndFriend(String owner, String friend);
}
