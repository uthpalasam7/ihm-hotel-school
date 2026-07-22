import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { StudentRequest } from './student.models';
import { StudentService } from './student.service';

describe('StudentService', () => {
  let service: StudentService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(StudentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends server-side list filters and pagination', () => {
    service.list({ search: 'nimal', status: 'ACTIVE', page: 2, size: 10 }).subscribe();
    const request = http.expectOne((candidate) => candidate.url === '/api/v1/students');
    expect(request.request.params.get('search')).toBe('nimal');
    expect(request.request.params.get('status')).toBe('ACTIVE');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('10');
    request.flush({ content: [], page: 2, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('uses the exact NIC lookup and student write endpoints', () => {
    const body: StudentRequest = { fullName: 'Nimal Perera', nic: '200012345678', contactNumber: '0712345678', address: 'Kurunegala' };
    service.findByNic('2000 123V').subscribe();
    http.expectOne('/api/v1/students/by-nic/2000%20123V').flush({});
    service.create(body).subscribe();
    const create = http.expectOne('/api/v1/students');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(body);
    create.flush({});
  });

  it('uploads and retrieves authenticated photo blobs through HttpClient', () => {
    const file = new File(['image'], 'student.png', { type: 'image/png' });
    service.uploadPhoto(7, file).subscribe();
    const upload = http.expectOne('/api/v1/students/7/photo');
    expect(upload.request.method).toBe('POST');
    expect(upload.request.body instanceof FormData).toBe(true);
    upload.flush({});

    service.loadPhoto(7, 'thumbnail').subscribe();
    const photo = http.expectOne((candidate) => candidate.url === '/api/v1/students/7/photo');
    expect(photo.request.params.get('variant')).toBe('thumbnail');
    expect(photo.request.responseType).toBe('blob');
    photo.flush(new Blob(['image'], { type: 'image/png' }));
  });
});
