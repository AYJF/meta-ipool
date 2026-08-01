# Newer oe-core (whinlatter) rejects a literal S = "${WORKDIR}/git" (see
# insane.bbclass do_qa_unpack): the git fetcher now unpacks to ${UNPACKDIR}/${BP}
# and bitbake.conf's default S points there. meta-rockchip's rockchip-rkbin.inc
# still hardcodes the old value, so restore the working default here.
S = "${UNPACKDIR}/${BP}"
