import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { BranchService } from '../branches/branch.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { UserListComponent } from './user-list.component';
import { UserService } from './user.service';

describe('UserListComponent', () => {
  let fixture: ComponentFixture<UserListComponent>;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [UserListComponent],
      providers: [
        provideRouter([]),
        {
          provide: UserService,
          useValue: {
            roles: () => of([{ id: 3, code: 'LECTURER', name: 'Lecturer' }]),
            list: () => of({
              content: [{
                id: 20,
                username: 'multi_lecturer',
                email: 'multi@example.invalid',
                fullName: 'Multi Branch Lecturer',
                contactNumber: null,
                status: 'ACTIVE',
                roles: [{ id: 3, code: 'LECTURER', name: 'Lecturer' }],
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
              }],
              page: 0,
              size: 20,
              totalElements: 1,
              totalPages: 1,
            }),
          },
        },
        {
          provide: BranchService,
          useValue: {
            list: () => of({
              content: [
                {
                  id: 1,
                  code: 'IHM-MAIN',
                  name: 'IHM Hotel School',
                  status: 'ACTIVE',
                  defaultBranch: true,
                  createdAt: '2026-06-27T00:00:00Z',
                  updatedAt: '2026-06-27T00:00:00Z',
                  version: 0,
                },
              ],
              page: 0,
              size: 100,
              totalElements: 1,
              totalPages: 1,
            }),
          },
        },
      ],
    }).compileComponents();

    TestBed.inject(ActiveBranchService).configure([{ id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' }]);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('displays all assigned branch badges for a user', async () => {
    fixture = TestBed.createComponent(UserListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('IHM-MAIN');
    expect(text).toContain('IHM-CITY');
    expect(text).toContain('IHM-NORTH');
    expect(text).toContain('IHM-SOUTH');
  });
});
