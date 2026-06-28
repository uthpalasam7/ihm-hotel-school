import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { BranchService } from './branch.service';
import { BranchListComponent } from './branch-list.component';

describe('BranchListComponent', () => {
  let fixture: ComponentFixture<BranchListComponent>;
  let list = () => of({
    content: [{
      id: 1,
      code: 'IHM-MAIN',
      name: 'IHM Hotel School',
      address: null,
      contactNumber: null,
      status: 'ACTIVE' as const,
      defaultBranch: true,
      createdAt: '2026-06-27T00:00:00Z',
      updatedAt: '2026-06-27T00:00:00Z',
      version: 0,
    }],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BranchListComponent],
      providers: [
        provideRouter([]),
        {
          provide: BranchService,
          useValue: {
            list: () => list(),
            changeStatus: () => of({}),
          },
        },
      ],
    }).compileComponents();
  });

  it('renders loaded branches', async () => {
    fixture = TestBed.createComponent(BranchListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('IHM-MAIN');
    expect(fixture.nativeElement.textContent).toContain('IHM Hotel School');
  });

  it('shows an error state when loading fails', async () => {
    list = () => throwError(() => ({ error: { message: 'Access denied' } }));
    fixture = TestBed.createComponent(BranchListComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Access denied');
  });
});
