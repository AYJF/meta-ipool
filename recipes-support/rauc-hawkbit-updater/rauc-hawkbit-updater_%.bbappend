FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# Override the upstream config.conf.example with our own (gateway-token auto-
# registration), plus a boot-time script + systemd drop-in that set a per-board
# controller id. The base recipe's do_install:append installs the example first;
# ours (parsed later) overwrites it.
SRC_URI += "file://config.conf \
            file://hawkbit-set-target-id \
            file://10-target-id.conf"

do_install:append() {
    install -m 0644 ${UNPACKDIR}/config.conf ${D}${sysconfdir}/${PN}/config.conf

    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/hawkbit-set-target-id ${D}${bindir}/hawkbit-set-target-id

    install -d ${D}${systemd_system_unitdir}/rauc-hawkbit-updater.service.d
    install -m 0644 ${UNPACKDIR}/10-target-id.conf \
        ${D}${systemd_system_unitdir}/rauc-hawkbit-updater.service.d/10-target-id.conf
}

FILES:${PN} += "${systemd_system_unitdir}/rauc-hawkbit-updater.service.d"
