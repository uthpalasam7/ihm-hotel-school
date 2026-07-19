import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Branch } from './branch.models';
import { BranchService } from './branch.service';

const mainBranch = {
  id: 1,
  code: 'IHM-MAIN',
  name: 'IHM Hotel School',
  address: null,
  contactNumber: null,
  status: 'ACTIVE' as const,
  defaultBranch: true,
  createdAt: '2026-06-27T00:00:00Z',
  updatedAt: '2026-06-27T00:00:00Z',
  version: 0,
};

describe('BranchService', () => {
  let service: BranchService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BranchService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends existing server-side filters and pagination parameters', () => {
    service.list({ search: 'main', status: 'ACTIVE', page: 2, size: 10 }).subscribe();

    const request = http.expectOne((candidate) => candidate.url === '/api/v1/branches');
    expect(request.request.params.get('search')).toBe('main');
    expect(request.request.params.get('status')).toBe('ACTIVE');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('10');
    request.flush({ content: [], page: 2, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('loads every active branch page for the super-admin selector', () => {
    let result: Branch[] = [];
    service.listAllActive().subscribe((branches) => {
      result = branches;
    });

    const first = http.expectOne((candidate) =>
      candidate.url === '/api/v1/branches'
      && candidate.params.get('status') === 'ACTIVE'
      && candidate.params.get('page') === '0'
      && candidate.params.get('size') === '100');
    first.flush({
      content: [{ ...mainBranch, id: 2, code: 'IHM-WEST', name: 'IHM West', defaultBranch: false }],
      page: 0,
      size: 100,
      totalElements: 2,
      totalPages: 2,
    });

    const second = http.expectOne((candidate) =>
      candidate.url === '/api/v1/branches'
      && candidate.params.get('page') === '1');
    second.flush({
      content: [mainBranch],
      page: 1,
      size: 100,
      totalElements: 2,
      totalPages: 2,
    });

    expect(result.map((branch) => branch.code)).toEqual(['IHM-MAIN', 'IHM-WEST']);
  });
});
