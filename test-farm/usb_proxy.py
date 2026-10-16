"""A minimal HTTP proxy for giving farm phones network access over USB.

The farm runs it on this PC, `adb reverse`s its port onto a phone and sets the
phone's global HTTP proxy to 127.0.0.1:<port>, so apps that honour the system
proxy (OkHttp does) reach the internet through the PC with Wi-Fi off. It
handles CONNECT (HTTPS tunnels, which is all the app uses) and plain HTTP.
"""

import select
import socket
import socketserver
import threading
import time
from urllib.parse import urlsplit

PORT = 8118


class _Handler(socketserver.StreamRequestHandler):
    timeout = 60

    def handle(self):
        line = self.rfile.readline(65537).decode("latin-1")
        try:
            method, target, version = line.split()
        except ValueError:
            return
        headers = []
        while True:
            h = self.rfile.readline(65537)
            if h in (b"\r\n", b"\n", b""):
                break
            headers.append(h)
        if method == "CONNECT":
            host, _, port = target.rpartition(":")
            try:
                upstream = socket.create_connection((host, int(port)), timeout=30)
            except OSError:
                self.wfile.write(b"HTTP/1.1 502 Bad Gateway\r\n\r\n")
                return
            self.wfile.write(b"HTTP/1.1 200 Connection established\r\n\r\n")
            self.wfile.flush()
            leftover = self.rfile.read1(65536) if self.rfile.peek(1) else b""
            if leftover:
                upstream.sendall(leftover)
        else:
            url = urlsplit(target)
            try:
                upstream = socket.create_connection((url.hostname, url.port or 80), timeout=30)
            except OSError:
                self.wfile.write(b"HTTP/1.1 502 Bad Gateway\r\n\r\n")
                return
            path = url.path or "/"
            if url.query:
                path += "?" + url.query
            upstream.sendall(f"{method} {path} {version}\r\n".encode("latin-1"))
            upstream.sendall(b"".join(h for h in headers
                                      if not h.lower().startswith(b"proxy-")) + b"\r\n")
        self._pipe(self.connection, upstream)

    @staticmethod
    def _pipe(a, b):
        socks = [a, b]
        try:
            while True:
                ready, _, _ = select.select(socks, [], [], 120)
                if not ready:
                    return
                for s in ready:
                    data = s.recv(65536)
                    if not data:
                        return
                    (b if s is a else a).sendall(data)
        except OSError:
            pass
        finally:
            b.close()


class _Server(socketserver.ThreadingTCPServer):
    daemon_threads = True
    allow_reuse_address = True


def start(port=PORT):
    """Starts the proxy on 127.0.0.1 in a background thread. Retries for a
    while if the port is busy, as it is while a farm that's just been
    stopped is still shutting down."""
    for attempt in range(30):
        try:
            server = _Server(("127.0.0.1", port), _Handler)
            break
        except OSError:
            if attempt == 29:
                raise
            time.sleep(1)
    threading.Thread(target=server.serve_forever, daemon=True, name="usb-proxy").start()
    return server
