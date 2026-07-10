import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    path: 'login',
    renderMode: RenderMode.Prerender
  },
  {
    path: 'registration',
    renderMode: RenderMode.Prerender
  },
  {
    path: 'help-center',
    renderMode: RenderMode.Prerender
  },
  {
    path: 'privacy-policy',
    renderMode: RenderMode.Prerender
  },
  {
    path: 'terms-of-service',
    renderMode: RenderMode.Prerender
  },
  {
    path: '',
    renderMode: RenderMode.Prerender
  },
  // Authenticated/dynamic routes depend on live backend data and auth
  // guards that can't resolve at build time, so render them per-request.
  {
    path: '**',
    renderMode: RenderMode.Server
  }
];
