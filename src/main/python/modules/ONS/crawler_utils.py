import urllib
_BASE_URL = "http://localhost:20000/"

def _buildUrl(baseUrl, searchTerm, size, **kwargs):
    if (baseUrl.endswith("/")):
        baseUrl = baseUrl[:-1]
    searchUrl = "%s/search?q=%s&size=%d" % (baseUrl, urllib.quote_plus(searchTerm), size)

    for key in kwargs:
        searchUrl += "&%s=%s" % (key, kwargs[key])
    return searchUrl

def _search(searchUrl):
    import urllib2
    from bs4 import BeautifulSoup

    page = urllib2.urlopen(searchUrl)
    soup = BeautifulSoup(page)

    return soup

def search(searchTerm, baseUrl=_BASE_URL, size=10, verbose=False, **kwargs):
    # Performs a search on the website
    searchUrl = _buildUrl(baseUrl, searchTerm, size, **kwargs)
    if (verbose): print "Getting page ", searchUrl
    return _search(searchUrl)

def sltr(searchTerm, model, baseUrl=_BASE_URL, size=10, **kwargs):
    return search(searchTerm, baseUrl=baseUrl, size=size, sortBy="ltr", model=model, **kwargs)