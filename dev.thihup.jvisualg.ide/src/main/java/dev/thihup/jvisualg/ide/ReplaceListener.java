package dev.thihup.jvisualg.ide;

import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

class ReplaceListener implements SearchListener {
    private final RTextArea textArea;
    private final Supplier<SearchContext[]> searchContext;

    public ReplaceListener(RTextArea textArea, Supplier<SearchContext[]> searchContext) {
        this.textArea = textArea;
        this.searchContext = searchContext;
    }

    @Override
    public void searchEvent(SearchEvent searchEvent) {
        switch (searchEvent.getType()) {
            case FIND -> SearchEngine.find(textArea, searchContext.get()[0]);
            case REPLACE -> SearchEngine.replace(textArea, searchContext.get()[0]);
            case REPLACE_ALL -> SearchEngine.replaceAll(textArea, searchContext.get()[0]);
            case MARK_ALL -> SearchEngine.markAll(textArea, searchContext.get()[0]);
        }
    }

    @Override
    public String getSelectedText() {
        return textArea.getSelectedText();
    }
}
