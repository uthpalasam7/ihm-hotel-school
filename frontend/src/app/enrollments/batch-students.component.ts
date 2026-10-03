import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
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
  imports: [DatePipe, MatButtonModule, MatCardModule, MatPaginatorModule, MatTableModule, RouterLink,
    PageHeaderComponent, PageStateComponent, StatusChipComponent],
  templateUrl: './batch-students.component.html',
  styleUrl: './enrollments.scss',
})
export class BatchStudentsComponent implements OnInit {
  protected readonly displayedColumns = ['registrationNumber', 'student', 'enrollmentDate', 'status'];
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
