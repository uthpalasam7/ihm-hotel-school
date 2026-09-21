import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { ConfirmationDialogResult } from '../shared/confirmation-dialog.component';
import { NotificationService } from '../shared/notification.service';
import { Course } from './course.models';
import { CourseListComponent } from './course-list.component';
import { CourseService } from './course.service';

describe('CourseListComponent', () => {
  const course: Course = {
    id: 1,
    name: 'Pastry & Bakery',
    shortCode: 'PB',
    description: 'Certificate course',
    status: 'ACTIVE',
    batchCount: 2,
    createdAt: '',
    updatedAt: '',
    version: 0,
  };
  let list: ReturnType<typeof vi.fn>;
  let changeStatus: ReturnType<typeof vi.fn>;
  let dialogResult: ConfirmationDialogResult | undefined;

  beforeEach(async () => {
    list = vi.fn(() => of({
      content: [course],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    }));
    changeStatus = vi.fn(() => of({ ...course, status: 'INACTIVE' as const }));
    dialogResult = undefined;

    await TestBed.configureTestingModule({
      imports: [CourseListComponent],
      providers: [
        provideRouter([]),
        { provide: CourseService, useValue: { list, changeStatus } },
        {
          provide: MatDialog,
          useValue: {
            open: () => ({ afterClosed: () => of(dialogResult) }),
          },
        },
        {
          provide: NotificationService,
          useValue: { success: vi.fn(), error: vi.fn() },
        },
      ],
    }).compileComponents();
  });

  it('renders course data and server pagination', async () => {
    const fixture = TestBed.createComponent(CourseListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Pastry & Bakery');
    expect(fixture.nativeElement.textContent).toContain('Certificate course');
    expect(fixture.nativeElement.textContent).toContain('PB');
    const mobileCard = fixture.nativeElement.querySelector('.ihm-mobile-cards .ihm-mobile-card');
    expect(mobileCard?.textContent).toContain('Pastry & Bakery');
    expect(mobileCard?.textContent).toContain('Certificate course');
    expect(mobileCard?.textContent).toContain('Deactivate');
    expect(list).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 20 }));
  });

  it('resets pagination and sends the current filters', () => {
    const fixture = TestBed.createComponent(CourseListComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as {
      search: string;
      status: string;
      pageIndex: { set: (value: number) => void };
      applyFilters: () => void;
    };
    component.search = 'pastry';
    component.status = 'ACTIVE';
    component.pageIndex.set(3);

    component.applyFilters();

    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({
      search: 'pastry',
      status: 'ACTIVE',
      page: 0,
    }));
  });

  it('passes the optional audit reason through the confirmation dialog', () => {
    dialogResult = { confirmed: true, reason: 'Course paused' };
    const fixture = TestBed.createComponent(CourseListComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as {
      changeStatus: (value: Course) => void;
    };

    component.changeStatus(course);

    expect(changeStatus).toHaveBeenCalledWith(1, 'INACTIVE', 'Course paused');
  });
});
