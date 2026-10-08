#!/usr/bin/env python3
"""Print the Postgres MD5 password verifier for a role: "md5" + MD5(password + role).

Reads the password on stdin. Use it to roll a role back to MD5. The verifier comes from
the same password, so the password does not change.

Usage: printf '%s' "$PW" | md5-verifier.py ROLE

Exits non-zero and prints nothing on stdout if it cannot make a valid verifier, for
example when MD5 is not available. A caller must still check the format before it uses
the output: Postgres treats a string that is not a verifier as a plaintext password.
"""
import hashlib
import re
import sys


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__, file=sys.stderr)
        return 64
    password = sys.stdin.buffer.read()
    if not password:
        print("md5-verifier: empty password on stdin", file=sys.stderr)
        return 1
    data = password + sys.argv[1].encode()
    try:
        # A FIPS Python refuses MD5 unless the call says it is not for security.
        digest = hashlib.md5(data, usedforsecurity=False).hexdigest()
    except TypeError:  # Python before 3.9 has no usedforsecurity argument
        digest = hashlib.md5(data).hexdigest()
    except ValueError as e:  # MD5 is blocked
        print(f"md5-verifier: MD5 is not available: {e}", file=sys.stderr)
        return 1
    verifier = "md5" + digest
    if not re.fullmatch(r"md5[0-9a-f]{32}", verifier):
        print("md5-verifier: bad verifier", file=sys.stderr)
        return 1
    sys.stdout.write(verifier)
    return 0


if __name__ == "__main__":
    sys.exit(main())
