import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { PageResponse } from '../shared/page.model';
import { ClassSession, SessionFilters, SessionRequest, SessionRescheduleRequest, SessionRescheduleResponse } from './session.models';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly http = inject(HttpClient);

  list(filters: SessionFilters = {}) {
    let params = new HttpParams().set('page', filters.page ?? 0).set('size', filters.size ?? 20);
    for (const key of ['batchId', 'lecturerId', 'dateFrom', 'dateTo', 'status'] as const) {
      const value = filters[key];
      if (value !== undefined && value !== '') params = params.set(key, value);
    }
    return this.http.get<PageResponse<ClassSession>>('/api/v1/sessions', { params });
  }

  get(id: number) { return this.http.get<ClassSession>(`/api/v1/sessions/${id}`); }
  create(body: SessionRequest) { return this.http.post<ClassSession>('/api/v1/sessions', body); }
  update(id: number, body: SessionRequest) { return this.http.put<ClassSession>(`/api/v1/sessions/${id}`, body); }
  cancel(id: number, reason: string, version: number) {
    return this.http.post<ClassSession>(`/api/v1/sessions/${id}/cancel`, { reason, version });
  }
  reschedule(id: number, body: SessionRescheduleRequest) {
    return this.http.post<SessionRescheduleResponse>(`/api/v1/sessions/${id}/reschedule`, body);
  }
}
