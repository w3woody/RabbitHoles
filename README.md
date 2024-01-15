# Rabbit Holes

This is a set of projects that are my own personal 'rabbit holes': things I spent trying to figure out, out of personal curiosity or in an attempt to solve interesting problems.

And I'm sharing them here in the hope that perhaps they'll help someone else figure out something.

----

These are my own personal rabbit holes, complete with documentation as to what I found.

## [GIT](git/docs/README.md)

Out of an abundance of curiosity I wanted to know the contents of the .git/object files in a GIT repository. My goal with this rabbit hole--which is incomplete--was to build enough of the architecture of GIT from scratch so that I could clone a repository.

There are plenty of good sources for information out there--but all of them seemed somewhat 'incomplete.' So I scowered the [GIT source kit,](https://github.com/git/git) as well as several interesting sites, such as [here](https://shafiul.github.io//gitbook/7_the_packfile.html), [here](https://git-scm.com/book/en/v2/Git-Internals-Git-Objects) and even SlackOverflow, and build parsers capable of parsing the most common objects you find in a GIT repository.