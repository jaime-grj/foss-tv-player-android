# FOSS TV Player [WIP]
Live TV streams player for Android and Android TV.

This app aims to create a simple interface for watching live streaming channels and sorting them in a TV-style list.

It reads a JSON file and stores the channels in the app.

The JSON file must be created by reverse engineering channel APIs. It supports JSON API REST, getting URL from HTML page directly, setting custom headers, GET and POST methods, playing streams with DRM ClearKey. An example.json file is provided (WIP).

Channels are stored in categories. Each channel will have a category number and can have a "favorite" number. The app will create a default "Favorites" category.

The user can browse channels up/down and by entering channel number.
