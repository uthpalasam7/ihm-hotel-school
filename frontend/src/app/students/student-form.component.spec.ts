import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { NotificationService } from '../shared/notification.service';
import { Student } from './student.models';
import { StudentFormComponent } from './student-form.component';
import { StudentService } from './student.service';

@Component({ template: '' })
class BlankComponent {}

describe('StudentFormComponent', () => {
  const student: Student = {
    id: 12, fullName: 'Nimal Perera', nic: '200012345678', contactNumber: '0712345678',
    alternativeContactNumber: null, email: null, address: 'Kurunegala', dateOfBirth: null,
    gender: null, remarks: null, status: 'ACTIVE', photoAvailable: false, photoUrl: null,
    photoThumbnailUrl: null, createdAt: '', updatedAt: '', version: 0,
  };
  let service: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(async () => {
    service = {
      create: vi.fn(() => of(student)), update: vi.fn(() => of(student)), get: vi.fn(() => of(student)),
      findByNic: vi.fn(() => of(student)), uploadPhoto: vi.fn(() => of({ ...student, photoAvailable: true })),
      loadPhoto: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [StudentFormComponent],
      providers: [
        provideNativeDateAdapter(),
        provideRouter([{ path: 'students/:id', component: BlankComponent }, { path: 'students', component: BlankComponent }]),
        { provide: StudentService, useValue: service },
        { provide: NotificationService, useValue: { success: vi.fn(), error: vi.fn() } },
      ],
    }).compileComponents();
  });

  it('blocks save and shows required-field validation', () => {
    const fixture = TestBed.createComponent(StudentFormComponent);
    fixture.detectChanges();
    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(service['create']).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Full name is required');
    expect(fixture.nativeElement.textContent).toContain('NIC is required');
    expect(fixture.nativeElement.textContent).toContain('Contact number is required');
    expect(fixture.nativeElement.textContent).toContain('Address is required');
  });

  it('submits the documented student request', () => {
    const fixture = TestBed.createComponent(StudentFormComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as { form: { patchValue: (value: unknown) => void }; save: () => void };
    component.form.patchValue({ fullName: 'Nimal Perera', nic: '200012345678', contactNumber: '0712345678', address: 'Kurunegala', gender: 'Male' });
    component.save();
    expect(service['create']).toHaveBeenCalledWith(expect.objectContaining({
      fullName: 'Nimal Perera', nic: '200012345678', contactNumber: '0712345678', address: 'Kurunegala', gender: 'Male',
    }));
  });

  it('normalizes NIC and phone input while enforcing phone length', () => {
    const fixture = TestBed.createComponent(StudentFormComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as {
      form: {
        controls: {
          nic: { value: string };
          contactNumber: { value: string; hasError: (error: string) => boolean };
        };
        patchValue: (value: unknown) => void;
      };
      normalizeNicInput: () => void;
      normalizePhoneInput: (controlName: 'contactNumber' | 'alternativeContactNumber') => void;
      save: () => void;
    };
    component.form.patchValue({ fullName: 'Nimal Perera', nic: 'ab-12 v', contactNumber: '07123abc45678', address: 'Kurunegala' });

    component.normalizeNicInput();
    component.normalizePhoneInput('contactNumber');

    expect(component.form.controls.nic.value).toBe('AB12V');
    expect(component.form.controls.contactNumber.value).toBe('0712345678');

    component.form.patchValue({ contactNumber: '07123' });
    component.save();
    expect(component.form.controls.contactNumber.hasError('pattern')).toBe(true);
    expect(service['create']).not.toHaveBeenCalled();
  });

  it('blocks duplicate NIC creation and displays the existing profile', () => {
    const fixture = TestBed.createComponent(StudentFormComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance as unknown as { form: { patchValue: (value: unknown) => void }; checkNic: () => void; save: () => void };
    component.form.patchValue({ nic: '200012345678' });
    component.checkNic();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Existing student found');
    component.save();
    expect(service['create']).not.toHaveBeenCalled();
  });
});
