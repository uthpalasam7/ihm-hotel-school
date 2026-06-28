import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../shared/page.model';
import { Role, UserAccount, UserProfileRequest, UserRequest, UserStatus } from './user.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  list(filters: { search?: string; role?: string; branchId?: number | string; status?: string; page?: number; size?: number } = {}): Observable<PageResponse<UserAccount>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20);
    if (filters.search) {
      params = params.set('search', filters.search);
    }
    if (filters.role) {
      params = params.set('role', filters.role);
    }
    if (filters.branchId) {
      params = params.set('branchId', filters.branchId);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    return this.http.get<PageResponse<UserAccount>>('/api/v1/users', { params });
  }

  roles(): Observable<Role[]> {
    return this.http.get<Role[]>('/api/v1/users/roles');
  }

  get(id: number): Observable<UserAccount> {
    return this.http.get<UserAccount>(`/api/v1/users/${id}`);
  }

  create(request: UserRequest): Observable<UserAccount> {
    return this.http.post<UserAccount>('/api/v1/users', request);
  }

  updateProfile(id: number, request: UserProfileRequest): Observable<UserAccount> {
    return this.http.put<UserAccount>(`/api/v1/users/${id}`, request);
  }

  replaceRoles(id: number, roleCodes: string[]): Observable<UserAccount> {
    return this.http.put<UserAccount>(`/api/v1/users/${id}/roles`, { roleCodes });
  }

  replaceBranches(id: number, branchIds: number[]): Observable<UserAccount> {
    return this.http.put<UserAccount>(`/api/v1/users/${id}/branches`, { branchIds });
  }

  changeStatus(id: number, status: UserStatus, reason: string): Observable<UserAccount> {
    return this.http.patch<UserAccount>(`/api/v1/users/${id}/status`, { status, reason });
  }

  resetPassword(id: number, temporaryPassword: string | null, reason: string): Observable<UserAccount> {
    return this.http.post<UserAccount>(`/api/v1/users/${id}/reset-password`, { temporaryPassword, reason });
  }
}
