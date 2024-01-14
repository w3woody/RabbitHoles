# GIT Commit objects

As mentioned [elsewhere](objecttypes.md), a commit object represents a [commit operation](https://git-scm.com/docs/git-commit). When you commit your changes in git, *presumably* a new collection of objects are constructed wich represent the current state of your source kit. (If objects or directories have not changed, then references to the unchanged objects are used.) The new root tree object is then stored in the commit, along with the parent object (or objects) that were used as the starting point of the changes (such as a merge operation), and a new commit object is then constructed.

Where the current commit object is stored, along with the current list of other objects is beyond the scope of my investigations. However, [there is some good documentation here](https://git-scm.com/book/en/v2/Git-Internals-Git-References) as to how commit object references are stored to track the current list of branches and the current 'head' branch. (Most of the files referred to in that documentation are short text files and can be easily inspected.)

## Commit Object storage

Our tree object is stored as type 'commit', meaning when stored in our object file as a single object, [the decompressed file will have the header](objectstorage.md)

    commit (size)\0

Following the null character is the contents of our commit itself.

## Commit data format

The commit data itself is stored as a text file. For example, 