import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { errorMessage } from '../shared/api-error';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { StatusChipComponent } from '../shared/status-chip.component';
import { Enrollment, EnrollmentStatus } from './enrollment.models';
import { EnrollmentService } from './enrollment.service';

@Component({
  selector: 'app-enrollment-list',
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatPaginatorModule, MatSelectModule, RouterLink, PageHeaderComponent, PageStateComponent, StatusChipComponent],
  template: `
    <section class="enrollment-page">
      <app-page-header eyebrow="Student administration" title="Enrollments" subtitle="Student registrations and their saved charge schedules.">
        <a mat-flat-button routerLink="/enrollments/new" [queryParams]="{studentId: studentId || null, batchId: batchId || null}">Enroll student</a>
      </app-page-header>
      <mat-card appearance="outlined" class="filter-card"><mat-card-content>
        <div class="enrollment-filters" aria-label="Enrollment filters">
          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="search-filter">
            <mat-label>Search</mat-label>
            <input matInput type="search" [formControl]="search" (keyup.enter)="applyFilters()"
              placeholder="Registration number, student or batch" />
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>Status</mat-label>
            <mat-select [formControl]="status">
              <mat-option value="">All statuses</mat-option>
              <mat-option value="ACTIVE">Active</mat-option>
              <mat-option value="SUSPENDED">Suspended</mat-option>
              <mat-option value="COMPLETED">Completed</mat-option>
              <mat-option value="WITHDRAWN">Withdrawn</mat-option>
              <mat-option value="CANCELLED">Cancelled</mat-option>
            </mat-select>
          </mat-form-field>
          <div class="filter-actions">
            <button mat-flat-button type="button" (click)="applyFilters()">Apply filters</button>
            <button mat-button type="button" (click)="clearFilters()">Reset</button>
          </div>
        </div>
        @if (studentId || batchId) { <p class="muted">Showing enrollments for the selected {{ studentId ? 'student' : 'batch' }}. <a routerLink="/enrollments">Show all</a></p> }
      </mat-card-content></mat-card>
      <app-page-state [loading]="loading()" [error]="error()" [empty]="!loading() && !error() && !rows().length"
        emptyTitle="No enrollments found" emptyMessage="No enrollments match the current filters." (retry)="load(page())">
        <button mat-stroked-button type="button" (click)="clearFilters()">Clear filters</button>
      </app-page-state>
      @if (!loading() && !error() && rows().length) {
        <mat-card appearance="outlined" class="ihm-data-grid enrollment-list-grid"><div class="ihm-desktop-table table-wrap"><table>
          <caption class="visually-hidden">Enrollments</caption>
          <thead><tr><th>Registration number</th><th>Student</th><th>Course batch</th><th>Enrolled</th><th>Status</th><th><span class="visually-hidden">Actions</span></th></tr></thead>
          <tbody>@for (row of rows(); track row.id) { <tr>
            <td><a [routerLink]="['/enrollments', row.id]">{{ row.registrationNumber }}</a></td>
            <td><a [routerLink]="['/students', row.studentId]">{{ row.studentName }}</a></td>
            <td>{{ row.batchNumber }}<span class="secondary">{{ row.courseName }}</span></td>
            <td>{{ row.enrollmentDate | date:'d MMM y' }}</td><td><app-status-chip [value]="row.status" /></td>
            <td class="action-cell"><a mat-button [routerLink]="['/enrollments', row.id]" [attr.aria-label]="'View enrollment ' + row.registrationNumber">View</a></td>
          </tr> }</tbody>
        </table></div>
        <div class="ihm-mobile-cards" aria-label="Enrollments">
          @for (row of rows(); track row.id) {
            <article class="ihm-mobile-card">
              <div class="ihm-mobile-card__heading">
                <div><h2>{{ row.studentName }}</h2><p>{{ row.registrationNumber }}</p></div>
                <app-status-chip [value]="row.status" />
              </div>
              <dl class="ihm-mobile-card__details">
                <div><dt>Course batch</dt><dd>{{ row.batchNumber }} — {{ row.courseName }}</dd></div>
                <div><dt>Enrolled</dt><dd>{{ row.enrollmentDate | date:'d MMM y' }}</dd></div>
              </dl>
              <div class="ihm-mobile-card__actions">
                <a mat-button [routerLink]="['/enrollments', row.id]">View enrollment</a>
                <a mat-button [routerLink]="['/students', row.studentId]">View student</a>
              </div>
            </article>
          }
        </div>
        <mat-paginator [length]="total()" [pageIndex]="page()" [pageSize]="pageSize()"
          [pageSizeOptions]="[10, 20, 50, 100]" [showFirstLastButtons]="true"
          (page)="changePage($event)" aria-label="Enrollment pages" /></mat-card>
      }
    </section>
  `,
  styleUrl: './enrollments.scss',
})
export class EnrollmentListComponent implements OnInit {
  private readonly api = inject(EnrollmentService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  protected readonly search = new FormControl('', { nonNullable: true });
  protected readonly status = new FormControl<EnrollmentStatus | ''>('', { nonNullable: true });
  private appliedSearch = '';
  private appliedStatus: EnrollmentStatus | '' = '';
  protected readonly rows = signal<Enrollment[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly page = signal(0);
  protected readonly pageSize = signal(20);
  protected readonly total = signal(0);
  protected studentId?: number;
  protected batchId?: number;
  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      this.studentId = Number(params.get('studentId')) || undefined;
      this.batchId = Number(params.get('batchId')) || undefined;
      this.load(0);
    });
  }
  protected load(page = 0): void {
    this.request?.unsubscribe(); this.loading.set(true); this.error.set(null); this.page.set(page);
    this.request = this.api.list({ search: this.appliedSearch, status: this.appliedStatus,
      studentId: this.studentId, batchId: this.batchId, page, size: this.pageSize() })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: result => { this.rows.set(result.content); this.total.set(result.totalElements); this.loading.set(false); },
        error: error => { this.error.set(errorMessage(error)); this.loading.set(false); },
      });
  }
  protected applyFilters(): void {
    this.appliedSearch = this.search.value.trim();
    this.appliedStatus = this.status.value;
    this.load(0);
  }
  protected clearFilters(): void {
    this.search.setValue('');
    this.status.setValue('');
    this.applyFilters();
  }
  protected changePage(event: PageEvent): void {
    this.pageSize.set(event.pageSize);
    this.load(event.pageIndex);
  }
}
