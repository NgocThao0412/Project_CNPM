export const config = Object.freeze({
  mode: window.NY_CONFIG?.mode === 'api' ? 'api' : 'demo',
  apiBase: String(window.NY_CONFIG?.apiBase || 'http://localhost:8000/api').replace(/\/+$/, ''),
});

export const LANGUAGES = [
  { code: 'vi', name: 'Tiếng Việt' },
  { code: 'en', name: 'English' },
  { code: 'fr', name: 'Français' },
];

export function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, character => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  })[character]);
}

export function safeMediaUrl(value) {
  if (!value) return '';
  if (String(value).startsWith('blob:')) return String(value);
  if (String(value).startsWith('data:audio/')) return String(value);
  try {
    const url = new URL(String(value), window.location.href);
    return ['http:', 'https:'].includes(url.protocol) ? url.href : '';
  } catch {
    return '';
  }
}

export function distanceMeters(lat1, lon1, lat2, lon2) {
  const rad = x => x * Math.PI / 180;
  const deltaLat = rad(lat2 - lat1);
  const deltaLon = rad(lon2 - lon1);
  const a = Math.sin(deltaLat / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(deltaLon / 2) ** 2;
  return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
