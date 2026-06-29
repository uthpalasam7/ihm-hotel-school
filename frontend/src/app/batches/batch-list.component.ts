import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Course } from '../courses/course.models';
import { CourseService } from '../courses/course.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { errorMessage } from '../shared/api-error';
import { Batch, BatchStatus } from './batch.models';
import { BatchService } from './batch.service';

@Component({
  selector: 'app-batch-list',
  imports: [FormsModule, RouterLink],
  templateUrl: './batch-list.component.html',
  styleUrl: './batch-list.component.scss',
})
export class BatchListComponent implements OnInit {
  private readonly batchService = inject(BatchService);
  private readonly courseService = inject(CourseService);
  private readonly activeBranchService = inject(ActiveBranchService);
  private initialized = false;

  protected readonly batches = signal<Batch[]>([]);
  protected readonly courses = signal<Course[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected search = '';
  protected courseId = '';
  protected status = '';
  protected startDateFrom = '';
  protected startDateTo = '';

  constructor() {
    effect(() => {
      this.activeBranchService.activeBranchId();
      if (this.initialized) {
        this.load();
      }
    });
  }

  ngOnInit(): void {
    this.courseService.list({ status: 'ACTIVE', size: 100 }).subscribe({ next: (page) => this.courses.set(page.content) });
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
      startDateFrom: this.startDateFrom,
      startDateTo: this.startDateTo,
    }).subscribe({
      next: (page) => {
        this.batches.set(page.content);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected changeStatus(batch: Batch): void {
    const status: BatchStatus = batch.status === 'CANCELLED' ? 'UPCOMING' : 'CANCELLED';
    const reason = window.prompt(`Reason for marking ${batch.batchNumber} ${status.toLowerCase()}:`);
    if (reason === null) {
      return;
    }
    this.loading.set(true);
    this.batchService.changeStatus(batch.id, status, reason).subscribe({
      next: () => {
        this.success.set(`${batch.batchNumber} status updated`);
        this.load();
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }
}
