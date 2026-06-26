import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CurrentUser } from '../core/auth/auth.models';
import { AuthService } from '../core/auth/auth.service';
import { ChangePasswordComponent } from './change-password.component';

describe('ChangePasswordComponent', () => {
  let changePassword: AuthService['changePassword'];
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

    await TestBed.configureTestingModule({
      imports: [ChangePasswordComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            changePassword: (currentPassword: string, newPassword: string) => changePassword(currentPassword, newPassword),
            logout: () => undefined,
            requiresPasswordChange: () => true,
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

    const submitButton = fixture.nativeElement.querySelector('.submit-button') as HTMLButtonElement;
    expect(fixture.nativeElement.textContent).toContain('Could not change password');
    expect(submitButton.disabled).toBeFalsy();
  });
});
