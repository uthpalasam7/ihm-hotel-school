import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AccessDeniedComponent } from './access-denied.component';

describe('AccessDeniedComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccessDeniedComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('explains the restriction without exposing role details', () => {
    const fixture = TestBed.createComponent(AccessDeniedComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('You don’t have access to this page');
    expect(fixture.nativeElement.textContent).toContain('Back to Dashboard');
    expect(fixture.nativeElement.textContent).not.toContain('SUPER_ADMIN');
  });
});
