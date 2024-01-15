# Open Questions

1. How are SHA-1 values calculated for OFS\_DELTA and REF\_DELTA objects calculated? My theory is that this is the checksum of the combined header (offset or SHA1) header and the decompressed delta data--but I'm uncertain.
