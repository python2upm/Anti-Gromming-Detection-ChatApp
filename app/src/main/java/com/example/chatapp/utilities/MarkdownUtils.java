package com.example.chatapp.utilities;

import android.content.Context;
import io.noties.markwon.Markwon;

public class MarkdownUtils {

    public static CharSequence formatMarkdown(Context context, String text) {
        if (text == null) return "";
        
        final Markwon markwon = Markwon.create(context);
        return markwon.toMarkdown(text);
    }
}
