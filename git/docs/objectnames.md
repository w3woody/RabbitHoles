# GIT Objects File names

Each object in the GIT object file directories is named after the SHA-1 hash of the contents of the file.

For example, if I have a file `test.txt` whose contents are the 12-byte ASCII string:

    Hello world!

Which, in hex, is:

    00000000:  48 65 6C 6C 6F 20 77 6F  72 6C 64 21              Hello.wo rld! 

The [SHA-1 checksum](https://en.wikipedia.org/wiki/SHA-1) of the file would be:

    6769dd60bdf536a83c9353272157893043e9f7d0

(You can see this by running the git command `git hash-object test.txt`.)

If you were to store this file (or search for this file) in the local git repository, you would find the file by splitting the first two characters off the *lower-case* SHA-1 checksum (in hex) of the file:

    67 69dd60bdf536a83c9353272157893043e9f7d0

Then you'd build your file path relative to the repository's root folder as:

    .git/objects/67/69dd60bdf536a83c9353272157893043e9f7d0
