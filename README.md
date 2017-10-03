# Beer Game

Implementation of the [Beer
Game](https://en.wikipedia.org/wiki/Beer_distribution_game) as a
webapp.

## Technologies

A List of notable technology used.

- Server Side
  - [Clojure](http://clojure.org)
  - [HTTP Kit](http://www.http-kit.org/index.html)
  - [Websockets](https://github.com/ptaoussanis/sente/)
- Client Side
  - [ClojureScript](https://clojurescript.org/)
  - [React](https://reactjs.org/)
  - [Re-Frame](https://github.com/Day8/re-frame)
  - [Semantic UI](https://semantic-ui.com)

For a complete list, please look at [build.boot](build.boot) under the
keyword `:dependencies`.

## Development & Building

### Requirements

[The Boot Build Tool](http://boot-clj.com/) has to be installed

### Development Mode

Run the following command in the project directory:
```
boot dev
```
This will start a development server with auto-reloading code, as well
as a repl with project-context (which can be connected to using the
printed port). The webapp can then be accessed on port `3000` by default.

### Production Build

Run the following command in the project directory:
```
boot package
```
This will create a `jar` file in the `target` directory with all
dependencies included.

## Running
Download or build `beer-game.jar` and run it using:
```
java -jar beer-game.jar
```
Note that there has to be a file called `server-config.edn` in the
directory from which the jar is run from. This file contains some
private configuration details, like the password for the game leader.
See [server-config.edn](server-config.edn) for an example of the file
structure and the included options.
