import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../shared/api-error';
import { Branch } from './branch.models';
import { BranchService } from './branch.service';

@Component({
  selector: 'app-branch-list',
  imports: [FormsModule, RouterLink],
  templateUrl: './branch-list.component.html',
  styleUrl: './branch-list.component.scss',
})
export class BranchListComponent implements OnInit {
  private readonly branchService = inject(BranchService);

  protected readonly branches = signal<Branch[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly total = signal(0);
  protected search = '';
  protected status = '';

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.branchService.list({ search: this.search.trim(), status: this.status }).subscribe({
      next: (page) => {
        this.branches.set(page.content);
        this.total.set(page.totalElements);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }

  protected changeStatus(branch: Branch): void {
    const status = branch.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const reason = window.prompt(`Reason for marking ${branch.code} ${status.toLowerCase()}:`);
    if (reason === null) {
      return;
    }
    this.loading.set(true);
    this.branchService.changeStatus(branch.id, status, reason).subscribe({
      next: () => {
        this.success.set(`${branch.code} status updated`);
        this.load();
      },
      error: (error) => {
        this.error.set(errorMessage(error));
        this.loading.set(false);
      },
    });
  }
}
