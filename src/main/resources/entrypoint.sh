#!/bin/sh

show_help() {
  echo "Usage : ./entrypoint.sh [-d] [-s] [/bin/bash]"
  echo "      : The -d flag enables debugging without suspending first"
  echo "      : The -s flag enables debugging suspending the JVM upon startup"
  echo "      : If the word '/bin/bash' is provided as a parameter, BASH is executed instead."
}

OPTIND=1         # Reset in case getopts has been used previously in the shell.
debug=0
pause=0
DEBUG_ARGS=""
while getopts "h?ds" opt; do
    case "$opt" in
    h|\?)
        show_help
        exit 0
        ;;
    d)  debug=1
        ;;
    s)  pause=1
        ;;
    esac
done

shift $((OPTIND-1))

[ "$1" = "--" ] && shift

if [ $debug == 1 ]; then
  export LOGLEVEL=DEBUG  # If we are connecting a debugger, lets also have debug logging output
  DEBUG_ARGS="-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=n"
elif [ $pause == 1 ]; then
  export LOGLEVEL=DEBUG  # If we are connecting a debugger, lets also have debug logging output
  DEBUG_ARGS="-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=y"
fi

if [ "$1" == "/bin/bash" ]; then
  exec /bin/sh
else
  exec java ${DEBUG_ARGS} -jar wcfc-quiz.jar server configuration.ftl
fi
