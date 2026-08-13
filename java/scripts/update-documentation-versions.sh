#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 4 ]]; then
    echo "Usage: $0 <release-version> <development-version> <readme> <jbang-example>" >&2
    exit 2
fi

VERSION=$1
DEV_VERSION=$2
README=$3
JBANG_EXAMPLE=$4
VERSION_FORMAT='[0-9]+\.[0-9]+\.[0-9]+(-(preview|(beta-)?java(-preview)?)\.[0-9]+)?'

if [[ ! "$VERSION" =~ ^${VERSION_FORMAT}$ ]]; then
    echo "Invalid release version: $VERSION" >&2
    exit 2
fi
if [[ ! "$DEV_VERSION" =~ ^${VERSION_FORMAT}-SNAPSHOT$ ]]; then
    echo "Invalid development version: $DEV_VERSION" >&2
    exit 2
fi
if [[ ! -f "$README" || ! -f "$JBANG_EXAMPLE" ]]; then
    echo "README and JBang example files must exist" >&2
    exit 2
fi

export VERSION DEV_VERSION

perl -0 - "$README" <<'PERL'
use strict;
use warnings;

my ($path) = @ARGV;
open my $input, '<', $path or die "Cannot read $path: $!\n";
my $content = do { local $/; <$input> };
close $input or die "Cannot close $path: $!\n";

# Match accepted release versions plus numeric suffixes left by the former broken updater.
my $version = qr/[0-9]+\.[0-9]+\.[0-9]+(?:-(?:preview|(?:beta-)?java(?:-preview)?)\.[0-9]+)?(?:-[0-9]+)*/;
my $sdk_dependency_version = qr{(<artifactId>copilot-sdk-java</artifactId>(?:(?!</dependency>).)*?<version>)}s;
my $snapshot_xml = ($content =~ s{$sdk_dependency_version$version-SNAPSHOT</version>}{$1$ENV{DEV_VERSION}</version>}g);
my $snapshot_gradle = ($content =~ s{(copilot-sdk-java:)$version-SNAPSHOT(?![-A-Za-z0-9.])}{$1 . $ENV{DEV_VERSION}}ge);
my $release_xml = ($content =~ s{$sdk_dependency_version$version</version>}{$1$ENV{VERSION}</version>}g);
my $release_gradle = ($content =~ s{(copilot-sdk-java:)$version(?![-A-Za-z0-9.])}{$1 . $ENV{VERSION}}ge);

die "Expected one release and one snapshot example for both Maven and Gradle in $path\n"
    unless $snapshot_xml == 1 && $snapshot_gradle == 1 && $release_xml == 1 && $release_gradle == 1;

open my $output, '>', $path or die "Cannot write $path: $!\n";
print {$output} $content;
close $output or die "Cannot close $path: $!\n";
PERL

perl -0 - "$JBANG_EXAMPLE" <<'PERL'
use strict;
use warnings;

my ($path) = @ARGV;
open my $input, '<', $path or die "Cannot read $path: $!\n";
my $content = do { local $/; <$input> };
close $input or die "Cannot close $path: $!\n";

my $version = qr/[0-9]+\.[0-9]+\.[0-9]+(?:-(?:preview|(?:beta-)?java(?:-preview)?)\.[0-9]+)?(?:-[0-9]+)*/;
my $version_count = ($content =~ s{(copilot-sdk-java:)$version(?![-A-Za-z0-9.])}{$1 . $ENV{VERSION}}ge);
my $placeholder_count = ($content =~ s{copilot-sdk-java:\$\{project\.version\}}{copilot-sdk-java:$ENV{VERSION}}g);

die "Expected exactly one Copilot SDK dependency in $path\n"
    unless $version_count + $placeholder_count == 1;

open my $output, '>', $path or die "Cannot write $path: $!\n";
print {$output} $content;
close $output or die "Cannot close $path: $!\n";
PERL

grep -Fqx "    <version>${VERSION}</version>" "$README"
grep -Fqx "implementation 'com.github:copilot-sdk-java:${VERSION}'" "$README"
grep -Fqx "    <version>${DEV_VERSION}</version>" "$README"
grep -Fqx "implementation 'com.github:copilot-sdk-java:${DEV_VERSION}'" "$README"
grep -Fqx "//DEPS com.github:copilot-sdk-java:${VERSION}" "$JBANG_EXAMPLE"
