SUMMARY = "iPool WiFi (RTL8723DS) bring-up diagnostic (serial console dump)"
DESCRIPTION = "Boot-time oneshot that prints the RTL8723DS driver/SDIO/wlan0 + \
wpa_supplicant + networkd state to ttyS8, so WiFi bring-up can be verified/debugged \
without an interactive shell. TEMPORARY -- remove once WiFi + SSH are confirmed."
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://wifi-diag.sh file://wifi-diag.service"

inherit systemd
SYSTEMD_SERVICE:${PN} = "wifi-diag.service"
SYSTEMD_AUTO_ENABLE = "enable"

RDEPENDS:${PN} += "systemd iw"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/wifi-diag.sh ${D}${bindir}/wifi-diag.sh
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${UNPACKDIR}/wifi-diag.service ${D}${systemd_system_unitdir}/wifi-diag.service
}

COMPATIBLE_MACHINE = "sz3568"
