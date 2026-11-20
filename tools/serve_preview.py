"""Static preview server that never lets a browser reuse a stale renderer."""
import functools
import http.server
import os


class NoCacheHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header('Cache-Control', 'no-store, no-cache, must-revalidate, max-age=0')
        self.send_header('Pragma', 'no-cache')
        self.send_header('Expires', '0')
        super().end_headers()

    def log_message(self, fmt, *args):
        print('%s - %s' % (self.address_string(), fmt % args), flush=True)


if __name__ == '__main__':
    here = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    os.chdir(os.path.join(here, 'preview'))
    handler = functools.partial(NoCacheHandler, directory=os.getcwd())
    http.server.ThreadingHTTPServer(('0.0.0.0', 4173), handler).serve_forever()
