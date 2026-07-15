export const APP_CONSTANTS = {
  APP_NAME: 'Smart Travel Management System',
  APP_SHORT_NAME: 'SMTS',
  DEFAULT_PAGE_SIZE: 20,
  MAX_PAGE_SIZE: 100,
  PASSWORD_MIN_LENGTH: 8,
  PASSWORD_MAX_LENGTH: 100,
  USERNAME_MIN_LENGTH: 3,
  USERNAME_MAX_LENGTH: 50,
  STORAGE_KEYS: {
    TOKEN: 'auth_token',
    USER: 'current_user',
    THEME: 'app_theme'
  },
  DATE_FORMAT: 'yyyy-MM-dd',
  DATETIME_FORMAT: 'yyyy-MM-dd HH:mm:ss'
} as const;
