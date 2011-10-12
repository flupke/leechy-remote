from gevent.server import StreamServer
from gevent import socket
from leechyremote import settings


class SMPlayerInterface(object):

    actions_map = {
        "play_pause": "play_or_pause",
        "volume_up": "increase_volume",
        "volume_down": "decrease_volume",
        "mute": "mute",
        "play_previous": "pl_prev",
        "play_next": "pl_next",
        "forward_1": "forward1",
        "forward_2": "forward2",
        "forward_3": "forward3",
        "rewind_1": "rewind1",
        "rewind_2": "rewind2",
        "rewind_3": "rewind3",
        "toggle_fullscreen": "fullscreen",
    }

    def __init__(self):
        self.socket = None

    def _connect(self):
        if self.socket is None:
            print "connect %r" % (settings.SMPLAYER_ADDRESS,)
            self.socket = socket.create_connection(settings.SMPLAYER_ADDRESS)

    def _send(self, line):
        try:
            self._connect()
            print repr("%s\n" % line)
            self.socket.send("%s\n" % line)
        except IOError, err:
            self.socket = None
            print "Error: %s" % err

    def open(self, path):
        self._send("open %s" % path)

    def queue(self, path):
        raise NotImplementedError()

    def send_action(self, action, *params):
        action_str = self.actions_map[action]
        if params:
            action_str += " " + " ".join(params)
        self._send("f %s" % action_str)


media_player_interfaces = {
    "SMPLAYER": SMPlayerInterface(),
}


class MediaPlayersControlServer(StreamServer):
    """
    A server used to control media players.

    The protocol consists of command lines, e.g.::
        
        SMPLAYER OPEN /path/to/file

    Each line is made of a media player code name, the class of the command
    send to the player, and additional arguments relevant to the particular
    command.

    The only supported media player is 'SMPLAYER' for now.

    Command class may be one of 'OPEN', 'QUEUE' or 'ACTION'.
    """

    def handle(self, socket, address):
        fp = socket.makefile()
        while True:
            line = fp.readline()
            if not line:
                break
            player, command, args = line.strip().split(" ", 2)
            interface = media_player_interfaces[player]
            if command == "OPEN":
                interface.open(args)
            elif command == "QUEUE":
                interface.queue(args)
            elif command == "ACTION":
                interface.send_action(*args.split())
