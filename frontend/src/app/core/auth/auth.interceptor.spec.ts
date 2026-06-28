import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActiveBranchService } from './active-branch.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let httpTesting: HttpTestingController;
  let activeBranchService: ActiveBranchService;

  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('ihm.accessToken', 'access-token');

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    httpTesting = TestBed.inject(HttpTestingController);
    activeBranchService = TestBed.inject(ActiveBranchService);
    activeBranchService.configure([{ id: 2, code: 'IHM-CITY', name: 'IHM City' }]);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('sends active branch context with branch-scoped API requests', () => {
    TestBed.inject(HttpClient).get('/api/v1/users').subscribe();

    const request = httpTesting.expectOne('/api/v1/users');
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-token');
    expect(request.request.headers.get('X-Active-Branch-Id')).toBe('2');
    request.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });
});
