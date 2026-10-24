(() => {
    'use strict';

    const root = document.querySelector('[data-auto-job]');
    if (!root) return;

    const jobId = root.dataset.autoJob;
    const note = root.querySelector('[data-auto-note]');

    const labels = {
        QUEUED: 'Đang chờ',
        RUNNING: 'Đang xử lý',
        DONE: 'Hoàn tất',
        FAILED: 'Có lỗi'
    };

    async function refresh() {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 15000);

        try {
            const response = await fetch(
                '/admin/content/auto/status?jobId='
                    + encodeURIComponent(jobId),
                {
                    credentials: 'same-origin',
                    cache: 'no-store',
                    headers: { Accept: 'application/json' },
                    signal: controller.signal
                }
            );

            if (response.redirected) {
                throw new Error(
                    'Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.'
                );
            }

            const contentType = response.headers.get('Content-Type') || '';

            if (!contentType.includes('application/json')) {
                throw new Error(
                    'Chưa lấy được tiến độ. Bấm Tải lại tiến độ để kiểm tra.'
                );
            }

            const job = await response.json();

            if (!response.ok) {
                throw new Error(
                    job.message || ('Không lấy được tiến độ: HTTP ' + response.status)
                );
            }

            if (!Array.isArray(job.languages)) {
                throw new Error('Dữ liệu tiến độ không hợp lệ.');
            }

            for (const item of job.languages) {
                const code = item.languageCode;

                if (!['vi', 'en', 'zh', 'ko'].includes(code)) continue;

                const row = root.querySelector(
                    '[data-auto-language="' + code + '"]'
                );

                if (!row) continue;

                const state = row.querySelector('[data-auto-state]');
                state.textContent = labels[item.status] || item.status;
                state.className = 'tag'
                    + (item.status === 'DONE' ? ' tag-green' : '');

                row.querySelector('[data-auto-message]').textContent =
                    item.message || '';

                const edit = row.querySelector('[data-auto-edit]');
                const contentId = Number(item.contentId);

                if (Number.isSafeInteger(contentId) && contentId > 0) {
                    edit.href = '/admin/content?id=' + contentId;
                    edit.hidden = false;
                }
            }

            if (job.status === 'DONE') {
                note.textContent =
                    'Đã hoàn tất nội dung và giọng đọc cho cả bốn ngôn ngữ.';
            } else if (job.status === 'PARTIAL') {
                note.textContent =
                    'Đã hoàn tất một phần. Xem chi tiết các ngôn ngữ bị lỗi bên dưới.';
            } else if (job.status === 'FAILED') {
                note.textContent =
                    'Tác vụ chưa hoàn tất. Xem nguyên nhân của từng ngôn ngữ bên dưới.';
            } else {
                note.textContent =
                    'Hệ thống đang xử lý. Tiến độ tự cập nhật mỗi 3 giây.';

                setTimeout(refresh, 3000);
            }
        } catch (error) {
            note.textContent = error.name === 'AbortError'
                ? 'Lấy tiến độ quá lâu. Bấm Tải lại tiến độ để kiểm tra.'
                : error.message;
        } finally {
            clearTimeout(timeout);
        }
    }

    void refresh();
})();