import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Course } from './course.models';
import { CourseService } from './course.service';

const pastryCourse: Course = {
  id: 1,
  name: 'Pastry & Bakery',
  shortCode: 'PB',
  description: null,
  status: 'ACTIVE',
  batchCount: 1,
  createdAt: '2026-07-01T00:00:00Z',
  updatedAt: '2026-07-01T00:00:00Z',
  version: 0,
};

describe('CourseService', () => {
  let service: CourseService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CourseService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends server-side filters and pagination parameters', () => {
    service.list({ search: 'pastry', status: 'ACTIVE', page: 2, size: 10 }).subscribe();

    const request = http.expectOne((candidate) => candidate.url === '/api/v1/courses');
    expect(request.request.params.get('search')).toBe('pastry');
    expect(request.request.params.get('status')).toBe('ACTIVE');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('10');
    request.flush({ content: [], page: 2, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('loads and sorts every active course page for batch selection', () => {
    let result: Course[] = [];
    service.listAllActive().subscribe((courses) => {
      result = courses;
    });

    const first = http.expectOne((candidate) =>
      candidate.url === '/api/v1/courses'
      && candidate.params.get('status') === 'ACTIVE'
      && candidate.params.get('page') === '0'
      && candidate.params.get('size') === '100');
    first.flush({
      content: [{ ...pastryCourse, id: 2, shortCode: 'PC', name: 'Professional Cookery' }],
      page: 0,
      size: 100,
      totalElements: 2,
      totalPages: 2,
    });

    const second = http.expectOne((candidate) =>
      candidate.url === '/api/v1/courses'
      && candidate.params.get('page') === '1');
    second.flush({
      content: [pastryCourse],
      page: 1,
      size: 100,
      totalElements: 2,
      totalPages: 2,
    });

    expect(result.map((course) => course.shortCode)).toEqual(['PB', 'PC']);
  });
});
