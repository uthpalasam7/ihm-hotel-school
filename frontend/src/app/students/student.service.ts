import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../shared/page.model';
import { Student, StudentRequest, StudentStatus } from './student.models';

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http = inject(HttpClient);

  list(filters: { search?: string; nic?: string; status?: string; page?: number; size?: number } = {}): Observable<PageResponse<Student>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20);
    if (filters.search) {
      params = params.set('search', filters.search);
    }
    if (filters.nic) {
      params = params.set('nic', filters.nic);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    return this.http.get<PageResponse<Student>>('/api/v1/students', { params });
  }

  get(id: number): Observable<Student> {
    return this.http.get<Student>(`/api/v1/students/${id}`);
  }

  findByNic(nic: string): Observable<Student> {
    return this.http.get<Student>(`/api/v1/students/by-nic/${encodeURIComponent(nic)}`);
  }

  create(request: StudentRequest): Observable<Student> {
    return this.http.post<Student>('/api/v1/students', request);
  }

  update(id: number, request: StudentRequest): Observable<Student> {
    return this.http.put<Student>(`/api/v1/students/${id}`, request);
  }

  changeStatus(id: number, status: StudentStatus, reason: string): Observable<Student> {
    return this.http.patch<Student>(`/api/v1/students/${id}/status`, { status, reason });
  }

  uploadPhoto(id: number, photo: File): Observable<Student> {
    const body = new FormData();
    body.append('photo', photo);
    return this.http.post<Student>(`/api/v1/students/${id}/photo`, body);
  }

  loadPhoto(id: number, variant: 'full' | 'thumbnail' = 'full'): Observable<Blob> {
    const params = new HttpParams().set('variant', variant);
    return this.http.get(`/api/v1/students/${id}/photo`, { params, responseType: 'blob' });
  }

  deletePhoto(id: number): Observable<void> {
    return this.http.delete<void>(`/api/v1/students/${id}/photo`);
  }
}
