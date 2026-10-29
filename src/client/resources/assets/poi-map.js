
document.addEventListener("DOMContentLoaded", function () {
    const container = document.getElementById("sgu-map");
    const pointsBox = document.getElementById("sgu-map-points");

    if (!container || !pointsBox) return;

    if (!window.maptilersdk) {
        console.error("MapTiler SDK chưa được tải.");
        return;
    }

    maptilersdk.config.apiKey = container.dataset.key;

    const map = new maptilersdk.Map({
        container: "sgu-map",
        style: maptilersdk.MapStyle.STREETS,
        center: [
            Number(container.dataset.lon),
            Number(container.dataset.lat)
        ],
        zoom: 18
    });

    const points = pointsBox.querySelectorAll(".sgu-map-point");

    points.forEach(function (point) {
        const name = point.dataset.name;
        const active = point.dataset.active === "true";

        const el = document.createElement("a");
        el.className = "sgu-poi-marker" + (active ? " selected" : "");
        el.href = "/map?id=" + encodeURIComponent(point.dataset.id)
                + "&basemap=osm";
        el.title = "Xem địa điểm: " + name;
        el.setAttribute("aria-label", "Xem địa điểm: " + name);

        const pin = document.createElement("span");
        pin.className = "sgu-poi-pin";
        pin.setAttribute("aria-hidden", "true");

        const label = document.createElement("span");
        label.className = "sgu-poi-label";
        label.textContent = name;

        el.append(pin, label);

        new maptilersdk.Marker({
            element: el,
            anchor: "bottom"
        })
        .setLngLat([
            Number(point.dataset.lon),
            Number(point.dataset.lat)
        ])
        .addTo(map);
    });

    map.on("error", function (event) {
        console.error("SGU map error:", event.error);
    });
});
