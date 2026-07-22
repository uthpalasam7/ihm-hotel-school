import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, RouterLink } from '@angular/router';
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
  selector: 'app-student-profile',
  imports: [DatePipe, MatButtonModule, MatCardModule, PageHeaderComponent, PageStateComponent, RouterLink, StatusChipComponent, StudentPhotoComponent],
  templateUrl: './student-profile.component.html',
  styleUrl: './student-profile.component.scss',
})
export class StudentProfileComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly studentService = inject(StudentService);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);

  protected readonly student = signal<Student | null>(null);
  protected readonly loading = signal(false);
  protected readonly updating = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly studentId = Number(this.route.snapshot.paramMap.get('id'));

  ngOnInit(): void { this.load(); }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.studentService.get(this.studentId).subscribe({
      next: (student) => { this.student.set(student); this.loading.set(false); },
      error: (error) => { this.error.set(errorMessage(error)); this.loading.set(false); },
    });
  }

  protected selectPhoto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024) {
      this.notifications.error('Choose a JPEG or PNG image up to 5 MiB.');
      return;
    }
    this.updating.set(true);
    this.studentService.uploadPhoto(this.studentId, file).pipe(finalize(() => this.updating.set(false))).subscribe({
      next: (student) => { this.student.set(student); this.notifications.success('Student photo updated'); },
      error: (error) => this.notifications.error(errorMessage(error)),
    });
  }

  protected deletePhoto(): void {
    const student = this.student();
    if (!student?.photoAvailable) return;
    this.dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(ConfirmationDialogComponent, {
      data: { title: 'Remove student photo?', message: 'This removes only the current photo. The student record remains unchanged.', confirmLabel: 'Remove photo' },
      maxWidth: '34rem', width: 'calc(100vw - 2rem)',
    }).afterClosed().pipe(
      filter((result): result is ConfirmationDialogResult => Boolean(result?.confirmed)),
      switchMap(() => { this.updating.set(true); return this.studentService.deletePhoto(this.studentId); }),
      finalize(() => this.updating.set(false)),
    ).subscribe({
      next: () => { this.student.set({ ...student, photoAvailable: false, photoUrl: null, photoThumbnailUrl: null }); this.notifications.success('Student photo removed'); },
      error: (error) => this.notifications.error(errorMessage(error)),
    });
  }

  protected changeStatus(): void {
    const student = this.student();
    if (!student) return;
    const status: StudentStatus = student.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(ConfirmationDialogComponent, {
      data: { title: `${status === 'ACTIVE' ? 'Activate' : 'Deactivate'} ${student.fullName}?`, message: 'The student record and history will be preserved.', confirmLabel: status === 'ACTIVE' ? 'Activate student' : 'Deactivate student', reasonLabel: 'Reason (optional)' },
      maxWidth: '34rem', width: 'calc(100vw - 2rem)',
    }).afterClosed().pipe(
      filter((result): result is ConfirmationDialogResult => Boolean(result?.confirmed)),
      switchMap((result) => { this.updating.set(true); return this.studentService.changeStatus(student.id, status, result.reason); }),
      finalize(() => this.updating.set(false)),
    ).subscribe({
      next: (updated) => { this.student.set(updated); this.notifications.success('Student status updated'); },
      error: (error) => this.notifications.error(errorMessage(error)),
    });
  }
}
