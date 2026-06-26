import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { CurrentUser } from '../core/auth/auth.models';
import { ShellComponent } from './shell.component';

describe('ShellComponent', () => {
  it('shows lecturer navigation without administrator controls', async () => {
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
});
