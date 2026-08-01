# Bring-up/diagnostic tools for the pool-controller image.
# i2c-tools provides i2cdetect/i2cget/i2cset — needed to probe the DSI-LVDS
# bridge on i2c2 (TC358775 @0x0f vs gm8775c @0x2c).
# libdrm-tests provides modetest — to enumerate DRM connectors/CRTCs and drive
# the display mode-set by hand while bringing up the LVDS/bridge panel.
IMAGE_INSTALL:append = " i2c-tools libdrm-tests"

# PATH B: the "tools-profile" image feature pulls in perf, whose oe-core recipe
# builds against the kernel's tools/ tree and does NOT build against the vendor
# 6.1 BSP tree (missing install_headers in tools/lib/api). A pool controller
# doesn't need profiling tools, so drop the feature (also removes valgrind etc.).
IMAGE_FEATURES:remove = "tools-profile"
EXTRA_IMAGE_FEATURES:remove = "tools-profile"
