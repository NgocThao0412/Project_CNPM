(() => {
  'use strict';

  const form = document.querySelector('#listening-filter');
  if (!form) return;

  const message = document.querySelector('#listening-message');
  const rows = document.querySelector('#listening-rows');
  const listens = document.querySelector('#history-listens');
  const sessions = document.querySelector('#history-sessions');
  const button = form.querySelector('button[type="submit"]');
  const from = form.elements.namedItem('from');
  const to = form.elements.namedItem('to');

  // Chọn ngày theo múi giờ Việt Nam, không theo múi giờ thiết bị.
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).formatToParts(new Date());

  const part = type => parts.find(p => p.type === type).value;
  const today = `${part('year')}-${part('month')}-${part('day')}`;
  const firstDay = new Date(`${today}T00:00:00Z`);
  firstDay.setUTCDate(firstDay.getUTCDate() - 29);

  from.value = firstDay.toISOString().slice(0, 10);
  to.value = today;

  function showTableMessage(text) {
    rows.replaceChildren();
    const row = document.createElement('tr');
    const cell = document.createElement('td');
    cell.colSpan = 5;
    cell.textContent = text;
    row.append(cell);
    rows.append(row);
  }

  function count(value) {
    if (!Number.isSafeInteger(value) || value < 0) {
      throw new Error('API trả số liệu thống kê không hợp lệ.');
    }
    return value.toLocaleString('vi-VN');
  }

  function displayDate(value) {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value));
    return match ? `${match[3]}/${match[2]}/${match[1]}` : String(value);
  }

  async function loadStats() {
    if (!form.reportValidity()) return;

    if (from.value > to.value) {
      message.textContent = 'Từ ngày phải nhỏ hơn hoặc bằng đến ngày.';
      return;
    }

    button.disabled = true;
    listens.textContent = '—';
    sessions.textContent = '—';
    message.textContent = 'Đang tải thống kê…';
    showTableMessage('Đang tải…');

    const query = new URLSearchParams();
    for (const [name, value] of new FormData(form)) {
      if (String(value).trim()) query.set(name, String(value).trim());
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 30000);

    try {
      const response = await fetch(
        '/api/admin/listening-stats?' + query.toString(),
        {
          credentials: 'same-origin',
          cache: 'no-store',
          signal: controller.signal,
          headers: { Accept: 'application/json' }
        }
      );

      if (response.status === 401) {
        throw new Error('Phiên admin đã hết hạn. Vui lòng đăng nhập lại.');
      }
      if (!response.ok) {
        throw new Error(
          `Không tải được thống kê (HTTP ${response.status}). ` +
          'Kiểm tra bộ lọc hoặc thử lại.'
        );
      }

      const data = await response.json();
      if (!Array.isArray(data.rows)) {
        throw new Error('API trả dữ liệu thống kê không đúng định dạng.');
      }

      // Dùng tổng do backend tính, không cộng số phiên ở từng dòng.
      const totalListens = count(data.totalListens);
      const totalSessions = count(data.uniqueSessions);
      const fragment = document.createDocumentFragment();

      for (const item of data.rows) {
        const row = document.createElement('tr');
        const values = [
          displayDate(item.date),
          item.poiName,
          item.languageName || item.languageCode,
          count(item.listens),
          count(item.uniqueSessions)
        ];

        for (const value of values) {
          const cell = document.createElement('td');
          cell.textContent = String(value ?? '');
          row.append(cell);
        }
        fragment.append(row);
      }

      rows.replaceChildren(fragment);
      if (data.rows.length === 0) {
        showTableMessage('Không có lượt nghe phù hợp với bộ lọc.');
      }

      listens.textContent = totalListens;
      sessions.textContent = totalSessions;
      message.textContent =
        `Từ ${displayDate(data.from)} đến ${displayDate(data.to)} · ` +
        `Múi giờ: ${data.timezone}. Bấm “Xem thống kê” để cập nhật.`;
    } catch (error) {
      listens.textContent = '—';
      sessions.textContent = '—';
      showTableMessage('Chưa tải được dữ liệu.');
      message.textContent = error.name === 'AbortError'
        ? 'Yêu cầu quá thời gian chờ. Vui lòng thử lại.'
        : error.message;
    } finally {
      clearTimeout(timeout);
      button.disabled = false;
    }
  }

  form.addEventListener('submit', event => {
    event.preventDefault();
    void loadStats();
  });

  void loadStats();
})();