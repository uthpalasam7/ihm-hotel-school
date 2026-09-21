import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { errorMessage } from '../shared/api-error';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';
import { StatusChipComponent } from '../shared/status-chip.component';
import { BatchStudent } from './enrollment.models';
import { EnrollmentService } from './enrollment.service';

@Component({
  selector: 'app-batch-students',
  imports: [DatePipe, MatButtonModule, MatCardModule, MatPaginatorModule, RouterLink,
    PageHeaderComponent, PageStateComponent, StatusChipComponent],
  template: `
    <section class="enrollment-page">
      <app-page-header eyebrow="Academics" title="Batch students" subtitle="Students enrolled in this batch.">
        <a mat-stroked-button routerLink="/batches">Back to batches</a>
      </app-page-header>
      <app-page-state [loading]="loading()" [error]="error()"
        [empty]="!loading() && !error() && !rows().length"
        emptyTitle="No students enrolled" (retry)="load(page())" />
      @if (!loading() && !error() && rows().length) {
        <mat-card appearance="outlined" class="ihm-data-grid"><div class="ihm-desktop-table table-wrap"><table>
          <caption class="visually-hidden">Batch students</caption>
          <thead><tr><th>Registration number</th><th>Student</th><th>Enrolled</th><th>Status</th></tr></thead>
          <tbody>@for (row of rows(); track row.registrationNumber) {
            <tr><td>{{ row.registrationNumber }}</td><td>{{ row.studentName }}</td>
              <td>{{ row.enrollmentDate | date:'d MMM y' }}</td>
              <td><app-status-chip [value]="row.status" /></td></tr>
          }</tbody>
        </table></div>
        <div class="ihm-mobile-cards" aria-label="Batch students">
          @for (row of rows(); track row.registrationNumber) {
            <article class="ihm-mobile-card">
              <div class="ihm-mobile-card__heading">
                <div><h2>{{ row.studentName }}</h2><p>{{ row.registrationNumber }}</p></div>
                <app-status-chip [value]="row.status" />
              </div>
              <dl class="ihm-mobile-card__details">
                <div><dt>Enrolled</dt><dd>{{ row.enrollmentDate | date:'d MMM y' }}</dd></div>
              </dl>
            </article>
          }
        </div>
        <mat-paginator [length]="total()" [pageIndex]="page()" [pageSize]="pageSize()"
          [pageSizeOptions]="[10, 20, 50, 100]" [showFirstLastButtons]="true"
          (page)="changePage($event)" aria-label="Batch student pages" />
        </mat-card>
      }
    </section>
  `,
  styleUrl: './enrollments.scss',
})
export class BatchStudentsComponent implements OnInit {
  private readonly api=inject(EnrollmentService);
  private readonly route=inject(ActivatedRoute);
  private readonly destroyRef=inject(DestroyRef);
  private request?: Subscription;
  private batchId=0;
  protected readonly rows=signal<BatchStudent[]>([]);
  protected readonly loading=signal(false);
  protected readonly error=signal<string | null>(null);
  protected readonly page=signal(0);
  protected readonly pageSize=signal(20);
  protected readonly total=signal(0);
  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      this.batchId=Number(params.get('id')); this.load(0);
    });
  }
  protected load(page=0): void {
    this.request?.unsubscribe(); this.loading.set(true); this.error.set(null); this.page.set(page);
    this.request=this.api.batchStudents(this.batchId,page,this.pageSize()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => { this.rows.set(result.content); this.total.set(result.totalElements); this.loading.set(false); },
      error: error => { this.error.set(errorMessage(error)); this.loading.set(false); },
    });
  }
  protected changePage(event: PageEvent): void {
    this.pageSize.set(event.pageSize);
    this.load(event.pageIndex);
  }
}
