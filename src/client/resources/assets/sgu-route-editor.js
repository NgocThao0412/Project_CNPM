(() => {
  'use strict';
  const list = document.getElementById('sgu-route-step-list');
  const add = document.getElementById('sgu-route-add-step');
  const count = document.getElementById('sgu-route-step-count');
  if (!list || !add || !count) return;
  const form = list.closest('form');
  if (!form) return;
  const fieldPrefix = /^(stopTitle|stopLocation|stopPoi|stopMinutes|stopInstructions)\d+$/;
  const cards = () => Array.from(list.querySelectorAll('[data-sgu-route-step]'));
  const active = () => cards().filter(card => !card.hidden);

  function field(card, key) {
    return Array.from(card.querySelectorAll('input, select, textarea'))
      .find(el => fieldPrefix.test(el.name) && el.name.replace(/\d+$/, '') === key);
  }
  function renumber() {
    const visible = active();
    const hidden = cards().filter(card => card.hidden);
    [...visible, ...hidden].forEach((card, index) => {
      const number = index + 1;
      const badge = card.querySelector('[data-sgu-route-number]');
      const label = card.querySelector('[data-sgu-route-label]');
      if (badge) badge.textContent = String(number);
      const value = field(card, 'stopTitle');
      const title = value ? value.value.trim() : '';
      if (label) label.textContent = `Bước ${number}: ${title || 'Chưa nhập'}`;
      card.querySelectorAll('input, select, textarea').forEach(el => {
        const match = el.name.match(fieldPrefix);
        if (match) el.name = `${match[1]}${number}`;
      });
    });
    count.textContent = `Tổng số bước: ${visible.length}`;
    add.disabled = visible.length >= 12;
    add.setAttribute('aria-disabled', String(add.disabled));
    if (add.disabled) add.title = 'Đã đạt tối đa 12 bước';
    else add.removeAttribute('title');
  }
  function clearCard(card) {
    card.querySelectorAll('input, select, textarea').forEach(el => {
      if (el.tagName === 'SELECT') el.value = '0';
      else if (el.name.startsWith('stopMinutes')) el.value = '0';
      else el.value = '';
      el.setCustomValidity('');
    });
  }
  add.addEventListener('click', () => {
    const spare = cards().find(card => card.hidden);
    if (!spare) return;
    clearCard(spare);
    spare.hidden = false;
    list.appendChild(spare);
    spare.open = true;
    renumber();
    const title = field(spare, 'stopTitle');
    if (title) title.focus();
  });
  list.addEventListener('click', (event) => {
    const edit = event.target.closest('[data-sgu-route-edit]');
    const remove = event.target.closest('[data-sgu-route-remove]');
    if (!edit && !remove) return;
    event.preventDefault();
    event.stopPropagation();
    const card = (edit || remove).closest('[data-sgu-route-step]');
    if (!card) return;
    if (edit) {
      card.open = !card.open;
      if (card.open) {
        const title = field(card, 'stopTitle');
        if (title) title.focus();
      }
    } else {
      if (!window.confirm('Xóa bước này khỏi lộ trình?')) return;
      card.open = false;
      card.hidden = true;
      clearCard(card);
      list.appendChild(card);
      renumber();
    }
  });
  // Reorder steps by dragging the dotted handle; the submitted field names
  // are renumbered to match the new visual order.
  let dragged = null;
  list.addEventListener('dragstart', event => {
    const grip = event.target.closest('.sgu-route-step-grip');
    if (!grip) { event.preventDefault(); return; }
    dragged = grip.closest('[data-sgu-route-step]');
    if (!dragged || dragged.hidden) return;
    event.dataTransfer.effectAllowed = 'move';
    event.dataTransfer.setData('text/plain', 'sgu-route-step');
    dragged.classList.add('sgu-route-dragging');
  });
  list.addEventListener('dragover', event => {
    if (!dragged) return;
    const target = event.target.closest('[data-sgu-route-step]');
    if (!target || target.hidden || target === dragged) return;
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
    list.querySelectorAll('.sgu-route-drop-target').forEach(card => card.classList.remove('sgu-route-drop-target'));
    target.classList.add('sgu-route-drop-target');
  });
  list.addEventListener('drop', event => {
    if (!dragged) return;
    const target = event.target.closest('[data-sgu-route-step]');
    if (target && !target.hidden && target !== dragged) {
      event.preventDefault();
      const midpoint = target.getBoundingClientRect().top + target.getBoundingClientRect().height / 2;
      if (event.clientY < midpoint) list.insertBefore(dragged, target);
      else list.insertBefore(dragged, target.nextSibling);
      renumber();
    }
  });
  list.addEventListener('dragend', () => {
    dragged = null;
    list.querySelectorAll('.sgu-route-dragging,.sgu-route-drop-target').forEach(card => {
      card.classList.remove('sgu-route-dragging','sgu-route-drop-target');
    });
  });
  list.addEventListener('input', event => {
    const item = event.target;
    if (item.matches('input[name^="stopTitle"]')) renumber();
    if (item.setCustomValidity) item.setCustomValidity('');
  });
  form.addEventListener('submit', event => {
    renumber();
    const mode = event.submitter && event.submitter.name === 'saveMode'
      ? event.submitter.value : 'draft';
    if (mode !== 'publish') return;
    if (!active().length) {
      event.preventDefault();
      window.alert('Cần thêm ít nhất một bước trước khi công khai lộ trình.');
      return;
    }
    for (const card of active()) {
      for (const [key, notice] of [
        ['stopTitle', 'Vui lòng nhập tên bước trước khi công khai.'],
        ['stopInstructions', 'Vui lòng nhập hướng dẫn thực hiện trước khi công khai.']
      ]) {
        const control = field(card, key);
        if (!control || control.value.trim()) continue;
        event.preventDefault();
        card.open = true;
        control.setCustomValidity(notice);
        control.reportValidity();
        control.focus();
        return;
      }
    }
  });
  renumber();
})();
