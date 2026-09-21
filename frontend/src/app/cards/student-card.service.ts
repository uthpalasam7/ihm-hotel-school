import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { PageResponse } from '../shared/page.model';

export interface StudentCard {
  id: number;
  studentId: number;
  identifier: string;
  status: 'ACTIVE' | 'REVOKED';
  issuedAt: string;
  revokedAt: string | null;
  version: number;
  contactLine: string;
}
export interface CardDelivery {
  id: number;
  documentType: string;
  documentId: number;
  recipientEmail: string;
  status: 'QUEUED' | 'SENDING' | 'ACCEPTED' | 'FAILED';
  attempts: number;
  nextAttemptAt: string;
  providerAcceptedAt: string | null;
  lastError: string | null;
  createdAt: string;
}
export interface StudentCardEvent {
  id: number;
  action: 'ISSUED' | 'REPLACED' | 'REVOKED';
  reason: string | null;
  occurredAt: string;
  actorId: number;
}

@Injectable({ providedIn: 'root' })
export class StudentCardService {
  private readonly http = inject(HttpClient);
  private path(studentId: number) { return `/api/v1/students/${studentId}/card`; }
  get(studentId: number) { return this.http.get<StudentCard | null>(this.path(studentId)); }
  issue(studentId: number) { return this.http.post<StudentCard>(this.path(studentId), {}); }
  replace(studentId: number, reason: string) { return this.http.post<StudentCard>(`${this.path(studentId)}/replace`, { reason }); }
  revoke(studentId: number, reason: string) { return this.http.post<StudentCard>(`${this.path(studentId)}/revoke`, { reason }); }
  qr(studentId: number) { return this.http.get(`${this.path(studentId)}/qr`, { responseType: 'blob' }); }
  pdf(studentId: number) { return this.http.get(`${this.path(studentId)}/pdf`, { responseType: 'blob' }); }
  emailAvailability() { return this.http.get<{ available: boolean }>('/api/v1/document-deliveries/availability'); }
  email(studentId: number, idempotencyKey: string) { return this.http.post<CardDelivery>(`${this.path(studentId)}/email`, { idempotencyKey }); }
  deliveries(studentId: number) { return this.http.get<PageResponse<CardDelivery>>(`${this.path(studentId)}/deliveries`, { params: { page: 0, size: 20 } }); }
  history(studentId: number) { return this.http.get<PageResponse<StudentCardEvent>>(`${this.path(studentId)}/history`, { params: { page: 0, size: 20 } }); }
}
