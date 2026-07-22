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
import { ConfirmationDialogComponent, ConfirmationDialogResult } from '../shared/confirmation-dialog.component';
import { NotificationService } from '../shared/notification.service';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { StatusChipComponent } from '../shared/status-chip.component';
import { Student, StudentStatus } from './student.models';
import { StudentPhotoComponent } from './student-photo.component';
import { StudentService } from './student.service';

@Component({
  selector: 'app-student-list',
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
    StudentPhotoComponent,
  ],
  templateUrl: './student-list.component.html',
  styleUrl: './student-list.component.scss',
})
export class StudentListComponent implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);

  protected readonly students = signal<Student[]>([]);
  protected readonly loading = signal(false);
  protected readonly updatingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly total = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(20);
  protected readonly displayedColumns = ['student', 'nic', 'contact', 'status', 'actions'];
  protected search = '';
  protected status = '';

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.studentService.list({
      search: this.search.trim(),
      status: this.status,
      page: this.pageIndex(),
      size: this.pageSize(),
    }).subscribe({
      next: (page) => {
        this.students.set(page.content);
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

  protected changeStatus(student: Student): void {
    const status: StudentStatus = student.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(ConfirmationDialogComponent, {
      data: {
        title: `${status === 'ACTIVE' ? 'Activate' : 'Deactivate'} ${student.fullName}?`,
        message: status === 'ACTIVE'
          ? 'This will make the student available for active workflows again.'
          : 'This keeps the student record and all future history while marking it inactive.',
        confirmLabel: status === 'ACTIVE' ? 'Activate student' : 'Deactivate student',
        reasonLabel: 'Reason (optional)',
      },
      maxWidth: '34rem',
      width: 'calc(100vw - 2rem)',
    }).afterClosed().pipe(
      filter((result): result is ConfirmationDialogResult => Boolean(result?.confirmed)),
      switchMap((result) => {
        this.updatingId.set(student.id);
        return this.studentService.changeStatus(student.id, status, result.reason);
      }),
      finalize(() => this.updatingId.set(null)),
    ).subscribe({
      next: () => {
        this.notifications.success(`${student.fullName} status updated`);
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
