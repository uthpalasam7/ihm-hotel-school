import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SessionService } from './session.service';

describe('SessionService', () => {
  let api: SessionService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(SessionService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('sends only selected server filters and pagination', () => {
    api.list({ batchId: 7, lecturerId: 20, dateFrom: '2026-07-01', dateTo: '2026-07-31', status: 'SCHEDULED', page: 2, size: 50 }).subscribe();
    const request = http.expectOne(req => req.url === '/api/v1/sessions');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.keys().sort()).toEqual(['batchId', 'dateFrom', 'dateTo', 'lecturerId', 'page', 'size', 'status']);
    expect(request.request.params.get('batchId')).toBe('7');
    expect(request.request.params.get('page')).toBe('2');
    request.flush({ content: [], page: 2, size: 50, totalElements: 0, totalPages: 0 });
  });

  it('sends the exact versioned create, update, cancel and reschedule bodies', () => {
    const body = { batchId: 7, sessionDate: '2026-07-10', startTime: '09:00', endTime: '13:00', lecturerUserId: null,
      topic: 'Kitchen safety', classroom: null, remarks: null };
    api.create(body).subscribe(); let request = http.expectOne('/api/v1/sessions');
    expect(request.request.method).toBe('POST'); expect(request.request.body).toEqual(body); request.flush({});
    api.update(12, { ...body, version: 3 }).subscribe(); request = http.expectOne('/api/v1/sessions/12');
    expect(request.request.method).toBe('PUT'); expect(request.request.body.version).toBe(3); request.flush({});
    api.cancel(12, 'Public holiday', 3).subscribe(); request = http.expectOne('/api/v1/sessions/12/cancel');
    expect(request.request.body).toEqual({ reason: 'Public holiday', version: 3 }); request.flush({});
    const move = { newDate: '2026-07-11', newStartTime: '10:00', newEndTime: '14:00', lecturerUserId: null, reason: 'Lecturer unavailable', version: 3 };
    api.reschedule(12, move).subscribe(); request = http.expectOne('/api/v1/sessions/12/reschedule');
    expect(request.request.body).toEqual(move); request.flush({});
    api.get(12).subscribe(); request = http.expectOne('/api/v1/sessions/12'); expect(request.request.method).toBe('GET'); request.flush({});
  });
});
