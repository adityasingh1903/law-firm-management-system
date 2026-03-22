package com.lawfirm.service.impl;

import com.lawfirm.dto.*;
import com.lawfirm.entity.*;
import com.lawfirm.repository.*;
import com.lawfirm.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ClientServiceImpl implements ClientService {

    @Autowired private HearingRepository  hearingRepository;
    @Autowired private CaseRepository     caseRepository;
    @Autowired private UserRepository     userRepository;
    @Autowired private MessageRepository  messageRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private InvoiceRepository  invoiceRepository;
    @Autowired private PasswordEncoder    passwordEncoder;

    @Value("${app.upload.dir:uploads/documents}")
    private String uploadDir;

    // ── Dashboard ─────────────────────────────────────────────────────────────
    @Override
    public ClientDashboardDto getDashboard(String username) {
        User user = resolveUser(username);
        List<Case> cases = caseRepository.findByClientUsername(username);

        int open       = (int) cases.stream().filter(c -> c.getStatus() == Case.CaseStatus.OPEN).count();
        int inProgress = (int) cases.stream().filter(c -> c.getStatus() == Case.CaseStatus.IN_PROGRESS).count();
        int closed     = (int) cases.stream().filter(c -> c.getStatus() == Case.CaseStatus.CLOSED).count();

        double totalFees       = cases.stream().filter(c -> c.getFeesCharged()      != null).mapToDouble(Case::getFeesCharged).sum();
        double totalSettlement = cases.stream().filter(c -> c.getSettlementAmount() != null).mapToDouble(Case::getSettlementAmount).sum();
        long   unread          = messageRepository.countUnreadByReceiverUsername(username);

        List<ClientCaseDto> recentCases = cases.stream()
                .limit(5).map(c -> ClientCaseDto.fromEntity(c, 0L)).collect(Collectors.toList());

        List<HearingDto> upcomingHearings = hearingRepository
                .findUpcomingByClientUsername(username, LocalDateTime.now())
                .stream().limit(5).map(HearingDto::fromEntity).collect(Collectors.toList());

        LawyerSummaryDto lawyerSummary = cases.stream()
                .filter(c -> c.getAssignedLawyer() != null
                          && (c.getStatus() == Case.CaseStatus.OPEN
                           || c.getStatus() == Case.CaseStatus.IN_PROGRESS))
                .findFirst()
                .map(c -> LawyerSummaryDto.fromEntity(c.getAssignedLawyer()))
                .orElse(null);

        ClientDashboardDto dto = new ClientDashboardDto();
        dto.setTotalCases(cases.size());
        dto.setOpenCases(open);
        dto.setInProgressCases(inProgress);
        dto.setClosedCases(closed);
        dto.setTotalFees(totalFees);
        dto.setTotalSettlement(totalSettlement);
        dto.setUnreadMessages(unread);
        dto.setRecentCases(recentCases);
        dto.setUpcomingHearings(upcomingHearings);
        dto.setAssignedLawyer(lawyerSummary);
        return dto;
    }

    // ── Cases ─────────────────────────────────────────────────────────────────
    @Override
    public List<ClientCaseDto> getMyCases(String username) {
        User user = resolveUser(username);
        return caseRepository.findByClientUsername(username).stream()
                .map(c -> {
                    long unread = c.getAssignedLawyer() != null
                            ? messageRepository.countBySenderIdAndReceiverIdAndIsReadFalse(
                                    c.getAssignedLawyer().getId(), user.getId())
                            : 0L;
                    return ClientCaseDto.fromEntity(c, unread);
                }).collect(Collectors.toList());
    }

    @Override
    public ClientCaseDto getCaseDetail(String username, Long caseId) {
        User user = resolveUser(username);
        Case c = caseRepository.findByIdAndClientUsername(caseId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
        long unread = c.getAssignedLawyer() != null
                ? messageRepository.countBySenderIdAndReceiverIdAndIsReadFalse(
                        c.getAssignedLawyer().getId(), user.getId())
                : 0L;
        return ClientCaseDto.fromEntity(c, unread);
    }

    // ── Hearings ──────────────────────────────────────────────────────────────
    @Override
    public HearingDto.GroupedHearingsDto getGroupedHearings(String username) {
        LocalDateTime now = LocalDateTime.now();
        List<HearingDto> upcoming = hearingRepository.findUpcomingByClientUsername(username, now)
                .stream().map(HearingDto::fromEntity).collect(Collectors.toList());
        List<HearingDto> past = hearingRepository.findPastByClientUsername(username, now)
                .stream().map(HearingDto::fromEntity).collect(Collectors.toList());
        return new HearingDto.GroupedHearingsDto(upcoming, past);
    }

    @Override
    public List<HearingDto> getMyHearings(String username) {
        return hearingRepository.findAllByClientUsername(username)
                .stream().map(HearingDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<HearingDto> getHearingsForCase(String username, Long caseId) {
        caseRepository.findByIdAndClientUsername(caseId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
        return hearingRepository.findByCaseIdAndClientUsername(caseId, username)
                .stream().map(HearingDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public HearingDto getHearingDetail(String username, Long hearingId) {
        Hearing h = hearingRepository.findByIdAndClientUsername(hearingId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hearing not found"));
        return HearingDto.fromEntity(h);
    }

    // ── Documents ─────────────────────────────────────────────────────────────
    @Override
    public List<DocumentDto> getMyDocuments(String username) {
        return documentRepository.findAllByClientUsername(username)
                .stream().map(DocumentDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<DocumentDto> getDocumentsForCase(String username, Long caseId) {
        caseRepository.findByIdAndClientUsername(caseId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
        return documentRepository.findByCaseIdAndClientUsername(caseId, username)
                .stream().map(DocumentDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public DocumentDto getDocumentDetail(String username, Long documentId) {
        Document d = documentRepository.findByIdAndClientUsername(documentId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return DocumentDto.fromEntity(d);
    }

    @Override
    @Transactional(readOnly = false)
    public DocumentDto uploadDocument(String username, Long caseId, MultipartFile file,
                                      String title, String description, String documentType) {
        User user = resolveUser(username);
        Case c = caseRepository.findByIdAndClientUsername(caseId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));

        if (file == null || file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        if (file.getSize() > 10 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size must not exceed 10 MB");

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String extension    = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase() : "";

        if (!List.of(".pdf",".doc",".docx",".jpg",".jpeg",".png",".xlsx",".xls",".txt").contains(extension))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type");

        String storedName = UUID.randomUUID() + extension;
        Path uploadPath   = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), uploadPath.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file: " + e.getMessage());
        }

        Document.DocumentType docType;
        try { docType = documentType != null ? Document.DocumentType.valueOf(documentType.toUpperCase()) : Document.DocumentType.OTHER; }
        catch (IllegalArgumentException e) { docType = Document.DocumentType.OTHER; }

        Document doc = new Document();
        doc.setTitle(title != null && !title.isBlank() ? title : originalName);
        doc.setDescription(description);
        doc.setFileName(originalName);
        doc.setFilePath(uploadPath.resolve(storedName).toString());
        doc.setFileType(extension.replace(".", "").toUpperCase());
        doc.setFileSize(file.getSize());
        doc.setDocumentType(docType);
        doc.setStatus(Document.DocumentStatus.UPLOADED);
        doc.setCaseEntity(c);
        doc.setUploadedByUser(user);
        doc.setUploadedBy(user.getId());
        doc.setUploadedAt(LocalDateTime.now());
        return DocumentDto.fromEntity(documentRepository.save(doc));
    }

    @Override
    public List<DocumentDto> searchDocuments(String username, String keyword) {
        if (keyword == null || keyword.isBlank()) return getMyDocuments(username);
        return documentRepository.searchByClientUsername(username, keyword.trim())
                .stream().map(DocumentDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<DocumentDto> getDocumentsByType(String username, String documentType) {
        try {
            Document.DocumentType type = Document.DocumentType.valueOf(documentType.toUpperCase());
            return documentRepository.findByClientUsernameAndType(username, type)
                    .stream().map(DocumentDto::fromEntity).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document type");
        }
    }

    // ── Messages ──────────────────────────────────────────────────────────────
    @Override
    public List<MessageDto.ConversationSummary> getConversationList(String username) {
        User user = resolveUser(username);
        List<Case> cases = caseRepository.findByClientUsername(username);
        List<MessageDto.ConversationSummary> summaries = new ArrayList<>();

        for (Case c : cases) {
            Optional<Message> latest = messageRepository.findLatestByCaseId(c.getId());
            if (latest.isEmpty()) continue;
            Message lastMsg = latest.get();
            long unread = messageRepository.countUnreadByCaseIdAndReceiverUsername(c.getId(), username);

            MessageDto.ConversationSummary summary = new MessageDto.ConversationSummary();
            summary.setCaseId(c.getId());
            summary.setCaseTitle(c.getTitle());
            summary.setCaseNumber(c.getCaseNumber());
            summary.setLawyerName(c.getAssignedLawyer() != null
                    ? c.getAssignedLawyer().getFirstName() + " " + c.getAssignedLawyer().getLastName()
                    : "Unassigned");
            String preview = lastMsg.getContent();
            if (preview != null && preview.length() > 60) preview = preview.substring(0, 60) + "…";
            summary.setLastMessage(preview);
            summary.setLastMessageType(lastMsg.getType().name());
            summary.setLastMessageAt(lastMsg.getCreatedAt());
            summary.setLastMessageIsFromMe(lastMsg.getSender() != null &&
                    lastMsg.getSender().getUsername().equals(username));
            summary.setUnreadCount(unread);
            summaries.add(summary);
        }

        summaries.sort(Comparator.comparing(MessageDto.ConversationSummary::getLastMessageAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return summaries;
    }

    @Override
    @Transactional(readOnly = false)
    public List<MessageDto> getConversation(String username, Long caseId) {
        User user = resolveUser(username);
        Case c = caseRepository.findByIdAndClientUsername(caseId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
        Long lawyerId = c.getAssignedLawyer() != null ? c.getAssignedLawyer().getId() : -1L;
        List<Message> messages = messageRepository.findConversation(caseId, user.getId(), lawyerId);
        if (c.getAssignedLawyer() != null)
            messageRepository.markConversationAsRead(caseId, lawyerId, user.getId());
        return messages.stream().map(MessageDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = false)
    public MessageDto sendMessage(String username, SendMessageRequest request) {
        User sender = resolveUser(username);
        Case c = caseRepository.findByIdAndClientUsername(request.getCaseId(), username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver not found"));
        if (c.getAssignedLawyer() == null || !c.getAssignedLawyer().getId().equals(receiver.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only message the assigned lawyer");

        Message.MessageType msgType;
        try { msgType = request.getType() != null ? Message.MessageType.valueOf(request.getType().toUpperCase()) : Message.MessageType.TEXT; }
        catch (IllegalArgumentException e) { msgType = Message.MessageType.TEXT; }

        Message msg = new Message();
        msg.setContent(request.getContent());
        msg.setType(msgType);
        msg.setIsRead(false);
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setCaseEntity(c);
        msg.setAttachmentUrl(request.getAttachmentUrl());
        msg.setCreatedAt(LocalDateTime.now());
        return MessageDto.fromEntity(messageRepository.save(msg));
    }

    @Override
    public Long getUnreadCount(String username) {
        return messageRepository.countUnreadByReceiverUsername(username);
    }

    // ── Billing ───────────────────────────────────────────────────────────────
    @Override
    public InvoiceDto.BillingSummary getBillingSummary(String username) {
        double totalDue  = invoiceRepository.getTotalDueByClientUsername(username);
        double totalPaid = invoiceRepository.getTotalPaidByClientUsername(username);
        long unpaid   = invoiceRepository.countByClientUsernameAndStatus(username, Invoice.InvoiceStatus.UNPAID);
        long overdue  = invoiceRepository.countByClientUsernameAndStatus(username, Invoice.InvoiceStatus.OVERDUE);
        long paid     = invoiceRepository.countByClientUsernameAndStatus(username, Invoice.InvoiceStatus.PAID);
        long partial  = invoiceRepository.countByClientUsernameAndStatus(username, Invoice.InvoiceStatus.PARTIALLY_PAID);
        // 6-arg constructor: totalDue, totalPaid, unpaid, overdue, paid, partial
        return new InvoiceDto.BillingSummary(totalDue, totalPaid, unpaid, overdue, paid, partial);
    }

    @Override
    public List<InvoiceDto> getMyInvoices(String username) {
        return invoiceRepository.findAllByClientUsername(username)
                .stream().map(InvoiceDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<InvoiceDto> getInvoicesForCase(String username, Long caseId) {
        caseRepository.findByIdAndClientUsername(caseId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
        return invoiceRepository.findByCaseIdAndClientUsername(caseId, username)
                .stream().map(InvoiceDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public InvoiceDto getInvoiceDetail(String username, Long invoiceId) {
        Invoice inv = invoiceRepository.findByIdAndClientUsername(invoiceId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        return InvoiceDto.fromEntity(inv);
    }

    @Override
    public List<InvoiceDto> getPendingInvoices(String username) {
        return invoiceRepository.findPendingByClientUsername(username)
                .stream().map(InvoiceDto::fromEntity).collect(Collectors.toList());
    }

    // ── Profile ───────────────────────────────────────────────────────────────
    @Override
    public UserDto getProfile(String username) {
        return UserDto.fromEntity(resolveUser(username));
    }

    @Override
    @Transactional(readOnly = false)
    public UserDto updateProfile(String username, UpdateProfileRequest request) {
        User user = resolveUser(username);
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
            userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId()))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Email is already in use by another account");
            });
        }
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        return UserDto.fromEntity(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = false)
    public void changePassword(String username, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New password and confirm password do not match");
        User user = resolveUser(username);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "New password must be different from your current password");
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}