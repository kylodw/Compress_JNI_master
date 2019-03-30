#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/bitmap.h>
#include <malloc.h>
#include <setjmp.h>


#define LOG_TAG "bitmap_jni"
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, format, ##__VA_ARGS__)
#define LOGW(format, ...) __android_log_write(ANDROID_LOG_WARN,LOG_TAG,format, ##__VA_ARGS__)
#define  true 1
#define  false 0
typedef uint8_t BYTE;

extern "C" {
#include "jpeglib.h"
#include "cdjpeg.h"
#include "jversion.h"
#include "android/config.h"

JNIEXPORT jstring JNICALL
Java_com_kylodw_bitmap_bitmapmasterjni_NativeUtil_compressBitmap(
        JNIEnv *env, jclass type, jobject bitmap, jint width, jint height, jint options,
        jbyteArray bytes_, jboolean enable);
}


char *error;

struct my_error_mgr {
    struct jpeg_error_mgr pub;
    jmp_buf setjmp_buffer;
};


typedef struct my_error_mgr *my_error_ptr;


METHODDEF(void)
my_error_exit(j_common_ptr cinfo) {
    my_error_ptr myerr = (my_error_ptr) cinfo->err;
    (*cinfo->err->output_message)(cinfo);
    error = (char *) myerr->pub.jpeg_message_table[myerr->pub.msg_code];
    LOGE("jpeg_message_table[%d]:%s", myerr->pub.msg_code,
         myerr->pub.jpeg_message_table[myerr->pub.msg_code]);
    // LOGE("addon_message_table:%s", myerr->pub.addon_message_table);
//  LOGE("SIZEOF:%d",myerr->pub.msg_parm.i[0]);
//  LOGE("sizeof:%d",myerr->pub.msg_parm.i[1]);
    longjmp(myerr->setjmp_buffer, 1);
}


int generateJPEG(BYTE *data, int w, int h, int quality,
                 const char *outfilename, jboolean optimize) {

    //jpeg的结构体，保存的比如宽、高、位深、图片格式等信息，相当于java的类
    struct jpeg_compress_struct jcs;

    //当读完整个文件的时候就会回调my_error_exit这个退出方法。setjmp是一个系统级函数，是一个回调。
    struct my_error_mgr jem;
    jcs.err = jpeg_std_error(&jem.pub);
    jem.pub.error_exit = my_error_exit;
    if (setjmp(jem.setjmp_buffer)) {
        return 0;
    }

    //初始化jsc结构体
    jpeg_create_compress(&jcs);
    //打开输出文件 wb:可写byte
    FILE *f = fopen(outfilename, "wb");
    if (f == NULL) {
        return 0;
    }
    //设置结构体的文件路径
    jpeg_stdio_dest(&jcs, f);
    jcs.image_width = w;//设置宽高
    jcs.image_height = h;
//	if (optimize) {
//		LOGI("optimize==ture");
//	} else {
//		LOGI("optimize==false");
//	}

    //看源码注释，设置哈夫曼编码：/* TRUE=arithmetic coding, FALSE=Huffman */
    jcs.arith_code = false;
    int nComponent = 3;
    /* 颜色的组成 rgb，三个 # of color components in input image */
    jcs.input_components = nComponent;
    //设置结构体的颜色空间为rgb
    jcs.in_color_space = JCS_RGB;
//	if (nComponent == 1)
//		jcs.in_color_space = JCS_GRAYSCALE;
//	else
//		jcs.in_color_space = JCS_RGB;

    //全部设置默认参数/* Default parameter setup for compression */
    jpeg_set_defaults(&jcs);
    //是否采用哈弗曼表数据计算 品质相差5-10倍
    jcs.optimize_coding = optimize;
    //设置质量
    jpeg_set_quality(&jcs, quality, true);
    //开始压缩，(是否写入全部像素)
    jpeg_start_compress(&jcs, TRUE);

    JSAMPROW row_pointer[1];
    int row_stride;
    //一行的rgb数量
    row_stride = jcs.image_width * nComponent;
    //一行一行遍历
    while (jcs.next_scanline < jcs.image_height) {
        //得到一行的首地址
        row_pointer[0] = &data[jcs.next_scanline * row_stride];

        //此方法会将jcs.next_scanline加1
        jpeg_write_scanlines(&jcs, row_pointer, 1);//row_pointer就是一行的首地址，1：写入的行数
    }
    jpeg_finish_compress(&jcs);//结束
    jpeg_destroy_compress(&jcs);//销毁 回收内存
    fclose(f);//关闭文件

    return 1;
}

/**
 * byte数组转c的字符串
 */
char *jstrinToString(JNIEnv *env, jbyteArray byte_array) {
    char *rtn = NULL;
    jsize alen = env->GetArrayLength(byte_array);
    jbyte *bytes = env->GetByteArrayElements(byte_array, NULL);
    if (alen > 0) {
        rtn = (char *) (malloc(alen + 1));
        memcpy(rtn, bytes, alen);
        rtn[alen] = 0;
    }
    env->ReleaseByteArrayElements(byte_array, bytes, 0);
    return rtn;
}

JNIEXPORT jstring JNICALL
Java_com_kylodw_bitmap_bitmapmasterjni_NativeUtil_compressBitmap(
        JNIEnv *env, jclass type, jobject bitmap, jint width, jint height, jint options,
        jbyteArray bytes_, jboolean enable) {
    //1，将android的bitmap 解码，并转换成rgb的数据
    //2,JPEG对象分配空间以及初始化
    //3指定压缩数据源
    //4，获取文件信息
    //5，为压缩设置参数
    //6 ，开始压缩
    //7 压缩结束
    //8，释放资源
//    AndroidBitmapInfo bitmapInfo;
    //头指针
    BYTE *pixels;

//    AndroidBitmap_getInfo(env, bitmap, &bitmapInfo);
    AndroidBitmap_lockPixels(env, bitmap, (void**)&pixels);
//    BYTE *pix_data = (BYTE *) (pixels);

    //解析每一个像素点的rgb值 保存到一位数组
    BYTE *data;
    BYTE a, r, g, b;
    data = (BYTE *) malloc(width * height * 3);
    BYTE *tmp_data;
    tmp_data = data; //保存data的首地址
    int i = 0, j = 0;
    int color = 0;
    for (i = 0; i < height; ++i) {
        for (j = 0; j < width; ++j) {
            //color 拿到二维数组的每一个信息
            color = *((int *) pixels);
            //& 0XFF000000  混合在
//            a = (BYTE)((color & 0XFF000000) >> 24);
            r = (BYTE) ((color & 0X00FF0000) >> 16);
            g = (BYTE) ((color & 0X0000FF00) >> 8);
            b = (BYTE) ((color & 0X000000FF));
            *data = b;
            *(data + 1) = g;
            *(data + 2) = r;
            //开始下一个像素点  rgb 所以+3
            data = data + 3;
            //这是最原始的像素点  argb
            pixels += 4;
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    //开始压缩
    char *file_out_name = jstrinToString(env, bytes_);
    int result_code = generateJPEG(tmp_data, width, height, options, file_out_name, enable);
    //返回String
    if (result_code == 0) {
        jstring result = env->NewStringUTF("-1");
        return result;
    }
    return env->NewStringUTF("1");
}

