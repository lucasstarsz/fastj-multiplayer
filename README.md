# Party House

Party House is a multiplayer game built using Java, the [FastJ](https://github.com/fastjengine/FastJ) game engine, and a self-written multiplayer library.

The game's port is 19999.

## How to Play

### Running your own Server
It's as easy as downloading the latest server release, and running it.
There's currently no frontend, and it writes to a logfile.

### Running the client
On the client side, download the client release matching the server's version.
Once opened, click the `Set Server IP` button and type the IP of the computer host.
This is `localhost` if you're running the server on the same pc as the client.

Once all players are in the lobby, click the button in the botton right to confirm.
If all players are confirmed, the game will begin!

https://user-images.githubusercontent.com/64715411/181523676-b83c3428-7ae2-468c-93fe-5748814e764f.mp4

## Version Logs

#### v0.0.1
- First prototype made for Java Community's Java Jam 2022
- Engine is occasionally spotty with null rendering issues
- reading/writing arrays through the network serializer(s) does not work as intended
- lobbies, sessions, and demo gameplay of Snowball Fight have been implemented

