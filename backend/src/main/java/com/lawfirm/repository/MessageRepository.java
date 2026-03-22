package com.lawfirm.repository;

import com.lawfirm.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // ── Existing (keep as-is) ─────────────────────────────────────────────────

    @Query("""
        SELECT m FROM Message m
        WHERE m.caseEntity.id = :caseId
          AND (
            (m.sender.id = :userId AND m.receiver.id = :otherId)
            OR
            (m.sender.id = :otherId AND m.receiver.id = :userId)
          )
        ORDER BY m.createdAt ASC
    """)
    List<Message> findConversation(@Param("caseId")  Long caseId,
                                   @Param("userId")  Long userId,
                                   @Param("otherId") Long otherId);

    @Query("""
        SELECT m FROM Message m
        WHERE m.caseEntity.id = :caseId
        ORDER BY m.createdAt ASC
    """)
    List<Message> findByCaseId(@Param("caseId") Long caseId);

    long countBySenderIdAndReceiverIdAndIsReadFalse(Long senderId, Long receiverId);

    @Modifying
    @Transactional
    @Query("""
        UPDATE Message m SET m.isRead = true
        WHERE m.caseEntity.id = :caseId
          AND m.sender.id = :senderId
          AND m.receiver.id = :receiverId
          AND m.isRead = false
    """)
    void markConversationAsRead(@Param("caseId")     Long caseId,
                                @Param("senderId")   Long senderId,
                                @Param("receiverId") Long receiverId);

    // ── New (needed for messages page) ────────────────────────────────────────

    /**
     * All distinct cases where this client has a conversation, newest message first.
     * Used to build the conversation list on the left panel.
     */
    @Query("""
        SELECT m FROM Message m
        JOIN FETCH m.caseEntity c
        JOIN FETCH m.sender
        JOIN FETCH m.receiver
        WHERE (m.sender.username = :username OR m.receiver.username = :username)
        ORDER BY m.createdAt DESC
    """)
    List<Message> findAllInvolvingUsername(@Param("username") String username);

    /**
     * Latest message per case for the conversation list preview.
     */
    @Query("""
        SELECT m FROM Message m
        WHERE m.caseEntity.id = :caseId
        ORDER BY m.createdAt DESC
        LIMIT 1
    """)
    java.util.Optional<Message> findLatestByCaseId(@Param("caseId") Long caseId);

    /**
     * Total unread count for a receiver across ALL cases.
     */
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.receiver.username = :username
          AND m.isRead = false
    """)
    long countUnreadByReceiverUsername(@Param("username") String username);

    /**
     * Unread count for a specific case conversation.
     */
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.caseEntity.id = :caseId
          AND m.receiver.username = :username
          AND m.isRead = false
    """)
    long countUnreadByCaseIdAndReceiverUsername(@Param("caseId")   Long caseId,
                                                @Param("username") String username);
}