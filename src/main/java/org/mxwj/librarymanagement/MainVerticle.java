package org.mxwj.librarymanagement;

import org.mxwj.librarymanagement.graphql.UserFetcher;
import org.mxwj.librarymanagement.service.AccountService;
import org.mxwj.librarymanagement.service.UserService;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.ext.web.handler.graphql.GraphQLHandlerOptions;
import io.vertx.ext.web.handler.graphql.GraphiQLHandler;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.ext.web.handler.graphql.instrumentation.JsonObjectAdapter;

public class MainVerticle extends AbstractVerticle {
    private UserService userService;
    private AccountService accountService;

    @Override
    public void start(Promise<Void> startPromise) {
        initializeUserService()
            .compose(this::setupGraphQL)
            .compose(this::setupRouter)
            .onSuccess(v -> startPromise.complete())
            .onFailure(startPromise::fail);
    }

    private Future<Void> initializeUserService() {
        return vertx.executeBlocking(() -> {
            userService = new UserService();
           // accountService = new AccountService();
            return null;
        });
    }

    private Future<GraphQL> setupGraphQL(Void v) {
        return vertx.fileSystem()
            .readFile("schema.graphqls")
            .map(buffer -> {
                String schema = buffer.toString();
                SchemaParser schemaParser = new SchemaParser();
                TypeDefinitionRegistry typeRegistry = schemaParser.parse(schema);

                // 创建 Fetcher
                UserFetcher userFetcher = new UserFetcher(userService);
                // AuthFetcher authFetcher = new AuthFetcher(accountService);

                RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                    .type("Query", builder ->
                        builder
                            .dataFetcher("user", userFetcher.getUserById())
                            .dataFetcher("users", userFetcher.getUsers())
                    )
                    .type("Mutation", builder ->
                        builder
                            .dataFetcher("createUser", userFetcher.createUser())
                            .dataFetcher("updateUser", userFetcher.updateUser())
                            .dataFetcher("deleteUser", userFetcher.deleteUser())
                            //.dataFetcher("login", authFetcher.login())
                            // .dataFetcher("register", authFetcher.register())
                    )
                    .build();

                GraphQLSchema graphQLSchema = new SchemaGenerator()
                    .makeExecutableSchema(typeRegistry, runtimeWiring);

                return GraphQL.newGraphQL(graphQLSchema)
                    .instrumentation(new JsonObjectAdapter())
                    .build();
            });
    }

    private Future<Void> setupRouter(GraphQL graphQL) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        
        router.route("/graphql").handler(
            GraphQLHandler.create(graphQL, 
                new GraphQLHandlerOptions()
                    .setRequestBatchingEnabled(true))
        );
        
        router.route("/graphiql/*").handler(
            GraphiQLHandler.create(vertx, 
                new GraphiQLHandlerOptions()
                    .setEnabled(true)
                    .setGraphQLUri("/graphql"))
        );
        
        return vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888)
            .map(server -> {
                System.out.println("Server started on port " + server.actualPort());
                return null;
            });
    }
}
