Consuming a camera stream
=========================


# From a local source

The entry point is
[LocalWebcamWindow](src/main/scala/fr/xebia/streams/LocalWebcamWindow.scala).

You need at least a camera attached to your computer.

# From a remote source

The entry point is
[RemoteWebcamWindow](src/main/scala/fr/xebia/streams/RemoteWebcamWindow.scala).

## Configuring raspberry's flow

In order to consume the video flow you need to change the following ip accordingly.

```
http {
  host = "192.168.0.17"
}
```

By the time being, we only support the following implementations:

* [RPiCamWebInterface](https://github.com/silvanmelchior/RPi_Cam_Web_Interface)

You are free to implement any other implementation by extending the trait [RemoteProvider](src/main/scala/fr/xebia/streams/video/RemoteProvider.scala).

# How run any of the examples?

Just type the following commands

```
$ sbt 
$ run
```

Then you need to choose which class you want to execute.

