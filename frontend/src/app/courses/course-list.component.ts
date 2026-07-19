import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { filter, finalize, switchMap } from 'rxjs';
import { errorMessage } from '../shared/api-error';
import {
  ConfirmationDialogComponent,
  ConfirmationDialogResult,
} from '../shared/confirmation-dialog.component';
import { NotificationService } from '../shared/notification.service';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { StatusChipComponent } from '../shared/status-chip.component';
import { Course, CourseStatus } from './course.models';
import { CourseService } from './course.service';

@Component({
  selector: 'app-course-list',
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
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
  templateUrl: './course-list.component.html',
  styleUrl: './course-list.component.scss',
})
export class CourseListComponent implements OnInit {
  private readonly courseService = inject(CourseService);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);

  protected readonly courses = signal<Course[]>([]);
  protected readonly loading = signal(false);
  protected readonly updatingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly total = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(20);
  protected readonly displayedColumns = ['course', 'shortCode', 'batchCount', 'status', 'actions'];
  protected search = '';
  protected status = '';

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.courseService.list({
      search: this.search.trim(),
      status: this.status,
      page: this.pageIndex(),
      size: this.pageSize(),
    }).subscribe({
      next: (page) => {
        this.courses.set(page.content);
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
    this.status = '';
    this.applyFilters();
  }

  protected changePage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  protected changeStatus(course: Course): void {
    const status: CourseStatus = course.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(
      ConfirmationDialogComponent,
      {
        data: {
          title: `${status === 'ACTIVE' ? 'Activate' : 'Deactivate'} ${course.shortCode}?`,
          message: `This will mark ${course.name} as ${status.toLowerCase()}. Existing batches and academic history will be preserved.`,
          confirmLabel: status === 'ACTIVE' ? 'Activate course' : 'Deactivate course',
          reasonLabel: 'Reason (optional)',
        },
        maxWidth: '34rem',
        width: 'calc(100vw - 2rem)',
      },
    ).afterClosed().pipe(
      filter((result): result is ConfirmationDialogResult => Boolean(result?.confirmed)),
      switchMap((result) => {
        this.updatingId.set(course.id);
        this.error.set(null);
        return this.courseService.changeStatus(course.id, status, result.reason);
      }),
      finalize(() => this.updatingId.set(null)),
    ).subscribe({
      next: () => {
        this.notifications.success(`${course.shortCode} status updated`);
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
