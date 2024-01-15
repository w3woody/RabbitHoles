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