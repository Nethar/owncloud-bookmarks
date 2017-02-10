package cz.nethar.owncloudbookmarks;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by schabesbergerch64244 on 09.02.17.
 */

class BookmarkMaps {
    private SortedMap<String, SortedMap<String, BookmarkData>> bookmarks;
    private SortedMap<String, BookmarkData> noTagBookmarks;

    BookmarkMaps() {
        bookmarks = new TreeMap<String, SortedMap<String, BookmarkData>>(String.CASE_INSENSITIVE_ORDER);
        noTagBookmarks = new TreeMap<String, BookmarkData>(String.CASE_INSENSITIVE_ORDER);
    }

    SortedMap<String, SortedMap<String, BookmarkData>> getTagedBookmarks() {
        return bookmarks;
    }

    SortedMap<String, BookmarkData> getNoTagBookmakrs() {
        return noTagBookmarks;
    }

    Boolean isEmpty() {
        return (bookmarks.isEmpty() && noTagBookmarks.isEmpty());
    }

    int count() {
        int c = noTagBookmarks.size();

        Object[] arr = bookmarks.keySet().toArray();
        for (int i = 0; i < arr.length; i++) {
            c += bookmarks.get((String)arr[i]).size();
        }

        return c;
    }

    void addBookmark(String name, BookmarkData bookmark) {
        noTagBookmarks.put(name, bookmark);
    }
}