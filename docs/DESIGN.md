# CEA Design Document

## Features

### Max eye limit

> As a player, I want to limit the number of eyes that I can place so that no one player can enter the End.

- [ ] Limit the number of eyes placeable in a particular End Portal frame structure per player.
- [ ] Reject attempts to place new eye if player is at or over their limit.
- [ ] Provide negative feedback to attempts to place more than allowed.
- [ ] Provide positive feedback for successful attempts to place an eye.

### Eye removal

> As a player, I want to take back my eye in case I changed my mind about this portal or want to let other players place their eyes.

- [ ] Record ownership of eye placement in frame.
- [ ] Allow players to remove their own eye from a frame.
- [ ] Reject players from removing eyes that they do not own from a frame.
- [ ] Provide negative feedback to attempts to remove an eye that they do not own detailing who has the authority to remove it.
- [ ] Provide positive feedback for successful attempts to remove an eye.

### Portal opening

> As a committed player, I want the portal to open when all twelve End Portal Frames are filled with eyes (regardless of who placed them) AND all committed players are present.

- [ ] Trigger portal eligibility state when twelve eyes fill the portal.
- [ ] Open portal when all committed players to that portal are within distance N of the portal center.
- [ ] Provide persistent feedback to committed players with list of names of committed players that must be present.
- [ ] Provide persistent feedback to committed players about of the status of the nearby portal.

> As a committed player, I want to make sure that the portal does not collapse during an ongoing fight so I can rejoin the fight quickly.

- [ ] Keep portal open as long as a committed player is present in the End dimension.
- [ ] Provide feedback detailing who is a committed player in the End dimension.

### Dragon death

> As a player, I want to be able to open End Portals normally after beating the Dragon, since I don't need to worry about anyone soloing the Dragon anymore.

- [ ] Re-open any collapsed portals upon dragon death and return to standard functionality.
- [ ] Provide feedback stating that portals are permanently opened.

### Server administrators

> As a server administrator, I want to configure the range and content of feedback so that I can alter my players' experience as I wish.

- [ ] Provide configuration variables for range of action bar feedback
- [ ] Provide configuration variables for range of chat feedback
- [ ] Provide configuration variables for content of action bar feedback
- [ ] Provide configuration variables for content of chat feedback
- [ ] Provide configuration variables for various sounds: volume, pitch, etc.