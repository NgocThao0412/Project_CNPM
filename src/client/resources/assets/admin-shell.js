
"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const body = document.body;
    const button = document.querySelector(
        ".admin-menu-toggle"
    );
    const sidebar = document.getElementById(
        "admin-sidebar"
    );

    if (!button || !sidebar) return;

    const mobile = window.matchMedia(
        "(max-width: 900px)"
    );

    function isExpanded() {
        if (mobile.matches) {
            return body.classList.contains(
                "admin-menu-open"
            );
        }

        return !body.classList.contains(
            "admin-menu-collapsed"
        );
    }

    function updateButton() {
        button.setAttribute(
            "aria-expanded",
            String(isExpanded())
        );
    }

    function closeMobileMenu() {
        body.classList.remove("admin-menu-open");
        updateButton();
    }

    button.addEventListener("click", () => {
        if (mobile.matches) {
            body.classList.toggle("admin-menu-open");
        } else {
            body.classList.toggle(
                "admin-menu-collapsed"
            );
        }

        updateButton();
    });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            closeMobileMenu();
        }
    });

    document.addEventListener("click", event => {
        if (!mobile.matches) return;

        if (!body.classList.contains(
            "admin-menu-open"
        )) return;

        if (
            !sidebar.contains(event.target) &&
            !button.contains(event.target)
        ) {
            closeMobileMenu();
        }
    });

    sidebar.querySelectorAll("a").forEach(link => {
        link.addEventListener(
            "click",
            closeMobileMenu
        );
    });

    function resetOnResize() {
        body.classList.remove(
            "admin-menu-open",
            "admin-menu-collapsed"
        );

        updateButton();
    }

    if (mobile.addEventListener) {
        mobile.addEventListener(
            "change",
            resetOnResize
        );
    } else {
        mobile.addListener(resetOnResize);
    }

    updateButton();
});
