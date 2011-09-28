from leechyremote.discover import BonjourServer
from leechyremote.mediaplayer import MediaPlayersControlServer
from leechyremote import settings


def main():
    server = MediaPlayersControlServer(
            (settings.SERVER_ADDR, settings.SERVER_PORT))
    server.start()
    bonjour_server = BonjourServer(settings.SERVICE_NAME,
            settings.SERVICE_REG_TYPE, server.server_port)
    bonjour_server.start()
    print "Server listening on %s:%s" % (server.server_host,
            server.server_port)
    server.serve_forever()
