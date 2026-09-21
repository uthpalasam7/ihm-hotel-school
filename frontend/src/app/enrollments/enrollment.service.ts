import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { PageResponse } from '../shared/page.model';
import { BatchStudent, Enrollment, EnrollmentPreview, EnrollmentRequest, EnrollmentStatus, StudentCharge } from './enrollment.models';

@Injectable({ providedIn: 'root' })
export class EnrollmentService {
  private readonly http = inject(HttpClient);
  list(filters: { search?: string; studentId?: number; batchId?: number; status?: string; page?: number; size?: number } = {}) {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== undefined && value !== '') params = params.set(key, value);
    }
    return this.http.get<PageResponse<Enrollment>>('/api/v1/enrollments', { params });
  }
  get(id: number) { return this.http.get<Enrollment>(`/api/v1/enrollments/${id}`); }
  preview(request: EnrollmentRequest) { return this.http.post<EnrollmentPreview>('/api/v1/enrollments/preview', request); }
  create(request: EnrollmentRequest) { return this.http.post<Enrollment>('/api/v1/enrollments', request); }
  changeStatus(id: number, status: EnrollmentStatus, reason: string, version: number) {
    return this.http.patch<Enrollment>(`/api/v1/enrollments/${id}/status`, { status, reason, version });
  }
  charges(id: number, page = 0, size = 20) {
    return this.http.get<PageResponse<StudentCharge>>(`/api/v1/enrollments/${id}/charges`, { params: { page, size } });
  }
  batchStudents(batchId: number, page = 0, size = 20) {
    return this.http.get<PageResponse<BatchStudent>>(`/api/v1/batches/${batchId}/students`, { params: { page, size } });
  }
}
