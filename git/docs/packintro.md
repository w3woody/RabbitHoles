# Pack files

Within the `.git/objects/pack` directory are pack files, which store multiple object files within a pair of files.

The first file is the packfile index, stored with the name `pack-(SHA1).idx`. This is an index into the packfile itself, stored with the name `pack-(SHA1).pack`. In both cases, the "SHA1" part of the file name is the SHA-1 checksum of the pack file itself.

- [Index File Format](idx.md)
- [Pack File Format](pack.md)