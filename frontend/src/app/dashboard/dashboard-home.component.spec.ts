import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { CurrentUser } from '../core/auth/auth.models';
import { AuthService } from '../core/auth/auth.service';
import { DashboardHomeComponent } from './dashboard-home.component';

describe('DashboardHomeComponent', () => {
  afterEach(() => localStorage.clear());

  it('shows a welcoming branch context and authorized task cards without fake statistics', async () => {
    const user: CurrentUser = {
      id: 1,
      username: 'admin',
      fullName: 'School Administrator',
      status: 'ACTIVE',
      passwordChangeRequired: false,
      roles: ['SUPER_ADMIN'],
      branches: [{ id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' }],
    };

    await TestBed.configureTestingModule({
      imports: [DashboardHomeComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            currentUser: signal(user).asReadonly(),
            hasAnyRole: (roles: string[]) => user.roles.some((role) => roles.includes(role)),
          },
        },
      ],
    }).compileComponents();
    TestBed.inject(ActiveBranchService).configure(user.branches);

    const fixture = TestBed.createComponent(DashboardHomeComponent);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Welcome back');
    expect(text).not.toContain('Current branch');
    expect(text).toContain('Continue your work');
    expect(text).toContain('Branches');
    expect(text).not.toContain('Operational metrics are coming later');
    expect(text).not.toContain('Fees collected');

    const branchLink = fixture.nativeElement.querySelector(
      'a[aria-label^="View branches"]',
    ) as HTMLAnchorElement;
    expect(branchLink).toBeTruthy();
    expect(branchLink.getAttribute('href')).toBe('/branches');
  });
});
