import { BreakpointObserver } from '@angular/cdk/layout';
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

  it('shows only implemented lecturer navigation without administrator controls', async () => {
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
    expect(text).toContain('Dashboard');
    expect(text).toContain('Class sessions');
    expect(text).not.toContain('Attendance');
    expect(text).not.toContain('Users');
    expect(text).not.toContain('Audit');
    expect(fixture.nativeElement.querySelector('.nav-marker')).toBeNull();

    const accountButton = fixture.nativeElement.querySelector(
      'button[aria-label="Open account menu for Lecturer User"]',
    ) as HTMLButtonElement;
    expect(accountButton).toBeTruthy();
    expect(accountButton.textContent).toContain('LU');

    accountButton.click();
    fixture.detectChanges();
    await fixture.whenStable();

    const menuText = document.body.textContent ?? '';
    expect(menuText).toContain('Lecturer User');
    expect(menuText).toContain('LECTURER');
    expect(menuText).toContain('Change password');
    expect(menuText).toContain('Logout');
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

    const branchSwitcher = fixture.nativeElement.querySelector(
      'button[aria-label^="Switch active branch"]',
    ) as HTMLButtonElement;
    expect(branchSwitcher).toBeTruthy();
    expect(branchSwitcher.textContent).toContain('IHM-CITY');
    expect(fixture.nativeElement.querySelector('mat-select')).toBeNull();

    branchSwitcher.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(document.body.textContent).toContain('IHM-MAIN');

    const component = fixture.componentInstance as unknown as { changeBranch: (branchId: number) => void };
    component.changeBranch(1);
    fixture.detectChanges();

    expect(TestBed.inject(ActiveBranchService).activeBranch()?.code).toBe('IHM-MAIN');
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

    expect(fixture.nativeElement.querySelector('button[aria-label^="Switch active branch"]')).toBeNull();
    const branchContext = fixture.nativeElement.querySelector('.branch-static') as HTMLElement;
    expect(branchContext).toBeTruthy();
    expect(branchContext.textContent).toContain('IHM Hotel School');
    expect(branchContext.textContent).toContain('IHM-MAIN');
  });

  it('uses overlay navigation and exposes a menu control on mobile screens', async () => {
    const lecturer: CurrentUser = {
      id: 13,
      username: 'mobile_lecturer',
      fullName: 'Mobile Lecturer',
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
          useValue: { listAllActive: () => of([]) },
        },
        {
          provide: BreakpointObserver,
          useValue: { observe: () => of({ matches: true }) },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('button[aria-label="Open navigation"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('mat-sidenav').classList).toContain('mat-drawer-over');
  });
});
