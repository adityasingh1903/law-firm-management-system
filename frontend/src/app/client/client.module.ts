import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { ClientLayoutComponent }    from './layout/client-layout.component';
import { ClientDashboardComponent } from './dashboard/client-dashboard.component';
import { ClientCasesComponent }     from './cases/client-cases.component';
import { ClientHearingsComponent }  from './hearings/client-hearings.component';
import { ClientMessagesComponent }  from './messages/client-messages.component';
import { ClientDocumentsComponent } from './documents/client-documents.component';
import { ClientBillingComponent } from './billing/client-billing.component';
import { ClientProfileComponent } from './profile/client-profile.component';

@NgModule({
  declarations: [
    ClientLayoutComponent,
    ClientDashboardComponent,
    ClientCasesComponent,
    ClientHearingsComponent,
    ClientMessagesComponent,
    ClientDocumentsComponent,
    ClientBillingComponent,
    ClientProfileComponent,
  ],
  imports: [
    CommonModule,
    FormsModule,           // needed for [(ngModel)] in messages compose + filters
    ReactiveFormsModule,   // needed for future forms
    RouterModule.forChild([
      {
        path: '',
        component: ClientLayoutComponent,
        children: [
          { path: '',          redirectTo: 'dashboard', pathMatch: 'full' },
          { path: 'dashboard', component: ClientDashboardComponent },
          { path: 'cases',     component: ClientCasesComponent },
          { path: 'hearings',  component: ClientHearingsComponent },
          { path: 'messages',  component: ClientMessagesComponent },
          { path: 'documents', component: ClientDocumentsComponent },
          { path: 'billing', component: ClientBillingComponent },
          { path: 'profile', component: ClientProfileComponent },
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
    MatSnackBarModule
  ]
})
export class ClientModule { }