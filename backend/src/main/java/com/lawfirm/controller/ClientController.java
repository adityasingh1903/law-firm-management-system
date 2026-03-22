package com.lawfirm.controller;

import com.lawfirm.dto.*;
import com.lawfirm.service.ClientService;
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

@RestController
@RequestMapping("/api/client")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ROLE_CLIENT')")
public class ClientController {

    @Autowired private ClientService clientService;

    // ── Dashboard ─────────────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<ClientDashboardDto> getDashboard(Principal principal) {
        return ResponseEntity.ok(clientService.getDashboard(principal.getName()));
    }

    // ── Cases ─────────────────────────────────────────────────────────────────
    @GetMapping("/cases")
    public ResponseEntity<List<ClientCaseDto>> getMyCases(Principal principal) {
        return ResponseEntity.ok(clientService.getMyCases(principal.getName()));
    }

    @GetMapping("/cases/{id}")
    public ResponseEntity<ClientCaseDto> getCaseDetail(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(clientService.getCaseDetail(principal.getName(), id));
    }

    // ── Hearings ──────────────────────────────────────────────────────────────
    @GetMapping("/hearings/grouped")
    public ResponseEntity<HearingDto.GroupedHearingsDto> getGroupedHearings(Principal principal) {
        return ResponseEntity.ok(clientService.getGroupedHearings(principal.getName()));
    }

    @GetMapping("/hearings")
    public ResponseEntity<List<HearingDto>> getMyHearings(Principal principal) {
        return ResponseEntity.ok(clientService.getMyHearings(principal.getName()));
    }

    @GetMapping("/hearings/{hearingId}")
    public ResponseEntity<HearingDto> getHearingDetail(Principal principal, @PathVariable Long hearingId) {
        return ResponseEntity.ok(clientService.getHearingDetail(principal.getName(), hearingId));
    }

    @GetMapping("/cases/{caseId}/hearings")
    public ResponseEntity<List<HearingDto>> getHearingsForCase(Principal principal, @PathVariable Long caseId) {
        return ResponseEntity.ok(clientService.getHearingsForCase(principal.getName(), caseId));
    }

    // ── Documents ─────────────────────────────────────────────────────────────
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentDto>> getMyDocuments(Principal principal) {
        return ResponseEntity.ok(clientService.getMyDocuments(principal.getName()));
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentDto> getDocumentDetail(Principal principal, @PathVariable Long documentId) {
        return ResponseEntity.ok(clientService.getDocumentDetail(principal.getName(), documentId));
    }

    @GetMapping("/cases/{caseId}/documents")
    public ResponseEntity<List<DocumentDto>> getDocumentsForCase(Principal principal, @PathVariable Long caseId) {
        return ResponseEntity.ok(clientService.getDocumentsForCase(principal.getName(), caseId));
    }

    @PostMapping(value = "/cases/{caseId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDto> uploadDocument(
            Principal principal, @PathVariable Long caseId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "title",        required = false) String title,
            @RequestParam(value = "description",  required = false) String description,
            @RequestParam(value = "documentType", required = false, defaultValue = "OTHER") String documentType) {
        return ResponseEntity.ok(clientService.uploadDocument(
                principal.getName(), caseId, file, title, description, documentType));
    }

    @GetMapping("/documents/search")
    public ResponseEntity<List<DocumentDto>> searchDocuments(Principal principal, @RequestParam String keyword) {
        return ResponseEntity.ok(clientService.searchDocuments(principal.getName(), keyword));
    }

    @GetMapping("/documents/filter")
    public ResponseEntity<List<DocumentDto>> filterByType(Principal principal, @RequestParam String type) {
        return ResponseEntity.ok(clientService.getDocumentsByType(principal.getName(), type));
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(Principal principal, @PathVariable Long documentId) {
        DocumentDto doc = clientService.getDocumentDetail(principal.getName(), documentId);
        try {
            Path filePath = Paths.get(doc.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(resolveContentType(doc.getFileType())))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Messages ──────────────────────────────────────────────────────────────
    @GetMapping("/messages")
    public ResponseEntity<List<MessageDto.ConversationSummary>> getConversationList(Principal principal) {
        return ResponseEntity.ok(clientService.getConversationList(principal.getName()));
    }

    @GetMapping("/cases/{caseId}/messages")
    public ResponseEntity<List<MessageDto>> getConversation(Principal principal, @PathVariable Long caseId) {
        return ResponseEntity.ok(clientService.getConversation(principal.getName(), caseId));
    }

    @PostMapping("/messages/send")
    public ResponseEntity<MessageDto> sendMessage(Principal principal,
                                                   @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.ok(clientService.sendMessage(principal.getName(), request));
    }

    @GetMapping("/messages/unread-count")
    public ResponseEntity<Long> getUnreadCount(Principal principal) {
        return ResponseEntity.ok(clientService.getUnreadCount(principal.getName()));
    }

    // ── Billing ───────────────────────────────────────────────────────────────
    @GetMapping("/billing/summary")
    public ResponseEntity<InvoiceDto.BillingSummary> getBillingSummary(Principal principal) {
        return ResponseEntity.ok(clientService.getBillingSummary(principal.getName()));
    }

    @GetMapping("/billing/invoices")
    public ResponseEntity<List<InvoiceDto>> getMyInvoices(Principal principal) {
        return ResponseEntity.ok(clientService.getMyInvoices(principal.getName()));
    }

    @GetMapping("/billing/invoices/pending")
    public ResponseEntity<List<InvoiceDto>> getPendingInvoices(Principal principal) {
        return ResponseEntity.ok(clientService.getPendingInvoices(principal.getName()));
    }

    @GetMapping("/billing/invoices/{invoiceId}")
    public ResponseEntity<InvoiceDto> getInvoiceDetail(Principal principal, @PathVariable Long invoiceId) {
        return ResponseEntity.ok(clientService.getInvoiceDetail(principal.getName(), invoiceId));
    }

    @GetMapping("/cases/{caseId}/invoices")
    public ResponseEntity<List<InvoiceDto>> getInvoicesForCase(Principal principal, @PathVariable Long caseId) {
        return ResponseEntity.ok(clientService.getInvoicesForCase(principal.getName(), caseId));
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    /**
     * GET /api/client/profile
     * Returns full profile of the logged-in client.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfile(Principal principal) {
        return ResponseEntity.ok(clientService.getProfile(principal.getName()));
    }

    /**
     * PUT /api/client/profile
     * Update name, email, phone, address.
     */
    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(
            Principal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(clientService.updateProfile(principal.getName(), request));
    }

    /**
     * PUT /api/client/profile/change-password
     * Change password with current password verification.
     */
    @PutMapping("/profile/change-password")
    public ResponseEntity<Void> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        clientService.changePassword(principal.getName(), request);
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