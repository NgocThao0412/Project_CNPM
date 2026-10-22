import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source=fs.readFileSync('app/src/main/resources/assets/visitor-activity.js','utf8');
const events={};
const timers=[];
const out=[];
const audio={paused:true,ended:false,dataset:{audioId:'5'},addEventListener(name,fn){events[name]=fn;}};
const store=new Map();
const sandbox={
  document:{body:{dataset:{visitorActivity:'true'}}, querySelector(){return audio;}},
  window:{crypto:{randomUUID(){return '024bb224-ad64-4af8-a01a-37f902623042';}},addEventListener(name,fn){events[name]=fn;}},
  crypto:{randomUUID(){return '024bb224-ad64-4af8-a01a-37f902623042';}},
  sessionStorage:{getItem(k){return store.get(k)},setItem(k,v){store.set(k,v)}},
  URLSearchParams,location:{pathname:'/place',search:'?id=3'},
  fetch(_url,opts){out.push(JSON.parse(opts.body));return Promise.resolve({ok:true});},
  Blob,navigator:{sendBeacon(_url,blob){out.push({sentBeacon:true,type:blob.type});return true;}},
  setInterval(fn,ms){timers.push({fn,ms});}
};
vm.runInNewContext(source,sandbox);
assert.equal(timers[0].ms,10000);
assert.equal(out.at(-1).event,'heartbeat');
assert.equal(out.at(-1).poiId,3);
events.play(); assert.equal(out.at(-1).event,'play'); assert.equal(out.at(-1).listening,true);
events.pause();assert.equal(out.at(-1).event,'pause');assert.equal(out.at(-1).listening,false);
events.ended();assert.equal(out.at(-1).event,'ended');
timers[0].fn();assert.equal(out.at(-1).event,'heartbeat');
events.pagehide();assert.equal(out.at(-1).sentBeacon,true);
console.log('Browser activity hooks: heartbeat 10s, play/pause/ended/close PASS');

const adminCode=fs.readFileSync('app/src/main/resources/assets/admin-visitors.js','utf8');
const counters=[{textContent:'—'}], listening=[{textContent:'—'}];
const note={textContent:''};
const adminTimers=[];
vm.runInNewContext(adminCode,{
  document:{
    hidden:false,
    querySelectorAll(selector){return selector==='[data-online-count]'?counters:listening;},
    querySelector(selector){return selector==='[data-visitor-note]'?note:null;},
    addEventListener(){}
  },
  fetch(){return Promise.resolve({ok:true,json(){return Promise.resolve({online:2,listening:1,visitors:[]});}});},
  setInterval(fn,ms){adminTimers.push({fn,ms});},
  console
});
await new Promise(resolve=>setTimeout(resolve,10));
assert.equal(adminTimers[0].ms,5000);
assert.equal(counters[0].textContent,'2');
assert.equal(listening[0].textContent,'1');
console.log('Admin polling: 5s, accessing != listening PASS');
