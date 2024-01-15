# GIT Objects types

A file in your file system (such as a text file containing the string "Hello world!") would be stored as a 'blob'.

There are only four types of object files stored in our object file directory:

- `blob` Represents an arbitrary file in your source kit.
- `tree` Represents the directory contents within your source kit. Each tree object is the equivalent of a directory node: it lists the file names, file permissions and the file's SHA-1 hash (used to find the blob holding the file or the directory's contents).
- `commit` Represents a 'commit' in your repository. That is, represents a reference to the root tree of a particular version of your source tree stored in GIT, combined with references to the parent tree nodes making up this commit, along with the author, the committer, and commit remarks.
- `tag` Represents a ['tag' in your repository.](https://git-scm.com/book/en/v2/Git-Basics-Tagging) Contains a reference to an object and the type of object this tag points to, as well as the name of the tagger, the name of the tag, and a commit message associated with this tag, if any. *Note: I don't know if this only is used for annotated tags; I did not investigate.*

Within the `.git/objects/pack` directory are two further file types that can be encountered within the contents of a `.pack` file:

- `ofs-delta` Represents the "delta" changes that can be applied to a different object file (contained with a pack file) that can be used to reconstruct the specified file.
- `ref-delta` Represents the "delta" changes that can be applied to a different object file named by the SHA-1 file name (so, in theory, it can be in a different .pack file or as a single object file) to reconstruct the specified file.

Both are described in the section on pack files.
