FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# Mark our board as compatible with linux-yocto. meta-rockchip's
# linux-yocto_%.bbappend enumerates COMPATIBLE_MACHINE per board and does not
# include ours, so virtual/kernel has no provider for sz3568 without this.
# The rockchip-kmeta SRC_URI (kernel config/DT fragments) is added
# unconditionally by meta-rockchip's bbappend, so we don't repeat it here.
COMPATIBLE_MACHINE:sz3568 = "sz3568"

# Ship our board device tree into the kernel's dts dir before compile. The
# kernel's implicit %.dtb:%.dts rule builds it when KERNEL_DEVICETREE names it
# (see sz3568.conf), so no Makefile edit is needed. Our DTS #includes the EVB
# DTS (same directory), which provides /dts-v1/ and the rk3568.dtsi base.
SRC_URI:append:sz3568 = " file://rk3568-sz3568.dts"

# Raise 8250 UART count so uart8 becomes ttyS8 (matches console=ttyS8/getty).
SRC_URI:append:sz3568 = " file://serial8.cfg"

# Enable the TC358775 bridge + panel-lvds/DSI for the display chain.
SRC_URI:append:sz3568 = " file://display.cfg"

# Backport native RK3568 (single-link) LVDS encoder support (not yet upstream).
SRC_URI:append:sz3568 = " file://rockchip-lvds-rk3568.patch"

# Power the LVDS DSI-DPHY at encoder-enable (after VOP2 dclk is live), matching
# the vendor BSP ordering, instead of at probe. NOTE: we deliberately do NOT
# retarget the inno-dsidphy LVDS PLL (kept at the mainline/vendor prediv=2,
# fbdiv=28 = 168MHz) — the vendor drives real panels with that same value, so
# it's an analog bias, not the 7x pixel serializer; the pixel rate comes from
# dclk_vp1 (GPLL 1200/25 = 48MHz, see the DTS). The panel-latch/black issue is
# addressed by DCLK_INV_SEL (in the encoder patch) + PHY-power-on timing here.
SRC_URI:append:sz3568 = " file://rockchip-lvds-phy-enable-timing.patch"

do_configure:prepend:sz3568() {
    install -m 0644 ${UNPACKDIR}/rk3568-sz3568.dts \
        ${S}/arch/arm64/boot/dts/rockchip/rk3568-sz3568.dts
}
