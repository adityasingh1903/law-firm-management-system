// src/app/lawyer/lawyer.module.ts

import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { MatCardModule }            from '@angular/material/card';
import { MatButtonModule }          from '@angular/material/button';
import { MatIconModule }            from '@angular/material/icon';
import { MatToolbarModule }         from '@angular/material/toolbar';
import { MatTableModule }           from '@angular/material/table';
import { MatPaginatorModule }       from '@angular/material/paginator';
import { MatSortModule }            from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule }        from '@angular/material/snack-bar';
import { MatChipsModule }           from '@angular/material/chips';
import { MatBadgeModule }           from '@angular/material/badge';
import { MatTooltipModule }         from '@angular/material/tooltip';
import { MatMenuModule }            from '@angular/material/menu';
import { MatDialogModule }          from '@angular/material/dialog';
import { MatSelectModule }          from '@angular/material/select';
import { MatInputModule }           from '@angular/material/input';
import { MatFormFieldModule }       from '@angular/material/form-field';
import { MatDividerModule }         from '@angular/material/divider';
import { MatTabsModule }            from '@angular/material/tabs';

import { LawyerLayoutComponent }    from './layout/lawyer-layout.component';
import { LawyerDashboardComponent } from './dashboard/lawyer-dashboard.component';
import { LawyerCasesComponent }     from './cases/lawyer-cases.component';
import { LawyerHearingsComponent }  from './hearings/lawyer-hearings.component';
import { LawyerDocumentsComponent } from './documents/lawyer-documents.component';
import { LawyerMessagesComponent }  from './messages/lawyer-messages.component';
import { LawyerClientsComponent }   from './clients/lawyer-clients.component';

// Uncomment as you build each section:
import { LawyerBillingComponent }   from './billing/lawyer-billing.component';
// import { LawyerProfileComponent }   from './profile/lawyer-profile.component';

@NgModule({
  declarations: [
    LawyerLayoutComponent,
    LawyerDashboardComponent,
    LawyerCasesComponent,
    LawyerHearingsComponent,
    LawyerDocumentsComponent,
    LawyerMessagesComponent,
    LawyerClientsComponent,

    // Uncomment as you build each section:
    LawyerBillingComponent,
    // LawyerProfileComponent,
  ],

  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,

    RouterModule.forChild([
      {
        path: '',
        component: LawyerLayoutComponent,
        children: [
          { path: '',          redirectTo: 'dashboard', pathMatch: 'full' },
          { path: 'dashboard', component: LawyerDashboardComponent },
          { path: 'cases',     component: LawyerCasesComponent },
          { path: 'hearings',  component: LawyerHearingsComponent },
          { path: 'documents', component: LawyerDocumentsComponent },
          { path: 'messages',  component: LawyerMessagesComponent },
          { path: 'clients',   component: LawyerClientsComponent },

          { path: 'billing',   component: LawyerBillingComponent },
          // { path: 'profile',   component: LawyerProfileComponent },
        ]
      }
    ]),

    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatChipsModule,
    MatBadgeModule,
    MatTooltipModule,
    MatMenuModule,
    MatDialogModule,
    MatSelectModule,
    MatInputModule,
    MatFormFieldModule,
    MatDividerModule,
    MatTabsModule,
  ]
})
export class LawyerModule { }