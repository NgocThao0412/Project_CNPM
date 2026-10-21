/* Adapter API: chỉnh endpoint / ánh xạ field ở đây theo backend thực tế của nhóm. */
import { config } from './config.js';
import { mock } from './mock.js';

const AUTH_KEY = 'ny-client-auth';
export const auth = {
  get() { try { return JSON.parse(sessionStorage.getItem(AUTH_KEY)); } catch { return null; } },
  set(value) { if (value) sessionStorage.setItem(AUTH_KEY, JSON.stringify(value)); else sessionStorage.removeItem(AUTH_KEY); },
};

function unpack(result) {
  const value = result?.data ?? result;
  return value?.items ?? value;
}
async function request(path, options = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 12000);
  const token = auth.get()?.token;
  const headers = { ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }), ...options.headers };
  if (token) headers.Authorization = `Bearer ${token}`;
  try {
    const response = await fetch(`${config.apiBase}${path}`, { ...options, headers, signal: controller.signal });
    const raw = await response.text();
    let data;
    try { data = raw ? JSON.parse(raw) : null; } catch { data = raw; }
    if (!response.ok) {
      if (response.status === 401) auth.set(null);
      throw new Error(data?.message || data?.error || `API lỗi HTTP ${response.status}`);
    }
    return unpack(data);
  } catch (error) {
    if (error.name === 'AbortError') throw new Error('API phản hồi quá chậm. Vui lòng thử lại.');
    if (error instanceof TypeError) throw new Error('Không kết nối được backend. Kiểm tra API URL, server và CORS.');
    throw error;
  } finally { clearTimeout(timeout); }
}
const json = (method, body) => ({ method, body: JSON.stringify(body) });
const isDemo = config.mode === 'demo';
export const api = {
  async login(email, password) {
    const value = isDemo ? await mock.login(email, password) : await request('/auth/login', json('POST', { email, password }));
    if (!value?.token) throw new Error('API đăng nhập không trả về token.');
    auth.set(value); return value;
  },
  async logout() {
    try { if (isDemo) await mock.logout(); else await request('/auth/logout', { method: 'POST' }); }
    finally { auth.set(null); }
  },
  pois(publicOnly = false) { return isDemo ? mock.pois(publicOnly) : request(publicOnly ? '/public/pois' : '/pois'); },
  narrations(publicOnly = false) { return isDemo ? mock.narrations(publicOnly) : request(publicOnly ? '/public/narrations' : '/narrations'); },
  languages() { return isDemo ? mock.languages() : request('/languages'); },
  routes() { return isDemo ? mock.routes() : request('/public/routes'); },
  savePoi(item) { return isDemo ? mock.savePoi(item) : request(item.id ? `/pois/${encodeURIComponent(item.id)}` : '/pois', json(item.id ? 'PUT' : 'POST', item)); },
  togglePoi(id, visible) { return isDemo ? mock.togglePoi(id, visible) : request(`/pois/${encodeURIComponent(id)}/visibility`, json('PATCH', { is_visible: visible })); },
  saveNarration(item) { return isDemo ? mock.saveNarration(item) : request(item.id ? `/narrations/${encodeURIComponent(item.id)}` : '/narrations', json(item.id ? 'PUT' : 'POST', item)); },
  toggleNarration(id, visible) { return isDemo ? mock.toggleNarration(id, visible) : request(`/narrations/${encodeURIComponent(id)}/visibility`, json('PATCH', { is_visible: visible })); },
  uploadAudio(id, file) {
    if (isDemo) return mock.uploadAudio(id, file);
    const body = new FormData(); body.append('audio', file);
    return request(`/narrations/${encodeURIComponent(id)}/audio`, { method: 'POST', body });
  },
  createSession() { return isDemo ? mock.createSession() : request('/visitor-sessions', json('POST', {})); },
  logListen(payload) { return isDemo ? mock.logListen(payload) : request('/listening-logs', json('POST', payload)); },
  resetDemo() { if (isDemo) mock.reset(); },
};
