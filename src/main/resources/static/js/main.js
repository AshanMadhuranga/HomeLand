document.addEventListener("DOMContentLoaded", () => {
    const navbar = document.querySelector(".landhub-navbar");
    const revealItems = document.querySelectorAll(".reveal");
    const navCollapse = document.querySelector(".navbar-collapse");

    const updateNavbar = () => {
        if (!navbar) {
            return;
        }

        navbar.classList.toggle("nav-scrolled", window.scrollY > 32);
    };

    updateNavbar();
    window.addEventListener("scroll", updateNavbar, { passive: true });

    if ("IntersectionObserver" in window) {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-visible");
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.16 });

        revealItems.forEach((item) => observer.observe(item));
    } else {
        revealItems.forEach((item) => item.classList.add("is-visible"));
    }

    document.querySelectorAll('a[href^="#"]').forEach((anchor) => {
        anchor.addEventListener("click", (event) => {
            const target = document.querySelector(anchor.getAttribute("href"));

            if (!target) {
                return;
            }

            event.preventDefault();
            target.scrollIntoView({ behavior: "smooth", block: "start" });

            if (navCollapse && navCollapse.classList.contains("show")) {
                bootstrap.Collapse.getOrCreateInstance(navCollapse).hide();
            }
        });
    });
});
