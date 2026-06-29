import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../shared/page.model';
import {
  Batch,
  BatchLecturer,
  BatchLecturerRequest,
  BatchLecturerStatus,
  BatchLecturerSyncRequest,
  BatchRequest,
  BatchStatus,
  FeePlan,
  FeePlanRequest,
  InstallmentPreview,
} from './batch.models';

@Injectable({ providedIn: 'root' })
export class BatchService {
  private readonly http = inject(HttpClient);

  list(filters: {
    search?: string;
    branchId?: number | string;
    courseId?: number | string;
    status?: string;
    startDateFrom?: string;
    startDateTo?: string;
    page?: number;
    size?: number;
  } = {}): Observable<PageResponse<Batch>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20);
    if (filters.search) {
      params = params.set('search', filters.search);
    }
    if (filters.branchId) {
      params = params.set('branchId', filters.branchId);
    }
    if (filters.courseId) {
      params = params.set('courseId', filters.courseId);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.startDateFrom) {
      params = params.set('startDateFrom', filters.startDateFrom);
    }
    if (filters.startDateTo) {
      params = params.set('startDateTo', filters.startDateTo);
    }
    return this.http.get<PageResponse<Batch>>('/api/v1/batches', { params });
  }

  get(id: number): Observable<Batch> {
    return this.http.get<Batch>(`/api/v1/batches/${id}`);
  }

  create(request: BatchRequest): Observable<Batch> {
    return this.http.post<Batch>('/api/v1/batches', request);
  }

  update(id: number, request: BatchRequest): Observable<Batch> {
    return this.http.put<Batch>(`/api/v1/batches/${id}`, request);
  }

  changeStatus(id: number, status: BatchStatus, reason: string): Observable<Batch> {
    return this.http.patch<Batch>(`/api/v1/batches/${id}/status`, { status, reason });
  }

  getFeePlan(batchId: number): Observable<FeePlan> {
    return this.http.get<FeePlan>(`/api/v1/batches/${batchId}/fee-plan`);
  }

  saveFeePlan(batchId: number, request: FeePlanRequest): Observable<FeePlan> {
    return this.http.put<FeePlan>(`/api/v1/batches/${batchId}/fee-plan`, request);
  }

  previewFeePlan(batchId: number, request: FeePlanRequest): Observable<InstallmentPreview> {
    return this.http.post<InstallmentPreview>(`/api/v1/batches/${batchId}/fee-plan/installment-preview`, request);
  }

  lecturers(batchId: number): Observable<BatchLecturer[]> {
    return this.http.get<BatchLecturer[]>(`/api/v1/batches/${batchId}/lecturers`);
  }

  addLecturer(batchId: number, request: BatchLecturerRequest): Observable<BatchLecturer> {
    return this.http.post<BatchLecturer>(`/api/v1/batches/${batchId}/lecturers`, request);
  }

  syncLecturers(batchId: number, request: BatchLecturerSyncRequest): Observable<BatchLecturer[]> {
    return this.http.put<BatchLecturer[]>(`/api/v1/batches/${batchId}/lecturers`, request);
  }

  updateLecturer(batchId: number, assignmentId: number, request: BatchLecturerRequest): Observable<BatchLecturer> {
    return this.http.put<BatchLecturer>(`/api/v1/batches/${batchId}/lecturers/${assignmentId}`, request);
  }

  changeLecturerStatus(batchId: number, assignmentId: number, status: BatchLecturerStatus, reason: string): Observable<BatchLecturer> {
    return this.http.patch<BatchLecturer>(`/api/v1/batches/${batchId}/lecturers/${assignmentId}/status`, { status, reason });
  }
}
