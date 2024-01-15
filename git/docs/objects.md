# GIT Objects

The GIT `objects` directory consists of a series of directories named 'XX', with each 'X' being a hex value from 00 to ff. Within each directory are object files, each with a 38 character file name as hex digits.

Each file contains a single GIT object.

The GIT `objects` directory also contains two additional directories: `info` and `pack`.

- [GIT Object Names](objectnames.md)
    - [GIT Object Types](objecttypes.md)
    - [GIT Object Storage](objectstorage.md)
    - [GIT Tree Objects](trees.md)
    - [GIT Commit Objects](commits.md)
    - [GIT Tag Objects](tags.md)

GIT pack files are stored in the `pack` directory, and usually consists of an index file (.idx) and a pack file (.pack). The index file is a table of contents that refers to the associated pack file.

The contents of the file is [described elsewhere](packintro.md).
