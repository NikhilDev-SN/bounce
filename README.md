# Bounce Recharged (Java)

A modernized Java/Swing take on the classic Bounce-style game.

## Features
- Side-scrolling platform gameplay with bounce physics
- Detailed ball-feel tuning: acceleration, drag, ground friction, restitution-based rebounds, and angular roll
- Coins and combo scoring
- Enemies and spike hazards
- Smoother edge jumping with jump-buffer + coyote-time handling
- Reliable jump input even while moving left/right
- Jump buffering now supports rapid spam/hold for near-instant jump response
- Corner correction for less frustrating platform-edge head bumps
- Ball now visibly rolls (spin seam) and shows squash/bounce feedback on landings
- Modern power-ups:
  - Shield (one-hit protection)
  - Slow Motion (temporary gameplay slowdown)
  - Double Jump unlock
- Multiple environments in a single run:
  - Neon City
  - Sky Ruins (lighter gravity + light wind)
  - Volcanic Core (heavier gravity)
  - Crystal Cave
- HUD with score, high score, lives, and active abilities

## Controls
- `A / D` or `← / →`: move
- `W`, `↑`, or `Space`: jump
- `R`: restart after win/lose

## Run
```bash
javac BounceGame.java
java BounceGame
```
