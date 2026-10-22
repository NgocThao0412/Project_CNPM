/* Saigon Audio Guide visitor telemetry. Anonymous per-tab session, no personal data. */
(() => {
  'use strict';
  if (!document.body?.dataset.visitorActivity || !window.crypto?.randomUUID ||
      new URLSearchParams(location.search).get('preview') === '1') return;
  const key = 'sgu.audioGuide.anonymousTab';
  let sessionId;
  try {
    sessionId = sessionStorage.getItem(key);
    if (!sessionId) { sessionId = crypto.randomUUID(); sessionStorage.setItem(key, sessionId); }
  } catch (_) { sessionId = crypto.randomUUID(); }
  const endpoint = '/client/visitor/activity';
  const audio = document.querySelector('audio[data-visitor-audio]');
  const parsedId = new URLSearchParams(location.search).get('id');
  const poiId = location.pathname === '/place' && /^\d+$/.test(parsedId || '') ? Number(parsedId) : 0;
  const audioId = Number(audio?.dataset.audioId || 0);
  let listening = Boolean(audio && !audio.paused && !audio.ended);

  function data(event) {
    return JSON.stringify({sessionId, event, page: location.pathname,
      poiId, audioId, listening});
  }
  function notify(event) {
    fetch(endpoint, {method:'POST', credentials:'same-origin',
      headers:{'Content-Type':'application/json'}, body:data(event), keepalive:true})
      .catch(() => { /* Network loss expires on the server after 30 seconds. */ });
  }
  if (audio) {
    audio.addEventListener('play', () => { listening=true; notify('play'); });
    audio.addEventListener('pause', () => { listening=false; notify('pause'); });
    audio.addEventListener('ended', () => { listening=false; notify('ended'); });
  }
  notify('heartbeat');
  setInterval(() => notify('heartbeat'), 10000);
  window.addEventListener('pagehide', () => {
    listening=false;
    try { navigator.sendBeacon(endpoint, new Blob([data('close')],
      {type:'application/json'})); } catch (_) { /* Timeout handles disconnect. */ }
  });
})();
