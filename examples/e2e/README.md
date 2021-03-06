# Unidirectional JSON-RPC from Scala JS to Scala JVM over HTTP

In this example, we will show how to implement JSON-RPC client on Scala JS and JSON-RPC server on Scala JVM and how to let them communicate via JSON-RPC APIs.

You can see the complete code under this directory, but we have documented some highlights below.

## JSON-RPC API

We define the following 3 JSON-RPC APIs.

```scala
trait CalculatorAPI {
  def add(lhs: Int, rhs: Int): Future[Int]
  def subtract(lhs: Int, rhs: Int): Future[Int]
}

trait EchoAPI {
  def echo(message: String): Future[String]
}

trait LoggerAPI {
  def log(message: String): Unit
}
```

## JSON-RPC server

We implement the APIs on server side like below.

```scala
class CalculatorAPIImpl extends CalculatorAPI {
  override def add(lhs: Int, rhs: Int): Future[Int] = {
    Future(lhs + rhs)
  }
  override def subtract(lhs: Int, rhs: Int): Future[Int] = {
    Future(lhs - rhs)
  }
}

class EchoAPIImpl extends EchoAPI {
  override def echo(message: String): Future[String] = {
    Future(message) // It just returns the message as is
  }
}

class LoggerAPIImpl extends LoggerAPI {
  override def log(message: String): Unit = {
    println(message) // It logs the message
  }
}
```

We build JSON-RPC server using those API implementations.

```scala
object JSONRPCModule {
  lazy val jsonRPCServer: JSONRPCServer[UpickleJSONSerializer] = {
    val server = JSONRPCServer(UpickleJSONSerializer())
    server.bindAPI[CalculatorAPI](new CalculatorAPIImpl)
    server.bindAPI[EchoAPI](new EchoAPIImpl)
    server.bindAPI[LoggerAPI](new LoggerAPIImpl)
    server
  }
}
```

To expose HTTP end point on the server, we are using [Scalatra](http://www.scalatra.org). We expose POST /jsonrpc end point to receive JSON-RPC request and notification.

```scala
class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext): Unit = {
    context.mount(new JSONRPCServlet, "/jsonrpc/*")
  }
}

class JSONRPCServlet extends ScalatraServlet {
  post("/") {
    val server = JSONRPCModule.jsonRPCServer
    val futureResult: Future[ActionResult] = server.receive(request.body).map {
      case Some(responseJSON) => Ok(responseJSON) // For JSON-RPC request, we return response.
      case None => NoContent() // For JSON-RPC notification, we do not return response.
    }
    Await.result(futureResult, 1.minutes)
  }
}
```

## JSON-RPC client

On client side, we are using Ajax to send JSON-RPC request and notification. If the server responded 204 (no content), it is JSON-RPC notification.

```scala
val jsonSender: (String) => Future[Option[String]] =
  (json: String) => {
    val NoContentStatus = 204
    dom.ext.Ajax
        .post(url = "/jsonrpc", data = json)
        .map(response => {
          if (response.status == NoContentStatus) {
            None
          } else {
            Option(response.responseText)
          }
        })
  }

val client = JSONRPCClient(UpickleJSONSerializer(), jsonSender)
```

Once the client is built, we can use it to create and use the APIs like below.

```scala
val calculatorAPI = client.createAPI[CalculatorAPI]
val echoAPI = client.createAPI[EchoAPI]
val loggerAPI = client.createAPI[LoggerAPI]

loggerAPI.log("This is the beginning of my example.")

calculatorAPI.add(1, 2).onComplete {
  case Success(result) => println(s"1 + 2 = $result")
  case _ =>
}

calculatorAPI.subtract(1, 2).onComplete {
  case Success(result) => println(s"1 - 2 = $result")
  case _ =>
}

echoAPI.echo("Hello, World!").onComplete {
  case Success(result) => println(s"""You said "$result"""")
  case _ =>
}

loggerAPI.log("This is the end of my example.")
```

When you run this, you will see that:

- calculations via ```calculatorAPI``` is operated on server and returned to client.
- messages sent via ```echoAPI``` reaches to server and returned to client as is.
- messages sent via ```loggerAPI``` is logged on server.
