SUMMARY = "Activate staged RAUC updates at a quiet hour"
DESCRIPTION = "rauc-hawkbit-updater installs with post_update_reboot = false so an \
OTA never interrupts pool equipment supervision mid-cycle. This timer reboots into \
the newly installed slot at 03:00 local time instead, but only when an update is \
actually staged and the target slot is marked good."
LICENSE = "CLOSED"

SRC_URI = "file://rauc-activate \
           file://rauc-activate.service \
           file://rauc-activate.timer"

S = "${UNPACKDIR}"

inherit systemd
SYSTEMD_SERVICE:${PN} = "rauc-activate.timer"
# The service is triggered by the timer; do not also enable it directly or it
# would fire once at every boot.
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

# Only the .timer is listed in SYSTEMD_SERVICE (so only it gets enabled), which
# means systemd.bbclass adds the timer to FILES but not the service. The service
# is timer-triggered and deliberately has no [Install] section, so package it
# explicitly rather than enabling it.
FILES:${PN} += "${systemd_system_unitdir}/rauc-activate.service"

RDEPENDS:${PN} = "rauc u-boot-fw-utils"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/rauc-activate ${D}${bindir}/rauc-activate

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/rauc-activate.service ${D}${systemd_system_unitdir}/
    install -m 0644 ${UNPACKDIR}/rauc-activate.timer   ${D}${systemd_system_unitdir}/
}

COMPATIBLE_MACHINE = "sz3568"
