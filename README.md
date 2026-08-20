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
- `recipes-core/rauc/rauc-conf.bbappend` — ships **our own** CA as the device keyring
- `recipes-core/rauc-activate/` — reboots into a staged slot at 03:00, not mid-cycle
- `server/hawkbit/` — the cloud hawkBit stack (compose + Caddy + release script)

---

## Reproduce on a new machine

Prereqs: Ubuntu 22.04/24.04 host, the `repo` tool, `git`, ~200 GB free disk,
and the standard Yocto build dependencies.

```bash
# 1) Bootstrap the Boot2Qt 6.11 tree
mkdir -p ~/rockchip && cd ~/rockchip
repo init -u https://code.qt.io/yocto/boot2qt-manifest -b default -m v6.11.1.xml

# 2) Pin the iPool additions (meta-rockchip + meta-rauc + meta-ipool) by
#    pulling this repo's tracked local manifest straight from GitHub (public):
mkdir -p .repo/local_manifests
curl -fsSL https://raw.githubusercontent.com/AYJF/meta-ipool/main/manifests/ipool.xml \
     -o .repo/local_manifests/ipool.xml

# 3) Fetch all layers at their pinned revisions
repo sync -j8

# 3b) (optional) keep the manifest tracked: after the sync has cloned meta-ipool,
#     symlink it so future edits to manifests/ipool.xml take effect on re-sync.
ln -sf ../../sources/meta-ipool/manifests/ipool.xml .repo/local_manifests/ipool.xml

# 4) Configure the build (Boot2Qt setup script generates build-sz3568/conf/)
export MACHINE=sz3568
source ./setup-environment.sh build-sz3568

# 5) Ensure meta-ipool + meta-rauc are layers and MACHINE is sz3568
# order matters: each layer's LAYERDEPENDS must already be present
bitbake-layers add-layer ../sources/meta-arm/meta-arm-toolchain \
                         ../sources/meta-arm/meta-arm \
                         ../sources/meta-rauc \
                         ../sources/meta-rockchip \
                         ../sources/meta-ipool
grep -q '^MACHINE = "sz3568"' conf/local.conf || echo 'MACHINE = "sz3568"' >> conf/local.conf

# 6) Build the image + the OTA bundle
bitbake b2qt-embedded-qt6-image ipool-rauc-bundle
```

Artifacts land in `build-sz3568/tmp/deploy/images/sz3568/`:
`*.rootfs.wic` (flash with `rkdeveloptool wl 0`) and `ipool-rauc-bundle-*.raucb`
(upload to hawkBit).

## Secrets you must carry to a new machine

**`repo sync` alone will NOT give you a buildable tree.** Four secrets live
deliberately outside this (public) repo, in `rockchip/secrets/`. Copy that
directory across by hand — encrypted USB, password manager, whatever — never
through git:

| File | What it is | Lose it and… |
|---|---|---|
| `rauc-ca/ca.key.pem` | RAUC **CA private key** | you can never sign an update any deployed device will accept. **Back this up offline.** |
| `rauc-ca/ipool-signing-1.key.pem` | bundle signing key | re-issuable from the CA |
| `rauc-ca/ipool-signing-1.cert.pem` | bundle signing cert | re-issuable from the CA |
| `hawkbit-gateway-token` | hawkBit gateway token | baked into the image at build time; rotate both sides |
| `hawkbit-admin-password` | hawkBit admin password | reset via the container env |
| `hawkbit.env` | server DB passwords + admin bcrypt | only needed to redeploy the server |

Only `recipes-core/rauc/files/ipool-ca.cert.pem` (the CA **certificate**, public
by design) is tracked here. Two recipes refuse to build without the private
material and say so at parse time rather than failing obscurely:

```bash
# override if secrets live elsewhere on the new machine (conf/local.conf):
IPOOL_RAUC_KEY_DIR        = "/path/to/secrets/rauc-ca"
IPOOL_HAWKBIT_TOKEN_FILE  = "/path/to/secrets/hawkbit-gateway-token"
```

## OTA / hawkBit

The fleet talks to a self-hosted hawkBit at **https://updates.nexpoolsystem.com**
(Hetzner CX23, Helsinki). The whole stack is in `server/hawkbit/`:

```bash
# recreate the server from scratch (fresh Ubuntu, x86_64 -- hawkBit is amd64-only)
scp server/hawkbit/bootstrap-server.sh root@HOST:/tmp/ && ssh root@HOST 'sh /tmp/bootstrap-server.sh'
cp server/hawkbit/.env.example .env   # fill in, then copy to /opt/hawkbit/ with the rest
ssh root@HOST 'cd /opt/hawkbit && docker compose up -d'
```

Cut a release (uploads the artifact, builds the distribution set, assigns it):

```bash
./server/hawkbit/release.sh v1.0.4 "" sz3568-<serial>
```

Devices **self-register** on first poll via the gateway token, under a controller
id derived from the SoC serial (`sz3568-<serial>`), so no per-device provisioning.

## Notes / gotchas worth keeping

- **Bundles are signed by our own CA** (`O=QEI Inc, CN=iPool RAUC Root CA`), not
  meta-rauc's public `development-1` demo keys. A device flashed with an older
  image trusts the *demo* CA and will reject these bundles — such a board must be
  reflashed over USB once to transition.
- The signing cert's EKU **must include `emailProtection`**. RAUC verifies CMS via
  OpenSSL, which applies the S/MIME-signing purpose; a `codeSigning`-only EKU
  fails with `unsuitable certificate purpose`.
- `RK_IMAGE_INCLUDES_UBOOT_ENV = "1"` is **required**, not optional. With
  `RK_RAUC_DEMO` the kernel cmdline comes from the U-Boot env var `bootargsbase`
  (via `boot.scr`), not extlinux. Default is "no", which ships an empty env
  partition and a board that will not boot.
- `post_update_reboot = false` on purpose: the relay board is hardware-latched, so
  a surprise reboot leaves pumps/heaters unsupervised. `rauc-activate.timer`
  reboots at 03:00 instead, only when a slot is staged and marked good.
- Ethernet (gmac1) needs the vendor **Maxio MAE0621A** PHY driver
  (`recipes-kernel/linux-rockchip/files/maxio.c`), built in via
  `ethernet-maxio.cfg`. The PHY sits at MDIO address **0** and its ID is pinned in
  the DTS as `ethernet-phy-id7b74.4411`, because the first read after reset
  returns `0xffff` and would otherwise bind `Generic PHY`.
- Qt Creator deploys need `LD_LIBRARY_PATH=/usr/lib/mali` and
  `QT_QPA_PLATFORM=eglfs` in the **Run** environment. libmali only beats Mesa via
  systemd's `DefaultEnvironment`, which SSH sessions never see — without it the
  app silently renders on Mesa softpipe.
- A reference copy of the local manifest is kept in `manifests/ipool.xml`.
