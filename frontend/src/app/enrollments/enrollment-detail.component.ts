import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, filter, switchMap } from 'rxjs';
import { errorMessage } from '../shared/api-error';
import { PageHeaderComponent } from '../shared/page-header.component';
import { ConfirmationDialogComponent, ConfirmationDialogResult } from '../shared/confirmation-dialog.component';
import { NotificationService } from '../shared/notification.service';
import { PageStateComponent } from '../shared/page-state.component';
import { StatusChipComponent } from '../shared/status-chip.component';
import { Enrollment, EnrollmentStatus, StudentCharge } from './enrollment.models';
import { EnrollmentService } from './enrollment.service';

@Component({
  selector: 'app-enrollment-detail',
  imports: [DatePipe, DecimalPipe, MatButtonModule, MatCardModule, MatPaginatorModule, MatTableModule, RouterLink,
    PageHeaderComponent, PageStateComponent, StatusChipComponent],
  templateUrl: './enrollment-detail.component.html',
  styleUrl: './enrollments.scss',
})
export class EnrollmentDetailComponent implements OnInit {
  protected readonly displayedColumns = ['description', 'dueDate', 'amount', 'status'];
  private readonly api = inject(EnrollmentService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);
  private request?: Subscription;
  private chargeRequest?: Subscription;
  private id = 0;
  protected readonly enrollment = signal<Enrollment | null>(null);
  protected readonly loading = signal(false);
  protected readonly changing = signal(false);
  protected readonly statusError = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly charges = signal<StudentCharge[]>([]);
  protected readonly chargesLoading = signal(false);
  protected readonly chargesError = signal<string | null>(null);
  protected readonly chargePage = signal(0);
  protected readonly chargePageSize = signal(20);
  protected readonly chargeTotal = signal(0);
  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => { this.id=Number(params.get('id')); this.load(); });
  }
  protected load(): void {
    this.request?.unsubscribe(); this.chargeRequest?.unsubscribe(); this.loading.set(true); this.error.set(null);
    this.request = this.api.get(this.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: row => { this.enrollment.set(row); this.loading.set(false); this.loadCharges(0); },
      error: error => { this.error.set(errorMessage(error)); this.loading.set(false); },
    });
  }
  protected changeStatus(next: EnrollmentStatus): void {
    const current=this.enrollment();
    if (!current || this.changing()) return;
    this.dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(ConfirmationDialogComponent, {
      data: { title: `${next.charAt(0)+next.slice(1).toLowerCase()} enrollment?`,
        message: 'The enrollment status will change. Existing charges and the student card remain unchanged.',
        confirmLabel: 'Update status', reasonLabel: 'Reason', reasonRequired: true },
      width: 'calc(100vw - 2rem)', maxWidth: '34rem',
    }).afterClosed().pipe(
      filter((result): result is ConfirmationDialogResult => Boolean(result?.confirmed && result.reason.trim())),
      switchMap(result => {
        this.changing.set(true); this.statusError.set(null);
        return this.api.changeStatus(current.id,next,result.reason,current.version);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: updated => { this.enrollment.set(updated); this.changing.set(false); this.notifications.success('Enrollment status updated'); },
      error: error => { this.statusError.set(errorMessage(error)); this.changing.set(false); },
    });
  }
  protected loadCharges(page = 0): void {
    this.chargeRequest?.unsubscribe(); this.chargesLoading.set(true); this.chargesError.set(null); this.chargePage.set(page);
    this.chargeRequest = this.api.charges(this.id, page, this.chargePageSize()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => { this.charges.set(result.content); this.chargeTotal.set(result.totalElements); this.chargesLoading.set(false); },
      error: error => { this.chargesError.set(errorMessage(error)); this.chargesLoading.set(false); },
    });
  }
  protected changeChargePage(event: PageEvent): void {
    this.chargePageSize.set(event.pageSize);
    this.loadCharges(event.pageIndex);
  }
}
