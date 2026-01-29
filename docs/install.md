Installation
============

This document describes how to install [actionlint](../docs).

## Windows

### [Chocolatey](https://chocolatey.org/)

[`actionlint` package][chocolatey] is available in the community repository:

```powershell
choco install actionlint
```

### [Scoop](https://scoop.sh/)

[`actionlint` package][scoop] is available in the main bucket:

```powershell
scoop install actionlint
```

### [Winget](https://learn.microsoft.com/en-us/windows/package-manager/)

[`actionlint` package][winget] is available in the winget-pkgs repository:

```powershell
winget install actionlint
```

## Linux

### [Arch Linux](https://archlinux.org/)

[`actionlint` package][archlinux] is available in the official repository:

```sh
pacman -S actionlint
```

Alternatively actionlint is also available on [AUR][aur]. The packages can be installed via [`paru`][paru] command.

- [actionlint-bin](https://aur.archlinux.org/packages/actionlint-bin)
- [actionlint-git](https://aur.archlinux.org/packages/actionlint-git)

### [Nix](https://nixos.wiki/)

[`actionlint` package][nixpkgs] is available in the Nix ecosystem:

On NixOS:

```sh
nix-env -iA nixos.actionlint
```

On Non NixOS:

```sh
nix-env -iA nixpkgs.actionlint
```

## macOS

### [Homebrew][homebrew]

[`actionlint`][formula] formula is provided by Homebrew officially.

```sh
brew install actionlint
```

Alternatively, rhysd/actionlint repository also provides its own Homebrew package, which is automatically updated on new release.
If you prefer it, tap the repository before running `brew install`.

```sh
brew tap "rhysd/actionlint" "https://github.com/rhysd/actionlint"
brew install actionlint
```

## Prebuilt binaries

Download an archive file from [the releases page][releases] for your platform, unarchive it and put the executable file to a
directory in `$PATH`.

Prebuilt binaries are built at each release by CI for the following OS and arch:

- macOS (x86_64, arm64)
- Linux (i386, x86_64, arm32, arm64)
- Windows (i386, x86_64, arm64)
- FreeBSD (i386, x86_64)

Note: The following targets are not tested since GitHub Actions doesn't support them:

- Linux i386, arm32, arm64
- Windows i386, arm64
- FreeBSD i386, x86_64

<a id="download-script"></a>
## Download script

To install `actionlint` executable with one command, [the download script](../scripts/download-actionlint.bash) is available.
It downloads the latest version of actionlint (`actionlint.exe` on Windows and `actionlint` on other OSes) to the current
directory automatically. This is a recommended way if you install actionlint in some shell script.

```sh
bash <(curl https://raw.githubusercontent.com/rhysd/actionlint/main/scripts/download-actionlint.bash)
```

When you need to install specific version of actionlint, please give the version to the 1st command line argument. The following
example installs v1.6.17.

```sh
bash <(curl https://raw.githubusercontent.com/rhysd/actionlint/main/scripts/download-actionlint.bash) 1.6.17
```

This script downloads `actionlint` (or `actionlint.exe` on Windows) binary to the current working directory. When you need to put
the downloaded binary to some other directory, please give the directory path to the 2nd command line argument. The following
example installs the latest version to `/usr/bin`.

```sh
bash <(curl https://raw.githubusercontent.com/rhysd/actionlint/main/scripts/download-actionlint.bash) latest /usr/bin
```

For the usage of actionlint on GitHub Actions, see [the usage document](usage.md#on-github-actions).

## Docker image

See [the usage document](./usage.md#docker) to know how to install and use an official actionlint Docker image.

## asdf

You can install actionlint with the [asdf version manager][asdf] using the [asdf-actionlint][asdf-plugin] plugin, which
automates the process of installing (and switching between) various versions of GitHub release binaries. With asdf already
installed, run these commands to install actionlint:

```bash
# Add actionlint plugin
asdf plugin add actionlint

# Show all installable versions
asdf list-all actionlint

# Install specific version
asdf install actionlint latest

# Set a version globally (on your ~/.tool-versions file)
asdf global actionlint latest
```

## Build from source

Recent [Go][] toolchain is necessary to build actionlint from source. Use Go 1.16 or later.

```sh
# Install the latest stable version
go install github.com/rhysd/actionlint/cmd/actionlint@latest

# Install the head of main branch
go install github.com/rhysd/actionlint/cmd/actionlint
```

---

[Checks](checks.md) | [Usage](usage.md) | [Configuration](config.md) | [Go API](api.md) | [References](reference.md)

[formula]: https://formulae.brew.sh/formula/actionlint
[homebrew]: https://brew.sh/
[releases]: https://github.com/rhysd/actionlint/releases
[Go]: https://golang.org/
[asdf]: https://asdf-vm.com/
[asdf-plugin]: https://github.com/crazy-matt/asdf-actionlint
[chocolatey]: https://community.chocolatey.org/packages/actionlint
[scoop]: https://scoop.sh/#/apps?q=actionlint&s=0&d=1&o=true
[winget]: https://github.com/microsoft/winget-pkgs/tree/master/manifests/r/rhysd/actionlint
[archlinux]: https://archlinux.org/packages/extra/x86_64/actionlint/
[aur]: https://aur.archlinux.org/
[paru]: https://github.com/Morganamilo/paru
[nixpkgs]: https://github.com/NixOS/nixpkgs/blob/master/pkgs/development/tools/analysis/actionlint/default.nix
