import caliban.interop.tapir.HttpInterpreter
import caliban.schema.GenericSchema
import caliban.ZHttpAdapter
import zio._
import zio.http._
import caliban._
import caliban.execution.ExecutionRequest
import caliban.wrappers.Wrapper.ExecutionWrapper
import sttp.tapir.model.ServerRequest

// Represents zio.telemetry.opentelemetry.tracing.Tracing
final case class MockTracing()

object ExampleApp extends ZIOAppDefault {
  import sttp.tapir.json.zio._

  final case class Token(value: String)

  final case class Response(value: String)

  final case class Queries(
    someQuery: RIO[Token, Response]
  )

  object customSchema extends GenericSchema[Token]
  import customSchema.auto._

  // Simulate TracingWrapper.traced
  val mockTracingWrapper = new ExecutionWrapper[MockTracing] {
    def wrap[R <: MockTracing](
      f: ExecutionRequest => ZIO[R, Nothing, GraphQLResponse[CalibanError]]
    ): ExecutionRequest => ZIO[R, Nothing, GraphQLResponse[CalibanError]] =
      request => { ZIO.service[MockTracing] *> f(request) }
  }

  val api: GraphQL[MockTracing with Token] = graphQL(
    RootResolver(
      Queries(ZIO.service[Token].map(token => Response(token.value)))
    )
  ) @@ mockTracingWrapper

  val tokenInterceptor: URLayer[ServerRequest, Token] =
    ZLayer(ZIO.service[ServerRequest] *> ZIO.succeed(Token("token")))

  val traceStuffInterceptor = ZLayer {
    // In actual project would use zio-telemetry unsafe api to set stuff
    ZIO.service[MockTracing].unit
  }

  val mockTracingLive: ULayer[MockTracing] =
    ZLayer.succeed(MockTracing())

  // Works
  val interceptor: URLayer[ServerRequest, Token with MockTracing] =
    ZLayer.makeSome[ServerRequest, Token with MockTracing](tokenInterceptor, mockTracingLive)

  // How to use this in combination with mockTracingWrapper?
  val interceptor2: URLayer[ServerRequest, Token] =
    ZLayer.makeSome[ServerRequest, Token](tokenInterceptor)

  // Or this? More closely resembles actual project
  val interceptorWithTraceStuff: URLayer[ServerRequest with MockTracing, Token] =
    ZLayer.makeSome[ServerRequest with MockTracing, Token](traceStuffInterceptor.flatMap(_ => tokenInterceptor))

  override def run: ZIO[Any, Throwable, Unit] =
    (for {
      interpreter <- api.interpreter
      _           <-
        Server
          .serve(
            Http
              .collectHttp[Request] { case _ -> !! / "api" / "graphql" =>
                ZHttpAdapter.makeHttpService(
                  HttpInterpreter(interpreter).intercept(interceptor)
                )
              }
          )
      _           <- Console.printLine("Server online at http://localhost:8088/")
      _           <- Console.printLine("Press RETURN to stop...") *> Console.readLine
    } yield ())
      .provide(Server.default)
}
