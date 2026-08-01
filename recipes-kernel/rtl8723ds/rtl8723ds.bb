SUMMARY = "Realtek RTL8723DS SDIO WiFi driver (out-of-tree)"
DESCRIPTION = "Out-of-tree kernel module for the Realtek RTL8723DS SDIO WiFi chip \
on the iPool SZ3568 (the board's wifi_chip_type). The JeffyCN 6.1 kernel mirror \
trimmed the in-tree Realtek SDIO driver (only a Kconfig stub remains), so we build \
lwfinger's maintained standalone rtl8723ds against our kernel. Firmware is embedded \
in the driver source (no separate /lib/firmware needed for WiFi)."
HOMEPAGE = "https://github.com/lwfinger/rtl8723ds"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=1f6f1c0be32491a0c8d2915607a28f36"

inherit module

SRC_URI = "git://github.com/lwfinger/rtl8723ds.git;branch=master;protocol=https"
SRCREV = "52e593e8c889b68ba58bd51cbdbcad7fe71362e4"

# lwfinger's Makefile defaults to the x86 host platform; turn that off and pass the
# Yocto cross toolchain + kernel source through so it builds for arm64 generically.
EXTRA_OEMAKE = " \
    ARCH=${ARCH} \
    CROSS_COMPILE=${TARGET_PREFIX} \
    KSRC=${STAGING_KERNEL_DIR} \
    CONFIG_PLATFORM_I386_PC=n \
"

MODULE_NAME = "8723ds"

# The driver's Makefile only enables key defines inside per-platform blocks, none
# of which cover generic arm64. We select no platform, so inject them by hand:
#  - CONFIG_LITTLE_ENDIAN: without it the build errors "#error Must be LITTLE/BIG
#    Endian Host" (aarch64 is little-endian).
#  - CONFIG_IOCTL_CFG80211 + RTW_USE_CFG80211_STA_EVENT: build the driver with the
#    nl80211/cfg80211 interface instead of the Realtek wext-only one. This is
#    REQUIRED so ConnMan (via wpa_supplicant + nl80211) can manage wlan0 -- which
#    is exactly what the poolnook app's QtDeviceUtilities.NetworkSettings backend
#    uses. Without it the chip only exposes a private wext iface ConnMan ignores.
#  - -Wno-error: relax gcc-15's newer -Werror warnings this older source trips.
# Appended EXTRA_CFLAGS is picked up by kbuild when the kernel builds the module.
do_configure:prepend() {
    echo 'EXTRA_CFLAGS += -DCONFIG_LITTLE_ENDIAN -DCONFIG_IOCTL_CFG80211 -DRTW_USE_CFG80211_STA_EVENT -Wno-error' >> ${S}/Makefile
}

# Install 8723ds.ko (built in-tree by the Realtek Makefile) as an out-of-tree module.
module_do_install() {
    install -d ${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}/extra
    install -m 0644 $(find ${B} ${S} -name '${MODULE_NAME}.ko' -print -quit) \
        ${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}/extra/

    # Disable WiFi power management. With power save on (the driver's PS_MODE_MAX
    # default in the SDIO build), the chip drops broadcast frames while dozing,
    # which breaks DHCP: the client associates but never gets a lease, so the OS
    # falls back to a 169.254.x.x link-local address. rtw_power_mgnt=0
    # (PS_MODE_ACTIVE) + rtw_ips_mode=0 (IPS_NONE) keep the radio awake so DHCP
    # completes. A mains-powered always-on board doesn't care about the power cost.
    install -d ${D}${sysconfdir}/modprobe.d
    printf 'options %s rtw_power_mgnt=0 rtw_ips_mode=0\n' "${MODULE_NAME}" \
        > ${D}${sysconfdir}/modprobe.d/${MODULE_NAME}.conf
}

FILES:${PN} += "${sysconfdir}/modprobe.d/${MODULE_NAME}.conf"

# Auto-load at boot so WiFi comes up without manual modprobe.
KERNEL_MODULE_AUTOLOAD += "8723ds"

RPROVIDES:${PN} += "kernel-module-8723ds"
COMPATIBLE_MACHINE = "sz3568"
