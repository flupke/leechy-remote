import subprocess
from gevent.server import StreamServer
from leechyremote import settings


class CommandLineInterface(object):
    """
    Interface to control programs via the command line.
    """
    
    action_arg = None
    actions_map = {}

    def call(self, *args):
        final_args = [self.get_executable()]
        final_args.extend(args)
        subprocess.call(final_args)

    def get_executable(self):
        raise NotImplementedError("subclasses of CommandLineInterface must "
                "implement the get_executable() method")

    def send_action(self, action, *params):
        mapped_action = self.actions_map.get(action, action)
        args = [self.action_arg, mapped_action]
        args.extend(params)
        self.call(*args)


class SMPlayerInterface(CommandLineInterface):

    action_arg = "-send-action"
    actions_map = {
        "play_pause": "play_or_pause",
        "volume_up": "increase_volume",
        "volume_down": "decrease_volume",
        "mute": "mute",
        "play_previous": "pl_prev",
        "play_next": "pl_next",
    }

    def get_executable(self):
        return settings.SMPLAYER_EXECUTABLE

    def open(self, path):
        self.send_action("pl_remove_all")
        self.queue(path)
        self.send_action("pl_play")

    def queue(self, path):
        self.call("-add-to-playlist", path)


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
        fd = socket.makefile()
        while True:
            line = fd.readline()
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
