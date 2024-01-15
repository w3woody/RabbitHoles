# GIT Tag objects

As mentioned [elsewhere](objecttypes.md), a tag object represents a [tag operation](https://git-scm.com/docs/git-tag).

## Tag Object storage

Our tree object is stored as type 'commit', meaning when stored in our object file as a single object, [the decompressed file will have the header](objectstorage.md)

    tag (size)\0

Following the null character is the contents of our tag itself.

## Tag data format

The tag data itself is stored as a text file, with a header and a body of text, similar to the [commit object file](commits.md). This includes how headers can wrap, and also includes the format of the tagger of the tag.

For example:

    object d813f505dfd1e78f074c35f75f50ef25ecd11734
    type commit
    tag SampleTag
    tagger William Woody <woody@alumni.caltech.edu> 1705259204 -0500
    
    This is a sample tag.
    -----BEGIN PGP SIGNATURE-----
    
    iQIzBAABCAAdFiEEHpCA320AYSUkcKap+odwLGmTGD4FAmWkMMQACgkQ+odwLGmT
    GD6YxQ/6AnPulfXjEsM44AOhPYfB766DiUHaQBWc4kBmIPxnJ48VLpwAV6AhovY8
    kTH1fDAyUpyh4gnviyvMUf8qx0nh+ko6KGchTJpj4hyDCKXmgGMc4LoI4Q9RkDIY
    F/9cXnX/cKZ3Af0gvaRynImdZO5ckD21qb2Ga1X45IFl657ctyI2omCIAY+mmct6
    s4Mt8TiCSaYXTFs/hbfJ7jFWnyyWIDzgcVOrm8nFzIbblwvVY2jOAbHccU8i+bN1
    foH3rHFxLNhVVPiUfnQr6pOWLlQdHoAPFUY1LUazswuxhlYJ300crXVlwn0AgvQ5
    r/yQg5Sy7IvnQ4QY8WXQ+F/IPKXEEleRQHGumIVdndeHLE0ie1KjVZX2cJOb1toA
    GKqMJ4Xd8dSWWwahOmcXlQt20tRYW+0OIY/uHcbXpccJLVkDOp+PROnIS7l8icrH
    iaBOpKmsPmrfAa8WYtjBUDIwTvTnwQ/W0vAf/jmaVdsVLOSzAZGe4sqW9M/gYW0y
    4s9gGLLzmZYA3Mt+g9U5Nxoys/DFEayBwr3D7AvSBTAmhhGHsVag0+yxbrlpkHzx
    7lOJlgKUkliZw6wZWg21krhOHC9IqbGnlAXY3B92WMM45rj/g9FB/i5fZQvNv9Am
    0nenGRTD8X3msNldWDKqFtvMsHHRVrRJTrb3p9OjVYtPaMze60s=
    =yF9K
    -----END PGP SIGNATURE-----

See the discussion on the [commit object format](commits.md) for more information on how this would be parsed.

### Required Tag Header Objects

It appears the commit message requires the following:

- `object ` A reference to the object (in this case, the commit) this tag refers to.
- `type ` The type of object this tags. (I believe this can be our [various object types](objecttypes.md), but I did not investigate.
- `tagger` The tagger who created this tag.
- `tag` The name of the tag. In this case, `SampleTag`.

I am unaware if other tags can be present.

### The tag message

Following the header is a single blank line, then the plain text of the tag message. The tag message may contain other information, such as, in this case, a PGP signature key.
