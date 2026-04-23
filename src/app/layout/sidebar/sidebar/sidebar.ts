import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { SidebarStateService } from '../../../core/services/sidebar-state.service';
import { NAV_ITEMS,BOTTOM_NAV_ITEMS,NavItem } from '../../../config/navigation.config';


@Component({
  selector: 'app-sidebar',
  standalone:true,
  imports: [CommonModule],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.scss'],
})
export class Sidebar implements OnInit {
  // ── Config de navegación ──
  navItems       = NAV_ITEMS;
  bottomNavItems = BOTTOM_NAV_ITEMS;

  constructor(
    public auth:        AuthService,
    public sidebarState: SidebarStateService
  ) {}

  ngOnInit(): void {}

  toggle(): void {
    this.sidebarState.toggle();
  }

}
