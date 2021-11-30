package com.mycompany.app;

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;

public class RemoveSpecialFilter extends FilteringTokenFilter {

    private final CharTermAttribute charTermAttr;

    public RemoveSpecialFilter(TokenStream in) {
        super(in);
        this.charTermAttr = addAttribute(CharTermAttribute.class);
    }

    // todo: idk if I want to do something more complex
    //  ---> may want to change depending on the tokenizer
    //private static boolean onlySpecialCharacters

    @Override
    protected boolean accept() throws IOException {
        return charTermAttr.length() <= 0 || Character.isLetter(charTermAttr.charAt(0));
    }
}
