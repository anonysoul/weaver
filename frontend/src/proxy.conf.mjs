const apiTarget = process.env.WEAVER_API_ENDPOINT
  ? process.env.WEAVER_API_ENDPOINT
  : 'http://localhost:8080';

export default [
  {
    context: ['/api'],
    target: apiTarget,
    secure: false,
    headers: {
      // Keep this in sync with the ng serve port (default 8765).
      'X-Forwarded-Port': '8765',
      'X-Forwarded-Proto': 'http'
    }
  }
];
