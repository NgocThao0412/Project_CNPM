/* Admin: refresh independent browsing/listening counts every five seconds. */
(() => {
  'use strict';
  const nodes = [...document.querySelectorAll('[data-online-count]')];
  if (!nodes.length) return;
  const listeningNodes = [...document.querySelectorAll('[data-listening-count]')];
  const note = document.querySelector('[data-visitor-note]');
  const tbody = document.querySelector('[data-visitor-rows]');
  let busy = false;
  const safeInt = value => Number.isInteger(value) && value >= 0 ? value : null;
  const cell = text => { const td=document.createElement('td'); td.textContent=String(text); return td; };
  function unavailable(reason) {
    nodes.forEach(n => n.textContent='—');
    listeningNodes.forEach(n => n.textContent='—');
    if (note) note.textContent=reason || 'Chưa kết nối API người dùng.';
    if (tbody) {
      tbody.replaceChildren();
      const tr=document.createElement('tr'), td=cell(reason || 'Không nhận được dữ liệu');
      td.colSpan=4; td.className='visitor-empty'; tr.append(td); tbody.append(tr);
    }
  }
  function render(payload) {
    const online=safeInt(payload.online), listening=safeInt(payload.listening);
    if (online===null || listening===null || listening>online || !Array.isArray(payload.visitors))
      throw new Error('Định dạng API người dùng không hợp lệ');
    nodes.forEach(n => n.textContent=String(online));
    listeningNodes.forEach(n => n.textContent=String(listening));
    if(note) note.textContent='Cập nhật tự động mỗi 5 giây · Phiên hết hạn sau 30 giây không có tín hiệu';
    if (!tbody) return;
    const fragment=document.createDocumentFragment();
    if (!payload.visitors.length) {
      const tr=document.createElement('tr'), td=cell('Chưa có khách đang hoạt động');
      td.colSpan=4; td.className='visitor-empty';tr.append(td);fragment.append(tr);
    }
    for(const visitor of payload.visitors.slice(0,80)) {
      const tr=document.createElement('tr');
      const isListening=visitor.status==='listening';
      const id=typeof visitor.visitorId==='string' ? visitor.visitorId.slice(0,16) : 'Ẩn danh';
      const page=typeof visitor.page==='string' ? visitor.page.slice(0,100) : '/';
      const seconds=safeInt(visitor.secondsAgo);
      tr.append(cell('Khách '+id), cell(isListening?'Đang nghe':'Đang truy cập'),
        cell(page),cell(seconds===null?'—':seconds+' giây trước'));
      fragment.append(tr);
    }
    tbody.replaceChildren(fragment);
  }
  async function update() {
    if (busy || document.hidden) return;
    busy=true;
    try {
      const response=await fetch('/client/admin/visitor-summary',
        {credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'}});
      if (!response.ok) throw new Error('API HTTP '+response.status);
      render(await response.json());
    } catch (_) { unavailable('Chưa kết nối API người dùng hoặc phiên đăng nhập hết hạn.'); }
    finally { busy=false; }
  }
  update();
  setInterval(update,5000);
  document.addEventListener('visibilitychange', () => { if(!document.hidden) update(); });
})();
