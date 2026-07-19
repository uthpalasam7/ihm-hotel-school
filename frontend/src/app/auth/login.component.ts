import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../core/auth/auth.service';
import { AuthLayoutComponent } from './auth-layout.component';

@Component({
  selector: 'app-login',
  imports: [
    AuthLayoutComponent,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    ReactiveFormsModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly showPassword = signal(false);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly infoMessage = signal(this.loginReasonMessage());
  protected readonly successMessage = signal(
    this.route.snapshot.queryParamMap.get('passwordChanged') === 'true'
      ? 'Password changed. Sign in with your new password.'
      : '',
  );

  protected readonly form = this.formBuilder.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  protected submit(): void {
    if (this.loading()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    const { username, password } = this.form.getRawValue();

    this.authService.login(username, password).pipe(
      finalize(() => {
        this.loading.set(false);
      }),
    ).subscribe({
      next: (response) => {
        const target = response.user.passwordChangeRequired ? '/change-password' : '/';
        this.router.navigateByUrl(target);
      },
      error: () => {
        this.errorMessage.set('Invalid username or password');
      },
    });
  }

  private loginReasonMessage(): string {
    switch (this.route.snapshot.queryParamMap.get('reason')) {
      case 'session-expired':
        return 'Your session expired. Sign in again to continue.';
      case 'sign-in-required':
        return 'Sign in to continue to the management system.';
      default:
        return '';
    }
  }
}
