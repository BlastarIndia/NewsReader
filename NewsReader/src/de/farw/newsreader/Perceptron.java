package de.farw.newsreader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.Context;
import android.util.Log;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class Perceptron {
	private static TIntDoubleHashMap w;
	private static final double alpha = 0.001;
	private static String wFile = "serializedW";

	public Perceptron(Context ctx) {
		if (w == null) {
			try {
				FileInputStream fis = ctx.openFileInput(wFile);
				ObjectInputStream is = new ObjectInputStream(fis);
				w.readExternal(is);
				is.close();
			} catch (IOException e) {
				Log.e("NewsDroid", e.toString());
				w = new TIntDoubleHashMap();
			} catch (ClassNotFoundException e) {
				Log.e("NewsDroid", e.toString());
			}
		}
	}

	public void learnArticle(TIntDoubleHashMap x, int y) {
		int yA = sgn(dotProduct(x));
		if (yA != y) {
			for (int k : x.keys()) {
				double adjusted = (w.get(k) + y * x.get(k)) * alpha;
				w.put(k, adjusted);
			}
		}
	}

	public static int getAssumption(TIntDoubleHashMap x) {
		return sgn(dotProduct(x));
	}

	public static TIntDoubleHashMap generateX(double bleuValue, long feedId,
			int commonWords, long timeDiff) {
		TIntDoubleHashMap x = new TIntDoubleHashMap();
		x.put(0, bleuValue);
		x.put(1, (double) commonWords);
		x.put(2, (double) timeDiff);
		x.put((int) (feedId + 3), 1.0);

		return x;
	}

	private static double dotProduct(TIntDoubleHashMap x) {
		int xSize = x.size();
		int wSize = w.size();
		TIntDoubleHashMap s = xSize < wSize ? x : w;
		TIntDoubleHashMap l = xSize >= wSize ? x : w;

		double sum = 0.0;
		int[] sKeys = s.keys();
		for (int k : sKeys) {
			sum += s.get(k) * l.get(k);
		}

		return sum;
	}

	private static int sgn(double in) {
		int retVal = in < 0.0 ? (-1) : 1;
		return retVal;
	}

	public static void saveW(Context ctx) {
		if (w == null)
			return;

		try {
			FileOutputStream fos = ctx.openFileOutput(wFile,
					Context.MODE_PRIVATE);
			ObjectOutputStream os = new ObjectOutputStream(fos);
			w.writeExternal(os);
			os.close();
			w = null;
		} catch (FileNotFoundException e) {
			Log.e("NewsDroid", e.toString());
		} catch (IOException e) {
			Log.e("NewsDroid", e.toString());
		}
	}
}
