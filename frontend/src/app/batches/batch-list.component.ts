import { DatePipe } from '@angular/common';
import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { filter, finalize, switchMap } from 'rxjs';
import { Course } from '../courses/course.models';
import { CourseService } from '../courses/course.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { AuthService } from '../core/auth/auth.service';
import { errorMessage } from '../shared/api-error';
import { DateValue, toIsoDate } from '../shared/date-value';
import {
  ConfirmationDialogComponent,
  ConfirmationDialogResult,
} from '../shared/confirmation-dialog.component';
import { NotificationService } from '../shared/notification.service';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { StatusChipComponent } from '../shared/status-chip.component';
import { Batch, BatchStatus } from './batch.models';
import { BatchService } from './batch.service';

@Component({
  selector: 'app-batch-list',
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatPaginatorModule,
    MatSelectModule,
    MatTableModule,
    PageHeaderComponent,
    PageStateComponent,
    RouterLink,
    StatusChipComponent,
  ],
  templateUrl: './batch-list.component.html',
  styleUrl: './batch-list.component.scss',
})
export class BatchListComponent implements OnInit {
  private readonly batchService = inject(BatchService);
  private readonly courseService = inject(CourseService);
  private readonly activeBranchService = inject(ActiveBranchService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);
  private initialized = false;

  protected readonly batches = signal<Batch[]>([]);
  protected readonly courses = signal<Course[]>([]);
  protected readonly loading = signal(false);
  protected readonly updatingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly total = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(20);
  protected readonly displayedColumns = ['batch', 'course', 'branch', 'dates', 'duration', 'status', 'counts', 'actions'];
  protected readonly canManage = this.authService.hasAnyRole(['SUPER_ADMIN', 'ADMIN']);
  protected search = '';
  protected courseId = '';
  protected status = '';
  protected startDateFrom: DateValue = null;
  protected startDateTo: DateValue = null;

  constructor() {
    effect(() => {
      this.activeBranchService.activeBranchId();
      if (this.initialized) {
        this.pageIndex.set(0);
        this.load();
      }
    });
  }

  ngOnInit(): void {
    if (this.canManage) {
      this.courseService.listAllActive().subscribe({
        next: (courses) => this.courses.set(courses),
        error: (error) => this.error.set(errorMessage(error)),
      });
    }
    this.initialized = true;
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.batchService.list({
      search: this.search.trim(),
      courseId: this.courseId,
      status: this.status,
      startDateFrom: toIsoDate(this.startDateFrom),
      startDateTo: toIsoDate(this.startDateTo),
      page: this.pageIndex(),
      size: this.pageSize(),
    }).subscribe({
      next: (page) => {
        this.batches.set(page.content);
        this.total.set(page.totalElements);
        this.pageIndex.set(page.page);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected applyFilters(): void {
    this.pageIndex.set(0);
    this.load();
  }

  protected clearFilters(): void {
    this.search = '';
    this.courseId = '';
    this.status = '';
    this.startDateFrom = null;
    this.startDateTo = null;
    this.applyFilters();
  }

  protected changePage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  protected changeStatus(batch: Batch): void {
    const status: BatchStatus = batch.status === 'CANCELLED' ? 'UPCOMING' : 'CANCELLED';
    this.dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(
      ConfirmationDialogComponent,
      {
        data: {
          title: `${status === 'CANCELLED' ? 'Cancel' : 'Reopen'} ${batch.batchNumber}?`,
          message: status === 'CANCELLED'
            ? 'This will cancel the batch without deleting its academic history.'
            : 'This will return the batch to upcoming status.',
          confirmLabel: status === 'CANCELLED' ? 'Cancel batch' : 'Reopen batch',
          reasonLabel: 'Reason (optional)',
        },
        maxWidth: '34rem',
        width: 'calc(100vw - 2rem)',
      },
    ).afterClosed().pipe(
      filter((result): result is ConfirmationDialogResult => Boolean(result?.confirmed)),
      switchMap((result) => {
        this.updatingId.set(batch.id);
        this.error.set(null);
        return this.batchService.changeStatus(batch.id, status, result.reason);
      }),
      finalize(() => this.updatingId.set(null)),
    ).subscribe({
      next: () => {
        this.notifications.success(`${batch.batchNumber} status updated`);
        this.load();
      },
      error: (error) => {
        const message = errorMessage(error);
        this.error.set(message);
        this.notifications.error(message);
      },
    });
  }
}
