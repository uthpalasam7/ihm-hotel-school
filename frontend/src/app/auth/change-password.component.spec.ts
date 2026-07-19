import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CurrentUser } from '../core/auth/auth.models';
import { AuthService } from '../core/auth/auth.service';
import { ChangePasswordComponent } from './change-password.component';

describe('ChangePasswordComponent', () => {
  let changePassword: AuthService['changePassword'];
  let requiredChange: boolean;
  let logout: ReturnType<typeof vi.fn>;
  const user: CurrentUser = {
    id: 1,
    username: 'admin',
    fullName: 'Admin User',
    status: 'ACTIVE',
    passwordChangeRequired: false,
    roles: ['SUPER_ADMIN'],
    branches: [{ id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' }],
  };

  beforeEach(async () => {
    changePassword = () => of(user);
    requiredChange = true;
    logout = vi.fn();

    await TestBed.configureTestingModule({
      imports: [ChangePasswordComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            changePassword: (currentPassword: string, newPassword: string) => changePassword(currentPassword, newPassword),
            logout,
            requiresPasswordChange: () => requiredChange,
          },
        },
      ],
    }).compileComponents();
  });

  it('shows validation messages when submitted empty', async () => {
    const fixture = TestBed.createComponent(ChangePasswordComponent);
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('Current password is required');
    expect(fixture.nativeElement.textContent).toContain('New password is required');
    expect(fixture.nativeElement.textContent).toContain('Confirm the new password');
  });

  it('requires matching new passwords', async () => {
    const fixture = TestBed.createComponent(ChangePasswordComponent);
    fixture.detectChanges();

    const inputs = fixture.nativeElement.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    inputs[0].value = 'Admin@123';
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = 'NewAdmin@123';
    inputs[1].dispatchEvent(new Event('input'));
    inputs[2].value = 'Different@123';
    inputs[2].dispatchEvent(new Event('input'));

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.nativeElement.textContent).toContain('New passwords do not match');
  });

  it('shows an error and re-enables submit after failed password change', async () => {
    changePassword = () => throwError(() => new Error('Unauthorized'));
    const fixture = TestBed.createComponent(ChangePasswordComponent);
    fixture.detectChanges();

    const inputs = fixture.nativeElement.querySelectorAll('input') as NodeListOf<HTMLInputElement>;
    inputs[0].value = 'wrong-password';
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = 'NewAdmin@123';
    inputs[1].dispatchEvent(new Event('input'));
    inputs[2].value = 'NewAdmin@123';
    inputs[2].dispatchEvent(new Event('input'));

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const submitButton = fixture.nativeElement.querySelector('.auth-submit') as HTMLButtonElement;
    expect(fixture.nativeElement.textContent).toContain('Could not change password');
    expect(submitButton.disabled).toBeFalsy();
  });

  it('explains a required password change and hides the dashboard link', () => {
    const fixture = TestBed.createComponent(ChangePasswordComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Your account is using a temporary password');
    expect(fixture.nativeElement.querySelector('a[routerlink="/"]')).toBeNull();
  });

  it('shows a dashboard link for an optional password change', () => {
    requiredChange = false;
    const fixture = TestBed.createComponent(ChangePasswordComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Update the password used to protect your school account.');
    expect(fixture.nativeElement.textContent).toContain('Back to Dashboard');
  });

  it('logs out and confirms a successful password change on the login screen', () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate');
    const fixture = TestBed.createComponent(ChangePasswordComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].setValue({
      currentPassword: 'Admin@123',
      newPassword: 'NewAdmin@123',
      confirmPassword: 'NewAdmin@123',
    });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(logout).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { passwordChanged: 'true' },
    });
  });
});
