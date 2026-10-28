/* Header language switcher: CSP-safe external script, no direct backend calls. */
(() => {
  'use strict';
  document.querySelectorAll('form.header-language').forEach((form) => {
    const next = form.querySelector('input[name="next"]');
    const picker = form.querySelector('select[name="lang"]');
    if (!next || !picker) return;
    // Keep current visitor page, POI id, preview state, search, and map options.
    next.value = location.pathname + location.search;
    form.classList.add('js-ready');
    picker.addEventListener('change', () => {
      if (typeof form.requestSubmit === 'function') form.requestSubmit();
      else form.submit();
    });
  });
})();
