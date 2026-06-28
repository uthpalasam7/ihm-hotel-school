import { Component } from '@angular/core';

@Component({
  selector: 'app-dashboard-home',
  template: `
    <section class="work-panel" aria-labelledby="workspace-heading">
      <div>
        <p class="eyebrow">Management Console</p>
        <h2 id="workspace-heading">Administration workspace</h2>
        <p>Use the navigation to manage branches and users.</p>
      </div>
    </section>
  `,
  styles: [`
    .work-panel {
      background: #fdfdfd;
      border: 1px solid #e5e5e5;
      border-radius: 0.5rem;
      padding: 1.25rem;
    }

    h2,
    p {
      margin: 0;
    }

    h2 {
      color: #292929;
      font-size: 1.35rem;
      margin-bottom: 0.5rem;
    }
  `],
})
export class DashboardHomeComponent {
}
