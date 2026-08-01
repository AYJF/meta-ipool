# Rockchip vendor BSP kernel (linux 6.1) for the iPool SZ3568 (RK3568), built
# on oe-core's linux-yocto framework (kernel + kernel-yocto) so the git source
# is properly checked out into the shared kernel-source workdir.
#
# PATH B: mainline linux-yocto 6.16 could not drive this board's native RK3568
# single-link LVDS panel. The Rockchip 6.1 vendor kernel + the board's vendor
# device tree + libmali is the known-working display stack; boot2qt/Qt 6.11
# userspace rides on top unchanged. Source = JeffyCN's mirror of the Rockchip
# 6.1 BSP (same tree JeffyCN/meta-rockchip's linux-rockchip_6.1.bb uses).

SUMMARY = "Rockchip vendor BSP Linux kernel 6.1 for the iPool SZ3568 (RK3568)"
SECTION = "kernel"
LICENSE = "GPL-2.0-only"

# LINUX_KERNEL_TYPE=custom => no yocto-kernel-cache / kmeta needed; configure
# straight from the vendor in-tree defconfig.
LINUX_KERNEL_TYPE = "custom"
KCONFIG_MODE = "alldefconfig"
KBUILD_DEFCONFIG = "rockchip_linux_defconfig"

LINUX_VERSION = "6.1"
LINUX_VERSION_EXTENSION = "-ipool"
# The tree is a 6.1.x stable; don't fail on PV(6.1) != Makefile(6.1.57).
KERNEL_VERSION_SANITY_SKIP = "1"

# JeffyCN mirror, Rockchip 6.1 BSP branch + pinned commit.
KBRANCH = "kernel-6.1-2024_04_14"
# nobranch=1: the pinned SRCREV isn't the branch tip, so don't verify it lives
# in the branch (matches JeffyCN's recipe); KBRANCH is still used by kernel-yocto.
SRC_URI = "git://github.com/JeffyCN/mirrors.git;protocol=https;nobranch=1;branch=${KBRANCH};name=machine"
SRCREV_machine = "ea9e2a9344bfe7f1130dee8100173b6cb95445d2"

# Don't hard-panic when the NPU power domain fails idle-ack during boot.
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += "file://0001-soc-rockchip-pm_domains-do-not-panic-on-idle-ack-timeout.patch"

# Device-mapper + dm-verity so RAUC can mount its verity-format OTA bundles
# (vendor defconfig leaves CONFIG_BLK_DEV_DM off -> "Failed mounting bundle").
SRC_URI += "file://dm-verity.cfg"

# NOTE: wired ethernet (gmac1) was abandoned -- the board's crystal-less GbE PHY
# needs the Rockchip vendor loader's gmac output-mode pin setup, which mainline
# U-Boot + this kernel driver can't reproduce (a clk_mac1_out driver patch was
# tried and never woke the PHY). gmac1 is disabled in the board DTS; connectivity
# is RTL8723DS WiFi. If ethernet is ever revisited, the vendor U-Boot graft is the
# known-good path (see the WiFi/ethernet notes in the layer history).

# GPU: panfrost/Mesa was tried and HARD-HANGS this vendor kernel/board (panfrost
# locks the CPU on GPU power-on). The vendor's working image uses the proprietary
# Mali Bifrost KO + libmali userspace instead. Reverted to the vendor defconfig
# GPU drivers; the GPU node is left disabled in the DT until libmali is wired in.
# (gpu-panfrost.cfg kept in files/ for reference.)

# NOTE: the GM8775C MIPI-DSI->LVDS bridge driver + its cfg were REMOVED. Decoding
# the vendor's shipped working image proved this board drives the 1024x600 panel
# via the SoC's NATIVE LVDS (&lvds -> simple-panel), not a MIPI bridge. The
# gm8775c driver (drivers/gpu/drm/bridge/gm8775c.c, still in local_repos for
# reference) was never in the real display path.

# Board device tree: use the VENDOR's real SZ3568 device tree (the 3 files
# Hpraise supplied) instead of an EVB-derived reconstruction. rk3568-sz3568-linux.dts
# includes rk3568-sz3568-v10.dtsi -> rk3568-sz3568.dtsi (both shipped here) plus
# the in-kernel rk3568.dtsi / rk3568-linux.dtsi. KERNEL_DEVICETREE (sz3568.conf)
# builds rk3568-sz3568-linux.dtb. All 3 must land in the kernel dts dir before
# do_compile. (rk3568-sz3568.dts kept as an EVB-based fallback, not built.)
SRC_URI += "file://rk3568-sz3568.dtsi"
SRC_URI += "file://rk3568-sz3568-v10.dtsi"
SRC_URI += "file://rk3568-sz3568-linux.dts"
SRC_URI += "file://rk3568-sz3568.dts"
do_configure:prepend() {
    install -m 0644 ${UNPACKDIR}/rk3568-sz3568.dtsi \
        ${S}/arch/arm64/boot/dts/rockchip/rk3568-sz3568.dtsi
    install -m 0644 ${UNPACKDIR}/rk3568-sz3568-v10.dtsi \
        ${S}/arch/arm64/boot/dts/rockchip/rk3568-sz3568-v10.dtsi
    install -m 0644 ${UNPACKDIR}/rk3568-sz3568-linux.dts \
        ${S}/arch/arm64/boot/dts/rockchip/rk3568-sz3568-linux.dts
    install -m 0644 ${UNPACKDIR}/rk3568-sz3568.dts \
        ${S}/arch/arm64/boot/dts/rockchip/rk3568-sz3568.dts
}

# No separate kernel-metadata repo.
KMETA = ""
SRCREV_meta = "${SRCREV_machine}"

# meta-rockchip's rk3568.inc does KERNEL_FEATURES:append:rk3568 with a BSP SCC
# ("remove-non-rockchip-arch-arm64.scc") that lives in its yocto-kernel-cache,
# only wired to linux-yocto. We're a custom vendor kernel with no kmeta, so that
# feature is dangling -> drop it (:remove wins over the machine's :append).
KERNEL_FEATURES:remove = "bsp/rockchip/remove-non-rockchip-arch-arm64.scc"
# Belt-and-suspenders: treat any other stray SCC feature as a warning, not fatal.
KERNEL_DANGLING_FEATURES_WARN_ONLY = "1"

require recipes-kernel/linux/linux-yocto.inc

# Vendor tree's COPYING (GPL-2.0 WITH Linux-syscall-note) has a different md5
# than linux-yocto.inc's mainline default; override with the actual checksum.
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

# Plain Image + separate DTB via extlinux (matches our u-boot; no rockchip FIT).
KERNEL_IMAGETYPE = "Image"

# B1: prove the vendor kernel boots on a vendor EVB DT (KERNEL_DEVICETREE is set
# in sz3568.conf); B2 switches to the board's vendor DT for the panel.

# 6.1 vendor kernel vs whinlatter gcc 15: if do_compile trips on -Werror, relax:
# KERNEL_EXTRA_ARGS += "KCFLAGS=-Wno-error"

# Kernel build-generated sources (oid_registry_data.c, consolemap_deftbl.c)
# embed the build path; harmless in the debug -src package.
INSANE_SKIP:${PN}-src += "buildpaths"

COMPATIBLE_MACHINE = "sz3568"
