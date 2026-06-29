import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../shared/page.model';
import { Course, CourseRequest, CourseStatus } from './course.models';

@Injectable({ providedIn: 'root' })
export class CourseService {
  private readonly http = inject(HttpClient);

  list(filters: { search?: string; status?: string; page?: number; size?: number } = {}): Observable<PageResponse<Course>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20);
    if (filters.search) {
      params = params.set('search', filters.search);
    }
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    return this.http.get<PageResponse<Course>>('/api/v1/courses', { params });
  }

  get(id: number): Observable<Course> {
    return this.http.get<Course>(`/api/v1/courses/${id}`);
  }

  create(request: CourseRequest): Observable<Course> {
    return this.http.post<Course>('/api/v1/courses', request);
  }

  update(id: number, request: CourseRequest): Observable<Course> {
    return this.http.put<Course>(`/api/v1/courses/${id}`, request);
  }

  changeStatus(id: number, status: CourseStatus, reason: string): Observable<Course> {
    return this.http.patch<Course>(`/api/v1/courses/${id}/status`, { status, reason });
  }
}
