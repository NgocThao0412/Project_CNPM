/* UC-21: view/export a report using existing authenticated listening-stats API. */
(() => {
  'use strict';
  const section = document.getElementById('summary-report');
  if (!section) return;
  const find = id => document.getElementById(id);
  const form = find('report-filter');
  const loadButton = find('report-load');
  const csvButton = find('report-export-csv');
  const printButton = find('report-print');
  const feedback = find('report-feedback');
  const tbody = find('report-rows');
  let currentReport = null;
  let requesting = false;

  const dateParts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit'
  }).formatToParts(new Date());
  const datePart = key => dateParts.find(part => part.type === key).value;
  const today = `${datePart('year')}-${datePart('month')}-${datePart('day')}`;
  const firstDay = new Date(`${today}T00:00:00Z`);
  firstDay.setUTCDate(firstDay.getUTCDate() - 29);
  form.elements.namedItem('from').value = firstDay.toISOString().slice(0, 10);
  form.elements.namedItem('to').value = today;

  const number = value => {
    if (!Number.isSafeInteger(value) || value < 0) {
      throw new Error('API trả về số liệu không hợp lệ.');
    }
    return value.toLocaleString('vi-VN');
  };
  const displayDate = value => {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value));
    return match ? `${match[3]}/${match[2]}/${match[1]}` : String(value);
  };
  const selectedLabel = name => {
    const select = form.elements.namedItem(name);
    return select.options[select.selectedIndex]?.textContent?.trim() || 'Tất cả';
  };

  function placeholder(message) {
    tbody.replaceChildren();
    const row = document.createElement('tr');
    const cell = document.createElement('td');
    cell.colSpan = 5;
    cell.textContent = message;
    row.append(cell);
    tbody.append(row);
  }
  function setMetric(id, value) { find(id).textContent = number(value); }
  function invalidate() {
    currentReport = null;
    csvButton.disabled = true;
    printButton.disabled = true;
    for (const id of ['report-poi-count','report-poi-visible','report-listens','report-sessions']) {
      find(id).textContent = '—';
    }
    find('report-period').textContent = 'Chưa tải dữ liệu';
    find('report-selection').textContent = '';
    placeholder('Bộ lọc đã thay đổi, chưa cập nhật kết quả.');
    feedback.textContent = 'Bộ lọc đã thay đổi. Nhấn “Xem báo cáo” để cập nhật số liệu trước khi xuất.';
  }
  form.addEventListener('input', invalidate);
  form.addEventListener('change', invalidate);

  async function loadReport() {
    if (requesting || !form.reportValidity()) return;
    const from = form.elements.namedItem('from').value;
    const to = form.elements.namedItem('to').value;
    const start = new Date(`${from}T00:00:00Z`);
    const finish = new Date(`${to}T00:00:00Z`);
    const days = Math.round((finish - start) / 86400000) + 1;
    if (from > to || !Number.isFinite(days) || days < 1 || days > 366) {
      feedback.textContent = 'Ngày bắt đầu phải trước ngày kết thúc và tối đa 366 ngày.';
      return;
    }
    const params = new URLSearchParams();
    for (const [name, value] of new FormData(form)) {
      if (String(value).trim()) params.set(name, String(value).trim());
    }
    requesting = true;
    loadButton.disabled = true;
    invalidate();
    feedback.textContent = 'Đang tổng hợp báo cáo từ dữ liệu thật…';
    placeholder('Đang tải báo cáo…');
    for (const id of ['report-poi-count','report-poi-visible','report-listens','report-sessions']) {
      find(id).textContent = '—';
    }
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 30000);
    try {
      const response = await fetch('/api/admin/listening-stats?' + params.toString(), {
        method: 'GET', credentials: 'same-origin', cache: 'no-store',
        headers: {Accept: 'application/json'}, signal: controller.signal
      });
      if (response.status === 401) {
        throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
      }
      if (!response.ok) throw new Error(`Không tải được báo cáo (HTTP ${response.status}).`);
      const data = await response.json();
      if (!data || !Array.isArray(data.rows)) {
        throw new Error('API trả cấu trúc báo cáo không hợp lệ.');
      }
      // Validate BEFORE rendering/exporting, preventing partial/incorrect reports.
      number(data.totalListens);
      number(data.uniqueSessions);
      const rows = data.rows.map(row => ({
        date: String(row.date ?? ''),
        poiName: String(row.poiName ?? ''),
        languageName: String(row.languageName || row.languageCode || ''),
        listens: row.listens,
        uniqueSessions: row.uniqueSessions
      }));
      for (const row of rows) {
        number(row.listens);
        number(row.uniqueSessions);
      }
      const selectedPoi = form.elements.namedItem('poiId');
      const option = selectedPoi.options[selectedPoi.selectedIndex];
      const filtered = Boolean(selectedPoi.value);
      const poiTotal = filtered ? 1 : Number(section.dataset.poiTotal);
      const poiVisible = filtered ? Number(option.dataset.visible) : Number(section.dataset.poiVisible);
      number(poiTotal); number(poiVisible);
      const chosenPoi = selectedLabel('poiId');
      const chosenLanguage = selectedLabel('languageCode');
      const period = `${displayDate(from)} – ${displayDate(to)} (giờ Việt Nam)`;
      setMetric('report-poi-count', poiTotal);
      setMetric('report-poi-visible', poiVisible);
      setMetric('report-listens', data.totalListens);
      setMetric('report-sessions', data.uniqueSessions);
      find('report-poi-count-label').textContent = filtered ? 'POI được chọn' : 'Tổng POI hiện tại';
      find('report-period').textContent = 'Khoảng thống kê lượt nghe: ' + period;
      find('report-selection').textContent = `Địa điểm: ${chosenPoi} · Ngôn ngữ: ${chosenLanguage}`;
      const fragment = document.createDocumentFragment();
      for (const item of rows) {
        const tr = document.createElement('tr');
        for (const val of [displayDate(item.date), item.poiName, item.languageName,
                           number(item.listens), number(item.uniqueSessions)]) {
          const td = document.createElement('td');
          td.textContent = val;
          tr.append(td);
        }
        fragment.append(tr);
      }
      tbody.replaceChildren(fragment);
      if (!rows.length) placeholder('Không có lượt nghe phù hợp với bộ lọc.');
      currentReport = {from, to, chosenPoi, chosenLanguage, poiTotal, poiVisible,
                       totalListens: data.totalListens, uniqueSessions: data.uniqueSessions, rows};
      feedback.textContent = rows.length
        ? `Đã tổng hợp ${rows.length} dòng thống kê. Có thể xuất CSV hoặc in/lưu PDF.`
        : 'Không có lượt nghe trong khoảng lọc; báo cáo vẫn ghi nhận số POI hiện tại.';
      csvButton.disabled = false;
      printButton.disabled = false;
    } catch (error) {
      placeholder('Không thể tải báo cáo.');
      feedback.textContent = error.name === 'AbortError'
        ? 'Quá thời gian chờ 30 giây. Vui lòng thử lại.'
        : error.message;
    } finally {
      clearTimeout(timeout);
      requesting = false;
      loadButton.disabled = false;
    }
  }

  form.addEventListener('submit', event => {
    event.preventDefault();
    void loadReport();
  });

  // CSV escapes quotes, formulas and leading control characters (Excel injection).
  function csvCell(value) {
    let text = String(value ?? '');
    if (/^[\s\uFEFF]*[=+\-@\t\r\n]/.test(text)) text = "'" + text;
    return '"' + text.replace(/"/g, '""') + '"';
  }
  csvButton.addEventListener('click', () => {
    if (!currentReport) return;
    const r = currentReport;
    const lines = [
      ['BÁO CÁO TỔNG HỢP - SGU AUDIO GUIDE'],
      ['Từ ngày', r.from], ['Đến ngày', r.to],
      ['Địa điểm', r.chosenPoi], ['Ngôn ngữ', r.chosenLanguage],
      ['Số POI hiện tại (theo bộ lọc POI)', r.poiTotal],
      ['POI đang hiển thị (theo bộ lọc POI)', r.poiVisible],
      ['Tổng lượt nghe', r.totalListens],
      ['Số phiên có nghe', r.uniqueSessions],
      [],
      ['Ngày', 'Địa điểm', 'Ngôn ngữ', 'Lượt nghe', 'Phiên có nghe'],
      ...r.rows.map(row => [displayDate(row.date), row.poiName,
         row.languageName, row.listens, row.uniqueSessions])
    ];
    const csv = '\uFEFF' + lines.map(line => line.map(csvCell).join(';')).join('\r\n');
    const url = URL.createObjectURL(new Blob([csv], {type:'text/csv;charset=utf-8'}));
    const link = document.createElement('a');
    link.href = url;
    link.download = `SGU_BaoCao_${r.from}_${r.to}.csv`;
    document.body.append(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  });

  printButton.addEventListener('click', () => {
    if (!currentReport) return;
    document.body.classList.add('printing-sgu-report');
    window.print();
  });
  window.addEventListener('afterprint', () => {
    document.body.classList.remove('printing-sgu-report');
  });
})();
