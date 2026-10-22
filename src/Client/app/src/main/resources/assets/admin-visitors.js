/* Admin visitor counts: GET /api/admin/visitor-stats every 5 seconds on Client :8080. */
(() => {
  'use strict';
  const visitorsNodes=[...document.querySelectorAll('[data-online-count]')];
  const listenersNodes=[...document.querySelectorAll('[data-listening-count]')];
  if (!visitorsNodes.length && !listenersNodes.length) return;
  const note=document.querySelector('[data-visitor-note]');
  const tbody=document.querySelector('[data-visitor-rows]');
  let busy=false;
  function rowMessage(message) {
    if (!tbody) return;
    const tr=document.createElement('tr');
    const td=document.createElement('td');
    td.colSpan=4;td.className='visitor-empty';td.textContent=message;
    tr.append(td);tbody.replaceChildren(tr);
  }
  function unavailable(message) {
    visitorsNodes.forEach(el=>el.textContent='—');
    listenersNodes.forEach(el=>el.textContent='—');
    if(note) note.textContent=message;
    rowMessage('Không thể lấy số liệu từ visitor-service.');
  }
  async function update() {
    if(busy || document.hidden)return;
    busy=true;
    try {
      const response=await fetch('/api/admin/visitor-stats',{
        method:'GET',credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'}
      });
      if (!response.ok) throw new Error('visitor-service HTTP '+response.status);
      const body=await response.json();
      const visitors=body.activeVisitors, listeners=body.activeListeners;
      if (!Number.isSafeInteger(visitors) || !Number.isSafeInteger(listeners) ||
          visitors<0 || listeners<0 || listeners>visitors)
        throw new Error('Malformed stats');
      visitorsNodes.forEach(el=>el.textContent=String(visitors));
      listenersNodes.forEach(el=>el.textContent=String(listeners));
      if(note)note.textContent='Số liệu từ visitor-service · Cập nhật mỗi 5 giây';
      // This API provides aggregate counts, not personal visitor/session data.
      rowMessage('Backend cung cấp tổng số phiên ẩn danh, không cung cấp danh sách khách.');
    } catch (_) { unavailable('Chưa kết nối API thống kê hoặc phiên quản trị hết hạn.'); }
    finally { busy=false; }
  }
  void update();
  setInterval(() => { void update(); },5000);
  document.addEventListener('visibilitychange',() => { if(!document.hidden)void update(); });
})();
