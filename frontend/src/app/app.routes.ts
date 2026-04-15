import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Home } from './home/home';
import { Registration } from './registration/registration';
import {Dashboard} from './dashboard/dashboard';

export const routes: Routes = [
    {
        path: 'login', component: Login
    },
    {
        path: '', component: Home
    },{
        path: 'registration', component: Registration
    },{
        path: 'dashboard' , component: Dashboard
    }
];
