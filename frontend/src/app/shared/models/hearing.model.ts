// src/app/shared/models/hearing.model.ts

export interface HearingDto {
  id:          number;
  title:       string;
  description: string | null;
  hearingDate: string;        // ISO datetime string
  courtName:   string | null;
  courtRoom:   string | null;
  judgeName:   string | null;
  status:      HearingStatus;
  notes:       string | null;
  caseId:      number | null;
  caseNumber:  string | null;
  caseTitle:   string | null;
}

export interface GroupedHearingsDto {
  upcoming:      HearingDto[];
  past:          HearingDto[];
  totalUpcoming: number;
  totalPast:     number;
}

export type HearingStatus = 'SCHEDULED' | 'COMPLETED' | 'POSTPONED' | 'CANCELLED';