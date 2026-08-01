FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# Stop udev-extraconf's automounter (mount.sh) from grabbing the internal eMMC.
# It otherwise mounts every labeled partition to /run/media/<label> — including
# the INACTIVE RAUC slot (rootfsB), which then can't be opened for writing:
#   "Failed updating slot rootfs.1: ... rootfsB ... Device or resource busy".
# It also steals the data partition from its proper /data mount. mount.sh reads
# /etc/udev/mount.ignorelist.d/* and skips any device whose name matches; the
# entry "/dev/mmcblk0" covers the whole eMMC (rootfsA/B, data) while leaving real
# removable media (SD mmcblk1*, USB sd*) auto-mountable.
SRC_URI += "file://emmc.ignore"

do_install:append() {
    install -d ${D}${sysconfdir}/udev/mount.ignorelist.d
    install -m 0644 ${UNPACKDIR}/emmc.ignore ${D}${sysconfdir}/udev/mount.ignorelist.d/emmc.ignore
}

FILES:${PN} += "${sysconfdir}/udev/mount.ignorelist.d"
