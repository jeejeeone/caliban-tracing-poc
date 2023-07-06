import caliban.interop.tapir.HttpInterpreter
import caliban.schema.GenericSchema
import caliban.ZHttpAdapter
import zio._
import zio.http._
import caliban._
import caliban.execution.ExecutionRequest
import caliban.wrappers.Wrapper.ExecutionWrapper
import sttp.tapir.model.ServerRequest

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

  val mockTracingWrapper = new ExecutionWrapper[MockTracing] {
    def wrap[R <: MockTracing](
      f: ExecutionRequest => ZIO[R, Nothing, GraphQLResponse[CalibanError]]
    ): ExecutionRequest => ZIO[R, Nothing, GraphQLResponse[CalibanError]] =
      request => { ZIO.service[MockTracing] *> f(request) }
  }

  val api = graphQL(
    RootResolver(
      Queries(ZIO.service[Token].map(token => Response(token.value)))
    )
  ) @@ mockTracingWrapper

  val tokenInterceptor = ZLayer(ZIO.succeed(Token("token")))
  val mockTracingLayer = ZLayer.succeed(MockTracing())

  // Works
  val interceptor = ZLayer.makeSome[ServerRequest, Token with MockTracing](tokenInterceptor, mockTracingLayer)

  // How to use this?
  val interceptor2 = ZLayer.makeSome[MockTracing with ServerRequest, Token](tokenInterceptor)

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
