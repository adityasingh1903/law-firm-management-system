package com.lawfirm.service;

import com.lawfirm.dto.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface LawyerService {

    // ── Dashboard ─────────────────────────────────────────────────────────────
    LawyerDashboardDto getDashboard(String username);

    // ── Cases ─────────────────────────────────────────────────────────────────
    List<CaseDto> getMyCases(String username);
    CaseDto getCaseDetail(String username, Long caseId);
    CaseDto updateCaseStatus(String username, Long caseId, String status);
    CaseDto updateCaseNotes(String username, Long caseId, String notes);

    // ── Case Requests ─────────────────────────────────────────────────────────
    List<CaseRequestDto> getPendingRequests(String username);
    List<CaseRequestDto> getPendingRequests(String username, String caseType);
    List<CaseRequestDto> getMyHandledRequests(String username);
    CaseDto acceptRequest(String username, Long requestId, AcceptCaseRequestDto dto);
    CaseRequestDto rejectRequest(String username, Long requestId, RejectCaseRequestDto dto);
    long getPendingRequestCount();

    // ── Hearings ──────────────────────────────────────────────────────────────
    HearingDto.GroupedHearingsDto getGroupedHearings(String username);
    List<HearingDto> getMyHearings(String username);
    List<HearingDto> getTodayHearings(String username);
    List<HearingDto> getHearingsForCase(String username, Long caseId);
    HearingDto getHearingDetail(String username, Long hearingId);
    HearingDto createHearing(String username, CreateHearingDto dto);
    HearingDto updateHearing(String username, Long hearingId, CreateHearingDto dto);
    HearingDto updateHearingStatus(String username, Long hearingId, String status);
    void deleteHearing(String username, Long hearingId);

    // ── Documents ─────────────────────────────────────────────────────────────
    List<DocumentDto> getMyDocuments(String username);
    List<DocumentDto> getDocumentsForCase(String username, Long caseId);
    DocumentDto getDocumentDetail(String username, Long documentId);
    DocumentDto uploadDocument(String username, Long caseId, MultipartFile file,
                               String title, String description, String documentType);
    List<DocumentDto> searchDocuments(String username, String keyword);
    List<DocumentDto> getDocumentsByType(String username, String documentType);
    List<DocumentDto> getDocumentsByStatus(String username, String status);
    DocumentDto updateDocumentStatus(String username, Long documentId, UpdateDocumentStatusDto dto);
    void deleteDocument(String username, Long documentId);

    // ── Messages ──────────────────────────────────────────────────────────────
    List<MessageDto.ConversationSummary> getConversationList(String username);
    List<MessageDto> getConversation(String username, Long caseId);
    MessageDto sendMessage(String username, SendMessageRequest request);
    Long getUnreadCount(String username);

    // ── Clients ───────────────────────────────────────────────────────────────
    List<UserDto> getMyClients(String username);
    UserDto getClientDetail(String username, Long clientId);

    // ── Billing ───────────────────────────────────────────────────────────────
    InvoiceDto.BillingSummary getBillingSummary(String username);
    List<InvoiceDto> getMyInvoices(String username);
    InvoiceDto getInvoiceDetail(String username, Long invoiceId);
    InvoiceDto createInvoice(String username, CreateInvoiceDto dto);
    InvoiceDto updateInvoiceStatus(String username, Long invoiceId, UpdateInvoiceStatusDto dto);
    void deleteInvoice(String username, Long invoiceId);

    // ── Profile ───────────────────────────────────────────────────────────────
    UserDto getProfile(String username);
    UserDto updateProfile(String username, UpdateProfileRequest request);
    void changePassword(String username, ChangePasswordRequest request);
}