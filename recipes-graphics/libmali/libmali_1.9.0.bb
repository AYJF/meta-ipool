SUMMARY = "Rockchip Mali GPU userspace driver (proprietary libmali blob)"
DESCRIPTION = "Prebuilt ARM Mali GPU userspace driver (libmali) for the RK3568 \
Mali-G52 (Bifrost, r2p0). Provides EGL / GLESv1 / GLESv2 / GBM (+OpenCL) on the \
hardware GPU. Installed in its OWN dir (${libdir}/mali) so it does NOT conflict \
with Mesa's packages, and forced to win at runtime via LD_LIBRARY_PATH (which is \
searched before /usr/lib -- unlike ld.so.conf.d, which lost to Mesa). Mesa still \
builds/installs (Vulkan, software) but Weston/Qt load libmali's GLES/EGL/GBM. \
Wayland-gbm variant, since our compositor stack is Weston / Qt-Wayland."
HOMEPAGE = "https://github.com/JeffyCN/mirrors/tree/libmali"

# Proprietary blob (ARM/Rockchip EULA in the repo). CLOSED skips the FOSS checks.
LICENSE = "CLOSED"

SRC_URI = "git://github.com/JeffyCN/mirrors.git;branch=libmali;protocol=https;nobranch=1"
SRCREV = "44bcc8e3ed82ee3ff10568d56c30931cda577387"
# (whinlatter oe-core sets S for git fetches automatically; do not assign it.)

# RK3568 = Mali-G52 (Bifrost) r2p0. Wayland+GBM variant for Weston.
MALI_BLOB   = "libmali-bifrost-g52-g2p0-wayland-gbm"
# The repo stores blobs under Debian-triplet dirs (lib/aarch64-linux-gnu/).
MALI_SUBDIR = "aarch64-linux-gnu"

COMPATIBLE_MACHINE = "sz3568"
# Prebuilt aarch64 shared object -- keep the closed blob untouched.
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_SYSROOT_STRIP = "1"
INSANE_SKIP:${PN} += "ldflags textrel already-stripped dev-so libdir arch staticdev"

MALIDIR = "${libdir}/mali"

do_install() {
    install -d ${D}${MALIDIR}

    # The blob, as libmali.so.1(.9.0) (matches the vendor soname).
    install -m 0755 ${S}/lib/${MALI_SUBDIR}/${MALI_BLOB}.so \
        ${D}${MALIDIR}/libmali.so.1.9.0
    ln -sf libmali.so.1.9.0 ${D}${MALIDIR}/libmali.so.1
    ln -sf libmali.so.1     ${D}${MALIDIR}/libmali.so

    # EGL / GLES / GBM / CL / wayland-egl entrypoints -> the blob.
    ln -sf libmali.so.1 ${D}${MALIDIR}/libEGL.so.1
    ln -sf libmali.so.1 ${D}${MALIDIR}/libGLESv2.so.2
    ln -sf libmali.so.1 ${D}${MALIDIR}/libGLESv1_CM.so.1
    ln -sf libmali.so.1 ${D}${MALIDIR}/libgbm.so.1
    ln -sf libmali.so.1 ${D}${MALIDIR}/libOpenCL.so.1
    ln -sf libmali.so.1 ${D}${MALIDIR}/libwayland-egl.so.1

    # Force libmali ahead of Mesa for EVERY systemd service (Weston, the Qt demo
    # launcher, etc.) via LD_LIBRARY_PATH -- checked before /usr/lib, so libmali's
    # libEGL.so.1 / libGLESv2.so.2 / libgbm.so.1 win. (ld.so.conf.d could not beat
    # /usr/lib in OE.) Global DefaultEnvironment is simplest and appliance-safe.
    install -d ${D}${sysconfdir}/systemd/system.conf.d
    cat > ${D}${sysconfdir}/systemd/system.conf.d/00-libmali.conf <<EOF
[Manager]
DefaultEnvironment=LD_LIBRARY_PATH=${MALIDIR}
EOF
    # Also cover the user manager (in case the launcher runs as a user service).
    install -d ${D}${sysconfdir}/systemd/user.conf.d
    cat > ${D}${sysconfdir}/systemd/user.conf.d/00-libmali.conf <<EOF
[Manager]
DefaultEnvironment=LD_LIBRARY_PATH=${MALIDIR}
EOF
}

FILES:${PN} = "${MALIDIR} ${sysconfdir}/systemd"

# The wayland-gbm blob links libdrm + libwayland-client/server at runtime.
RDEPENDS:${PN} += "libdrm wayland"
