package gateway.controller;

import com.sun.net.httpserver.HttpExchange;

import core.Data;
import core.Http;
import core.Settings;
import gateway.DAO.UpstreamDAO;

public final class GatewayController {

    private final UpstreamDAO upstream;

    public GatewayController(Settings settings) {
        upstream = new UpstreamDAO(settings);
    }

    public void handle(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();

        if (path.equals("/health")) {
            Http.json(
                    exchange,
                    200,
                    Data.row(
                            "service", "client-gateway",
                            "mode", "live",
                            "status", "UP"
                    )
            );
            return;
        }

        upstream.forward(exchange);
    }
}