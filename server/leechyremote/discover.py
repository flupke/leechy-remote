from gevent import Greenlet, select
import pybonjour
from leechyremote.exceptions import ServiceRegistrationError


class BonjourServer(Greenlet):

    def __init__(self, name, regtype, port):
        super(BonjourServer, self).__init__()
        self.stopped = False
        self.service_fd = pybonjour.DNSServiceRegister(name=name, regtype=regtype,
                port=port, callBack=self.register_callback)

    def _run(self):
        while not self.stopped:
            ready = select.select([self.service_fd], [], [])
            if self.service_fd in ready[0]:
                pybonjour.DNSServiceProcessResult(self.service_fd)
        self.service_fd.close()

    def stop(self):
        self.stopped = True

    def register_callback(self, service, flags, error_code, name, regtype, domain):
        if error_code != pybonjour.kDNSServiceErr_NoError:
            raise ServiceRegistrationError("can't register service '%s', "
                    "error code: %s" & (regtype, error_code))
