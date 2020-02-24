# Sea Battle CorDapp

## How to start the app

Run these commands:

```sh
# Build and prepare corda nodes
./gradlew clean deployNodes

# Run corda nodes
./build/nodes/runnodes
```

## How to play

```sh
# Start a new game
flow start NewGameInitiator name: a, p2: Player2

# Set positions
# Each position is a coordinate followed by a compass direction for the end of the ship (N,E,S,W)
flow start SetPosInitiator gameName: a, aircraftCarrier: A0E, battleship: A1E, cruiser: A2E, destroyer1: A3E, destroyer2: A4E, submarine1: A5E, submarine2: A6E

# Print the current game state (press ctrl+c to finish the command)
flow start PrintGameInitiator gameName: a

# The turn command takes a game name and a coordinate to target
flow start TurnInitiator gameName: a, coordinate: A0
```

## Debug commands

These commands will allow you to inspect the game state in your vault.

```
run vaultQuery contractStateType: com.template.states.GameState
run vaultQuery contractStateType: com.template.states.PositionState
```
