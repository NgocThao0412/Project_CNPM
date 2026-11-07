import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const code = fs.readFileSync('resources/assets/visitor-gps.js', 'utf8');
function element(id = '') {
  return {
    id, dataset: {}, disabled: false, hidden: false, textContent: '', href: '',
    style: {}, children: [], events: {},
    addEventListener(name, fn) { this.events[name] = fn; },
    replaceChildren(...nodes) { this.children = nodes; },
    appendChild(node) { this.children.push(node); },
    append(...nodes) { this.children.push(...nodes); },
    setAttribute(name, value) { this[name] = value; },
    scrollIntoView() {}
  };
}
function create({ secure = true, withMap = false } = {}) {
  const ids = Object.fromEntries([
    'sgu-gps-start', 'sgu-gps-stop', 'sgu-gps-center', 'sgu-gps-status',
    'sgu-gps-coordinates', 'sgu-gps-nearest', 'sgu-gps-nearest-text',
    'sgu-gps-nearest-link', 'sgu-gps-pois', 'sgu-coordinate-diagram',
    'sgu-gps-layer'
  ].map(id => [id, element(id)]));
  ids['sgu-gps-nearest'].hidden = true;
  ids['sgu-gps-nearest-link'].hidden = true;
  ids['sgu-gps-coordinates'].hidden = true;
  ids['sgu-coordinate-diagram'].dataset = {
    minLat: '10.000', maxLat: '10.002', minLon: '106.000', maxLon: '106.002'
  };
  const poi = element('poi');
  poi.dataset = {id:'3', name:'Cổng SGU', lat:'10.001', lon:'106.001', radius:'50'};
  ids['sgu-gps-pois'].querySelectorAll = () => [poi];
  const documentEvents = {};
  const document = {
    readyState:'complete', documentElement: {lang:'vi'},
    getElementById(id) { return ids[id] || null; },
    createElement() { return element(); }, createElementNS() { return element(); },
    addEventListener(name, fn) { documentEvents[name] = fn; }
  };
  const callbacks = [];
  const stopped = [];
  const navigator = {geolocation: {
    watchPosition(onSuccess, onError, options) {
      callbacks.push({onSuccess,onError,options});
      return callbacks.length;
    },
    clearWatch(id) { stopped.push(id); }
  }};
  const mapEvents = {}, mapMoves = [];
  const map = {
    project([lon,lat]) {return {x:lon * 50000, y:lat * 50000};},
    on(name,fn) {mapEvents[name]=fn;},
    easeTo(args) {mapMoves.push(args);}, getZoom(){return 16;}
  };
  const markers = [];
  const windowEvents = {};
  class Marker {
    constructor() { this.removed=false; markers.push(this); }
    setLngLat(coords) {this.coords=coords; return this;}
    addTo(m) {assert.equal(m,map);return this;}
    remove() {this.removed=true;}
  }
  const window = {
    isSecureContext: secure, sguPoiMap: withMap ? map : undefined,
    maptilersdk: withMap ? {Marker} : undefined,
    addEventListener(name,fn) {windowEvents[name]=fn;}
  };
  vm.runInNewContext(code,{document,window,navigator,console});
  return {ids,callbacks,stopped,markers,mapEvents,mapMoves,windowEvents,documentEvents};
}

const mock = create();
const id = mock.ids;
id['sgu-gps-start'].events.click();
assert.equal(mock.callbacks.length, 1);
assert.equal(mock.callbacks[0].options.enableHighAccuracy, true);
assert.equal(mock.callbacks[0].options.maximumAge, 0);
assert.equal(id['sgu-gps-start'].disabled, true);
mock.callbacks[0].onSuccess({coords:{latitude:10.001, longitude:106.001, accuracy:12}});
assert.match(id['sgu-gps-coordinates'].textContent,/10\.001000, 106\.001000/);
assert.match(id['sgu-gps-coordinates'].textContent,/12 m/);
assert.match(id['sgu-gps-nearest-text'].textContent,/Cổng SGU/);
assert.equal(id['sgu-gps-nearest-link'].href, '/place?id=3');
assert.equal(id['sgu-gps-nearest-link'].hidden, false);
assert.equal(id['sgu-gps-layer'].children.length, 2); // accuracy ellipse + position
id['sgu-gps-stop'].events.click();
assert.equal(mock.stopped[0], 1);
assert.equal(id['sgu-gps-layer'].children.length, 0);
assert.equal(id['sgu-gps-nearest'].hidden, true);
// Old callbacks may arrive after stop; they must not paint the UI again.
mock.callbacks[0].onSuccess({coords:{latitude:10.001, longitude:106.001, accuracy:4}});
assert.equal(id['sgu-gps-coordinates'].hidden, true);
// Explicit permission-denied handling.
id['sgu-gps-start'].events.click();
mock.callbacks[1].onError({code:1});
assert.equal(id['sgu-gps-status'].dataset.state,'error');
assert.equal(id['sgu-gps-start'].disabled,false);

const outside = create();
outside.ids['sgu-gps-start'].events.click();
outside.callbacks[0].onSuccess({coords:{latitude:11.0,longitude:107.0,accuracy:10}});
assert.equal(outside.ids['sgu-gps-layer'].children.length,0);
assert.match(outside.ids['sgu-gps-status'].textContent,/ngoài phạm vi/);
assert.equal(outside.ids['sgu-gps-nearest-link'].hidden,true);

const insecure = create({secure:false});
insecure.ids['sgu-gps-start'].events.click();
assert.equal(insecure.callbacks.length,0);
assert.match(insecure.ids['sgu-gps-status'].textContent,/HTTPS/);

const mapped = create({withMap:true});
mapped.ids['sgu-gps-start'].events.click();
mapped.callbacks[0].onSuccess({coords:{latitude:10.001,longitude:106.001,accuracy:5}});
assert.equal(mapped.markers.length,1);
assert.equal(mapped.mapMoves.length,1); // auto-center on first GPS fix
mapped.callbacks[0].onSuccess({coords:{latitude:10.0011,longitude:106.0011,accuracy:6}});
assert.equal(mapped.mapMoves.length,1); // do not constantly hijack user's map panning
mapped.ids['sgu-gps-center'].events.click();
assert.equal(mapped.mapMoves.length,2);
mapped.windowEvents.pagehide();
assert.equal(mapped.markers[0].removed,true);
console.log('PASS: live GPS watch, nearest POI, map and diagram, secure context, stop/error handling');
