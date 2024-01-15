# GIT Object File Storage Format

Object are stored as [zlib DEFLATE compressed files,](https://www.zlib.net) using the content format described in [RFC-1951](https://www.rfc-editor.org/rfc/rfc1951), and complete with headers and tail checksums as described in [RFC-1950](https://www.rfc-editor.org/rfc/rfc1950).

(The specific format of those files is another rabbit hole.)

**NOTE:** This is true anywhere zlib compressed files are used in GIT, including when embedded in pack files.

Each 'inflated' (or decompressed) object file starts with the following header:

    TYPE (sp) SIZE (\0)

The 'type' field is a variable length string (terminated by a space) that contains one of the four strings `blob`, `tree`, `commit` or `tag`. Any other value would be illegal.

After the space is a numeric value, terminated by a null character, giving the size of the decompressed file.

Following the null character is the contents of the file itself.

For example, if we were to store the contents of our "Hello world!" `test.txt` file [described elsewhere](objectnames.md) into an object file, the resulting *decompressed* object file would look like:

    00000000:  62 6C 6F 62 20 31 32 00  48 65 6C 6C 6F 20 77 6F  blob.12. Hello.wo
               ^^^^^^^^^^^^^^^^^^^^^^^                           ^^^^^^^^
    00000010:  72 6C 64 21                                       rld!             

The header (both as HEX and as ASCII) is underlined. Note it consists of the text string "blob 12" followed by a null character. Recall our file name was 12 bytes long. Following the null character is the contents of our object file itself.

After being compressed using zlib's DEFLATE algorithm, this file would be stored using the SHA checksum `6769dd60bdf536a83c9353272157893043e9f7d0` at the file location:

    .git/objects/67/69dd60bdf536a83c9353272157893043e9f7d0

## Validating an object file

From the above, you can see there are two different ways in which an object file can be validated.

1. By decompressing the contents of the file and running the SHA-1 checksum. If the checksum does not match the 40 character file object ID (the 2 character directory and 38 character filename) then the file is not valid.
2. By matching the length of the decompressed file with the file size stored in our header. If the two do not match, then the file is not valid.

Of course the ZLib compressed file has its own internal checks that must be passed to successfully decompress a file--but that's a different rabbit hole to go down.
