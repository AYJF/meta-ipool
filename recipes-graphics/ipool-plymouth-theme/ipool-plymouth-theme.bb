SUMMARY = "iPool boot splash (Plymouth theme) + Boot2Qt hand-off"
DESCRIPTION = "A centered-logo Plymouth splash for the SZ3568, using the script \
plugin on the DRM renderer (VOP2->LVDS). Sets itself as the default theme and \
wires the plymouth->eglfs DRM hand-off so the logo shows from early boot until \
the Qt app draws its first frame, with no black flash."
LICENSE = "CLOSED"

SRC_URI = " \
    file://ipool.plymouth \
    file://ipool.script \
    file://logo.png \
    file://10-plymouth-handoff.conf \
    file://10-plymouth-quit-retain.conf \
"

# Plymouth theme + the app hand-off need plymouth (with the DRM renderer, enabled
# by our plymouth bbappend). qtbase brings b2qt.service that we drop into.
RDEPENDS:${PN} = "plymouth"

THEMEDIR = "${datadir}/plymouth/themes/ipool"

do_install() {
    # the theme
    install -d ${D}${THEMEDIR}
    install -m 0644 ${UNPACKDIR}/ipool.plymouth ${D}${THEMEDIR}/ipool.plymouth
    install -m 0644 ${UNPACKDIR}/ipool.script   ${D}${THEMEDIR}/ipool.script
    install -m 0644 ${UNPACKDIR}/logo.png       ${D}${THEMEDIR}/logo.png

    # Select this theme via the default.plymouth symlink. plymouth ships
    # /etc/plymouth/plymouthd.conf with the theme commented out, so it defers to
    # this symlink -- no need to ship (and clash on) our own plymouthd.conf.
    ln -sf ipool/ipool.plymouth ${D}${datadir}/plymouth/themes/default.plymouth

    # DRM hand-off: b2qt.service quits plymouth (retain-splash) right before it
    # grabs KMS; and the stock plymouth-quit retains the splash if it runs first.
    install -d ${D}${systemd_system_unitdir}/b2qt.service.d
    install -m 0644 ${UNPACKDIR}/10-plymouth-handoff.conf \
        ${D}${systemd_system_unitdir}/b2qt.service.d/10-plymouth-handoff.conf
    install -d ${D}${systemd_system_unitdir}/plymouth-quit.service.d
    install -m 0644 ${UNPACKDIR}/10-plymouth-quit-retain.conf \
        ${D}${systemd_system_unitdir}/plymouth-quit.service.d/10-plymouth-quit-retain.conf
}

FILES:${PN} = " \
    ${THEMEDIR} \
    ${datadir}/plymouth/themes/default.plymouth \
    ${systemd_system_unitdir}/b2qt.service.d/10-plymouth-handoff.conf \
    ${systemd_system_unitdir}/plymouth-quit.service.d/10-plymouth-quit-retain.conf \
"

COMPATIBLE_MACHINE = "sz3568"
