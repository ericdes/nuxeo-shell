Use nxclient.sh or nxshell.sh
'./nxshell.sh' is equivalent to './nxclient.sh interactive'

Usage:
    ./nxclient.sh [-dev] [-clear] [interactive] [command] [-h server_ip] [-d]
    ./nxshell.sh [-h server_ip] [-d]

Options:
    -dev 
        Developer mode (JVM listening for transport dt_socket at address: 8788).
        Not fully implemented in 5.1
    -clear
        Clear classpath cache
    interactive
        Console mode.
    command
        Any valid Nuxeo Shell command (such as 'ls', '--script file.js', ...).
        See http://doc.nuxeo.org/5.1/books/nuxeo-book/html/nuxeo-shell.html
    -h server_ip
        Connect on Nuxeo Core listening on server_ip. Use at least '-h localhost'.
    -d
        Debug mode. Logging switched from INFO to DEBUG.
        See 'log' command for more options about logging.
