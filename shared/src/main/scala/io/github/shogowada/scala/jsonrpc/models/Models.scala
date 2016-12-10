package io.github.shogowada.scala.jsonrpc.models

import scala.concurrent.Future

object Models {
  val jsonRpc = "2.0"

  type Id = Either[String, BigDecimal]

  type JsonRpcRequestMethod[PARAMS, ERROR, RESULT] = (JsonRpcRequest[PARAMS]) => Future[Either[JsonRpcErrorResponse[ERROR], JsonRpcResponse[RESULT]]]
  type JsonRpcNotificationMethod[PARAMS] = (JsonRpcNotification[PARAMS]) => Unit

  case class JsonRpcMethod(jsonrpc: String = jsonRpc, id: Option[Id], method: String)

  case class JsonRpcRequest[PARAMS]
  (
      jsonrpc: String = jsonRpc,
      id: Id,
      method: String,
      params: PARAMS
  )

  case class JsonRpcNotification[PARAMS]
  (
      jsonrpc: String = jsonRpc,
      method: String,
      params: PARAMS
  )

  case class JsonRpcResponse[RESULT]
  (
      jsonrpc: String = jsonRpc,
      id: Id,
      result: RESULT
  )

  case class JsonRpcErrorResponse[ERROR]
  (
      jsonrpc: String = jsonRpc,
      id: Option[Id],
      error: JsonRpcError[ERROR]
  )

  object JsonRpcResponse {
    def apply[ERROR](error: JsonRpcError[ERROR]) = JsonRpcErrorResponse(id = None, error = error)

    def apply[ERROR](id: Id, error: JsonRpcError[ERROR]) = JsonRpcErrorResponse(id = Some(id), error = error)
  }

  case class JsonRpcError[ERROR]
  (
      code: Int,
      message: String,
      data: Option[ERROR]
  )

  object JsonRpcError {
    def apply(code: Int, message: String): JsonRpcError[String] =
      JsonRpcError[String](code, message, None)

    def apply[ERROR](code: Int, message: String, data: ERROR): JsonRpcError[ERROR] =
      JsonRpcError[ERROR](code, message, Option(data))
  }

  object JsonRpcErrors {
    lazy val parseError = JsonRpcError(-32700, "Parse error", "Invalid JSON was received by the server. An error occurred on the server while parsing the JSON text.")
    lazy val invalidRequest = JsonRpcError(-32600, "Invalid Request", "The JSON sent is not a valid Request object.")
    lazy val methodNotFound = JsonRpcError(-32601, "Method not found", "The method does not exist / is not available.")
    lazy val invalidParams = JsonRpcError(-32602, "Invalid params", "Invalid method parameter(s).")
    lazy val internalError = JsonRpcError(-32603, "Internal error", "Internal JSON-RPC error.")
  }

}
