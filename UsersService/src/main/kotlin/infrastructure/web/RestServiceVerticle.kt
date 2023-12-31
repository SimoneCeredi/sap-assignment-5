package infrastructure.web

import infrastructure.web.handlers.UserHandler
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import java.util.logging.Level
import java.util.logging.Logger


interface RestServiceVerticle {
    val port: Int
    val userHandler: UserHandler
    fun start()
}

class RestServiceVerticleImpl(
    override val port: Int,
    override val userHandler: UserHandler,
) : RestServiceVerticle, AbstractVerticle() {

    private val logger = Logger.getLogger("[RestService]")


    override fun start() {
        logger.log(Level.INFO, "Service initializing...")
        val server = vertx.createHttpServer()
        val router = Router.router(vertx)

        router.apply {
            route("/static/*").handler(StaticHandler.create().setCachingEnabled(false))
            route().handler(BodyHandler.create())

            route(HttpMethod.POST, "/users").handler(userHandler::registerNewUser)
            route(HttpMethod.GET, "/users/:userId").handler(userHandler::getUser)
            route(HttpMethod.GET, "/health").handler { context: RoutingContext ->
                context.response().end(JsonObject().put("status", "UP").toString())
            }
        }

        server.apply {
            requestHandler(router)
            listen(port)
        }

        logger.log(
            Level.INFO, "Service ready, listening on port $port"
        )
    }
}

fun RoutingContext.sendReply(message: JsonObject) {
    this.response().putHeader("content-type", "application/json").end(message.toString())
}

fun RestServiceVerticle(
    port: Int,
    userHandler: UserHandler,
) = RestServiceVerticleImpl(port, userHandler)