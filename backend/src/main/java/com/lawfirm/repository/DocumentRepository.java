package com.lawfirm.repository;

import com.lawfirm.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // ── Generic ───────────────────────────────────────────────────────────────
    List<Document> findByCaseEntityId(Long caseId);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.caseEntity.id = :caseId")
    Long countByCaseId(@Param("caseId") Long caseId);

    // ── Client-scoped ─────────────────────────────────────────────────────────
    @Query("""
        SELECT d FROM Document d JOIN FETCH d.caseEntity c LEFT JOIN FETCH d.uploadedByUser
        WHERE c.client.username = :username ORDER BY d.uploadedAt DESC
    """)
    List<Document> findAllByClientUsername(@Param("username") String username);

    @Query("""
        SELECT d FROM Document d LEFT JOIN FETCH d.uploadedByUser
        WHERE d.caseEntity.id = :caseId AND d.caseEntity.client.username = :username
        ORDER BY d.uploadedAt DESC
    """)
    List<Document> findByCaseIdAndClientUsername(@Param("caseId") Long caseId,
                                                 @Param("username") String username);

    @Query("""
        SELECT d FROM Document d LEFT JOIN FETCH d.uploadedByUser
        WHERE d.id = :docId AND d.caseEntity.client.username = :username
    """)
    Optional<Document> findByIdAndClientUsername(@Param("docId") Long docId,
                                                 @Param("username") String username);

    @Query("""
        SELECT d FROM Document d JOIN FETCH d.caseEntity c LEFT JOIN FETCH d.uploadedByUser
        WHERE c.client.username = :username AND d.documentType = :type ORDER BY d.uploadedAt DESC
    """)
    List<Document> findByClientUsernameAndType(@Param("username") String username,
                                               @Param("type") Document.DocumentType type);

    @Query("""
        SELECT d FROM Document d JOIN FETCH d.caseEntity c LEFT JOIN FETCH d.uploadedByUser
        WHERE c.client.username = :username AND d.status = :status ORDER BY d.uploadedAt DESC
    """)
    List<Document> findByClientUsernameAndStatus(@Param("username") String username,
                                                 @Param("status") Document.DocumentStatus status);

    @Query("""
        SELECT d FROM Document d JOIN FETCH d.caseEntity c LEFT JOIN FETCH d.uploadedByUser
        WHERE c.client.username = :username
          AND (LOWER(d.title) LIKE LOWER(CONCAT('%',:kw,'%'))
            OR LOWER(d.fileName) LIKE LOWER(CONCAT('%',:kw,'%'))
            OR LOWER(d.description) LIKE LOWER(CONCAT('%',:kw,'%')))
        ORDER BY d.uploadedAt DESC
    """)
    List<Document> searchByClientUsername(@Param("username") String username,
                                          @Param("kw") String keyword);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.caseEntity.client.username = :username")
    long countByClientUsername(@Param("username") String username);

    // ── Lawyer-scoped ─────────────────────────────────────────────────────────
    @Query("""
        SELECT d FROM Document d JOIN FETCH d.caseEntity c LEFT JOIN FETCH d.uploadedByUser
        WHERE c.assignedLawyer.username = :username ORDER BY d.uploadedAt DESC
    """)
    List<Document> findAllByLawyerUsername(@Param("username") String username);

    @Query("""
        SELECT d FROM Document d LEFT JOIN FETCH d.uploadedByUser
        WHERE d.caseEntity.id = :caseId AND d.caseEntity.assignedLawyer.username = :username
        ORDER BY d.uploadedAt DESC
    """)
    List<Document> findByCaseIdAndLawyerUsername(@Param("caseId") Long caseId,
                                                 @Param("username") String username);

    @Query("""
        SELECT d FROM Document d LEFT JOIN FETCH d.uploadedByUser
        WHERE d.id = :docId AND d.caseEntity.assignedLawyer.username = :username
    """)
    Optional<Document> findByIdAndLawyerUsername(@Param("docId") Long docId,
                                                 @Param("username") String username);

    @Query("""
        SELECT d FROM Document d JOIN FETCH d.caseEntity c LEFT JOIN FETCH d.uploadedByUser
        WHERE c.assignedLawyer.username = :username AND d.documentType = :type
        ORDER BY d.uploadedAt DESC
    """)
    List<Document> findByLawyerUsernameAndType(@Param("username") String username,
                                               @Param("type") Document.DocumentType type);

    @Query("""
        SELECT d FROM Document d JOIN FETCH d.caseEntity c LEFT JOIN FETCH d.uploadedByUser
        WHERE c.assignedLawyer.username = :username AND d.status = :status
        ORDER BY d.uploadedAt DESC
    """)
    List<Document> findByLawyerUsernameAndStatus(@Param("username") String username,
                                                 @Param("status") Document.DocumentStatus status);

    @Query("""
        SELECT d FROM Document d JOIN FETCH d.caseEntity c LEFT JOIN FETCH d.uploadedByUser
        WHERE c.assignedLawyer.username = :username
          AND (LOWER(d.title) LIKE LOWER(CONCAT('%',:kw,'%'))
            OR LOWER(d.fileName) LIKE LOWER(CONCAT('%',:kw,'%'))
            OR LOWER(d.description) LIKE LOWER(CONCAT('%',:kw,'%')))
        ORDER BY d.uploadedAt DESC
    """)
    List<Document> searchByLawyerUsername(@Param("username") String username,
                                          @Param("kw") String keyword);

    /** Count documents pending review for a lawyer — for sidebar badge */
    @Query("""
        SELECT COUNT(d) FROM Document d
        WHERE d.caseEntity.assignedLawyer.username = :username
          AND d.status = 'UPLOADED'
    """)
    long countPendingReviewByLawyerUsername(@Param("username") String username);
}