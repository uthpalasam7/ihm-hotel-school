import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { NotificationService } from '../shared/notification.service';
import { Student } from './student.models';
import { StudentListComponent } from './student-list.component';
import { StudentService } from './student.service';

describe('StudentListComponent', () => {
  const student: Student = {
    id: 1, fullName: 'Nimal Perera', nic: '200012345678', contactNumber: '0712345678',
    alternativeContactNumber: null, email: 'nimal@example.invalid', address: 'Kurunegala',
    dateOfBirth: null, gender: null, remarks: null, status: 'ACTIVE', photoAvailable: false,
    photoUrl: null, photoThumbnailUrl: null, createdAt: '', updatedAt: '', version: 0,
  };
  let list: ReturnType<typeof vi.fn>;
  let changeStatus: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    list = vi.fn(() => of({ content: [student], page: 0, size: 20, totalElements: 1, totalPages: 1 }));
    changeStatus = vi.fn(() => of({ ...student, status: 'INACTIVE' as const }));
    await TestBed.configureTestingModule({
      imports: [StudentListComponent],
      providers: [
        provideRouter([]),
        { provide: StudentService, useValue: { list, changeStatus, loadPhoto: vi.fn() } },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(undefined) }) } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();
  });

  it('renders student data in desktop and mobile result presentations', async () => {
    const fixture = TestBed.createComponent(StudentListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nimal Perera');
    expect(fixture.nativeElement.textContent).toContain('200012345678');
    expect(fixture.nativeElement.querySelector('.desktop-table')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.mobile-cards')).toBeTruthy();
  });

  it('resets pagination and submits active filters', () => {
    const fixture = TestBed.createComponent(StudentListComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as { search: string; status: string; pageIndex: { set: (value: number) => void }; applyFilters: () => void };
    component.search = 'nimal';
    component.status = 'ACTIVE';
    component.pageIndex.set(3);
    component.applyFilters();
    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ search: 'nimal', status: 'ACTIVE', page: 0 }));
  });
});
