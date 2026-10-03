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
import { MatTableModule } from '@angular/material/table';
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
    MatInputModule, MatPaginatorModule, MatSelectModule, MatTableModule, RouterLink, PageHeaderComponent, PageStateComponent, StatusChipComponent],
  templateUrl: './enrollment-list.component.html',
  styleUrl: './enrollments.scss',
})
export class EnrollmentListComponent implements OnInit {
  protected readonly displayedColumns = ['registrationNumber', 'student', 'batch', 'enrollmentDate', 'status', 'actions'];
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
