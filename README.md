# meta-ipool

Custom Yocto BSP layer for the **iPool SZ3568** board (Rockchip RK3568), built on
Qt's **Boot2Qt 6.11** + the Rockchip vendor 6.1 kernel, with **RAUC A/B OTA**
driven by a self-hosted **hawkBit** server.

This layer is the only hand-written part of the project. Everything else
(poky, meta-qt6, meta-boot2qt, meta-openembedded, meta-rockchip, meta-rauc) is
upstream and pinned by the `repo` manifest below — so a fresh machine is
reproduced with `repo init` + `repo sync` + `bitbake`, **not** by copying the
140 GB build tree (tmp/, downloads/, sstate-cache/ are all regenerated).

## What this layer provides
- `conf/machine/sz3568.conf` — the board machine (kernel, DTB, console, OTA, WiFi)
- `recipes-kernel/linux-rockchip/` — vendor 6.1 kernel + board DTS + config fragments
  (incl. `dm-verity.cfg` so RAUC verity bundles mount)
- `recipes-bsp/u-boot/` — console routing + the RAUC A/B `boot.cmd` (booti, slot select)
- `recipes-core/rauc-bundles/` — the `ipool-rauc-bundle` (b2qt image, verity)
- `recipes-support/rauc-hawkbit-updater/` — hawkBit client config (gateway-token auto-register)
- `recipes-core/udev/` — don't-automount-the-eMMC rule (keeps RAUC's inactive slot free)

---

## Reproduce on a new machine

Prereqs: Ubuntu 22.04/24.04 host, the `repo` tool, `git`, ~200 GB free disk,
and the standard Yocto build dependencies.

```bash
# 1) Bootstrap the Boot2Qt 6.11 tree
mkdir -p ~/rockchip && cd ~/rockchip
repo init -u https://code.qt.io/yocto/boot2qt-manifest -b default -m v6.11.1.xml

# 2) Pin the iPool additions (meta-rockchip + meta-rauc + meta-ipool).
mkdir -p .repo/local_manifests
cat > .repo/local_manifests/ipool.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<manifest>
  <!-- Community OE Rockchip BSP (whinlatter) -->
  <project name="meta-rockchip" remote="yocto"
           revision="refs/heads/whinlatter" upstream="whinlatter"
           path="sources/meta-rockchip"/>

  <!-- RAUC layer, pinned to the exact commit this project was built with -->
  <remote name="rauc-gh" fetch="https://github.com/rauc"/>
  <project name="meta-rauc" remote="rauc-gh"
           revision="4bc963abde09904392d3ce7524d430b7e4b808aa"
           upstream="whinlatter" path="sources/meta-rauc"/>

  <!-- This layer -->
  <remote name="ipool-gh" fetch="https://github.com/AYJF"/>
  <project name="meta-ipool" remote="ipool-gh"
           revision="main" path="sources/meta-ipool"/>
</manifest>
EOF

# 3) Fetch all layers at their pinned revisions
repo sync -j8

# 4) Configure the build (Boot2Qt setup script generates build-sz3568/conf/)
export MACHINE=sz3568
source ./setup-environment.sh build-sz3568

# 5) Ensure meta-ipool + meta-rauc are layers and MACHINE is sz3568
bitbake-layers add-layer ../sources/meta-ipool ../sources/meta-rauc 2>/dev/null || true
grep -q '^MACHINE = "sz3568"' conf/local.conf || echo 'MACHINE = "sz3568"' >> conf/local.conf

# 6) Build the image + the OTA bundle
bitbake b2qt-embedded-qt6-image ipool-rauc-bundle
```

Artifacts land in `build-sz3568/tmp/deploy/images/sz3568/`:
`*.rootfs.wic` (flash with `rkdeveloptool wl 0`) and `ipool-rauc-bundle-*.raucb`
(upload to hawkBit).

## Notes
- The RAUC bundle is signed with meta-rauc's **development-1 dev keys** — replace
  with production keys (`recipes-core/rauc-bundles/files/`) before shipping.
- hawkBit client points at `10.0.0.82:8080`; change in
  `recipes-support/rauc-hawkbit-updater/files/config.conf`.
- A reference copy of the local manifest is kept in `manifests/ipool.xml`.
