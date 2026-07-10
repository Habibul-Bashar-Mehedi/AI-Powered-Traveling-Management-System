export const API_ENDPOINTS = {
  AUTH: {
    REGISTER: '/auth/register',
    LOGIN: '/auth/login',
    REFRESH: '/auth/refresh',
    LOGOUT: '/auth/logout',
    LOGOUT_ALL: '/auth/logout-all',
    ME: '/auth/me',
    // Legacy endpoints (deprecated)
    LEGACY_REGISTER: '/auth/user/register',
    LEGACY_LOGIN: '/auth/user/login',
    GET_ALL_USERS: '/auth/user',
    UPDATE_USER: (id: number) => `/auth/user/${id}`,
    DELETE_USER: (id: number) => `/auth/user/${id}`
  },
  BOOKING: {
    CREATE: '/booking/add',
    GET_ALL: '/booking',
    UPDATE: (id: number) => `/booking/${id}`,
    DELETE: (id: number) => `/booking/${id}`
  },
  HOTEL: {
    CREATE: '/hotels/add',
    GET_ALL: '/hotels',
    GET_BY_ID: (id: number) => `/hotels/${id}`,
    UPDATE: (id: number) => `/hotels/${id}`,
    DELETE: (id: number) => `/hotels/${id}`
  },
  ROOM: {
    CREATE: '/rooms/add',
    GET_ALL: '/rooms',
    UPDATE: (id: number) => `/rooms/${id}`,
    DELETE: (id: number) => `/rooms/${id}`
  },
  DESTINATION: {
    CREATE: '/destination/add',
    GET_ALL: '/destination',
    UPDATE: (id: number) => `/destination/${id}`,
    DELETE: (id: number) => `/destination/${id}`
  },
  VENDOR: {
    REGISTER: '/v1/vendor/register',
    PROFILE: '/v1/vendor/profile',
    SERVICES: '/v1/vendor/services',
    SERVICE_BY_ID: (id: string) => `/v1/vendor/services/${id}`,
    SERVICE_STATUS: (id: string) => `/v1/vendor/services/${id}/status`,
    SERVICE_IMAGES: '/v1/vendor/services/images',
    BOOKINGS: '/v1/vendor/bookings',
    BOOKING_BY_ID: (id: string) => `/v1/vendor/bookings/${id}`,
    BOOKING_CONFIRM: (id: string) => `/v1/vendor/bookings/${id}/confirm`,
    BOOKING_REJECT: (id: string) => `/v1/vendor/bookings/${id}/reject`,
    BOOKING_CANCEL: (id: string) => `/v1/vendor/bookings/${id}/cancel`,
    WALLET: '/v1/vendor/wallet',
    WALLET_TRANSACTIONS: '/v1/vendor/wallet/transactions',
    WALLET_PAYOUT: '/v1/vendor/wallet/payout',
    ANALYTICS_SUMMARY: '/v1/vendor/analytics/summary',
  },
  USER: {
    SERVICE_REQUESTS: '/v1/user/service-requests',
    MY_BOOKINGS: '/v1/user/service-requests',
    MY_BOOKINGS_STATUS_SUMMARY: '/v1/user/service-requests/status-summary',
    MY_BOOKING_CANCEL: (id: string) => `/v1/user/service-requests/${id}/cancel`,
  },
  SERVICE_CATALOG: {
    LIST: '/v1/services',
    BOOK: (id: string) => `/v1/services/${id}/book`,
    AVAILABILITY: (id: string) => `/v1/services/${id}/availability`,
  },
  ADMIN_VENDOR: {
    ALL: '/v1/admin/vendors',
    PENDING: '/v1/admin/vendors/pending',
    APPROVE: (id: string) => `/v1/admin/vendors/${id}/approve`,
    REJECT: (id: string) => `/v1/admin/vendors/${id}/reject`,
    SUSPEND: (id: string) => `/v1/admin/vendors/${id}/suspend`,
    REINSTATE: (id: string) => `/v1/admin/vendors/${id}/reinstate`,
    PENDING_PAYOUTS: '/v1/admin/vendors/payouts/pending',
    PROCESS_PAYOUT: (id: string) => `/v1/admin/vendors/payouts/${id}/process`,
  },
  ADMIN_MANAGEMENT: {
    USERS: '/v1/admin/management/users',
    USER_BY_ID: (id: string) => `/v1/admin/management/users/${id}`,
  },
  ADMIN_BOOKING: {
    ALL: '/v1/admin/bookings',
    STATUS_SUMMARY: '/v1/admin/bookings/status-summary',
  },
  BANNER: {
    ACTIVE: '/v1/banners/active',
  },
  ADMIN_BANNER: {
    ALL: '/v1/admin/banners',
    BY_ID: (id: string) => `/v1/admin/banners/${id}`,
    ACTIVE_TOGGLE: (id: string) => `/v1/admin/banners/${id}/active`,
    IMAGES: '/v1/admin/banners/images',
  },
  AI: {
    CHAT: '/ai/chat',
  }
} as const;
