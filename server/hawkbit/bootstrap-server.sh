#!/bin/sh
# Run ONCE on a fresh Ubuntu (24.04 / 26.04 "resolute" verified) Hetzner CX as root.
# Installs Docker, locks down the firewall, and prepares /opt/hawkbit.
set -eu

echo "==> packages"
apt-get update -qq
apt-get install -y -qq ca-certificates curl ufw fail2ban unattended-upgrades

echo "==> docker (official repo)"
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
. /etc/os-release
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" \
  > /etc/apt/sources.list.d/docker.list
apt-get update -qq
apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker

echo "==> firewall: only SSH + HTTP/HTTPS"
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

echo "==> unattended security upgrades"
systemctl enable --now unattended-upgrades

echo "==> swap (CX22 has 4GB; a JVM + MariaDB benefits from headroom)"
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile >/dev/null
  swapon /swapfile
  grep -q '^/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

mkdir -p /opt/hawkbit
echo
echo "Done. Next:"
echo "  1) copy docker-compose.yml, Caddyfile and your .env into /opt/hawkbit"
echo "  2) cd /opt/hawkbit && docker compose up -d"
echo "  3) docker compose logs -f hawkbit    # wait for 'Started Application'"
