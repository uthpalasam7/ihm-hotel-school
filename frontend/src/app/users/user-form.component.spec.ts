import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { BranchService } from '../branches/branch.service';
import { UserFormComponent } from './user-form.component';
import { UserService } from './user.service';

describe('UserFormComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserFormComponent],
      providers: [
        provideRouter([]),
        {
          provide: UserService,
          useValue: {
            roles: () => of([{ id: 3, code: 'LECTURER', name: 'Lecturer' }]),
            create: () => of({
              id: 10,
              username: 'lecturer',
              fullName: 'Lecturer User',
              status: 'PASSWORD_CHANGE_REQUIRED',
              roles: [{ id: 3, code: 'LECTURER', name: 'Lecturer' }],
              branches: [{ id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' }],
              createdAt: '2026-06-27T00:00:00Z',
              updatedAt: '2026-06-27T00:00:00Z',
              version: 0,
              temporaryPassword: 'TempPass123',
            }),
          },
        },
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
                createdAt: '2026-06-27T00:00:00Z',
                updatedAt: '2026-06-27T00:00:00Z',
                version: 0,
              }],
              page: 0,
              size: 100,
              totalElements: 1,
              totalPages: 1,
            }),
          },
        },
      ],
    }).compileComponents();
  });

  it('auto-selects the only assignable lecturer role and branch', async () => {
    const fixture = TestBed.createComponent(UserFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    const roleCheckbox = fixture.nativeElement.querySelector('fieldset input') as HTMLInputElement;
    expect(text).toContain('Lecturer');
    expect(text).toContain('IHM-MAIN');
    expect(roleCheckbox.checked).toBe(true);
  });
});
