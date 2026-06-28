import { Injectable, computed, signal } from '@angular/core';
import { BranchSummary } from './auth.models';

const ACTIVE_BRANCH_KEY = 'ihm.activeBranchId';

@Injectable({ providedIn: 'root' })
export class ActiveBranchService {
  private readonly branchesSignal = signal<BranchSummary[]>([]);
  private readonly activeBranchSignal = signal<BranchSummary | null>(null);

  readonly branches = this.branchesSignal.asReadonly();
  readonly activeBranch = this.activeBranchSignal.asReadonly();
  readonly activeBranchId = computed(() => this.activeBranchSignal()?.id ?? null);
  readonly canSwitch = computed(() => this.branchesSignal().length > 1);

  configure(branches: BranchSummary[]): void {
    const sortedBranches = [...branches].sort((left, right) => left.code.localeCompare(right.code));
    this.branchesSignal.set(sortedBranches);

    const current = this.activeBranchSignal();
    if (current && sortedBranches.some((branch) => branch.id === current.id)) {
      return;
    }

    const storedBranchId = this.storedBranchId();
    const storedBranch = sortedBranches.find((branch) => branch.id === storedBranchId);
    if (storedBranch) {
      this.setActiveBranch(storedBranch);
      return;
    }

    localStorage.removeItem(ACTIVE_BRANCH_KEY);
    this.setActiveBranch(sortedBranches[0] ?? null);
  }

  selectBranch(branchId: number | string): boolean {
    const numericBranchId = Number(branchId);
    const branch = this.branchesSignal().find((item) => item.id === numericBranchId);
    if (!branch) {
      return false;
    }
    this.setActiveBranch(branch);
    return true;
  }

  clear(): void {
    this.branchesSignal.set([]);
    this.activeBranchSignal.set(null);
    localStorage.removeItem(ACTIVE_BRANCH_KEY);
  }

  private setActiveBranch(branch: BranchSummary | null): void {
    this.activeBranchSignal.set(branch);
    if (branch) {
      localStorage.setItem(ACTIVE_BRANCH_KEY, String(branch.id));
    }
  }

  private storedBranchId(): number | null {
    const value = localStorage.getItem(ACTIVE_BRANCH_KEY);
    if (!value) {
      return null;
    }
    const numericValue = Number(value);
    return Number.isInteger(numericValue) ? numericValue : null;
  }
}
