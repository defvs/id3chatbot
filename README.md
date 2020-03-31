# ID3Chatbot

## Compiling and launching

This project uses gradle to build with a Kotlin DSL config file.
You can build it using your IDE or use ``./gradlew shadowJar``, this will output the jar file to ``build/libs/id3chatbot-version-all.jar``

Run using ``java -jar id3chatbot.jar [pathToConfigFile]``

## Configuration

If you start for the first time with a valid path, it will ask you to create a new file with the default values. Accept this, then open the file and change the settings. Here is the default file:

```
{
    "id3chatbot" : {
        "authToken" : "",
        "mountpoint" : "",
        "commands" : [],
        "authorizedUserIDs" : [],
        "timedMessages" : []
    }
}
```

You need to fill in ``authToken`` and ``mountpoint`` for the software to work. The other values can be changed directly when the bot is running via commands.

* ``authToken`` can be found using your browser console in "Network" mode while on a live, refreshing, and fiding the "Websocket" or WS. One of the first message of the websocket will include both the token and the mountpoint
* ``mountpoint`` can be found with the same technique as above.

## Default commands

* ``!about``: returns informations about the bot.
* ``!ping``: Tests the bot; returns Pong!
* ``!addmod``: Adds a new user to the modlist. Requires the sender to be a mod as well (only exception is when no mod is present in the list, where it will allow you to add one entry without being mod). Usage: ``!addmod [username or id]``
* ``!addcommand``: Creates a new command for the bot to respond to. See "Creating commands"
* ``!addtimedmessage``: Creates a new message that will be sent on a timed interval. Usage: ``!addtimedmessage [interval in milliseconds] [message]``

## Creating commands

Using ``!addcommand [keyword] [message]``, you can create a new command.

Commands support arguments, which should be written in the message when creating the command. Arguments are like ``%arg0%``, ``%arg1%``...

The user will be passing arguments like ``[your keyword] [arg0] [arg1]...``. For now, arguments are limited to being space-less. If there is a space, it'll count as the next args.

Some variables are available as well:
* ``%username%`` which will return the username of the command sender.
* ``%firstname%`` same for firstname
* More soon

**_NOTE_**: _when creating a command, ``[keyword]`` does not make the command work when saying ``![keyword]`` but will respond when it sees ``[keyword]`` at the start of the message. You can use your own prefixes like ! or $ or anything!_

A command might require mod permission, you need to edit the config file directly and change ``elevated`` to true.