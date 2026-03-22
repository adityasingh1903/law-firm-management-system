package com.lawfirm.repository;

import com.lawfirm.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // ── Client-scoped (existing) ──────────────────────────────────────────────
    @Query("""
        SELECT i FROM Invoice i JOIN FETCH i.caseEntity c LEFT JOIN FETCH i.lawyer
        WHERE i.client.username = :username ORDER BY i.createdAt DESC
    """)
    List<Invoice> findAllByClientUsername(@Param("username") String username);

    @Query("""
        SELECT i FROM Invoice i LEFT JOIN FETCH i.lawyer
        WHERE i.caseEntity.id = :caseId AND i.client.username = :username
        ORDER BY i.createdAt DESC
    """)
    List<Invoice> findByCaseIdAndClientUsername(@Param("caseId") Long caseId,
                                                @Param("username") String username);

    @Query("""
        SELECT i FROM Invoice i JOIN FETCH i.caseEntity LEFT JOIN FETCH i.lawyer
        WHERE i.id = :id AND i.client.username = :username
    """)
    Optional<Invoice> findByIdAndClientUsername(@Param("id") Long id,
                                                @Param("username") String username);

    @Query("""
        SELECT i FROM Invoice i JOIN FETCH i.caseEntity c
        WHERE i.client.username = :username AND i.status IN ('UNPAID','PARTIALLY_PAID','OVERDUE')
        ORDER BY i.dueDate ASC
    """)
    List<Invoice> findPendingByClientUsername(@Param("username") String username);

    @Query("""
        SELECT COALESCE(SUM(i.totalAmount - COALESCE(i.paidAmount,0)),0)
        FROM Invoice i WHERE i.client.username = :username
          AND i.status IN ('UNPAID','PARTIALLY_PAID','OVERDUE')
    """)
    Double getTotalDueByClientUsername(@Param("username") String username);

    @Query("SELECT COALESCE(SUM(i.paidAmount),0) FROM Invoice i WHERE i.client.username = :username AND i.status IN ('PAID','PARTIALLY_PAID')")
    Double getTotalPaidByClientUsername(@Param("username") String username);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.client.username = :username AND i.status = :status")
    long countByClientUsernameAndStatus(@Param("username") String username,
                                        @Param("status") Invoice.InvoiceStatus status);

    @Query("SELECT COALESCE(MAX(i.id),0) FROM Invoice i")
    Long findMaxId();

    // ── Lawyer-scoped (NEW) ───────────────────────────────────────────────────

    /** All invoices created by / assigned to this lawyer */
    @Query("""
        SELECT i FROM Invoice i JOIN FETCH i.caseEntity c JOIN FETCH i.client
        WHERE i.lawyer.username = :username ORDER BY i.createdAt DESC
    """)
    List<Invoice> findAllByLawyerUsername(@Param("username") String username);

    /** Single invoice with lawyer ownership */
    @Query("""
        SELECT i FROM Invoice i JOIN FETCH i.caseEntity JOIN FETCH i.client
        WHERE i.id = :id AND i.lawyer.username = :username
    """)
    Optional<Invoice> findByIdAndLawyerUsername(@Param("id") Long id,
                                                @Param("username") String username);

    /** Total amount due on lawyer's invoices */
    @Query("""
        SELECT COALESCE(SUM(i.totalAmount - COALESCE(i.paidAmount,0)),0)
        FROM Invoice i WHERE i.lawyer.username = :username
          AND i.status IN ('UNPAID','PARTIALLY_PAID','OVERDUE')
    """)
    Double getTotalDueByLawyerUsername(@Param("username") String username);

    /** Total paid on lawyer's invoices */
    @Query("SELECT COALESCE(SUM(i.paidAmount),0) FROM Invoice i WHERE i.lawyer.username = :username AND i.status IN ('PAID','PARTIALLY_PAID')")
    Double getTotalPaidByLawyerUsername(@Param("username") String username);

    /** Count by status for a lawyer */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.lawyer.username = :username AND i.status = :status")
    long countByLawyerUsernameAndStatus(@Param("username") String username,
                                        @Param("status") Invoice.InvoiceStatus status);
}