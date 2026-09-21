import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { EnrollmentService } from '../enrollments/enrollment.service';
import { Student } from '../students/student.models';
import { StudentPhotoComponent } from '../students/student-photo.component';
import { StudentService } from '../students/student.service';
import { errorMessage } from '../shared/api-error';
import { ConfirmationDialogComponent, ConfirmationDialogResult } from '../shared/confirmation-dialog.component';
import { NotificationService } from '../shared/notification.service';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { CardDelivery, StudentCard, StudentCardEvent, StudentCardService } from './student-card.service';

@Component({
  selector: 'app-student-card',
  imports: [DatePipe, MatButtonModule, MatCardModule, RouterLink, PageHeaderComponent, PageStateComponent, StudentPhotoComponent],
  templateUrl: './student-card.component.html',
  styleUrl: './student-card.component.scss',
})
export class StudentCardComponent implements OnInit {
  private readonly studentApi = inject(StudentService);
  private readonly enrollmentApi = inject(EnrollmentService);
  private readonly cards = inject(StudentCardService);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly studentId = Number(this.route.snapshot.paramMap.get('id'));
  protected readonly student = signal<Student | null>(null);
  protected readonly card = signal<StudentCard | null>(null);
  protected readonly events = signal<StudentCardEvent[]>([]);
  protected readonly deliveries = signal<CardDelivery[]>([]);
  protected readonly emailAvailable = signal(false);
  private pendingEmailKey: string | null = null;
  protected readonly enrollmentCount = signal(0);
  protected readonly qrUrl = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  private objectUrl: string | null = null;

  ngOnInit(): void {
    this.load();
    this.cards.emailAvailability().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => this.emailAvailable.set(result.available),
      error: () => this.emailAvailable.set(false),
    });
    this.destroyRef.onDestroy(() => this.clearQr());
  }
  protected load(): void {
    this.loading.set(true); this.error.set(null);
    this.studentApi.get(this.studentId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: student => {
        this.student.set(student);
        this.enrollmentApi.list({ studentId: this.studentId, page: 0, size: 1 })
          .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
            next: page => { this.enrollmentCount.set(page.totalElements); this.loadCard(); },
            error: error => { this.loading.set(false); this.error.set(errorMessage(error)); },
          });
      },
      error: error => { this.loading.set(false); this.error.set(errorMessage(error)); },
    });
  }
  private loadCard(): void {
    this.cards.get(this.studentId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: card => { this.card.set(card); this.loading.set(false); this.refreshCardResources(); },
      error: error => { this.loading.set(false); this.error.set(errorMessage(error)); },
    });
  }
  private refreshCardResources(): void {
    this.clearQr();
    if (!this.card()) { this.events.set([]); this.deliveries.set([]); return; }
    this.refreshDeliveries();
    this.cards.history(this.studentId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: page => this.events.set(page.content),
      error: error => this.error.set(errorMessage(error)),
    });
    if (this.card()?.status === 'ACTIVE') {
      this.cards.qr(this.studentId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: blob => { this.clearQr(); this.objectUrl=URL.createObjectURL(blob); this.qrUrl.set(this.objectUrl); },
        error: error => this.error.set(errorMessage(error)),
      });
    }
  }
  protected refreshDeliveries(): void {
    this.cards.deliveries(this.studentId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: page => this.deliveries.set(page.content),
      error: error => this.error.set(errorMessage(error)),
    });
  }
  protected email(): void {
    if (this.busy() || !this.emailAvailable() || !this.student()?.email || this.card()?.status !== 'ACTIVE') return;
    const destination=this.student()!.email!;
    this.dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(ConfirmationDialogComponent, {
      data: { title: 'Email student card?', message: `Send the current card PDF to the saved student email: ${destination}. Check this address before sending.`, confirmLabel: 'Queue email' },
      width: 'calc(100vw - 2rem)', maxWidth: '34rem',
    }).afterClosed().pipe(
      filter((result): result is ConfirmationDialogResult => Boolean(result?.confirmed)),
      switchMap(() => {
        this.busy.set(true); this.error.set(null);
        this.pendingEmailKey ||= crypto.randomUUID();
        return this.cards.email(this.studentId,this.pendingEmailKey);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: () => { this.busy.set(false); this.pendingEmailKey=null; this.refreshDeliveries(); this.notifications.success('Card email queued'); },
      error: error => { this.busy.set(false); this.error.set(errorMessage(error)); },
    });
  }
  protected issue(): void {
    if (this.busy() || !this.enrollmentCount()) return;
    this.busy.set(true); this.error.set(null);
    this.cards.issue(this.studentId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: card => { this.card.set(card); this.busy.set(false); this.refreshCardResources(); this.notifications.success('Student card issued'); },
      error: error => { this.busy.set(false); this.error.set(errorMessage(error)); },
    });
  }
  protected change(action: 'replace' | 'revoke'): void {
    if (this.busy()) return;
    const replacing=action==='replace';
    this.dialog.open<ConfirmationDialogComponent, unknown, ConfirmationDialogResult>(ConfirmationDialogComponent, {
      data: { title: replacing?'Replace student card?':'Cancel student card?',
        message: 'The current QR code will stop working immediately. This does not change enrollment or attendance history.',
        confirmLabel: replacing?'Replace card':'Cancel card', reasonLabel: 'Reason', reasonRequired: true },
      width: 'calc(100vw - 2rem)', maxWidth: '34rem',
    }).afterClosed().pipe(
      filter((result): result is ConfirmationDialogResult => Boolean(result?.confirmed && result.reason.trim())),
      switchMap(result => {
        this.busy.set(true); this.error.set(null);
        return replacing ? this.cards.replace(this.studentId,result.reason) : this.cards.revoke(this.studentId,result.reason);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: card => { this.card.set(card); this.pendingEmailKey=null; this.busy.set(false); this.refreshCardResources(); this.notifications.success(replacing?'Card replaced':'Card cancelled'); },
      error: error => { this.busy.set(false); this.error.set(errorMessage(error)); },
    });
  }
  protected download(): void {
    if (this.busy()) return;
    this.busy.set(true); this.error.set(null);
    this.cards.pdf(this.studentId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: blob => {
        const url=URL.createObjectURL(blob);
        const link=document.createElement('a'); link.href=url;
        link.download=`ihm-student-card-${this.card()?.identifier || this.studentId}.pdf`;
        link.click(); setTimeout(()=>URL.revokeObjectURL(url),60_000);
        this.busy.set(false);
      },
      error: error => { this.busy.set(false); this.error.set(errorMessage(error)); },
    });
  }
  protected print(): void {
    if (this.busy()) return;
    const tab=window.open('', '_blank');
    if (!tab) { this.error.set('Allow a new tab to open for printing, or download the PDF.'); return; }
    this.busy.set(true); this.error.set(null);
    this.cards.pdf(this.studentId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: blob => { const url=URL.createObjectURL(blob); tab.location.href=url;
        setTimeout(()=>URL.revokeObjectURL(url),120_000); this.busy.set(false); },
      error: error => { tab.close(); this.busy.set(false); this.error.set(errorMessage(error)); },
    });
  }
  private clearQr(): void {
    if (this.objectUrl) URL.revokeObjectURL(this.objectUrl);
    this.objectUrl=null; this.qrUrl.set(null);
  }
}
