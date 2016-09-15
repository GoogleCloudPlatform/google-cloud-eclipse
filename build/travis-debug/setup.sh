#!/bin/sh
# Script that (re-)creates a VNC session

# Cleanup X11 sockets from previous run
rm -f /tmp/.X1-lock

# Fork the X11 session; startup script in $HOME/.vnc/xstartup
vncserver :1 -geometry 1280x800 -depth 24 > $HOME/.vnc/vncserver.log 2>&1
sleep 3
#metacity &
#sleep 3

echo ">>> $*"
eval '"$@"'
