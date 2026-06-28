import { TestBed } from '@angular/core/testing';
import { ActiveBranchService } from './active-branch.service';

describe('ActiveBranchService', () => {
  let service: ActiveBranchService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(ActiveBranchService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('auto-selects and persists the only authorized branch', () => {
    service.configure([{ id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' }]);

    expect(service.activeBranch()?.code).toBe('IHM-MAIN');
    expect(service.canSwitch()).toBe(false);
    expect(localStorage.getItem('ihm.activeBranchId')).toBe('1');
  });

  it('restores a stored branch only when it is still authorized', () => {
    localStorage.setItem('ihm.activeBranchId', '2');

    service.configure([
      { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
      { id: 2, code: 'IHM-CITY', name: 'IHM City' },
    ]);

    expect(service.activeBranch()?.code).toBe('IHM-CITY');

    service.configure([{ id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' }]);

    expect(service.activeBranch()?.code).toBe('IHM-MAIN');
    expect(localStorage.getItem('ihm.activeBranchId')).toBe('1');
  });

  it('rejects unauthorized branch selection', () => {
    service.configure([{ id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' }]);

    expect(service.selectBranch(999)).toBe(false);
    expect(service.activeBranch()?.id).toBe(1);
  });

  it('switches between authorized branches', () => {
    service.configure([
      { id: 1, code: 'IHM-MAIN', name: 'IHM Hotel School' },
      { id: 2, code: 'IHM-CITY', name: 'IHM City' },
    ]);

    expect(service.selectBranch(2)).toBe(true);

    expect(service.activeBranch()?.code).toBe('IHM-CITY');
    expect(localStorage.getItem('ihm.activeBranchId')).toBe('2');
  });
});
