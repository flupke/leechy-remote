import gevent
from gevent import socket
import mox
from leechyremote import mediaplayer


def test_server():
    # Save original SMPlayer interface
    old_smplayer_if = mediaplayer.media_player_interfaces["SMPLAYER"]
    # Mock SMPlayerInterface
    mediaplayer_mocker = mox.MockObject(mediaplayer.SMPlayerInterface)
    mediaplayer_mocker.open("/path/to/file")
    mediaplayer_mocker.queue("/path/to/file")
    mediaplayer_mocker.send_action("volume_up")
    mox.Replay(mediaplayer_mocker) 
    mediaplayer.media_player_interfaces["SMPLAYER"] = mediaplayer_mocker
    # Start server
    server = mediaplayer.MediaPlayersControlServer(("localhost", 0))
    server.start()
    # Send commands to the server
    def send_commands():
        conn = socket.create_connection(("localhost", server.server_port))
        conn.send("SMPLAYER OPEN /path/to/file\n")
        conn.send("SMPLAYER QUEUE /path/to/file\n")
        conn.send("SMPLAYER ACTION volume_up\n")
        conn.close()
    client = gevent.spawn(send_commands)
    client.join()
    # Stop server and verify commands were executed correctly
    server.kill()
    mox.Verify(mediaplayer_mocker)
    # Restore SMPlayer interface
    mediaplayer.media_player_interfaces["SMPLAYER"] = old_smplayer_if
