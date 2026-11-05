/* Shared client-side pagination for the two statistics tables only.
 * Data, totals, filters, API requests and CSV export remain unchanged. */
(() => {
  'use strict';

  const PAGE_SIZE = 8;

  function createButton(text, label, onClick, className = '') {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `sgu-page-button ${className}`.trim();
    button.textContent = text;
    button.setAttribute('aria-label', label);
    button.addEventListener('click', onClick);
    return button;
  }

  function create(tbody, label, {printAll = false} = {}) {
    if (!tbody || !tbody.closest('.table-wrap')) {
      throw new Error('Không tìm thấy bảng cần phân trang.');
    }

    const nav = document.createElement('nav');
    nav.className = 'sgu-table-pagination';
    nav.setAttribute('aria-label', `Phân trang ${label}`);
    nav.hidden = true;

    const info = document.createElement('span');
    info.className = 'sgu-page-info';
    info.setAttribute('aria-live', 'polite');

    const controls = document.createElement('div');
    controls.className = 'sgu-page-controls';
    const prev = createButton('‹', 'Trang trước', () => goto(page - 1));
    const numbers = document.createElement('div');
    numbers.className = 'sgu-page-numbers';
    const next = createButton('›', 'Trang tiếp theo', () => goto(page + 1));
    controls.append(prev, numbers, next);
    nav.append(info, controls);
    tbody.closest('.table-wrap').insertAdjacentElement('afterend', nav);

    let page = 1;
    let count = 0;
    let printing = false;

    function pagesToShow(total) {
      const wanted = new Set([1, total]);
      for (let index = Math.max(1, page - 1); index <= Math.min(total, page + 1); index++) {
        wanted.add(index);
      }
      return [...wanted].sort((a, b) => a - b);
    }

    function render() {
      const total = Math.ceil(count / PAGE_SIZE);
      if (total === 0) {
        nav.hidden = true;
        return;
      }
      page = Math.min(Math.max(page, 1), total);
      const start = (page - 1) * PAGE_SIZE;
      const end = Math.min(start + PAGE_SIZE, count);
      // The placeholder row displayed for empty/error states is not counted.
      Array.from(tbody.rows).forEach((row, index) => {
        row.hidden = !printing && (index < start || index >= end);
      });
      nav.hidden = printing || total <= 1;
      info.textContent = `Hiển thị ${start + 1}–${end} trên ${count} dòng`;
      prev.disabled = page === 1;
      next.disabled = page === total;
      numbers.replaceChildren();
      let previous = 0;
      for (const index of pagesToShow(total)) {
        if (previous && index - previous > 1) {
          const ellipsis = document.createElement('span');
          ellipsis.className = 'sgu-page-ellipsis';
          ellipsis.textContent = '…';
          ellipsis.setAttribute('aria-hidden', 'true');
          numbers.append(ellipsis);
        }
        const button = createButton(String(index), `Trang ${index}`, () => goto(index),
          index === page ? 'is-current' : '');
        if (index === page) button.setAttribute('aria-current', 'page');
        numbers.append(button);
        previous = index;
      }
    }

    function goto(nextPage) {
      if (printing || nextPage < 1 || nextPage > Math.ceil(count / PAGE_SIZE)) return;
      page = nextPage;
      render();
    }

    function update(rowCount) {
      count = Number.isSafeInteger(rowCount) && rowCount > 0 ? rowCount : 0;
      page = 1; // A new API result always starts from the first page.
      render();
    }

    function clear() {
      count = 0;
      page = 1;
      nav.hidden = true;
    }

    // Report printing must include every row, not only the visible page.
    if (printAll) {
      window.addEventListener('beforeprint', () => {
        if (!count) return;
        printing = true;
        Array.from(tbody.rows).forEach(row => { row.hidden = false; });
        nav.hidden = true;
      });
      window.addEventListener('afterprint', () => {
        printing = false;
        render();
      });
    }

    return Object.freeze({ update, clear });
  }

  window.SguTablePagination = Object.freeze({ create });
})();
