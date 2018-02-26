import os, json

class FileScanner(object):
    def __init__(self, zebedee_root=None):
        if (zebedee_root == None):
            zebedee_root = os.getenv("zebedee_root")
        self.zebedee_root = zebedee_root

        if (self.zebedee_root == None):
            raise ValueError("zebedee_root cannot be None")

    def scan(self):
        for path, subdirs, files in os.walk(self.zebedee_root):
            if (len(files) > 0):
                for fname in files:
                    if "data.json" == fname:
                        yield "%s/%s" % (path, fname)

    @staticmethod
    def load_page(fname):
        page = None
        with open(fname) as f:
            page = json.load(f)
        return page

    def load_pages(self):
        return [self.load_page(fname) for fname in self.scan()]

    def yield_pages(self):
        for fname in self.scan():
            yield self.load_page(fname)
