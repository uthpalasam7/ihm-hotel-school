import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { BranchService } from '../branches/branch.service';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { AuthService } from '../core/auth/auth.service';

interface NavigationItem {
  label: string;
  path: string;
  roles: string[];
}

@Component({
  selector: 'app-shell',
  imports: [FormsModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent implements OnInit {
  protected readonly authService = inject(AuthService);
  protected readonly activeBranchService = inject(ActiveBranchService);
  private readonly branchService = inject(BranchService);
  private readonly router = inject(Router);

  protected readonly user = this.authService.currentUser;
  protected readonly activeBranch = this.activeBranchService.activeBranch;
  protected readonly availableBranches = this.activeBranchService.branches;
  protected readonly canSwitchBranch = this.activeBranchService.canSwitch;

  private readonly navigationItems: NavigationItem[] = [
    { label: 'Dashboard', path: '/', roles: ['SUPER_ADMIN', 'ADMIN', 'LECTURER'] },
    { label: 'Branches', path: '/branches', roles: ['SUPER_ADMIN'] },
    { label: 'Users', path: '/users', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Courses', path: '/courses', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Batches', path: '/batches', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Students', path: '/', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Enrollments', path: '/', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Sessions', path: '/', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Attendance', path: '/', roles: ['SUPER_ADMIN', 'ADMIN', 'LECTURER'] },
    { label: 'Reports', path: '/', roles: ['SUPER_ADMIN', 'ADMIN', 'LECTURER'] },
    { label: 'Audit', path: '/', roles: ['SUPER_ADMIN'] },
    { label: 'Settings', path: '/', roles: ['SUPER_ADMIN', 'ADMIN'] },
  ];
  protected visibleNavigation(): NavigationItem[] {
    return this.navigationItems.filter((item) => this.authService.hasAnyRole(item.roles));
  }

  ngOnInit(): void {
    this.configureBranches();
  }

  protected branchLabel(): string {
    const branch = this.activeBranch();
    return branch ? `${branch.code} - ${branch.name}` : 'No active branch';
  }

  protected changeBranch(branchId: string): void {
    if (!this.activeBranchService.selectBranch(branchId)) {
      return;
    }
    this.router.navigateByUrl(this.router.url);
  }

  protected logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }

  private configureBranches(): void {
    const user = this.user();
    if (!user) {
      this.activeBranchService.clear();
      return;
    }
    if (!this.authService.hasAnyRole(['SUPER_ADMIN'])) {
      this.activeBranchService.configure(user.branches);
      return;
    }
    this.branchService.list({ status: 'ACTIVE', size: 100 }).subscribe({
      next: (page) => this.activeBranchService.configure(page.content),
      error: () => this.activeBranchService.configure(user.branches),
    });
  }
}
