#!/usr/bin/env python3
"""Print which password method a Postgres server asks a user for.

Sends a startup message and reads the first reply. It never sends a password,
so it needs no credentials and changes nothing.

Usage: probe-auth.py HOST PORT USER DATABASE

Output is one line: "authType 5: MD5" or "authType 10: SASL (SCRAM)".
"""
import socket
import struct
import sys

NAMES = {0: "trust (ok)", 3: "cleartext password", 5: "MD5", 10: "SASL (SCRAM)"}


def recv_exact(s: socket.socket, n: int) -> bytes:
    """Read n bytes, or fewer if the server closes the connection first.

    One recv() call can return less than n bytes, because TCP can split a header.
    """
    data = b""
    while len(data) < n:
        chunk = s.recv(n - len(data))
        if not chunk:
            break
        data += chunk
    return data


def main() -> int:
    if len(sys.argv) != 5:
        print(__doc__)
        return 64
    host, port, user, db = sys.argv[1], int(sys.argv[2]), sys.argv[3], sys.argv[4]
    body = b"user\0" + user.encode() + b"\0database\0" + db.encode() + b"\0\0"
    msg = struct.pack("!ii", 8 + len(body), 196608) + body
    try:
        with socket.create_connection((host, port), timeout=10) as s:
            s.sendall(msg)
            head = recv_exact(s, 9)
    except OSError as e:  # includes timeouts
        print(f"probe failed: {e}")
        return 3
    if len(head) < 9 or head[0:1] != b"R":
        print(f"unexpected reply: {head!r}")
        return 2
    code = struct.unpack("!i", head[5:9])[0]
    print(f"authType {code}: {NAMES.get(code, 'other')}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
