# Android Actors

An implementation of an actor system, strongly influenced by Akka, but implemented specifically for Android. Although the library is small at only 32kb of size, it does provide a lot of additional functionality beside basic functionality like starting, stopping, and sending messages to actors:
* creating multiple actor systems,
* support for different modes of actor threading (checkout [Executors](core/src/main/java/android/actor/Executors.java)),
* pausing actors,
* and creating child actors.

A simple example that uses this library to create an actor and send messages is shown next:
```java
import android.actor.Actor
import android.actor.Executors
import android.actor.Reference
import android.actor.System

// define an actor
public class Ping extends Actor<String> {

  public Ping() {
    super();
  }

  @Override
  protected final void onMessage(@NonNls @NonNull final String message) {
    if ("ping".equals(message)) {
      System.out.println("pong");
    }
  }
}

// create an actor system and use one thread for all actors
System system = new System(Executors.singleThread());
// register a ping actor with the system
Reference<String> ping = system.register("ping", new Ping());

// send messages to the ping actor
ping.tell("ping"); // should print "pong"
ping.tell("whatever"); // should not print anything

// stop the actor system and all actors that were registered with it
system.stop();
```

## Controlling execution

Both actor and actor system provide three methods to control the execution: `start`, `stop`, and `pause`. The `stop` method is self-explanatory; it stops an actor or an actor system and releases all resources associated with it. Stopping actor system also stops all actors that were registered with it. The `pause` pauses an actor or an actor system. When an actor is paused, it will not be executing, but it will accept messages, which will be delivered next time the actor is started. Pausing an actor system will pause all actors created with it and furthermore pause execution of its executor (i.e. it will stop execution of threads that are created specifically for the actor system).

## Actor callbacks

Beside having to implement the `onMessage` method in an actor, you can also override the `postStart` and `preStop` that are invoked when actor is started and stopped respectively. The default implementations of these two methods do not do anything.

## Child actors

Each actor receives a `Context` through its `postStart` method that can be used to create child actors using the `register` method. The special behaviour of child actors is that whenever their parent actor is stopped, they will be stopped as well.
