package com.kylodw.bitmap.bitmapmasterjni;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    //skia 去掉哈夫曼算法  采用定长编码算法 导致了图片处理后文件变大了
    //绕过android的api层自己编码，采用哈夫曼算法编码，解码google 保留了
    //下载JPEG -libjpeg库
    public static final int REQUEST_PICK_IMAGE = 10011;
    public static final int REQUEST_KITKAT_PICK_IMAGE = 10012;
    private File imageFile;
    private File sdFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sdFile = Environment.getExternalStorageDirectory();
        imageFile = new File(sdFile, "kylodw.jpg");

    }


    /**
     * 1.质量压缩
     * 原理：通过算法抠掉(同化)了图片中的一些某个些点附近相近的像素，达到降低质量介绍文件大小的目的。
     * 减小了图片质量
     * 注意：它其实只能实现对file的影响，对加载这个图片出来的bitmap内存是无法节省的，还是那么大。
     * 因为bitmap在内存中的大小是按照像素计算的，也就是width*height，对于质量压缩，并不会改变图片的真实的像素（像素大小不会变）。
     * <p>
     * 使用场景：
     * 将图片压缩后保存到本地，或者将图片上传到服务器。根据实际需求来。
     */
    public void qualitCompress(View v) {
//		BitmapFactory.decodeFile(pathName)
//		BitmapFactory.decodeResource(res, id)
//		BitmapFactory.decodeStream(is)

        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        //压缩图片
        compressImageToFile(bitmap, new File(sdFile, "qualityCompress.jpeg"));
    }

    public void sizeCompress(View v) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        compressBitmapToFileBySize(bitmap, new File(sdFile, "sizeCompress.jpeg"));

    }

    /**
     * 2.尺寸压缩
     * 通过减少单位尺寸的像素值，正真意义上的降低像素。1020*8880--
     * 使用场景：缓存缩略图的时候（头像处理）
     *
     * @param bmp
     * @param file
     */
    public static void compressBitmapToFileBySize(Bitmap bmp, File file) {
        //压缩尺寸倍数，值越大，图片的尺寸就越小
        int ratio = 8;
        Bitmap result = Bitmap.createBitmap(bmp.getWidth() / ratio, bmp.getHeight() / ratio, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(result);
        RectF rect = new RectF(0, 0, bmp.getWidth() / ratio, bmp.getHeight() / ratio);
        canvas.drawBitmap(bmp, null, rect, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        result.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static void compressImageToFile(Bitmap bmp, File file) {
        //0~100
        int quality = 50;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * 设置图片的采样率，降低图片像素
     *
     * @param filePath
     * @param file
     */
    public static void compressBitmap(String filePath, File file) {
        // 数值越高，图片像素越低
        int inSampleSize = 8;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
//	        options.inJustDecodeBounds = true;//为true的时候不会真正加载图片，而是得到图片的宽高信息。
        //采样率
        options.inSampleSize = inSampleSize;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 把压缩后的数据存放到baos中
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        try {
            if (file.exists()) {
                file.delete();
            } else {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void jniCompress(View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"),
                    REQUEST_PICK_IMAGE);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_KITKAT_PICK_IMAGE);
        }

    }
    //图片存在的方式： 文件file（质量和尺寸压缩）     流的形式  bitmap形式(内存)


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_IMAGE:
                    if (data != null) {
                        Uri uri = data.getData();
                        compressImage(uri);
                    } else {
                        Log.e("======", "========图片为空======");
                    }
                    break;
                case REQUEST_KITKAT_PICK_IMAGE:
                    if (data != null) {
                        Uri uri = ensureUriPermission(this, data);
                        compressImage(uri);
                    } else {
                        Log.e("======", "====-----==图片为空======");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void compressImage(Uri uri) {

        try {
            File file = new File(getExternalCacheDir(), "end_compress.jpg");
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            Log.e("======", "开始压缩"+file.getAbsolutePath());
            NativeUtil.compressBitmap(bitmap, file.getAbsolutePath());
            Log.e("======", "压缩结束"+file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ResourceType")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static Uri ensureUriPermission(Context context, Intent intent) {
        Uri uri = intent.getData();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final int takeFlags = intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
        }
        return uri;
    }

}
