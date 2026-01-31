DESCRIPTION = "Package Manager for OCaml"
SECTION = "devel"
LICENSE = "GPLv2"

SRC_URI = " \
    git://github.com/ocaml/opam.git;protocol=https;tag=2.0.0 \
    file://0001-jbuilder-pathfix.patch \
    "

# Tag 2.0.0
SRCREV = "b2b8f1e32c355a1230d9be9c54a2f56750d3dc7f"

LIC_FILES_CHKSUM = "file://LICENSE;md5=d9e77cf0b09010013d038358f983b42a"

S = "${WORKDIR}/git"

# * opam has an autotools-style 'configure' file, but the makefile has been
#   created manually.
# * we inherit 'cross' because opam will run on the build system, but will
#   facilitate builds for the target system.
inherit autotools-brokensep cross

PROVIDES = "${TARGET_PREFIX}opam-cross"
PN = "opam-cross-${TARGET_ARCH}"

DEPENDS += " \
    ocaml-native \
    bzip2-native \
    rsync-native \
    unzip-native \
    coreutils-native \
    "

# somehow this seems to be required...
PARALLEL_MAKE = "-j 1"

# create opam-root in work-shared directory
OPAM_ROOT = "${TMPDIR}/work-shared/ocaml/opam-root-${TARGET_SYS}"

# --prefix seems to be the only config option that's actually observed.
EXTRA_OECONF = "\
    --prefix=${prefix} \
    "

do_compile:prepend () {
    oe_runmake lib-ext
}

do_install () {
    oe_runmake install DESTDIR=${D}

    # initialize, create opam root in work-shared
    rm -rf ${OPAM_ROOT}
    ${D}${prefix}/bin/opam init \
        --disable-sandboxing \
        --root=${OPAM_ROOT} \
        --no-setup
}

sysroot_stage_all:append () {
    sysroot_stage_dir ${D}${prefix}/bin ${SYSROOT_DESTDIR}${prefix}/bin
}
