# Last.FM Universal Scrobbler with Discord Support
This application automatically scrobbles music information from any
supported music application.

For a list of supported applications, check out the `plugins` folder.
You may also create your own plugins using the application itself.

This application also allows for the updating of the Discord Game
status to show `Playing {{Song}}` instead of a video game as expected.

## Getting Started
There are two methods to use this program. Either you can download the
source, add your own Last.FM API key and secret, and compile it through
Maven, OR you can use the included jar file found in the `compiled`
folder.

#### Manually Compile (skip to `No Compile` if you will download the JAR file instead)

You will need to retrieve a Last.FM API key/secret combination from
[here](https://www.last.fm/api/account/create).

Please also read the API rules to ensure that any additions to the code
does not break any rules stated by Last.FM. The same goes for Discord
if you plan on using more Discord API functionalities.

Paste the key and secret in the class
`com.arkazeen.DiscordLastFMScrobbler.LastFM` for the variables
`LAST_FM_API_KEY` and `LAST_FM_API_SECRET`.

To compile the jar with dependencies using Maven, execute
`mvn clean compile assembly:single`.

#### No Compile

Download the JAR file from the `compiled` folder. This uses my own
Last.FM API key and secret.

## Configuration
You must input your Last.FM username and password when prompted to use
Last.FM scrobble functionality. To use Discord Game update
functionality, you will also have to follow the on-screen instructions
to retrieve the token.

## TODO (in no particular order):
* Show Last.FM recent plays menu
* Add right-click plugin save/edit buttons
* Minimize to tray rather than always visible on taskbar
* Experiment with JNA to get active windows rather than using command prompt
* Allow for custom song format to be created by user
* Add more general logging methods

## Built With
* [Maven](https://maven.apache.org/) - Dependency Management
* [Org.JSON](https://github.com/stleary/JSON-java) - JSON Parser and Creator
* [Apache HTTP Components](https://hc.apache.org/) - HTTP Client Connections
* [Apache Commons Validator](https://commons.apache.org/proper/commons-validator/) - URL Verification
* [Java WebSocket](https://github.com/TooTallNate/Java-WebSocket) - Java WebSocket Connection