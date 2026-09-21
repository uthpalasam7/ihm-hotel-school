import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { BranchService } from '../branches/branch.service';
import { CourseService } from '../courses/course.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { toIsoDate } from '../shared/date-value';
import { UserService } from '../users/user.service';
import { BatchFormComponent } from './batch-form.component';
import { BatchService } from './batch.service';

@Component({ template: '' })
class BlankComponent {}

describe('BatchFormComponent', () => {
  let fixture: ComponentFixture<BatchFormComponent>;
  let batchService: {
    create: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    get: ReturnType<typeof vi.fn>;
    getFeePlan: ReturnType<typeof vi.fn>;
    saveFeePlan: ReturnType<typeof vi.fn>;
    lecturers: ReturnType<typeof vi.fn>;
    addLecturer: ReturnType<typeof vi.fn>;
    syncLecturers: ReturnType<typeof vi.fn>;
  };
  let routeId: string | null;

  beforeEach(async () => {
    localStorage.clear();
    routeId = null;
    batchService = {
      create: vi.fn(() => of({
      id: 30,
      course: { id: 5, name: 'Pastry & Bakery', shortCode: 'PB' },
      branch: { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
      batchNumber: '2026/PB02',
      startDate: '2026-07-01',
      endDate: '2026-12-31',
      durationMonths: 6,
      scheduleMode: 'REGULAR',
      status: 'UPCOMING',
      remarks: null,
      registrationSequence: 0,
      lecturerCount: 0,
      studentCount: 0,
      feePlanConfigured: false,
      createdAt: '2026-06-28T00:00:00Z',
      updatedAt: '2026-06-28T00:00:00Z',
      version: 0,
      })),
      update: vi.fn(),
      get: vi.fn(),
      getFeePlan: vi.fn(),
      saveFeePlan: vi.fn(() => of({
      id: 40,
      batchId: 30,
      registrationFee: 5000,
      courseFee: 10000,
      examinationFee: 2500,
      durationMonths: 3,
      monthlyDueDay: 31,
      examinationDueDate: '2026-10-31',
      currencyCode: 'LKR',
      status: 'ACTIVE',
      createdAt: '2026-06-28T00:00:00Z',
      updatedAt: '2026-06-28T00:00:00Z',
      version: 0,
      })),
      lecturers: vi.fn(),
      addLecturer: vi.fn(() => of({
      id: 50,
      batchId: 30,
      lecturerUserId: 9,
      lecturerFullName: 'Chef Lecturer',
      lecturerUsername: 'chef',
      assignmentStartDate: '2026-07-01',
      assignmentEndDate: null,
      status: 'ACTIVE',
      createdAt: '2026-06-28T00:00:00Z',
      updatedAt: '2026-06-28T00:00:00Z',
      })),
      syncLecturers: vi.fn((batchId: number, request: { lecturerUserIds: number[]; assignmentStartDate: string; assignmentEndDate?: string | null }) => of(request.lecturerUserIds.map((lecturerUserId) => ({
        id: 50 + lecturerUserId,
        batchId,
        lecturerUserId,
        lecturerFullName: lecturerUserId === 10 ? 'Pastry Lecturer' : 'Chef Lecturer',
        lecturerUsername: lecturerUserId === 10 ? 'pastry' : 'chef',
        assignmentStartDate: request.assignmentStartDate,
        assignmentEndDate: request.assignmentEndDate ?? null,
        status: 'ACTIVE',
        createdAt: '2026-06-28T00:00:00Z',
        updatedAt: '2026-06-28T00:00:00Z',
      })))),
    };

    await TestBed.configureTestingModule({
      imports: [BatchFormComponent],
      providers: [
        provideRouter([
          { path: 'batches', component: BlankComponent },
          { path: 'batches/:id/edit', component: BatchFormComponent },
        ]),
        provideNativeDateAdapter(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => key === 'id' ? routeId : null,
              },
            },
          },
        },
        { provide: BatchService, useValue: batchService },
        {
          provide: CourseService,
          useValue: {
            listAllActive: () => of([
              { id: 5, name: 'Pastry & Bakery', shortCode: 'PB', description: null, status: 'ACTIVE', batchCount: 0, createdAt: '', updatedAt: '', version: 0 },
            ]),
          },
        },
        {
          provide: BranchService,
          useValue: {
            listAllActive: () => of([
              { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School', status: 'ACTIVE', defaultBranch: true, createdAt: '', updatedAt: '', version: 0 },
            ]),
          },
        },
        {
          provide: UserService,
          useValue: {
            list: () => of({
              content: [
                { id: 9, username: 'chef', email: null, fullName: 'Chef Lecturer', contactNumber: null, status: 'ACTIVE', roles: [], branches: [], createdAt: '', updatedAt: '', version: 0 },
                { id: 10, username: 'pastry', email: null, fullName: 'Pastry Lecturer', contactNumber: null, status: 'ACTIVE', roles: [], branches: [], createdAt: '', updatedAt: '', version: 0 },
              ],
              page: 0,
              size: 100,
              totalElements: 2,
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

  it.each([
    ['01/07/2026', '2026-07-01', '2026-12-31'],
    ['29/02/2028', '2028-02-29', '2028-08-28'],
  ])('interprets typed date %s as day/month/year', async (typed, start, end) => {
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[formControlName="startDate"]');
    input.value = typed;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
    const controls = fixture.componentInstance['generalForm'].controls;

    expect(toIsoDate(controls.startDate.value)).toBe(start);
    expect(toIsoDate(controls.endDate.value)).toBe(end);
    expect(controls.startDate.valid).toBe(true);
  });

  it.each(['31/02/2026', '29/02/2027', '13/31/2026', '00/01/2026', 'not a date'])(
    'rejects invalid typed date %s', async (typed) => {
      fixture = TestBed.createComponent(BatchFormComponent);
      fixture.detectChanges();
      await fixture.whenStable();
      const input: HTMLInputElement = fixture.nativeElement.querySelector('input[formControlName="startDate"]');
      input.value = typed;
      input.dispatchEvent(new Event('input'));
      input.dispatchEvent(new Event('blur'));
      fixture.detectChanges();
      const controls = fixture.componentInstance['generalForm'].controls;
      expect(controls.startDate.hasError('matDatepickerParse')).toBe(true);
      expect(controls.endDate.value).toBeNull();
      expect(fixture.nativeElement.textContent).toContain('Enter a valid date as DD/MM/YYYY');
    },
  );

  it.each([
    ['2', '2026/PB02'],
    ['002', '2026/PB02'],
    ['123', '2026/PB123'],
  ])('builds a full batch identifier from numeric entry %s', async (sequence, expected) => {
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const controls = fixture.componentInstance['generalForm'].controls;
    controls.startDate.setValue(new Date(2026, 6, 1));
    controls.batchSequence.setValue(sequence);
    fixture.detectChanges();

    expect(controls.batchNumber.value).toBe(expected);
    expect(fixture.nativeElement.textContent).toContain(`Full batch number: ${expected}`);
    expect(fixture.nativeElement.querySelector('input[formControlName="batchNumber"]')).toBeNull();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[formControlName="batchSequence"]');
    input.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
    expect(input.value).toBe(expected.replace('2026/PB', ''));
  });

  it('updates the prefix from the chosen start year and course without losing the number', async () => {
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const component = fixture.componentInstance;
    const controls = component['generalForm'].controls;
    controls.batchSequence.setValue('2');
    expect(controls.batchNumber.value).toBe('');
    expect(component['batchNumberPrefix']()).toBe('YYYY/PB');
    controls.startDate.setValue(new Date(2027, 0, 1));
    expect(controls.batchNumber.value).toBe('2027/PB02');
    component['courses'].update((courses) => [...courses, { ...courses[0], id: 6, shortCode: 'FB' }]);
    controls.courseId.setValue(6);
    expect(controls.batchNumber.value).toBe('2027/FB02');
    expect(controls.batchSequence.value).toBe('2');
    controls.startDate.setValue(null);
    expect(controls.batchNumber.value).toBe('');
    controls.startDate.setValue(new Date(2027, 0, 1));
    controls.courseId.setValue(0);
    expect(controls.batchNumber.value).toBe('');
  });

  it.each(['', '0', '000', '-1', '1.5', 'PB02', '2/3', '1'.repeat(36)])(
    'rejects invalid numeric batch entry %s', async (sequence) => {
      fixture = TestBed.createComponent(BatchFormComponent);
      fixture.detectChanges();
      await fixture.whenStable();
      const component = fixture.componentInstance;
      const controls = component['generalForm'].controls;
      controls.startDate.setValue(new Date(2026, 6, 1));
      controls.batchSequence.setValue(sequence);
      expect(controls.batchSequence.invalid).toBe(true);
      expect(controls.batchNumber.value).toBe('');
      component['save']();
      expect(batchService.create).not.toHaveBeenCalled();
    },
  );

  it.each([
    ['2026-07-01', 6, '2026-12-31'],
    ['2026-07-15', 6, '2027-01-14'],
    ['2026-01-31', 1, '2026-02-28'],
    ['2028-01-31', 1, '2028-02-29'],
    ['2028-02-29', 12, '2029-02-28'],
    ['2026-12-01', 1, '2026-12-31'],
  ])('suggests an inclusive end date for %s and %i months', async (start, months, expected) => {
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const controls = fixture.componentInstance['generalForm'].controls;

    controls.durationMonths.setValue(months);
    controls.startDate.setValue(start);

    expect(toIsoDate(controls.endDate.value)).toBe(expected);
  });

  it('updates automatic dates when either input changes and rejects incomplete or fractional duration', async () => {
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const controls = fixture.componentInstance['generalForm'].controls;

    controls.startDate.setValue(new Date(2026, 6, 1));
    controls.durationMonths.setValue(3);
    expect(toIsoDate(controls.endDate.value)).toBe('2026-09-30');
    controls.startDate.setValue(new Date(2026, 7, 1));
    expect(toIsoDate(controls.endDate.value)).toBe('2026-10-31');
    controls.startDate.setValue(null);
    expect(controls.endDate.value).toBeNull();
    controls.startDate.setValue(new Date(2026, 7, 1));
    for (const months of [0, -1, 1.5]) {
      controls.durationMonths.setValue(months);
      expect(controls.durationMonths.invalid).toBe(true);
      expect(controls.endDate.value).toBeNull();
    }
    controls.durationMonths.setValue(6);
    expect(toIsoDate(controls.endDate.value)).toBe('2027-01-31');
    controls.startDate.setValue(new Date(NaN));
    expect(controls.endDate.value).toBeNull();
  });

  it('keeps a manual date until the user explicitly restores calculation', async () => {
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const component = fixture.componentInstance;
    const controls = component['generalForm'].controls;
    controls.startDate.setValue(new Date(2026, 6, 1));
    controls.endDate.setValue(new Date(2027, 0, 20));
    controls.durationMonths.setValue(3);
    controls.startDate.setValue(new Date(2026, 7, 1));

    expect(toIsoDate(controls.endDate.value)).toBe('2027-01-20');
    fixture.detectChanges();
    const button = Array.from(fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>)
      .find((item) => item.textContent?.includes('Use calculated end date'));
    expect(button).toBeDefined();
    button!.click();
    expect(toIsoDate(controls.endDate.value)).toBe('2026-10-31');
    expect(component.hasUnsavedChanges()).toBe(true);
    controls.durationMonths.setValue(4);
    expect(toIsoDate(controls.endDate.value)).toBe('2026-11-30');
    expect(component['feeForm'].controls.examinationDueDate.value).toBeNull();
  });

  it('preserves a saved end date on load and subsequent input changes', async () => {
    routeId = '30';
    batchService.get.mockReturnValue(of({
      id: 30,
      course: { id: 5, name: 'Pastry & Bakery', shortCode: 'PB' },
      branch: { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
      batchNumber: '2026/PB02',
      startDate: '2026-07-01',
      endDate: '2027-01-20',
      durationMonths: 6,
      status: 'UPCOMING',
      scheduleMode: 'REGULAR',
      remarks: null,
    }));
    batchService.getFeePlan.mockReturnValue(of(null));
    batchService.lecturers.mockReturnValue(of([]));
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const component = fixture.componentInstance;
    const controls = component['generalForm'].controls;

    expect(toIsoDate(controls.endDate.value)).toBe('2027-01-20');
    expect(component.hasUnsavedChanges()).toBe(false);
    controls.durationMonths.setValue(3);
    controls.startDate.setValue(new Date(2026, 7, 1));
    expect(toIsoDate(controls.endDate.value)).toBe('2027-01-20');
    expect(controls.batchNumber.value).toBe('2026/PB02');
    expect(controls.batchSequence.disabled).toBe(true);
    component['useCalculatedEndDate']();
    expect(toIsoDate(controls.endDate.value)).toBe('2026-10-31');
  });

  it('places duration before dates and fills the end-date input', async () => {
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const controls = fixture.componentInstance['generalForm'].controls;
    controls.startDate.setValue(new Date(2026, 6, 1));
    fixture.detectChanges();
    const inputs = Array.from(fixture.nativeElement.querySelectorAll('input[formControlName]') as NodeListOf<HTMLInputElement>);
    const names = inputs.map((input) => input.getAttribute('formControlName'));

    expect(names.indexOf('durationMonths')).toBeLessThan(names.indexOf('startDate'));
    expect(inputs.find((input) => input.getAttribute('formControlName') === 'endDate')?.value).toContain('2026');
  });

  it('previews installments using the final day when the due day is unavailable', async () => {
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      generalForm: { patchValue: (value: unknown) => void };
      feeForm: { patchValue: (value: unknown) => void };
      previewCharges: () => Array<{ type: string; dueDate: string; amount: number }>;
    };
    component.generalForm.patchValue({ startDate: '2026-01-30', durationMonths: 3 });
    component.feeForm.patchValue({ courseFee: 10000, registrationFee: 5000, examinationFee: 2500, monthlyDueDay: 31, examinationDueDate: '2026-04-30' });

    const charges = component.previewCharges();

    expect(charges.filter((charge) => charge.type === 'COURSE_INSTALLMENT').map((charge) => charge.dueDate)).toEqual([
      '2026-01-31',
      '2026-02-28',
      '2026-03-31',
    ]);
    expect(charges.reduce((total, charge) => total + charge.amount, 0)).toBe(17500);
  });

  it('blocks guided navigation until the current step is valid', async () => {
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const next = vi.fn();
    const component = fixture.componentInstance as unknown as {
      generalForm: {
        patchValue: (value: unknown) => void;
        markAllAsTouched: () => void;
        invalid: boolean;
      };
      next: (stepper: { next: () => void }, control: unknown) => void;
    };
    component.generalForm.patchValue({ batchNumber: '', startDate: '', endDate: '' });

    component.next({ next }, component.generalForm);

    expect(next).not.toHaveBeenCalled();
  });

  it('validates the batch date order', async () => {
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      generalForm: {
        patchValue: (value: unknown) => void;
        hasError: (error: string) => boolean;
      };
    };
    component.generalForm.patchValue({ startDate: '2026-08-01', endDate: '2026-07-01' });

    expect(component.generalForm.hasError('dateOrder')).toBe(true);
  });

  it('saves the batch, fee plan, and selected lecturer assignment', async () => {
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      generalForm: { patchValue: (value: unknown) => void };
      feeForm: { patchValue: (value: unknown) => void };
      scheduleForm: { patchValue: (value: unknown) => void };
      selectedLecturerIds: number[];
      save: () => void;
    };
    component.generalForm.patchValue({
      courseId: 5,
      branchId: 1,
      batchSequence: '2',
      startDate: new Date(2026, 6, 1),
      endDate: new Date(2026, 11, 31),
      durationMonths: 6,
      status: 'UPCOMING',
    });
    component.feeForm.patchValue({
      registrationFee: 5000,
      courseFee: 10000,
      examinationFee: 2500,
      monthlyDueDay: 10,
      examinationDueDate: new Date(2026, 11, 15),
      currencyCode: 'LKR',
    });
    component.scheduleForm.patchValue({ scheduleMode: 'MANUAL' });
    component.selectedLecturerIds = [9];

    component.save();

    expect(batchService.create).toHaveBeenCalledWith(expect.objectContaining({
      courseId: 5,
      branchId: 1,
      batchNumber: '2026/PB02',
      startDate: '2026-07-01',
      endDate: '2026-12-31',
      scheduleMode: 'MANUAL',
    }));
    expect(batchService.saveFeePlan).toHaveBeenCalledWith(30, expect.objectContaining({
      registrationFee: 5000,
      courseFee: 10000,
      examinationFee: 2500,
      examinationDueDate: '2026-12-15',
      currencyCode: 'LKR',
    }));
    expect(batchService.syncLecturers).toHaveBeenCalledWith(30, expect.objectContaining({
      lecturerUserIds: [9],
      assignmentStartDate: '2026-07-01',
      assignmentEndDate: null,
    }));
    expect(batchService.addLecturer).not.toHaveBeenCalled();
  });

  it('allows saving a batch without immediate lecturer assignment', async () => {
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      generalForm: { patchValue: (value: unknown) => void };
      feeForm: { patchValue: (value: unknown) => void };
      scheduleForm: { patchValue: (value: unknown) => void };
      selectedLecturerIds: number[];
      save: () => void;
    };
    component.generalForm.patchValue({
      courseId: 5,
      branchId: 1,
      batchSequence: '3',
      startDate: '2026-08-01',
      endDate: '2026-12-31',
      durationMonths: 5,
      status: 'UPCOMING',
    });
    component.feeForm.patchValue({
      registrationFee: 5000,
      courseFee: 10000,
      examinationFee: 2500,
      monthlyDueDay: 10,
      examinationDueDate: '2026-12-15',
      currencyCode: 'LKR',
    });
    component.scheduleForm.patchValue({ scheduleMode: 'REGULAR' });
    component.selectedLecturerIds = [];

    component.save();

    expect(batchService.create).toHaveBeenCalledWith(expect.objectContaining({ batchNumber: '2026/PB03' }));
    expect(batchService.syncLecturers).not.toHaveBeenCalled();
    expect(batchService.addLecturer).not.toHaveBeenCalled();
  });

  it('preselects active lecturer assignments during edit', async () => {
    batchService.get.mockReturnValue(of({
      id: 30,
      course: { id: 5, name: 'Pastry & Bakery', shortCode: 'PB' },
      branch: { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
      batchNumber: '2026/PB02',
      startDate: '2026-07-01',
      endDate: '2026-12-31',
      durationMonths: 6,
      scheduleMode: 'REGULAR',
      status: 'UPCOMING',
      remarks: null,
      registrationSequence: 0,
      lecturerCount: 1,
      studentCount: 0,
      feePlanConfigured: true,
      createdAt: '2026-06-28T00:00:00Z',
      updatedAt: '2026-06-28T00:00:00Z',
      version: 0,
    }));
    batchService.getFeePlan.mockReturnValue(of({
      id: 40,
      batchId: 30,
      registrationFee: 5000,
      courseFee: 10000,
      examinationFee: 2500,
      durationMonths: 6,
      monthlyDueDay: 10,
      examinationDueDate: '2026-12-15',
      currencyCode: 'LKR',
      status: 'ACTIVE',
      createdAt: '2026-06-28T00:00:00Z',
      updatedAt: '2026-06-28T00:00:00Z',
      version: 0,
    }));
    batchService.lecturers.mockReturnValue(of([
      {
        id: 70,
        batchId: 30,
        lecturerUserId: 9,
        lecturerFullName: 'Chef Lecturer',
        lecturerUsername: 'chef',
        assignmentStartDate: '2026-07-01',
        assignmentEndDate: '2026-10-31',
        status: 'ACTIVE',
        createdAt: '2026-06-28T00:00:00Z',
        updatedAt: '2026-06-28T00:00:00Z',
      },
    ]));

    routeId = '30';
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      selectedLecturerIds: number[];
      isLecturerSelected: (id: number) => boolean;
      selectedLecturerSummaries: () => Array<{ fullName: string }>;
      assignments: () => Array<{ assignmentStartDate: string; assignmentEndDate: string | null }>;
    };

    expect(component.selectedLecturerIds).toEqual([9]);
    expect(component.isLecturerSelected(9)).toBe(true);
    expect(component.selectedLecturerSummaries().map((lecturer) => lecturer.fullName)).toEqual(['Chef Lecturer']);
    expect(component.assignments()[0]).toEqual(expect.objectContaining({
      assignmentStartDate: '2026-07-01',
      assignmentEndDate: '2026-10-31',
    }));
  });

  it('syncs edited lecturer additions and removals without duplicate submissions', async () => {
    const batch = {
      id: 30,
      course: { id: 5, name: 'Pastry & Bakery', shortCode: 'PB' },
      branch: { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
      batchNumber: '2026/PB02',
      startDate: '2026-07-01',
      endDate: '2026-12-31',
      durationMonths: 6,
      scheduleMode: 'REGULAR',
      status: 'UPCOMING',
      remarks: null,
      registrationSequence: 0,
      lecturerCount: 1,
      studentCount: 0,
      feePlanConfigured: true,
      createdAt: '2026-06-28T00:00:00Z',
      updatedAt: '2026-06-28T00:00:00Z',
      version: 0,
    };
    batchService.update.mockReturnValue(of(batch));
    batchService.get.mockReturnValue(of(batch));
    batchService.getFeePlan.mockReturnValue(of({
      id: 40,
      batchId: 30,
      registrationFee: 5000,
      courseFee: 10000,
      examinationFee: 2500,
      durationMonths: 6,
      monthlyDueDay: 10,
      examinationDueDate: '2026-12-15',
      currencyCode: 'LKR',
      status: 'ACTIVE',
      createdAt: '2026-06-28T00:00:00Z',
      updatedAt: '2026-06-28T00:00:00Z',
      version: 0,
    }));
    batchService.lecturers.mockReturnValue(of([
      {
        id: 70,
        batchId: 30,
        lecturerUserId: 9,
        lecturerFullName: 'Chef Lecturer',
        lecturerUsername: 'chef',
        assignmentStartDate: '2026-07-01',
        assignmentEndDate: null,
        status: 'ACTIVE',
        createdAt: '2026-06-28T00:00:00Z',
        updatedAt: '2026-06-28T00:00:00Z',
      },
    ]));

    routeId = '30';
    fixture = TestBed.createComponent(BatchFormComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const component = fixture.componentInstance as unknown as {
      selectedLecturerIds: number[];
      toggleLecturer: (id: number, selected: boolean) => void;
      removeSelectedLecturer: (id: number) => void;
      save: () => void;
    };
    component.toggleLecturer(10, true);
    component.toggleLecturer(10, true);
    component.removeSelectedLecturer(9);

    component.save();
    component.save();

    expect(batchService.syncLecturers).toHaveBeenCalledTimes(1);
    expect(batchService.syncLecturers).toHaveBeenCalledWith(30, expect.objectContaining({
      lecturerUserIds: [10],
      assignmentStartDate: '2026-07-01',
      assignmentEndDate: null,
    }));
    expect(batchService.addLecturer).not.toHaveBeenCalled();
  });
});
