import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { NotificationService } from '../shared/notification.service';
import { EnrollmentDetailComponent } from './enrollment-detail.component';
import { EnrollmentService } from './enrollment.service';

describe('EnrollmentDetailComponent', () => {
  it('places charges in the shared grid and pages them on the server', async () => {
    const charges = vi.fn(() => of({ content: [{ id: 1, description: 'Registration fee',
      dueDate: '2026-08-01', finalPayableAmount: 10000, currencyCode: 'LKR', status: 'OVERDUE' }],
      totalElements: 51, page: 0, size: 20, totalPages: 3 }));
    const enrollment = { id: 4, studentId: 1, studentName: 'Nimal Perera', batchId: 3,
      batchNumber: '2026/CK01', courseName: 'Cookery', branchId: 1,
      branchName: 'IHM Hotel School', registrationNumber: '2026/CK01/0001',
      enrollmentDate: '2026-08-01', status: 'ACTIVE', remarks: null, createdAt: '', version: 0 };
    await TestBed.configureTestingModule({
      imports: [EnrollmentDetailComponent],
      providers: [provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '4' })) } },
        { provide: EnrollmentService, useValue: { get: vi.fn(() => of(enrollment)), charges } },
        { provide: MatDialog, useValue: { open: vi.fn() } },
        { provide: NotificationService, useValue: { success: vi.fn() } }],
    }).compileComponents();

    const fixture = TestBed.createComponent(EnrollmentDetailComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.charge-grid.ihm-data-grid')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.charge-grid table caption')?.textContent).toContain('Student charges');
    expect(fixture.nativeElement.querySelector('.charge-grid .ihm-mobile-card')?.textContent).toContain('Registration fee');

    const component = fixture.componentInstance as unknown as {
      changeChargePage: (event: { pageIndex: number; pageSize: number; length: number }) => void;
    };
    component.changeChargePage({ pageIndex: 1, pageSize: 50, length: 51 });
    expect(charges).toHaveBeenLastCalledWith(4, 1, 50);
  });
});
