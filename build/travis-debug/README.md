# Docker simulation of Travis-CI build

This Docker image is useful for building and running UI tests via Maven.
It runs Maven under an X11 session with VNC using the Travis-CI `travis-jvm` container.

## Setup

You first need to create a docker image:
```
$ docker build -t travis-debug .
```

## Typical Usage

### Follow VNC during a build

Build the project in the current directory
```
$ docker run -it --rm \
    --name travis-build \
    -P \
    -v "$PWD":/home/travis/build \
    travis-debug
```
This does a few things:

  - `-P`: publishes exposed ports, including port 5901 for the VNC connection
  - `-v $PWD:/home/travis/build`: mounts the current directory (assuming
    you're in the repository root) to the build location
  - runs the default command line, `mvn -Ptravis --fail-at-end verify`

### Attach VNC

You need to find the mapped port for the container's 5901.  For example, on OS X:
```
$ docker port travis-build 5901
0.0.0.0:32768
$ open vnc://localhost:32768
```
The VNC password is `abc123`.

### Attach the Eclipse Debugger to the remote JVM

Build the project in the current directory
```
$ docker run -it --rm \
    -P -p 8000:8000 \
    -v "$PWD":/home/travis/build \
    travis-debug \
    mvn -DdebugPort=8000 verify
```
This does a few things:

  - `-P`: publishes exposed ports, including port 5901 for the VNC connection
  - `-p 8000:8000`: exposes container port 8000 as port 8000 on the
    local machine; you can connect a _Remote Java Application_ in Eclipse to
    _localhost_ on port 8000.


## Speeding up builds 

You can speed up your builds by mounting your ~/.m2 directory.

```
$ docker run -it --rm \
    -v "$HOME/.m2":/home/travis/.m2 \
    -v "$PWD":/home/travis/build \
    mvn verify
```
This can speed up Maven by:
  - using local mirror information in your `~/.m2/settings.xml`
  - using your local repository in `~/.m2/repository`

You may need to add `--add-host=hostname:IP` if your local mirror
can't be resolved over the network.  The image doesn't contain
a zeroconf/bonjour DNS mapper.
