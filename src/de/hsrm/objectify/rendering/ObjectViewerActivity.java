package de.hsrm.objectify.rendering;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import de.hsrm.objectify.R;
import de.hsrm.objectify.actionbarcompat.ActionBarActivity;
import de.hsrm.objectify.utils.ExternalDirectory;

/**
 * This {@link Activity} holds a {@link TouchSurfaceView} which contains the
 * actual rendered object and takes care of the different states such as
 * <code>onPause</code> and <code>onResume</code>.
 * 
 * @author kwolf001
 * 
 */
public class ObjectViewerActivity extends ActionBarActivity {

	private static final String TAG = "ObjectViewer";
	private TouchSurfaceView glSurfaceView;
	private FrameLayout frameLayout;
	private Context context;
	private ObjectModel objectModel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		frameLayout = new FrameLayout(this);
		setContentView(frameLayout);
		setTitle(getString(R.string.object_viewer));
		// TODO: add actionbar buttons
//		addNewActionButton(R.drawable.ic_title_share, R.string.share,
//				new OnClickListener() {
//
//					@Override
//					public void onClick(View v) {
//						new GetScreenshot().execute();
//					}
//				});
//		addNewActionButton(R.drawable.ic_title_export, R.string.export,
//				new OnClickListener() {
//
//					@Override
//					public void onClick(View v) {
//						new ExportToObj().execute();
//					}
//				});
		Display display = getWindowManager().getDefaultDisplay();
		Bundle b = getIntent().getBundleExtra("bundle");
		objectModel = b.getParcelable("objectModel");
		glSurfaceView = new TouchSurfaceView(this, objectModel,
				display.getWidth(), display.getHeight());
		frameLayout.addView(glSurfaceView);
		// Adding controls
		LayoutInflater inflater = LayoutInflater.from(this);
		LinearLayout ll = (LinearLayout) inflater.inflate(
				R.layout.objectviewer_controls, null);
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
		frameLayout.addView(ll, params);
	}

	public void actionControl(View view) {
		String tag = (String) view.getTag();
		if (tag.equals("texture")) {
			objectModel.setRenderingMode(GL10.GL_TRIANGLES);
			glSurfaceView.requestRender();
		} else if (tag.equals("points")) {
			objectModel.setRenderingMode(GL10.GL_POINTS);
			glSurfaceView.requestRender();
		} else if (tag.equals("wireframe")) {
			objectModel.setRenderingMode(GL10.GL_LINES);
			glSurfaceView.requestRender();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		glSurfaceView.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		glSurfaceView.onResume();
	}

	private class ExportToObj extends AsyncTask<Void, Void, Boolean> {

		private ProgressDialog pleaseWait;
		private String path;

		@Override
		protected void onPreExecute() {
			pleaseWait = ProgressDialog.show(context, "",
					getString(R.string.creating_obj), true);
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			path = ExternalDirectory.getExternalRootDirectory()
					+ "/objectify_model.obj";
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(path));
				out.write("# Created by Objectify\n");
				float[] vertices = objectModel.getVertices();
				short[] faces = objectModel.getFaces();
				/* writing vertices */
				for (int i = 0; i < vertices.length; i += 3) {
					out.write("v " + vertices[i] + " " + vertices[i + 1] + " "
							+ vertices[i + 2] + "\n");
				}
				/* writing faces */
				for (int i = 0; i < faces.length; i += 3) {
					int one = faces[i] + 1;
					int two = faces[i + 1] + 1;
					int three = faces[i + 2] + 1;
					out.write("f " + one + " " + two + " " + three + "\n");
				}
				out.write("\n");
				out.flush();
				out.close();
			} catch (IOException e) {
				Log.e("ExportToObj", e.getLocalizedMessage());
				return false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			pleaseWait.dismiss();
			if (result) {
				Intent export = new Intent(Intent.ACTION_SEND);
				export.setType("model/obj");
				export.putExtra(Intent.EXTRA_STREAM,
						Uri.parse("file://" + path));
				startActivity(Intent.createChooser(export,
						getString(R.string.export)));
			} else {
				Toast.makeText(context,
						getString(R.string.creating_obj_failed),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * This class inherits from an {@link AsyncTask} and takes care of creating
	 * a screenshot from the given {@link GL10} context. It tries a few times to
	 * get the surface as a bitmap and finally gives up if theres no screenshot
	 * returned.
	 * 
	 * @author kwolf001
	 * 
	 */
	private class GetScreenshot extends AsyncTask<Void, Void, Bitmap> {

		private ProgressDialog pleaseWait;
		/**
		 * Amount of trials how many times we try to get a bitmap from the given
		 * surfaceview.
		 */
		private int TRIALS = 7;

		@Override
		protected void onPreExecute() {
			pleaseWait = ProgressDialog.show(context, "",
					getString(R.string.screenshot_creating), true);
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			Bitmap screenshot = null;
			for (int i = 1; i <= TRIALS; i++) {
				Log.d("SCREENSHOT", "i = " + i);
				screenshot = glSurfaceView.getSurfaceBitmap();
				SystemClock.sleep(100 * i);
				if (screenshot != null) {
					return screenshot;
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result != null) {
				Intent share = new Intent(Intent.ACTION_SEND);
				share.setType("image/jpeg");
				String path = ExternalDirectory.getExternalImageDirectory()
						+ "/objectify_screenshot.png";
				try {
					FileOutputStream fos = new FileOutputStream(path);
					BufferedOutputStream bos = new BufferedOutputStream(fos);
					result.compress(CompressFormat.PNG, 100, bos);
					bos.flush();
					bos.close();
					share.putExtra(Intent.EXTRA_STREAM,
							Uri.parse("file://" + path));
					startActivity(Intent.createChooser(share,
							getString(R.string.share)));
				} catch (FileNotFoundException e) {
					Toast.makeText(context,
							getString(R.string.screenshot_failed),
							Toast.LENGTH_LONG).show();
					Log.e(TAG, e.getMessage());
				} catch (IOException e) {
					Toast.makeText(context,
							getString(R.string.screenshot_failed),
							Toast.LENGTH_LONG).show();
					Log.e(TAG, e.getMessage());
				}
			} else {
				Toast.makeText(context, getString(R.string.screenshot_failed),
						Toast.LENGTH_LONG).show();
			}
			pleaseWait.dismiss();
		}

	}
}
