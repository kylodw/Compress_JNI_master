package com.kylodw.bitmap.bitmapmasterjni;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

/**
 * @Author kylodw
 * @Description:
 * @Date 2019/03/30
 */
public class NativeUtil {
    static {
        System.loadLibrary("jpegbither");
        System.loadLibrary("native-lib");
    }

    public static void compressBitmap(Bitmap image, String filePath) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int options = 20;
        saveBitmap(image, options, filePath, true);
    }

    /**
     *
     * @param image
     * @param options
     * @param filePath
     * @param enable  是否采用哈夫曼
     */
    private  static void saveBitmap(Bitmap image, int options, String filePath, boolean enable){
        compressBitmap(image,image.getWidth(),image.getHeight(),options,filePath.getBytes(),enable);
    }

    /**
     *
     * @param image
     * @param width
     * @param height
     * @param options
     * @param bytes
     * @param enable
     */
    private  static native String compressBitmap(Bitmap image, int width, int height, int options, byte[] bytes, boolean enable);



}
