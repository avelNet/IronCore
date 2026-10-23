#!/usr/bin/env bash
# Drive the dev Minecraft client inside a virtual X display (Xvfb) for automated in-game checks.
# Needs: xvfb, xdotool, imagemagick. Output (logs, screenshots) goes to run/autotest/.
#
#   mc.sh start            build + launch client in Xvfb, wait for the title screen
#   mc.sh enter            start + open the first singleplayer world, wait until in-game
#   mc.sh stop             kill client and Xvfb
#   mc.sh shot <name>      screenshot (cropped to the game window) -> run/autotest/shots/<name>.png
#   mc.sh cmd "<command>"  type a chat command, e.g. mc.sh cmd "/time set day"
#   mc.sh key <keys...>    tap keys (xdotool names: w, space, Control_L, F5 ...)
#   mc.sh hold <key> <s>   hold a key for <s> seconds
#   mc.sh dtap <key>       double-tap a key (100 ms presses — the client polls once per tick)
#   mc.sh click <x> <y> [button]   click at screen coords (1280x720 screen; game at +213+120)
#   mc.sh log [n]          tail the client log
set -u
DISP=:99
PROJ="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$PROJ/run/autotest"
LOG="$OUT/client.log"
mkdir -p "$OUT/shots"
export DISPLAY=$DISP

wait_for() { # wait_for <regex> <timeout_s>
  for _ in $(seq 1 "$2"); do grep -q -E "$1" "$LOG" 2>/dev/null && return 0; sleep 1; done; return 1
}

case "${1:-}" in
  start)
    "$0" stop >/dev/null
    pgrep -f "Xvfb $DISP" >/dev/null || { Xvfb $DISP -screen 0 1280x720x24 -nolisten tcp >"$OUT/xvfb.log" 2>&1 & sleep 2; }
    cd "$PROJ"
    ./gradlew build --console=plain >"$OUT/build.log" 2>&1 || { echo BUILD_FAILED; grep -E 'error:|warning:' "$OUT/build.log" | head -20; exit 1; }
    LIBGL_ALWAYS_SOFTWARE=1 nohup ./gradlew runClient --console=plain >"$LOG" 2>&1 &
    echo $! >"$OUT/client.pid"
    wait_for 'Sound engine started|BUILD FAILED|Crash report' 900
    grep -q 'Sound engine started' "$LOG" || { echo CLIENT_FAILED; tail -20 "$LOG"; exit 1; }
    sleep 15; echo TITLE
    ;;
  enter)
    "$0" start || exit 1
    "$0" click 638 355; sleep 3      # Singleplayer
    "$0" click 600 255; sleep 0.5    # first world in the list
    "$0" click 480 515               # Play Selected World
    wait_for 'joined the game' 120 || { echo JOIN_FAILED; exit 1; }
    sleep 8; echo READY
    ;;
  stop)
    [ -f "$OUT/client.pid" ] && kill "$(cat "$OUT/client.pid")" 2>/dev/null
    pkill -f 'cpw.mods.bootstraplauncher' 2>/dev/null
    pkill -f "Xvfb $DISP" 2>/dev/null
    echo stopped
    ;;
  shot)
    f="$OUT/shots/${2:-shot}.png"
    import -window root -display $DISP -crop 854x480+213+120 +repage "$f" && echo "$f"
    ;;
  cmd)
    xdotool key t; sleep 0.6; xdotool type --delay 30 "$2"; sleep 0.3; xdotool key Return; sleep 0.8
    ;;
  key)
    shift; for k in "$@"; do xdotool keydown "$k"; sleep 0.1; xdotool keyup "$k"; sleep 0.1; done
    ;;
  hold)
    xdotool keydown "$2"; sleep "$3"; xdotool keyup "$2"
    ;;
  dtap)
    xdotool keydown "$2"; sleep 0.1; xdotool keyup "$2"; sleep 0.1; xdotool keydown "$2"; sleep 0.1; xdotool keyup "$2"
    ;;
  click)
    xdotool mousemove "$2" "$3" click "${4:-1}"
    ;;
  log)
    tail -n "${2:-40}" "$LOG"
    ;;
  *)
    sed -n '2,17p' "$0"; exit 1 ;;
esac
