### DESCRIPTION

Simple [netcat](http://en.wikipedia.org/wiki/Netcat) implementation for Android. It can be used for direct transfer character data between computer and Android device.
 
Netcat binary (nc) is included to "netcat-openbsd" or "netcat-traditional" packages (for Debian/Ubuntu).
All examples is written for nc from netcat-openbsd package because it is default (at least for Ubuntu 12.04/14.04).

### EXAMPLES

#### Listen for inbound connection

```
$ nc -v -l 9999
```

Assuming that computer IP address is 192.168.1.2 one can connect with Simple NetCat using "192.168.1.2:9999" as "Connect to" string.

#### Connect to remote host

```
$ nc -v host|ip-address 9999
```

Start listening port 9999 with Simple NetCat before executing this command (use your Android device IP-address). 
To determine mobile device IP-address one can use [ipconfig](https://play.google.com/store/apps/details?id=com.mankind.ipconfig) application.

#### Write data to file

```
nc -v -l 9999 > file.out
```

Data sent from Simple NetCat will be saved in "file.out".


#### Read data from file

```
nc -v -l 9999 < file.in
```

"file.in" content will be received by Simple NetCat.

### COMMENTS

Simple NetCat will receive data from computer after terminating nc binary (with Ctrl-C).