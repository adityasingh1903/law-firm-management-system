package com.lawfirm.controller;

import com.lawfirm.dto.*;
import com.lawfirm.service.LawyerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lawyer")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ROLE_LAWYER')")
public class LawyerController {

    @Autowired private LawyerService lawyerService;

    // ── Dashboard ─────────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<LawyerDashboardDto> getDashboard(Principal p) {
        return ResponseEntity.ok(lawyerService.getDashboard(p.getName()));
    }

    // ── Cases ─────────────────────────────────────────────────────────────────
    @GetMapping("/cases")
    public ResponseEntity<List<CaseDto>> getMyCases(Principal p) {
        return ResponseEntity.ok(lawyerService.getMyCases(p.getName()));
    }

    @GetMapping("/cases/{caseId}")
    public ResponseEntity<CaseDto> getCaseDetail(Principal p, @PathVariable Long caseId) {
        return ResponseEntity.ok(lawyerService.getCaseDetail(p.getName(), caseId));
    }

    @PatchMapping("/cases/{caseId}/status")
    public ResponseEntity<CaseDto> updateCaseStatus(Principal p, @PathVariable Long caseId,
                                                     @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(lawyerService.updateCaseStatus(p.getName(), caseId, body.get("status")));
    }

    @PatchMapping("/cases/{caseId}/notes")
    public ResponseEntity<CaseDto> updateCaseNotes(Principal p, @PathVariable Long caseId,
                                                    @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(lawyerService.updateCaseNotes(p.getName(), caseId, body.get("notes")));
    }

    @GetMapping("/cases/{caseId}/hearings")
    public ResponseEntity<List<HearingDto>> getHearingsForCase(Principal p, @PathVariable Long caseId) {
        return ResponseEntity.ok(lawyerService.getHearingsForCase(p.getName(), caseId));
    }

    @GetMapping("/cases/{caseId}/documents")
    public ResponseEntity<List<DocumentDto>> getDocumentsForCase(Principal p, @PathVariable Long caseId) {
        return ResponseEntity.ok(lawyerService.getDocumentsForCase(p.getName(), caseId));
    }

    @PostMapping(value = "/cases/{caseId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDto> uploadDocumentToCase(
            Principal p, @PathVariable Long caseId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "OTHER") String documentType) {
        return ResponseEntity.ok(lawyerService.uploadDocument(
                p.getName(), caseId, file, title, description, documentType));
    }

    // ── Case Requests ─────────────────────────────────────────────────────────
    @GetMapping("/requests")
    public ResponseEntity<List<CaseRequestDto>> getPendingRequests(Principal p,
            @RequestParam(required = false) String caseType) {
        return ResponseEntity.ok(caseType != null
                ? lawyerService.getPendingRequests(p.getName(), caseType)
                : lawyerService.getPendingRequests(p.getName()));
    }

    @GetMapping("/requests/handled")
    public ResponseEntity<List<CaseRequestDto>> getMyHandledRequests(Principal p) {
        return ResponseEntity.ok(lawyerService.getMyHandledRequests(p.getName()));
    }

    @GetMapping("/requests/count")
    public ResponseEntity<Long> getPendingCount() {
        return ResponseEntity.ok(lawyerService.getPendingRequestCount());
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<CaseDto> acceptRequest(Principal p, @PathVariable Long requestId,
                                                  @Valid @RequestBody AcceptCaseRequestDto dto) {
        return ResponseEntity.ok(lawyerService.acceptRequest(p.getName(), requestId, dto));
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<CaseRequestDto> rejectRequest(Principal p, @PathVariable Long requestId,
                                                         @Valid @RequestBody RejectCaseRequestDto dto) {
        return ResponseEntity.ok(lawyerService.rejectRequest(p.getName(), requestId, dto));
    }

    // ── Hearings ──────────────────────────────────────────────────────────────
    @GetMapping("/hearings/grouped")
    public ResponseEntity<HearingDto.GroupedHearingsDto> getGroupedHearings(Principal p) {
        return ResponseEntity.ok(lawyerService.getGroupedHearings(p.getName()));
    }

    @GetMapping("/hearings")
    public ResponseEntity<List<HearingDto>> getMyHearings(Principal p) {
        return ResponseEntity.ok(lawyerService.getMyHearings(p.getName()));
    }

    @GetMapping("/hearings/today")
    public ResponseEntity<List<HearingDto>> getTodayHearings(Principal p) {
        return ResponseEntity.ok(lawyerService.getTodayHearings(p.getName()));
    }

    @GetMapping("/hearings/{hearingId}")
    public ResponseEntity<HearingDto> getHearingDetail(Principal p, @PathVariable Long hearingId) {
        return ResponseEntity.ok(lawyerService.getHearingDetail(p.getName(), hearingId));
    }

    @PostMapping("/hearings")
    public ResponseEntity<HearingDto> createHearing(Principal p,
                                                     @Valid @RequestBody CreateHearingDto dto) {
        return ResponseEntity.ok(lawyerService.createHearing(p.getName(), dto));
    }

    @PutMapping("/hearings/{hearingId}")
    public ResponseEntity<HearingDto> updateHearing(Principal p, @PathVariable Long hearingId,
                                                     @RequestBody CreateHearingDto dto) {
        return ResponseEntity.ok(lawyerService.updateHearing(p.getName(), hearingId, dto));
    }

    @PatchMapping("/hearings/{hearingId}/status")
    public ResponseEntity<HearingDto> updateHearingStatus(Principal p, @PathVariable Long hearingId,
                                                           @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(lawyerService.updateHearingStatus(p.getName(), hearingId, body.get("status")));
    }

    @DeleteMapping("/hearings/{hearingId}")
    public ResponseEntity<Void> deleteHearing(Principal p, @PathVariable Long hearingId) {
        lawyerService.deleteHearing(p.getName(), hearingId);
        return ResponseEntity.ok().build();
    }

    // ── Documents ─────────────────────────────────────────────────────────────
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentDto>> getMyDocuments(Principal p) {
        return ResponseEntity.ok(lawyerService.getMyDocuments(p.getName()));
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentDto> getDocumentDetail(Principal p, @PathVariable Long documentId) {
        return ResponseEntity.ok(lawyerService.getDocumentDetail(p.getName(), documentId));
    }

    @GetMapping("/documents/search")
    public ResponseEntity<List<DocumentDto>> searchDocuments(Principal p,
                                                              @RequestParam String keyword) {
        return ResponseEntity.ok(lawyerService.searchDocuments(p.getName(), keyword));
    }

    @GetMapping("/documents/filter")
    public ResponseEntity<List<DocumentDto>> filterByType(Principal p,
                                                           @RequestParam String type) {
        return ResponseEntity.ok(lawyerService.getDocumentsByType(p.getName(), type));
    }

    @GetMapping("/documents/by-status")
    public ResponseEntity<List<DocumentDto>> filterByStatus(Principal p,
                                                             @RequestParam String status) {
        return ResponseEntity.ok(lawyerService.getDocumentsByStatus(p.getName(), status));
    }

    @PatchMapping("/documents/{documentId}/status")
    public ResponseEntity<DocumentDto> updateDocumentStatus(Principal p,
                                                             @PathVariable Long documentId,
                                                             @Valid @RequestBody UpdateDocumentStatusDto dto) {
        return ResponseEntity.ok(lawyerService.updateDocumentStatus(p.getName(), documentId, dto));
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(Principal p, @PathVariable Long documentId) {
        lawyerService.deleteDocument(p.getName(), documentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(Principal p, @PathVariable Long documentId) {
        DocumentDto doc = lawyerService.getDocumentDetail(p.getName(), documentId);
        try {
            Path filePath = Paths.get(doc.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable())
                return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(resolveContentType(doc.getFileType())))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + doc.getFileName() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Messages ──────────────────────────────────────────────────────────────
    @GetMapping("/messages")
    public ResponseEntity<List<MessageDto.ConversationSummary>> getConversationList(Principal p) {
        return ResponseEntity.ok(lawyerService.getConversationList(p.getName()));
    }

    @GetMapping("/cases/{caseId}/messages")
    public ResponseEntity<List<MessageDto>> getConversation(Principal p, @PathVariable Long caseId) {
        return ResponseEntity.ok(lawyerService.getConversation(p.getName(), caseId));
    }

    @PostMapping("/messages/send")
    public ResponseEntity<MessageDto> sendMessage(Principal p,
                                                   @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(lawyerService.sendMessage(p.getName(), request));
    }

    @GetMapping("/messages/unread-count")
    public ResponseEntity<Long> getUnreadCount(Principal p) {
        return ResponseEntity.ok(lawyerService.getUnreadCount(p.getName()));
    }

    // ── Clients ───────────────────────────────────────────────────────────────
    @GetMapping("/clients")
    public ResponseEntity<List<UserDto>> getMyClients(Principal p) {
        return ResponseEntity.ok(lawyerService.getMyClients(p.getName()));
    }

    @GetMapping("/clients/{clientId}")
    public ResponseEntity<UserDto> getClientDetail(Principal p, @PathVariable Long clientId) {
        return ResponseEntity.ok(lawyerService.getClientDetail(p.getName(), clientId));
    }

    // ── Billing ───────────────────────────────────────────────────────────────
    @GetMapping("/billing/summary")
    public ResponseEntity<InvoiceDto.BillingSummary> getBillingSummary(Principal p) {
        return ResponseEntity.ok(lawyerService.getBillingSummary(p.getName()));
    }

    @GetMapping("/billing/invoices")
    public ResponseEntity<List<InvoiceDto>> getMyInvoices(Principal p) {
        return ResponseEntity.ok(lawyerService.getMyInvoices(p.getName()));
    }

    @GetMapping("/billing/invoices/{invoiceId}")
    public ResponseEntity<InvoiceDto> getInvoiceDetail(Principal p, @PathVariable Long invoiceId) {
        return ResponseEntity.ok(lawyerService.getInvoiceDetail(p.getName(), invoiceId));
    }

    @PostMapping("/billing/invoices")
    public ResponseEntity<InvoiceDto> createInvoice(Principal p,
                                                     @Valid @RequestBody CreateInvoiceDto dto) {
        return ResponseEntity.status(201).body(lawyerService.createInvoice(p.getName(), dto));
    }

    @PatchMapping("/billing/invoices/{invoiceId}/status")
    public ResponseEntity<InvoiceDto> updateInvoiceStatus(Principal p,
                                                           @PathVariable Long invoiceId,
                                                           @Valid @RequestBody UpdateInvoiceStatusDto dto) {
        return ResponseEntity.ok(lawyerService.updateInvoiceStatus(p.getName(), invoiceId, dto));
    }

    @DeleteMapping("/billing/invoices/{invoiceId}")
    public ResponseEntity<Void> deleteInvoice(Principal p, @PathVariable Long invoiceId) {
        lawyerService.deleteInvoice(p.getName(), invoiceId);
        return ResponseEntity.noContent().build();
    }

    // ── Profile ───────────────────────────────────────────────────────────────
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfile(Principal p) {
        return ResponseEntity.ok(lawyerService.getProfile(p.getName()));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(Principal p,
                                                  @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(lawyerService.updateProfile(p.getName(), request));
    }

    @PutMapping("/profile/change-password")
    public ResponseEntity<Void> changePassword(Principal p,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        lawyerService.changePassword(p.getName(), request);
        return ResponseEntity.ok().build();
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private String resolveContentType(String fileType) {
        if (fileType == null) return "application/octet-stream";
        return switch (fileType.toUpperCase()) {
            case "PDF"  -> "application/pdf";
            case "DOC"  -> "application/msword";
            case "DOCX" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "XLS"  -> "application/vnd.ms-excel";
            case "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "JPG", "JPEG" -> "image/jpeg";
            case "PNG"  -> "image/png";
            case "TXT"  -> "text/plain";
            default     -> "application/octet-stream";
        };
    }
}