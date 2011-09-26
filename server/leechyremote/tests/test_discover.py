from nose.tools import assert_equal
import random
from leechyremote.discover import BonjourServer
import gevent
from gevent import select
import pybonjour


def test_discover():
    ref = {
        "name": "Leechy",
        "regtype": "_leechyremote._tcp.",
        "port": random.randint(4096, 65000),
    }
    greenlets = []

    # Start service
    server = BonjourServer(**ref)
    server.start()
    greenlets.append(server)

    # Browse for service
    data = {
        "name": None,
        "regtype": None,
        "port": None,
    }

    def resolve_callback(fd, flags, interface_index, error_code, name,
                         hosttarget, port, txt_record):
        if error_code == pybonjour.kDNSServiceErr_NoError:        
            data["port"] = port
            for greenlet in greenlets:
                greenlet.kill()

    def resolve(interface_index, name, regtype, reply_domain):
        resolve_fd = pybonjour.DNSServiceResolve(0, interface_index,
                name, regtype, reply_domain, resolve_callback)
        try:
            while True:
                ready = select.select([resolve_fd], [], [])
                if resolve_fd not in ready[0]:
                    raise Exception('Resolve timed out')
                pybonjour.DNSServiceProcessResult(resolve_fd)
        finally:
            resolve_fd.close()

    def browse_callback(fd, flags, interface_index, error_code, name,
                        regtype, reply_domain):
        global resolver
        if error_code != pybonjour.kDNSServiceErr_NoError:
            raise Exception()
        if not (flags & pybonjour.kDNSServiceFlagsAdd):
            raise Exception('Service removed')
            return
        data["name"] = name
        data["regtype"] = regtype
        resolver = gevent.spawn(resolve, interface_index, name,
            regtype, reply_domain)
        greenlets.append(resolver)

    def browse():
        browse_fd = pybonjour.DNSServiceBrowse(regtype=ref["regtype"],
                callBack = browse_callback)
        try:
            while True:
                ready = select.select([browse_fd], [], [])
                if browse_fd in ready[0]:
                    pybonjour.DNSServiceProcessResult(browse_fd)
        finally:
            browse_fd.close()

    browser = gevent.spawn(browse)
    greenlets.append(browser)

    gevent.joinall(greenlets, raise_error=True)

    for name in ref:
        if ref[name] != data[name]:
            raise AssertionError("%s != %r, got %r" % 
                    (name, ref[name], data[name]))
