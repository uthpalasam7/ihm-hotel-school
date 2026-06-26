import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

interface NavigationItem {
  label: string;
  roles: string[];
}

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly user = this.authService.currentUser;

  private readonly navigationItems: NavigationItem[] = [
    { label: 'Dashboard', roles: ['SUPER_ADMIN', 'ADMIN', 'LECTURER'] },
    { label: 'Branches', roles: ['SUPER_ADMIN'] },
    { label: 'Users', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Courses', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Batches', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Students', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Enrollments', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Sessions', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Attendance', roles: ['SUPER_ADMIN', 'ADMIN', 'LECTURER'] },
    { label: 'Reports', roles: ['SUPER_ADMIN', 'ADMIN', 'LECTURER'] },
    { label: 'Audit', roles: ['SUPER_ADMIN'] },
    { label: 'Settings', roles: ['SUPER_ADMIN', 'ADMIN'] },
  ];
  protected visibleNavigation(): NavigationItem[] {
    return this.navigationItems.filter((item) => this.authService.hasAnyRole(item.roles));
  }

  protected primaryBranchCode(): string {
    return this.user()?.branches[0]?.code ?? 'No branch';
  }

  protected logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
