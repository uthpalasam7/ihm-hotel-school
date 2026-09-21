import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { EnrollmentListComponent } from './enrollment-list.component';
import { EnrollmentService } from './enrollment.service';

describe('EnrollmentListComponent', () => {
  it('uses the shared grid and sends selected page size to the API', async () => {
    const list = vi.fn(() => of({ content: [{ id: 4, registrationNumber: '2026/CK01/0001',
      studentId: 1, studentName: 'Nimal Perera', batchNumber: '2026/CK01', courseName: 'Cookery',
      enrollmentDate: '2026-08-01', status: 'ACTIVE' }], totalElements: 51, page: 0, size: 20, totalPages: 3 }));
    await TestBed.configureTestingModule({
      imports: [EnrollmentListComponent],
      providers: [provideRouter([]),
        { provide: ActivatedRoute, useValue: { queryParamMap: of(convertToParamMap({ batchId: '8' })) } },
        { provide: EnrollmentService, useValue: { list } }],
    }).compileComponents();

    const fixture = TestBed.createComponent(EnrollmentListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.ihm-data-grid')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('table caption')?.textContent).toContain('Enrollments');
    const mobileCard = fixture.nativeElement.querySelector('.ihm-mobile-cards .ihm-mobile-card');
    expect(mobileCard?.textContent).toContain('Nimal Perera');
    expect(mobileCard?.textContent).toContain('View enrollment');

    const component = fixture.componentInstance as unknown as {
      search: { setValue: (value: string) => void };
      status: { setValue: (value: string) => void };
      applyFilters: () => void;
      clearFilters: () => void;
      changePage: (event: { pageIndex: number; pageSize: number; length: number }) => void;
    };
    component.search.setValue(' Nimal ');
    component.status.setValue('SUSPENDED');
    component.applyFilters();
    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({
      search: 'Nimal', status: 'SUSPENDED', batchId: 8, page: 0,
    }));

    component.changePage({ pageIndex: 1, pageSize: 50, length: 51 });
    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({
      search: 'Nimal', status: 'SUSPENDED', batchId: 8, page: 1, size: 50,
    }));

    component.clearFilters();
    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({
      search: '', status: '', batchId: 8, page: 0, size: 50,
    }));
  });
});
