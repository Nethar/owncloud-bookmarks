package cz.nethar.owncloudbookmarks;

public class BookmarkData {
	public String id;
	public String url;
	public int added;
	
	BookmarkData(String id, String url, int added) {
		this.id = id;
		this.url = url;
		this.added = added;
	}
}
