export const VALIDATION_MESSAGES = {
  REQUIRED: (field: string) => `${field} is required`,
  EMAIL: 'Please enter a valid email address',
  MIN_LENGTH: (field: string, length: number) => `${field} must be at least ${length} characters`,
  MAX_LENGTH: (field: string, length: number) => `${field} cannot exceed ${length} characters`,
  PASSWORD_MISMATCH: 'Passwords do not match',
  PASSWORD_PATTERN: 'Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character',
  INVALID_DATE_RANGE: 'Check-out date must be after check-in date',
  MIN_VALUE: (field: string, value: number) => `${field} must be at least ${value}`,
  MAX_VALUE: (field: string, value: number) => `${field} cannot exceed ${value}`
} as const;
