import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { PageResponse } from '../shared/page.model';
import { GenerationPreview, GenerationRequest, GenerationResult, Schedule, ScheduleRequest } from './schedule.models';

@Injectable({ providedIn: 'root' })
export class ScheduleService {
  private readonly http = inject(HttpClient);
  list(batchId: number, page = 0, size = 20) {
    return this.http.get<PageResponse<Schedule>>(`/api/v1/batches/${batchId}/schedules`, { params: new HttpParams().set('page', page).set('size', size) });
  }
  save(batchId: number, request: ScheduleRequest, id?: number) {
    const url = `/api/v1/batches/${batchId}/schedules`;
    return id === undefined ? this.http.post<Schedule>(url, request) : this.http.put<Schedule>(`${url}/${id}`, request);
  }
  deactivate(batchId: number, schedule: Schedule) {
    return this.http.delete<void>(`/api/v1/batches/${batchId}/schedules/${schedule.id}`, { params: new HttpParams().set('version', schedule.version) });
  }
  preview(batchId: number, request: GenerationRequest) {
    return this.http.post<GenerationPreview>(`/api/v1/batches/${batchId}/sessions/preview`, request);
  }
  generate(batchId: number, request: GenerationRequest, previewToken: string) {
    return this.http.post<GenerationResult>(`/api/v1/batches/${batchId}/sessions/generate`, { ...request, previewToken });
  }
}
