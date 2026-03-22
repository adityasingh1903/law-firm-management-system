package com.lawfirm.repository;

import com.lawfirm.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {

    // ── Existing ──────────────────────────────────────────────────────────────
    long countByStatus(Case.CaseStatus status);

    @Query("SELECT c FROM Case c WHERE c.client.id = :clientId")
    List<Case> findByClientId(@Param("clientId") Long clientId);

    @Query("SELECT c FROM Case c WHERE c.client.username = :username ORDER BY c.createdAt DESC")
    List<Case> findByClientUsername(@Param("username") String username);

    @Query("SELECT c FROM Case c WHERE c.id = :caseId AND c.client.username = :username")
    Optional<Case> findByIdAndClientUsername(@Param("caseId") Long caseId,
                                             @Param("username") String username);

    // ── Lawyer queries (NEW) ──────────────────────────────────────────────────

    /** All cases assigned to this lawyer, newest first */
    @Query("""
        SELECT c FROM Case c
        LEFT JOIN FETCH c.client
        WHERE c.assignedLawyer.username = :username
        ORDER BY c.updatedAt DESC
    """)
    List<Case> findByLawyerUsername(@Param("username") String username);

    /** Single case with lawyer ownership check */
    @Query("""
        SELECT c FROM Case c
        LEFT JOIN FETCH c.client
        WHERE c.id = :caseId
          AND c.assignedLawyer.username = :username
    """)
    Optional<Case> findByIdAndLawyerUsername(@Param("caseId") Long caseId,
                                              @Param("username") String username);

    /** Distinct clients for a lawyer */
    @Query("""
        SELECT DISTINCT c.client FROM Case c
        WHERE c.assignedLawyer.username = :username
          AND c.client IS NOT NULL
    """)
    List<com.lawfirm.entity.User> findClientsByLawyerUsername(@Param("username") String username);

    /** Active cases count for a lawyer */
    @Query("""
        SELECT COUNT(c) FROM Case c
        WHERE c.assignedLawyer.username = :username
          AND c.status IN ('OPEN', 'IN_PROGRESS')
    """)
    long countActiveByLawyerUsername(@Param("username") String username);

    /** Total fees charged across lawyer's cases */
    @Query("""
        SELECT COALESCE(SUM(c.feesCharged), 0)
        FROM Case c
        WHERE c.assignedLawyer.username = :username
          AND c.feesCharged IS NOT NULL
    """)
    double sumFeesByLawyerUsername(@Param("username") String username);

    // ── Admin queries ─────────────────────────────────────────────────────────
    @Query("SELECT c FROM Case c ORDER BY c.createdAt DESC")
    List<Case> findAllOrderByCreatedAtDesc();

    @Query("SELECT c FROM Case c WHERE c.assignedLawyer.username = :username ORDER BY c.createdAt DESC")
    List<Case> findByAssignedLawyerUsername(@Param("username") String username);
}