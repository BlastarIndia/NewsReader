package de.farw.newsreader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.farw.newsreader.BleuAlgorithm.BleuData;
import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class FeedView extends Activity {
	private class FeedWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}

	private static final int ACTIVITY_READ = 0;

	private BleuAlgorithm ba;
	private WebView mWebView;
	private String url;
	private long id;
	private BleuData bd;

	// debugging stuff
	private FileOutputStream bleuOut = null;
	private OutputStreamWriter osw = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, ACTIVITY_READ, android.view.Menu.NONE, R.string.menu_read);

		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		switch (item.getItemId()) {
		case ACTIVITY_READ:
			NewsDroidDB db = new NewsDroidDB(getApplicationContext());
			db.open();
			db.markAsKnown(id);
			db.close();
			break;
		}
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			bleuOut = openFileOutput("bleu.txt", MODE_APPEND);
			osw = new OutputStreamWriter(bleuOut);
		} catch (IOException e) {
			Log.e("NewsDroid", e.toString());
		}
		ba = new BleuAlgorithm(getApplicationContext());
		mWebView = new WebView(this);
		mWebView.setWebViewClient(new FeedWebViewClient());
		String title;
		String content;
		setContentView(mWebView);
		if (savedInstanceState != null) {
			url = savedInstanceState.getString("url");
			content = savedInstanceState.getString("description");
			title = savedInstanceState.getString("title");
			id = savedInstanceState.getLong("id");
		} else {
			Bundle b = getIntent().getExtras();
			url = b.getString("url");
			content = b.getString("description");
			title = b.getString("title");
			id = b.getLong("id");
		}
		setTitle(title);
		bd = ba.scanArticle(content, id);
		content = generateHTMLContent(content, bd.matchingNGrams);
		mWebView.loadData(content, "text/html", "utf-8");
	}

	@Override
	public void onDestroy() { // for testing only
		super.onDestroy();
		try {
			NewsDroidDB db = new NewsDroidDB(this);
			db.open();
			int known = db.getArticleRead(id);
			osw.write(String.valueOf(bd.bleuValue) + ' ' + known + '\n');
			osw.flush();
			osw.close();
			bleuOut.close();
			db.close();
		} catch (IOException e) {
			Log.e("NewsDroid", e.toString());
		} catch (RuntimeException e) {
			Log.e("NewsDroid", e.toString());
		}
	}

	private String encodeHTML(String s) {
		StringBuffer out = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c > 127 || c == '"') { // || c == '<' || c == '>') {
				out.append("&#" + (int) c + ";");
			} else {
				out.append(c);
			}
		}
		return out.toString();
	}

	private String generateHTMLContent(String in, HashSet<String> matching) {
		ArrayList<String> tags = new ArrayList<String>();
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		final int imageSize = (int) (metrics.widthPixels * 0.95);

		in = in.replaceAll("\n", " ");
		// this pattern should match _all_ HTML tags
		Pattern pTag = Pattern
				.compile("</?\\w+((\\s+\\w+(\\s*=\\s*(?:\".*?\"|'.*?'|[^'\">\\s]+))?)+\\s*|\\s*)/?>|&[\\p{Alnum}]*?;");
		Matcher mTag = pTag.matcher(in);
		StringBuffer fromHTML = new StringBuffer();
		while (mTag.find()) {
			tags.add(mTag.group(0));
			mTag.appendReplacement(fromHTML, ":@:");
		}
		mTag.appendTail(fromHTML);

		StringBuffer sb = new StringBuffer(fromHTML);
		String buffer = fromHTML.toString();
		for (String match : matching) {
			String[] match_words = match.split("_");
			for (String mw : match_words) {
				sb = new StringBuffer();
				Pattern p = Pattern.compile("(?i)\\b" + mw + "\\p{Punct}*?");
				Matcher m = p.matcher(buffer);
				while (m.find()) {
					String replacement = "<font color=\"#00FF00\">" + m.group()
							+ "</font>";
					m.appendReplacement(sb, replacement);
				}
				m.appendTail(sb);
				buffer = sb.toString();
			}
		}

		String data = new String(sb);
		Iterator<String> iter = tags.iterator();
		while (iter.hasNext()) {
			data = data.replaceFirst(":@:", iter.next());
		}

		data = encodeHTML(data);
		String out = "<html><head><style type=\"text/css\">img {max-width:"
				+ imageSize + "px;}</style></head>";
		out += "<body>" + data + "<br><br>" + "<a href=" + url + ">"
				+ getString(R.string.read_more) + "</a></body></html>";

		return out;
	}
}
