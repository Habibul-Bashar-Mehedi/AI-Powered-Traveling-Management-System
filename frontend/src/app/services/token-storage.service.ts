import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * TokenStorageService
 * 
 * Securely manages JWT tokens with XSS prevention strategy:
 * - Access tokens stored in memory only (not persisted) - prevents XSS attacks from stealing long-lived tokens
 * - Refresh tokens stored in sessionStorage (not localStorage) - cleared on tab close, reducing exposure window
 * - Never use localStorage for tokens (persistent XSS risk)
 * 
 * Security Rationale:
 * - Memory-only access tokens mean XSS attacks can only steal tokens valid for 15 minutes
 * - SessionStorage refresh tokens are cleared when browser tab closes
 * - This approach balances security with usability (no re-login on page refresh within same session)
 */
@Injectable({
  providedIn: 'root'
})
export class TokenStorageService {
  
  private accessToken: string | null = null;
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private isBrowser: boolean;
  
  constructor(@Inject(PLATFORM_ID) platformId: Object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }
  
  /**
   * Store tokens (access in memory, refresh in sessionStorage)
   * @param accessToken JWT access token (15 min TTL)
   * @param refreshToken Refresh token (7 day TTL)
   */
  setTokens(accessToken: string, refreshToken: string): void {
    this.accessToken = accessToken;
    if (this.isBrowser) {
      sessionStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
    }
  }
  
  /**
   * Get access token from memory
   * @returns Access token or null if not set
   */
  getAccessToken(): string | null {
    return this.accessToken;
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
    this.accessToken = null;
    if (this.isBrowser) {
      sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
    }
  }
}
