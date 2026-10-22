/* Saigon Audio Guide - token-based anonymous visitor; all URLs stay on Client :8080. */
(() => {
  'use strict';
  if (document.body?.dataset.visitorActivity !== 'true' ||
      new URLSearchParams(location.search).get('preview') === '1') return;

  const key = 'sgu.audioGuide.visitorToken';
  const createUrl = '/api/visitor/sessions';
  const heartbeatUrl = '/api/visitor/heartbeat';
  const endUrl = '/api/visitor/end';
  const player = document.querySelector('audio[data-visitor-audio]');
  let listening = Boolean(player && !player.paused && !player.ended);
  let token = null;
  let pendingCreate = null;
  let navigatingInside = false;
  let closing = false;

  try { token = sessionStorage.getItem(key); } catch (_) { /* Private browsing */ }

  const request = (url, opts={}) => fetch(url, {
    method:'POST', credentials:'same-origin', cache:'no-store',
    ...opts
  });
  async function createSession() {
    if (pendingCreate) return pendingCreate;
    pendingCreate = (async () => {
      const response = await request(createUrl);
      if (!response.ok) throw new Error('Cannot create visitor session: '+response.status);
      const result = await response.json();
      if (typeof result.token !== 'string' || !/^[A-Za-z0-9._~-]{8,512}$/.test(result.token))
        throw new Error('Visitor service returned an invalid token');
      token = result.token;
      try { sessionStorage.setItem(key,token); } catch (_) { /* No storage */ }
      return token;
    })();
    try { return await pendingCreate; } finally { pendingCreate=null; }
  }
  async function currentToken() { return token || createSession(); }
  async function heartbeat(retry=true) {
    if (closing) return;
    try {
      const current=await currentToken();
      const response=await request(heartbeatUrl,{
        headers:{'X-Visitor-Token':current,'Content-Type':'application/json'},
        body:JSON.stringify({listening})
      });
      if ((response.status === 401 || response.status === 404 || response.status === 410) && retry) {
        token=null;
        try { sessionStorage.removeItem(key); } catch (_) {}
        await createSession();
        return heartbeat(false);
      }
      if (!response.ok) throw new Error('Heartbeat API '+response.status);
    } catch (_) { /* Backend will expire stale visitor after 30 s. Retry next tick. */ }
  }

  if (player) {
    const sync = () => { listening = !player.paused && !player.ended; void heartbeat(); };
    player.addEventListener('playing', sync);
    for (const name of ['pause','ended','error','emptied','abort']) {
      player.addEventListener(name, () => { listening=false; void heartbeat(); });
    }
  }

  // Moving between Client pages keeps one token for the same browser tab.
  document.addEventListener('click', event => {
    const link=event.target?.closest?.('a[href]');
    if (!link || event.defaultPrevented || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey
        || link.target === '_blank') return;
    try { const url=new URL(link.href,location.href);
      if (url.origin===location.origin) navigatingInside=true;
    } catch (_) {}
  },true);
  document.addEventListener('submit',event => {
    const form=event.target;
    if (form && (!form.action || new URL(form.action,location.href).origin===location.origin))
      navigatingInside=true;
  },true);
  window.addEventListener('pagehide', event => {
    if(event.persisted || navigatingInside || !token)return;
    closing=true;
    // No sendBeacon here: the API requires X-Visitor-Token header.
    try { void request(endUrl,{
      headers:{'X-Visitor-Token':token},keepalive:true
    }).catch(() => {}); } catch (_) {}
  });
  void heartbeat();
  setInterval(() => { void heartbeat(); },10000);
  document.addEventListener('visibilitychange',() => {
    if(!document.hidden) void heartbeat();
  });
})();
