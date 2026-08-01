SUMMARY = "iPool ethernet (GMAC) bring-up diagnostic (serial console dump)"
DESCRIPTION = "Boot-time oneshot that prints the GMAC clock tree + PHY/MAC state to \
ttyS8, so we can debug why gmac1 fails 'Failed to reset the dma' without an \
interactive shell (serial RX + ethernet + wifi are all down). TEMPORARY."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://eth-diag.sh file://eth-diag.service"

inherit systemd
SYSTEMD_SERVICE:${PN} = "eth-diag.service"
SYSTEMD_AUTO_ENABLE = "enable"

RDEPENDS:${PN} += "systemd"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/eth-diag.sh ${D}${bindir}/eth-diag.sh
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/eth-diag.service ${D}${systemd_system_unitdir}/eth-diag.service
}

COMPATIBLE_MACHINE = "sz3568"
