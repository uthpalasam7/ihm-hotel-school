import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../shared/page.model';
import { Branch, BranchRequest, BranchStatus } from './branch.models';

@Injectable({ providedIn: 'root' })
export class BranchService {
  private readonly http = inject(HttpClient);

  list(filters: { search?: string; status?: string; page?: number; size?: number } = {}): Observable<PageResponse<Branch>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20);
    if (filters.search) {
      params = params.set('search', filters.search);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    return this.http.get<PageResponse<Branch>>('/api/v1/branches', { params });
  }

  get(id: number): Observable<Branch> {
    return this.http.get<Branch>(`/api/v1/branches/${id}`);
  }

  create(request: BranchRequest): Observable<Branch> {
    return this.http.post<Branch>('/api/v1/branches', request);
  }

  update(id: number, request: BranchRequest): Observable<Branch> {
    return this.http.put<Branch>(`/api/v1/branches/${id}`, request);
  }

  changeStatus(id: number, status: BranchStatus, reason: string): Observable<Branch> {
    return this.http.patch<Branch>(`/api/v1/branches/${id}/status`, { status, reason });
  }
}
