import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * TokenStorageService
 * 
 * Manages JWT tokens using sessionStorage for persistence across page refreshes:
 * - Access tokens stored in sessionStorage so login survives page refresh
 * - Refresh tokens stored in sessionStorage (not localStorage) - cleared on tab close
 * - Never use localStorage for tokens (persistent XSS risk)
 * 
 * Security Rationale:
 * - sessionStorage is scoped to the current tab and cleared on tab close
 * - This prevents persistent XSS from accessing tokens after the tab is closed
 * - In-memory-only access tokens cause logout on every refresh, which is worse UX
 */
@Injectable({
  providedIn: 'root'
})
export class TokenStorageService {
  
  private readonly ACCESS_TOKEN_KEY = 'access_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private isBrowser: boolean;
  
  constructor(@Inject(PLATFORM_ID) platformId: Object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }
  
  /**
   * Store tokens in sessionStorage
   * @param accessToken JWT access token
   * @param refreshToken Refresh token
   */
  setTokens(accessToken: string, refreshToken: string): void {
    if (this.isBrowser) {
      sessionStorage.setItem(this.ACCESS_TOKEN_KEY, accessToken);
      sessionStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
    }
  }
  
  /**
   * Get access token from sessionStorage
   * @returns Access token or null if not set
   */
  getAccessToken(): string | null {
    if (this.isBrowser) {
      return sessionStorage.getItem(this.ACCESS_TOKEN_KEY);
    }
    return null;
  }
  
  /**
   * Get refresh token from sessionStorage
   * @returns Refresh token or null if not set
   */
  getRefreshToken(): string | null {
    if (this.isBrowser) {
      return sessionStorage.getItem(this.REFRESH_TOKEN_KEY);
    }
    return null;
  }
  
  /**
   * Clear all tokens (logout)
   */
  clearTokens(): void {
    if (this.isBrowser) {
      sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
      sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
    }
  }
}
