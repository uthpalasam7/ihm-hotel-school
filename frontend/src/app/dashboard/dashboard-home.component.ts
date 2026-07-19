import { Component, computed, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';
import { ActiveBranchService } from '../core/auth/active-branch.service';
import { AuthService } from '../core/auth/auth.service';
import { PageHeaderComponent } from '../shared/page-header.component';
import { PageStateComponent } from '../shared/page-state.component';

interface QuickAction {
  label: string;
  description: string;
  category: string;
  actionLabel: string;
  path: string;
  roles: string[];
}

@Component({
  selector: 'app-dashboard-home',
  imports: [MatCardModule, PageHeaderComponent, PageStateComponent, RouterLink],
  templateUrl: './dashboard-home.component.html',
  styleUrl: './dashboard-home.component.scss',
})
export class DashboardHomeComponent {
  private readonly authService = inject(AuthService);
  protected readonly activeBranchService = inject(ActiveBranchService);

  protected readonly activeBranch = this.activeBranchService.activeBranch;
  protected readonly user = this.authService.currentUser;
  protected readonly quickActions = computed(() => this.actions.filter((action) => this.authService.hasAnyRole(action.roles)));

  private readonly actions: QuickAction[] = [
    {
      label: 'Branches',
      description: 'Manage school locations, contact details, and branch status.',
      category: 'Organization',
      actionLabel: 'View branches',
      path: '/branches',
      roles: ['SUPER_ADMIN'],
    },
    {
      label: 'Users',
      description: 'Manage staff accounts, access, and branch assignments.',
      category: 'Access',
      actionLabel: 'View users',
      path: '/users',
      roles: ['SUPER_ADMIN', 'ADMIN'],
    },
    {
      label: 'Courses',
      description: 'Maintain reusable course definitions and availability.',
      category: 'Academic',
      actionLabel: 'View courses',
      path: '/courses',
      roles: ['SUPER_ADMIN', 'ADMIN'],
    },
    {
      label: 'Batches',
      description: 'Set up and manage course intakes for the active branch.',
      category: 'Intakes',
      actionLabel: 'View batches',
      path: '/batches',
      roles: ['SUPER_ADMIN', 'ADMIN'],
    },
  ];
}
