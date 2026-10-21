import { api, auth } from './api.js';
import { config, esc, safeMediaUrl, distanceMeters } from './config.js';
import { destroyMap, showMap } from './map.js';

const app = document.getElementById('app');
const toastNode = document.getElementById('toast');
const state = {
  account: auth.get(), pois: [], narrations: [], languages: [], routes: [],
  selectedPoi: null, language: 'vi', locationSource: 'simulation',
  visitSessionId: null, filter: '', busy: false,
};
let toastTimer;

function notify(message, error = false) {
  toastNode.textContent = message;
  toastNode.className = `toast${error ? ' error' : ''}`;
  toastNode.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toastNode.hidden = true; }, 4200);
}
function view() {
  const hash = window.location.hash.replace(/^#\/?/, '');
  if (hash.startsWith('admin/pois')) return 'pois';
  if (hash.startsWith('admin/contents')) return 'contents';
  if (hash.startsWith('admin')) return 'admin';
  return 'visitor';
}
function langName(code) { return state.languages.find(l => l.code === code)?.name || code; }
function poiName(id) { return state.pois.find(p => Number(p.id) === Number(id))?.name || `POI #${id}`; }
function isTruthy(v) { return v === true || v === 1 || v === '1'; }
function allPois() { return state.pois; }
function visiblePois() { return state.pois.filter(p => isTruthy(p.is_visible)); }
function visibleNarrations() { return state.narrations.filter(n => isTruthy(n.is_visible)); }
function currentPoi() {
  const pois = visiblePois();
  return pois.find(p => Number(p.id) === Number(state.selectedPoi)) || pois[0] || null;
}
function currentNarration(poi) {
  return visibleNarrations().find(n => Number(n.poi_id) === Number(poi?.id) && n.language_code === state.language);
}
function canAdmin() { return Boolean(state.account?.token); }
function badge(visible) { return isTruthy(visible) ? '<span class="tag">● Hiển thị</span>' : '<span class="tag tag-off">● Đang ẩn</span>'; }
function logo() {
  return `<a class="logo" href="#/visitor" aria-label="SGU Audio Guide - Trang chủ"><span class="logo-symbol">SGU</span><span><span class="logo-title">SGU Audio Guide</span><span class="logo-sub">Thuyết minh đa ngôn ngữ · Nhóm 12</span></span></a>`;
}
function header() {
  const route = view();
  return `<header class="site-header"><div class="header-wrap">${logo()}
    <nav class="header-nav" aria-label="Điều hướng chính">
      <a class="nav-link ${route === 'visitor' ? 'active' : ''}" href="#/visitor">Khách tham quan</a>
      <a class="nav-link ${route !== 'visitor' ? 'active' : ''}" href="${canAdmin() ? '#/admin/pois' : '#/admin/login'}">Quản trị viên</a>
    </nav><div class="header-right">${config.mode === 'demo' ? '<span class="mode-pill">Dữ liệu demo</span>' : '<span class="mode-pill">Kết nối API</span>'}
    ${canAdmin() ? `<span class="user-name">${esc(state.account.user?.name || 'Admin')}</span><button type="button" class="btn btn-outline btn-small" data-action="logout">Đăng xuất</button>` : ''}</div>
  </div></header>`;
}
function footer() {
  return `<footer><span>© 2026 · SGU Audio Guide · Phần Client do Ny phụ trách</span><span>${config.mode === 'demo' ? 'Dữ liệu và tọa độ trên màn hình chỉ nhằm minh họa.' : 'Dữ liệu do backend cung cấp.'}</span></footer>`;
}
function visitorHtml() {
  const pois = visiblePois();
  const poi = currentPoi();
  const narration = poi ? currentNarration(poi) : null;
  const audioUrl = narration ? safeMediaUrl(narration.audio_url) : '';
  const route = state.routes[0];
  const availableLang = (state.languages.length ? state.languages : [{ code: 'vi', name: 'Tiếng Việt' }]);
  const langOptions = availableLang.map(l => `<option value="${esc(l.code)}" ${l.code === state.language ? 'selected' : ''}>${esc(l.name)}</option>`).join('');
  return `<div class="container">
    <section class="page-title"><div><div class="eyebrow">Khám phá khuôn viên Đại học Sài Gòn</div><h1>Khám phá SGU theo cách của bạn</h1><p>Chọn điểm tham quan trên bản đồ, chọn ngôn ngữ và nghe thuyết minh. Không cần tạo tài khoản.</p></div>
      <button class="btn btn-outline" type="button" data-action="gps">◎ Sử dụng vị trí GPS</button></section>
    <div class="visitor-layout"><div class="stack">
      <section class="card"><div class="panel-head map-toolbar"><div><h2>Bản đồ tham quan</h2><span class="small muted">${pois.length} địa điểm đang hiển thị</span></div><label class="field" style="display:flex;align-items:center;gap:8px">Ngôn ngữ <select class="select" id="visitor-language" aria-label="Chọn ngôn ngữ">${langOptions}</select></label></div>
        <div class="map-area" id="campus-map" aria-label="Bản đồ điểm tham quan"></div></section>
      <section class="card"><div class="panel-head"><h2>Danh sách địa điểm</h2><input class="search" id="visitor-search" type="search" placeholder="Tìm địa điểm…" aria-label="Tìm điểm tham quan" style="max-width:240px" /></div>
        <div class="poi-list" id="visitor-poi-list">${pois.length ? pois.map(p => `<div class="poi-card ${Number(poi?.id) === Number(p.id) ? 'active' : ''}" data-card data-search-text="${esc((p.name + ' ' + p.area + ' ' + p.code).toLowerCase())}" data-action="select-poi" data-id="${esc(p.id)}" role="button" tabindex="0" aria-label="Chọn ${esc(p.name)}"><span class="poi-icon">⌖</span><div><h3>${esc(p.name)}</h3><p>${esc(p.area || 'Khuôn viên SGU')} · ${esc(p.code)}</p></div></div>`).join('') : '<div class="empty-state">Chưa có POI được phép hiển thị.</div>'}</div></section>
    </div><div class="visitor-side">
      <section class="card"><div class="panel-body">
      <div class="feature-image">${poi && safeMediaUrl(poi.image_url) ? `<img alt="Ảnh ${esc(poi.name)}" src="${esc(safeMediaUrl(poi.image_url))}" />` : '🎓'}</div>
      <div class="eyebrow">Địa điểm đang chọn · ${esc(state.locationSource === 'gps' ? 'GPS' : 'Mô phỏng')}</div>
      <h2>${esc(poi?.name || 'Chưa chọn địa điểm')}</h2>
      <p class="description">${esc(poi?.description || 'Chọn một địa điểm trên bản đồ để xem nội dung.')}</p>
      ${poi ? `<div class="small muted">Mã: ${esc(poi.code)} · Bán kính kích hoạt: ${esc(poi.radius_m)} m</div>` : ''}
      </div></section>
      <section class="card"><div class="panel-head"><h2>Thuyết minh</h2><span class="tag">${esc(langName(state.language))}</span></div><div class="panel-body">
      ${narration ? `<h3>${esc(narration.title)}</h3><div class="narration-text">${esc(narration.body)}</div>
        ${audioUrl ? `<audio id="visitor-audio" class="audio-player" controls preload="none" src="${esc(audioUrl)}">Trình duyệt không hỗ trợ audio.</audio><p class="small muted">Nhấn phát để nghe bản ghi thuyết minh.</p>` : `<p class="notice">Chưa có file audio cho POI và ngôn ngữ này. Bạn vẫn có thể đọc nội dung hoặc sử dụng giọng đọc của trình duyệt để thử giao diện.</p>`}
        <div class="row-actions"><button class="btn btn-soft btn-small" type="button" data-action="tts">▶ Đọc thử bằng trình duyệt (TTS)</button><button class="btn btn-outline btn-small" type="button" data-action="stop-tts">■ Dừng đọc</button></div>` : `<p class="notice">Chưa có nội dung thuyết minh bằng ${esc(langName(state.language))} cho địa điểm này. Hãy chọn ngôn ngữ khác.</p>`}
      </div></section>
      <section class="card route-panel"><div class="panel-head"><h2>Lộ trình gợi ý</h2><span class="small muted">${route ? 'Tham quan tự do' : 'Chưa có lộ trình'}</span></div><div class="panel-body">
        ${route ? `<p class="small muted">${esc(route.description || route.name)}</p><ol class="route-steps">${(route.poi_ids || []).filter(id => pois.some(p => Number(p.id) === Number(id))).map((id, i) => `<li><span class="step-dot">${i + 1}</span><a href="#/visitor" data-action="select-poi" data-id="${esc(id)}">${esc(poiName(id))}</a></li>`).join('')}</ol>` : '<p class="small muted">Hiện chưa có dữ liệu lộ trình.</p>'}
      </div></section>
    </div></div></div>`;
}
function loginHtml() {
  return `<div class="container"><section class="card login-wrap"><div class="login-art"><span class="tag">HỆ THỐNG QUẢN TRỊ</span><h1>Quản lý nội dung thuyết minh thật dễ dàng.</h1><p>Đăng nhập để quản lý điểm tham quan, cấu hình GPS và nội dung giới thiệu đa ngôn ngữ.</p><div class="login-circle"></div></div>
    <div class="login-pane"><div class="eyebrow">Dành cho quản trị viên</div><h2>Chào mừng trở lại</h2><p class="muted small">Nhập thông tin tài khoản được cấp để tiếp tục.</p>
    <form id="login-form" class="stack"><label class="field">Email đăng nhập<input name="email" type="email" autocomplete="username" required placeholder="admin@sgu.edu.vn" /></label>
    <label class="field">Mật khẩu<input name="password" type="password" autocomplete="current-password" required placeholder="Nhập mật khẩu" /></label>
    <button type="submit" class="btn btn-block">Đăng nhập →</button></form>
    ${config.mode === 'demo' ? '<div class="notice">Tài khoản demo: <strong>admin@sgu.edu.vn</strong> · Mật khẩu: <strong>123456</strong>. Chỉ dùng thử giao diện; không phải bảo mật thật.</div>' : '<div class="notice">Đăng nhập sẽ gửi tài khoản/mật khẩu đến backend qua API. Nên dùng HTTPS khi triển khai.</div>'}
    </div></section></div>`;
}
function sidebar() {
  return `<aside class="card sidebar" aria-label="Quản trị"><div class="sidebar-title">Công cụ quản lý</div>
    <a class="${view() === 'pois' ? 'active' : ''}" href="#/admin/pois"><span class="icon">⌖</span> Quản lý POI</a>
    <a class="${view() === 'contents' ? 'active' : ''}" href="#/admin/contents"><span class="icon">♫</span> Quản lý nội dung</a>
    <a href="#/visitor"><span class="icon">↗</span> Xem trang khách</a></aside>`;
}
function stats() {
  return `<div class="stat-grid"><article class="card stat-card"><p>Tổng số POI</p><div class="stat-value">${state.pois.length}</div></article>
    <article class="card stat-card"><p>POI đang hiển thị</p><div class="stat-value">${state.pois.filter(p => isTruthy(p.is_visible)).length}</div></article>
    <article class="card stat-card"><p>Nội dung thuyết minh</p><div class="stat-value">${state.narrations.length}</div></article></div>`;
}
function poiTable() {
  return `<section class="card"><div class="panel-head"><h2>Danh sách POI</h2><div class="table-tools"><input type="search" class="search" data-table-filter placeholder="Tìm theo mã, tên, khu vực…" aria-label="Tìm POI" /><button type="button" class="btn btn-small" data-action="new-poi">+ Thêm POI</button></div></div>
    <div class="table-wrap"><table class="data-table"><thead><tr><th>Mã POI / Địa điểm</th><th>Khu vực</th><th>Tọa độ GPS</th><th>Bán kính</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
    ${state.pois.length ? state.pois.map(p => `<tr data-row data-search-text="${esc((p.code + ' ' + p.name + ' ' + p.area).toLowerCase())}">
    <td><strong>${esc(p.name)}</strong><div class="sub">${esc(p.code)}</div></td><td>${esc(p.area || '—')}</td>
    <td>${esc(Number(p.latitude).toFixed(5))}, ${esc(Number(p.longitude).toFixed(5))}</td><td>${esc(p.radius_m)} m</td><td>${badge(p.is_visible)}</td>
    <td><div class="row-actions"><button type="button" class="btn btn-outline btn-small" data-action="edit-poi" data-id="${esc(p.id)}">Sửa</button><button type="button" class="btn btn-soft btn-small" data-action="toggle-poi" data-id="${esc(p.id)}">${isTruthy(p.is_visible) ? 'Ẩn' : 'Hiện'}</button></div></td></tr>`).join('') : '<tr><td colspan="6"><div class="empty-state">Chưa có POI nào. Hãy tạo điểm tham quan đầu tiên.</div></td></tr>'}
    </tbody></table></div></section>`;
}
function contentTable() {
  return `<section class="card"><div class="panel-head"><h2>Nội dung đa ngôn ngữ</h2><div class="table-tools"><input type="search" class="search" data-table-filter placeholder="Tìm nội dung, POI, ngôn ngữ…" aria-label="Tìm nội dung" /><button type="button" class="btn btn-small" data-action="new-narration">+ Thêm nội dung</button></div></div>
    <div class="table-wrap"><table class="data-table"><thead><tr><th>Tiêu đề</th><th>Điểm tham quan</th><th>Ngôn ngữ</th><th>Audio</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
    ${state.narrations.length ? state.narrations.map(n => `<tr data-row data-search-text="${esc((n.title + ' ' + poiName(n.poi_id) + ' ' + langName(n.language_code)).toLowerCase())}">
    <td><strong>${esc(n.title)}</strong><div class="sub">${esc(String(n.body || '').slice(0, 50))}${String(n.body || '').length > 50 ? '…' : ''}</div></td>
    <td>${esc(poiName(n.poi_id))}</td><td>${esc(langName(n.language_code))}</td>
    <td>${safeMediaUrl(n.audio_url) ? `<audio controls preload="none" style="max-width:140px;height:36px" src="${esc(safeMediaUrl(n.audio_url))}" aria-label="Nghe thử ${esc(n.title)}"></audio>` : '<span class="small muted">Chưa có file</span>'}<div><label class="btn btn-outline btn-small" style="margin-top:6px">${n.audio_url ? 'Thay file' : 'Tải audio'}<input type="file" accept="audio/*,.mp3,.wav,.m4a,.ogg" data-audio-id="${esc(n.id)}" hidden /></label></div></td>
    <td>${badge(n.is_visible)}</td>
    <td><div class="row-actions"><button type="button" class="btn btn-outline btn-small" data-action="edit-narration" data-id="${esc(n.id)}">Sửa</button><button type="button" class="btn btn-soft btn-small" data-action="toggle-narration" data-id="${esc(n.id)}">${isTruthy(n.is_visible) ? 'Ẩn' : 'Hiện'}</button></div></td></tr>`).join('') : '<tr><td colspan="6"><div class="empty-state">Chưa có nội dung thuyết minh nào.</div></td></tr>'}
    </tbody></table></div></section>`;
}
function adminHtml() {
  const section = view() === 'contents' ? 'contents' : 'pois';
  return `<div class="container"><div class="admin-layout">${sidebar()}<main><div class="page-title"><div><div class="eyebrow">Bảng điều khiển quản trị</div><h1>${section === 'pois' ? 'Quản lý điểm tham quan' : 'Quản lý nội dung thuyết minh'}</h1>
    <p>${section === 'pois' ? 'Thêm, chỉnh sửa, ẩn/hiện POI và thiết lập tọa độ GPS, bán kính kích hoạt.' : 'Viết nội dung theo từng địa điểm và ngôn ngữ, tải lên và nghe thử file âm thanh.'}</p></div></div>
    ${stats()}${section === 'pois' ? poiTable() : contentTable()}</main></div></div>`;
}
function poiDialogHtml(item = null) {
  const p = item || { code: '', name: '', area: '', description: '', latitude: 10.7601, longitude: 106.6822, radius_m: 50, image_url: '', is_visible: true };
  return `<dialog class="dialog" id="data-dialog"><div class="dialog-head"><h2>${item ? 'Chỉnh sửa POI' : 'Thêm điểm tham quan'}</h2><button type="button" class="btn btn-outline btn-small" data-action="close-dialog">✕</button></div><div class="dialog-content">
    <form id="poi-form"><input type="hidden" name="id" value="${esc(item?.id || '')}" /><div class="form-grid">
    <label class="field">Mã POI *<input name="code" required maxlength="40" value="${esc(p.code)}" placeholder="SGU-005" /></label>
    <label class="field">Tên địa điểm *<input name="name" required maxlength="200" value="${esc(p.name)}" /></label>
    <label class="field wide">Khu vực <input name="area" value="${esc(p.area)}" placeholder="Khu học tập" /></label>
    <label class="field">Vĩ độ (latitude) *<input name="latitude" type="number" required step="any" min="-90" max="90" value="${esc(p.latitude)}" /></label>
    <label class="field">Kinh độ (longitude) *<input name="longitude" type="number" required step="any" min="-180" max="180" value="${esc(p.longitude)}" /></label>
    <label class="field">Bán kính kích hoạt (m) *<input name="radius_m" type="number" required min="1" max="10000" value="${esc(p.radius_m)}" /></label>
    <label class="field">Trạng thái <select name="is_visible"><option value="true" ${isTruthy(p.is_visible) ? 'selected' : ''}>Hiển thị</option><option value="false" ${!isTruthy(p.is_visible) ? 'selected' : ''}>Ẩn</option></select></label>
    <label class="field wide">URL hình ảnh <input name="image_url" type="url" value="${esc(p.image_url)}" placeholder="https://example.com/image.jpg" /></label>
    <label class="field wide">Mô tả <textarea name="description" maxlength="5000">${esc(p.description)}</textarea></label>
    </div><div class="dialog-actions"><button type="button" class="btn btn-outline" data-action="close-dialog">Hủy</button><button type="submit" class="btn">Lưu POI</button></div></form>
    </div></dialog>`;
}
function narrationDialogHtml(item = null) {
  const n = item || { poi_id: state.pois[0]?.id || '', language_code: state.languages[0]?.code || 'vi', title: '', body: '', is_visible: true };
  return `<dialog class="dialog" id="data-dialog"><div class="dialog-head"><h2>${item ? 'Sửa thuyết minh' : 'Thêm thuyết minh'}</h2><button type="button" class="btn btn-outline btn-small" data-action="close-dialog">✕</button></div><div class="dialog-content">
    <form id="narration-form"><input type="hidden" name="id" value="${esc(item?.id || '')}" /><div class="form-grid">
    <label class="field">Điểm tham quan *<select name="poi_id" required>${state.pois.map(p => `<option value="${esc(p.id)}" ${Number(p.id) === Number(n.poi_id) ? 'selected' : ''}>${esc(p.name)}</option>`).join('')}</select></label>
    <label class="field">Ngôn ngữ *<select name="language_code" required>${state.languages.map(l => `<option value="${esc(l.code)}" ${l.code === n.language_code ? 'selected' : ''}>${esc(l.name)}</option>`).join('')}</select></label>
    <label class="field wide">Tiêu đề *<input name="title" maxlength="255" value="${esc(n.title)}" required /></label>
    <label class="field wide">Nội dung thuyết minh *<textarea name="body" minlength="5" rows="6" required>${esc(n.body)}</textarea></label>
    <label class="field wide">Trạng thái<select name="is_visible"><option value="true" ${isTruthy(n.is_visible) ? 'selected' : ''}>Hiển thị</option><option value="false" ${!isTruthy(n.is_visible) ? 'selected' : ''}>Ẩn</option></select></label>
    </div><div class="notice">Mỗi POI chỉ có một nội dung cho mỗi ngôn ngữ. Sau khi lưu, bạn có thể tải MP3 tại danh sách nội dung.</div>
    <div class="dialog-actions"><button type="button" class="btn btn-outline" data-action="close-dialog">Hủy</button><button type="submit" class="btn">Lưu nội dung</button></div></form>
    </div></dialog>`;
}
function showDialog(html) {
  document.getElementById('data-dialog')?.remove();
  document.body.insertAdjacentHTML('beforeend', html);
  document.getElementById('data-dialog').showModal();
}
function closeDialog() { document.getElementById('data-dialog')?.close(); document.getElementById('data-dialog')?.remove(); }
function stopTts() { try { window.speechSynthesis?.cancel(); } catch {} }
function render() {
  destroyMap(); stopTts(); closeDialog();
  const target = view();
  let content;
  if (target === 'visitor') content = visitorHtml();
  else if (!canAdmin()) content = loginHtml();
  else content = adminHtml();
  app.innerHTML = header() + content + footer();
  if (target === 'visitor') showMap(document.getElementById('campus-map'), visiblePois(), currentPoi()?.id, selectPoi);
  const audio = document.getElementById('visitor-audio');
  audio?.addEventListener('play', async () => {
    try {
      const poi = currentPoi(); const n = currentNarration(poi);
      if (!poi || !n) return;
      if (!state.visitSessionId) state.visitSessionId = (await api.createSession()).id;
      await api.logListen({ session_id: state.visitSessionId, poi_id: poi.id, language_code: state.language, narration_id: n.id, source: state.locationSource });
    } catch (error) { console.warn('Không ghi nhận được lượt nghe:', error); }
  });
}
function selectPoi(id) {
  if (!visiblePois().some(p => Number(p.id) === Number(id))) return;
  state.selectedPoi = Number(id);
  state.locationSource = 'simulation';
  render();
}
async function fetchData() {
  const [pois, narrations, languages, routes] = await Promise.all([
    api.pois(!canAdmin()), api.narrations(!canAdmin()), api.languages(), api.routes().catch(() => []),
  ]);
  state.pois = Array.isArray(pois) ? pois : [];
  state.narrations = Array.isArray(narrations) ? narrations : [];
  state.languages = Array.isArray(languages) ? languages : [];
  state.routes = Array.isArray(routes) ? routes : [];
  if (!state.languages.some(l => l.code === state.language)) state.language = state.languages[0]?.code || 'vi';
  if (!visiblePois().some(p => Number(p.id) === Number(state.selectedPoi))) state.selectedPoi = visiblePois()[0]?.id || null;
}
async function loadAndRender() {
  app.innerHTML = header() + '<div class="loading-screen">Đang tải dữ liệu…</div>';
  try { await fetchData(); render(); }
  catch (error) { render(); notify(error.message, true); }
}

async function guarded(operation, success) {
  if (state.busy) return;
  state.busy = true;
  try { await operation(); await fetchData(); render(); notify(success); }
  catch (error) { notify(error.message, true); }
  finally { state.busy = false; }
}

/* Sự kiện được ủy quyền vì nội dung giao diện thay đổi sau mỗi thao tác. */
document.addEventListener('click', async event => {
  const element = event.target.closest('[data-action]');
  if (!element) return;
  const action = element.dataset.action;
  const id = Number(element.dataset.id);
  if (action === 'select-poi') { event.preventDefault(); selectPoi(id); }
  else if (action === 'close-dialog') closeDialog();
  else if (action === 'logout') {
    await guarded(async () => { await api.logout(); state.account = null; window.location.hash = '#/admin/login'; }, 'Đăng xuất thành công.');
  }
  else if (action === 'new-poi' && canAdmin()) showDialog(poiDialogHtml());
  else if (action === 'edit-poi' && canAdmin()) showDialog(poiDialogHtml(state.pois.find(p => Number(p.id) === id)));
  else if (action === 'toggle-poi' && canAdmin()) {
    const p = state.pois.find(x => Number(x.id) === id);
    if (p && confirm(`${isTruthy(p.is_visible) ? 'Ẩn' : 'Hiển thị'} POI “${p.name}”?`)) await guarded(() => api.togglePoi(id, !isTruthy(p.is_visible)), 'Cập nhật trạng thái POI thành công.');
  }
  else if (action === 'new-narration' && canAdmin()) {
    if (!state.pois.length) return notify('Cần tạo POI trước khi thêm nội dung.', true);
    showDialog(narrationDialogHtml());
  }
  else if (action === 'edit-narration' && canAdmin()) showDialog(narrationDialogHtml(state.narrations.find(n => Number(n.id) === id)));
  else if (action === 'toggle-narration' && canAdmin()) {
    const n = state.narrations.find(x => Number(x.id) === id);
    if (n && confirm(`${isTruthy(n.is_visible) ? 'Ẩn' : 'Hiển thị'} nội dung “${n.title}”?`)) await guarded(() => api.toggleNarration(id, !isTruthy(n.is_visible)), 'Cập nhật trạng thái thuyết minh thành công.');
  }
  else if (action === 'gps') {
    if (!navigator.geolocation) return notify('Trình duyệt không hỗ trợ định vị GPS.', true);
    element.disabled = true;
    navigator.geolocation.getCurrentPosition(position => {
      element.disabled = false;
      const { latitude, longitude } = position.coords;
      const candidates = visiblePois().map(p => ({ poi: p, distance: distanceMeters(latitude, longitude, Number(p.latitude), Number(p.longitude)) }))
        .filter(x => x.distance <= Number(x.poi.radius_m)).sort((a, b) => a.distance - b.distance);
      if (!candidates.length) { notify('Chưa phát hiện POI nào trong bán kính kích hoạt. Bạn có thể chọn thủ công trên bản đồ.'); return; }
      state.selectedPoi = candidates[0].poi.id; state.locationSource = 'gps'; render(); notify(`Đã xác định ${candidates[0].poi.name} bằng GPS.`);
    }, error => { element.disabled = false; notify(`Không lấy được GPS (${error.message}). Hãy chọn POI thủ công.`, true); }, { enableHighAccuracy: true, timeout: 12000, maximumAge: 0 });
  }
  else if (action === 'tts') {
    const narration = currentNarration(currentPoi());
    if (!narration || !('speechSynthesis' in window)) return notify('Thiết bị không hỗ trợ giọng đọc TTS.', true);
    stopTts(); const utterance = new SpeechSynthesisUtterance(narration.body);
    utterance.lang = state.language; utterance.rate = 0.9;
    window.speechSynthesis.speak(utterance);
  }
  else if (action === 'stop-tts') stopTts();
});
document.addEventListener('keydown', event => {
  if ((event.key === 'Enter' || event.key === ' ') && event.target.matches('[data-action="select-poi"][role="button"]')) {
    event.preventDefault(); selectPoi(Number(event.target.dataset.id));
  }
});
document.addEventListener('change', async event => {
  if (event.target.id === 'visitor-language') {
    state.language = event.target.value; render();
  }
  const id = event.target.dataset.audioId;
  if (id && canAdmin()) {
    const file = event.target.files?.[0]; if (!file) return;
    if (file.size > 20 * 1024 * 1024) return notify('Audio tối đa 20 MB.', true);
    await guarded(() => api.uploadAudio(id, file), 'Đã tải audio. Bạn có thể nghe thử ngay trong bảng.');
  }
});
document.addEventListener('input', event => {
  if (event.target.matches('[data-table-filter],#visitor-search')) {
    const value = event.target.value.toLocaleLowerCase('vi').trim();
    const selector = event.target.id === 'visitor-search' ? '#visitor-poi-list [data-card]' : '.data-table [data-row]';
    for (const row of document.querySelectorAll(selector)) row.hidden = !row.dataset.searchText.includes(value);
  }
});
document.addEventListener('submit', async event => {
  const form = event.target;
  const formType = form.getAttribute('id'); // Không dùng form.id: input name="id" có thể che thuộc tính này.
  if (!['login-form', 'poi-form', 'narration-form'].includes(formType)) return;
  event.preventDefault();
  const values = Object.fromEntries(new FormData(form));
  if (formType === 'login-form') {
    await guarded(async () => { const session = await api.login(values.email, values.password); state.account = session; window.location.hash = '#/admin/pois'; }, 'Đăng nhập thành công.');
  } else if (formType === 'poi-form' && canAdmin()) {
    const data = {
      ...(values.id ? { id: Number(values.id) } : {}), code: values.code.trim(), name: values.name.trim(), area: values.area.trim(), description: values.description.trim(),
      latitude: Number(values.latitude), longitude: Number(values.longitude), radius_m: Number(values.radius_m), image_url: values.image_url.trim(), is_visible: values.is_visible === 'true',
    };
    if (!Number.isFinite(data.latitude) || !Number.isFinite(data.longitude) || !(data.radius_m > 0)) return notify('Vui lòng nhập tọa độ và bán kính GPS hợp lệ.', true);
    await guarded(() => api.savePoi(data), values.id ? 'Cập nhật POI thành công.' : 'Thêm POI thành công.');
  } else if (formType === 'narration-form' && canAdmin()) {
    const data = { ...(values.id ? { id: Number(values.id) } : {}), poi_id: Number(values.poi_id), language_code: values.language_code, title: values.title.trim(), body: values.body.trim(), is_visible: values.is_visible === 'true' };
    if (!data.title || !data.body) return notify('Vui lòng nhập tiêu đề và nội dung.', true);
    await guarded(() => api.saveNarration(data), values.id ? 'Cập nhật thuyết minh thành công.' : 'Thêm thuyết minh thành công.');
  }
}, true); // Bắt submit ở capture phase (kể cả form trong <dialog>).
window.addEventListener('hashchange', () => render());
window.addEventListener('pagehide', destroyMap);
if (!window.location.hash) window.location.hash = '#/visitor';
loadAndRender();
