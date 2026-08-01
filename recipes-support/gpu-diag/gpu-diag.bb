SUMMARY = "iPool GPU/EGL bring-up diagnostic (serial console dump)"
DESCRIPTION = "Boot-time oneshot that prints GPU/Mali/EGL state to ttyS8, so we \
can debug why Qt won't render without an interactive shell (serial RX + ethernet \
are both dead on this board). TEMPORARY -- remove once the GPU stack works."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://gpu-diag.sh file://gpu-diag.service"

inherit systemd
SYSTEMD_SERVICE:${PN} = "gpu-diag.service"
SYSTEMD_AUTO_ENABLE = "enable"

# eglinfo (mesa-demos) is handy in the dump if available.
RDEPENDS:${PN} += "systemd"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/gpu-diag.sh ${D}${bindir}/gpu-diag.sh
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/gpu-diag.service ${D}${systemd_system_unitdir}/gpu-diag.service
}

COMPATIBLE_MACHINE = "sz3568"
