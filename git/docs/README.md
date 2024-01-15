# The GIT rabbit hole

Out of an abundance of curiosity I wanted to know the contents of the .git/object files in a GIT repository. My goal with this rabbit hole--which is incomplete--was to build enough of the architecture of GIT from scratch so that I could clone a repository.

There are plenty of good sources for information out there--but all of them seemed somewhat 'incomplete.' So I scowered the [GIT source kit,](https://github.com/git/git) as well as several interesting sites, such as [here](https://shafiul.github.io//gitbook/7_the_packfile.html), [here](https://git-scm.com/book/en/v2/Git-Internals-Git-Objects) and even SlackOverflow, and build parsers capable of parsing the most common objects you find in a GIT repository.

----

So this is what I found.

----

- [GIT Objects](objects.md)
	- [GIT Object Names](objectnames.md)
	- [GIT Object Types](objecttypes.md)
	- [GIT Object Storage](objectstorage.md)
	- [GIT Tree Objects](trees.md)
	- [GIT Commit Objects](commits.md)
	- [GIT Tag Objects](tags.md)
	
- [GIT Pack Files](packintro.md)
    - [GIT Index File Format](idx.md)
    - [GIT Pack File Format](pack.md)

----

Note that there are still some [open questions](questions.md) about what I've found so far, and I haven't put these pieces together yet into a more coherent whole. I also have yet to figure out the specifics of the GIT wire protocol.

----

The example code associated with this contains two object directories from two (very small) GIT object directories. The first contains all naked objects; the second packs those objects aggressively into a pack file.

The sample code shows how to parse each of the different types, and shows examples of reading each of the object types as well as examples of applying delta changes to an object.

The code is intended for readability rather than for speed, though we do implement the binary search algorithm in our index file. There are a number of ways in which this could be improved, of course--but the whole point is to give a starting point to our basic object files, index files and pack file formats.

----

Our example code demonstrates the following:

1. Starting with a `commit` SHA-1, dumps the contents of the commit header and dumps the directory tree structure of the source tree contained under this commit.
2. Starting with a `tag` SHA-1, dumps the contents of a tag header and dumps the object (in this case, a commit) associated with the tag.
3. Dumps several blobs as text files given their SHA-1 name
4. Dumps the contents of a pack index file, including the type of the objects stored in that pack file
5. Pulls and dumps a blob from the pack file
6. Dumps the delta information associated with an OFS\_DELTA record. (Presumably the same would work with a REF\_DELTA record.)
7. Applies the delta information and dumps the results.

Hopefull this should give you a good idea how to perform certain operations, such as reconstructing the source directory from a commit (just recurse the tree, but build the directories that are named and dump the blob data objects as files), and give an idea of how a commit operation would work. (Essentially recurse down the current source tree, constructing the SHA-1 checksums of the resulting objects--and constructing a new commit tree. Note objects and directories that don't change will have the same SHA-1 checksum, so they don't need to be recreated.)

