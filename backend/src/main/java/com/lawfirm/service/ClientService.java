package com.lawfirm.service;

import com.lawfirm.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ClientService {

    // ── Dashboard ─────────────────────────────────────────────────────────────
    ClientDashboardDto getDashboard(String username);

    // ── Cases ─────────────────────────────────────────────────────────────────
    List<ClientCaseDto> getMyCases(String username);
    ClientCaseDto getCaseDetail(String username, Long caseId);

    // ── Hearings ──────────────────────────────────────────────────────────────
    HearingDto.GroupedHearingsDto getGroupedHearings(String username);
    List<HearingDto> getMyHearings(String username);
    List<HearingDto> getHearingsForCase(String username, Long caseId);
    HearingDto getHearingDetail(String username, Long hearingId);

    // ── Documents ─────────────────────────────────────────────────────────────
    List<DocumentDto> getMyDocuments(String username);
    List<DocumentDto> getDocumentsForCase(String username, Long caseId);
    DocumentDto getDocumentDetail(String username, Long documentId);
    DocumentDto uploadDocument(String username, Long caseId, MultipartFile file,
                               String title, String description, String documentType);
    List<DocumentDto> searchDocuments(String username, String keyword);
    List<DocumentDto> getDocumentsByType(String username, String documentType);

    // ── Messages ──────────────────────────────────────────────────────────────
    List<MessageDto.ConversationSummary> getConversationList(String username);
    List<MessageDto> getConversation(String username, Long caseId);
    MessageDto sendMessage(String username, SendMessageRequest request);
    Long getUnreadCount(String username);

    // ── Billing ───────────────────────────────────────────────────────────────
    InvoiceDto.BillingSummary getBillingSummary(String username);
    List<InvoiceDto> getMyInvoices(String username);
    List<InvoiceDto> getInvoicesForCase(String username, Long caseId);
    InvoiceDto getInvoiceDetail(String username, Long invoiceId);
    List<InvoiceDto> getPendingInvoices(String username);

    // ── Profile ───────────────────────────────────────────────────────────────

    /** Get full profile of the logged-in client */
    UserDto getProfile(String username);

    /** Update basic info: name, email, phone, address */
    UserDto updateProfile(String username, UpdateProfileRequest request);

    /** Change password with current password verification */
    void changePassword(String username, ChangePasswordRequest request);
}