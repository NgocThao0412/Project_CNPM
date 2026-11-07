/* Saigon Audio Guide - MapTiler 2D/3D campus map. POI + GPS integration remains intact. */
(function () {
  'use strict';
  document.addEventListener('DOMContentLoaded', function () {
    const container = document.getElementById('sgu-map');
    const pointsBox = document.getElementById('sgu-map-points');
    if (!container || !pointsBox) return;

    const status = document.getElementById('sgu-map-view-status');
    const btn2d = document.getElementById('sgu-map-2d');
    const btn3d = document.getElementById('sgu-map-3d');
    const requested2d = new URLSearchParams(window.location.search).get('view') === '2d';
    let is3d = !requested2d;
    let buildingsReady = false;
    const buildingId = 'sgu-buildings-3d';

    function report(message, error) {
      if (!status) return;
      status.textContent = message;
      status.dataset.error = error ? 'true' : 'false';
    }
    function buttons() {
      if (btn2d) { btn2d.setAttribute('aria-pressed', String(!is3d)); btn2d.classList.toggle('active', !is3d); }
      if (btn3d) { btn3d.setAttribute('aria-pressed', String(is3d)); btn3d.classList.toggle('active', is3d); }
    }
    buttons();

    if (!window.maptilersdk) {
      report('Không tải được MapTiler SDK. Kiểm tra kết nối Internet.', true);
      return;
    }
    if (!container.dataset.key) {
      report('Thiếu MAPTILER_API_KEY. Vui lòng cấu hình key trên Client.', true);
      return;
    }
    maptilersdk.config.apiKey = container.dataset.key;

    let map;
    try {
      map = new maptilersdk.Map({
        container: 'sgu-map',
        style: maptilersdk.MapStyle.STREETS,
        center: [Number(container.dataset.lon), Number(container.dataset.lat)],
        zoom: 18,
        pitch: is3d ? 57 : 0,
        bearing: is3d ? -25 : 0,
        maxPitch: 75
      });
    } catch (err) {
      report('Không khởi tạo được bản đồ MapTiler. Kiểm tra API key.', true);
      console.error('MapTiler initialization failed:', err);
      return;
    }

    // Exposed for visitor-gps.js. The map-ready event also supports asynchronous GPS init.
    window.sguPoiMap = map;
    document.dispatchEvent(new CustomEvent('sgu:map-ready', { detail: { map: map } }));

    function addBuildings() {
      if (buildingsReady) return true;
      // MapTiler Planet supplies building footprints/heights where mapped.
      if (!map.getSource('maptiler_planet')) return false;
      try {
        const layers = (map.getStyle() || {}).layers || [];
        const label = layers.find(layer => layer.type === 'symbol' && layer.layout && layer.layout['text-field']);
        map.addLayer({
          id: buildingId,
          source: 'maptiler_planet',
          'source-layer': 'building',
          type: 'fill-extrusion',
          minzoom: 15,
          filter: ['!', ['has', 'hide_3d']],
          layout: { visibility: is3d ? 'visible' : 'none' },
          paint: {
            'fill-extrusion-color': '#d8e2ed',
            'fill-extrusion-opacity': 0.84,
            'fill-extrusion-height': ['max', 2, ['to-number', ['coalesce', ['get', 'render_height'], ['get', 'height']], 8]],
            'fill-extrusion-base': ['to-number', ['get', 'render_min_height'], 0]
          }
        }, label ? label.id : undefined);
        buildingsReady = true;
        return true;
      } catch (err) {
        console.warn('3D building data unavailable for the selected map style:', err);
        return false;
      }
    }

    function setView(threeD, animate) {
      is3d = !!threeD;
      buttons();
      if (!map || !map.loaded()) return;
      if (is3d) {
        const hasBuildings = addBuildings();
        if (buildingsReady) map.setLayoutProperty(buildingId, 'visibility', 'visible');
        if (typeof map.enableTerrain === 'function') {
          try { map.enableTerrain(1); } catch (err) { console.warn('3D terrain unavailable:', err); }
        }
        report(hasBuildings
          ? 'Chế độ 3D: kéo để xoay hoặc nghiêng bản đồ. Tòa nhà phụ thuộc dữ liệu MapTiler.'
          : 'Chế độ 3D: đang nghiêng bản đồ; khu vực này có thể chưa có dữ liệu độ cao tòa nhà.');
      } else {
        if (buildingsReady) map.setLayoutProperty(buildingId, 'visibility', 'none');
        if (typeof map.disableTerrain === 'function') {
          try { if (!map.hasTerrain || map.hasTerrain()) map.disableTerrain(); }
          catch (err) { console.warn('Cannot turn off terrain:', err); }
        }
        report('Chế độ bản đồ 2D. Chọn 3D để xem tòa nhà và góc nhìn nghiêng.');
      }
      const options = { pitch: is3d ? 57 : 0, bearing: is3d ? -25 : 0, duration: animate ? 750 : 0 };
      if (animate && typeof map.easeTo === 'function') map.easeTo(options);
      else if (typeof map.jumpTo === 'function') map.jumpTo(options);
    }

    function setQueryView(mode) {
      try {
        const url = new URL(window.location.href);
        url.searchParams.set('view', mode);
        window.history.replaceState(window.history.state, '', url.pathname + url.search + url.hash);
      } catch (err) { console.warn('Could not persist map view:', err); }
    }
    if (btn2d) btn2d.addEventListener('click', function () { setView(false, true); setQueryView('2d'); });
    if (btn3d) btn3d.addEventListener('click', function () { setView(true, true); setQueryView('3d'); });

    // When changing the selected POI through the existing sidebar, retain 2D/3D mode.
    document.addEventListener('click', function (event) {
      const target = event.target && event.target.closest && event.target.closest('.map-list a[href*="/map?id="]');
      if (!target) return;
      const href = new URL(target.href, window.location.href);
      href.searchParams.set('view', is3d ? '3d' : '2d');
      target.href = href.pathname + href.search;
    });

    pointsBox.querySelectorAll('.sgu-map-point').forEach(function (point) {
      const lat = Number(point.dataset.lat), lon = Number(point.dataset.lon);
      if (!Number.isFinite(lat) || !Number.isFinite(lon)) return;
      const name = point.dataset.name || 'POI';
      const el = document.createElement('a');
      el.className = 'sgu-poi-marker' + (point.dataset.active === 'true' ? ' selected' : '');
      el.href = '/map?id=' + encodeURIComponent(point.dataset.id)
        + '&basemap=osm&view=' + (is3d ? '3d' : '2d');
      el.title = 'Xem địa điểm: ' + name;
      el.setAttribute('aria-label', el.title);
      const pin = document.createElement('span');
      pin.className = 'sgu-poi-pin';
      pin.setAttribute('aria-hidden', 'true');
      const label = document.createElement('span');
      label.className = 'sgu-poi-label';
      label.textContent = name;
      el.append(pin, label);
      // Keep link semantics for keyboards and touch interaction.
      el.addEventListener('click', function (event) {
        event.preventDefault();
        window.location.href = '/map?id=' + encodeURIComponent(point.dataset.id)
          + '&basemap=osm&view=' + (is3d ? '3d' : '2d');
      });
      new maptilersdk.Marker({ element: el, anchor: 'bottom' }).setLngLat([lon, lat]).addTo(map);
    });

    map.on('load', function () {
      setView(is3d, false);
      if (typeof maptilersdk.NavigationControl === 'function') {
        map.addControl(new maptilersdk.NavigationControl({ showCompass: true, visualizePitch: true }), 'bottom-right');
      }
    });
    map.on('error', function (event) {
      console.error('SGU MapTiler error:', event.error || event);
      report('Bản đồ không tải được. Kiểm tra mạng và MAPTILER_API_KEY, hoặc chuyển sang Sơ đồ tọa độ.', true);
    });
  });
})();
