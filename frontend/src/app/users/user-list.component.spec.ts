import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { BranchService } from '../branches/branch.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { AuthService } from '../core/auth/auth.service';
import { NotificationService } from '../shared/notification.service';
import { TemporaryPasswordDialogComponent } from './temporary-password-dialog.component';
import { UserAccount } from './user.models';
import { UserListComponent } from './user-list.component';
import { UserService } from './user.service';

describe('UserListComponent', () => {
  let fixture: ComponentFixture<UserListComponent>;
  let userService: {
    roles: ReturnType<typeof vi.fn>;
    list: ReturnType<typeof vi.fn>;
    changeStatus: ReturnType<typeof vi.fn>;
    resetPassword: ReturnType<typeof vi.fn>;
  };
  let dialog: { open: ReturnType<typeof vi.fn> };

  const user: UserAccount = {
    id: 20,
    username: 'multi_lecturer',
    email: 'multi@example.invalid',
    fullName: 'Multi Branch Lecturer',
    contactNumber: null,
    status: 'ACTIVE',
    roles: [
      { id: 2, code: 'ADMIN', name: 'Administrator' },
      { id: 3, code: 'LECTURER', name: 'Lecturer' },
    ],
    branches: [
      { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
      { id: 2, code: 'IHM-CITY', name: 'IHM City' },
      { id: 3, code: 'IHM-NORTH', name: 'IHM North' },
      { id: 4, code: 'IHM-SOUTH', name: 'IHM South' },
    ],
    lastLoginAt: null,
    createdAt: '2026-06-27T00:00:00Z',
    updatedAt: '2026-06-27T00:00:00Z',
    version: 0,
  };

  beforeEach(async () => {
    localStorage.clear();
    userService = {
      roles: vi.fn(() => of([
        { id: 2, code: 'ADMIN', name: 'Administrator' },
        { id: 3, code: 'LECTURER', name: 'Lecturer' },
      ])),
      list: vi.fn(() => of({
        content: [user],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      })),
      changeStatus: vi.fn(() => of({ ...user, status: 'DISABLED' })),
      resetPassword: vi.fn(() => of({
        ...user,
        status: 'PASSWORD_CHANGE_REQUIRED',
        temporaryPassword: 'TempPass123',
      })),
    };
    dialog = {
      open: vi.fn(() => ({ afterClosed: () => of({ confirmed: true, reason: 'Administrative request' }) })),
    };

    await TestBed.configureTestingModule({
      imports: [UserListComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userService },
        {
          provide: BranchService,
          useValue: {
            list: () => of({
              content: [
                { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School', status: 'ACTIVE', defaultBranch: true, createdAt: '', updatedAt: '', version: 0 },
                { id: 2, code: 'IHM-CITY', name: 'IHM City', status: 'ACTIVE', defaultBranch: false, createdAt: '', updatedAt: '', version: 0 },
              ],
              page: 0,
              size: 100,
              totalElements: 2,
              totalPages: 1,
            }),
          },
        },
        { provide: MatDialog, useValue: dialog },
        { provide: AuthService, useValue: { currentUser: () => ({ id: 1 }) } },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();

    TestBed.inject(ActiveBranchService).configure([
      { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
      { id: 2, code: 'IHM-CITY', name: 'IHM City' },
    ]);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('displays every assigned role and branch with a friendly empty last-login value', async () => {
    fixture = TestBed.createComponent(UserListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Administrator');
    expect(text).toContain('Lecturer');
    expect(text).toContain('IHM-MAIN');
    expect(text).toContain('IHM-CITY');
    expect(text).toContain('IHM-NORTH');
    expect(text).toContain('IHM-SOUTH');
    expect(text).toContain('Never');
  });

  it('serializes filters and resets to the first server page', async () => {
    fixture = TestBed.createComponent(UserListComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      search: string;
      role: string;
      branchId: string;
      status: string;
      pageIndex: { set: (value: number) => void };
      applyFilters: () => void;
    };
    component.pageIndex.set(3);
    component.search = '  chef  ';
    component.role = 'LECTURER';
    component.branchId = '2';
    component.status = 'ACTIVE';
    component.applyFilters();

    expect(userService.list).toHaveBeenLastCalledWith(expect.objectContaining({
      search: 'chef',
      role: 'LECTURER',
      branchId: '2',
      status: 'ACTIVE',
      page: 0,
      size: 20,
    }));
  });

  it('forwards the status-change reason through the existing API', async () => {
    fixture = TestBed.createComponent(UserListComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      changeStatus: (value: UserAccount) => void;
    };
    component.changeStatus(user);

    expect(userService.changeStatus).toHaveBeenCalledWith(20, 'DISABLED', 'Administrative request');
  });

  it('shows a reset password only in the one-time result dialog', async () => {
    dialog.open
      .mockReturnValueOnce({ afterClosed: () => of({ confirmed: true, reason: 'Requested by administrator' }) })
      .mockReturnValueOnce({});
    fixture = TestBed.createComponent(UserListComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      resetPassword: (value: UserAccount) => void;
    };
    component.resetPassword(user);

    expect(userService.resetPassword).toHaveBeenCalledWith(20, null, 'Requested by administrator');
    expect(dialog.open).toHaveBeenLastCalledWith(
      TemporaryPasswordDialogComponent,
      expect.objectContaining({
        data: expect.objectContaining({ username: 'multi_lecturer', temporaryPassword: 'TempPass123' }),
      }),
    );
  });
});
