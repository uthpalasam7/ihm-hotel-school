import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
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
  imports: [
    MatButtonModule,
    MatMenuModule,
    MatSidenavModule,
    MatToolbarModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent implements OnInit {
  protected readonly authService = inject(AuthService);
  protected readonly activeBranchService = inject(ActiveBranchService);
  private readonly branchService = inject(BranchService);
  private readonly router = inject(Router);
  private readonly breakpointObserver = inject(BreakpointObserver);

  protected readonly user = this.authService.currentUser;
  protected readonly activeBranch = this.activeBranchService.activeBranch;
  protected readonly availableBranches = this.activeBranchService.branches;
  protected readonly canSwitchBranch = this.activeBranchService.canSwitch;
  protected readonly branchesLoading = signal(false);
  protected readonly isHandset = toSignal(
    this.breakpointObserver.observe('(max-width: 900px)').pipe(map((state) => state.matches)),
    { initialValue: false },
  );
  protected readonly userInitials = computed(() => {
    const name = this.user()?.fullName.trim();
    if (!name) {
      return 'U';
    }
    return name.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase();
  });

  private readonly navigationItems: NavigationItem[] = [
    { label: 'Dashboard', path: '/', roles: ['SUPER_ADMIN', 'ADMIN', 'LECTURER'] },
    { label: 'Branches', path: '/branches', roles: ['SUPER_ADMIN'] },
    { label: 'Users', path: '/users', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Courses', path: '/courses', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Batches', path: '/batches', roles: ['SUPER_ADMIN', 'ADMIN', 'LECTURER'] },
    { label: 'Class sessions', path: '/sessions', roles: ['SUPER_ADMIN', 'ADMIN', 'LECTURER'] },
    { label: 'Students', path: '/students', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Enrollments', path: '/enrollments', roles: ['SUPER_ADMIN', 'ADMIN'] },
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

  protected changeBranch(branchId: number | string): void {
    if (!this.activeBranchService.selectBranch(branchId)) {
      return;
    }
  }

  protected closeNavigation(drawer: MatSidenav): void {
    if (this.isHandset()) {
      drawer.close();
    }
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
    this.branchesLoading.set(true);
    this.branchService.listAllActive().subscribe({
      next: (branches) => {
        this.activeBranchService.configure(branches);
        this.branchesLoading.set(false);
      },
      error: () => {
        this.activeBranchService.configure(user.branches);
        this.branchesLoading.set(false);
      },
    });
  }
}
