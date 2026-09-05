#!/bin/bash

# Port for the local server
PORT=4311

echo "Starting BioCue Redesign Prototype Server on port $PORT..."

# Check if something is already running on the port and kill it if necessary
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null ; then
    echo "Port $PORT is already in use. Shutting down the old server..."
    kill -9 $(lsof -Pi :$PORT -sTCP:LISTEN -t)
fi

# Start the python HTTP server in the background
# We serve from the root of the 'designs' directory so the URL matches
cd "$(dirname "$0")/.."
python3 -m http.server $PORT --directory . &
SERVER_PID=$!

echo "Server started with PID: $SERVER_PID"
echo ""
echo "====================================================="
echo "   BioCue Prototype is live!"
echo "   Opening http://localhost:$PORT/biocue-redesign/App.html"
echo "====================================================="
echo ""
echo "Press Ctrl+C to stop the server."

# Open the browser (macOS)
open "http://localhost:$PORT/biocue-redesign/App.html"

# Wait for the user to press Ctrl+C
wait $SERVER_PID
