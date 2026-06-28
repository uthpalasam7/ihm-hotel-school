import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { BranchService } from '../branches/branch.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { AuthService } from '../core/auth/auth.service';
import { CurrentUser } from '../core/auth/auth.models';
import { ShellComponent } from './shell.component';

describe('ShellComponent', () => {
  afterEach(() => {
    localStorage.clear();
  });

  it('shows lecturer navigation without administrator controls', async () => {
    localStorage.clear();
    const lecturer: CurrentUser = {
      id: 10,
      username: 'lecturer',
      fullName: 'Lecturer User',
      status: 'ACTIVE',
      passwordChangeRequired: false,
      roles: ['LECTURER'],
      branches: [{ id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' }],
    };

    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            currentUser: signal(lecturer).asReadonly(),
            hasAnyRole: (roles: string[]) => lecturer.roles.some((role) => roles.includes(role)),
            logout: () => undefined,
          },
        },
        {
          provide: BranchService,
          useValue: { list: () => of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }) },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Attendance');
    expect(text).toContain('Lecturer User');
    expect(text).not.toContain('Users');
    expect(text).not.toContain('Audit');
  });

  it('shows a selector and switches active branch for multi-branch users', async () => {
    localStorage.clear();
    const admin: CurrentUser = {
      id: 11,
      username: 'admin',
      fullName: 'Admin User',
      status: 'ACTIVE',
      passwordChangeRequired: false,
      roles: ['ADMIN'],
      branches: [
        { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
        { id: 2, code: 'IHM-CITY', name: 'IHM City' },
      ],
    };

    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            currentUser: signal(admin).asReadonly(),
            hasAnyRole: (roles: string[]) => admin.roles.some((role) => roles.includes(role)),
            logout: () => undefined,
          },
        },
        {
          provide: BranchService,
          useValue: { list: () => of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }) },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const select = fixture.nativeElement.querySelector('select[aria-label="Active branch"]') as HTMLSelectElement;
    select.value = '2';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(TestBed.inject(ActiveBranchService).activeBranch()?.code).toBe('IHM-CITY');
  });

  it('shows a fixed branch display for a single-branch user', async () => {
    localStorage.clear();
    const admin: CurrentUser = {
      id: 12,
      username: 'admin_single',
      fullName: 'Admin Single',
      status: 'ACTIVE',
      passwordChangeRequired: false,
      roles: ['ADMIN'],
      branches: [{ id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' }],
    };

    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            currentUser: signal(admin).asReadonly(),
            hasAnyRole: (roles: string[]) => admin.roles.some((role) => roles.includes(role)),
            logout: () => undefined,
          },
        },
        {
          provide: BranchService,
          useValue: { list: () => of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }) },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('select[aria-label="Active branch"]')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('IHM-MAIN - IHM Hotel School');
  });
});
