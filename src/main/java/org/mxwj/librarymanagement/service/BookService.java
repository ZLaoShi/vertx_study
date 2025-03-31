package org.mxwj.librarymanagement.service;

import org.hibernate.reactive.mutiny.Mutiny;
import org.mxwj.librarymanagement.lib.DatabaseManager;

public class BookService {
    private final Mutiny.SessionFactory factory;

    public BookService() {
        factory = DatabaseManager.getSessionFactory();
    }
    
}
