package com.lawfirm.dto;

import java.util.List;

public class LawyerDashboardDto {

    // ── Case stats ────────────────────────────────────────────────────────────
    private int totalCases;
    private int openCases;
    private int inProgressCases;
    private int closedCases;

    // ── Hearing stats ─────────────────────────────────────────────────────────
    private int upcomingHearings;   // next 30 days
    private int todayHearings;      // today only

    // ── Communication ─────────────────────────────────────────────────────────
    private long unreadMessages;

    // ── Financial ─────────────────────────────────────────────────────────────
    private double totalFeesCharged;
    private long pendingInvoices;

    // ── Lists ─────────────────────────────────────────────────────────────────
    private List<HearingDto>     upcomingHearingsList;  // next 5
    private List<CaseDto>        recentCases;           // last 5 updated
    private List<MessageDto.ConversationSummary> recentMessages; // last 3

    // ── Getters & Setters ─────────────────────────────────────────────────────
    public int getTotalCases()        { return totalCases; }
    public void setTotalCases(int v)  { this.totalCases = v; }

    public int getOpenCases()         { return openCases; }
    public void setOpenCases(int v)   { this.openCases = v; }

    public int getInProgressCases()           { return inProgressCases; }
    public void setInProgressCases(int v)     { this.inProgressCases = v; }

    public int getClosedCases()       { return closedCases; }
    public void setClosedCases(int v) { this.closedCases = v; }

    public int getUpcomingHearings()          { return upcomingHearings; }
    public void setUpcomingHearings(int v)    { this.upcomingHearings = v; }

    public int getTodayHearings()             { return todayHearings; }
    public void setTodayHearings(int v)       { this.todayHearings = v; }

    public long getUnreadMessages()           { return unreadMessages; }
    public void setUnreadMessages(long v)     { this.unreadMessages = v; }

    public double getTotalFeesCharged()       { return totalFeesCharged; }
    public void setTotalFeesCharged(double v) { this.totalFeesCharged = v; }

    public long getPendingInvoices()          { return pendingInvoices; }
    public void setPendingInvoices(long v)    { this.pendingInvoices = v; }

    public List<HearingDto> getUpcomingHearingsList()               { return upcomingHearingsList; }
    public void setUpcomingHearingsList(List<HearingDto> v)         { this.upcomingHearingsList = v; }

    public List<CaseDto> getRecentCases()                           { return recentCases; }
    public void setRecentCases(List<CaseDto> v)                     { this.recentCases = v; }

    public List<MessageDto.ConversationSummary> getRecentMessages() { return recentMessages; }
    public void setRecentMessages(List<MessageDto.ConversationSummary> v) { this.recentMessages = v; }
}