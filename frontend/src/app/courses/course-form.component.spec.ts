import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { CourseFormComponent } from './course-form.component';
import { CourseService } from './course.service';

@Component({ template: '' })
class BlankComponent {}

describe('CourseFormComponent', () => {
  let fixture: ComponentFixture<CourseFormComponent>;
  let courseService: {
    create: ReturnType<typeof vi.fn>;
    get: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    courseService = {
      create: vi.fn(() => of({
        id: 10,
        name: 'Pastry & Bakery',
        shortCode: 'PB',
        description: null,
        status: 'ACTIVE' as const,
        batchCount: 0,
        createdAt: '2026-06-28T00:00:00Z',
        updatedAt: '2026-06-28T00:00:00Z',
        version: 0,
      })),
      get: vi.fn(),
      update: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [CourseFormComponent],
      providers: [
        provideRouter([{ path: 'courses', component: BlankComponent }]),
        { provide: CourseService, useValue: courseService },
      ],
    }).compileComponents();
  });

  it('blocks save when required course fields are missing', () => {
    fixture = TestBed.createComponent(CourseFormComponent);
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('form') as HTMLFormElement;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(courseService.create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Name is required');
    expect(fixture.nativeElement.textContent).toContain('Short code is required');
  });

  it('submits a valid course request', () => {
    fixture = TestBed.createComponent(CourseFormComponent);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      form: { patchValue: (value: unknown) => void };
      save: () => void;
    };
    component.form.patchValue({
      name: 'Pastry & Bakery',
      shortCode: 'pb',
      description: 'Certificate course',
      status: 'ACTIVE',
    });
    component.save();

    expect(courseService.create).toHaveBeenCalledWith({
      name: 'Pastry & Bakery',
      shortCode: 'pb',
      description: 'Certificate course',
      status: 'ACTIVE',
    });
  });
});
