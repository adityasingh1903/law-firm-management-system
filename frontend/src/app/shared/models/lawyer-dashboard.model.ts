// src/app/shared/models/lawyer-dashboard.model.ts

import { HearingDto } from './hearing.model';
import { ConversationSummary } from './message.model';

export interface LawyerDashboardDto {
  totalCases:        number;
  openCases:         number;
  inProgressCases:   number;
  closedCases:       number;
  upcomingHearings:  number;
  todayHearings:     number;
  unreadMessages:    number;
  totalFeesCharged:  number;
  pendingInvoices:   number;
  upcomingHearingsList: HearingDto[];
  recentCases:       CaseSummary[];
  recentMessages:    ConversationSummary[];
}

export interface CaseSummary {
  id:            number;
  caseNumber:    string;
  title:         string;
  status:        string;
  caseType:      string | null;
  clientName:    string | null;
  clientEmail:   string | null;
  courtName:     string | null;
  feesCharged:   number | null;
  updatedAt:     string;
  nextHearingDate: string | null;
}