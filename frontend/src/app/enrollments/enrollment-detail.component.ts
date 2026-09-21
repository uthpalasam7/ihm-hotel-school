import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
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
  imports: [DatePipe, DecimalPipe, MatButtonModule, MatCardModule, MatPaginatorModule, RouterLink,
    PageHeaderComponent, PageStateComponent, StatusChipComponent],
  template: `
    <section class="enrollment-page">
      <app-page-header eyebrow="Enrollment" [title]="enrollment()?.registrationNumber || 'Enrollment details'" subtitle="Registration and charges saved at enrollment.">
        <a mat-stroked-button routerLink="/enrollments">Back to enrollments</a>
      </app-page-header>
      <app-page-state [loading]="loading()" [error]="error()" (retry)="load()" />
      @if (!loading() && !error() && enrollment(); as row) {
        <mat-card appearance="outlined" class="enrollment-summary"><mat-card-content>
          <div class="section-heading"><h2>{{ row.studentName }}</h2><app-status-chip [value]="row.status" /></div>
          <dl><div><dt>Student</dt><dd><a [routerLink]="['/students', row.studentId]">View student profile</a> · <a [routerLink]="['/students', row.studentId, 'card']">Student card</a></dd></div>
            <div><dt>Course batch</dt><dd>{{ row.batchNumber }} — {{ row.courseName }}</dd></div>
            <div><dt>Branch</dt><dd>{{ row.branchName }}</dd></div><div><dt>Enrollment date</dt><dd>{{ row.enrollmentDate | date:'d MMM y' }}</dd></div>
            <div><dt>Remarks</dt><dd>{{ row.remarks || 'No remarks' }}</dd></div>
          </dl>
          @if (row.status === 'ACTIVE' || row.status === 'SUSPENDED') {
            <div class="actions">
              @if (row.status === 'ACTIVE') {
                <button mat-stroked-button type="button" [disabled]="changing()" (click)="changeStatus('SUSPENDED')">Suspend</button>
                <button mat-stroked-button type="button" [disabled]="changing()" (click)="changeStatus('COMPLETED')">Complete</button>
              } @else { <button mat-stroked-button type="button" [disabled]="changing()" (click)="changeStatus('ACTIVE')">Resume</button> }
              <button mat-stroked-button type="button" [disabled]="changing()" (click)="changeStatus('WITHDRAWN')">Withdraw</button>
              <button mat-button type="button" [disabled]="changing()" (click)="changeStatus('CANCELLED')">Cancel enrollment</button>
            </div>
            <p class="muted">Changing enrollment status does not cancel or waive outstanding charges or automatically cancel the student card.</p>
          }
          @if (statusError()) { <p role="alert" class="error-message">{{ statusError() }}</p> }
        </mat-card-content></mat-card>
        <mat-card appearance="outlined" class="ihm-data-grid charge-grid">
          <mat-card-content class="charge-intro">
          <h2>Student charges</h2><p class="muted">These amounts and due dates were saved at enrollment. Later changes to the batch fee plan do not change them.</p>
          <app-page-state [loading]="chargesLoading()" [error]="chargesError()" [empty]="!chargesLoading() && !chargesError() && !charges().length" emptyTitle="No charges found" (retry)="loadCharges(chargePage())" />
          </mat-card-content>
          @if (!chargesLoading() && !chargesError() && charges().length) {
            <div class="ihm-desktop-table table-wrap"><table><caption class="visually-hidden">Student charges</caption>
              <thead><tr><th>Charge</th><th>Due date</th><th class="amount">Payable amount</th><th>Status</th></tr></thead>
              <tbody>@for (charge of charges(); track charge.id) { <tr><td>{{ charge.description }}</td>
                <td>{{ charge.dueDate | date:'d MMM y' }}</td><td class="amount">{{ charge.currencyCode }} {{ charge.finalPayableAmount | number:'1.2-2' }}</td>
                <td><app-status-chip [value]="charge.status" /></td></tr> }</tbody>
            </table></div>
            <div class="ihm-mobile-cards" aria-label="Student charges">
              @for (charge of charges(); track charge.id) {
                <article class="ihm-mobile-card">
                  <div class="ihm-mobile-card__heading">
                    <div><h2>{{ charge.description }}</h2></div>
                    <app-status-chip [value]="charge.status" />
                  </div>
                  <dl class="ihm-mobile-card__details">
                    <div><dt>Due date</dt><dd>{{ charge.dueDate | date:'d MMM y' }}</dd></div>
                    <div><dt>Payable amount</dt><dd>{{ charge.currencyCode }} {{ charge.finalPayableAmount | number:'1.2-2' }}</dd></div>
                  </dl>
                </article>
              }
            </div>
            <mat-paginator [length]="chargeTotal()" [pageIndex]="chargePage()" [pageSize]="chargePageSize()"
              [pageSizeOptions]="[10, 20, 50, 100]" [showFirstLastButtons]="true"
              (page)="changeChargePage($event)" aria-label="Charge pages" />
          }
        </mat-card>
      }
    </section>
  `,
  styleUrl: './enrollments.scss',
})
export class EnrollmentDetailComponent implements OnInit {
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
