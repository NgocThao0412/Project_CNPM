/* Saigon Audio Guide - real GPS for the existing Client map, not a simulator.
 * The visitor's coordinates never leave this browser. No backend/API calls.
 */
(function () {
  'use strict';

  function init() {
    const $ = (id) => document.getElementById(id);
    const startButton = $('sgu-gps-start');
    const stopButton = $('sgu-gps-stop');
    const centerButton = $('sgu-gps-center');
    const status = $('sgu-gps-status');
    const coordinates = $('sgu-gps-coordinates');
    const nearestBox = $('sgu-gps-nearest');
    const nearestText = $('sgu-gps-nearest-text');
    const nearestLink = $('sgu-gps-nearest-link');
    const pointsBox = $('sgu-gps-pois');
    const diagram = $('sgu-coordinate-diagram');
    const diagramLayer = $('sgu-gps-layer');
    if (!startButton || !stopButton || !centerButton || !status || !pointsBox) return;

    const language = (document.documentElement.lang || 'vi').toLowerCase().slice(0, 2);
    const messages = {
      vi: {
        starting: 'Đang xin quyền và tìm tín hiệu GPS…',
        active: 'GPS thực tế đang cập nhật vị trí.',
        stopped: 'Đã dừng GPS. Vị trí không được lưu lên máy chủ.',
        unsupported: 'Trình duyệt này không hỗ trợ GPS.',
        https: 'GPS trên điện thoại cần HTTPS. Hãy mở trang bằng liên kết HTTPS (hoặc localhost khi thử trên máy tính).',
        denied: 'Bạn đã từ chối quyền vị trí. Hãy bật quyền Vị trí cho website trong cài đặt trình duyệt rồi thử lại.',
        unavailable: 'Chưa lấy được vị trí. Hãy bật định vị trên điện thoại, ra nơi thoáng và thử lại.',
        timeout: 'GPS phản hồi chậm. Đang tiếp tục chờ vị trí…',
        invalid: 'Trình duyệt trả về tọa độ không hợp lệ.',
        outside: 'Vị trí thật đang nằm ngoài phạm vi sơ đồ POI. Có thể chuyển sang bản đồ nền để xem.',
        coords: 'Vị trí thật', accuracy: 'Độ chính xác ước tính',
        nearby: 'POI gần nhất', within: 'Bạn đang trong bán kính kích hoạt',
        notWithin: 'Chưa vào bán kính kích hoạt POI',
        uncertainty: 'Tín hiệu GPS chưa đủ chính xác để xác nhận điểm dừng.',
        meters: 'm', km: 'km'
      },
      en: {
        starting: 'Requesting location permission and GPS fix…', active: 'Live GPS is updating your location.',
        stopped: 'GPS stopped. Your location is not saved to the server.',
        unsupported: 'This browser does not support location services.',
        https: 'Live GPS requires HTTPS on phones. Use an HTTPS address (or localhost on your computer).',
        denied: 'Location access was denied. Enable it in your browser settings and retry.',
        unavailable: 'Location unavailable. Turn on GPS, move outdoors, and retry.',
        timeout: 'GPS is taking longer than expected. Still waiting for a fix…',
        invalid: 'The browser supplied invalid coordinates.',
        outside: 'Your position is outside this coordinate diagram. Try the online map.',
        coords: 'Live position', accuracy: 'Estimated accuracy',
        nearby: 'Nearest POI', within: 'Within activation radius',
        notWithin: 'Outside the POI activation radius',
        uncertainty: 'GPS accuracy is too low to confirm this stop.', meters: 'm', km: 'km'
      },
      zh: {
        starting: '正在请求定位权限和 GPS 信号…', active: '实时 GPS 正在更新位置。',
        stopped: '已停止 GPS，位置不会上传到服务器。', unsupported: '此浏览器不支持定位。',
        https: '手机实时定位需要 HTTPS，请使用 HTTPS 地址访问。',
        denied: '定位权限已拒绝，请在浏览器设置中开启后重试。',
        unavailable: '无法获取位置，请开启手机定位并到开阔处重试。',
        timeout: 'GPS 响应较慢，正在继续等待…', invalid: '浏览器返回了无效坐标。',
        outside: '当前位置超出坐标图范围，请切换到在线地图。',
        coords: '实时位置', accuracy: '估计精度', nearby: '最近地点',
        within: '位于触发范围内', notWithin: '尚未进入触发范围',
        uncertainty: 'GPS 精度不足以确认已到达。', meters: '米', km: '公里'
      },
      ko: {
        starting: '위치 권한과 GPS 신호를 확인하는 중…', active: '실시간 GPS 위치를 업데이트하고 있습니다.',
        stopped: 'GPS가 중지되었습니다. 위치는 서버에 저장되지 않습니다.',
        unsupported: '이 브라우저는 위치 서비스를 지원하지 않습니다.',
        https: '휴대전화 GPS는 HTTPS 주소에서만 사용할 수 있습니다.',
        denied: '위치 권한이 거부되었습니다. 브라우저 설정에서 허용 후 다시 시도하세요.',
        unavailable: '위치를 가져올 수 없습니다. 위치 서비스를 켜고 야외에서 다시 시도하세요.',
        timeout: 'GPS 응답이 느립니다. 계속 위치를 기다리는 중…',
        invalid: '브라우저가 올바르지 않은 좌표를 반환했습니다.',
        outside: '현재 위치가 좌표 지도 범위 밖에 있습니다. 온라인 지도를 이용하세요.',
        coords: '현재 위치', accuracy: '예상 정확도', nearby: '가장 가까운 장소',
        within: '활성화 반경 안에 있습니다', notWithin: '활성화 반경 밖에 있습니다',
        uncertainty: 'GPS 정확도가 낮아 도착 여부를 확인할 수 없습니다.', meters: 'm', km: 'km'
      }
    };
    const t = messages[language] || messages.vi;

    const points = Array.from(pointsBox.querySelectorAll('.sgu-gps-poi'))
      .map((element) => ({
        id: element.dataset.id, name: element.dataset.name,
        lat: Number(element.dataset.lat), lon: Number(element.dataset.lon),
        radius: Number(element.dataset.radius)
      })).filter((point) => Number.isFinite(point.lat) && Number.isFinite(point.lon));

    let watchId = null;
    let active = false;
    let generation = 0;
    let latest = null;
    let map = window.sguPoiMap || null;
    let marker = null;
    let markerRing = null;
    let latestMapFix = null;
    let firstFix = true;

    function setStatus(text, state) {
      status.textContent = text;
      status.dataset.state = state || 'normal';
    }
    function formatDistance(meters) {
      return meters < 1000 ? Math.round(meters) + ' ' + t.meters
        : (meters / 1000).toFixed(2) + ' ' + t.km;
    }
    function haversine(lat, lon, otherLat, otherLon) {
      const radians = (deg) => deg * Math.PI / 180;
      const dLat = radians(otherLat - lat);
      const dLon = radians(otherLon - lon);
      const a = Math.sin(dLat / 2) ** 2
        + Math.cos(radians(lat)) * Math.cos(radians(otherLat)) * Math.sin(dLon / 2) ** 2;
      return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0, 1 - a)));
    }
    function showClosest(lat, lon, accuracy) {
      if (!points.length || !nearestBox || !nearestText || !nearestLink) return;
      let best = null;
      for (const point of points) {
        const distance = haversine(lat, lon, point.lat, point.lon);
        if (!best || distance < best.distance) best = { point, distance };
      }
      if (!best) return;
      const { point, distance } = best;
      const inside = distance <= point.radius;
      const uncertain = inside && Number.isFinite(accuracy) && accuracy > point.radius;
      nearestText.textContent = t.nearby + ': ' + point.name + ' (' + formatDistance(distance) + '). '
        + (inside ? t.within : t.notWithin) + ' (' + formatDistance(point.radius) + ').'
        + (uncertain ? ' ' + t.uncertainty : '');
      nearestBox.hidden = false;
      nearestLink.hidden = !inside;
      if (inside) nearestLink.href = '/place?id=' + encodeURIComponent(point.id);
    }
    function drawDiagram(lat, lon, accuracy) {
      if (!diagram || !diagramLayer) return false;
      const d = diagram.dataset;
      const minLat = Number(d.minLat), maxLat = Number(d.maxLat);
      const minLon = Number(d.minLon), maxLon = Number(d.maxLon);
      if (!(maxLat > minLat && maxLon > minLon)) return false;
      const x = 70 + (lon - minLon) / (maxLon - minLon) * 620;
      const y = 390 - (lat - minLat) / (maxLat - minLat) * 290;
      // This diagram has fixed campus bounds; do not show a false clamped GPS position.
      diagramLayer.replaceChildren();
      if (!Number.isFinite(x) || !Number.isFinite(y)
          || x < 0 || x > 760 || y < 0 || y > 460) return false;
      const ns = 'http://www.w3.org/2000/svg';
      if (Number.isFinite(accuracy) && accuracy > 0) {
        const ellipse = document.createElementNS(ns, 'ellipse');
        ellipse.setAttribute('class', 'sgu-gps-circle');
        ellipse.setAttribute('cx', String(x));
        ellipse.setAttribute('cy', String(y));
        ellipse.setAttribute('rx', String(accuracy / (111320 * Math.max(.01, Math.cos(lat * Math.PI / 180))) * 620 / (maxLon - minLon)));
        ellipse.setAttribute('ry', String(accuracy / 111320 * 290 / (maxLat - minLat)));
        diagramLayer.appendChild(ellipse);
      }
      const dot = document.createElementNS(ns, 'circle');
      dot.setAttribute('class', 'sgu-gps-position');
      dot.setAttribute('cx', String(x));
      dot.setAttribute('cy', String(y));
      dot.setAttribute('r', '8');
      diagramLayer.appendChild(dot);
      return true;
    }
    function accuracyPixels() {
      if (!map || !latestMapFix || !markerRing) return;
      const { lat, lon, accuracy } = latestMapFix;
      // MapTiler/MapLibre projects WGS84 to screen pixels at the current zoom.
      const longitudeDelta = Math.min(1, accuracy / (111320 * Math.max(.01, Math.cos(lat * Math.PI / 180))));
      const center = map.project([lon, lat]);
      const east = map.project([lon + longitudeDelta, lat]);
      const radius = Math.abs(east.x - center.x);
      if (Number.isFinite(radius)) {
        // Large circles may extend beyond the screen; the number shown is authoritative.
        markerRing.style.width = Math.max(8, radius * 2) + 'px';
        markerRing.style.height = Math.max(8, radius * 2) + 'px';
      }
    }
    function drawMap(lat, lon, accuracy) {
      if (!map || !window.maptilersdk) return;
      latestMapFix = { lat, lon, accuracy };
      if (!marker) {
        const element = document.createElement('div');
        element.className = 'sgu-gps-marker';
        markerRing = document.createElement('div');
        markerRing.className = 'sgu-gps-accuracy';
        const dot = document.createElement('div');
        dot.className = 'sgu-gps-dot';
        element.append(markerRing, dot);
        marker = new window.maptilersdk.Marker({ element, anchor:'center' })
          .setLngLat([lon, lat]).addTo(map);
        map.on('zoom', accuracyPixels);
      } else marker.setLngLat([lon, lat]);
      accuracyPixels();
      if (firstFix) {
        firstFix = false;
        map.easeTo({ center: [lon, lat], zoom: Math.max(17, map.getZoom()) });
      }
    }
    function onPosition(position, token) {
      if (!active || token !== generation) return;
      const { latitude: lat, longitude: lon, accuracy } = position.coords || {};
      if (!Number.isFinite(lat) || !Number.isFinite(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
        setStatus(t.invalid, 'error');
        return;
      }
      latest = { lat, lon, accuracy };
      const shownAccuracy = Number.isFinite(accuracy) && accuracy >= 0 ? formatDistance(accuracy) : '—';
      coordinates.textContent = t.coords + ': ' + lat.toFixed(6) + ', ' + lon.toFixed(6)
        + ' · ' + t.accuracy + ': ±' + shownAccuracy;
      coordinates.hidden = false;
      centerButton.disabled = false;
      const onDiagram = drawDiagram(lat, lon, accuracy);
      drawMap(lat, lon, accuracy);
      setStatus(diagram && !onDiagram ? t.active + ' ' + t.outside : t.active);
      showClosest(lat, lon, accuracy);
    }
    function stop(message) {
      generation++;
      active = false;
      if (watchId !== null && navigator.geolocation && navigator.geolocation.clearWatch) {
        navigator.geolocation.clearWatch(watchId);
      }
      watchId = null;
      latest = null;
      latestMapFix = null;
      firstFix = true;
      if (marker) { marker.remove(); marker = null; markerRing = null; }
      if (diagramLayer) diagramLayer.replaceChildren();
      coordinates.hidden = true;
      nearestBox.hidden = true;
      nearestLink.hidden = true;
      startButton.disabled = false;
      stopButton.disabled = true;
      centerButton.disabled = true;
      setStatus(message || t.stopped, message && message !== t.stopped ? 'error' : 'normal');
    }
    function onError(error, token) {
      if (!active || token !== generation) return;
      if (error && error.code === 1) stop(t.denied);
      else if (error && error.code === 2) setStatus(t.unavailable, 'error');
      else if (error && error.code === 3) setStatus(t.timeout);
      else setStatus(t.unavailable, 'error');
    }
    function start() {
      if (active) return;
      if (!navigator.geolocation || !navigator.geolocation.watchPosition) {
        setStatus(t.unsupported, 'error');
        return;
      }
      if (window.isSecureContext === false) {
        setStatus(t.https, 'error');
        return;
      }
      active = true;
      firstFix = true;
      const token = ++generation;
      startButton.disabled = true;
      stopButton.disabled = false;
      centerButton.disabled = true;
      setStatus(t.starting);
      try {
        watchId = navigator.geolocation.watchPosition(
          (position) => onPosition(position, token),
          (error) => onError(error, token),
          { enableHighAccuracy: true, maximumAge: 0, timeout: 20000 }
        );
      } catch (e) {
        stop(t.unavailable);
      }
    }
    function recenter() {
      if (!latest) return;
      if (map) map.easeTo({ center:[latest.lon, latest.lat], zoom:Math.max(17, map.getZoom()) });
      else if (diagram) {
        diagram.scrollIntoView({ behavior:'smooth', block:'nearest' });
        if (!drawDiagram(latest.lat, latest.lon, latest.accuracy)) setStatus(t.outside);
      }
    }
    function onMapReady(event) {
      if (!event.detail || !event.detail.map) return;
      map = event.detail.map;
      if (latest) drawMap(latest.lat, latest.lon, latest.accuracy);
    }
    startButton.addEventListener('click', start);
    stopButton.addEventListener('click', () => stop());
    centerButton.addEventListener('click', recenter);
    document.addEventListener('sgu:map-ready', onMapReady);
    window.addEventListener('pagehide', () => { if (active) stop(); });
    if (map && latest) drawMap(latest.lat, latest.lon, latest.accuracy);
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
