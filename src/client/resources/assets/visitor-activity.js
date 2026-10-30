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
    let currentPlay = null;

    function newEventId() {
      const bytes = crypto.getRandomValues(new Uint8Array(16));
      bytes[6] = (bytes[6] & 0x0f) | 0x40;
      bytes[8] = (bytes[8] & 0x3f) | 0x80;

      const hex = Array.from(
        bytes, value => value.toString(16).padStart(2, '0')
      ).join('');

      return [
        hex.slice(0, 8),
        hex.slice(8, 12),
        hex.slice(12, 16),
        hex.slice(16, 20),
        hex.slice(20)
      ].join('-');
    }

    async function sendListeningEvent(event) {
      if (event.sent || event.sending || event.stopped || closing) return;
      event.sending = true;
      event.attempts++;

      try {
        // Giữ nguyên token và eventId khi gửi lại cùng một lượt.
        if (!event.token) {
          await heartbeat();
          event.token = await currentToken();
        }

        const response = await request('/api/visitor/listening-events', {
          headers: {
            'X-Visitor-Token': event.token,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            eventId: event.id,
            audioFileId: event.audioId
          })
        });

        if (response.ok) {
          event.sent = true;
        } else {
          // Dữ liệu hoặc phiên không hợp lệ: không gửi lặp vô hạn.
          if (response.status >= 400 &&
              response.status < 500 &&
              response.status !== 429) {
            event.stopped = true;
          }
          throw new Error('Listening API HTTP ' + response.status);
        }
      } catch (error) {
        console.warn('Chưa lưu được lượt nghe:', error.message);
      } finally {
        event.sending = false;

        if (!event.sent && !event.stopped && !closing) {
          if (event.attempts < 5) {
            setTimeout(() => {
              void sendListeningEvent(event);
            }, 5000);
          } else {
            event.stopped = true;
            console.warn('Đã dừng thử gửi lượt nghe sau 5 lần.');
          }
        }
      }
    }

    player.addEventListener('playing', () => {
      listening = true;
      void heartbeat();

      const audioId = Number(player.dataset.audioId);
      if (!Number.isSafeInteger(audioId) || audioId <= 0) {
        console.warn('Trình phát thiếu ID audio hợp lệ.');
        return;
      }

      const source = player.currentSrc || player.src;

      if (!currentPlay ||
          currentPlay.audioId !== audioId ||
          currentPlay.source !== source) {
        currentPlay = {
          id: newEventId(),
          audioId,
          source,
          token: null,
          sent: false,
          sending: false,
          stopped: false,
          attempts: 0
        };

        void sendListeningEvent(currentPlay);
      }
    });

    for (const name of ['pause', 'ended', 'error', 'emptied', 'abort']) {
      player.addEventListener(name, () => {
        listening = false;
        void heartbeat();

        // Nghe hết hoặc nạp bài mới: lần phát sau là lượt mới.
        if (name === 'ended' || name === 'emptied') {
          currentPlay = null;
        }
      });
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
  setInterval(() => { void heartbeat(); },3000);
  document.addEventListener('visibilitychange',() => {
    if(!document.hidden) void heartbeat();
  });
})();
