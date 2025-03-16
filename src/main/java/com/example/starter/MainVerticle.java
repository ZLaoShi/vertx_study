package com.example.starter;

import java.util.Map;

import com.example.starter.model.User;
import com.example.starter.service.UserService;

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
                
                RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                    .type("Query", builder ->
                        builder
                            .dataFetcher("user", env -> {
                                Integer id = Integer.parseInt(env.getArgument("id"));
                                return userService.findById(id)
                                    .subscribeAsCompletionStage();
                            })
                            .dataFetcher("users", env -> 
                                userService.findAll()
                                    .subscribeAsCompletionStage())
                    )
                    .type("Mutation", builder ->
                        builder
                            .dataFetcher("createUser", env -> {
                                Map<String, Object> input = env.getArgument("input");
                                User user = new User();
                                user.setName((String) input.get("name"));
                                user.setEmail((String) input.get("email"));
                                return userService.createUser(user)
                                    .subscribeAsCompletionStage();
                            })
                            .dataFetcher("updateUser", env -> {
                                Integer id = Integer.parseInt(env.getArgument("id"));
                                Map<String, Object> input = env.getArgument("input");
                                User user = new User();
                                user.setName((String) input.get("name"));
                                user.setEmail((String) input.get("email"));
                                return userService.updateUser(id, user)
                                    .subscribeAsCompletionStage();
                            })
                            .dataFetcher("deleteUser", env -> {
                                Integer id = Integer.parseInt(env.getArgument("id"));
                                return userService.deleteUser(id)
                                    .subscribeAsCompletionStage();
                            })
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
 