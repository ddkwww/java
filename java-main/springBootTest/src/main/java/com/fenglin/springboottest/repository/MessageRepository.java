package com.fenglin.springboottest.repository;

import com.fenglin.springboottest.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 两人之间的全部历史消息（双向，按时间正序）。
     * 等价于 (from=A,to=B) 与 (from=B,to=A) 的并集。
     */
    @Query("select m from Message m where (m.fromUser = :a1 and m.toUser = :b1) " +
           "or (m.fromUser = :a2 and m.toUser = :b2) order by m.createdAt asc")
    List<Message> findConversation(@Param("a1") String a1, @Param("b1") String b1,
                                   @Param("a2") String a2, @Param("b2") String b2);

    /** 某用户所有未读消息（按时间正序） */
    List<Message> findByToUserAndReadFalseOrderByCreatedAtAsc(String toUser);

    /** 把"某人发给我的所有未读消息"标记为已读 */
    @Modifying
    @Transactional
    @Query("update Message m set m.read = true where m.toUser = :to and m.fromUser = :from and m.read = false")
    int markRead(@Param("to") String to, @Param("from") String from);
}
