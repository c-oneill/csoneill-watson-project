package com.mycompany.app;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.HashMap;
import java.util.Map;

public class RemoveSpecialFilterFactory extends TokenFilterFactory {

    public RemoveSpecialFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new RemoveSpecialFilter(tokenStream);
    }

    public static void main( String[] args ) {
        RemoveSpecialFilterFactory f = new RemoveSpecialFilterFactory(new HashMap<>());
    }
}
