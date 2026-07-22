import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { StudentService } from './student.service';

@Component({
  selector: 'app-student-photo',
  templateUrl: './student-photo.component.html',
  styleUrl: './student-photo.component.scss',
})
export class StudentPhotoComponent {
  private readonly studentService = inject(StudentService);

  readonly studentId = input.required<number>();
  readonly fullName = input.required<string>();
  readonly photoAvailable = input(false);
  readonly version = input(0);
  readonly variant = input<'full' | 'thumbnail'>('thumbnail');
  readonly size = input<'small' | 'large'>('small');

  protected readonly source = signal<string | null>(null);
  protected readonly initials = computed(() => {
    const parts = this.fullName().trim().split(/\s+/).filter(Boolean);
    return parts.slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'S';
  });

  constructor() {
    effect((onCleanup) => {
      const id = this.studentId();
      const available = this.photoAvailable();
      const variant = this.variant();
      this.version();
      let objectUrl: string | null = null;
      this.source.set(null);
      if (!available) {
        return;
      }
      const subscription = this.studentService.loadPhoto(id, variant).subscribe({
        next: (blob) => {
          objectUrl = URL.createObjectURL(blob);
          this.source.set(objectUrl);
        },
        error: () => this.source.set(null),
      });
      onCleanup(() => {
        subscription.unsubscribe();
        if (objectUrl) {
          URL.revokeObjectURL(objectUrl);
        }
      });
    });
  }
}
