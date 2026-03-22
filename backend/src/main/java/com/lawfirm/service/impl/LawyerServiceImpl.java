package com.lawfirm.service.impl;

import com.lawfirm.dto.*;
import com.lawfirm.entity.*;
import com.lawfirm.repository.*;
import com.lawfirm.service.LawyerService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LawyerServiceImpl implements LawyerService {

    @Autowired private CaseRepository        caseRepository;
    @Autowired private CaseRequestRepository caseRequestRepository;
    @Autowired private HearingRepository     hearingRepository;
    @Autowired private DocumentRepository    documentRepository;
    @Autowired private MessageRepository     messageRepository;
    @Autowired private InvoiceRepository     invoiceRepository;
    @Autowired private UserRepository        userRepository;
    @Autowired private PasswordEncoder       passwordEncoder;

    @Value("${app.upload.dir:uploads/documents}")
    private String uploadDir;

    // ── Dashboard ─────────────────────────────────────────────────────────────
    @Override
    public LawyerDashboardDto getDashboard(String username) {
        List<Case> cases = caseRepository.findByLawyerUsername(username);
        LocalDateTime now   = LocalDateTime.now();
        LocalDateTime start = now.toLocalDate().atStartOfDay();
        LocalDateTime end   = start.plusDays(1);

        int open       = (int) cases.stream().filter(c -> c.getStatus() == Case.CaseStatus.OPEN).count();
        int inProgress = (int) cases.stream().filter(c -> c.getStatus() == Case.CaseStatus.IN_PROGRESS).count();
        int closed     = (int) cases.stream().filter(c -> c.getStatus() == Case.CaseStatus.CLOSED).count();
        double fees    = cases.stream().filter(c -> c.getFeesCharged() != null).mapToDouble(Case::getFeesCharged).sum();

        List<Hearing> upcoming = hearingRepository.findUpcomingByLawyerUsername(username, now);
        List<Hearing> today    = hearingRepository.findTodayByLawyerUsername(username, start, end);
        long unread     = messageRepository.countUnreadByReceiverUsername(username);
        long pendingInv = invoiceRepository.countByLawyerUsernameAndStatus(username, Invoice.InvoiceStatus.UNPAID)
                        + invoiceRepository.countByLawyerUsernameAndStatus(username, Invoice.InvoiceStatus.OVERDUE);

        List<MessageDto.ConversationSummary> recentMsgs = buildConversationList(username, cases)
                .stream().limit(3).collect(Collectors.toList());

        LawyerDashboardDto dto = new LawyerDashboardDto();
        dto.setTotalCases(cases.size());
        dto.setOpenCases(open);
        dto.setInProgressCases(inProgress);
        dto.setClosedCases(closed);
        dto.setUpcomingHearings(upcoming.size());
        dto.setTodayHearings(today.size());
        dto.setUnreadMessages(unread);
        dto.setTotalFeesCharged(fees);
        dto.setPendingInvoices(pendingInv);
        dto.setUpcomingHearingsList(upcoming.stream().limit(5).map(HearingDto::fromEntity).collect(Collectors.toList()));
        dto.setRecentCases(cases.stream().limit(5).map(CaseDto::fromEntity).collect(Collectors.toList()));
        dto.setRecentMessages(recentMsgs);
        return dto;
    }

    // ── Cases ─────────────────────────────────────────────────────────────────
    @Override
    public List<CaseDto> getMyCases(String username) {
        return caseRepository.findByLawyerUsername(username)
                .stream().map(CaseDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public CaseDto getCaseDetail(String username, Long caseId) {
        return CaseDto.fromEntity(resolveCase(username, caseId));
    }

    @Override
    @Transactional(readOnly = false)
    public CaseDto updateCaseStatus(String username, Long caseId, String status) {
        Case c = resolveCase(username, caseId);
        try { c.setStatus(Case.CaseStatus.valueOf(status.toUpperCase())); }
        catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }
        if (c.getStatus() == Case.CaseStatus.CLOSED || c.getStatus() == Case.CaseStatus.SETTLED)
            c.setDateClosed(LocalDateTime.now());
        return CaseDto.fromEntity(caseRepository.save(c));
    }

    @Override
    @Transactional(readOnly = false)
    public CaseDto updateCaseNotes(String username, Long caseId, String notes) {
        Case c = resolveCase(username, caseId);
        c.setDescription(notes);
        return CaseDto.fromEntity(caseRepository.save(c));
    }

    // ── Case Requests ─────────────────────────────────────────────────────────
    @Override
    public List<CaseRequestDto> getPendingRequests(String username) {
        return caseRequestRepository.findAllPendingOrderedByUrgency()
                .stream().map(CaseRequestDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<CaseRequestDto> getPendingRequests(String username, String caseType) {
        if (caseType == null || caseType.isBlank()) return getPendingRequests(username);
        try {
            Case.CaseType type = Case.CaseType.valueOf(caseType.toUpperCase());
            return caseRequestRepository.findPendingByCaseType(type)
                    .stream().map(CaseRequestDto::fromEntity).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid case type");
        }
    }

    @Override
    public List<CaseRequestDto> getMyHandledRequests(String username) {
        return caseRequestRepository.findByLawyerUsernameOrderByUpdatedAtDesc(username)
                .stream().map(CaseRequestDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = false)
    public CaseDto acceptRequest(String username, Long requestId, AcceptCaseRequestDto dto) {
        User lawyer = resolveUser(username);
        CaseRequest request = caseRequestRepository.findPendingById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (caseRepository.findAll().stream().anyMatch(c -> dto.getCaseNumber().equals(c.getCaseNumber())))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Case number already exists");

        Case newCase = new Case();
        newCase.setCaseNumber(dto.getCaseNumber());
        newCase.setTitle(request.getTitle());
        newCase.setDescription(request.getDescription() +
                (dto.getNotes() != null ? "\n\nLawyer Notes: " + dto.getNotes() : ""));
        newCase.setCaseType(request.getCaseType());
        newCase.setStatus(Case.CaseStatus.OPEN);
        newCase.setClient(request.getClient());
        newCase.setAssignedLawyer(lawyer);
        newCase.setCourtName(dto.getCourtName());
        newCase.setJudgeName(dto.getJudgeName());
        newCase.setFeesCharged(dto.getFeesCharged());
        newCase.setDateOpened(LocalDateTime.now());
        Case saved = caseRepository.save(newCase);

        request.setStatus(CaseRequest.RequestStatus.ACCEPTED);
        request.setLawyer(lawyer);
        request.setCreatedCase(saved);
        request.setResolvedAt(LocalDateTime.now());
        caseRequestRepository.save(request);
        return CaseDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = false)
    public CaseRequestDto rejectRequest(String username, Long requestId, RejectCaseRequestDto dto) {
        User lawyer = resolveUser(username);
        CaseRequest request = caseRequestRepository.findPendingById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        request.setStatus(CaseRequest.RequestStatus.REJECTED);
        request.setLawyer(lawyer);
        request.setRejectionReason(dto.getReason());
        request.setResolvedAt(LocalDateTime.now());
        return CaseRequestDto.fromEntity(caseRequestRepository.save(request));
    }

    @Override
    public long getPendingRequestCount() {
        return caseRequestRepository.countByStatus(CaseRequest.RequestStatus.PENDING);
    }

    // ── Hearings ──────────────────────────────────────────────────────────────
    @Override
    public HearingDto.GroupedHearingsDto getGroupedHearings(String username) {
        LocalDateTime now = LocalDateTime.now();
        List<HearingDto> upcoming = hearingRepository.findUpcomingByLawyerUsername(username, now)
                .stream().map(HearingDto::fromEntity).collect(Collectors.toList());
        List<HearingDto> past = hearingRepository.findPastByLawyerUsername(username, now)
                .stream().map(HearingDto::fromEntity).collect(Collectors.toList());
        return new HearingDto.GroupedHearingsDto(upcoming, past);
    }

    @Override
    public List<HearingDto> getMyHearings(String username) {
        return hearingRepository.findAllByLawyerUsername(username)
                .stream().map(HearingDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<HearingDto> getTodayHearings(String username) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        return hearingRepository.findTodayByLawyerUsername(username, start, start.plusDays(1))
                .stream().map(HearingDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<HearingDto> getHearingsForCase(String username, Long caseId) {
        resolveCase(username, caseId);
        return hearingRepository.findByCaseIdAndLawyerUsername(caseId, username)
                .stream().map(HearingDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public HearingDto getHearingDetail(String username, Long hearingId) {
        return HearingDto.fromEntity(resolveHearing(username, hearingId));
    }

    @Override
    @Transactional(readOnly = false)
    public HearingDto createHearing(String username, CreateHearingDto dto) {
        Case c = resolveCase(username, dto.getCaseId());
        Hearing h = new Hearing();
        h.setTitle(dto.getTitle());
        h.setDescription(dto.getDescription());
        h.setHearingDate(dto.getHearingDate());
        h.setCourtName(dto.getCourtName());
        h.setCourtRoom(dto.getCourtRoom());
        h.setJudgeName(dto.getJudgeName());
        h.setNotes(dto.getNotes());
        h.setStatus(Hearing.HearingStatus.SCHEDULED);
        h.setCaseEntity(c);
        if (c.getNextHearingDate() == null || dto.getHearingDate().isBefore(c.getNextHearingDate()))
            c.setNextHearingDate(dto.getHearingDate());
        caseRepository.save(c);
        return HearingDto.fromEntity(hearingRepository.save(h));
    }

    @Override
    @Transactional(readOnly = false)
    public HearingDto updateHearing(String username, Long hearingId, CreateHearingDto dto) {
        Hearing h = resolveHearing(username, hearingId);
        if (dto.getTitle() != null)       h.setTitle(dto.getTitle());
        if (dto.getDescription() != null) h.setDescription(dto.getDescription());
        if (dto.getHearingDate() != null) h.setHearingDate(dto.getHearingDate());
        if (dto.getCourtName() != null)   h.setCourtName(dto.getCourtName());
        if (dto.getCourtRoom() != null)   h.setCourtRoom(dto.getCourtRoom());
        if (dto.getJudgeName() != null)   h.setJudgeName(dto.getJudgeName());
        if (dto.getNotes() != null)       h.setNotes(dto.getNotes());
        return HearingDto.fromEntity(hearingRepository.save(h));
    }

    @Override
    @Transactional(readOnly = false)
    public HearingDto updateHearingStatus(String username, Long hearingId, String status) {
        Hearing h = resolveHearing(username, hearingId);
        try { h.setStatus(Hearing.HearingStatus.valueOf(status.toUpperCase())); }
        catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid hearing status");
        }
        return HearingDto.fromEntity(hearingRepository.save(h));
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteHearing(String username, Long hearingId) {
        Hearing h = resolveHearing(username, hearingId);
        if (h.getStatus() != Hearing.HearingStatus.SCHEDULED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only SCHEDULED hearings can be deleted");
        hearingRepository.delete(h);
    }

    // ── Documents ─────────────────────────────────────────────────────────────
    @Override
    public List<DocumentDto> getMyDocuments(String username) {
        return documentRepository.findAllByLawyerUsername(username)
                .stream().map(DocumentDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<DocumentDto> getDocumentsForCase(String username, Long caseId) {
        resolveCase(username, caseId);
        return documentRepository.findByCaseIdAndLawyerUsername(caseId, username)
                .stream().map(DocumentDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public DocumentDto getDocumentDetail(String username, Long documentId) {
        return DocumentDto.fromEntity(resolveDocument(username, documentId));
    }

    @Override
    @Transactional(readOnly = false)
    public DocumentDto uploadDocument(String username, Long caseId, MultipartFile file,
                                      String title, String description, String documentType) {
        User lawyer = resolveUser(username);
        Case c = resolveCase(username, caseId);

        if (file == null || file.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        if (file.getSize() > 10 * 1024 * 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size must not exceed 10 MB");

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String extension    = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase() : "";

        List<String> allowed = List.of(".pdf",".doc",".docx",".jpg",".jpeg",".png",".xlsx",".xls",".txt");
        if (!allowed.contains(extension))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type");

        String storedName = UUID.randomUUID() + extension;
        Path uploadPath   = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), uploadPath.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file");
        }

        Document.DocumentType docType;
        try { docType = documentType != null
                ? Document.DocumentType.valueOf(documentType.toUpperCase())
                : Document.DocumentType.OTHER; }
        catch (IllegalArgumentException e) { docType = Document.DocumentType.OTHER; }

        Document doc = new Document();
        doc.setTitle(title != null && !title.isBlank() ? title : originalName);
        doc.setDescription(description);
        doc.setFileName(originalName);
        doc.setFilePath(uploadPath.resolve(storedName).toString());
        doc.setFileType(extension.replace(".", "").toUpperCase());
        doc.setFileSize(file.getSize());
        doc.setDocumentType(docType);
        doc.setStatus(Document.DocumentStatus.APPROVED);
        doc.setCaseEntity(c);
        doc.setUploadedByUser(lawyer);
        doc.setUploadedBy(lawyer.getId());
        doc.setUploadedAt(LocalDateTime.now());
        return DocumentDto.fromEntity(documentRepository.save(doc));
    }

    @Override
    public List<DocumentDto> searchDocuments(String username, String keyword) {
        if (keyword == null || keyword.isBlank()) return getMyDocuments(username);
        return documentRepository.searchByLawyerUsername(username, keyword.trim())
                .stream().map(DocumentDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public List<DocumentDto> getDocumentsByType(String username, String documentType) {
        try {
            Document.DocumentType type = Document.DocumentType.valueOf(documentType.toUpperCase());
            return documentRepository.findByLawyerUsernameAndType(username, type)
                    .stream().map(DocumentDto::fromEntity).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document type");
        }
    }

    @Override
    public List<DocumentDto> getDocumentsByStatus(String username, String status) {
        try {
            Document.DocumentStatus s = Document.DocumentStatus.valueOf(status.toUpperCase());
            return documentRepository.findByLawyerUsernameAndStatus(username, s)
                    .stream().map(DocumentDto::fromEntity).collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document status");
        }
    }

    @Override
    @Transactional(readOnly = false)
    public DocumentDto updateDocumentStatus(String username, Long documentId, UpdateDocumentStatusDto dto) {
        Document doc = resolveDocument(username, documentId);
        Document.DocumentStatus newStatus;
        try { newStatus = Document.DocumentStatus.valueOf(dto.getStatus().toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + dto.getStatus());
        }
        doc.setStatus(newStatus);
        if (dto.getRejectionNote() != null && !dto.getRejectionNote().isBlank()) {
            String existing = doc.getDescription() != null ? doc.getDescription() + "\n\n" : "";
            doc.setDescription(existing + "Rejection note: " + dto.getRejectionNote());
        }
        return DocumentDto.fromEntity(documentRepository.save(doc));
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteDocument(String username, Long documentId) {
        Document doc = resolveDocument(username, documentId);
        if (doc.getStatus() != Document.DocumentStatus.UPLOADED
                && doc.getStatus() != Document.DocumentStatus.REJECTED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only UPLOADED or REJECTED documents can be deleted");
        try { Files.deleteIfExists(Paths.get(doc.getFilePath())); } catch (IOException ignored) {}
        documentRepository.delete(doc);
    }

    // ── Messages ──────────────────────────────────────────────────────────────
    @Override
    public List<MessageDto.ConversationSummary> getConversationList(String username) {
        return buildConversationList(username, caseRepository.findByLawyerUsername(username));
    }

    @Override
    @Transactional(readOnly = false)
    public List<MessageDto> getConversation(String username, Long caseId) {
        User lawyer = resolveUser(username);
        Case c = resolveCase(username, caseId);
        Long clientId = c.getClient() != null ? c.getClient().getId() : -1L;
        List<Message> messages = messageRepository.findConversation(caseId, lawyer.getId(), clientId);
        if (c.getClient() != null)
            messageRepository.markConversationAsRead(caseId, clientId, lawyer.getId());
        return messages.stream().map(MessageDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = false)
    public MessageDto sendMessage(String username, SendMessageRequest request) {
        User sender = resolveUser(username);
        Case c = resolveCase(username, request.getCaseId());
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receiver not found"));
        if (c.getClient() == null || !c.getClient().getId().equals(receiver.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only message the client on this case");

        Message.MessageType type;
        try { type = request.getType() != null
                ? Message.MessageType.valueOf(request.getType().toUpperCase())
                : Message.MessageType.TEXT; }
        catch (IllegalArgumentException e) { type = Message.MessageType.TEXT; }

        Message msg = new Message();
        msg.setContent(request.getContent());
        msg.setType(type);
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

    // ── Clients ───────────────────────────────────────────────────────────────
    @Override
    public List<UserDto> getMyClients(String username) {
        return caseRepository.findClientsByLawyerUsername(username)
                .stream().map(UserDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public UserDto getClientDetail(String username, Long clientId) {
        boolean hasAccess = caseRepository.findByLawyerUsername(username)
                .stream().anyMatch(c -> c.getClient() != null && c.getClient().getId().equals(clientId));
        if (!hasAccess) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        return UserDto.fromEntity(userRepository.findById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found")));
    }

    // ── Billing ───────────────────────────────────────────────────────────────
    @Override
    public InvoiceDto.BillingSummary getBillingSummary(String username) {
        double totalDue  = invoiceRepository.getTotalDueByLawyerUsername(username);
        double totalPaid = invoiceRepository.getTotalPaidByLawyerUsername(username);
        long unpaid   = invoiceRepository.countByLawyerUsernameAndStatus(username, Invoice.InvoiceStatus.UNPAID);
        long overdue  = invoiceRepository.countByLawyerUsernameAndStatus(username, Invoice.InvoiceStatus.OVERDUE);
        long paid     = invoiceRepository.countByLawyerUsernameAndStatus(username, Invoice.InvoiceStatus.PAID);
        long partial  = invoiceRepository.countByLawyerUsernameAndStatus(username, Invoice.InvoiceStatus.PARTIALLY_PAID);
        return new InvoiceDto.BillingSummary(totalDue, totalPaid, unpaid, overdue, paid, partial);
    }

    @Override
    public List<InvoiceDto> getMyInvoices(String username) {
        return invoiceRepository.findAllByLawyerUsername(username)
                .stream().map(InvoiceDto::fromEntity).collect(Collectors.toList());
    }

    @Override
    public InvoiceDto getInvoiceDetail(String username, Long invoiceId) {
        return InvoiceDto.fromEntity(
            invoiceRepository.findByIdAndLawyerUsername(invoiceId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found")));
    }

    @Override
    @Transactional(readOnly = false)
    public InvoiceDto createInvoice(String username, CreateInvoiceDto dto) {
        User lawyer = resolveUser(username);
        Case c      = resolveCase(username, dto.getCaseId());

        if (c.getClient() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case has no client assigned.");

        // Generate invoice number: INV-YYYY-{sequence}
        long seq           = invoiceRepository.findMaxId() + 1;
        String invoiceNumber = String.format("INV-%d-%04d", LocalDate.now().getYear(), seq);

        // Resolve invoice type
        Invoice.InvoiceType invoiceType;
        try {
            invoiceType = dto.getInvoiceType() != null
                    ? Invoice.InvoiceType.valueOf(dto.getInvoiceType().toUpperCase())
                    : Invoice.InvoiceType.FEES;
        } catch (IllegalArgumentException e) {
            invoiceType = Invoice.InvoiceType.FEES;
        }

        double taxAmount   = dto.getTaxAmount()  != null ? dto.getTaxAmount()  : 0.0;
        double totalAmount = dto.getAmount() + taxAmount;

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setTitle(dto.getTitle());
        invoice.setDescription(dto.getDescription());
        invoice.setInvoiceType(invoiceType);
        invoice.setAmount(dto.getAmount());
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalAmount);
        invoice.setPaidAmount(0.0);
        invoice.setStatus(Invoice.InvoiceStatus.UNPAID);
        invoice.setDueDate(dto.getDueDate() != null
                ? dto.getDueDate()
                : LocalDate.now().plusDays(30));
        invoice.setNotes(dto.getNotes());
        invoice.setCaseEntity(c);
        invoice.setClient(c.getClient());
        invoice.setLawyer(lawyer);

        // Keep feesCharged on the case in sync
        if (c.getFeesCharged() == null) c.setFeesCharged(0.0);
        c.setFeesCharged(c.getFeesCharged() + totalAmount);
        caseRepository.save(c);

        return InvoiceDto.fromEntity(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional(readOnly = false)
    public InvoiceDto updateInvoiceStatus(String username, Long invoiceId, UpdateInvoiceStatusDto dto) {
        Invoice invoice = invoiceRepository.findByIdAndLawyerUsername(invoiceId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        Invoice.InvoiceStatus newStatus;
        try { newStatus = Invoice.InvoiceStatus.valueOf(dto.getStatus().toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + dto.getStatus());
        }

        invoice.setStatus(newStatus);

        switch (newStatus) {
            case PAID -> {
                invoice.setPaidAmount(invoice.getTotalAmount());
                invoice.setPaidDate(dto.getPaidDate() != null ? dto.getPaidDate() : LocalDate.now());
                if (dto.getPaymentMethod() != null)    invoice.setPaymentMethod(dto.getPaymentMethod());
                if (dto.getPaymentReference() != null) invoice.setPaymentReference(dto.getPaymentReference());
            }
            case PARTIALLY_PAID -> {
                if (dto.getPaidAmount() == null || dto.getPaidAmount() <= 0)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "paidAmount is required for PARTIALLY_PAID status.");
                if (dto.getPaidAmount() > invoice.getTotalAmount())
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "paidAmount cannot exceed totalAmount.");
                invoice.setPaidAmount(dto.getPaidAmount());
                if (dto.getPaymentMethod() != null)    invoice.setPaymentMethod(dto.getPaymentMethod());
                if (dto.getPaymentReference() != null) invoice.setPaymentReference(dto.getPaymentReference());
            }
            case UNPAID, OVERDUE, CANCELLED, WAIVED -> {
                invoice.setPaidAmount(0.0);
                invoice.setPaidDate(null);
                invoice.setPaymentMethod(null);
                invoice.setPaymentReference(null);
            }
        }

        return InvoiceDto.fromEntity(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteInvoice(String username, Long invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndLawyerUsername(invoiceId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paid invoices cannot be deleted.");

        // Reverse feesCharged on the case
        Case c = invoice.getCaseEntity();
        if (c != null && c.getFeesCharged() != null) {
            c.setFeesCharged(Math.max(0.0, c.getFeesCharged() - invoice.getTotalAmount()));
            caseRepository.save(c);
        }

        invoiceRepository.delete(invoice);
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
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        User user = resolveUser(username);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different");
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private User resolveUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Case resolveCase(String username, Long caseId) {
        return caseRepository.findByIdAndLawyerUsername(caseId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
    }

    private Hearing resolveHearing(String username, Long hearingId) {
        return hearingRepository.findByIdAndLawyerUsername(hearingId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hearing not found"));
    }

    private Document resolveDocument(String username, Long documentId) {
        return documentRepository.findByIdAndLawyerUsername(documentId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    private List<MessageDto.ConversationSummary> buildConversationList(String username, List<Case> cases) {
        List<MessageDto.ConversationSummary> summaries = new ArrayList<>();
        for (Case c : cases) {
            Optional<Message> latest = messageRepository.findLatestByCaseId(c.getId());
            if (latest.isEmpty()) continue;
            Message lastMsg = latest.get();
            long unread = messageRepository.countUnreadByCaseIdAndReceiverUsername(c.getId(), username);
            MessageDto.ConversationSummary s = new MessageDto.ConversationSummary();
            s.setCaseId(c.getId());
            s.setCaseTitle(c.getTitle());
            s.setCaseNumber(c.getCaseNumber());
            s.setLawyerName(c.getClient() != null
                    ? c.getClient().getFirstName() + " " + c.getClient().getLastName() : "Unknown Client");
            String preview = lastMsg.getContent();
            if (preview != null && preview.length() > 60) preview = preview.substring(0, 60) + "…";
            s.setLastMessage(preview);
            s.setLastMessageType(lastMsg.getType().name());
            s.setLastMessageAt(lastMsg.getCreatedAt());
            s.setLastMessageIsFromMe(lastMsg.getSender() != null &&
                    lastMsg.getSender().getUsername().equals(username));
            s.setUnreadCount(unread);
            summaries.add(s);
        }
        summaries.sort(Comparator.comparing(MessageDto.ConversationSummary::getLastMessageAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return summaries;
    }
//     @Override
// public List<Object[]> getMonthlyEarnings(String username, int year) {

//     List<Object[]> result = invoiceRepository.getMonthlyEarnings(username, year);

//     // Optional: Fill missing months with 0
//     Map<Integer, Double> monthMap = new HashMap<>();

//     for (Object[] row : result) {
//         Integer month = (Integer) row[0];
//         Double amount = (Double) row[1];
//         monthMap.put(month, amount);
//     }

//     List<Object[]> finalResult = new ArrayList<>();

//     for (int i = 1; i <= 12; i++) {
//         Double amount = monthMap.getOrDefault(i, 0.0);
//         finalResult.add(new Object[]{i, amount});
//     }

//     return finalResult;
// }
}