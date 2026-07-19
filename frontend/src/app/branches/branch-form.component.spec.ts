import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { NotificationService } from '../shared/notification.service';
import { BranchFormComponent } from './branch-form.component';
import { BranchService } from './branch.service';

@Component({ template: '' })
class BlankComponent {}

const existingBranch = {
  id: 7,
  code: 'IHM-CITY',
  name: 'IHM City',
  address: 'Colombo',
  contactNumber: '0111234567',
  status: 'ACTIVE' as const,
  defaultBranch: false,
  createdAt: '2026-07-01T00:00:00Z',
  updatedAt: '2026-07-01T00:00:00Z',
  version: 0,
};

describe('BranchFormComponent', () => {
  let fixture: ComponentFixture<BranchFormComponent>;
  let branchService: {
    create: ReturnType<typeof vi.fn>;
    get: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };
  let notifications: { success: ReturnType<typeof vi.fn>; error: ReturnType<typeof vi.fn> };

  async function configure(editingId: string | null = null): Promise<void> {
    branchService = {
      create: vi.fn(() => of(existingBranch)),
      get: vi.fn(() => of(existingBranch)),
      update: vi.fn(() => of(existingBranch)),
    };
    notifications = { success: vi.fn(), error: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [BranchFormComponent],
      providers: [
        provideRouter([
          { path: 'branches', component: BlankComponent },
          { path: 'branches/new', component: BranchFormComponent },
        ]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: (key: string) => key === 'id' ? editingId : null },
            },
          },
        },
        { provide: BranchService, useValue: branchService },
        { provide: NotificationService, useValue: notifications },
      ],
    }).compileComponents();
  }

  it('blocks create while required fields are missing', async () => {
    await configure();
    fixture = TestBed.createComponent(BranchFormComponent);
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(branchService.create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Code is required');
    expect(fixture.nativeElement.textContent).toContain('Name is required');
  });

  it('submits the existing API request and reports success', async () => {
    await configure();
    fixture = TestBed.createComponent(BranchFormComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      form: { patchValue: (value: unknown) => void };
      save: () => void;
    };
    component.form.patchValue({
      code: 'ihm-city',
      name: 'IHM City',
      address: 'Colombo',
      contactNumber: '0111234567',
      status: 'ACTIVE',
    });
    component.save();
    await fixture.whenStable();

    expect(branchService.create).toHaveBeenCalledWith({
      code: 'ihm-city',
      name: 'IHM City',
      address: 'Colombo',
      contactNumber: '0111234567',
      status: 'ACTIVE',
    });
    expect(notifications.success).toHaveBeenCalledWith('Branch created successfully');
    expect(TestBed.inject(Router).url).toBe('/branches');
  });

  it('loads edit values and updates the same branch', async () => {
    await configure('7');
    fixture = TestBed.createComponent(BranchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      form: { controls: { code: { value: string }; name: { value: string } } };
      save: () => void;
    };
    expect(component.form.controls.code.value).toBe('IHM-CITY');
    expect(component.form.controls.name.value).toBe('IHM City');

    component.save();
    expect(branchService.update).toHaveBeenCalledWith(7, expect.objectContaining({ code: 'IHM-CITY' }));
  });

  it('preserves server field validation and allows retry after a failed save', async () => {
    await configure();
    branchService.create.mockReturnValue(throwError(() => ({
      error: {
        message: 'Request validation failed',
        fieldErrors: [{ field: 'code', message: 'Branch code already exists' }],
      },
    })));
    fixture = TestBed.createComponent(BranchFormComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      form: {
        patchValue: (value: unknown) => void;
        markAsDirty: () => void;
        controls: { code: { hasError: (name: string) => boolean } };
      };
      save: () => void;
      hasUnsavedChanges: () => boolean;
    };
    component.form.patchValue({ code: 'IHM-MAIN', name: 'Duplicate', status: 'ACTIVE' });
    component.form.markAsDirty();
    component.save();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Branch code already exists');
    expect(component.form.controls.code.hasError('server')).toBe(true);
    expect(component.hasUnsavedChanges()).toBe(true);
    expect(notifications.error).toHaveBeenCalledWith('Request validation failed');
  });
});
