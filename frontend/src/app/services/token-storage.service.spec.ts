import { TestBed } from '@angular/core/testing';
import { TokenStorageService } from './token-storage.service';

describe('TokenStorageService', () => {
  let service: TokenStorageService;
  
  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TokenStorageService);
    
    // Clear sessionStorage before each test
    sessionStorage.clear();
  });
  
  afterEach(() => {
    // Clean up after each test
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('setTokens', () => {
    it('should store access token in memory', () => {
      const accessToken = 'test-access-token';
      const refreshToken = 'test-refresh-token';
      
      service.setTokens(accessToken, refreshToken);
      
      expect(service.getAccessToken()).toBe(accessToken);
    });

    it('should store refresh token in sessionStorage', () => {
      const accessToken = 'test-access-token';
      const refreshToken = 'test-refresh-token';
      
      service.setTokens(accessToken, refreshToken);
      
      expect(sessionStorage.getItem('refresh_token')).toBe(refreshToken);
    });

    it('should not persist access token to sessionStorage', () => {
      const accessToken = 'test-access-token';
      const refreshToken = 'test-refresh-token';
      
      service.setTokens(accessToken, refreshToken);
      
      // Verify access token is NOT in sessionStorage
      expect(sessionStorage.getItem('access_token')).toBeNull();
      expect(sessionStorage.getItem('token')).toBeNull();
      
      // Only refresh token should be in sessionStorage
      const keys = Object.keys(sessionStorage);
      expect(keys).toEqual(['refresh_token']);
    });
  });

  describe('getAccessToken', () => {
    it('should return null when no token is set', () => {
      expect(service.getAccessToken()).toBeNull();
    });

    it('should return access token from memory', () => {
      const accessToken = 'test-access-token';
      const refreshToken = 'test-refresh-token';
      
      service.setTokens(accessToken, refreshToken);
      
      expect(service.getAccessToken()).toBe(accessToken);
    });
  });

  describe('getRefreshToken', () => {
    it('should return null when no token is set', () => {
      expect(service.getRefreshToken()).toBeNull();
    });

    it('should return refresh token from sessionStorage', () => {
      const accessToken = 'test-access-token';
      const refreshToken = 'test-refresh-token';
      
      service.setTokens(accessToken, refreshToken);
      
      expect(service.getRefreshToken()).toBe(refreshToken);
    });
  });

  describe('clearTokens', () => {
    it('should remove access token from memory', () => {
      const accessToken = 'test-access-token';
      const refreshToken = 'test-refresh-token';
      
      service.setTokens(accessToken, refreshToken);
      service.clearTokens();
      
      expect(service.getAccessToken()).toBeNull();
    });

    it('should remove refresh token from sessionStorage', () => {
      const accessToken = 'test-access-token';
      const refreshToken = 'test-refresh-token';
      
      service.setTokens(accessToken, refreshToken);
      service.clearTokens();
      
      expect(service.getRefreshToken()).toBeNull();
      expect(sessionStorage.getItem('refresh_token')).toBeNull();
    });

    it('should remove all tokens', () => {
      const accessToken = 'test-access-token';
      const refreshToken = 'test-refresh-token';
      
      service.setTokens(accessToken, refreshToken);
      service.clearTokens();
      
      expect(service.getAccessToken()).toBeNull();
      expect(service.getRefreshToken()).toBeNull();
      expect(sessionStorage.length).toBe(0);
    });
  });
});
