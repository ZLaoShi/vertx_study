package org.mxwj.librarymanagement;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.mxwj.librarymanagement.graphql.AccountFetcher;
import org.mxwj.librarymanagement.graphql.AuthFetcher;
import org.mxwj.librarymanagement.graphql.BookFetcher;
import org.mxwj.librarymanagement.graphql.BorrowFetcher;
import org.mxwj.librarymanagement.graphql.UserFetcher;
import org.mxwj.librarymanagement.graphql.UserInfoFetcher;
import org.mxwj.librarymanagement.middleware.GraphQLAuthHandler;
import org.mxwj.librarymanagement.service.AccountService;
import org.mxwj.librarymanagement.service.BookService;
import org.mxwj.librarymanagement.service.BorrowService;
import org.mxwj.librarymanagement.service.UserInfoService;
import org.mxwj.librarymanagement.service.UserService;
import org.mxwj.librarymanagement.utils.JWTUtils;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
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
    private UserInfoService userInfoService;
    private BookService bookService;
    private BorrowService borrowService;

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
            accountService = new AccountService(vertx);
            userInfoService = new UserInfoService();
            bookService = new BookService();
            borrowService = new BorrowService();
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

                UserFetcher userFetcher = new UserFetcher(userService);
                AuthFetcher authFetcher = new AuthFetcher(accountService);
                UserInfoFetcher userInfoFetcher = new UserInfoFetcher(userInfoService);
                BookFetcher bookFetcher = new BookFetcher(bookService);
                BorrowFetcher borrowFetcher = new BorrowFetcher(borrowService);
                AccountFetcher accountFetcher = new AccountFetcher(accountService);

                RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                    .type("Query", builder ->
                        builder
                            .dataFetcher("user", GraphQLAuthHandler.requireUser(userFetcher.getUserById()))
                            .dataFetcher("users", userFetcher.getUsers())
                    
                            .dataFetcher("userInfo", userInfoFetcher.getUserInfoById())

                            .dataFetcher("book", bookFetcher.getBookById())
                            .dataFetcher("books", bookFetcher.getBooks())
                            .dataFetcher("searchBooks", bookFetcher.searchBooks())

                            .dataFetcher("myBorrowRecords", 
                                GraphQLAuthHandler.requireUser(borrowFetcher.getMyBorrowRecords()))
                            .dataFetcher("borrowRecords", 
                                GraphQLAuthHandler.requireAdmin(borrowFetcher.getAllBorrowRecords()))

                            .dataFetcher("account", GraphQLAuthHandler.requireAdmin(accountFetcher.getAccountById()))
                            .dataFetcher("accounts", GraphQLAuthHandler.requireAdmin(accountFetcher.getAccounts()))
                            .dataFetcher("searchAccounts", GraphQLAuthHandler.requireAdmin(accountFetcher.searchAccounts()))
                    )
                    .type("Mutation", builder ->
                        builder
                            .dataFetcher("login", authFetcher.login())
                            .dataFetcher("register", authFetcher.register())
                            .dataFetcher("logout", GraphQLAuthHandler.requireUser(authFetcher.logout()))

                            .dataFetcher("createUserInfo", GraphQLAuthHandler.requireUser(userInfoFetcher.createUserInfo()))
                            .dataFetcher("updateUserInfo", GraphQLAuthHandler.requireUser(userInfoFetcher.updateUserInfo()))

                            .dataFetcher("createBook", GraphQLAuthHandler.requireAdmin(bookFetcher.createBook()))
                            .dataFetcher("updateBook", GraphQLAuthHandler.requireAdmin(bookFetcher.updateBook()))
                            .dataFetcher("deleteBook", GraphQLAuthHandler.requireAdmin(bookFetcher.deleteBook()))

                            .dataFetcher("borrowBook", GraphQLAuthHandler.requireUser(borrowFetcher.borrowBook()))
                            .dataFetcher("returnBook", GraphQLAuthHandler.requireUser(borrowFetcher.returnBook()))

                            .dataFetcher("updateAccountStatus", GraphQLAuthHandler.requireAdmin(accountFetcher.updateAccountStatus()))
                            .dataFetcher("updataAccountType", GraphQLAuthHandler.requireAdmin(accountFetcher.updateAccountType()))
                            .dataFetcher("resetPassword", GraphQLAuthHandler.requireAdmin(accountFetcher.resetPassword()))

                            .dataFetcher("adminBorrowBook", GraphQLAuthHandler.requireAdmin(borrowFetcher.adminBorrowBook()))
                            .dataFetcher("adminReturnBook", GraphQLAuthHandler.requireAdmin(borrowFetcher.adminReturnBook()))
                            .dataFetcher("adminForceReturn", GraphQLAuthHandler.requireAdmin(borrowFetcher.adminForceReturn()))

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

        // 添加CORS处理器
        router.route().handler(context -> {
            context.response()
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                .putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization")
                .putHeader("Access-Control-Allow-Credentials", "true");

            if (context.request().method() == HttpMethod.OPTIONS) {
                context.response().setStatusCode(204).end();
            } else {
                context.next();
            }
        });

        router.route().handler(BodyHandler.create());
        JWTUtils jwtUtils = new JWTUtils(vertx);

        //鉴权
        router.route("/graphql").handler(context -> {
            String authHeader = context.request().getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                jwtUtils.validateToken(token)
                    .onSuccess(user -> {
                        // 将用户信息存储在 RoutingContext 中
                        context.put("userPrincipal", user.principal());

                        System.out.printf("Current Date and Time (UTC): %s%n",
                            LocalDateTime.now(ZoneOffset.UTC).format(
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            ));
                        System.out.printf("Current User's Login: %s%n",
                            user.principal().getString("sub"));

                        context.next();
                    })
                    .onFailure(err -> context.fail(401));
            } else {
                context.next();
            }
        }); //TODO 第一次启动后如果输入一个不存在的token会导致服务器错误

        GraphQLHandler graphQLHandler = GraphQLHandler.create(graphQL,
            new GraphQLHandlerOptions()
                .setRequestBatchingEnabled(true));

        router.route("/graphql").handler(graphQLHandler);

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
