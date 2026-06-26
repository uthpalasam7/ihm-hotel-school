import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly navigationItems = [
    'Dashboard',
    'Branches',
    'Users',
    'Courses',
    'Batches',
    'Students',
    'Attendance',
    'Finance',
    'Reports',
  ];

  protected readonly statusItems = [
    { label: 'Frontend', value: 'Ready' },
    { label: 'Backend API', value: '/api/v1' },
    { label: 'Timezone', value: 'Asia/Colombo' },
    { label: 'Currency', value: 'LKR' },
  ];
}
