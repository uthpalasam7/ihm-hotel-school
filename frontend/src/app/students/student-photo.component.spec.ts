import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { StudentPhotoComponent } from './student-photo.component';
import { StudentService } from './student.service';

describe('StudentPhotoComponent', () => {
  let loadPhoto: ReturnType<typeof vi.fn>;
  let createObjectUrl: ReturnType<typeof vi.fn>;
  let revokeObjectUrl: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    loadPhoto = vi.fn(() => of(new Blob(['image'], { type: 'image/png' })));
    createObjectUrl = vi.fn(() => 'blob:student-photo');
    revokeObjectUrl = vi.fn();
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: createObjectUrl });
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: revokeObjectUrl });
    await TestBed.configureTestingModule({
      imports: [StudentPhotoComponent],
      providers: [{ provide: StudentService, useValue: { loadPhoto } }],
    }).compileComponents();
  });

  it('shows initials without requesting a missing photo', () => {
    const fixture = TestBed.createComponent(StudentPhotoComponent);
    fixture.componentRef.setInput('studentId', 1);
    fixture.componentRef.setInput('fullName', 'Nimal Perera');
    fixture.componentRef.setInput('photoAvailable', false);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('NP');
    expect(loadPhoto).not.toHaveBeenCalled();
  });

  it('loads an authenticated blob and revokes its object URL', () => {
    const fixture = TestBed.createComponent(StudentPhotoComponent);
    fixture.componentRef.setInput('studentId', 1);
    fixture.componentRef.setInput('fullName', 'Nimal Perera');
    fixture.componentRef.setInput('photoAvailable', true);
    fixture.componentRef.setInput('variant', 'thumbnail');
    fixture.detectChanges();
    expect(loadPhoto).toHaveBeenCalledWith(1, 'thumbnail');
    expect(fixture.nativeElement.querySelector('img').getAttribute('src')).toBe('blob:student-photo');
    fixture.destroy();
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:student-photo');
  });
});
