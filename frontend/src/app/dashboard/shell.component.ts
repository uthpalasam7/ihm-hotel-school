import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

interface NavigationItem {
  label: string;
  path: string;
  roles: string[];
}

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  protected readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly user = this.authService.currentUser;

  private readonly navigationItems: NavigationItem[] = [
    { label: 'Dashboard', path: '/', roles: ['SUPER_ADMIN', 'ADMIN', 'LECTURER'] },
    { label: 'Branches', path: '/branches', roles: ['SUPER_ADMIN'] },
    { label: 'Users', path: '/users', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Courses', path: '/', roles: ['SUPER_ADMIN', 'ADMIN'] },
    { label: 'Batches', path: '/', roles: ['SUPER_ADMIN', 'ADMIN'] },
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

  protected primaryBranchCode(): string {
    return this.user()?.branches[0]?.code ?? 'No branch';
  }

  protected logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
