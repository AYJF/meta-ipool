# Caddy + replace-response.
#
# Why a custom image: stock Caddy cannot rewrite the BODY of a proxied response.
# The hawkBit simple UI is a Vaadin app, so there is no HTML file of ours to edit
# -- the only way to get a stylesheet reference into its index.html is to rewrite
# the response as it passes through the proxy. That is what this plugin does.
FROM caddy:2-builder AS builder
RUN xcaddy build --with github.com/caddyserver/replace-response

FROM caddy:2
COPY --from=builder /usr/bin/caddy /usr/bin/caddy
