
/* Saigon Audio Guide - GPS thuc va GPS mo phong doc lap. */
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

    if (!startButton || !stopButton || !centerButton ||
        !status || !pointsBox) return;

    const language = (
      document.documentElement.lang || 'vi'
    ).toLowerCase().slice(0, 2);

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
        coords: 'Vị trí thật',
        accuracy: 'Độ chính xác ước tính',
        nearby: 'POI gần nhất',
        within: 'Bạn đang trong bán kính kích hoạt',
        notWithin: 'Chưa vào bán kính kích hoạt POI',
        uncertainty: 'Tín hiệu GPS chưa đủ chính xác để xác nhận điểm dừng.',
        meters: 'm',
        km: 'km'
      },
      en: {
        starting: 'Requesting location permission and GPS fix…',
        active: 'Live GPS is updating your location.',
        stopped: 'GPS stopped. Your location is not saved to the server.',
        unsupported: 'This browser does not support location services.',
        https: 'Live GPS requires HTTPS on phones. Use an HTTPS address (or localhost on your computer).',
        denied: 'Location access was denied. Enable it in your browser settings and retry.',
        unavailable: 'Location unavailable. Turn on GPS, move outdoors, and retry.',
        timeout: 'GPS is taking longer than expected. Still waiting for a fix…',
        invalid: 'The browser supplied invalid coordinates.',
        outside: 'Your position is outside this coordinate diagram. Try the online map.',
        coords: 'Live position',
        accuracy: 'Estimated accuracy',
        nearby: 'Nearest POI',
        within: 'Within activation radius',
        notWithin: 'Outside the POI activation radius',
        uncertainty: 'GPS accuracy is too low to confirm this stop.',
        meters: 'm',
        km: 'km'
      },
      zh: {
        starting: '正在请求定位权限和 GPS 信号…',
        active: '实时 GPS 正在更新位置。',
        stopped: '已停止 GPS，位置不会上传到服务器。',
        unsupported: '此浏览器不支持定位。',
        https: '手机实时定位需要 HTTPS，请使用 HTTPS 地址访问。',
        denied: '定位权限已拒绝，请在浏览器设置中开启后重试。',
        unavailable: '无法获取位置，请开启手机定位并到开阔处重试。',
        timeout: 'GPS 响应较慢，正在继续等待…',
        invalid: '浏览器返回了无效坐标。',
        outside: '当前位置超出坐标图范围，请切换到在线地图。',
        coords: '实时位置',
        accuracy: '估计精度',
        nearby: '最近地点',
        within: '位于触发范围内',
        notWithin: '尚未进入触发范围',
        uncertainty: 'GPS 精度不足以确认已到达。',
        meters: '米',
        km: '公里'
      },
      ko: {
        starting: '위치 권한과 GPS 신호를 확인하는 중…',
        active: '실시간 GPS 위치를 업데이트하고 있습니다.',
        stopped: 'GPS가 중지되었습니다. 위치는 서버에 저장되지 않습니다.',
        unsupported: '이 브라우저는 위치 서비스를 지원하지 않습니다.',
        https: '휴대전화 GPS는 HTTPS 주소에서만 사용할 수 있습니다.',
        denied: '위치 권한이 거부되었습니다. 브라우저 설정에서 허용 후 다시 시도하세요.',
        unavailable: '위치를 가져올 수 없습니다. 위치 서비스를 켜고 야외에서 다시 시도하세요.',
        timeout: 'GPS 응답이 느립니다. 계속 위치를 기다리는 중…',
        invalid: '브라우저가 올바르지 않은 좌표를 반환했습니다.',
        outside: '현재 위치가 좌표 지도 범위 밖에 있습니다. 온라인 지도를 이용하세요.',
        coords: '현재 위치',
        accuracy: '예상 정확도',
        nearby: '가장 가까운 장소',
        within: '활성화 반경 안에 있습니다',
        notWithin: '활성화 반경 밖에 있습니다',
        uncertainty: 'GPS 정확도가 낮아 도착 여부를 확인할 수 없습니다.',
        meters: 'm',
        km: 'km'
      }
    };

    const t = messages[language] || messages.vi;

    const points = Array.from(
      pointsBox.querySelectorAll('.sgu-gps-poi')
    ).map((element) => ({
      id: element.dataset.id,
      name: element.dataset.name,
      lat: Number(element.dataset.lat),
      lon: Number(element.dataset.lon),
      radius: Number(element.dataset.radius)
    })).filter((point) =>
      Number.isFinite(point.lat) &&
      Number.isFinite(point.lon)
    );

    let watchId = null;
    let active = false;
    let generation = 0;
    let latest = null;
    let map = window.sguPoiMap || null;
    let marker = null;
    let markerRing = null;
    let latestMapFix = null;
    let firstFix = true;

    const autoButton = $('sgu-auto-audio-toggle');
    const autoStatus = $('sgu-auto-audio-status');
    const autoPlayer = $('sgu-auto-audio-player');

    let autoEnabled = false;

    const playedPois = new Set();
    const failedPois = new Map();

    let narrationRequest = null;
    let narrationGeneration = 0;

    // Vi tri mo phong doc lap voi GPS thuc va POI dang xem.
    let simulatedPosition = null;
    let simulatedMarker = null;
    let simulatedDistanceBox = null;

    const simulationForm = document.querySelector(
      'form[action="/simulate"]'
    );

    function setStatus(text, state) {
      status.textContent = text;
      status.dataset.state = state || 'normal';
    }

    function formatDistance(meters) {
      return meters < 1000
        ? Math.round(meters) + ' ' + t.meters
        : (meters / 1000).toFixed(2) + ' ' + t.km;
    }

    function haversine(lat, lon, otherLat, otherLon) {
      const radians = (deg) => deg * Math.PI / 180;

      const dLat = radians(otherLat - lat);
      const dLon = radians(otherLon - lon);

      const a =
        Math.sin(dLat / 2) ** 2 +
        Math.cos(radians(lat)) *
        Math.cos(radians(otherLat)) *
        Math.sin(dLon / 2) ** 2;

      return 6371000 * 2 * Math.atan2(
        Math.sqrt(a),
        Math.sqrt(Math.max(0, 1 - a))
      );
    }

    // Tu dong phat MP3 khi GPS nam trong ban kinh POI.
    async function tryAutoNarration(point, distance, accuracy) {
      if (
        !autoEnabled ||
        !autoPlayer ||
        !point ||
        distance > point.radius
      ) return;

      if (
        !Number.isFinite(accuracy) ||
        accuracy < 0 ||
        accuracy > point.radius
      ) {
        if (autoStatus) {
          autoStatus.textContent =
            'Đang chờ GPS chính xác hơn.';
        }
        return;
      }

      const key = language + ':' + point.id;

      if (
        playedPois.has(key) ||
        narrationRequest === key
      ) return;

      if (
        Date.now() - (failedPois.get(key) || 0) < 30000
      ) return;

      const token = narrationGeneration;
      narrationRequest = key;

      try {
        if (autoStatus) {
          autoStatus.textContent =
            'Đang tải thuyết minh: ' + point.name;
        }

        // Tai trang chi tiet de lay MP3 giong che do thu cong.
        const detailUrl =
          '/place?id=' + encodeURIComponent(point.id) +
          '&lang=' + encodeURIComponent(language);

        const response = await fetch(detailUrl, {
          credentials: 'same-origin',
          cache: 'no-store'
        });

        if (!response.ok) {
          throw new Error(
            'Trang thuyết minh HTTP ' + response.status
          );
        }

        const html = await response.text();

        const detail = new DOMParser().parseFromString(
          html,
          'text/html'
        );

        const manualPlayer = detail.querySelector(
          'audio[data-visitor-audio="1"]'
        );

        if (!manualPlayer) {
          throw new Error(
            'POI chưa có MP3 công khai cho ngôn ngữ này.'
          );
        }

        const audioSrc = manualPlayer.getAttribute('src');
        const audioId = manualPlayer.dataset.audioId;

        if (
          !audioSrc ||
          !/^[1-9]\d*$/.test(String(audioId || ''))
        ) {
          throw new Error(
            'Thiếu URL hoặc ID MP3 hợp lệ.'
          );
        }

        // Khong phat neu nguoi dung da tat che do.
        if (
          !autoEnabled ||
          token !== narrationGeneration
        ) return;

        autoPlayer.pause();
        autoPlayer.dataset.audioId = String(audioId);

        autoPlayer.src = new URL(
          audioSrc,
          window.location.origin
        ).href;

        autoPlayer.hidden = false;
        autoPlayer.load();

        // Chi danh dau da tai sau khi gan MP3.
        playedPois.add(key);

        try {
          await autoPlayer.play();

          if (autoStatus) {
            autoStatus.textContent =
              'Đang thuyết minh: ' + point.name;
          }
        } catch (playError) {
          if (autoStatus) {
            autoStatus.textContent =
              'Đã tải MP3 của ' + point.name +
              '. Nhấn Play nếu trình duyệt chặn tự phát.';
          }
        }
      } catch (error) {
        failedPois.set(key, Date.now());

        console.warn(
          'GPS auto narration:',
          error
        );

        if (autoStatus) {
          autoStatus.textContent =
            'Lỗi tải MP3: ' + error.message;
        }
      } finally {
        if (narrationRequest === key) {
          narrationRequest = null;
        }
      }
    }

    function showClosest(lat, lon, accuracy) {
      if (
        !points.length ||
        !nearestBox ||
        !nearestText ||
        !nearestLink
      ) return;

      let best = null;

      for (const point of points) {
        const distance = haversine(
          lat, lon, point.lat, point.lon
        );

        if (!best || distance < best.distance) {
          best = { point, distance };
        }
      }

      if (!best) return;

      const { point, distance } = best;

      const inside = distance <= point.radius;

      const uncertain =
        inside &&
        Number.isFinite(accuracy) &&
        accuracy > point.radius;

      nearestText.textContent =
        t.nearby + ': ' +
        point.name + ' (' +
        formatDistance(distance) + '). ' +
        (inside ? t.within : t.notWithin) +
        ' (' + formatDistance(point.radius) + ').' +
        (uncertain ? ' ' + t.uncertainty : '');

      nearestBox.hidden = false;
      nearestLink.hidden = !inside;

      if (inside) {
        nearestLink.href =
          '/map?id=' + encodeURIComponent(point.id);
      }

      if (inside && !uncertain) {
        void tryAutoNarration(
          point,
          distance,
          accuracy
        );
      }
    }

    function drawDiagram(lat, lon, accuracy) {
      if (!diagram || !diagramLayer) return false;

      const d = diagram.dataset;

      const minLat = Number(d.minLat);
      const maxLat = Number(d.maxLat);
      const minLon = Number(d.minLon);
      const maxLon = Number(d.maxLon);

      if (!(maxLat > minLat && maxLon > minLon)) {
        return false;
      }

      const x =
        70 +
        (lon - minLon) / (maxLon - minLon) * 620;

      const y =
        390 -
        (lat - minLat) / (maxLat - minLat) * 290;

      diagramLayer.replaceChildren();

      if (
        !Number.isFinite(x) ||
        !Number.isFinite(y) ||
        x < 0 ||
        x > 760 ||
        y < 0 ||
        y > 460
      ) return false;

      const ns = 'http://www.w3.org/2000/svg';

      if (
        Number.isFinite(accuracy) &&
        accuracy > 0
      ) {
        const ellipse = document.createElementNS(
          ns,
          'ellipse'
        );

        ellipse.setAttribute(
          'class',
          'sgu-gps-circle'
        );

        ellipse.setAttribute('cx', String(x));
        ellipse.setAttribute('cy', String(y));

        ellipse.setAttribute(
          'rx',
          String(
            accuracy /
            (
              111320 *
              Math.max(
                .01,
                Math.cos(lat * Math.PI / 180)
              )
            ) *
            620 / (maxLon - minLon)
          )
        );

        ellipse.setAttribute(
          'ry',
          String(
            accuracy /
            111320 *
            290 / (maxLat - minLat)
          )
        );

        diagramLayer.appendChild(ellipse);
      }

      const dot = document.createElementNS(
        ns,
        'circle'
      );

      dot.setAttribute(
        'class',
        'sgu-gps-position'
      );

      dot.setAttribute('cx', String(x));
      dot.setAttribute('cy', String(y));
      dot.setAttribute('r', '8');

      diagramLayer.appendChild(dot);

      return true;
    }

    function accuracyPixels() {
      if (
        !map ||
        !latestMapFix ||
        !markerRing
      ) return;

      const { lat, lon, accuracy } = latestMapFix;

      const longitudeDelta = Math.min(
        1,
        accuracy /
        (
          111320 *
          Math.max(
            .01,
            Math.cos(lat * Math.PI / 180)
          )
        )
      );

      const center = map.project([lon, lat]);
      const east = map.project([
        lon + longitudeDelta,
        lat
      ]);

      const radius = Math.abs(
        east.x - center.x
      );

      if (Number.isFinite(radius)) {
        markerRing.style.width =
          Math.max(8, radius * 2) + 'px';

        markerRing.style.height =
          Math.max(8, radius * 2) + 'px';
      }
    }

    function drawMap(lat, lon, accuracy) {
      if (!map || !window.maptilersdk) return;

      latestMapFix = {
        lat,
        lon,
        accuracy
      };

      if (!marker) {
        const element = document.createElement('div');
        element.className = 'sgu-gps-marker';

        markerRing = document.createElement('div');
        markerRing.className = 'sgu-gps-accuracy';

        const dot = document.createElement('div');
        dot.className = 'sgu-gps-dot';

        element.append(markerRing, dot);

        marker = new window.maptilersdk.Marker({
          element,
          anchor: 'center'
        }).setLngLat([lon, lat]).addTo(map);

        map.on('zoom', accuracyPixels);
      } else {
        marker.setLngLat([lon, lat]);
      }

      accuracyPixels();

      if (firstFix) {
        firstFix = false;

        if (!simulatedPosition) {
          map.easeTo({
            center: [lon, lat],
            zoom: Math.max(
              17,
              map.getZoom()
            )
          });
        }
      }
    }

    function onPosition(position, token) {
      if (
        !active ||
        token !== generation
      ) return;

      const {
        latitude: lat,
        longitude: lon,
        accuracy
      } = position.coords || {};

      if (
        !Number.isFinite(lat) ||
        !Number.isFinite(lon) ||
        lat < -90 ||
        lat > 90 ||
        lon < -180 ||
        lon > 180
      ) {
        setStatus(t.invalid, 'error');
        return;
      }

      latest = {
        lat,
        lon,
        accuracy
      };

      const shownAccuracy =
        Number.isFinite(accuracy) &&
        accuracy >= 0
          ? formatDistance(accuracy)
          : '—';

      coordinates.textContent =
        t.coords + ': ' +
        lat.toFixed(6) + ', ' +
        lon.toFixed(6) +
        ' · ' +
        t.accuracy + ': ±' +
        shownAccuracy;

      coordinates.hidden = false;
      centerButton.disabled = false;

      const onDiagram = drawDiagram(
        lat, lon, accuracy
      );

      drawMap(lat, lon, accuracy);

      setStatus(
        diagram && !onDiagram
          ? t.active + ' ' + t.outside
          : t.active
      );

      // GPS that khong ghi de ket qua GPS mo phong.
      if (!simulatedPosition) {
        showClosest(lat, lon, accuracy);
      }
    }

    function stop(message) {
      generation++;
      active = false;

      if (
        watchId !== null &&
        navigator.geolocation &&
        navigator.geolocation.clearWatch
      ) {
        navigator.geolocation.clearWatch(watchId);
      }

      watchId = null;
      latest = null;
      latestMapFix = null;
      firstFix = true;

      if (marker) {
        marker.remove();
        marker = null;
        markerRing = null;
      }

      if (diagramLayer) {
        diagramLayer.replaceChildren();
      }

      coordinates.hidden = true;

      nearestBox.hidden = Boolean(
        !simulatedPosition
      );

      nearestLink.hidden = true;

      if (simulatedPosition) {
        showClosest(
          simulatedPosition.lat,
          simulatedPosition.lon,
          0
        );
      }

      startButton.disabled = false;
      stopButton.disabled = true;
      centerButton.disabled = true;

      setStatus(
        message || t.stopped,
        message && message !== t.stopped
          ? 'error'
          : 'normal'
      );
    }

    function onError(error, token) {
      if (
        !active ||
        token !== generation
      ) return;

      if (error && error.code === 1) {
        stop(t.denied);
      } else if (error && error.code === 2) {
        setStatus(t.unavailable, 'error');
      } else if (error && error.code === 3) {
        setStatus(t.timeout);
      } else {
        setStatus(t.unavailable, 'error');
      }
    }

    function start() {
      if (active) return;

      if (
        !navigator.geolocation ||
        !navigator.geolocation.watchPosition
      ) {
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
          {
            enableHighAccuracy: true,
            maximumAge: 0,
            timeout: 20000
          }
        );
      } catch (e) {
        stop(t.unavailable);
      }
    }

    function recenter() {
      if (!latest) return;

      if (map) {
        map.easeTo({
          center: [
            latest.lon,
            latest.lat
          ],
          zoom: Math.max(
            17,
            map.getZoom()
          )
        });
      } else if (diagram) {
        diagram.scrollIntoView({
          behavior: 'smooth',
          block: 'nearest'
        });

        if (
          !drawDiagram(
            latest.lat,
            latest.lon,
            latest.accuracy
          )
        ) {
          setStatus(t.outside);
        }
      }
    }

    function onMapReady(event) {
      if (
        !event.detail ||
        !event.detail.map
      ) return;

      map = event.detail.map;

      if (latest) {
        drawMap(
          latest.lat,
          latest.lon,
          latest.accuracy
        );
      }

      if (simulatedPosition) {
        drawSimulatedPosition(
          simulatedPosition.lat,
          simulatedPosition.lon
        );
      }
    }

    // Chon POI khong tai lai trang.
    function selectPoi(
      poiId,
      updateHistory = true
    ) {
      const point = points.find(
        (item) =>
          String(item.id) === String(poiId)
      );

      if (!point) return false;

      document.querySelectorAll(
        '.map-list a[href*="/map?id="]'
      ).forEach((link) => {
        try {
          const id = new URL(
            link.href,
            window.location.href
          ).searchParams.get('id');

          link.classList.toggle(
            'active',
            id === String(point.id)
          );
        } catch (_) {}
      });

      // Cap nhat marker POI dang chon.
      document.querySelectorAll(
        '.sgu-poi-marker'
      ).forEach((el) => {
        el.classList.toggle(
          'selected',
          el.dataset.poiId === String(point.id)
        );
      });

      // Cap nhat so do neu co.
      if (diagram) {
        diagram.querySelectorAll(
          'a[href*="/map?id="]'
        ).forEach((link) => {
          const selected = new URL(
            link.getAttribute('href'),
            window.location.href
          ).searchParams.get('id') ===
          String(point.id);

          const pin = link.querySelector(
            '.map-pin'
          );

          if (pin) {
            pin.classList.toggle(
              'active',
              selected
            );
          }

          const halo = link.querySelector(
            'circle[data-selection-halo="true"]'
          );

          if (halo) halo.remove();

          if (selected && pin) {
            const ring =
              document.createElementNS(
                'http://www.w3.org/2000/svg',
                'circle'
              );

            ring.setAttribute(
              'data-selection-halo',
              'true'
            );

            ring.setAttribute(
              'cx',
              pin.getAttribute('cx')
            );

            ring.setAttribute(
              'cy',
              pin.getAttribute('cy')
            );

            ring.setAttribute('r', '33');
            ring.setAttribute(
              'fill',
              '#149aea22'
            );

            link.insertBefore(
              ring,
              pin
            );
          }
        });
      }

      // Cap nhat POI va nut nghe thu cong.
      const sidebar = document.querySelector(
        '.map-grid aside.panel'
      );

      if (sidebar) {
        const selectedName =
          sidebar.querySelector(
            '.tip strong'
          );

        if (selectedName) {
          selectedName.textContent =
            point.name;

          if (
            selectedName.parentNode &&
            selectedName.parentNode.firstChild &&
            selectedName.parentNode.firstChild.nodeType === 3
          ) {
            selectedName.parentNode.firstChild.textContent =
              'Đang xem: ';
          }
        }

        const narrationLink =
          sidebar.querySelector(
            '.form-actions a[href*="/place?id="]'
          );

        if (narrationLink) {
          narrationLink.href =
            '/place?id=' +
            encodeURIComponent(point.id);
        }
      }

      // Khong thay doi toa do mo phong khi chi xem POI.

      if (
        updateHistory &&
        window.history &&
        window.history.pushState
      ) {
        const url = new URL(
          window.location.href
        );

        url.searchParams.set(
          'id',
          String(point.id)
        );

        window.history.pushState(
          window.history.state,
          '',
          url.pathname +
          url.search +
          url.hash
        );
      }

      if (
        map &&
        typeof map.easeTo === 'function'
      ) {
        map.easeTo({
          center: [
            point.lon,
            point.lat
          ],
          duration: 450
        });
      }

      return true;
    }

    window.sguSelectPoi = selectPoi;

    // Chon POI tu danh sach hoac so do.
    document.addEventListener(
      'click',
      (event) => {
        if (
          event.defaultPrevented ||
          event.button > 0 ||
          event.ctrlKey ||
          event.metaKey ||
          event.shiftKey ||
          event.altKey
        ) return;

        const link =
          event.target.closest &&
          event.target.closest(
            '.map-list a[href*="/map?id="], ' +
            '#sgu-coordinate-diagram a[href*="/map?id="]'
          );

        if (
          !link ||
          link.target === '_blank' ||
          link.hasAttribute('download')
        ) return;

        const id = new URL(
          link.href,
          window.location.href
        ).searchParams.get('id');

        if (id && selectPoi(id)) {
          event.preventDefault();
        }
      }
    );

    window.addEventListener(
      'popstate',
      () => {
        const id = new URL(
          window.location.href
        ).searchParams.get('id');

        if (id) {
          selectPoi(id, false);
        }
      }
    );

    // Bat/tat thuyet minh tu dong.
    if (autoButton && autoPlayer) {
      autoButton.addEventListener(
        'click',
        () => {
          autoEnabled = !autoEnabled;

          if (autoEnabled) {
            autoButton.textContent =
              'Tắt thuyết minh tự động';

            if (autoStatus) {
              autoStatus.textContent =
                'Đã bật. Đang chờ GPS nhận diện POI.';
            }

            playedPois.clear();

            if (simulatedPosition) {
              showClosest(
                simulatedPosition.lat,
                simulatedPosition.lon,
                0
              );
            } else if (latest) {
              showClosest(
                latest.lat,
                latest.lon,
                latest.accuracy
              );
            }
          } else {
            narrationGeneration++;

            autoButton.textContent =
              'Bật thuyết minh tự động';

            autoPlayer.pause();

            if (autoStatus) {
              autoStatus.textContent =
                'Đã tắt thuyết minh tự động.';
            }
          }
        }
      );
    }

    // Xem POI gan nhat ma khong tai lai trang.
    if (nearestLink) {
      nearestLink.addEventListener(
        'click',
        function (event) {
          const id = new URL(
            this.href,
            window.location.href
          ).searchParams.get('id');

          if (
            id &&
            typeof window.sguSelectPoi === 'function'
          ) {
            if (window.sguSelectPoi(id)) {
              event.preventDefault();
            }
          }
        }
      );
    }

    // Tinh khoang cach tu GPS mo phong den tat ca POI.
    function renderSimulationDistances(lat, lon) {
      if (!simulationForm) return;

      if (!simulatedDistanceBox) {
        simulatedDistanceBox =
          document.createElement('div');

        simulatedDistanceBox.id =
          'sgu-simulation-distances';

        simulatedDistanceBox.setAttribute(
          'aria-live',
          'polite'
        );

        simulationForm.insertAdjacentElement(
          'afterend',
          simulatedDistanceBox
        );
      }

      const heading =
        document.createElement('h4');

      heading.textContent =
        'Khoảng cách từ GPS mô phỏng đến các POI';

      const summary =
        document.createElement('p');

      summary.textContent =
        'Vị trí mô phỏng: ' +
        lat.toFixed(6) + ', ' +
        lon.toFixed(6);

      const list =
        document.createElement('div');

      const distances = points.map(
        (p) => ({
          point: p,
          distance: haversine(
            lat,
            lon,
            p.lat,
            p.lon
          )
        })
      ).sort(
        (a, b) =>
          a.distance - b.distance
      );

      distances.forEach(
        ({ point, distance }) => {
          const row =
            document.createElement('p');

          row.textContent =
            point.name + ': ' +
            formatDistance(distance) +
            (
              distance <= point.radius
                ? ' (trong bán kính ' +
                  formatDistance(point.radius) +
                  ')'
                : ''
            );

          list.appendChild(row);
        }
      );

      simulatedDistanceBox.replaceChildren(
        heading,
        summary,
        list
      );
    }

    // Marker rieng cho GPS mo phong.
    function drawSimulatedPosition(lat, lon) {
      if (
        !map ||
        !window.maptilersdk
      ) return;

      if (!simulatedMarker) {
        const el =
          document.createElement('div');

        el.title = 'GPS mô phỏng';

        el.style.cssText =
          'width:18px;' +
          'height:18px;' +
          'border-radius:50%;' +
          'background:#f59e0b;' +
          'border:3px solid white;' +
          'box-shadow:0 0 0 3px #b45309;';

        simulatedMarker =
          new window.maptilersdk.Marker({
            element: el,
            anchor: 'center'
          }).setLngLat(
            [lon, lat]
          ).addTo(map);
      } else {
        simulatedMarker.setLngLat(
          [lon, lat]
        );
      }
    }

    // Ap dung toa do GPS mo phong.
    function applySimulation(lat, lon) {
      simulatedPosition = {
        lat,
        lon
      };

      if (autoPlayer) {
        autoPlayer.pause();
      }

      narrationGeneration++;

      renderSimulationDistances(
        lat,
        lon
      );

      drawSimulatedPosition(
        lat,
        lon
      );

      if (
        map &&
        typeof map.easeTo === 'function'
      ) {
        map.easeTo({
          center: [lon, lat],
          zoom: 18
        });
      }

      // Sai so mo phong = 0.
      showClosest(lat, lon, 0);
    }

    // Dung lai form mo phong co san trong VisitorView.java.
    if (simulationForm) {
      simulationForm.addEventListener(
        'submit',
        (event) => {
          const latInput =
            simulationForm.querySelector(
              'input[name="latitude"]'
            );

          const lonInput =
            simulationForm.querySelector(
              'input[name="longitude"]'
            );

          if (!latInput || !lonInput) {
            return;
          }

          const lat = Number(
            latInput.value
          );

          const lon = Number(
            lonInput.value
          );

          if (
            !latInput.value.trim() ||
            !lonInput.value.trim() ||
            !Number.isFinite(lat) ||
            !Number.isFinite(lon) ||
            lat < -90 ||
            lat > 90 ||
            lon < -180 ||
            lon > 180
          ) {
            event.preventDefault();

            alert(
              'Tọa độ mô phỏng không hợp lệ.'
            );

            return;
          }

          // Khong tai lai trang khi ap dung toa do.
          event.preventDefault();

          applySimulation(lat, lon);
        }
      );

      // Nut thoat che do mo phong.
      const exitButton =
        document.createElement('button');

      exitButton.type = 'button';
      exitButton.className =
        'btn btn-light';

      exitButton.textContent =
        'Thoát GPS mô phỏng';

      const actions =
        simulationForm.querySelector(
          '.form-actions'
        ) || simulationForm;

      actions.appendChild(
        exitButton
      );

      exitButton.addEventListener(
        'click',
        () => {
          simulatedPosition = null;

          if (autoPlayer) {
            autoPlayer.pause();
          }

          narrationGeneration++;

          if (simulatedMarker) {
            simulatedMarker.remove();
            simulatedMarker = null;
          }

          if (simulatedDistanceBox) {
            simulatedDistanceBox.replaceChildren();
          }

          if (latest) {
            showClosest(
              latest.lat,
              latest.lon,
              latest.accuracy
            );
          } else if (nearestBox) {
            nearestBox.hidden = true;

            if (nearestLink) {
              nearestLink.hidden = true;
            }
          }
        }
      );
    }

    startButton.addEventListener(
      'click',
      start
    );

    stopButton.addEventListener(
      'click',
      () => stop()
    );

    centerButton.addEventListener(
      'click',
      recenter
    );

    document.addEventListener(
      'sgu:map-ready',
      onMapReady
    );

    window.addEventListener(
      'pagehide',
      () => {
        if (active) stop();
      }
    );

    if (map && latest) {
      drawMap(
        latest.lat,
        latest.lon,
        latest.accuracy
      );
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener(
      'DOMContentLoaded',
      init
    );
  } else {
    init();
  }
})();
