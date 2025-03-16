package com.example.starter;

import com.example.starter.service.UserService;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.AbstractVerticle;
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
    public void start(Promise<Void> startPromise) throws Exception {
        // 1. 异步读取 schema 文件
        vertx.fileSystem().readFile("schema.graphqls")
            .compose(buffer -> {
                // 2. 异步初始化 UserService
                return vertx.executeBlocking(() -> {
                    userService = new UserService();
                    
                    // 3. 设置 GraphQL
                    String schema = buffer.toString();
                    SchemaParser schemaParser = new SchemaParser();
                    TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);
                    
                    RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                        .type("Query", builder ->
                            builder.dataFetcher("user", environment -> {
                                String idStr = environment.getArgument("id");
                                Integer id = Integer.parseInt(idStr);
                                return userService.findById(id)
                                    .subscribeAsCompletionStage();
                            })
                        )
                        .build();

                    SchemaGenerator schemaGenerator = new SchemaGenerator();
                    GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(
                        typeDefinitionRegistry, 
                        runtimeWiring
                    );

                    return GraphQL.newGraphQL(graphQLSchema)
                        .instrumentation(new JsonObjectAdapter())
                        .build();
                });
            })
            .onSuccess(graphQL -> {
                // 4. 设置路由
                Router router = Router.router(vertx);
                router.route().handler(BodyHandler.create());
                
                router.route("/graphql").handler(
                    GraphQLHandler.create(graphQL, new GraphQLHandlerOptions()
                        .setRequestBatchingEnabled(true))
                );
                
                router.route("/graphiql/*").handler(
                    GraphiQLHandler.create(vertx, new GraphiQLHandlerOptions()
                        .setEnabled(true)
                        .setGraphQLUri("/graphql"))
                );
                
                // 5. 启动服务器
                vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(8888)
                    .onSuccess(server -> {
                        System.out.println("HTTP server started on port " + server.actualPort());
                        startPromise.complete();
                    })
                    .onFailure(startPromise::fail);
            })
            .onFailure(startPromise::fail);
    }
}