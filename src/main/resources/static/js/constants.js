const API_BASE_URL = window.location.origin;
const AUTH_HEADER = 'X-Auth-Token';
const AUTH_TOKEN_KEY = 'auth_token';
const AUTH_USER_KEY = 'auth_user';
const HEARTBEAT_INTERVAL = 5 * 60 * 1000;
const SESSION_TIMEOUT = 30 * 60 * 1000;