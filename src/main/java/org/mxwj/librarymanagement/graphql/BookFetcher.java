package org.mxwj.librarymanagement.graphql;

import org.mxwj.librarymanagement.service.BookService;

public class BookFetcher {
    private final BookService bookService;

    public BookFetcher(BookService bookService) {
        this.bookService = bookService;
    }

    
}
