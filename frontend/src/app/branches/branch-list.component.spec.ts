import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { NotificationService } from '../shared/notification.service';
import { BranchService } from './branch.service';
import { BranchListComponent } from './branch-list.component';

const branch = {
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

describe('BranchListComponent', () => {
  let fixture: ComponentFixture<BranchListComponent>;
  let list: ReturnType<typeof vi.fn>;
  let changeStatus: ReturnType<typeof vi.fn>;
  let notifications: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    list = vi.fn((filters: { page?: number; size?: number }) => of({
      content: [branch],
      page: filters.page ?? 0,
      size: filters.size ?? 20,
      totalElements: 21,
      totalPages: 2,
    }));
    changeStatus = vi.fn(() => of(branch));
    notifications = { success: vi.fn(), error: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [BranchListComponent],
      providers: [
        provideRouter([]),
        {
          provide: BranchService,
          useValue: {
            list,
            changeStatus,
          },
        },
        {
          provide: NotificationService,
          useValue: notifications,
        },
        {
          provide: MatDialog,
          useValue: {
            open: () => ({ afterClosed: () => of({ confirmed: true, reason: 'Seasonal closure' }) }),
          },
        },
      ],
    }).compileComponents();
  });

  it('renders loaded branches with an accessible status label', async () => {
    fixture = TestBed.createComponent(BranchListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('IHM-MAIN');
    expect(fixture.nativeElement.textContent).toContain('IHM Hotel School');
    expect(fixture.nativeElement.textContent).toContain('active');
    expect(fixture.nativeElement.querySelector('mat-paginator')).toBeTruthy();
  });

  it('requests the selected server-side page and page size', async () => {
    fixture = TestBed.createComponent(BranchListComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      changePage: (event: { pageIndex: number; pageSize: number; length: number; previousPageIndex: number }) => void;
    };
    component.changePage({ pageIndex: 1, pageSize: 10, length: 21, previousPageIndex: 0 });

    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1, size: 10 }));
  });

  it('shows an error state and retry action when loading fails', async () => {
    list.mockReturnValue(throwError(() => ({ error: { message: 'Access denied' } })));
    fixture = TestBed.createComponent(BranchListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Access denied');
    expect(fixture.nativeElement.textContent).toContain('Try again');
  });

  it('preserves the status-change reason and audit API request', async () => {
    fixture = TestBed.createComponent(BranchListComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as { changeStatus: (value: typeof branch) => void };
    component.changeStatus(branch);

    expect(changeStatus).toHaveBeenCalledWith(1, 'INACTIVE', 'Seasonal closure');
    expect(notifications.success).toHaveBeenCalledWith('IHM-MAIN status updated');
  });
});
