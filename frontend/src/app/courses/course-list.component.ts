import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../shared/api-error';
import { Course, CourseStatus } from './course.models';
import { CourseService } from './course.service';

@Component({
  selector: 'app-course-list',
  imports: [FormsModule, RouterLink],
  templateUrl: './course-list.component.html',
  styleUrl: './course-list.component.scss',
})
export class CourseListComponent implements OnInit {
  private readonly courseService = inject(CourseService);

  protected readonly courses = signal<Course[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected search = '';
  protected status = '';

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.courseService.list({ search: this.search.trim(), status: this.status }).subscribe({
      next: (page) => {
        this.courses.set(page.content);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected changeStatus(course: Course): void {
    const status: CourseStatus = course.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const reason = window.prompt(`Reason for marking ${course.shortCode} ${status.toLowerCase()}:`);
    if (reason === null) {
      return;
    }
    this.loading.set(true);
    this.courseService.changeStatus(course.id, status, reason).subscribe({
      next: () => {
        this.success.set(`${course.shortCode} status updated`);
        this.load();
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }
}
