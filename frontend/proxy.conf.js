const target = process.env['BACKEND_URL'] || 'http://localhost:8080';

module.exports = {
  '/api': {
    target,
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
  '/uploads': {
    target,
    secure: false,
    changeOrigin: true,
    logLevel: 'debug',
  },
};
