import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { CourseService } from '../courses/course.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { AuthService } from '../core/auth/auth.service';
import { Batch } from './batch.models';
import { BatchListComponent } from './batch-list.component';
import { BatchService } from './batch.service';

describe('BatchListComponent', () => {
  let fixture: ComponentFixture<BatchListComponent>;
  let batchService: {
    list: ReturnType<typeof vi.fn>;
    changeStatus: ReturnType<typeof vi.fn>;
  };
  let activeBranchService: ActiveBranchService;
  let hasAnyRole: ReturnType<typeof vi.fn>;
  let listAllActive: ReturnType<typeof vi.fn>;

  const batch: Batch = {
    id: 30,
    course: { id: 5, name: 'Pastry & Bakery', shortCode: 'PB' },
    branch: { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
    batchNumber: '2026/PB02',
    startDate: '2026-07-01',
    endDate: '2026-12-31',
    durationMonths: 6,
    scheduleMode: 'REGULAR',
    status: 'UPCOMING',
    remarks: null,
    registrationSequence: 0,
    lecturerCount: 1,
    studentCount: 0,
    feePlanConfigured: true,
    createdAt: '2026-06-28T00:00:00Z',
    updatedAt: '2026-06-28T00:00:00Z',
    version: 0,
  };

  beforeEach(async () => {
    localStorage.clear();
    batchService = {
      list: vi.fn(() => of({
        content: [batch],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })),
      changeStatus: vi.fn(),
    };
    hasAnyRole=vi.fn(() => true);
    listAllActive=vi.fn(() => of([
      { id: 5, name: 'Pastry & Bakery', shortCode: 'PB', description: null, status: 'ACTIVE', batchCount: 1, createdAt: '', updatedAt: '', version: 0 },
    ]));

    await TestBed.configureTestingModule({
      imports: [BatchListComponent],
      providers: [
        provideRouter([]),
        provideNativeDateAdapter(),
        { provide: BatchService, useValue: batchService },
        { provide: AuthService, useValue: { hasAnyRole } },
        {
          provide: CourseService,
          useValue: { listAllActive },
        },
      ],
    }).compileComponents();

    activeBranchService = TestBed.inject(ActiveBranchService);
    activeBranchService.configure([
      { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
      { id: 2, code: 'IHM-CITY', name: 'IHM City' },
    ]);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('renders batches with fee plan and lecturer counts', async () => {
    fixture = TestBed.createComponent(BatchListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('2026/PB02');
    expect(text).toContain('Fee plan configured');
    expect(text).toContain('PB');
    expect(fixture.nativeElement.querySelector('.batch-cell .batch-link')?.getAttribute('href')).toBe('/batches/30/edit');
    expect(fixture.nativeElement.querySelector('.batch-card-heading .batch-link')?.getAttribute('href')).toBe('/batches/30/edit');
    const mobileActions = fixture.nativeElement.querySelector('.batch-card-actions');
    expect(mobileActions?.querySelectorAll('a, button').length).toBe(6);
  });

  it('reloads branch-specific batches when the active branch changes', async () => {
    fixture = TestBed.createComponent(BatchListComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    activeBranchService.selectBranch(2);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(batchService.list.mock.calls.length).toBeGreaterThanOrEqual(2);
  });

  it('serializes selected filter dates for the existing API', async () => {
    fixture = TestBed.createComponent(BatchListComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      startDateFrom: Date;
      startDateTo: Date;
      load: () => void;
    };
    component.startDateFrom = new Date(2026, 6, 1);
    component.startDateTo = new Date(2026, 6, 31);
    component.load();

    expect(batchService.list).toHaveBeenLastCalledWith(expect.objectContaining({
      startDateFrom: '2026-07-01',
      startDateTo: '2026-07-31',
    }));
  });

  it('shows assigned batches read-only to lecturers', async () => {
    hasAnyRole.mockReturnValue(false);
    fixture=TestBed.createComponent(BatchListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text=fixture.nativeElement.textContent as string;
    expect(text).toContain('View students');
    expect(text).toContain('Class sessions');
    expect(text).not.toContain('Add batch');
    expect(text).not.toContain('Edit batch');
    expect(text).not.toContain('Weekly schedule');
    expect(fixture.nativeElement.querySelector('.batch-cell .batch-link')).toBeNull();
    expect(fixture.nativeElement.querySelector('.batch-card-heading .batch-link')).toBeNull();
    expect(listAllActive).not.toHaveBeenCalled();
  });
});
