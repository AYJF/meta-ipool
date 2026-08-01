# Plymouth's DRM renderer is only enabled for x86/x86-64 in the base recipe
# (PACKAGECONFIG:append:x86 = " drm"). This board is aarch64 with a DRM-only
# display (VOP2 -> LVDS, no fbdev), so without the DRM renderer plymouth has no
# way to draw the splash. Enable it here (pulls in libdrm).
PACKAGECONFIG:append = " drm"
