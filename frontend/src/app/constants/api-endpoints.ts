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
  }
} as const;
