import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import { webcrypto } from 'node:crypto';

const browserJS = fs.readFileSync('resources/assets/visitor-activity.js','utf8');
const store=new Map();
const events={};
const timers=[];
const requests=[];
const audio={paused:true,ended:false,dataset:{audioId:'2'},src:'http://localhost:8080/media/audio/2',addEventListener(name,fn){events[name]=fn;}};
const documentEvents={};
const visitorToken='backend-issued-random-token-abc';
const sandbox={
  crypto:webcrypto,
  setTimeout(){},
  console:{warn(){}},
  document:{body:{dataset:{visitorActivity:'true'}},querySelector(){return audio;},addEventListener(name,fn){documentEvents[name]=fn;},hidden:false},
  window:{addEventListener(name,fn){events[name]=fn;}},
  URL,URLSearchParams,location:{href:'http://localhost:8080/place?id=2',origin:'http://localhost:8080',pathname:'/place',search:'?id=2'},
  sessionStorage:{getItem(k){return store.get(k)||null;},setItem(k,v){store.set(k,v);},removeItem(k){store.delete(k);}},
  fetch(url,opts){
    requests.push({url,opts});
    if(url==='/api/visitor/sessions')return Promise.resolve({ok:true,status:200,json:()=>Promise.resolve({token:visitorToken})});
    return Promise.resolve({ok:true,status:204});
  },
  setInterval(fn,ms){timers.push({fn,ms});}
};
vm.runInNewContext(browserJS,sandbox);
const flush=async()=>{for(let i=0;i<10;i++)await Promise.resolve();};
await flush();
assert.equal(requests[0].url,'/api/visitor/sessions','token comes from backend');
assert.equal(requests[1].url,'/api/visitor/heartbeat');
assert.equal(requests[0].opts.body,undefined,'Create session does not require a request body');
assert.equal(requests[1].opts.headers['X-Visitor-Token'],visitorToken);
assert.equal(JSON.parse(requests[1].opts.body).listening,false);
assert.equal(timers[0].ms,10000);
assert.equal(store.get('sgu.audioGuide.visitorToken'),visitorToken);

const latestHeartbeat = () => requests.filter(r => r.url === '/api/visitor/heartbeat').at(-1);
audio.paused=false;events.playing();await flush();
assert.equal(JSON.parse(latestHeartbeat().opts.body).listening,true);
audio.paused=true;events.pause();await flush();
assert.equal(JSON.parse(latestHeartbeat().opts.body).listening,false);
audio.paused=false;events.playing();await flush();
audio.ended=true;events.ended();await flush();
assert.equal(JSON.parse(latestHeartbeat().opts.body).listening,false);
timers[0].fn();await flush();assert.equal(requests.at(-1).url,'/api/visitor/heartbeat');

// An internal route change cannot end the token/guest session.
documentEvents.click({target:{closest:()=>({href:'http://localhost:8080/places',target:''})},
  defaultPrevented:false,ctrlKey:false,metaKey:false,shiftKey:false,altKey:false});
events.pagehide({persisted:false});await flush();
assert.equal(requests.some(r=>r.url==='/api/visitor/end'),false);

// New document in same tab picks up stored token, no second POST sessions.
const secondCalls=[];
const secondEnv={...sandbox,document:{...sandbox.document,addEventListener(){}},window:{addEventListener(){}},
  fetch(url,opts){secondCalls.push({url,opts});return Promise.resolve({ok:true,status:204});},setInterval(){}};
vm.runInNewContext(browserJS,secondEnv);await flush();
assert.equal(secondCalls[0].url,'/api/visitor/heartbeat');
assert.equal(secondCalls[0].opts.headers['X-Visitor-Token'],visitorToken);

// Closing browser tab attempts POST end with token header and keepalive.
const freshEvents={};
const finalEnv={...sandbox,window:{addEventListener(n,f){freshEvents[n]=f;}},
 document:{...sandbox.document,addEventListener(){}},setInterval(){}};
vm.runInNewContext(browserJS,finalEnv);await flush();
freshEvents.pagehide({persisted:false});await flush();
const endCall=requests.find(r=>r.url==='/api/visitor/end');
assert.ok(endCall);assert.equal(endCall.opts.keepalive,true);
assert.equal(endCall.opts.headers['X-Visitor-Token'],visitorToken);
console.log('PASS: token reuse per tab, heartbeat 10s, actual audio events, end attempt, same-origin');

const adminCode=fs.readFileSync('resources/assets/admin-visitors.js','utf8');
const online=[{textContent:'—'}],listeners=[{textContent:'—'}],note={textContent:''};
const adminTimers=[],adminCalls=[];
vm.runInNewContext(adminCode,{
  document:{hidden:false,querySelectorAll(selector){return selector==='[data-online-count]'?online:listeners;},
    querySelector(selector){return selector==='[data-visitor-note]'?note:null;},addEventListener(){}},
  fetch(url,opts){adminCalls.push({url,opts});return Promise.resolve({ok:true,json:()=>Promise.resolve({activeVisitors:4,activeListeners:2})});},
  setInterval(fn,ms){adminTimers.push({fn,ms});}
});
await flush();
assert.equal(adminCalls[0].url,'/api/admin/visitor-stats');
assert.equal(adminCalls[0].opts.credentials,'same-origin');
assert.equal(adminTimers[0].ms,5000);
assert.equal(online[0].textContent,'4');assert.equal(listeners[0].textContent,'2');
console.log('PASS: admin polls 5s, 4 accessing vs 2 listening, same-origin cookies');
