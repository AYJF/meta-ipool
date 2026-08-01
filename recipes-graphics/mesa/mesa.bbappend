# Mesa 25.x (whinlatter) removed the standalone "kmsro" gallium driver; KMS
# render-only handling is now built into each gallium driver (e.g. panfrost).
# meta-rockchip's mesa.bbappend still appends the now-invalid "kmsro"
# PACKAGECONFIG for rk3568, which fails do_recipe_qa (invalid-packageconfig).
# Strip it. panfrost (Mali-G52) remains and is what we actually need.
PACKAGECONFIG:remove = "kmsro"

# CRITICAL: Mesa 25.x gates the Panfrost gallium (and vulkan) driver behind
# BOTH 'panfrost' AND 'libclc' being present in PACKAGECONFIG:
#   GALLIUMDRIVERS .= "${@bb.utils.contains('PACKAGECONFIG','panfrost libclc',
#                                            ',panfrost','',d)}"   (mesa.inc)
# Panfrost now precompiles its internal CL shaders with mesa-clc, which the
# 'libclc' PACKAGECONFIG enables. meta-rockchip appends only 'panfrost' (no
# libclc), so bb.utils.contains() was false, panfrost was dropped from
# GALLIUMDRIVERS, and mesa-megadriver shipped EMPTY (no /usr/lib/dri/
# panfrost_dri.so). Result on-target: libEGL/libgbm present but no HW driver ->
# eglInitialize fails (EGL_NOT_INITIALIZED 0x3001) -> Weston SIGSEGV and every
# Qt EGL/Wayland client aborts, while the (GL-less) framebuffer path works.
# Add libclc so the Panfrost driver actually builds and lands in the megadriver.
# ('libclc' pulls native clang/llvm/spirv build tooling via DEPENDS - a heavy
# one-time native build - but adds no runtime packages to the image.)
PACKAGECONFIG:append = " panfrost libclc"

# SECOND gap (Mesa 25.x, wayland-only builds): the DRI-loader stub target
# 'gallium/targets/dril' — the ONLY thing that installs /usr/lib/dri/<hw>_dri.so
# (rockchip_dri.so for the display node, panfrost_dri.so for render, swrast, ...)
# — is guarded in src/meson.build by `with_glx=='dri' OR x11 OR xcb`. Our image
# is wayland-only, and oe-core forces -Dglx=disabled when x11 is absent, so NONE
# of the three held: the dril target never built, /usr/lib/dri/ shipped EMPTY,
# and GBM/EGL had no <driver>_dri.so to dlopen -> eglInitialize fails
# (EGL_NOT_INITIALIZED) -> Weston SIGSEGV / Qt aborts, even though
# libgallium-<ver>.so (with panfrost+kmsro built in) is present.
#
# Enabling the x11 platform would satisfy the guard but pulls X11_DEPS
# (libxxf86vm etc.) which REQUIRE the 'x11' DISTRO_FEATURE — absent in the
# wayland-only b2qt distro ("Nothing PROVIDES libxxf86vm"). Adding x11 distro-
# wide is far too invasive. Instead patch the meson guard to build the dril
# stubs for any gallium+gbm config (they have no X11 dependency). See the patch.
FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += "file://0001-meson-build-dril-loader-stubs-for-wayland-only-gbm.patch"
