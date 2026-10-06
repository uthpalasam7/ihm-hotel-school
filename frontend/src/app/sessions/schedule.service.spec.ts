import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ScheduleService } from './schedule.service';
import { Schedule } from './schedule.models';

describe('ScheduleService', () => {
  let api: ScheduleService; let http: HttpTestingController;
  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }); api = TestBed.inject(ScheduleService); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());
  it('uses server pagination and versioned pattern mutation endpoints', () => {
    api.list(7, 2, 50).subscribe();
    let req = http.expectOne('/api/v1/batches/7/schedules?page=2&size=50'); expect(req.request.method).toBe('GET'); req.flush({ content: [] });
    const pattern: Schedule = { id: 5, batchId: 7, dayOfWeek: 1, startTime: '09:00', endTime: '13:00', defaultLecturerUserId: null, defaultLecturerName: null, classroom: null, status: 'ACTIVE', version: 3 };
    api.save(7, pattern, 5).subscribe(); req = http.expectOne('/api/v1/batches/7/schedules/5'); expect(req.request.method).toBe('PUT'); expect(req.request.body.version).toBe(3); req.flush(pattern);
    api.save(7, pattern).subscribe(); req = http.expectOne('/api/v1/batches/7/schedules'); expect(req.request.method).toBe('POST'); req.flush(pattern);
    api.deactivate(7, pattern).subscribe(); req = http.expectOne('/api/v1/batches/7/schedules/5?version=3'); expect(req.request.method).toBe('DELETE'); req.flush(null);
  });
  it('sends the reviewed date-only range and token in the request body', () => {
    const input = { fromDate: '2026-07-01', toDate: '2026-07-31', excludeDates: ['2026-07-08'] };
    api.preview(7, input).subscribe(); const preview = http.expectOne('/api/v1/batches/7/sessions/preview'); expect(preview.request.body).toEqual(input); preview.flush({});
    api.generate(7, input, 'confirmation').subscribe(); const save = http.expectOne('/api/v1/batches/7/sessions/generate'); expect(save.request.method).toBe('POST'); expect(save.request.body).toEqual({ ...input, previewToken: 'confirmation' }); save.flush({});
  });
});
