import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { EMPTY, Observable, expand, map, reduce } from 'rxjs';
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

  listAllActive(): Observable<Course[]> {
    const size = 100;
    return this.list({ status: 'ACTIVE', page: 0, size }).pipe(
      expand((page) => page.page + 1 < page.totalPages
        ? this.list({ status: 'ACTIVE', page: page.page + 1, size })
        : EMPTY),
      reduce((courses, page) => [...courses, ...page.content], [] as Course[]),
      map((courses) => courses.sort((left, right) => left.shortCode.localeCompare(right.shortCode))),
    );
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
