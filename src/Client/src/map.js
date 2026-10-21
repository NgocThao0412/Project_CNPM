/* Leaflet khi có kết nối mạng; sơ đồ tương đối dạng SVG khi không tải được Leaflet. */
import { esc } from './config.js';
let activeMap;
let leafletLoading = false;
function ensureLeaflet(root, pois, selectedId, onSelect) {
  if (leafletLoading || window.L || !root) return;
  leafletLoading = true;
  const stylesheet = document.createElement('link');
  stylesheet.rel = 'stylesheet';
  stylesheet.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
  document.head.appendChild(stylesheet);
  const script = document.createElement('script');
  script.async = true;
  script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
  script.onload = () => {
    leafletLoading = false;
    // Nếu người dùng đã chuyển sang trang khác, không dựng bản đồ vào vùng cũ.
    const latestRoot = document.getElementById('campus-map');
    if (latestRoot) showMap(latestRoot, pois, selectedId, onSelect);
  };
  script.onerror = () => { leafletLoading = false; /* Sơ đồ SVG tiếp tục hoạt động. */ };
  document.head.appendChild(script);
}
export function destroyMap() {
  if (activeMap) { activeMap.remove(); activeMap = null; }
}

function fallback(root, pois, selectedId, onSelect) {
  root.classList.add('fallback-map');
  const lats = pois.map(p => Number(p.latitude)).filter(Number.isFinite);
  const lngs = pois.map(p => Number(p.longitude)).filter(Number.isFinite);
  const minLat = Math.min(...lats), maxLat = Math.max(...lats);
  const minLng = Math.min(...lngs), maxLng = Math.max(...lngs);
  const points = pois.map(p => {
    const x = 13 + 74 * (Number(p.longitude) - minLng) / (maxLng - minLng || 0.001);
    const y = 85 - 68 * (Number(p.latitude) - minLat) / (maxLat - minLat || 0.001);
    return `<g class="map-spot ${Number(p.id) === Number(selectedId) ? 'active' : ''}" tabindex="0" role="button" aria-label="Chọn ${esc(p.name)}" data-map-poi="${esc(p.id)}" transform="translate(${x * 8},${y * 4.3})">
      <circle r="17" fill="#ffffff" opacity="0.95"/><circle r="12" fill="${Number(p.id) === Number(selectedId) ? '#ec7c4f' : '#146a60'}"/>
      <text y="5" font-size="14" fill="white" font-weight="800" text-anchor="middle">${esc(p.id)}</text>
    </g>`;
  });
  root.innerHTML = `<div class="map-offline-note">Sơ đồ vị trí tương đối · nền bản đồ thật cần Internet</div>
    <svg class="campus-sketch" viewBox="0 0 800 440" role="img" aria-label="Sơ đồ điểm tham quan">
      <defs><pattern id="map-grid" width="44" height="44" patternUnits="userSpaceOnUse"><path d="M 44 0 L 0 0 0 44" fill="none" stroke="#d9e9e2" stroke-width="1"/></pattern></defs>
      <rect width="800" height="440" fill="#ecf4ec"/><rect width="800" height="440" fill="url(#map-grid)"/>
      <path d="M -10 285 L 810 155 M 280 -20 L 550 460" fill="none" stroke="#ffffff" stroke-width="36"/>
      <path d="M -10 285 L 810 155 M 280 -20 L 550 460" fill="none" stroke="#cbded1" stroke-width="3" stroke-dasharray="9 11"/>
      <rect x="76" y="67" width="135" height="91" rx="8" fill="#c7d5e0"/><rect x="561" y="280" width="144" height="85" rx="8" fill="#c8dac4"/>
      <rect x="315" y="289" width="120" height="95" rx="8" fill="#d9dcc7"/><rect x="528" y="46" width="120" height="74" rx="8" fill="#c7d5e0"/>
      <text x="76" y="56" font-size="14" fill="#5f7874">KHUÔN VIÊN ĐẠI HỌC SÀI GÒN</text>
      ${points.join('')}
    </svg>`;
  root.querySelectorAll('[data-map-poi]').forEach(el => {
    const activate = () => onSelect(Number(el.dataset.mapPoi));
    el.addEventListener('click', activate);
    el.addEventListener('keydown', event => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); activate(); } });
  });
}

export function showMap(root, pois, selectedId, onSelect) {
  destroyMap();
  if (!root) return;
  if (!pois.length) { root.innerHTML = '<div class="map-empty">Chưa có POI được phép hiển thị.</div>'; return; }
  if (!window.L) { fallback(root, pois, selectedId, onSelect); ensureLeaflet(root, pois, selectedId, onSelect); return; }
  root.classList.remove('fallback-map');
  root.innerHTML = '';
  try {
    const average = arr => arr.reduce((a, b) => a + b, 0) / arr.length;
    activeMap = window.L.map(root, { scrollWheelZoom: false, zoomControl: true });
    activeMap.setView([average(pois.map(p => Number(p.latitude))), average(pois.map(p => Number(p.longitude)))], 17);
    window.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors', maxZoom: 19,
    }).addTo(activeMap);
    const bounds = [];
    for (const p of pois) {
      const lat = Number(p.latitude), lng = Number(p.longitude);
      if (!Number.isFinite(lat) || !Number.isFinite(lng)) continue;
      bounds.push([lat, lng]);
      const selected = Number(p.id) === Number(selectedId);
      const marker = window.L.circleMarker([lat, lng], {
        radius: selected ? 12 : 9, weight: 3, color: '#fff', fillColor: selected ? '#e7774a' : '#156a60', fillOpacity: 1,
      }).addTo(activeMap);
      marker.bindTooltip(esc(p.name));
      marker.on('click', () => onSelect(Number(p.id)));
      if (selected) window.L.circle([lat, lng], { radius: Number(p.radius_m) || 40, color: '#e7774a', fillOpacity: .06, weight: 1 }).addTo(activeMap);
    }
    if (bounds.length > 1) activeMap.fitBounds(bounds, { padding: [60, 60], maxZoom: 18 });
    requestAnimationFrame(() => activeMap?.invalidateSize());
  } catch (error) {
    console.warn('Leaflet unavailable, showing relative campus map:', error);
    destroyMap(); fallback(root, pois, selectedId, onSelect);
  }
}
