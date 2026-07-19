import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { BranchService } from '../branches/branch.service';
import { NotificationService } from '../shared/notification.service';
import { TemporaryPasswordDialogComponent } from './temporary-password-dialog.component';
import { UserAccount } from './user.models';
import { UserFormComponent } from './user-form.component';
import { UserService } from './user.service';

@Component({ template: '' })
class BlankComponent {}

describe('UserFormComponent', () => {
  let fixture: ComponentFixture<UserFormComponent>;
  let routeId: string | null;
  let userService: {
    roles: ReturnType<typeof vi.fn>;
    get: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    updateProfile: ReturnType<typeof vi.fn>;
    replaceRoles: ReturnType<typeof vi.fn>;
    replaceBranches: ReturnType<typeof vi.fn>;
  };
  let dialog: { open: ReturnType<typeof vi.fn> };

  const existingUser: UserAccount = {
    id: 10,
    username: 'lecturer',
    email: 'lecturer@example.invalid',
    fullName: 'Lecturer User',
    contactNumber: '0771234567',
    status: 'ACTIVE',
    roles: [{ id: 3, code: 'LECTURER', name: 'Lecturer' }],
    branches: [{ id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' }],
    lastLoginAt: null,
    createdAt: '2026-06-27T00:00:00Z',
    updatedAt: '2026-06-27T00:00:00Z',
    version: 0,
  };

  beforeEach(async () => {
    routeId = null;
    userService = {
      roles: vi.fn(() => of([{ id: 3, code: 'LECTURER', name: 'Lecturer' }])),
      get: vi.fn(() => of(existingUser)),
      create: vi.fn(() => of({
        ...existingUser,
        status: 'PASSWORD_CHANGE_REQUIRED',
        temporaryPassword: 'TempPass123',
      })),
      updateProfile: vi.fn(() => of(existingUser)),
      replaceRoles: vi.fn(() => of(existingUser)),
      replaceBranches: vi.fn(() => of(existingUser)),
    };
    dialog = {
      open: vi.fn(() => ({ afterClosed: () => of(undefined) })),
    };

    await TestBed.configureTestingModule({
      imports: [UserFormComponent],
      providers: [
        provideRouter([{ path: 'users', component: BlankComponent }]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => key === 'id' ? routeId : null,
              },
            },
          },
        },
        { provide: UserService, useValue: userService },
        {
          provide: BranchService,
          useValue: {
            list: () => of({
              content: [{
                id: 1,
                code: 'IHM-MAIN',
                name: 'IHM Hotel School',
                status: 'ACTIVE',
                defaultBranch: true,
                createdAt: '',
                updatedAt: '',
                version: 0,
              }],
              page: 0,
              size: 100,
              totalElements: 1,
              totalPages: 1,
            }),
          },
        },
        { provide: MatDialog, useValue: dialog },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();
  });

  it('auto-selects the only assignable role and branch', async () => {
    fixture = TestBed.createComponent(UserFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      form: {
        controls: {
          roleCodes: { value: string[] };
          branchIds: { value: number[] };
        };
      };
    };
    expect(component.form.controls.roleCodes.value).toEqual(['LECTURER']);
    expect(component.form.controls.branchIds.value).toEqual([1]);
    expect(fixture.nativeElement.textContent).toContain('Lecturer');
    expect(fixture.nativeElement.textContent).toContain('IHM-MAIN');
  });

  it('creates a user with the existing request and shows the password once', async () => {
    fixture = TestBed.createComponent(UserFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      form: { patchValue: (value: unknown) => void };
      save: () => void;
    };
    component.form.patchValue({
      username: 'lecturer',
      fullName: 'Lecturer User',
      email: 'lecturer@example.invalid',
      contactNumber: '0771234567',
      status: 'ACTIVE',
      temporaryPassword: '',
    });
    component.save();

    expect(userService.create).toHaveBeenCalledWith({
      username: 'lecturer',
      fullName: 'Lecturer User',
      email: 'lecturer@example.invalid',
      contactNumber: '0771234567',
      status: 'ACTIVE',
      roleCodes: ['LECTURER'],
      branchIds: [1],
      temporaryPassword: null,
    });
    expect(dialog.open).toHaveBeenCalledWith(
      TemporaryPasswordDialogComponent,
      expect.objectContaining({
        data: expect.objectContaining({ username: 'lecturer', temporaryPassword: 'TempPass123' }),
      }),
    );
  });

  it('preserves the existing profile, role, and branch edit sequence', async () => {
    routeId = '10';
    fixture = TestBed.createComponent(UserFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      form: { patchValue: (value: unknown) => void };
      save: () => void;
    };
    component.form.patchValue({ fullName: 'Updated Lecturer' });
    component.save();

    expect(userService.updateProfile).toHaveBeenCalledWith(10, expect.objectContaining({
      fullName: 'Updated Lecturer',
    }));
    expect(userService.replaceRoles).toHaveBeenCalledWith(10, ['LECTURER']);
    expect(userService.replaceBranches).toHaveBeenCalledWith(10, [1]);
    expect(userService.updateProfile.mock.invocationCallOrder[0])
      .toBeLessThan(userService.replaceRoles.mock.invocationCallOrder[0]);
    expect(userService.replaceRoles.mock.invocationCallOrder[0])
      .toBeLessThan(userService.replaceBranches.mock.invocationCallOrder[0]);
  });

  it('reports unsaved access changes', async () => {
    fixture = TestBed.createComponent(UserFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      toggleRole: (code: string, checked: boolean) => void;
      hasUnsavedChanges: () => boolean;
    };
    component.toggleRole('LECTURER', false);

    expect(component.hasUnsavedChanges()).toBe(true);
  });
});
